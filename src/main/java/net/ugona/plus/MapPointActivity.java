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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MapPointActivity extends MapActivity {

    static final int REQUEST_ALARM = 4000;
    static final int UPDATE_INTERVAL = 30 * 1000;

    String car_id;
    String[] ids;

    String point_data;
    String track_data;

    DateFormat df;
    DateFormat tf;

    Map<String, String> times;

    BroadcastReceiver br;

    PendingIntent pi;
    AlarmManager alarmMgr;
    boolean active;

    HttpTask trackTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        car_id = getIntent().getStringExtra(Names.ID);
        super.onCreate(savedInstanceState);
        AppConfig appConfig = AppConfig.get(this);
        ids = appConfig.getIds().split(";");
        df = android.text.format.DateFormat.getDateFormat(this);
        tf = android.text.format.DateFormat.getTimeFormat(this);
        times = new HashMap<String, String>();
        if (savedInstanceState != null) {
            String car_data = savedInstanceState.getString(Names.POINT_DATA);
            if (car_data != null) {
                String[] data = car_data.split("\\|");
                for (String d : data) {
                    String[] p = d.split(";");
                    times.put(p[0], p[1]);
                }
            }
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        if (ids.length > 1) {
            findViewById(R.id.logo).setVisibility(View.GONE);
            Spinner navSpinner = (Spinner) findViewById(R.id.spinner_nav);
            navSpinner.setVisibility(View.VISIBLE);
            navSpinner.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return ids.length;
                }

                @Override
                public Object getItem(int position) {
                    return ids[position];
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    return getView(position, convertView, R.layout.car_list_item);
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    return getView(position, convertView, R.layout.car_list_dropdown_item);
                }

                public View getView(int position, View convertView, int layout_id) {
                    View v = convertView;
                    if (v == null) {
                        LayoutInflater inflater = LayoutInflater.from(getSupportActionBar().getThemedContext());
                        v = inflater.inflate(layout_id, null);
                    }
                    TextView tv = (TextView) v.findViewById(R.id.name);
                    CarConfig carConfig = CarConfig.get(MapPointActivity.this, ids[position]);
                    String name = carConfig.getName();
                    tv.setText(name);
                    return v;
                }

            });
            for (int i = 0; i < ids.length; i++) {
                if (ids[i].equals(car_id)) {
                    navSpinner.setSelection(i);
                    break;
                }
            }
            navSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (ids[i].equals(car_id))
                        return;
                    if (!loaded)
                        return;
                    point_data = null;
                    car_id = ids[i];
                    trackTask = null;
                    updateTrack();
                    callJs("showPoints()");
                    callJs("center()");
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        } else {
            findViewById(R.id.logo).setVisibility(View.VISIBLE);
            findViewById(R.id.spinner_nav).setVisibility(View.GONE);
        }
        updateTrack();

        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = createPendingResult(REQUEST_ALARM, new Intent(), 0);
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Names.ADDRESS_UPDATE)) {
                    if (loaded)
                        callJs("showPoints()");
                    return;
                }
                update();
                updateTrack();
                stopTimer();
                startTimer(false);
            }
        };
        IntentFilter intentFilter = new IntentFilter(FetchService.ACTION_UPDATE);
        intentFilter.addAction(Names.ADDRESS_UPDATE);
        registerReceiver(br, intentFilter);
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
            outState.putString(Names.POINT_DATA, data);
    }

    @Override
    int menuId() {
        return R.menu.map;
    }

    @Override
    Js js() {
        return new JsInterface();
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
            callJs("showPoints()");
    }

    void updateTrack() {
        if (trackTask != null)
            return;

        CarState state = CarState.get(this, car_id);
        if (!state.isIgnition() || state.isAz()) {
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
        Params params = new Params();
        CarConfig carConfig = CarConfig.get(this, car_id);
        CarState carState = CarState.get(this, car_id);
        params.skey = carConfig.getKey();
        params.end = carState.getTime();
        params.begin = params.end - 86400000;
        trackTask.execute("/tracks", params);
    }

    static class Params implements Serializable {
        String skey;
        long begin;
        long end;
    }

    class JsInterface extends MapActivity.JsInterface {

        private String createData(String id) {
            CarState carState = CarState.get(MapPointActivity.this, id);
            String[] gps = carState.getGps().split(",");
            double lat = Double.parseDouble(gps[0]);
            double lng = Double.parseDouble(gps[1]);
            String location = Math.round(lat * 10000) / 10000. + "," + Math.round(lng * 10000) / 10000.;
            if (!carState.isGps_valid()) {
                String gsm_sector = carState.getGsm();
                if (!gsm_sector.equals("")) {
                    String[] parts = gsm_sector.split(" ");
                    if (parts.length == 4)
                        location = "LAC: " + parts[2] + " CID: " + parts[3];
                }
            }
            String data = lat + ";" + lng + ";";
            if (carState.isGps_valid())
                data += carState.getCourse();
            data += ";";
            if (ids.length > 1) {
                CarConfig carConfig = CarConfig.get(MapPointActivity.this, id);
                String name = carConfig.getName();
                data += name + "<br/>";
            }

            long last_stand = carState.getLast_stand();
            if (last_stand > 0) {
                Calendar stand = Calendar.getInstance();
                stand.setTimeInMillis(last_stand);
                Calendar now = Calendar.getInstance();
                if ((stand.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) && (stand.get(Calendar.MONTH) == now.get(Calendar.MONTH))) {
                    data += tf.format(last_stand);
                } else {
                    data += df.format(last_stand) + " " + tf.format(last_stand);
                }
                data += "</b> ";
            } else if (last_stand < 0) {
                int speed = carState.getSpeed();
                if (!carState.isGps_valid())
                    speed = 0;
                if (speed > 0) {
                    data += String.format(getString(R.string.speed), speed);
                    data += "<br/>";
                }
            }

            data += location + "<br/>";
            String address = carState.getAddress(getBaseContext());
            if (address != null) {
                String[] parts = address.split(", ");
                if (parts.length >= 3) {
                    address = parts[0] + ", " + parts[1];
                    for (int n = 2; n < parts.length; n++)
                        address += "<br/>" + parts[n];
                }
                data += address;
            }
            data += ";" + carState.getHdop();
            if (times.containsKey(id))
                data += ";" + times.get(id);
            return data;
        }

        @JavascriptInterface
        public String getData() {

            String id = null;
            String data = point_data;
            if (data == null) {
                data = createData(car_id);
                if (track_data != null) {
                    data += ";";
                    CarState carState = CarState.get(MapPointActivity.this, car_id);
                    long last_time = carState.getTime();
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
                    data += carState.getGps() + ",";
                    int speed = carState.getSpeed();
                    if (!carState.isGps_valid())
                        speed = 0;
                    data += speed + ",";
                    data += last_time;
                }
                id = car_id;
            }

            for (String i : ids) {
                if ((id != null) && id.equals(i))
                    continue;
                data += "|" + createData(i);
            }
            return data;
        }
    }

}
