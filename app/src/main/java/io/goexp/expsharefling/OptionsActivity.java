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

import com.scala.exp.android.sdk.Exp;
import com.scala.exp.android.sdk.channels.IChannel;
import com.scala.exp.android.sdk.model.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Cesar Oyarzun on 1/4/17.
 */
public class OptionsActivity extends AppCompatActivity {

    public static final String CONTENT = "content";

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private final static String organization = "Organization";
    private final static String location = "Location";
    String[] shareOptions = {organization,location};
    private String url;
    private ListView listView ;
    private List<Location> listLocation=new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    if(organization.equalsIgnoreCase(itemValue)){
                        IChannel channel = Exp.getChannel("organization",false,false);
                        Map<String, Object> payload = new HashMap<String, Object>();
                        payload.put(CONTENT, url);
                        channel.fling(payload);
                    }else{
                        //start location activity
                        Intent i = new Intent(getApplicationContext(),LocationActivity.class);
                        i.putExtra(MainActivity.URL,url);
                        startActivity(i);
                    }
            }
        });
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
            SharedPreferences.Editor edit = getSharedPreferences(MainActivity.EXP_PREF,Context.MODE_PRIVATE).edit();
            edit.putString(MainActivity.EXP_TOKEN,"");
            edit.putString(MainActivity.EXP_TOKEN_EXPIRATION,"");
            edit.putString(MainActivity.EXP_ORGANIZATION,"");
            edit.commit();
            Intent intent = new Intent(OptionsActivity.this,
                    MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
