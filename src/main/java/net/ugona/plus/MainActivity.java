package net.ugona.plus;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.romorama.caldroid.CaldroidFragment;
import com.romorama.caldroid.CaldroidListener;
import com.viewpagerindicator.TitlePageIndicator;

import org.apache.http.HttpStatus;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

    static final int VERSION = 1;
    static final String SENDER_ID = "915289471784";

    ViewPager mViewPager;
    SharedPreferences preferences;
    BroadcastReceiver br;
    AlarmManager alarmMgr;
    PendingIntent pi;

    String car_id;
    Cars.Car[] cars;

    boolean show_date;
    boolean pointer;

    boolean[] show_pages;

    LocalDate current;
    Menu topSubMenu;

    boolean active;

    StateFragment state_fragment;

    CaldroidFragment caldroidFragment;

    Set<DateChangeListener> dateChangeListenerSet;

    GoogleCloudMessaging gcm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

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

        setContentView(R.layout.main);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        current = new LocalDate();

        show_pages = new boolean[5];
        show_pages[PAGE_PHOTO] = preferences.getBoolean(Names.SHOW_PHOTO + car_id, false);
        show_pages[PAGE_ACTIONS] = State.hasTelephony(this);
        show_pages[PAGE_STATE] = true;
        show_pages[PAGE_EVENT] = true;
        show_pages[PAGE_TRACK] = false;

        if (savedInstanceState != null) {
            car_id = savedInstanceState.getString(Names.ID);
            current = new LocalDate(savedInstanceState.getLong(DATE));
        } else {
            car_id = getIntent().getStringExtra(Names.ID);
            if (car_id == null)
                car_id = preferences.getString(Names.LAST, "");
            car_id = Preferences.getCar(preferences, car_id);
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

        TitlePageIndicator titleIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
        titleIndicator.setViewPager(mViewPager);
        titleIndicator.setFooterIndicatorStyle(TitlePageIndicator.IndicatorStyle.Triangle);

        titleIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
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
        });

        if (savedInstanceState == null) {
            String phone = preferences.getString(Names.CAR_PHONE + car_id, "");
            String key = preferences.getString(Names.CAR_KEY + car_id, "");
            if ((State.hasTelephony(this) && (phone.length() == 0)) || (key.length() == 0)) {
                Intent intent = new Intent(this, CarPreferences.class);
                intent.putExtra(Names.ID, car_id);
                startActivityForResult(intent, CAR_SETUP);
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
                                        Intent intent = new Intent(MainActivity.this, CarPreferences.class);
                                        intent.putExtra(Names.ID, i + "");
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
                    boolean new_show_photo = preferences.getBoolean(Names.SHOW_PHOTO + car_id, false);
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

        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerGCM();
        removeNotifications();
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
        if (!State.hasTelephony(this))
            menu.removeItem(R.id.passwd);
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
                Intent intent = new Intent(this, CarPreferences.class);
                intent.putExtra(Names.ID, car_id);
                startActivity(intent);
                return true;
            }
            case R.id.cars: {
                Intent intent = new Intent(getBaseContext(), Cars.class);
                startActivity(intent);
                return true;
            }
            case R.id.passwd:
                setPassword();
                return true;

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

                String api_key = preferences.getString(Names.CAR_KEY + car_id, "");
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
            String key = preferences.getString(Names.CAR_KEY + car_id, "");
            if (key.length() == 0)
                finish();
        }
        if (requestCode == REQUEST_ALARM) {
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
        mViewPager.setCurrentItem(preferences.getBoolean(Names.SHOW_PHOTO + car_id, false) ? 2 : 1);
        removeNotifications();
    }

/*
    @Override
    public void onAttachedToWindow() {
        //make the activity show even the screen is locked.
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }
*/

    void setCar(String id) {
        id = Preferences.getCar(preferences, id);
        if (id.equals(car_id))
            return;
        int current_id = getPageId(mViewPager.getCurrentItem());
        car_id = id;
        setActionBar();
        update();
        int current = getPagePosition(current_id);
        mViewPager.setCurrentItem(current);
        setShowDate(current);
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
        String n_ids = preferences.getString(Names.N_IDS + car_id, "");
        if (n_ids.equals(""))
            return;
        String[] ids = n_ids.split(",");
        for (String id : ids) {
            try {
                manager.cancel(Integer.parseInt(id));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        SharedPreferences.Editor ed = preferences.edit();
        ed.remove(Names.N_IDS + car_id);
        ed.remove(Names.BALANCE_NOTIFICATION + car_id);
        ed.remove(Names.GUARD_NOTIFY + car_id);
        ed.remove(Names.MOTOR_ON_NOTIFY + car_id);
        ed.remove(Names.MOTOR_OFF_NOTIFY + car_id);
        ed.remove(Names.VALET_ON_NOTIFY + car_id);
        ed.remove(Names.VALET_OFF_NOTIFY + car_id);
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
        for (int i = 0; i < 5; i++) {
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
        boolean show_tracks = (preferences.getFloat(Names.LAT + car_id, 0) != 0) ||
                (preferences.getFloat(Names.LNG + car_id, 0) != 0);
        pointer = preferences.getBoolean(Names.POINTER + car_id, false);
        if (pointer)
            show_tracks = false;
        boolean show_actions = !pointer && State.hasTelephony(this);
        if (show_pages[PAGE_ACTIONS] != show_actions) {
            show_pages[PAGE_ACTIONS] = show_actions;
            changed = true;
        }
        if (show_pages[PAGE_TRACK] != show_tracks) {
            show_pages[PAGE_TRACK] = show_tracks;
            changed = true;
        }
        boolean show_photo = preferences.getBoolean(Names.SHOW_PHOTO + car_id, false);
        if (show_pages[PAGE_PHOTO] != show_photo) {
            show_pages[PAGE_PHOTO] = show_photo;
            changed = true;
        }
        if (changed)
            mViewPager.getAdapter().notifyDataSetChanged();
    }

    void setShowDate(int i) {
        int id = getPageId(i);
        boolean new_show_date = ((id >= 3) || (id == 0));
        if (pointer && (id == 3))
            new_show_date = false;
        if (new_show_date != show_date) {
            show_date = new_show_date;
            updateMenu();
        }
    }

    void update(String id) {
        Intent intent = new Intent(this, FetchService.class);
        intent.putExtra(Names.ID, id);
        startService(intent);
    }

    void update() {
        int cur = mViewPager.getCurrentItem();
        setShowTracks();
        mViewPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
        mViewPager.setCurrentItem(cur);
        Intent intent = new Intent(this, FetchService.class);
        intent.putExtra(Names.ID, car_id);
        startService(intent);
    }

    void changeDate(Date d) {
        current = new LocalDate(d);
        updateMenu();
        for (DateChangeListener listener : dateChangeListenerSet) {
            listener.dateChanged(current);
        }
    }

    class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
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
            }
            return null;
        }

        @Override
        public int getCount() {
            return getPagePosition(5);
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

    void registerDateListener(DateChangeListener listener) {
        dateChangeListenerSet.add(listener);
    }

    void unregisterDateListener(DateChangeListener listener) {
        dateChangeListenerSet.remove(listener);
    }

    static interface DateChangeListener {
        abstract void dateChanged(LocalDate current);
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
                        String key = preferences.getString(Names.CAR_KEY + car.id, "");
                        if (key.equals(""))
                            continue;
                        JsonObject c = new JsonObject();
                        if (d != null) {
                            d += "|" + key;
                        } else {
                            d = key;
                        }
                        d += ";" + car.id;
                    }
                    data.add("cars", d);
                    String url = "https://car-online.ugona.net/reg";
                    URL u = new URL(url);
                    connection = (HttpURLConnection) u.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Charset", "utf-8");
                    connection.setUseCaches(false);

                    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                    wr.writeBytes(data.toString());
                    wr.flush();
                    wr.close();

                    InputStream in = new BufferedInputStream(connection.getInputStream());
                    int status = connection.getResponseCode();
                    if (status != HttpStatus.SC_OK)
                        return null;
                    reader = new InputStreamReader(in);
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

}
