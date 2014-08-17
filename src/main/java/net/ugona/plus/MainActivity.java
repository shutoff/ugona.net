package net.ugona.plus;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.romorama.caldroid.CaldroidFragment;
import com.romorama.caldroid.CaldroidListener;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.viewpagerindicator.TitlePageIndicator;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.io.Reader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends ActionBarActivity {

    static final int REQUEST_ALARM = 4000;
    static final int CAR_SETUP = 4001;

    static final int UPDATE_INTERVAL = 30 * 1000;

    final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    static final String DATE = "date";

    static final int PAGE_PHOTO = 0;
    static final int PAGE_ACTIONS = 1;
    static final int PAGE_STATE = 2;
    static final int PAGE_EVENT = 3;
    static final int PAGE_TRACK = 4;
    static final int PAGE_STAT = 5;

    static final int VERSION = 4;
    static final String SENDER_ID = "915289471784";
    final static String URL_KEY = "https://car-online.ugona.net/key?login=$1&password=$2";
    final static String URL_PROFILE = "https://car-online.ugona.net/version?skey=$1";
    final static String URL_PHOTOS = "https://car-online.ugona.net/photos?skey=$1&begin=$2";
    ViewPager mViewPager;
    SharedPreferences preferences;
    BroadcastReceiver br;
    AlarmManager alarmMgr;
    PendingIntent pi;
    String car_id;
    Cars.Car[] cars;
    boolean show_date;
    boolean show_stat;
    boolean pointer;
    boolean[] show_pages;
    LocalDate current;
    Menu topSubMenu;
    boolean active;
    StateFragment state_fragment;
    StatFragment stat_fragment;
    CaldroidFragment caldroidFragment;
    Set<DateChangeListener> dateChangeListenerSet;
    GoogleCloudMessaging gcm;
    PowerManager powerMgr;
    ProgressDialog auth_progress;
    Vector<AuthData> auth_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

/*
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                State.print(ex);
                System.exit(1);
            }
        });
*/

        Thread.currentThread().setContextClassLoader(this.getClassLoader());
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }
        dateChangeListenerSet = new HashSet<DateChangeListener>();

        super.onCreate(savedInstanceState);

        powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        setContentView(R.layout.main);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        current = new LocalDate();

        show_pages = new boolean[6];
        show_pages[PAGE_PHOTO] = preferences.getBoolean(Names.Car.SHOW_PHOTO + car_id, false);
        show_pages[PAGE_ACTIONS] = true;
        show_pages[PAGE_STATE] = true;
        show_pages[PAGE_EVENT] = true;
        show_pages[PAGE_TRACK] = false;
        show_pages[PAGE_STAT] = false;

        if (savedInstanceState != null) {
            car_id = savedInstanceState.getString(Names.ID);
            current = new LocalDate(savedInstanceState.getLong(DATE));
        } else {
            car_id = getIntent().getStringExtra(Names.ID);
            if (car_id == null) {
                car_id = preferences.getString(Names.LAST, "");
                if (preferences.getBoolean(Names.Car.POINTER + car_id, false))
                    car_id = "";
            }
            car_id = Preferences.getCar(preferences, car_id);
            int inputs = preferences.getInt(Names.Car.INPUTS, -1);
            if (inputs == -1) {
                HttpTask task = new HttpTask() {
                    @Override
                    void result(JsonObject res) throws ParseException {
                        int inputs = 0;
                        if (res.get("in1") != null)
                            inputs |= 1;
                        if (res.get("in2") != null)
                            inputs |= 2;
                        if (res.get("in3") != null)
                            inputs |= 4;
                        if (res.get("in4") != null)
                            inputs |= 8;
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putInt(Names.Car.INPUTS + car_id, inputs);
                        ed.commit();
                    }

                    @Override
                    void error() {

                    }
                };
                task.execute(URL_PROFILE, preferences.getString(Names.Car.CAR_KEY, ""), "auth", preferences.getString(Names.Car.AUTH, ""));
            }
        }

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));

        update(car_id);
        cars = Cars.getCars(this);
        for (Cars.Car car : cars) {
            update(car.id);
            for (String p : car.pointers) {
                update(p);
            }
        }

        setShowTracks();
        mViewPager.setCurrentItem(getPagePosition(PAGE_STATE));

        ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                setShowDate(i);
                if ((getPageId(i) == PAGE_STATE) && (state_fragment != null))
                    state_fragment.startAnimation();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        };

        TitlePageIndicator titleIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
        if (titleIndicator != null) {
            titleIndicator.setViewPager(mViewPager);
            titleIndicator.setFooterIndicatorStyle(TitlePageIndicator.IndicatorStyle.Triangle);
            titleIndicator.setOnPageChangeListener(pageChangeListener);
        } else {
            MenuPager pager = (MenuPager) findViewById(R.id.menu);
            pager.setPager(mViewPager);
            pager.setOnPageChangeListener(pageChangeListener);
        }

        if (savedInstanceState == null) {
            String phone = preferences.getString(Names.Car.CAR_PHONE + car_id, "");
            String auth = preferences.getString(Names.Car.AUTH + car_id, "");

            if (preferences.getString(Names.CARS, "").equals("") && (auth.equals(""))) {
                firstSetup();
            } else if (auth.equals("")) {
                Intent i = new Intent(this, AuthDialog.class);
                i.putExtra(Names.ID, car_id);
                i.putExtra(Names.Car.AUTH, true);
                if (State.hasTelephony(this) && (phone.length() == 0))
                    i.putExtra(Names.Car.CAR_PHONE, true);
                startActivityForResult(i, CAR_SETUP);
            } else if (State.hasTelephony(this) && (phone.length() == 0)) {
                Intent i = new Intent(this, AuthDialog.class);
                i.putExtra(Names.ID, car_id);
                i.putExtra(Names.Car.CAR_PHONE, true);
                startActivityForResult(i, CAR_SETUP);
            } else if (!preferences.getBoolean(Names.INIT_POINTER, false)) {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(Names.INIT_POINTER, true);
                ed.commit();
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.pointer_support)
                        .setMessage(R.string.pointer_support_msg)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                for (int i = 1; ; i++) {
                                    if (!isId(i + "")) {
                                        Cars.deleteCarKeys(MainActivity.this, i + "");
                                        Intent intent = new Intent(MainActivity.this, AuthDialog.class);
                                        intent.putExtra(Names.ID, i + "");
                                        intent.putExtra(Names.Car.AUTH, true);
                                        if (State.hasTelephony(MainActivity.this))
                                            intent.putExtra(Names.Car.CAR_PHONE, true);
                                        startActivityForResult(intent, CAR_SETUP);
                                        break;
                                    }
                                }
                            }
                        })
                        .create();
                dialog.show();
            }
        }

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(FetchService.ACTION_UPDATE)) {
                    setShowTracks();
                    stopTimer();
                    startTimer(false);
                    return;
                }
                if (action.equals(FetchService.ACTION_UPDATE_FORCE)) {
                    boolean new_show_photo = preferences.getBoolean(Names.Car.SHOW_PHOTO + car_id, false);
                    if (new_show_photo != show_pages[PAGE_PHOTO]) {
                        int id = getPageId(mViewPager.getCurrentItem());
                        update();
                        mViewPager.setCurrentItem(getPagePosition(id));
                    }
                    changeDate(current.toDate());
                    return;
                }
            }
        };
        IntentFilter intFilter = new IntentFilter(FetchService.ACTION_UPDATE);
        intFilter.addAction(FetchService.ACTION_UPDATE_FORCE);
        registerReceiver(br, intFilter);

        pi = createPendingResult(REQUEST_ALARM, new Intent(), 0);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (state_fragment != null) {
            if (getPageId(mViewPager.getCurrentItem()) == PAGE_STATE)
                state_fragment.startAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(br);
        try {
            Intent i = new Intent(this, FetchService.class);
            stopService(i);
            System.gc();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerGCM();
        removeNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.gc();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
        outState.putLong(DATE, current.toDate().getTime());
    }

    int menuId() {
        return R.menu.main;
    }

    boolean isId(String id) {
        for (Cars.Car car : cars) {
            if (car.id.equals(id))
                return true;
            for (String p : car.pointers)
                if (p.equals(id))
                    return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        topSubMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId(), menu);
        MenuItem item = menu.findItem(R.id.date);
        if (show_date) {
            item.setTitle(current.toString("d MMMM"));
        } else {
            menu.removeItem(R.id.date);
        }
        if (!show_stat)
            menu.removeItem(R.id.recalc);
        if (!State.hasTelephony(this))
            menu.removeItem(R.id.passwd);
        if (State.isPandora(preferences, car_id))
            menu.removeItem(R.id.charts);
        return super.onCreateOptionsMenu(menu);
    }

    void updateMenu() {
        if (topSubMenu == null)
            return;
        topSubMenu.clear();
        onCreateOptionsMenu(topSubMenu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preferences: {
                Intent intent = new Intent(this, SettingActivity.class);
                intent.putExtra(Names.ID, car_id);
                startActivity(intent);
                return true;
            }
            case R.id.cars: {
                Intent intent = new Intent(getBaseContext(), Cars.class);
                startActivity(intent);
                return true;
            }
            case R.id.charts: {
                Intent intent = new Intent(getBaseContext(), HistoryActivity.class);
                intent.putExtra(Names.ID, car_id);
                intent.putExtra(Names.STATE, "voltage");
                startActivity(intent);
                return true;
            }
            case R.id.passwd:
                setPassword();
                return true;
            case R.id.recalc: {
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.recalc_stat)
                        .setMessage(R.string.recalc_message)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, null)
                        .create();
                dialog.show();
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (stat_fragment != null)
                            stat_fragment.recalc();
                        dialog.dismiss();
                    }
                });
                return true;
            }

/*
            case R.id.log: {
                HttpTask task = new HttpTask() {
                    @Override
                    void result(JsonObject res) throws ParseException {
                        State.appendLog(res.toString());
                    }

                    @Override
                    void error() {
                        State.appendLog("log error");
                    }
                };

                String api_key = preferences.getString(Names.Car.CAR_KEY + car_id, "");
                task.execute(FetchService.STATUS_URL, api_key);
                return true;
            }
*/

            case R.id.about: {
                Intent intent = new Intent(this, About.class);
                startActivity(intent);
                return true;
            }
            case R.id.date: {
                caldroidFragment = new CaldroidFragment() {

                    @Override
                    public void onAttach(Activity activity) {
                        super.onAttach(activity);
                        CaldroidListener listener = new CaldroidListener() {

                            @Override
                            public void onSelectDate(Date date, View view) {
                                changeDate(date);
                                caldroidFragment.dismiss();
                            }
                        };
                        setCaldroidListener(listener);
                    }

                };
                Bundle args = new Bundle();
                args.putString(CaldroidFragment.DIALOG_TITLE, getString(R.string.day));
                args.putInt(CaldroidFragment.MONTH, current.getMonthOfYear());
                args.putInt(CaldroidFragment.YEAR, current.getYear());
                args.putInt(CaldroidFragment.START_DAY_OF_WEEK, 1);
                caldroidFragment.setArguments(args);
                LocalDateTime now = new LocalDateTime();
                caldroidFragment.setMaxDate(now.toDate());
                long first = preferences.getLong(Names.Car.FIRST_TIME + car_id, 0);
                if (first > 0)
                    caldroidFragment.setMinDate(new Date(first));
                Date sel = current.toDate();
                caldroidFragment.setSelectedDates(sel, sel);
                caldroidFragment.show(getSupportFragmentManager(), "TAG");
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        setActionBar();
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
        if (requestCode == CAR_SETUP) {
            String key = preferences.getString(Names.Car.CAR_KEY + car_id, "");
            if (key.length() == 0)
                finish();
        }
        if (requestCode == REQUEST_ALARM) {
            if (!powerMgr.isScreenOn())
                return;
            Intent intent = new Intent(this, FetchService.class);
            intent.putExtra(Names.ID, car_id);
            startService(intent);
            return;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String id = intent.getStringExtra(Names.ID);
        if (id != null)
            setCar(id);
        mViewPager.setCurrentItem(preferences.getBoolean(Names.Car.SHOW_PHOTO + car_id, false) ? 2 : 1);
        removeNotifications();
    }

    void setCar(String id) {
        id = Preferences.getCar(preferences, id);
        if (id.equals(car_id))
            return;
        try {
            int current_id = getPageId(mViewPager.getCurrentItem());
            car_id = id;
            setActionBar();
            update();
            updateMenu();
            int current = getPagePosition(current_id);
            mViewPager.setCurrentItem(current);
            setShowDate(current);
        } catch (Exception ex) {
            ex.printStackTrace();
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

    private void removeNotifications() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String n_ids = preferences.getString(Names.Car.N_IDS + car_id, "");
        if (n_ids.equals(""))
            return;
        int id_valet_on = preferences.getInt(Names.Car.VALET_ON_NOTIFY + car_id, 0);
        int id_lost = preferences.getInt(Names.Car.LOST_NOTIFY + car_id, 0);
        String[] ids = n_ids.split(",");
        for (String id : ids) {
            try {
                int current_id = Integer.parseInt(id);
                if ((current_id != id_valet_on) && (current_id != id_lost))
                    manager.cancel(current_id);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        SharedPreferences.Editor ed = preferences.edit();
        if (id_valet_on != 0) {
            ed.putString(Names.Car.N_IDS + car_id, id_valet_on + "");
        } else {
            ed.remove(Names.Car.N_IDS + car_id);
        }
        Field[] fields = Names.Notify.class.getDeclaredFields();
        for (Field f : fields) {
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                continue;
            try {
                String val = (String) f.get(Names.Notify.class);
                ed.remove(val + car_id);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        ed.commit();
    }

    Cars.Car[] getCars() {
        return Cars.getCars(this);
    }

    void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        cars = getCars();
        setCar(car_id);
        if (cars.length > 1) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(new CarsAdapter(), new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int i, long l) {
                    String id = Preferences.getCar(preferences, cars[i].id);
                    if (id.equals(car_id))
                        return true;
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putString(Names.LAST, id);
                    ed.commit();
                    setCar(id);
                    removeNotifications();
                    return true;
                }
            });
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayUseLogoEnabled(false);
            for (int i = 0; i < cars.length; i++) {
                if (cars[i].id.equals(car_id)) {
                    actionBar.setSelectedNavigationItem(i);
                    break;
                }
            }
            setTitle("");
        } else {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayUseLogoEnabled(false);
            setTitle(getString(R.string.app_name));
        }
    }

    int getPageId(int n) {
        int last = 0;
        for (int i = 0; i < 6; i++) {
            if (!show_pages[i])
                continue;
            last = i;
            if (n == 0)
                return i;
            n--;
        }
        return last;
    }

    int getPagePosition(int id) {
        int pos = 0;
        for (int i = 0; i < id; i++) {
            if (show_pages[i])
                pos++;
        }
        return pos;
    }

    void setShowTracks() {
        boolean changed = false;
        boolean show_tracks = (preferences.getFloat(Names.Car.LAT + car_id, 0) != 0) ||
                (preferences.getFloat(Names.Car.LNG + car_id, 0) != 0);
        pointer = preferences.getBoolean(Names.Car.POINTER + car_id, false);
        if (pointer)
            show_tracks = false;
        boolean show_actions = !pointer;
        if (show_pages[PAGE_ACTIONS] != show_actions) {
            show_pages[PAGE_ACTIONS] = show_actions;
            changed = true;
        }
        if (show_pages[PAGE_TRACK] != show_tracks) {
            show_pages[PAGE_TRACK] = show_tracks;
            show_pages[PAGE_STAT] = show_tracks;
            changed = true;
        }
        boolean show_photo = preferences.getBoolean(Names.Car.SHOW_PHOTO + car_id, false);
        if (show_pages[PAGE_PHOTO] != show_photo) {
            show_pages[PAGE_PHOTO] = show_photo;
            changed = true;
        }
        if (changed) {
            try {
                mViewPager.getAdapter().notifyDataSetChanged();
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    void setShowDate(int i) {
        int id = getPageId(i);
        boolean new_show_date = (id == PAGE_PHOTO) || (id == PAGE_EVENT) || (id == PAGE_TRACK);
        if (pointer && (id == PAGE_EVENT))
            new_show_date = false;
        boolean new_show_stat = (id == PAGE_STAT);
        if ((new_show_date != show_date) || (new_show_stat != show_stat)) {
            show_date = new_show_date;
            show_stat = new_show_stat;
            updateMenu();
        }
    }

    void update(String id) {
        Intent intent = new Intent(this, FetchService.class);
        intent.putExtra(Names.ID, id);
        startService(intent);
    }

    void update() {
        final int cur = mViewPager.getCurrentItem();
        setShowTracks();
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mViewPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
                mViewPager.setCurrentItem(cur);
                Intent intent = new Intent(MainActivity.this, FetchService.class);
                intent.putExtra(Names.ID, car_id);
                startService(intent);
            }
        };
        task.execute();
    }

    void changeDate(Date d) {
        current = new LocalDate(d);
        updateMenu();
        for (DateChangeListener listener : dateChangeListenerSet) {
            listener.dateChanged(current);
        }
    }

    void registerDateListener(DateChangeListener listener) {
        dateChangeListenerSet.add(listener);
    }

    void unregisterDateListener(DateChangeListener listener) {
        dateChangeListenerSet.remove(listener);
    }

    void setPassword() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.password)
                .setMessage(R.string.password_summary)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .setView(inflater.inflate(R.layout.setpassword, null))
                .create();
        dialog.show();
        final String password = preferences.getString(Names.PASSWORD, "");
        final EditText etOldPswd = (EditText) dialog.findViewById(R.id.old_password);
        if (password.length() == 0) {
            TextView tvOldLabel = (TextView) dialog.findViewById(R.id.old_password_label);
            tvOldLabel.setVisibility(View.GONE);
            etOldPswd.setVisibility(View.GONE);
        }
        final EditText etPasswd1 = (EditText) dialog.findViewById(R.id.password);
        final EditText etPasswd2 = (EditText) dialog.findViewById(R.id.password1);
        final TextView tvConfrim = (TextView) dialog.findViewById(R.id.invalid_confirm);
        final Button btnSave = dialog.getButton(Dialog.BUTTON_POSITIVE);
        final Context context = this;

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (etPasswd1.getText().toString().equals(etPasswd2.getText().toString())) {
                    tvConfrim.setVisibility(View.INVISIBLE);
                    btnSave.setEnabled(true);
                } else {
                    tvConfrim.setVisibility(View.VISIBLE);
                    btnSave.setEnabled(false);
                }
            }
        };

        etPasswd1.addTextChangedListener(watcher);
        etPasswd2.addTextChangedListener(watcher);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!password.equals(etOldPswd.getText().toString())) {
                    Actions.showMessage(context, R.string.password, R.string.invalid_password);
                    return;
                }
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.PASSWORD, etPasswd1.getText().toString());
                ed.commit();
                dialog.dismiss();
            }
        });
    }

    void registerGCM() {
        if (!checkPlayServices())
            return;
        gcm = GoogleCloudMessaging.getInstance(this);
        String reg_id = preferences.getString(Names.GCM_ID, "");
        long gcm_time = preferences.getLong(Names.GCM_TIME, 0);
        if (preferences.getInt(Names.GCM_VERSION, 0) != VERSION)
            reg_id = "";
        if (!reg_id.equals("") && (gcm_time > new Date().getTime() - 86400 * 1000))
            return;
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String reg = null;
                Reader reader = null;
                HttpURLConnection connection = null;
                try {
                    if (gcm == null)
                        gcm = GoogleCloudMessaging.getInstance(MainActivity.this);
                    reg = gcm.register(SENDER_ID);
                    JsonObject data = new JsonObject();
                    data.add("reg", reg);
                    Cars.Car[] cars = Cars.getCars(MainActivity.this);
                    String d = null;
                    for (Cars.Car car : cars) {
                        String key = preferences.getString(Names.Car.CAR_KEY + car.id, "");
                        if (key.equals(""))
                            continue;
                        JsonObject c = new JsonObject();
                        if (d != null) {
                            d += "|" + key;
                        } else {
                            d = key;
                        }
                        d += ";" + car.id;
                        String phone = preferences.getString(Names.Car.CAR_PHONE + car.id, "");
                        if (!phone.equals(""))
                            d += ";" + phone;
                    }
                    data.add("cars", d);
                    Calendar cal = Calendar.getInstance();
                    TimeZone tz = cal.getTimeZone();
                    data.add("tz", tz.getID());
                    String phone = "";
                    TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    try {
                        phone = tm.getLine1Number();
                    } catch (Exception ex) {
                        // ignore
                    }
                    if (!phone.equals(""))
                        data.add("phone", phone);
                    String url = "https://car-online.ugona.net/reg";
                    RequestBody body = RequestBody.create(MediaType.parse("application/json"), data.toString());
                    Request request = new Request.Builder().url(url).post(body).build();
                    Response response = HttpTask.client.newCall(request).execute();
                    if (response.code() != HttpURLConnection.HTTP_OK)
                        return null;
                    reader = response.body().charStream();
                    JsonObject res = JsonValue.readFrom(reader).asObject();
                    if (res.asObject().get("error") != null)
                        return null;
                } catch (Exception ex) {
                    return null;
                } finally {
                    if (connection != null)
                        connection.disconnect();
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
                return reg;
            }

            @Override
            protected void onPostExecute(String s) {
                if (s == null)
                    return;
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.GCM_ID, s);
                ed.putLong(Names.GCM_TIME, new Date().getTime());
                ed.putInt(Names.GCM_VERSION, VERSION);
                ed.commit();
            }

        }.execute();
    }

    boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (preferences.getBoolean(Names.NO_GOOGLE, false))
                return false;
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(Names.NO_GOOGLE, true);
                ed.commit();
            }
            return false;
        }

        return true;
    }

    void firstSetup() {
        auth_progress = new ProgressDialog(this);
        auth_progress.setMessage(getString(R.string.init));
        auth_progress.show();
        auth_progress.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                auth_progress = null;
                String auth = preferences.getString(Names.Car.AUTH + car_id, "");
                String phone = preferences.getString(Names.Car.CAR_PHONE + car_id, "");
                if (auth.equals("")) {
                    Intent i = new Intent(MainActivity.this, AuthDialog.class);
                    i.putExtra(Names.ID, car_id);
                    i.putExtra(Names.Car.AUTH, true);
                    if (State.hasTelephony(MainActivity.this) && (phone.length() == 0))
                        i.putExtra(Names.Car.CAR_PHONE, true);
                    startActivityForResult(i, CAR_SETUP);
                }
            }
        });

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Uri uri = Uri.parse("content://sms/inbox");
                    Cursor cur = getContentResolver().query(uri, new String[]{"_id", "address", "body"}, null, null, null);
                    auth_data = new Vector<AuthData>();
                    if (cur.moveToFirst()) {
                        Pattern pat = Pattern.compile("www.car-online.ru \\(login:([A-Za-z0-9]+),password:([A-Za-z0-9]+)\\)");
                        while (cur.moveToNext()) {
                            try {
                                String body = cur.getString(2);
                                Matcher matcher = pat.matcher(body);
                                if (!matcher.find())
                                    continue;
                                AuthData auth = new AuthData();
                                auth.phone = cur.getString(1);
                                auth.login = matcher.group(1);
                                auth.pass = matcher.group(2);
                                auth_data.add(auth);
                            } catch (Exception ex) {
                                // ignore
                            }
                        }
                    }
                } catch (Exception ex) {
                    // ignore
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                checkAuth();
            }
        };
        task.execute();
    }

    void checkAuth() {
        if (auth_progress == null)
            return;
        for (final AuthData data : auth_data) {
            if (data.checked)
                continue;
            data.checked = true;
            HttpTask task = new HttpTask() {
                @Override
                void result(JsonObject res) throws ParseException {
                    data.key = res.get("key").asString();
                    HttpTask verTask = new HttpTask() {
                        @Override
                        void result(JsonObject res) throws ParseException {
                            data.version = res.get("version").asString();
                            HttpTask photoTask = new HttpTask() {
                                @Override
                                void result(JsonObject res) throws ParseException {
                                    JsonArray array = res.get("photos").asArray();
                                    data.photo = array.size() > 0;
                                    checkAuth();
                                }

                                @Override
                                void error() {

                                }
                            };
                            long start = new Date().getTime() - 3 * 86400000;
                            photoTask.execute(URL_PHOTOS, data.key, start);
                        }

                        @Override
                        void error() {
                            data.key = null;
                            checkAuth();
                        }
                    };
                    data.auth = res.get("auth").asString();
                    verTask.execute(URL_PROFILE, data.key);
                }

                @Override
                void error() {
                    checkAuth();
                }
            };
            task.execute(URL_KEY, data.login, data.pass);
            return;
        }
        int id = 0;
        SharedPreferences.Editor ed = preferences.edit();
        String cars = "";
        for (AuthData data : auth_data) {
            if (data.key == null)
                continue;
            if (data.version.toUpperCase().substring(0, 5).equals("MS-TR"))
                continue;
            String c_id = id + "";
            if (id == 0) {
                c_id = "";
            } else {
                cars += ",";
            }
            cars += c_id;
            id++;
            ed.putString(Names.Car.CAR_PHONE + c_id, data.phone);
            ed.putString(Names.Car.CAR_KEY + c_id, data.key);
            ed.putString(Names.Car.LOGIN + c_id, data.login);
            ed.putString(Names.Car.AUTH + c_id, data.auth);
            if (data.photo)
                ed.putBoolean(Names.Car.SHOW_PHOTO + c_id, true);
        }
        String pointers = "";
        int p_id = 0;
        for (AuthData data : auth_data) {
            if (data.key == null)
                continue;
            if (!data.version.toUpperCase().substring(0, 5).equals("MS-TR"))
                continue;
            String c_id = id + "";
            if (id == 0)
                c_id = "";
            id++;
            String name = getString(R.string.pointer);
            if (p_id++ > 0)
                name += p_id;
            ed.putString(Names.Car.CAR_NAME + c_id, name);
            ed.putString(Names.Car.CAR_PHONE + c_id, data.phone);
            ed.putString(Names.Car.CAR_KEY + c_id, data.key);
            ed.putString(Names.Car.LOGIN + c_id, data.login);
            ed.putString(Names.Car.AUTH + c_id, data.auth);
            ed.putBoolean(Names.Car.POINTER + c_id, true);
            if (!pointers.equals(""))
                pointers += ",";
            pointers += c_id;
        }
        if (!pointers.equals("")) {
            ed.putString(Names.Car.POINTERS, pointers);
            ed.putBoolean(Names.INIT_POINTER, true);
        }
        ed.putString(Names.CARS, cars);
        ed.commit();
        for (int i = 0; i < id; i++) {
            Intent intent = new Intent(this, FetchService.class);
            String c_id = i + "";
            if (i == 0)
                c_id = "";
            intent.putExtra(Names.ID, c_id);
            startService(intent);
            intent = new Intent(FetchService.ACTION_UPDATE_FORCE);
            intent.putExtra(Names.ID, c_id);
            sendBroadcast(intent);
        }
        auth_progress.dismiss();
        registerGCM();
    }

    static interface DateChangeListener {
        abstract void dateChanged(LocalDate current);
    }

    static class AuthData {
        String phone;
        String login;
        String pass;
        String key;
        String version;
        String auth;
        boolean checked;
        boolean photo;
    }

    class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Log.v("v", "getItem " + i + ", " + getPageId(i));
            switch (getPageId(i)) {
                case PAGE_PHOTO: {
                    PhotoFragment fragment = new PhotoFragment();
                    fragment.car_id = car_id;
                    fragment.current = current;
                    return fragment;
                }
                case PAGE_ACTIONS: {
                    ActionFragment fragment = new ActionFragment();
                    fragment.car_id = car_id;
                    return fragment;
                }
                case PAGE_STATE: {
                    StateFragment fragment = new StateFragment();
                    fragment.car_id = car_id;
                    state_fragment = fragment;
                    return fragment;
                }
                case PAGE_EVENT: {
                    EventsFragment fragment = new EventsFragment();
                    fragment.car_id = car_id;
                    fragment.current = current;
                    return fragment;
                }
                case PAGE_TRACK: {
                    TracksFragment fragment = new TracksFragment();
                    fragment.car_id = car_id;
                    fragment.current = current;
                    return fragment;
                }
                case PAGE_STAT: {
                    StatFragment fragment = new StatFragment();
                    fragment.car_id = car_id;
                    stat_fragment = fragment;
                    return fragment;
                }
            }
            return null;
        }

        @Override
        public int getCount() {
            return getPagePosition(6);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (getPageId(position)) {
                case PAGE_PHOTO:
                    return getString(R.string.photo);
                case PAGE_ACTIONS:
                    return getString(R.string.control);
                case PAGE_STATE:
                    return getString(R.string.state);
                case PAGE_EVENT:
                    return getString(R.string.events);
                case PAGE_TRACK:
                    return getString(R.string.tracks);
                case PAGE_STAT:
                    return getString(R.string.stat);
            }
            return super.getPageTitle(position);
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

