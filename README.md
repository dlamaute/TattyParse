# TattyParse
Parse backend version of Tatty, an Android app for interacting with NFC tattoos.

There are 4 screens:

1. LoginActivity
-can login or sign up on this screen.

2. CoreActivity
-lets the user choose between going to (3.) or (4.)

3. ScanTattooActivity
-Can scan a tattoo, then see:
-its UID (unique identifier number of the given tattoo)
-its current owner (if it has none, then the app automatically claims it for you)
-a name and message given to it--these are modifiable if the tattoo belongs to you.

4. MyTattoosActivity
-a list pulled from the database of your tattoos, which are represented as:
_________________
tattooName
tattooMessage
-----------------

Future capabilities:

LoginActivity: forgot password option*, auto complete username*, sign in with Facebook*
CoreActivity: logout
ScanTattooActivity: save & change images
MyTattoosActivity: click on list item; see date created, UID

*if desired
