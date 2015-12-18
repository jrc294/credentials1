package com.learning.jonathan.credentials1;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    GoogleApiClient mGoogleApiClient;
    SignInButton btnSignIn;
    Button btnSignOut;
    Button btnRevoke;
    TextView txtStatus;

    int mSignInProgress;

    final static String TAG = MainActivity.class.getSimpleName();
    final static int STATE_SIGNED_IN = 0;
    final static int STATE_SIGN_IN = 1;
    final static int STATE_PROGRESS = 2;

    private static final int RC_SIGN_IN = 0;
    private static final int DIALOG_PLAY_SERVICES_ERROR = 0;

    PendingIntent mSignInIntent;
    int mSignInError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        buildClient();

        btnSignIn = (SignInButton) findViewById(R.id.btnSignIn);
        btnSignOut = (Button) findViewById(R.id.btnSignOut);
        btnRevoke = (Button) findViewById(R.id.btnRevokeAccess);
        txtStatus = (TextView) findViewById(R.id.txtStatus);

        btnSignIn.setOnClickListener(this);
        btnSignOut.setOnClickListener(this);
        btnRevoke.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected");

        mSignInProgress = STATE_SIGNED_IN;
        btnSignIn.setEnabled(false);
        btnRevoke.setEnabled(true);
        btnSignOut.setEnabled(true);

        Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        if (person != null) {
            txtStatus.setText(String.format("Signed into G+ as %s", person.getDisplayName()));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

        Log.e(TAG, "Connection suspected cause " + i);
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.d(TAG, "onConnectionFailed");
        if (mSignInProgress != STATE_PROGRESS) {
            // If we are SIGNED_OUT or SIGNED_IN and are reconnecting the client...
            //
            // We do not have an intent in progress so we should store the latest
            // error resolution intent for use when the sign in button is clicked
            mSignInIntent = connectionResult.getResolution();
            mSignInError = connectionResult.getErrorCode();
            if (mSignInProgress == STATE_SIGN_IN) {
                Log.d(TAG, "onConnectionFailed STATE_SIGN_IN");
                // STATE_SIGN_IN indicates the user already clicked the sign in
                // so we should continue processing errors until the user is signed in
                // or they click cancel
                resolveSignInError();
            }
        }

        onSignedOut();
    }

    private void buildClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(new Scope(Scopes.PROFILE))
                //.addApi(Plus.API)
                //.addScope(Plus.SCOPE_PLUS_PROFILE)
                //.addScope(Plus.SCOPE_PLUS_LOGIN)*/
                .build();
    }

    @Override
    public void onClick(View view) {
        if (!mGoogleApiClient.isConnecting()) {
            // We only process button clicks with GoogleApiClient is not transitioning
            // between connected and not connected
            switch (view.getId()) {
                case R.id.btnSignIn:
                    txtStatus.setText("Signing in");
                    resolveSignInError();
                    break;
                case R.id.btnSignOut:
                    // We clear the default account on sign out so that Google
                    // services will not return an onConnected callback without
                    // interaction
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                    mGoogleApiClient.connect();
                    break;
                case R.id.btnRevokeAccess:
                    // After we revoke permissions for the user with a GoogleApiClient
                    // instance, we must discard it and create a new one
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    // Our sample has caches no user data from Google+, however
                    // would normally register a callback to revokeAccessAndDisable
                    // to delete user data so that we comply with Google developer
                    // policies
                    Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                    buildClient();
                    mGoogleApiClient.connect();
                    break;
            }
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private void onSignedOut() {
        Log.d(TAG, "onSignedOut");
        // Update the UI to reflect that the user is signed out
        btnSignIn.setEnabled(true);
        btnSignOut.setEnabled(false);
        btnRevoke.setEnabled(false);
        txtStatus.setText("Signed Out");
    }

    private void resolveSignInError() {
        Log.d(TAG, "resolveSignInError");
        if (mSignInIntent != null) {
            // We have an intent which will allow our user to sign in or
            // resolve an error. For example if the user needs to
            // select an account to sign in with, or if they need to consent
            // to the permissions your app is requesting.

            try {
                // Send the pending intent that we stored on the most recent
                // OnConnectionFailed callback. this will allow the user to
                // resolve the error currently preventing our connection to
                // Google Play services
                mSignInProgress = STATE_PROGRESS;
                startIntentSenderForResult(mSignInIntent.getIntentSender(), RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                Log.i(TAG, "Sign in intent could not be sent: " + e.getLocalizedMessage());
                // The intent was cancelled before it was sent. Attempt to connect
                // get an updated ConnectionResult.
                mSignInProgress = STATE_SIGN_IN;
                mGoogleApiClient.connect();
            }
        } else {
            // Google Play Services wasn't able to provide an intent for some
            // error types. So we should show the default Google Play services error
            // dialog which may still start an intent on our behalf if the
            // user can resolve the issue.
            showDialog(DIALOG_PLAY_SERVICES_ERROR);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        switch(requestCode) {
            case RC_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    mSignInProgress = STATE_SIGN_IN;
                    Log.d(TAG, "onActivityResult STATE_SIGN_IN");
                } else {
                    mSignInProgress = STATE_SIGNED_IN;
                    Log.d(TAG, "onActivityResult STATE_SIGNED_IN");
                }

                if (!mGoogleApiClient.isConnecting()) {
                    mGoogleApiClient.connect();
                }
                break;
        }
    }
}
