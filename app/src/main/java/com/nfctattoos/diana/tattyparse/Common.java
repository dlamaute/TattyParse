package com.nfctattoos.diana.tattyparse;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import com.parse.Parse;

/**
 * Common functions and variables for all Activities.
 * @author Gerhard Klostermeier
 * modified by Diana Lamaute
 */
public class Common extends Application {

    /**
     * This file contains some standard Mifare keys.
     * <ul>
     * <li>0xFFFFFFFFFFFF - Un-formatted, factory fresh tags.</li>
     * <li>0xA0A1A2A3A4A5 - First sector of the tag (Mifare MAD).</li>
     * <li>0xD3F7D3F7D3F7 - NDEF formatted tags.</li>
     * </ul>
     */

    private static final String LOG_TAG = Common.class.getSimpleName();

    /**
     * The last detected tag.
     * Set by {@link #treatAsNewTag(android.content.Intent, android.content.Context)}
     */
    private static Tag mTag = null;

    /**
     * The last detected UID.
     * Set by {@link #treatAsNewTag(android.content.Intent, android.content.Context)}
     */
    private static byte[] mUID = null;

    /**
     * The version code from the Android manifest.
     */
    private static String mVersionCode;

    /**
     * 1 if the device does support Mifare Classic. -1 if it doesn't support
     * it. 0 if the support check was not yet performed.
     * Checking for Mifare Classic support is really expensive. Therefore
     * remember the result here.
     */
    private static int mHasMifareClassicSupport = 0;


    private static NfcAdapter mNfcAdapter;
    private static Context mAppContext;
    private static float mScale;

    protected static String applicationID = "FAC70125-3ADD-4880-FF6B-FF065C81F300";
    protected static String androidSecretKey = "BC13AD3C-241D-1E61-FF4D-E84E5DCAEE00";
    protected static String appVersion = "v1";

// ############################################################################

    /**
     * Initialize the {@link #mAppContext} with the application context
     * (for {@link #getPreferences()}) and {@link #mVersionCode}.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = getApplicationContext();
        mScale = getResources().getDisplayMetrics().density;
        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);
        Parse.initialize(this);


        try {
            mVersionCode = getPackageManager().getPackageInfo(
                    getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(LOG_TAG, "Version not found.");
        }
    }

    /**
     * Get the shared preferences with application context for saving
     * and loading ("global") values.
     *
     * @return The shared preferences object with application context.
     */
    public static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(mAppContext);
    }

    /**
     * Enables the NFC foreground dispatch system for the given Activity.
     *
     * @param targetActivity The Activity that is in foreground and wants to
     *                       have NFC Intents.
     * @see #disableNfcForegroundDispatch(android.app.Activity)
     */
    public static void enableNfcForegroundDispatch(Activity targetActivity) {
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {

            Intent intent = new Intent(targetActivity,
                    targetActivity.getClass()).addFlags(
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    targetActivity, 0, intent, 0);
            mNfcAdapter.enableForegroundDispatch(
                    targetActivity, pendingIntent, null, new String[][]{
                            new String[]{NfcA.class.getName()}});
        }
    }

    /**
     * Disable the NFC foreground dispatch system for the given Activity.
     *
     * @param targetActivity An Activity that is in foreground and has
     *                       NFC foreground dispatch system enabled.
     * @see #enableNfcForegroundDispatch(Activity)
     */
    public static void disableNfcForegroundDispatch(Activity targetActivity) {
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
            mNfcAdapter.disableForegroundDispatch(targetActivity);
        }
    }

    /**
     * For Activities which want to treat new Intents as Intents with a new
     * Tag attached. If the given Intent has a Tag extra, it will be patched
     * by {@link MCReader#patchTag(Tag)} and  {@link #mTag} as well as
     * {@link #mUID} will be updated. A Toast message will be shown in the
     * Context of the calling Activity. This method will also check if the
     * device/tag supports Mifare Classic (see return values and
     * {@link #checkMifareClassicSupport(Tag, Context)}).
     *
     * @param intent  The Intent which should be checked for a new Tag.
     * @param context The Context in which the Toast will be shown.
     * @return <ul>
     * <li>0 - The device/tag supports Mifare Classic</li>
     * <li>-1 - Device does not support Mifare Classic.</li>
     * <li>-2 - Tag does not support Mifare Classic.</li>
     * <li>-3 - Error (tag or context is null).</li>
     * <li>-4 - Wrong Intent (action is not "ACTION_TECH_DISCOVERED").</li>
     * </ul>
     * @see #mTag
     * @see #mUID
     * @see #checkMifareClassicSupport(Tag, Context)
     */
    public static String treatAsNewTag(Intent intent, Context context) {
        // Check if Intent has a NFC Tag.
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            tag = MCReader.patchTag(tag);
            setTag(tag);
            String id = byte2HexString(tag.getId());
            return id;
        }
        return context.getString(R.string.tag_failure);
    }

    /**
     * Check if the device supports the Mifare Classic technology.
     * In order to do so, check if there are files like "/dev/bcm2079x-i2c" or
     * "/system/lib/libnfc-bcrm*". Files like these are indicators for a
     * NFC controller manufactured by Broadcom. Broadcom chips don't support
     * Mifare Classic.
     *
     * @return True if the device supports Mifare Classic. False otherwise.
     * @see #mHasMifareClassicSupport
     */
    public static boolean hasMifareClassicSupport() {
        if (mHasMifareClassicSupport != 0) {
            return mHasMifareClassicSupport == 1;
        }

        // Check for the MifareClassic class.
        // It is most likely there on all NFC enabled phones.
        // Therefore this check is not needed.
        /*
        try {
            Class.forName("android.nfc.tech.MifareClassic");
        } catch( ClassNotFoundException e ) {
            // Class not found. Devices does not support Mifare Classic.
            return false;
        }
        */

        // Check if there is the NFC device "bcm2079x-i2c".
        // Chips by Broadcom don't support Mifare Classic.
        // This could fail because on a lot of devices apps don't have
        // the sufficient permissions.
        File device = new File("/dev/bcm2079x-i2c");
        if (device.exists()) {
            mHasMifareClassicSupport = -1;
            return false;
        }

        // Check if there is the NFC device "pn544".
        // The PN544 NFC chip is manufactured by NXP.
        // Chips by NXP support Mifare Classic.
        device = new File("/dev/pn544");
        if (device.exists()) {
            mHasMifareClassicSupport = 1;
            return true;
        }

        // Check if there are NFC libs with "brcm" in their names.
        // "brcm" libs are for devices with Broadcom chips. Broadcom chips
        // don't support Mifare Classic.
        File libsFolder = new File("/system/lib");
        File[] libs = libsFolder.listFiles();
        for (File lib : libs) {
            if (lib.isFile()
                    && lib.getName().startsWith("libnfc")
                    && lib.getName().contains("brcm")
                // Add here other non NXP NFC libraries.
                    ) {
                mHasMifareClassicSupport = -1;
                return false;
            }
        }
        mHasMifareClassicSupport = 1;
        return true;
    }

    /**
     * Check if the tag and the device support the Mifare Classic technology.
     *
     * @param tag     The tag to check.
     * @param context The context of the package manager.
     * @return <ul>
     * <li>0 - Device and tag support Mifare Classic.</li>
     * <li>-1 - Device does not support Mifare Classic.</li>
     * <li>-2 - Tag does not support Mifare Classic.</li>
     * <li>-3 - Error (tag or context is null).</li>
     * </ul>
     */
    public static int checkMifareClassicSupport(Tag tag, Context context) {
        if (tag == null || context == null) {
            // Error.
            return -3;
        }

        if (Arrays.asList(tag.getTechList()).contains(
                MifareClassic.class.getName())) {
            // Device and tag support Mifare Classic.
            return 0;

        } else {
            // Check if device does not support Mifare Classic.
            // For doing so, check if the ATQA + SAK of the tag indicate that
            // it's a Mifare Classic tag.
            // See: http://www.nxp.com/documents/application_note/AN10833.pdf
            // (Table 5 and 6)
            NfcA nfca = NfcA.get(tag);
            byte[] atqa = nfca.getAtqa();
            if (atqa[1] == 0 &&
                    (atqa[0] == 4 || atqa[0] == (byte) 0x44 ||
                            atqa[0] == 2 || atqa[0] == (byte) 0x42)) {
                // ATQA says it is most likely a Mifare Classic tag.
                byte sak = (byte) nfca.getSak();
                if (sak == 8 || sak == 9 || sak == (byte) 0x18 ||
                        sak == (byte) 0x88) {
                    // SAK says it is most likely a Mifare Classic tag.
                    // --> Device does not support Mifare Classic.
                    return -1;
                }
            }
            // Nope, it's not the device (most likely).
            // The tag does not support Mifare Classic.
            return -2;
        }
    }

    /**
     * Create a connected {@link MCReader} if there is a present Mifare Classic
     * tag. If there is no Mifare Classic tag an error
     * message will be displayed to the user.
     *
     * @param context The Context in which the error Toast will be shown.
     * @return A connected {@link MCReader} or "null" if no tag was present.
     */
    public static MCReader checkForTagAndCreateReader(Context context) {
        MCReader reader;
        boolean tagLost = false;
        // Check for tag.
        if (mTag != null && (reader = MCReader.get(mTag)) != null) {
            try {
                reader.connect();
            } catch (Exception e) {
                tagLost = true;
            }
            if (!tagLost && !reader.isConnected()) {
                reader.close();
                tagLost = true;
            }
            if (!tagLost) {
                return reader;
            }
        }

        // Error. The tag is gone.
        Toast.makeText(context, R.string.info_no_tag_found,
                Toast.LENGTH_LONG).show();
        return null;
    }

    /**
     * Convert an array of bytes into a string of hex values.
     *
     * @param bytes Bytes to convert.
     * @return The bytes in hex string format.
     */
    public static String byte2HexString(byte[] bytes) {
        String ret = "";
        if (bytes != null) {
            for (Byte b : bytes) {
                ret += String.format("%02X", b.intValue() & 0xFF);
            }
        }
        return ret;
    }

    /**
     * Convert a string of hex data into a byte array.
     * Original author is: Dave L. (http://stackoverflow.com/a/140861).
     *
     * @param s The hex string to convert
     * @return An array of bytes with the values of the string.
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        try {
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i + 1), 16));
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "Argument(s) for hexStringToByteArray(String s)"
                    + "was not a hex string");
        }
        return data;
    }


    /**
     * Copy a text to the Android clipboard.
     *
     * @param text    The text that should by stored on the clipboard.
     * @param context Context of the SystemService
     *                (and the Toast message that will by shown).
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void copyToClipboard(String text, Context context) {
        if (!text.equals("")) {
            if (Build.VERSION.SDK_INT >= 11) {
                // Android API level 11+.
                android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager)
                                context.getSystemService(
                                        Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip =
                        android.content.ClipData.newPlainText(
                                "mifare classic tool data", text);
                clipboard.setPrimaryClip(clip);
            } else {
                // Android API level 10.
                android.text.ClipboardManager clipboard =
                        (android.text.ClipboardManager)
                                context.getSystemService(
                                        Context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
            }
            Toast.makeText(context, "info_copied_to_clipboard",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get the content of the Android clipboard (if it is plain text).
     *
     * @param context Context of the SystemService
     * @return The content of the Android clipboard. On error
     * (clipboard empty, clipboard content not plain text, etc.) null will
     * be returned.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static String getFromClipboard(Context context) {
        if (Build.VERSION.SDK_INT >= 11) {
            // Android API level 11+.
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager)
                            context.getSystemService(
                                    Context.CLIPBOARD_SERVICE);
            if (clipboard.getPrimaryClip() != null
                    && clipboard.getPrimaryClip().getItemCount() > 0
                    && clipboard.getPrimaryClipDescription().hasMimeType(
                    android.content.ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                return clipboard.getPrimaryClip()
                        .getItemAt(0).getText().toString();
            }
        } else {
            // Android API level 10.
            android.text.ClipboardManager clipboard =
                    (android.text.ClipboardManager)
                            context.getSystemService(
                                    Context.CLIPBOARD_SERVICE);
            if (clipboard.hasText()) {
                return clipboard.getText().toString();
            }
        }

        // Error.
        return null;
    }

    /**
     * Copy file.
     *
     * @param in  Input file (source).
     * @param out Output file (destination).
     * @throws java.io.IOException
     */
    public static void copyFile(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * Convert Dips to pixels.
     *
     * @param dp Dips.
     * @return Dips as px.
     */
    public static int dpToPx(int dp) {
        return (int) (dp * mScale + 0.5f);
    }

    /**
     * Get the current active (last detected) Tag.
     *
     * @return The current active Tag.
     * @see #mTag
     */
    public static Tag getTag() {
        return mTag;
    }

    /**
     * Set the new active Tag (and update {@link #mUID}).
     *
     * @param tag The new Tag.
     * @see #mTag
     * @see #mUID
     */
    public static void setTag(Tag tag) {
        mTag = tag;
        mUID = tag.getId();
    }

    /**
     * Get the App wide used NFC adapter.
     *
     * @return NFC adapter.
     */
    public static NfcAdapter getNfcAdapter() {
        return mNfcAdapter;
    }

    /**
     * Set the App wide used NFC adapter.
     *
     * @param nfcAdapter The NFC adapter that should be used.
     */
    public static void setNfcAdapter(NfcAdapter nfcAdapter) {
        mNfcAdapter = nfcAdapter;
    }

    /**
     * Get the UID of the current tag.
     *
     * @return The UID of the current tag.
     * @see #mUID
     */
    public static byte[] getUID() {
        return mUID;
    }

    /**
     * Get the version code.
     *
     * @return The version code.
     */
    public static String getVersionCode() {
        return mVersionCode;
    }


}