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

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SettingActivity extends ActionBarActivity {

    final static String UPDATE_SETTINGS = "net.ugona.plus.UPDATE_SETTINGS";
    final static String URL_SETTINGS = "https://car-online.ugona.net/settings?auth=$1";
    final static String URL_SET = "https://car-online.ugona.net/set?auth=$1&v=$2";
    final static String URL_SET_CCODE = "https://car-online.ugona.net/set?auth=$1&v=$2&ccode=$2";
    final static String URL_PROFILE = "https://car-online.ugona.net/version?skey=$1";
    String car_id;
    SharedPreferences preferences;
    BroadcastReceiver br;
    int[] values;
    int[] old_values;
    boolean values_error;
    boolean az;
    boolean rele;
    ActionBar.TabListener tabListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager);

        if (savedInstanceState != null) {
            car_id = savedInstanceState.getString(Names.ID);
        } else {
            car_id = getIntent().getStringExtra(Names.ID);
            if (car_id == null)
                car_id = preferences.getString(Names.LAST, "");
        }

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
                            .setTabListener(tabListener));
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
                        int index = adapter.fromID(4);
                        if (new_az) {
                            actionBar.addTab(
                                    actionBar.newTab()
                                            .setText(adapter.getPageTitle(index))
                                            .setTabListener(tabListener), index);
                        } else {
                            actionBar.removeTabAt(index);
                        }
                        az = new_az;
                    }
                    if (rele != new_rele) {
                        int index = adapter.fromID(5);
                        if (new_rele) {
                            actionBar.addTab(
                                    actionBar.newTab()
                                            .setText(adapter.getPageTitle(index))
                                            .setTabListener(tabListener), index);
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
        if (values != null) {
            outState.putIntArray("values", values);
            outState.putIntArray("old_values", old_values);
        }
    }

    @Override
    public void finish() {
        if (values != null) {
            int i;
            for (i = 0; i < 24; i++) {
                if (values[i] != old_values[i])
                    break;
            }
            if (i < 24) {
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
        }
        super.finish();
    }

    void updateSettings() {
        values_error = false;
        values = null;
        old_values = null;

        if (preferences.getBoolean(Names.POINTER + car_id, false))
            return;

        if (preferences.getLong(Names.EVENT_TIME + car_id, 0) <= preferences.getLong(Names.SETTINGS_TIME + car_id, 0)) {
            values = new int[24];
            old_values = new int[24];
            for (int i = 0; i < 24; i++) {
                int v = preferences.getInt("V_" + i + "_" + car_id, 0);
                if (i == 21)
                    v = preferences.getInt(Names.CAR_TIMER + car_id, 10);
                values[i] = v;
                old_values[i] = v;
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
                        v = preferences.getInt(Names.CAR_TIMER + car_id, 10);
                    values[i] = v;
                    old_values[i] = v;
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
        task.execute(URL_SETTINGS, preferences.getString(Names.AUTH + car_id, ""));

        HttpTask version = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                String ver = res.get("version").asString();
                if (ver.equals(preferences.getString(Names.VERSION + car_id, "")))
                    return;
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.VERSION + car_id, ver);
                ed.commit();
                sendUpdate();
            }

            @Override
            void error() {

            }
        };
        version.execute(URL_PROFILE, preferences.getString(Names.CAR_KEY + car_id, ""));
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
        ed.commit();
        if (val.equals("") && !need_sms)
            return;
        sendUpdate();
        final int[] set_values = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            set_values[i] = values[i];
        }
        final String value = val;
        final boolean sms = need_sms;
        if (request_ccode) {
            Actions.requestCCode(this, car_id, R.string.setup, R.string.setup_msg, new Actions.Answer() {
                @Override
                void answer(String text) {
                    do_update(value, text, set_values, sms);
                }
            });
            return;
        }

        Actions.requestPassword(this, R.string.setup, R.string.setup_msg, new Runnable() {
            @Override
            public void run() {
                do_update(value, null, set_values, sms);
            }
        });
    }

    void do_update_sms(ProgressDialog progressDialog, int[] set_values) {
        SharedPreferences.Editor ed = preferences.edit();
        if (set_values[22] != old_values[22]) {
            ed.putInt("V_22_" + car_id, set_values[22]);
            SmsMonitor.sendSMS(progressDialog.getContext(), car_id, new SmsMonitor.Sms(R.string.shock_sens, "ALRM PRIOR " + (set_values[22] + 1), "ALRM PRIOR OK"));
            old_values[22] = set_values[22];
        }
        if (set_values[23] != old_values[23]) {
            ed.putInt("V_23_" + car_id, set_values[23]);
            if (set_values[23] != 0) {
                SmsMonitor.sendSMS(progressDialog.getContext(), car_id, new SmsMonitor.Sms(R.string.inf_sms, "INFSMS=NO", "INFSMS=NOT"));
            } else {
                SmsMonitor.sendSMS(progressDialog.getContext(), car_id, new SmsMonitor.Sms(R.string.inf_sms, "INFSMS=YES", "INFSMS YES OK"));
            }
            old_values[23] = set_values[23];
        }
        ed.commit();
        progressDialog.dismiss();
        sendUpdate();
    }

    void do_update(final String value, final String ccode, final int[] set_values, final boolean need_sms) {
        final ProgressDialog progressDialog = new ProgressDialog(SettingActivity.this);
        progressDialog.setMessage(getString(R.string.send_command));
        progressDialog.show();

        if (values.equals("")) {
            do_update_sms(progressDialog, set_values);
            return;
        }

        HttpTask task = new HttpTask() {

            @Override
            void result(JsonObject res) throws ParseException {
                if (need_sms) {
                    do_update_sms(progressDialog, set_values);
                    return;
                }
                progressDialog.dismiss();
                LayoutInflater inflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                final AlertDialog dialog = new AlertDialog.Builder(SettingActivity.this)
                        .setTitle(R.string.setup)
                        .setView(inflater.inflate(R.layout.wait, null))
                        .setNegativeButton(R.string.ok, null)
                        .create();
                dialog.show();
                TextView tv = (TextView) dialog.findViewById(R.id.msg);
                final int wait_time = preferences.getInt(Names.CAR_TIMER + car_id, 10);
                String msg = getString(R.string.wait_msg).replace("$1", wait_time + "");
                tv.setText(msg);
                Button btnCall = (Button) dialog.findViewById(R.id.call);
                btnCall.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + preferences.getString(Names.CAR_PHONE + car_id, "")));
                        startActivity(intent);
                    }
                });
                Button btnSms = (Button) dialog.findViewById(R.id.sms);
                btnSms.setVisibility(View.GONE);
                SharedPreferences.Editor ed = preferences.edit();
                long time = preferences.getLong(Names.EVENT_TIME + car_id, 0);
                time += wait_time * 90000;
                ed.putLong(Names.SETTINGS_TIME + car_id, time);
                for (int i = 0; i < set_values.length; i++) {
                    if (set_values[i] == old_values[i])
                        continue;
                    ed.putInt("V_" + i + "_" + car_id, set_values[i]);
                    old_values[i] = set_values[i];
                    if (i == 21)
                        ed.putInt(Names.CAR_TIMER + car_id, set_values[i]);
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
        if (ccode != null) {
            task.execute(URL_SET_CCODE, preferences.getString(Names.AUTH + car_id, ""), value, ccode);
            return;
        }
        task.execute(URL_SET, preferences.getString(Names.AUTH + car_id, ""), value);
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
            visible = new boolean[6];
            visible[0] = true;
            visible[1] = true;
            visible[2] = true;
            visible[3] = true;
            int commands = State.getCommands(preferences, car_id);
            visible[4] = (commands & State.CMD_AZ) != 0;
            visible[5] = (commands & State.CMD_RELE) != 0;
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
                    res = new CommandsFragment();
                    break;
                case 3:
                    res = new DeviceSettingsFragment();
                    break;
                case 4:
                    res = new AutoStartFragment();
                    break;
                case 5:
                    res = new HeaterFragment();
                    break;
            }
            if (res == null)
                return null;
            res.car_id = car_id;
            return res;
        }

        @Override
        public int getCount() {
            if (preferences.getBoolean(Names.POINTER + car_id, false))
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
                    return getString(R.string.commands);
                case 3:
                    return getString(R.string.device_settings);
                case 4:
                    return getString(R.string.autostart);
                case 5:
                    return getString(R.string.rele);
            }
            return null;
        }
    }

}
