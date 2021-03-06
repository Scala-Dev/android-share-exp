package io.goexp.expsharefling;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.scala.exp.android.sdk.Exp;
import com.scala.exp.android.sdk.Utils;
import com.scala.exp.android.sdk.model.Auth;
import com.scala.exp.android.sdk.model.Identity;
import com.scala.exp.android.sdk.model.User;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import rx.Subscriber;

public class MainActivity extends AppCompatActivity {

    public static final String URL = "URL";
    public static final String EXP_PREF = "expPref";
    public static final String EXP_TOKEN = "exp_token";
    public static final String EXP_TOKEN_EXPIRATION = "exp_token_expiration";
    public static final String EXP_ORGANIZATION = "organization";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String USERNAME = "username";
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    //    public static final String host = "https://api.goexp.io";
    public static final String host = "https://api-staging.goexp.io";
    private SharedPreferences preferences;
    private String url;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createProgressDialog();
        preferences = getSharedPreferences(EXP_PREF, Context.MODE_PRIVATE);
        final String exp_token = preferences.getString(EXP_TOKEN, "");
        final String expiration = preferences.getString(EXP_TOKEN_EXPIRATION, "");
        final String organization = preferences.getString(EXP_ORGANIZATION, "");
        Log.d(LOG_TAG, "...ONCREATE...");
        if (!exp_token.isEmpty() && !expiration.isEmpty() && !organization.isEmpty()) {
            //check if token expired
            this.progressDialog.show();
            Exp.getUser(host, exp_token).subscribe(new Subscriber<User>() {
                @Override
                public void onCompleted() {}
                @Override
                public void onError(Throwable e) {
                    Log.d(LOG_TAG, "...SDK TOKEN INVALID...", e);
                    showLogin();
                    progressDialog.dismiss();
                }
                @Override
                public void onNext(User user) {
                    //start with last auth
                    Auth auth = createAuth(organization, exp_token, expiration);
                    Exp.start(host, auth).subscribe(new Subscriber<Boolean>() {
                        @Override
                        public void onCompleted() {}
                        @Override
                        public void onError(Throwable e) {
                            progressDialog.dismiss();
                            Log.e(LOG_TAG, "...SDK ERROR START WITH AUTH...", e);
                        }
                        @Override
                        public void onNext(Boolean aBoolean) {
                            progressDialog.dismiss();
                            launchOptions();
                        }
                    });
                }
            });
        } else {
            showLogin();
        }

        // Get intent  url from other app
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (TEXT_PLAIN.equals(type)) {
                handleSendText(intent);
            }
        }


    }

    /**
     * Launch Option activity
     */
    private void launchOptions() {
        Intent intent = new Intent(getApplicationContext(), OptionsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(URL,url);
        startActivity(intent);
        this.finish();
    }

    /**
     * Create Auth object from share preferences
     * @param organization
     * @param exp_token
     * @param expiration
     * @return
     */
    @NonNull
    private Auth createAuth(String organization, String exp_token, String expiration) {
        Identity ident = new Identity();
        ident.setOrganization(organization);
        Auth auth = new Auth();
        auth.setToken(exp_token);
        auth.setIdentity(ident);
        auth.setExpiration(BigInteger.valueOf(Long.parseLong(expiration)));
        return auth;
    }

    /**
     * Show login page
     */
    private void showLogin() {
        setContentView(R.layout.login);
        final TextView userText = (TextView) findViewById(R.id.textUsername);
        final TextView passText = (TextView) findViewById(R.id.textPassword);
        String userName = preferences.getString(USERNAME, "");
        userText.setText(userName);
        Button button = (Button) findViewById(R.id.buttonLogin);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login(userText, passText);
            }
        });
    }

    /**
     * Login into exp
     * @param userText
     * @param passText
     */
    private void login(TextView userText, TextView passText) {
        progressDialog.show();
        final String user = userText.getText().toString();
        final String pass = passText.getText().toString();
        if (!user.isEmpty() && !pass.isEmpty()) {
            final Map<String, Object> startOptions = new HashMap<>();
            startOptions.put(Utils.HOST, host);
            startOptions.put(Utils.USERNAME, user);
            startOptions.put(Utils.PASSWORD, pass);
            startOptions.put(Utils.ORGANIZATION, "");
            startOptions.put(Utils.ENABLE_EVENTS, true);

            Exp.start(startOptions).subscribe(new Subscriber<Boolean>() {
                @Override
                public void onCompleted() {
                    Log.i(LOG_TAG, "...START EXP SDK COMPLETED...");
                    progressDialog.dismiss();
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(LOG_TAG, "...SDK ERROR START...", e);
                    Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                }

                @Override
                public void onNext(final Boolean aBoolean) {
                    //set logged
                    SharedPreferences.Editor edit = preferences.edit();
                    edit.putString(EXP_TOKEN, Exp.getAuth().getToken());
                    edit.putString(EXP_TOKEN_EXPIRATION, Exp.getAuth().getExpiration().toString());
                    edit.putString(EXP_ORGANIZATION,Exp.getAuth().getIdentity().getOrganization());
                    edit.putString(USERNAME,user);
                    edit.commit();
                    launchOptions();
                    progressDialog.dismiss();
                }
            });
        } else {
            Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();
            progressDialog.dismiss();
        }
    }

    @NonNull
    private ProgressDialog createProgressDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(MainActivity.this);
        }
        progressDialog.setMessage("Loading.....");
        return progressDialog;
    }



    /**
     * Handel the url send to the app
     * @param intent
     */
    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            this.url = sharedText;
        }
    }
}
