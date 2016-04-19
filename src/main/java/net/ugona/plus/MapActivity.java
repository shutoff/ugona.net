package net.ugona.plus;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Locale;

abstract public class MapActivity extends WebViewActivity {

    static final int TWO_MINUTES = 1000 * 60 * 2;
    Menu topSubMenu;

    LocationManager locationManager;
    Location currentBestLocation;
    LocationListener netListener;
    LocationListener gpsListener;
    String language;
    boolean noLocation;
    AppConfig config;

    abstract int menuId();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        currentBestLocation = getLastBestLocation();
        language = Locale.getDefault().getLanguage();
        config = AppConfig.get(this);
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
        return "file:///android_asset/html/map.html";
    }

    void stopListener() {
        try {
            if (netListener != null)
                locationManager.removeUpdates(netListener);
            if (gpsListener != null)
                locationManager.removeUpdates(gpsListener);
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }
    }

    boolean requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startListener();
            return true;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                1);
        return true;
    }

    void startListener() {

        if (noLocation)
            return;

        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!granted) {
            requestPermissions();
            return;
        }


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

        if (config.isUse_gps()) {
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
        String map_type = config.getMap_type();
        if (map_type.equals("OSM")) {
            menu.findItem(R.id.osm).setChecked(true);
        } else if (map_type.equals("Yandex")) {
            menu.findItem(R.id.yandex).setChecked(true);
        } else if (map_type.equals("Bing")) {
            menu.findItem(R.id.bing).setChecked(true);
        } else {
            menu.findItem(R.id.google).setChecked(true);
        }
        MenuItem item = menu.findItem(R.id.show_traffic);
        if (item != null)
            item.setChecked(config.isShow_traffic());
        item = menu.findItem(R.id.gps);
        if (item != null)
            item.setChecked(config.isUse_gps());
        return super.onCreateOptionsMenu(menu);
    }

    void updateMenu() {
        topSubMenu.clear();
        onCreateOptionsMenu(topSubMenu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                share();
                break;
            case R.id.map:
                callJs("center()");
                break;
            case R.id.my: {
                if (config.isUse_gps()) {
                    boolean gps_enabled = false;
                    try {
                        gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    } catch (Exception ex) {
                        // ignore
                    }
                    if (!gps_enabled) {
                        AlertDialogWrapper.Builder ad = new AlertDialogWrapper.Builder(this);
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
                    }
                }
                if (currentBestLocation == null) {
                    Toast toast = Toast.makeText(this, R.string.no_location, Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                }
                callJs("setPosition()");
                break;
            }
            case R.id.google: {
                setMapType("Google");
                break;
            }
            case R.id.yandex: {
                setMapType("Yandex");
                break;
            }
            case R.id.bing: {
                setMapType("Bing");
                break;
            }
            case R.id.osm: {
                setMapType("OSM");
                break;
            }
            case R.id.show_traffic: {
                config.setShow_traffic(!config.isShow_traffic());
                updateMenu();
                callJs("showTraffic()");
                break;
            }
            case R.id.gps: {
                config.setUse_gps(!config.isUse_gps());
                updateMenu();
                stopListener();
                startListener();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    void share() {
        try {
            Js s = js();
            Class c = s.getClass();
            Class[] params = new Class[]{};
            Method m = c.getMethod("getData", params);
            String[] parts = m.invoke(s).toString().split("\\|")[0].split(";");
            String data = parts[3];
            data = data.replace("<b>", "").replace("</b>", "").replace("<br/>", "\n");
            String subj = "";
            int pos = data.indexOf("\n");
            if (pos > 0)
                subj = data.substring(0, pos);
            data += "\nhttp://maps.google.com/maps/place/" + parts[0] + "," + parts[1] + "/@" + parts[0] + "," + parts[1] + ",15z";
            Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subj);
            intent.putExtra(android.content.Intent.EXTRA_TEXT, data);
            startActivity(Intent.createChooser(intent, getResources().getString(R.string.share)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void setMapType(String type) {
        config.setMap_type(type);
        updateMenu();
        callJs("updateType()");
    }

    Location getLastBestLocation() {
        Location locationGPS = null;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException ex) {
            // ignore
        }
        Location locationNet = null;
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ex) {
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
        if (loaded && !noLocation) {
            callJs("myLocation()");
        }
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        String new_language = Locale.getDefault().getLanguage();
        if (language.equals(new_language))
            return;
        language = new_language;
        loadUrl(getUrl());
    }

    class JsInterface extends Js {

        @JavascriptInterface
        public void init() {
            loaded = true;
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
        public String getType() {
            return config.getMap_type();
        }

        @JavascriptInterface
        public String kmh() {
            return getString(R.string.kmh);
        }

        @JavascriptInterface
        public String traffic() {
            return config.isShow_traffic() ? "1" : "";
        }

        @JavascriptInterface
        public String speed() {
            return config.isShow_speed() ? "1" : "";
        }

        @JavascriptInterface
        public String language() {
            return language;
        }
    }

}
