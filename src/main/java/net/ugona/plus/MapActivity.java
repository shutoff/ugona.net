package net.ugona.plus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;

import org.osmdroid.api.IMapController;

public abstract class MapActivity extends ActionBarActivity {

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.webview);
        FragmentManager fm = this.getSupportFragmentManager();
        MapFragment mapFragment = new MapFragment();
        fm.beginTransaction().add(R.id.webview, mapFragment).commit();

    }

    abstract void initMap(IMapController controller);
}
