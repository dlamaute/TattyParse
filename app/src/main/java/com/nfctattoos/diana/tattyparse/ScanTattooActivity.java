package com.nfctattoos.diana.tattyparse;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScanTattooActivity extends AppCompatActivity {

    private AlertDialog mEnableNfc;
    private boolean mResume = true;
    private Intent mOldIntent = null;
    private TextView tagID;
    private String tagIDVal;
    private TextView tatUsername;
    private EditText tatName;
    private EditText tatMsg;
    //private ImageButton tatImg;
    private Button saveBtn;

    private ParseUser user;
    private ParseFile image;

    private ParseObject tattoo;
    private JSONArray userTattoos;

    private boolean isOwner;
    private boolean tagExists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_tattoo);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        user = ParseUser.getCurrentUser();

        //in application, keep track of universal username obj,
        //only allow modification if your username matches the one belonging to the tattoo

        //display found id on screen
        tagID = (TextView) findViewById(R.id.nfc_screen_id_text);

        //fetch username from backendless and replace
        tatUsername = (TextView) findViewById(R.id.username_text);

        //fetch tatname and replace
        tatName = (EditText) findViewById(R.id.tattoo_name_edit);

        //fetch tatmsg and replace
        tatMsg = (EditText) findViewById(R.id.tattoo_message_edit);

        //fetch tatimg, also change default img to neutral one
        //tatImg = (ImageButton) findViewById(R.id.tattoo_image);
        //tatImg.setEnabled(false);

        //give backendless command to take these data
        saveBtn = (Button) findViewById(R.id.save_btn);


        // Check if there is an NFC hardware component.
        Common.setNfcAdapter(NfcAdapter.getDefaultAdapter(this));
        if (Common.getNfcAdapter() == null) {
            Toast.makeText(this,"No NFC Adapter Available",Toast.LENGTH_LONG).show();
            mResume = false;
        }

        // Create a dialog that send user to NFC settings if NFC is off.
        // (Or let the user use the App in editor only mode / exit the App.)
        mEnableNfc = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_nfc_not_enabled_title)
                .setMessage(R.string.dialog_nfc_not_enabled)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.action_nfc,
                        new DialogInterface.OnClickListener() {
                            @Override
                            @SuppressLint("InlinedApi")
                            public void onClick(DialogInterface dialog, int which) {
                                // Goto NFC Settings.
                                if (Build.VERSION.SDK_INT >= 16) {
                                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                                } else {
                                    startActivity(new Intent(
                                            Settings.ACTION_WIRELESS_SETTINGS));
                                }
                            }
                        })
                .setNegativeButton(R.string.action_exit_app,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Exit the App.
                                finish();
                            }
                        }).create();

        // Check if there is Mifare Classic support.
        if (!Common.hasMifareClassicSupport()) {
            // Disable read/write tag options.
            CharSequence styledText = Html.fromHtml(
                    getString(R.string.dialog_no_mfc_support_device));
            AlertDialog ad = new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_no_mfc_support_device_title)
                    .setMessage(styledText)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.action_exit_app,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .setNegativeButton(R.string.action_continue,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    mResume = true;
                                    checkNfc();
                                }
                            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .show();
            // Make links clickable.
            ((TextView)ad.findViewById(android.R.id.message)).setMovementMethod(
                    LinkMovementMethod.getInstance());
            mResume = false;
        }
    }

    public void chooseNewImage(View v) {
        if (isOwner) {
            //make this an option between taking photo or choosing photo from library
            AlertDialog ad = new AlertDialog.Builder(this)
                    .setTitle(R.string.change_image)
                    .setPositiveButton(R.string.new_photo,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent takePictureIntent = new Intent(
                                            MediaStore.ACTION_IMAGE_CAPTURE);
                                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                                        startActivityForResult(takePictureIntent, 1);
                                    }
                                }
                            })
                    .setNegativeButton(R.string.existing_photo,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent getPictureIntent = new Intent();
                                    getPictureIntent.setType("image/*");
                                    getPictureIntent.setAction(Intent.ACTION_GET_CONTENT);
                                    startActivityForResult(Intent
                                            .createChooser(getPictureIntent, "Select Image"), 2);
                                }
                            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .show();
        }
    }

    public void saveTattoo(View v) {
        tattoo.put("UID", tagIDVal);
        tattoo.put("name", tatName.getText().toString());
        tattoo.put("message", tatMsg.getText().toString());
        //tattoo.put("image", image);
        tattoo.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("save", "tattoo saved");
                    Toast.makeText(ScanTattooActivity.this, "Tattoo saved.", Toast.LENGTH_LONG);
                }
                else {
                    Log.d("save", "tattoo did not save");
                    Toast.makeText(ScanTattooActivity.this, "Problem updating user", Toast.LENGTH_LONG);
                }
            }
        });

        user.put("tattoos", userTattoos);

        user.saveInBackground(new SaveCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("save", "user saved");
                    Toast.makeText(ScanTattooActivity.this, "Tattoo saved.", Toast.LENGTH_LONG);
                }
                else {
                    Log.d("save", "user is the worst");
                    Toast.makeText(ScanTattooActivity.this, "Problem updating user", Toast.LENGTH_LONG);
                }
            }
        });
    }

    /**
     * If resuming is allowed because all dependencies from
     * {@link #onCreate(Bundle)} are satisfied, call
     * {@link #checkNfc()}
     * @see #onCreate(Bundle)
     * @see #checkNfc()
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mResume) {
            checkNfc();
        }
    }

    /**
     * Check if NFC adapter is enabled. If not, show the user a dialog and let
     * him choose between "Goto NFC Setting", "Use Editor Only" and "Exit App".
     * Also enable NFC foreground dispatch system.
     * @see Common#enableNfcForegroundDispatch(Activity)
     */
    private void checkNfc() {
        // Check if the NFC hardware is enabled.
        if (Common.getNfcAdapter() != null
                && !Common.getNfcAdapter().isEnabled()) {
            // NFC is disabled. Show dialog.
            // Use as editor only?
            mEnableNfc.show();
        } else {
            // NFC is enabled. Hide dialog and enable NFC
            // foreground dispatch.
            if (mOldIntent != getIntent()) {
                String typeCheck = Common.treatAsNewTag(getIntent(), this);
                tagID.setText("UID: " + typeCheck);
                mOldIntent = getIntent();
            }
            Common.enableNfcForegroundDispatch(this);
            mEnableNfc.hide();
        }
    }


    /**
     * Disable NFC foreground dispatch system.
     * @see Common#disableNfcForegroundDispatch(Activity)
     */
    @Override
    public void onPause() {
        super.onPause();

        Common.disableNfcForegroundDispatch(this);
    }

    /**
     * Handle new Intent as a new tag Intent and if the tag/device does not
     * support Mifare Classic, tell it you suck
     * @see Common#treatAsNewTag(Intent, android.content.Context)
     */
    @Override
    public void onNewIntent(Intent intent) {
        // get UID
        tagIDVal = Common.treatAsNewTag(intent, this);
        tagID.setText("UID: " + tagIDVal);

        // determine ownership
        isOwner = false;
        tagExists = false;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Tattoo");
        query.whereEqualTo("UID", tagIDVal);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject result, ParseException e) {
                if (e == null) {
                    if (result == null) {
                        isOwner = true;
                        tagExists = false;
                        tattoo.put("UID", tagIDVal);
                        tattoo.put("owner", user.getUsername());

                        tatUsername.setText("Username: " + user.getUsername());
                        tatName.setText("");
                        tatMsg.setText("");
                    }
                    else {
                        tattoo = result;
                        tagExists = (tattoo.getString("owner") != null);
                        isOwner = (user.getUsername().equals(tattoo.getString("owner")));

                        if (!isOwner & tagExists) {
                            tatUsername.setText("Username: " + tattoo.getString("owner"));
                            tatName.setText(tattoo.getString("name"));
                            tatMsg.setText(tattoo.getString("message"));

                            tatName.setEnabled(false);
                            tatName.setFocusable(false);
                            tatName.setBackgroundColor(0x0FFFFFFF);
                            tatMsg.setEnabled(false);
                            tatMsg.setFocusable(false);
                            tatMsg.setBackgroundColor(0x0FFFFFFF);
                            //tatImg.setEnabled(false);
                            saveBtn.setEnabled(false);
                            saveBtn.setVisibility(View.GONE);

                        } else if (!tagExists) {
                            tattoo = new ParseObject("Tattoo");
                            tattoo.put("UID", tagIDVal);
                            tattoo.put("owner", user.getUsername());
                            userTattoos = user.getJSONArray("tattoos");
                            boolean tattooExists = userTattoos.toString().contains(", " + tattoo.get("UID").toString());
                            if (!tattooExists) {
                                userTattoos.put(tattoo.get("UID").toString());
                            }

                            tatUsername.setText("Username: " + user.getUsername());
                            tatName.getText().clear();
                            tatMsg.getText().clear();

                            tatName.setEnabled(true);
                            tatName.setFocusable(true);
                            tatName.setBackgroundColor(0x0F0F0F0F);
                            tatMsg.setEnabled(true);
                            tatMsg.setFocusable(true);
                            tatMsg.setBackgroundColor(0x0F0F0F0F);
                            //tatImg.setEnabled(true);
                            saveBtn.setEnabled(true);
                            saveBtn.setVisibility(View.VISIBLE);
                        } else {
                            userTattoos = user.getJSONArray("tattoos");
                            boolean tattooExists = userTattoos.toString()
                                    .contains("\"" + tattoo.getString("UID") + "\"");
                            if (!tattooExists) {
                                userTattoos.put(tattoo.getString("UID"));
                            }

                            tatUsername.setText("Username: " + user.getUsername());
                            tatName.setText(tattoo.getString("name"));
                            tatMsg.setText(tattoo.getString("message"));

                            tatName.setEnabled(true);
                            tatName.setFocusable(true);
                            tatName.setBackgroundColor(0x0F0F0F0F);
                            tatMsg.setEnabled(true);
                            tatMsg.setFocusable(true);
                            tatMsg.setBackgroundColor(0x0F0F0F0F);
                            //tatImg.setEnabled(true);
                            saveBtn.setEnabled(true);
                            saveBtn.setVisibility(View.VISIBLE);
                        }
                    }
                }
                else {
                    isOwner = true;
                    tagExists = false;
                    tattoo = new ParseObject("Tattoo");
                    tattoo.put("UID", tagIDVal);
                    tattoo.put("owner", user.getUsername());
                    userTattoos = user.getJSONArray("tattoos");
                    boolean tattooExists = userTattoos.toString().contains(", " + tattoo.get("UID").toString());
                    if (!tattooExists) {
                        userTattoos.put(tattoo.get("UID").toString());
                    }

                    tatUsername.setText("Username: " + user.getEmail());
                    tatName.getText().clear();
                    tatMsg.getText().clear();

                    tatName.setEnabled(true);
                    tatName.setFocusable(true);
                    tatName.setBackgroundColor(0x0F0F0F0F);
                    tatMsg.setEnabled(true);
                    tatMsg.setFocusable(true);
                    tatMsg.setBackgroundColor(0x0F0F0F0F);
                    //tatImg.setEnabled(true);
                    saveBtn.setEnabled(true);
                    saveBtn.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    //Enable when parse stops being stupid about uploading parsefiles
    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imgBit = (Bitmap) extras.get("data");
            tatImg.setImageBitmap(imgBit);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imgBit.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            if (byteArray != null) {
                image = new ParseFile(tatName.toString() + ".jpg", byteArray);
                try {
                    image.save();
                } catch (ParseException e) {
                    if (e == null) {
                        tattoo.put("image", image);
                        Toast.makeText(getApplicationContext(), "Saved picture.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Could not save picture.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }

        else if (requestCode == 2 && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();
            try{
                Bitmap imgBit = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                tatImg.setImageBitmap(imgBit);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                imgBit.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                if (byteArray != null) {
                    image = new ParseFile(tatName.toString() + ".jpg", byteArray);
                    try {
                        Toast.makeText(getApplicationContext(), "Saving Image", Toast.LENGTH_LONG).show();
                        image.save();
                    } catch (ParseException e) {
                        if (e == null) {
                            tattoo.put("image", image);
                            Toast.makeText(getApplicationContext(), "Saved picture.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Could not save picture.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }

    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
