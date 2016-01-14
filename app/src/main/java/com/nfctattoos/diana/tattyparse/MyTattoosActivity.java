package com.nfctattoos.diana.tattyparse;

import android.app.ListActivity;
import android.app.ListFragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class MyTattoosActivity extends AppCompatActivity {

    private ParseUser user;
    private TextView optText;
    private ArrayList<Map<String, String>> tattooList;
    private SimpleAdapter adapter;
    private ListView myTatList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_tattoos);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        user = ParseUser.getCurrentUser();
        optText = (TextView) findViewById(R.id.list_text);
        myTatList = (ListView) findViewById(R.id.list);

        JSONArray tattooListJSON = user.getJSONArray("tattoos");
        if (tattooListJSON != null) {
            optText.setText("You have " + tattooListJSON.length() + " saved tattoo(s).");
            tattooList = new ArrayList<Map<String, String>>();
            Map<String, String> tattooMap = new HashMap<>();
            for (int i=0; i<tattooListJSON.length(); i++){
                String tatName = "[No Name]";
                String tatMsg = "[No Message]";
                try {
                    ParseQuery<ParseObject> nameQuery = ParseQuery.getQuery("Tattoo");
                    nameQuery.whereEqualTo("UID", tattooListJSON.getString(i));
                    try {
                        ParseObject tattoo = nameQuery.getFirst();
                        tatName = tattoo.getString("name");
                        tatMsg = tattoo.getString("message");
                    } catch (com.parse.ParseException e) {
                        e.printStackTrace();
                    }
                    tattooMap.put("name", tatName);
                    tattooMap.put("message", tatMsg);
                    tattooList.add(tattooMap);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            adapter = new SimpleAdapter(this, tattooList, android.R.layout.simple_list_item_2,
                    new String[] {"name", "message"}, new int[] {android.R.id.text1, android.R.id.text2});
            myTatList.setAdapter(adapter);
        }
        else {
            optText.setText("No Existing User Tattoos");
        }

    }
    //should be listview
    //contains list of imgbuttons describing tattoos which belong to current user
}
