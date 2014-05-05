package net.ugona.plus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import org.joda.time.LocalDateTime;
import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayItem;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapPointActivity extends MapActivity {

    static final int REQUEST_ALARM = 4000;
    static final int UPDATE_INTERVAL = 30 * 1000;

    final static String URL_TRACKS = "https://car-online.ugona.net/tracks?skey=$1&begin=$2&end=$3";

    String car_id;
    String point_data;
    Map<String, String> times;
    DateFormat df;
    DateFormat tf;
    Cars.Car[] cars;
    CarsOverlay mMyLocationOverlay;
    TrackOverlay mTrackOverlay;

    AlarmManager alarmMgr;
    PendingIntent pi;
    BroadcastReceiver br;

    boolean active;
    HttpTask trackTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                updateTrack();
                stopTimer();
                startTimer(false);
            }
        };
        registerReceiver(br, new IntentFilter(FetchService.ACTION_UPDATE));
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
    protected void onDestroy() {
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    int menuId() {
        return R.menu.map;
    }

    @Override
    void initMap(IMapController controller) {

        final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        if (cars == null)
            cars = Cars.getCars(this);

        mTrackOverlay = new TrackOverlay(this);
        mMapView.getOverlays().add(mTrackOverlay);

        mMyLocationOverlay = new CarsOverlay(this);

        mMyLocationOverlay.setFocusItemsOnTap(true);
        mMyLocationOverlay.setFocusedItem(mMyLocationOverlay.find(car_id));

        mMapView.getOverlays().add(mMyLocationOverlay);

        controller.setZoom(16);
        int selected = mMyLocationOverlay.find(car_id);
        if (selected >= 0) {
            MyOverlayItem item = mMyLocationOverlay.getItem(selected);
            controller.setCenter(item.getPoint());
            if (item.zone != null)
                mMapView.fitToRect(new GeoPoint(item.min_lat, item.min_lon), new GeoPoint(item.max_lat, item.max_lon), 0.7);
        }
        updateTrack();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.map) {
            int current = mMyLocationOverlay.find(car_id);
            if (current < 0)
                return true;
            mMyLocationOverlay.setFocusedItem(current);
            MyOverlayItem i = mMyLocationOverlay.getItem(current);
            mMapView.getController().setCenter(i.getPoint());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    void updateLocation(Rect rc) {
        int selected = mMyLocationOverlay.find(car_id);
        if (selected >= 0) {
            MyOverlayItem item = mMyLocationOverlay.getItem(selected);
            updateLocation(rc, item);
        }
    }

    void updateTrack() {
        if (trackTask != null)
            return;

        boolean engine = preferences.getBoolean(Names.Car.INPUT3 + car_id, false) || preferences.getBoolean(Names.Car.ZONE_IGNITION + car_id, false);
        boolean az = preferences.getBoolean(Names.Car.AZ + car_id, false);
        if (!engine || az) {
            if (mTrackOverlay != null)
                mTrackOverlay.clear();
            return;
        }

        trackTask = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                JsonArray list = res.get("tracks").asArray();
                if (list.size() > 0) {
                    JsonObject v = list.get(list.size() - 1).asObject();
                    mTrackOverlay.clear();
                    mTrackOverlay.add(v.get("track").asString());
                    mMapView.invalidate();
                }
                trackTask = null;
            }

            @Override
            void error() {
                trackTask = null;
            }
        };
        long end = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
        trackTask.execute(URL_TRACKS, preferences.getString(Names.Car.CAR_KEY + car_id, ""), end - 86400000, end);

    }

    boolean updateItem(MyOverlayItem item) {
        double lat = preferences.getFloat(Names.Car.LAT + item.getUid(), 0);
        double lng = preferences.getFloat(Names.Car.LNG + item.getUid(), 0);
        item.zone = null;
        if ((lat == 0) || (lng == 0)) {
            String zone = preferences.getString(Names.Car.GSM_ZONE + item.getUid(), "");
            if (zone.equals(""))
                return false;
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
            item.setZone(zone);
        }
        String title = "";
        String speed = "";
        if (preferences.getBoolean(Names.Car.POINTER + car_id, false)) {
            long last_stand = preferences.getLong(Names.Car.EVENT_TIME + item.getUid(), 0);
            if (last_stand > 0)
                title += df.format(last_stand) + " " + tf.format(last_stand);
        } else {
            long last_stand = preferences.getLong(Names.Car.LAST_STAND + item.getUid(), 0);
            if (last_stand < 0) {
                double s = preferences.getFloat(Names.Car.SPEED + item.getUid(), 0);
                if (s > 0)
                    speed = String.format(getString(R.string.speed), s);
                last_stand = preferences.getLong(Names.Car.EVENT_TIME + item.getUid(), 0);
            }
            if (last_stand > 0) {
                LocalDateTime stand = new LocalDateTime(last_stand);
                LocalDateTime now = new LocalDateTime();
                if (stand.toLocalDate().equals(now.toLocalDate())) {
                    title += tf.format(last_stand);
                } else {
                    title += df.format(last_stand) + " " + tf.format(last_stand);
                }
            }
        }

        String data = Math.round(lat * 10000) / 10000. + "," + Math.round(lng * 10000) / 10000.;
        String address = Address.getAddress(getBaseContext(), lat, lng);
        if (address != null) {
            String[] parts = address.split(", ");
            if (parts.length >= 3) {
                address = parts[0] + ", " + parts[1] + "\n" + parts[2];
                for (int n = 3; n < parts.length; n++)
                    address += ", " + parts[n];
            }
            data += "\n" + address;
        } else {
            Address addr = new Address() {
                @Override
                void result(String address) {
                    update();
                }
            };
            addr.get(this, lat, lng);
        }
        if (!speed.equals(""))
            data += "\n" + speed;
        for (Cars.Car car : cars) {
            if (!car.id.equals(item.getUid()))
                continue;
            if (cars.length > 1)
                title += " " + car.name;
            item.set(title, data, new GeoPoint(lat, lng), preferences.getInt(Names.Car.COURSE + car.id, 0));
            return true;
        }
        return false;
    }

    void update() {
        mMyLocationOverlay.update();
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
                    car_id = cars[i].id;
                    int current = mMyLocationOverlay.find(car_id);
                    if (current < 0)
                        return true;
                    mMyLocationOverlay.setFocusedItem(current);
                    MyOverlayItem item = mMyLocationOverlay.getItem(current);
                    mMapView.getController().setCenter(item.getPoint());
                    if (item.zone != null)
                        mMapView.fitToRect(new GeoPoint(item.min_lat, item.min_lon), new GeoPoint(item.max_lat, item.max_lon), 0.7);
                    trackTask = null;
                    mTrackOverlay.clear();
                    updateTrack();
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

    class CarsOverlay extends LocationOverlay {

        public CarsOverlay(Context ctx) {
            super(ctx);
            for (Cars.Car car : cars) {
                MyOverlayItem item = new MyOverlayItem(car.id);
                if (updateItem(item))
                    mItemList.add(item);
            }
            populate();
        }

        void update() {
            for (MyOverlayItem item : mItemList) {
                if (!updateItem(item))
                    mItemList.remove(item);
            }
            populate();
        }

        int find(String id) {
            for (int i = 0; i < mItemList.size(); i++) {
                if (getItem(i).getUid().equals(id))
                    return i;
            }
            return -1;
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
