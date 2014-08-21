package net.ugona.plus;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.util.Date;

abstract public class MapActivity extends WebViewActivity {

    static final String TRAFFIC = "traffic";

    static final int TWO_MINUTES = 1000 * 60 * 2;
    Menu topSubMenu;
    SharedPreferences preferences;
    LocationManager locationManager;
    Location currentBestLocation;
    LocationListener netListener;
    LocationListener gpsListener;

    abstract int menuId();

    abstract JsInterface js();

    @Override
    String loadURL() {
        webView.addJavascriptInterface(js(), "android");
        return getUrl();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        currentBestLocation = getLastBestLocation();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStop() {
        stopListener();
        super.onStop();
    }

    @Override
    protected void onStart() {
        startListener();
        super.onStart();
    }

    String getUrl() {
        if (preferences.getString("map_type", "").equals("OSM"))
            return "file:///android_asset/html/osm.html";
        return "file:///android_asset/html/google.html";
    }

    void startListener() {
        if (netListener != null)
            locationManager.removeUpdates(netListener);
        if (gpsListener != null)
            locationManager.removeUpdates(gpsListener);
    }

    void stopListener() {
        netListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationChanged(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, netListener);
        } catch (Exception ex) {
            netListener = null;
        }

        if (preferences.getBoolean(Names.USE_GPS, false)) {
            gpsListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    locationChanged(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };

            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, gpsListener);
            } catch (Exception ex) {
                gpsListener = null;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        topSubMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId(), menu);
        if (preferences.getString("map_type", "OSM").equals("OSM")) {
            menu.findItem(R.id.osm).setChecked(true);
        } else {
            menu.findItem(R.id.google).setChecked(true);
        }
        MenuItem item = menu.findItem(R.id.traffic);
        if (item != null)
            item.setChecked(preferences.getBoolean(TRAFFIC, true));
        item = menu.findItem(R.id.traffic_layer);
        if (item != null)
            item.setChecked(preferences.getBoolean(Names.SHOW_TRAFFIC, false));
        item = menu.findItem(R.id.gps);
        if (item != null)
            item.setChecked(preferences.getBoolean(Names.USE_GPS, true));
        return super.onCreateOptionsMenu(menu);
    }

    void updateMenu() {
        topSubMenu.clear();
        onCreateOptionsMenu(topSubMenu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.map:
                webView.loadUrl("javascript:center()");
                break;
            case R.id.my: {
                boolean gps_enabled = false;
                try {
                    gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                } catch (Exception ex) {
                    // ignore
                }
                if (!gps_enabled) {
                    AlertDialog.Builder ad = new AlertDialog.Builder(this);
                    ad.setTitle(R.string.no_gps_title);
                    ad.setMessage((currentBestLocation == null) ? R.string.no_gps_message : R.string.net_gps_message);
                    ad.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    });
                    ad.setNegativeButton(R.string.cancel, null);
                    ad.show();
                    if (currentBestLocation == null)
                        return true;
                } else if (currentBestLocation == null) {
                    Toast toast = Toast.makeText(this, R.string.no_location, Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                }
                webView.loadUrl("javascript:setPosition()");
                break;
            }
            case R.id.google: {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.MAP_TYPE, "Google");
                ed.commit();
                updateMenu();
                webView.loadUrl(getUrl());
                break;
            }
            case R.id.osm: {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.MAP_TYPE, "OSM");
                ed.commit();
                updateMenu();
                webView.loadUrl(getUrl());
                break;
            }
            case R.id.traffic_layer: {
                boolean traffic = !preferences.getBoolean(Names.SHOW_TRAFFIC, false);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(Names.SHOW_TRAFFIC, traffic);
                ed.commit();
                updateMenu();
                webView.loadUrl("javascript:showTraffic()");
                break;
            }
            case R.id.gps: {
                boolean gps = !preferences.getBoolean(Names.USE_GPS, false);
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(Names.USE_GPS, gps);
                ed.commit();
                updateMenu();
                stopListener();
                startListener();
                break;
            }
        }
        return false;
    }

    Location getLastBestLocation() {
        Location locationGPS = null;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            // ignore
        }
        Location locationNet = null;
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            // ignore
        }
        long GPSLocationTime = 0;
        Date now = new Date();
        if (locationGPS != null) {
            GPSLocationTime = locationGPS.getTime();
            if (GPSLocationTime < now.getTime() - TWO_MINUTES) {
                locationGPS = null;
                GPSLocationTime = 0;
            }
        }
        long NetLocationTime = 0;
        if (locationNet != null) {
            NetLocationTime = locationNet.getTime();
            if (NetLocationTime < now.getTime() - TWO_MINUTES) {
                locationNet = null;
                NetLocationTime = 0;
            }
        }
        if (GPSLocationTime > NetLocationTime)
            return locationGPS;
        return locationNet;
    }

    public void locationChanged(Location location) {
        if (isBetterLocation(location, currentBestLocation))
            currentBestLocation = location;
        if (currentBestLocation == null)
            return;
        if (loaded)
            webView.loadUrl("javascript:myLocation()");
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null)
            return true;

        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null)
            return provider2 == null;
        return provider1.equals(provider2);
    }

    class JsInterface {

        @JavascriptInterface
        public String init() {
            loaded = true;
            return "";
        }

        @JavascriptInterface
        public String getLocation() {
            if (currentBestLocation == null)
                return "";
            String res = currentBestLocation.getLatitude() + ",";
            res += currentBestLocation.getLongitude() + ",";
            res += currentBestLocation.getAccuracy();
            if (currentBestLocation.hasBearing())
                res += currentBestLocation.getBearing();
            return res;
        }

        @JavascriptInterface
        public String kmh() {
            return getString(R.string.kmh);
        }

        @JavascriptInterface
        public String traffic() {
            return preferences.getBoolean(TRAFFIC, true) ? "1" : "";
        }
    }

}
