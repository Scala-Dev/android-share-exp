package io.goexp.expsharefling;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.scala.exp.android.sdk.AppSingleton;
import com.scala.exp.android.sdk.Exp;
import com.scala.exp.android.sdk.channels.IChannel;
import com.scala.exp.android.sdk.model.Auth;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import rx.Subscriber;

import static io.goexp.expsharefling.MainActivity.EXP_PREF;

/**
 * Created by Cesar Oyarzun on 1/4/17.
 */
public class OptionsActivity extends AppCompatActivity {

    public static final String CONTENT = "content";
    private final String LOG_TAG = OptionsActivity.class.getSimpleName();
    private final static String organization = "Organization";
    private final static String location = "Location";
    private String url;
    private ListView listView ;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(MainActivity.EXP_PREF, Context.MODE_PRIVATE);
        final String org = preferences.getString(MainActivity.EXP_ORGANIZATION, "");
        String[] shareOptions = {organization+" ("+org+")",location};
        setContentView(R.layout.activity_main);
        this.url=this.getIntent().getStringExtra(MainActivity.URL);
        ArrayAdapter adapter = new ArrayAdapter<String>(this,
                R.layout.textview, shareOptions);
        listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition     = position;
                String  itemValue    = (String) listView.getItemAtPosition(position);
                if(url == null || url.isEmpty()){
                    Toast.makeText(getBaseContext(), "You need to select an URL to share!", Toast.LENGTH_LONG).show();
                }else {
                    if (itemPosition == 0) {
                        //fling to the organization
                        IChannel channel = Exp.getChannel("organization", false, false);
                        Map<String, Object> payload = new HashMap<String, Object>();
                        payload.put(CONTENT, url);
                        channel.fling(payload);
                        Toast.makeText(getBaseContext(), "Fling Completed!", Toast.LENGTH_LONG).show();
                    } else {
                        //start location activity
                        Intent i = new Intent(getApplicationContext(), LocationActivity.class);
                        i.putExtra(MainActivity.URL, url);
                        startActivity(i);
                    }
                }
            }
        });

        // subscriber to handle the refresh token
        Subscriber updateSubscriber = new Subscriber<Boolean>() {
            @Override
            public void onCompleted() {}
            @Override
            public void onError(Throwable e) {Log.e(LOG_TAG,"...ERROR REFRESH TOKEN...",e);}
            @Override
            public void onNext(Boolean o) {
                Auth auth = AppSingleton.getInstance().getAuth();
                BigInteger expiration = auth.getExpiration();
                String token = auth.getToken();
                String organization = auth.getIdentity().getOrganization();
                SharedPreferences.Editor edit = preferences.edit();
                edit.putString(MainActivity.EXP_TOKEN, token);
                edit.putString(MainActivity.EXP_TOKEN_EXPIRATION, expiration.toString());
                edit.putString(MainActivity.EXP_ORGANIZATION,organization);
                edit.commit();
                Log.d(LOG_TAG, "...REFRESH TOKEN...");
                restartSDK(auth);
            }
        };
        Exp.on("update", updateSubscriber);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.logout){
            SharedPreferences.Editor edit = getSharedPreferences(EXP_PREF,Context.MODE_PRIVATE).edit();
            edit.putString(MainActivity.EXP_TOKEN,"");
            edit.putString(MainActivity.EXP_TOKEN_EXPIRATION,"");
            edit.putString(MainActivity.EXP_ORGANIZATION,"");
            edit.commit();
            Intent intent = new Intent(OptionsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }else if(id == R.id.organizations){
            Intent intent = new Intent(OptionsActivity.this,
                    OrganizationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void restartSDK(Auth auth){
        //start with last auth
        Exp.start(auth.getApi().getHost(), auth).subscribe(new Subscriber<Boolean>() {
            @Override
            public void onCompleted() {}
            @Override
            public void onError(Throwable e) {Log.e(LOG_TAG, "...SDK RESTART ERROR...", e);}
            @Override
            public void onNext(Boolean aBoolean) {
                Log.d(LOG_TAG,"...SDK RESTARTED...");
            }
        });
    }
}
