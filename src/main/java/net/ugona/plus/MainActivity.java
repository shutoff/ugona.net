package net.ugona.plus;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

import com.romorama.caldroid.CaldroidFragment;
import com.romorama.caldroid.CaldroidListener;
import com.viewpagerindicator.TitlePageIndicator;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends ActionBarActivity {

    static final int REQUEST_ALARM = 4000;
    static final int CAR_SETUP = 4001;

    static final int UPDATE_INTERVAL = 30 * 1000;

    static final String DATE = "date";

    ViewPager mViewPager;
    SharedPreferences preferences;
    BroadcastReceiver br;
    AlarmManager alarmMgr;
    PendingIntent pi;

    String car_id;
    Cars.Car[] cars;

    boolean show_date;
    boolean show_tracks;
    boolean show_photo;

    LocalDate current;
    Menu topSubMenu;

    boolean active;

    StateFragment state_fragment;

    CaldroidFragment caldroidFragment;

    Set<DateChangeListener> dateChangeListenerSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                State.print(ex);
                ex.printStackTrace();
            }
        });

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

        mViewPager.setCurrentItem(1);
        setShowTracks();
        mViewPager.setCurrentItem(preferences.getBoolean(Names.SHOW_PHOTO + car_id, false) ? 2 : 1);

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
                if (!show_photo)
                    i++;
                if ((i == 2) && (state_fragment != null))
                    state_fragment.startAnimation();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        if (savedInstanceState == null) {
            String phone = preferences.getString(Names.CAR_PHONE + car_id, "");
            String key = preferences.getString(Names.CAR_KEY + car_id, "");
            if ((phone.length() == 0) || (key.length() == 0)) {
                Intent intent = new Intent(this, CarPreferences.class);
                intent.putExtra(Names.ID, car_id);
                startActivityForResult(intent, CAR_SETUP);
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
                    if (new_show_photo != show_photo) {
                        int id = mViewPager.getCurrentItem();
                        if (!show_photo)
                            id++;
                        update();
                        if (!show_photo)
                            id--;
                        if (id < 0)
                            id = 0;
                        mViewPager.setCurrentItem(id);
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
            int item = mViewPager.getCurrentItem();
            if (!show_photo)
                item++;
            if (item == 2)
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
        removeNotifications();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
        outState.putLong(DATE, current.toDate().getTime());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        topSubMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.date);
        if (show_date) {
            item.setTitle(current.toString("d MMMM"));
        } else {
            menu.removeItem(R.id.date);
        }
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
            case R.id.log: {
                HttpTask task = new HttpTask() {
                    @Override
                    void result(JSONObject res) throws JSONException {
                        State.appendLog(res.toString());
                    }

                    @Override
                    void error() {
                        State.appendLog("get status error");
                    }
                };
                String key = preferences.getString(Names.CAR_KEY + car_id, "");
                task.execute(FetchService.STATUS_URL, key);
                return true;
            }
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

    void setCar(String id) {
        id = Preferences.getCar(preferences, id);
        if (id.equals(car_id))
            return;
        int current = mViewPager.getCurrentItem();
        if (!preferences.getBoolean(Names.SHOW_PHOTO + car_id, false))
            current++;
        car_id = id;
        if (!preferences.getBoolean(Names.SHOW_PHOTO + car_id, false))
            current--;
        if (current < 0)
            current = 0;
        setActionBar();
        update();
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
        ed.remove(Names.N_IDS);
        ed.commit();
    }

    void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        cars = Cars.getCars(this);
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

    void setShowTracks() {
        boolean changed = false;
        boolean new_show_tracks = !preferences.getString(Names.LATITUDE + car_id, "").equals("");
        if (show_tracks != new_show_tracks) {
            show_tracks = new_show_tracks;
            changed = true;
        }
        boolean new_show_photo = preferences.getBoolean(Names.SHOW_PHOTO + car_id, false);
        if (show_photo != new_show_photo) {
            show_photo = new_show_photo;
            changed = true;
        }
        if (changed)
            mViewPager.getAdapter().notifyDataSetChanged();
    }

    void setShowDate(int i) {
        if (!show_photo)
            i++;
        boolean new_show_date = ((i >= 3) || (i == 0));
        if (new_show_date != show_date) {
            show_date = new_show_date;
            updateMenu();
        }
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
            switch (id(i)) {
                case 0: {
                    PhotoFragment fragment = new PhotoFragment();
                    fragment.car_id = car_id;
                    fragment.current = current;
                    return fragment;
                }
                case 1: {
                    ActionFragment fragment = new ActionFragment();
                    fragment.car_id = car_id;
                    return fragment;
                }
                case 2: {
                    StateFragment fragment = new StateFragment();
                    fragment.car_id = car_id;
                    state_fragment = fragment;
                    return fragment;
                }
                case 3: {
                    EventsFragment fragment = new EventsFragment();
                    fragment.car_id = car_id;
                    fragment.current = current;
                    return fragment;
                }
                case 4: {
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
            int count = 3;
            if (show_photo)
                count++;
            if (show_tracks)
                count++;
            return count;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (id(position)) {
                case 0:
                    return getString(R.string.photo);
                case 1:
                    return getString(R.string.control);
                case 2:
                    return getString(R.string.state);
                case 3:
                    return getString(R.string.events);
                case 4:
                    return getString(R.string.tracks);

            }
            return super.getPageTitle(position);
        }

        int id(int position) {
            if (!show_photo)
                position++;
            return position;
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

}
