package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Vector;

public class SettingActivity extends ActionBarActivity {

    final static String UPDATE_SETTINGS = "net.ugona.plus.UPDATE_SETTINGS";
    final static String URL_SETTINGS = "/settings?auth=$1&skey=$2";
    final static String URL_SET = "/set?auth=$1&skey=$2&v=$3";
    final static String URL_PROFILE = "/version?skey=$1";

    final static String ZONES = "zones";
    final static String ZONE_DELETED = "zone_deleted";
    final static String TIMERS = "timers";
    final static String TIMER_DELETED = "timer_deleted";

    String car_id;
    SharedPreferences preferences;
    BroadcastReceiver br;
    int[] values;
    int[] old_values;
    boolean values_error;
    boolean az;
    boolean rele;
    ActionBar.TabListener tabListener;
    Vector<Zone> zones;
    Vector<Timer> timers;
    boolean zone_deleted;
    boolean timers_deleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager);

        if (savedInstanceState != null) {
            car_id = savedInstanceState.getString(Names.ID);
            zone_deleted = savedInstanceState.getBoolean(ZONE_DELETED);
            timers_deleted = savedInstanceState.getBoolean(TIMER_DELETED);
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(savedInstanceState.getByteArray(ZONES));
                ObjectInput in = new ObjectInputStream(bis);
                zones = (Vector<Zone>) in.readObject();
            } catch (Exception ex) {
                // ignore
            }
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(savedInstanceState.getByteArray(TIMERS));
                ObjectInput in = new ObjectInputStream(bis);
                timers = (Vector<Timer>) in.readObject();
            } catch (Exception ex) {
                // ignore
            }
        } else {
            car_id = getIntent().getStringExtra(Names.ID);
            if (car_id == null)
                car_id = preferences.getString(Names.LAST, "");
        }
        Cars.Car[] cars = Cars.getCars(this);
        String title = "";
        if (cars.length > 1)
            title = preferences.getString(Names.Car.CAR_NAME + car_id, "");
        if (!title.equals(""))
            setTitle(title);

        final ViewPager pager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

            }
        };

        pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int pos) {
                ActionBar b = getSupportActionBar();
                try {
                    b.setSelectedNavigationItem(pos);
                    View action_bar_view = findViewById(getResources().getIdentifier("action_bar", "id", "android"));
                    Class<?> action_bar_class = action_bar_view.getClass();
                    Field tab_scroll_view_prop = action_bar_class.getDeclaredField("mTabScrollView");
                    tab_scroll_view_prop.setAccessible(true);
                    Object tab_scroll_view = tab_scroll_view_prop.get(action_bar_view);
                    if (tab_scroll_view == null) return;
                    Field spinner_prop = tab_scroll_view.getClass().getDeclaredField("mTabSpinner");
                    spinner_prop.setAccessible(true);
                    Object tab_spinner = spinner_prop.get(tab_scroll_view);
                    if (tab_spinner == null) return;
                    Method set_selection_method = tab_spinner.getClass().getSuperclass().getDeclaredMethod("setSelection", Integer.TYPE, Boolean.TYPE);
                    set_selection_method.invoke(tab_spinner, pos, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        int commands = State.getCommands(preferences, car_id);
        if ((commands & State.CMD_AZ) != 0)
            az = true;
        if ((commands & State.CMD_RELE) != 0)
            rele = true;

        for (int i = 0; i < adapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(adapter.getPageTitle(i))
                            .setTabListener(tabListener)
            );
        }
        setResult(RESULT_CANCELED);

        if (savedInstanceState != null) {
            values = savedInstanceState.getIntArray("values");
            old_values = savedInstanceState.getIntArray("old_values");
        }

        if (values == null)
            updateSettings();

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ((car_id != null) && car_id.equals(intent.getStringExtra(Names.ID))) {
                    int commands = State.getCommands(preferences, car_id);
                    boolean new_az = (commands & State.CMD_AZ) != 0;
                    boolean new_rele = (commands & State.CMD_RELE) != 0;
                    if ((az == new_az) && (rele == new_rele))
                        return;
                    adapter.notifyDataSetChanged();
                    if (az != new_az) {
                        int index = adapter.fromID(5);
                        if (new_az) {
                            actionBar.addTab(
                                    actionBar.newTab()
                                            .setText(adapter.getPageTitle(index))
                                            .setTabListener(tabListener), index
                            );
                        } else {
                            actionBar.removeTabAt(index);
                        }
                        az = new_az;
                    }
                    if (rele != new_rele) {
                        int index = adapter.fromID(6);
                        if (new_rele) {
                            actionBar.addTab(
                                    actionBar.newTab()
                                            .setText(adapter.getPageTitle(index))
                                            .setTabListener(tabListener), index
                            );
                        } else {
                            actionBar.removeTabAt(index);
                        }
                        rele = new_rele;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(FetchService.ACTION_UPDATE_FORCE);
        registerReceiver(br, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
        if (values != null) {
            outState.putIntArray("values", values);
            outState.putIntArray("old_values", old_values);
        }
        if (zones != null) {
            try {
                byte[] data = null;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(zones);
                data = bos.toByteArray();
                out.close();
                bos.close();
                outState.putByteArray(ZONES, data);
            } catch (Exception ex) {
                // ignore
            }
        }
        if (timers != null) {
            try {
                byte[] data = null;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(timers);
                data = bos.toByteArray();
                out.close();
                bos.close();
                outState.putByteArray(TIMERS, data);
            } catch (Exception ex) {
                // ignore
            }
        }
        outState.putBoolean(ZONE_DELETED, zone_deleted);
        outState.putBoolean(TIMER_DELETED, timers_deleted);
    }

    @Override
    public void finish() {
        boolean changed = zone_deleted | timers_deleted;
        if (!changed && (values != null)) {
            int i;
            for (i = 0; i < 24; i++) {
                if (values[i] != old_values[i])
                    break;
            }
            if (i < 24)
                changed = true;
        }
        if (!changed && (zones != null)) {
            for (Zone z : zones) {
                if (z.isChanged()) {
                    changed = true;
                    break;
                }
            }
        }
        if (!changed && (timers != null)) {
            for (Timer t : timers) {
                if (t.isChanged()) {
                    changed = true;
                    break;
                }
            }
        }
        if (changed) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.changed)
                    .setMessage(R.string.changed_msg)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SettingActivity.super.finish();
                        }
                    })
                    .create();
            dialog.show();
            return;
        }
        super.finish();
    }

    void updateSettings() {
        values_error = false;
        values = null;
        old_values = null;

        if (preferences.getBoolean(Names.Car.POINTER + car_id, false))
            return;

        if (preferences.getLong(Names.Car.EVENT_TIME + car_id, 0) <= preferences.getLong(Names.Car.SETTINGS_TIME + car_id, 0)) {
            values = new int[24];
            old_values = new int[24];
            for (int i = 0; i < 24; i++) {
                int v = preferences.getInt("V_" + i + "_" + car_id, 0);
                if (i == 21)
                    v = preferences.getInt(Names.Car.CAR_TIMER + car_id, 10);
                values[i] = v;
                old_values[i] = v;
            }
            String[] zone_info = preferences.getString(Names.Car.ZONE_INFO + car_id, "").split("\\|");
            zones = new Vector<Zone>();
            for (String info : zone_info) {
                String[] z = info.split(",");
                if (z.length != 7)
                    continue;
                Zone zone = new Zone();
                zone.name = z[0];
                zone.lat1 = Double.parseDouble(z[1]);
                zone.lng1 = Double.parseDouble(z[2]);
                zone.lat2 = Double.parseDouble(z[3]);
                zone.lng2 = Double.parseDouble(z[4]);
                zone.sms = !z[5].equals("");
                zone.device = !z[6].equals("");
                zone.clearChanged();
                zones.add(zone);
            }
            String[] timers_info = preferences.getString(Names.Car.TIMERS_INFO + car_id, "").split("\\|");
            timers = new Vector<Timer>();
            for (String info : timers_info) {
                String[] z = info.split(",");
                if (z.length < 5)
                    continue;
                Timer timer = new Timer();
                timer.days = Integer.parseInt(z[0]);
                timer.hours = Integer.parseInt(z[1]);
                timer.minutes = Integer.parseInt(z[2]);
                timer.period = Integer.parseInt(z[3]);
                timer.com = Integer.parseInt(z[4]);
                timer.param = (z.length > 5) ? z[5] : "";
                timer.id = timers.size();
                timer.clearChanged();
                timers.add(timer);
            }
            sendUpdate();
            return;
        }

        HttpTask task = new HttpTask() {

            @Override
            void result(JsonObject res) throws ParseException {
                SharedPreferences.Editor ed = preferences.edit();
                values = new int[24];
                old_values = new int[24];
                for (int i = 0; i < 24; i++) {
                    int v = preferences.getInt("V_" + i + "_" + car_id, 0);
                    if (i < 20) {
                        JsonValue val = res.get("v" + i);
                        if (val != null) {
                            v = val.asInt();
                            ed.putInt("V_" + i + "_" + car_id, v);
                        }
                    }
                    if (i == 21)
                        v = preferences.getInt(Names.Car.CAR_TIMER + car_id, 10);
                    values[i] = v;
                    old_values[i] = v;
                }
                JsonValue z_value = res.get("zones");
                if (z_value != null) {
                    JsonArray z_array = z_value.asArray();
                    zones = new Vector<Zone>();
                    String info = "";
                    for (int i = 0; i < z_array.size(); i++) {
                        JsonObject z_val = z_array.get(i).asObject();
                        Zone zone = new Zone();
                        zone.name = z_val.get("name").asString();
                        zone.lat1 = z_val.get("lat1").asDouble();
                        zone.lng1 = z_val.get("lng1").asDouble();
                        zone.lat2 = z_val.get("lat2").asDouble();
                        zone.lng2 = z_val.get("lng2").asDouble();
                        zone.device = z_val.get("device").asBoolean();
                        zone.sms = z_val.get("sms").asBoolean();
                        zone._name = zone.name;
                        zone._lat1 = zone.lat1;
                        zone._lng1 = zone.lng1;
                        zone._lat2 = zone.lat2;
                        zone._lng2 = zone.lng2;
                        zone._device = zone.device;
                        zone._sms = zone.sms;
                        zone.id = i;
                        zones.add(zone);
                        if (!info.equals(""))
                            info += "|";
                        info += zone.name + "," + zone.lat1 + "," + zone.lng1 + "," + zone.lat2 + "," + zone.lng2 + ",";
                        if (zone.sms)
                            info += "1";
                        info += ",";
                        if (zone.device)
                            info += "1";
                    }
                    ed.putString(Names.Car.ZONE_INFO + car_id, info);
                }
                JsonValue t_value = res.get("timers");
                if (t_value != null) {
                    JsonArray t_array = t_value.asArray();
                    timers = new Vector<Timer>();
                    String info = "";
                    for (int i = 0; i < t_array.size(); i++) {
                        JsonObject t_val = t_array.get(i).asObject();
                        Timer timer = new Timer();
                        timer.days = 1 << t_val.get("day").asInt();
                        timer.hours = t_val.get("hour").asInt();
                        timer.minutes = t_val.get("min").asInt();
                        timer.period = t_val.get("period").asInt();
                        timer.com = t_val.get("com").asInt();
                        timer.param = t_val.get("param").asString();
                        timer.id = timers.size();
                        timer.clearChanged();
                        for (Timer t : timers) {
                            if (t.eq(timer)) {
                                t.days |= timer.days;
                                t.clearChanged();
                                timer.days = 0;
                                break;
                            }
                        }
                        if (timer.days != 0)
                            timers.add(timer);
                        if (!info.equals(""))
                            info += "|";
                        info += timer.days + "," + timer.hours + "," + timer.minutes + "," + timer.period + "," + timer.com + "," + timer.param;
                    }
                    ed.putString(Names.Car.TIMERS_INFO + car_id, info);
                }
                ed.commit();
                sendUpdate();
            }

            @Override
            void error() {
                values_error = true;
                sendUpdate();
            }
        };
        task.execute(URL_SETTINGS, preferences.getString(Names.Car.AUTH + car_id, ""), preferences.getString(Names.Car.CAR_KEY + car_id, ""));

        HttpTask version = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                String ver = res.get("version").asString();
                if (ver.equals(preferences.getString(Names.Car.VERSION + car_id, "")))
                    return;
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.Car.VERSION + car_id, ver);
                ed.commit();
                sendUpdate();
            }

            @Override
            void error() {

            }
        };
        version.execute(URL_PROFILE, preferences.getString(Names.Car.CAR_KEY + car_id, ""));
        sendUpdate();
    }

    void sendUpdate() {
        Intent i = new Intent(UPDATE_SETTINGS);
        i.putExtra(Names.ID, car_id);
        sendBroadcast(i);
    }

    void update() {
        if (values == null) {
            if (values_error) {
                values_error = false;
                updateSettings();
                sendUpdate();
            }
            return;
        }
        String val = "";
        SharedPreferences.Editor ed = preferences.edit();
        boolean request_ccode = false;
        boolean need_sms = false;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == old_values[i])
                continue;
            if (i < 22) {
                if (!val.equals(""))
                    val += ",";
                val += i + "." + values[i];
                if (i == 20)
                    request_ccode = true;
            } else {
                need_sms = true;
            }
        }
        String zone_data = null;
        boolean zone_changed = zone_deleted;
        if (!zone_changed && (zones != null)) {
            for (Zone z : zones) {
                if (z.isChanged()) {
                    zone_changed = true;
                    break;
                }
            }
        }
        if (zone_changed) {
            zone_data = "";
            for (Zone z : zones) {
                if (!zone_data.equals(""))
                    zone_data += "|";
                zone_data += z.name + ",";
                zone_data += z.lat1 + ",";
                zone_data += z.lng1 + ",";
                zone_data += z.lat2 + ",";
                zone_data += z.lng2 + ",";
                if (z.sms)
                    zone_data += "1";
                zone_data += ",";
                if (z.device)
                    zone_data += "1";
            }
            ed.putString(Names.Car.ZONE_INFO + car_id, zone_data);
        }

        String timer_data = null;
        boolean timer_changed = timers_deleted;
        if (!timer_changed && (timers != null)) {
            for (Timer t : timers) {
                if (t.isChanged()) {
                    timer_changed = true;
                    break;
                }
            }
        }
        if (timer_changed) {
            timer_data = "";
            for (Timer t : timers) {
                if (!timer_data.equals(""))
                    timer_data += "|";
                timer_data += t.days + ",";
                timer_data += t.hours + ",";
                timer_data += t.minutes + ",";
                timer_data += t.period + ",";
                timer_data += t.com + ",";
                timer_data += t.param;
            }
            ed.putString(Names.Car.TIMERS_INFO + car_id, timer_data);
        }

        ed.commit();
        if (val.equals("") && !need_sms && (zone_data == null) && (timer_data == null))
            return;
        sendUpdate();
        final int[] set_values = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            set_values[i] = values[i];
        }
        final String value = val;
        final String zones_data = zone_data;
        final String timers_data = timer_data;
        final boolean sms = need_sms;
        if (request_ccode) {
            Actions.requestCCode(this, car_id, R.string.setup, R.string.setup_msg, new Actions.Answer() {
                @Override
                void answer(String text) {
                    do_update(value, text, set_values, sms, zones_data, timers_data);
                }
            });
            return;
        }

        Actions.requestPassword(this, car_id, R.string.setup, R.string.setup_msg, new Actions.Answer() {
            @Override
            void answer(String text) {
                do_update(value, null, set_values, sms, zones_data, timers_data);
            }
        });
    }

    void do_update_sms(ProgressDialog progressDialog, int[] set_values, String pswd) {
        SharedPreferences.Editor ed = preferences.edit();
        if (set_values[22] != old_values[22]) {
            ed.putInt("V_22_" + car_id, set_values[22]);
            SmsMonitor.sendSMS(progressDialog.getContext(), car_id, pswd, new SmsMonitor.Sms(R.string.shock_sens, "ALRM PRIOR " + (set_values[22] + 1), "ALRM PRIOR OK"));
            old_values[22] = set_values[22];
        }
        if (set_values[23] != old_values[23]) {
            ed.putInt("V_23_" + car_id, set_values[23]);
            if (set_values[23] != 0) {
                SmsMonitor.sendSMS(progressDialog.getContext(), car_id, pswd, new SmsMonitor.Sms(R.string.inf_sms, "INFSMS=NO", "INFSMS=NOT"));
            } else {
                SmsMonitor.sendSMS(progressDialog.getContext(), car_id, pswd, new SmsMonitor.Sms(R.string.inf_sms, "INFSMS=YES", "INFSMS YES OK"));
            }
            old_values[23] = set_values[23];
        }
        ed.commit();
        progressDialog.dismiss();
        sendUpdate();
    }

    void do_update(final String value, final String ccode, final int[] set_values, final boolean need_sms, String zones_data, String timer_data) {
        final ProgressDialog progressDialog = new ProgressDialog(SettingActivity.this);
        progressDialog.setMessage(getString(R.string.send_command));
        progressDialog.show();

        zone_deleted = false;
        if (zones != null) {
            for (Zone z : zones) {
                z.clearChanged();
            }
        }
        timers_deleted = false;
        if (timers != null) {
            for (Timer t : timers) {
                t.clearChanged();
            }
        }
        sendUpdate();

        if (values.equals("")) {
            do_update_sms(progressDialog, set_values, null);
            return;
        }

        HttpTask task = new HttpTask() {

            @Override
            void result(JsonObject res) throws ParseException {
                if (need_sms) {
                    do_update_sms(progressDialog, set_values, null);
                    return;
                }
                progressDialog.dismiss();
                if (State.isPandora(preferences, car_id))
                    return;
                LayoutInflater inflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                final AlertDialog dialog = new AlertDialog.Builder(SettingActivity.this)
                        .setTitle(R.string.setup)
                        .setView(inflater.inflate(R.layout.wait, null))
                        .setNegativeButton(R.string.ok, null)
                        .create();
                dialog.show();
                TextView tv = (TextView) dialog.findViewById(R.id.msg);
                final int wait_time = preferences.getInt(Names.Car.CAR_TIMER + car_id, 10);
                String msg = getString(R.string.wait_msg).replace("$1", wait_time + "");
                tv.setText(msg);
                Button btnCall = (Button) dialog.findViewById(R.id.call);
                btnCall.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + preferences.getString(Names.Car.CAR_PHONE + car_id, "")));
                        startActivity(intent);
                    }
                });
                Button btnSms = (Button) dialog.findViewById(R.id.sms);
                btnSms.setVisibility(View.GONE);
                SharedPreferences.Editor ed = preferences.edit();
                long time = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
                time += wait_time * 90000;
                ed.putLong(Names.Car.SETTINGS_TIME + car_id, time);
                for (int i = 0; i < set_values.length; i++) {
                    if (set_values[i] == old_values[i])
                        continue;
                    ed.putInt("V_" + i + "_" + car_id, set_values[i]);
                    old_values[i] = set_values[i];
                    if (i == 21)
                        ed.putInt(Names.Car.CAR_TIMER + car_id, set_values[i]);
                }
                ed.commit();
            }

            @Override
            void error() {
                progressDialog.dismiss();
                Toast toast = Toast.makeText(SettingActivity.this, R.string.data_error, Toast.LENGTH_SHORT);
                toast.show();
            }
        };
        task.execute(URL_SET, preferences.getString(Names.Car.AUTH + car_id, ""), preferences.getString(Names.Car.CAR_KEY + car_id, ""), value, "ccode", ccode, "zone", zones_data, "t", timer_data);
    }

    static class Zone implements Serializable {

        int id;

        String name;
        double lat1;
        double lng1;
        double lat2;
        double lng2;
        boolean device;
        boolean sms;

        String _name;
        double _lat1;
        double _lng1;
        double _lat2;
        double _lng2;
        boolean _device;
        boolean _sms;

        boolean isChanged() {
            return !name.equals(_name) ||
                    (lat1 != _lat1) ||
                    (lng1 != _lng1) ||
                    (lat2 != _lat2) ||
                    (lng2 != _lng2) ||
                    (device != _device) ||
                    (sms != _sms);
        }

        void clearChanged() {
            _name = name;
            _lat1 = lat1;
            _lng1 = lng1;
            _lat2 = lat2;
            _lng2 = lng2;
            _device = device;
            _sms = sms;
        }

        void set(Zone z) {
            name = z.name;
            lat1 = z.lat1;
            lng1 = z.lng1;
            lat2 = z.lat2;
            lng2 = z.lng2;
            device = z.device;
            sms = z.sms;
        }
    }

    static class Timer implements Serializable {
        int id;

        int days;
        int hours;
        int minutes;
        int period;
        int com;
        String param;

        int _days;
        int _hours;
        int _minutes;
        int _period;
        int _com;
        String _param;

        void clearChanged() {
            _days = days;
            _hours = hours;
            _minutes = minutes;
            _period = period;
            _com = com;
            _param = param;
        }

        boolean isChanged() {
            return (days != _days) ||
                    (hours != _hours) ||
                    (minutes != _minutes) ||
                    (period != _period) ||
                    (com != _com) ||
                    !param.equals(_param);
        }

        void set(Timer t) {
            days = t.days;
            hours = t.hours;
            minutes = t.minutes;
            period = t.period;
            com = t.com;
            param = t.param;
        }

        boolean eq(Timer t) {
            return (hours == t.hours) && (minutes == t.minutes) && (period == t.period) && (com == t.com) && param.equals(t.param);
        }
    }

    public class PagerAdapter extends FragmentPagerAdapter {

        boolean[] visible;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
            updateVisible();
        }

        @Override
        public void notifyDataSetChanged() {
            updateVisible();
            super.notifyDataSetChanged();
        }

        void updateVisible() {
            visible = new boolean[9];
            visible[0] = true;
            visible[1] = !State.isPandora(preferences, car_id);
            visible[2] = true;
            visible[3] = true;
            visible[4] = true;
            int commands = State.getCommands(preferences, car_id);
            visible[5] = (commands & State.CMD_AZ) != 0;
            visible[6] = (commands & State.CMD_RELE) != 0;
            visible[7] = !State.isPandora(preferences, car_id) && (Preferences.getTemperaturesCount(preferences, car_id) > 1);
            visible[8] = !State.isPandora(preferences, car_id) && ((preferences.getFloat(Names.Car.LAT + car_id, 0) != 0) || (preferences.getFloat(Names.Car.LNG + car_id, 0) != 0));
        }

        int toID(int pos) {
            int i;
            for (i = 0; i < visible.length; i++) {
                if (!visible[i])
                    continue;
                if (pos-- == 0)
                    break;
            }
            return i;
        }

        int fromID(int id) {
            int res = 0;
            for (int i = 0; i < id; i++) {
                if (visible[i])
                    res++;
            }
            return res;
        }

        @Override
        public long getItemId(int position) {
            return toID(position);
        }

        @Override
        public Fragment getItem(int i) {
            SettingsFragment res = null;
            switch (toID(i)) {
                case 0:
                    res = new AuthFragment();
                    break;
                case 1:
                    res = new NotificationFragment();
                    break;
                case 2:
                    res = new WidgetsFragment();
                    break;
                case 3:
                    res = new CommandsFragment();
                    break;
                case 4:
                    res = new DeviceSettingsFragment();
                    break;
                case 5:
                    res = new AutoStartFragment();
                    break;
                case 6:
                    res = new HeaterFragment();
                    break;
                case 7:
                    res = new TemperatureFragment();
                    break;
                case 8:
                    res = new ZonesFragment();
                    break;
            }
            if (res == null)
                return null;
            res.car_id = car_id;
            return res;
        }

        @Override
        public int getCount() {
            if (preferences.getBoolean(Names.Car.POINTER + car_id, false))
                return 1;
            return fromID(visible.length);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (toID(position)) {
                case 0:
                    return getString(R.string.auth);
                case 1:
                    return getString(R.string.notifications);
                case 2:
                    return getString(R.string.widgets);
                case 3:
                    return getString(R.string.commands);
                case 4:
                    return getString(R.string.device_settings);
                case 5:
                    return getString(R.string.autostart);
                case 6:
                    return getString(R.string.rele);
                case 7:
                    return getString(R.string.temp_sensors);
                case 8:
                    return getString(R.string.zones);
            }
            return null;
        }
    }

}
