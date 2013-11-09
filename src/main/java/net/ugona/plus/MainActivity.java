package net.ugona.plus;

import android.app.Activity;
import android.app.AlarmManager;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.romorama.caldroid.CaldroidFragment;
import com.romorama.caldroid.CaldroidListener;
import com.viewpagerindicator.TitlePageIndicator;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

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
    LocalDate current;
    Menu topSubMenu;

    boolean active;

    CaldroidFragment caldroidFragment;

    Set<DateChangeListener> dateChangeListenerSet;

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

        TitlePageIndicator titleIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
        titleIndicator.setViewPager(mViewPager);
        titleIndicator.setFooterIndicatorStyle(TitlePageIndicator.IndicatorStyle.Triangle);

        titleIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                boolean new_show_date = (i >= 2);
                if (new_show_date != show_date) {
                    show_date = new_show_date;
                    updateMenu();
                }
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
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(br);
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
                Intent intent = new Intent(this, Preferences.class);
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
        removeNotifications();
        String id = intent.getStringExtra(Names.ID);
        if (id != null) {
            id = Preferences.getCar(preferences, id);
            if (!id.equals(car_id)) {
                car_id = id;
                setActionBar();
                update();
            }
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
        int id = preferences.getInt(Names.IDS, 0);
        for (int i = 1; i <= id; i++) {
            try {
                manager.cancel(i);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(Names.IDS, 0);
        ed.commit();
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
        if (!found) {
            car_id = cars[0].id;
            update();
        }
        if (cars.length > 1) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(new CarsAdapter(), new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int i, long l) {
                    if (cars[i].id.equals(car_id))
                        return true;
                    car_id = cars[i].id;
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putString(Names.LAST, car_id);
                    ed.commit();
                    update();
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
        boolean new_show_tracks = !preferences.getString(Names.LATITUDE + car_id, "").equals("");
        if (show_tracks != new_show_tracks) {
            show_tracks = new_show_tracks;
            mViewPager.getAdapter().notifyDataSetChanged();
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
            switch (i) {
                case 0:
                    return new ActionFragment();
                case 1: {
                    StateFragment fragment = new StateFragment();
                    fragment.car_id = car_id;
                    return fragment;
                }
                case 2: {
                    EventsFragment fragment = new EventsFragment();
                    fragment.car_id = car_id;
                    fragment.current = current;
                    return fragment;
                }
                case 3: {
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
            return show_tracks ? 4 : 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.actions);
                case 1:
                    return getString(R.string.state);
                case 2:
                    return getString(R.string.events);
                case 3:
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

}
