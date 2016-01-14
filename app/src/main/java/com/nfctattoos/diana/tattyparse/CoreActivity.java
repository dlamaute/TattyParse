package com.nfctattoos.diana.tattyparse;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class CoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_core);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void goToScan(View v) {
        Intent scanNFCIntent = new Intent(this, ScanTattooActivity.class);
        startActivity(scanNFCIntent);
    }

    public void goToTattoos(View v) {
        Intent myTattoosIntent = new Intent(this, MyTattoosActivity.class);
        startActivity(myTattoosIntent);
    }

}
