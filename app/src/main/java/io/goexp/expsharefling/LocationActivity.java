package io.goexp.expsharefling;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.scala.exp.android.sdk.Exp;
import com.scala.exp.android.sdk.channels.IChannel;
import com.scala.exp.android.sdk.model.Location;
import com.scala.exp.android.sdk.model.SearchResults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Subscriber;

/**
 * Created by Cesar Oyarzun on 1/3/17.
 */
public class LocationActivity extends AppCompatActivity {

    public static final String CONTENT = "content";
    private final String LOG_TAG = LocationActivity.class.getSimpleName();
    private ListView listView;
    private List<Location> listLocation=new ArrayList<>();
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.url = this.getIntent().getStringExtra(MainActivity.URL);
        setContentView(R.layout.location_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.list_location);

        final LocationAdapter locationAdapter = new LocationAdapter(this,listLocation);
        listView.setAdapter(locationAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition     = position;
                Location itemValue    = (Location) listView.getItemAtPosition(position);
                IChannel channel = itemValue.getChannel();
                Map<String, Object> payload = new HashMap<String, Object>();
                payload.put(CONTENT, url);
                channel.fling(payload);
                Toast.makeText(getBaseContext(), "Fling Completed!", Toast.LENGTH_LONG).show();
            }

        });


        Map<String,Object> options = new HashMap<>();
        options.put("limit","1000");
        options.put("skip", "0");
        options.put("sort", "asc");
        Exp.findLocations(options)
                .then(new Subscriber<SearchResults<Location>>() {
                    @Override
                    public void onCompleted() {

                    }
                    @Override
                    public void onError(Throwable e) {
                        Log.e("error", e.toString());
                    }
                    @Override
                    public void onNext(SearchResults<Location> resultLocation) {
                        Log.i("Response", resultLocation.toString());
                        listLocation = resultLocation.getResults();
                        locationAdapter.addAll(resultLocation.getResults());
                        locationAdapter.notifyDataSetChanged();
                    }
                });
    }
}
