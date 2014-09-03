package net.ugona.plus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import org.joda.time.LocalDateTime;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;

public class MapPointActivity extends MapActivity {

    static final int REQUEST_ALARM = 4000;
    static final int UPDATE_INTERVAL = 30 * 1000;

    final static String URL_TRACKS = "https://car-online.ugona.net/tracks?skey=$1&begin=$2&end=$3";

    String car_id;
    BroadcastReceiver br;
    PendingIntent pi;
    AlarmManager alarmMgr;
    boolean active;

    Cars.Car[] cars;
    String point_data;

    DateFormat df;
    DateFormat tf;

    Map<String, String> times;

    HttpTask trackTask;
    String track_data;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        car_id = getIntent().getStringExtra(Names.ID);
        super.onCreate(savedInstanceState);

        df = android.text.format.DateFormat.getDateFormat(this);
        tf = android.text.format.DateFormat.getTimeFormat(this);

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

        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = createPendingResult(REQUEST_ALARM, new Intent(), 0);
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
                updateTrack();
                stopTimer();
                startTimer(false);
            }
        };
        registerReceiver(br, new IntentFilter(FetchService.ACTION_UPDATE));
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
                    webView.loadUrl("javascript:showPoints()");
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
        updateTrack();
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

    void update() {
        if (loaded)
            webView.loadUrl("javascript:showPoints()");
    }

    void updateTrack() {
        if (trackTask != null)
            return;

        boolean engine = preferences.getBoolean(Names.Car.INPUT3 + car_id, false) || preferences.getBoolean(Names.Car.ZONE_IGNITION + car_id, false);
        boolean az = preferences.getBoolean(Names.Car.AZ + car_id, false);
        if (!engine || az) {
            if (track_data == null)
                return;
            track_data = null;
            update();
            return;
        }

        trackTask = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                JsonArray list = res.get("tracks").asArray();
                if (list.size() > 0) {
                    JsonObject v = list.get(list.size() - 1).asObject();
                    track_data = v.get("track").asString();
                }
                trackTask = null;
                update();
            }

            @Override
            void error() {
                trackTask = null;
            }
        };
        long end = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
        trackTask.execute(URL_TRACKS, preferences.getString(Names.Car.CAR_KEY + car_id, ""), end - 86400000, end);

    }

    @Override
    int menuId() {
        return R.menu.map;
    }

    @Override
    JsInterface js() {
        return new JsInterface();
    }

    class JsInterface extends MapActivity.JsInterface {

        @JavascriptInterface
        String createData(String id) {
            double lat = preferences.getFloat(Names.Car.LAT + id, 0);
            double lng = preferences.getFloat(Names.Car.LNG + id, 0);
            String zone = "";
            if ((lat == 0) && (lng == 0)) {
                zone = preferences.getString(Names.Car.GSM_ZONE + id, "");
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
            String data =
                    lat + ";" +
                            lng + ";" +
                            preferences.getInt(Names.Car.COURSE + id, 0) + ";";
            if (cars.length > 1) {
                String name = preferences.getString(Names.Car.CAR_NAME + id, "");
                if (name.length() == 0) {
                    name = getString(R.string.car);
                    if (id.length() > 0)
                        name += " " + id;
                }
                data += name + "<br/>";
            }

            if (preferences.getBoolean(Names.Car.POINTER + car_id, false)) {
                long last_stand = preferences.getLong(Names.Car.EVENT_TIME + id, 0);
                if (last_stand > 0) {
                    data += "<b>";
                    data += df.format(last_stand) + " " + tf.format(last_stand);
                    data += "</b> ";
                }
            } else {
                long last_stand = preferences.getLong(Names.Car.LAST_STAND + id, 0);
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
                    double speed = preferences.getFloat(Names.Car.SPEED + id, 0);
                    if (speed > 0) {
                        data += String.format(getString(R.string.speed), speed);
                        data += "<br/>";
                    }
                }
            }
            data += Math.round(lat * 10000) / 10000. + "," + Math.round(lng * 10000) / 10000. + "<br/>";
            String address = Address.get(getBaseContext(), lat, lng, new Address.Answer() {
                @Override
                public void result(String address) {
                    if (loaded)
                        webView.loadUrl("javascript:showPoints()");
                }
            });
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
            if (!zone.equals(""))
                data += ";" + zone;
            if (times.containsKey(id))
                data += ";" + times.get(id);
            return data;
        }

        @JavascriptInterface
        public String getData() {
            Cars.Car[] cars = Cars.getCars(MapPointActivity.this);

            String id = null;

            String data = point_data;

            if (data == null) {
                data = createData(car_id);
                if (track_data != null) {
                    data += ";";
                    long last_time = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
                    String[] points = track_data.split("\\|");
                    for (String point : points) {
                        String[] p = point.split(",");
                        if (p.length != 4)
                            continue;
                        long time = Long.parseLong(p[3]);
                        if (time > last_time)
                            continue;
                        data += point + "_";
                    }
                    data += preferences.getFloat(Names.Car.LAT + car_id, 0) + ",";
                    data += preferences.getFloat(Names.Car.LNG + car_id, 0) + ",";
                    data += preferences.getFloat(Names.Car.SPEED + car_id, 0) + ",";
                    data += last_time;
                }
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
