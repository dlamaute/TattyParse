package com.nfctattoos.diana.tattyparse;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import org.json.JSONArray;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;

    private ParseUser user;

    private String username;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("uh", "signin");
                attemptLogin();
            }
        });

        Button mSignUpButton = (Button) findViewById(R.id.sign_up_button);
        mSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("uh", "signup");
                attemptSignUp();
            }
        });
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        // Store values at the time of the login attempt.
        username = mEmailView.getText().toString();
        password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            Toast.makeText(this, getString(R.string.error_invalid_password), Toast.LENGTH_LONG).show();
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, getString(R.string.error_field_required), Toast.LENGTH_LONG).show();
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(username)) {
            Toast.makeText(this, getString(R.string.error_invalid_email), Toast.LENGTH_LONG).show();
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            ParseUser confUser = ParseUser.getCurrentUser();
            if (confUser != null) {
                confUser.logOut();
            }
            ParseUser.logInInBackground(username, password, new LogInCallback() {
                public void done(ParseUser sessionUser, ParseException e) {
                    if (e == null && sessionUser != null) {
                        Intent intent = new Intent(LoginActivity.this, CoreActivity.class);
                        user = sessionUser;
                        startActivity(intent);
                    } else if (sessionUser == null) {
                        Log.d("login", "usrname/pass invalid");
                        Toast.makeText(getApplicationContext(), "Username or password is invalid", Toast.LENGTH_LONG).show();
                    } else {
                        Log.d("login", "problem getting user");
                        Toast.makeText(LoginActivity.this, "Problem geting user", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private void attemptSignUp() {
        // Store values at the time of the login attempt.
        username = mEmailView.getText().toString();
        password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(username)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(username)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // kick off a background task to perform the user login attempt.
            user = new ParseUser();
            user.setUsername(username);
            user.setPassword(password);
            JSONArray tattooList = new JSONArray();
            user.put("tattoos", tattooList);
            ParseUser confUser = ParseUser.getCurrentUser();
                if (confUser != null) {
                    confUser.logOut();
                }
            user.signUpInBackground(new SignUpCallback() {
                public void done(ParseException e) {
                    if (e == null) {
                        Log.d("signup", "it worked");
                        Intent intent = new Intent(LoginActivity.this, CoreActivity.class);
                        startActivity(intent);
                    } else {
                        Log.d("signup", "failed");
                        Toast.makeText(LoginActivity.this, "User already taken", Toast.LENGTH_LONG).show();
                    }
                }
            });

        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }
}
