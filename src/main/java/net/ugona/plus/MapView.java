package net.ugona.plus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.joda.time.LocalDateTime;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;

public class MapView extends GpsActivity {

    static final int REQUEST_ALARM = 4000;
    static final int UPDATE_INTERVAL = 30 * 1000;

    BroadcastReceiver br;
    String car_id;
    String point_data;
    Map<String, String> times;
    AlarmManager alarmMgr;
    PendingIntent pi;
    boolean active;
    Cars.Car[] cars;
    DateFormat df;
    DateFormat tf;

    @Override
    String loadURL() {
        webView.addJavascriptInterface(new JsInterface(), "android");
        return getURL();
    }

    String getURL() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getString("map_type", "").equals("OSM"))
            return "file:///android_asset/html/omaps.html";
        return "file:///android_asset/html/maps.html";
    }

    @Override
    int menuId() {
        return R.menu.map;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        car_id = getIntent().getStringExtra(Names.ID);
        point_data = getIntent().getStringExtra(Names.POINT_DATA);
        times = new HashMap<String, String>();
        if (savedInstanceState != null) {
            String car_data = savedInstanceState.getString(Names.CARS);
            if (car_data != null) {
                String[] data = car_data.split("\\|");
                for (String d : data) {
                    String[] p = d.split(";");
                    times.put(p[0], p[1]);
                }
            }
        }

        df = android.text.format.DateFormat.getDateFormat(this);
        tf = android.text.format.DateFormat.getTimeFormat(this);
        super.onCreate(savedInstanceState);

        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = createPendingResult(REQUEST_ALARM, new Intent(), 0);
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (loaded)
                    webView.loadUrl("javascript:update()");
                stopTimer();
                startTimer(false);
            }
        };
        registerReceiver(br, new IntentFilter(FetchService.ACTION_UPDATE));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(br);
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
        startTimer(true);
        setActionBar();
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
        stopTimer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ALARM) {
            Intent intent = new Intent(this, FetchService.class);
            intent.putExtra(Names.ID, car_id);
            startService(intent);
        }
    }

    void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        cars = Cars.getCars(this);
        boolean found = false;
        for (Cars.Car car : cars) {
            if (car.id.equals(car_id)) {
                found = true;
                break;
            }
        }
        if (!found)
            cars = new Cars.Car[0];
        if (cars.length > 1) {
            String save_point_data = point_data;
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setListNavigationCallbacks(new CarsAdapter(), new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int i, long l) {
                    if (cars[i].id.equals(car_id))
                        return true;
                    if (!loaded)
                        return true;
                    point_data = null;
                    car_id = cars[i].id;
                    webView.loadUrl("javascript:update()");
                    webView.loadUrl("javascript:center()");
                    return true;
                }
            });
            for (int i = 0; i < cars.length; i++) {
                if (cars[i].id.equals(car_id)) {
                    actionBar.setSelectedNavigationItem(i);
                    break;
                }
            }
            point_data = save_point_data;
            setTitle("");
        } else {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayUseLogoEnabled(false);
            setTitle(getString(R.string.app_name));
        }
    }

    void startTimer(boolean now) {
        if (!active)
            return;
        alarmMgr.setInexactRepeating(AlarmManager.RTC,
                System.currentTimeMillis() + (now ? 0 : UPDATE_INTERVAL), UPDATE_INTERVAL, pi);
    }

    void stopTimer() {
        alarmMgr.cancel(pi);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String data = null;
        for (Map.Entry<String, String> v : times.entrySet()) {
            String p = v.getKey() + ";" + v.getValue();
            if (data == null) {
                data = p;
            } else {
                data += "|" + p;
            }
        }
        if (data != null)
            outState.putString(Names.CARS, data);
    }

    class JsInterface {

        @JavascriptInterface
        public void done() {
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
        String createData(String id) {
            double lat = preferences.getFloat(Names.LAT + id, 0);
            double lng = preferences.getFloat(Names.LNG + id, 0);
            String zone = "";
            if ((lat == 0) && (lng == 0)) {
                zone = preferences.getString(Names.GSM_ZONE + id, "");
                String points[] = zone.split("_");
                double min_lat = 180;
                double max_lat = -180;
                double min_lon = 180;
                double max_lon = -180;
                for (String point : points) {
                    try {
                        String[] p = point.split(",");
                        double p_lat = Double.parseDouble(p[0]);
                        double p_lon = Double.parseDouble(p[1]);
                        if (p_lat > max_lat)
                            max_lat = p_lat;
                        if (p_lat < min_lat)
                            min_lat = p_lat;
                        if (p_lon > max_lon)
                            max_lon = p_lon;
                        if (p_lon < min_lon)
                            min_lon = p_lon;
                    } catch (Exception ex) {
                        // ignore
                    }
                }
                lat = ((min_lat + max_lat) / 2);
                lng = ((min_lon + max_lon) / 2);
            }
            String data = id + ";" +
                    lat + ";" +
                    lng + ";" +
                    preferences.getInt(Names.COURSE + id, 0) + ";";
            if (cars.length > 1) {
                String name = preferences.getString(Names.CAR_NAME + id, "");
                if (name.length() == 0) {
                    name = getString(R.string.car);
                    if (id.length() > 0)
                        name += " " + id;
                }
                data += name + "<br/>";
            }

            if (preferences.getBoolean(Names.POINTER + car_id, false)) {
                long last_stand = preferences.getLong(Names.EVENT_TIME + id, 0);
                if (last_stand > 0) {
                    data += "<b>";
                    data += df.format(last_stand) + " " + tf.format(last_stand);
                    data += "</b> ";
                }
            } else {
                long last_stand = preferences.getLong(Names.LAST_STAND + id, 0);
                if (last_stand > 0) {
                    LocalDateTime stand = new LocalDateTime(last_stand);
                    LocalDateTime now = new LocalDateTime();
                    data += "<b>";
                    if (stand.toLocalDate().equals(now.toLocalDate())) {
                        data += tf.format(last_stand);
                    } else {
                        data += df.format(last_stand) + " " + tf.format(last_stand);
                    }
                    data += "</b> ";
                } else if (last_stand < 0) {
                    double speed = preferences.getFloat(Names.SPEED + id, 0);
                    if (speed > 0) {
                        data += String.format(getString(R.string.speed), speed);
                        data += "<br/>";
                    }
                }
            }
            if (zone.equals("")) {
                data += Math.round(lat * 10000) / 10000. + "," + Math.round(lng * 10000) / 10000. + "<br/>";
                String address = Address.getAddress(getBaseContext(), lat, lng);
                if (address != null) {
                    String[] parts = address.split(", ");
                    if (parts.length >= 3) {
                        address = parts[0] + ", " + parts[1];
                        for (int n = 2; n < parts.length; n++)
                            address += "<br/>" + parts[n];
                    }
                    data += address;
                }
                data += ";";
            } else {
                String address = Address.getAddress(MapView.this, lat, lng);
                if (address != null) {
                    String[] parts = address.split(", ");
                    if (parts.length >= 3) {
                        address = parts[0] + ", " + parts[1];
                        for (int n = 2; n < parts.length; n++)
                            address += "<br/>" + parts[n];
                    }
                    data += address;
                }
                data += ";" + zone;
            }
            if (times.containsKey(id))
                data += ";" + times.get(id);
            return data;
        }

        @JavascriptInterface
        public String getData() {
            Cars.Car[] cars = Cars.getCars(MapView.this);

            String id = null;

            String data = point_data;

            if (data == null) {
                data = createData(car_id);
                id = car_id;
            }

            for (Cars.Car car : cars) {
                if ((id != null) && id.equals(car.id))
                    continue;
                data += "|" + createData(car.id);
            }
            return data;
        }
    }

    class CarsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return cars.length;
        }

        @Override
        public Object getItem(int position) {
            return cars[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getBaseContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.car_list_item, null);
            }
            TextView tv = (TextView) v.findViewById(R.id.name);
            tv.setText(cars[position].name);
            return v;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getBaseContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.car_list_dropdown_item, null);
            }
            TextView tv = (TextView) v.findViewById(R.id.name);
            tv.setText(cars[position].name);
            return v;
        }
    }

}
