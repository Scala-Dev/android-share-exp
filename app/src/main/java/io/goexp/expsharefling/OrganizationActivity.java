package io.goexp.expsharefling;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.gson.internal.LinkedTreeMap;
import com.scala.exp.android.sdk.Exp;
import com.scala.exp.android.sdk.channels.IChannel;
import com.scala.exp.android.sdk.model.Auth;
import com.scala.exp.android.sdk.model.Location;
import com.scala.exp.android.sdk.model.SearchResults;
import com.scala.exp.android.sdk.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Subscriber;

/**
 * Created by Cesar Oyarzun on 1/3/17.
 */
public class OrganizationActivity extends AppCompatActivity {

    public static final String ORG = "org";
    public static final String NAME = "name";
    public static final String ORGANIZATIONS = "organizations";
    private final String LOG_TAG = OrganizationActivity.class.getSimpleName();
    private ListView listView;
    private List<LinkedTreeMap> listLocation=new ArrayList<>();
    private String url;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.url = this.getIntent().getStringExtra(MainActivity.URL);
        setContentView(R.layout.organization_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        preferences = getSharedPreferences(MainActivity.EXP_PREF, Context.MODE_PRIVATE);

        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.list_organization);

        final OrganizationAdapter organizationAdapter = new OrganizationAdapter(this,listLocation);
        listView.setAdapter(organizationAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition     = position;
                LinkedTreeMap itemValue    = (LinkedTreeMap) listView.getItemAtPosition(position);
                HashMap map=new HashMap();
                map.put(ORG,itemValue.get(NAME));
                Exp.getToken(map).then(new Subscriber<Auth>() {
                    @Override
                    public void onCompleted() {}
                    @Override
                    public void onError(Throwable e) {Log.e(LOG_TAG,"...GET TOKEN ERROR...",e);}
                    @Override
                    public void onNext(Auth auth) {
                        SharedPreferences.Editor edit = preferences.edit();
                        edit.putString(MainActivity.EXP_TOKEN, auth.getToken());
                        edit.putString(MainActivity.EXP_TOKEN_EXPIRATION, auth.getExpiration().toString());
                        edit.putString(MainActivity.EXP_ORGANIZATION,auth.getIdentity().getOrganization());
                        edit.commit();
                        Intent intent = new Intent(OrganizationActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                });

            }

        });

        Exp.getCurrentUser()
                .then(new Subscriber<User>() {
                    @Override
                    public void onCompleted() {}
                    @Override
                    public void onError(Throwable e) {Log.e(LOG_TAG, "...GET CURRENT USER ERROR...",e);}
                    @Override
                    public void onNext(User user) {
                        Log.d(LOG_TAG, user.toString());
                        listLocation = (List<LinkedTreeMap>) user.get(ORGANIZATIONS);
                        organizationAdapter.addAll(listLocation);
                        organizationAdapter.notifyDataSetChanged();
                    }
                });
    }
}
