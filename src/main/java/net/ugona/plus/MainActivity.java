package net.ugona.plus;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import org.joda.time.LocalDate;

import java.util.Date;

import static android.view.Gravity.START;

public class MainActivity extends ActionBarActivity {

    static final int DO_AUTH = 1;
    static final int DO_PHONE = 2;

    static final int PAGE_PHOTO = 0;
    static final int PAGE_ACTIONS = 1;
    static final int PAGE_STATE = 2;
    static final int PAGE_EVENT = 3;
    static final int PAGE_TRACK = 4;
    static final int PAGE_STAT = 5;

    String id;
    AppConfig config;
    CarState state;
    CarConfig car_config;
    PagerSlidingTabStrip tabs;
    ViewPager vPager;
    Menu topSubMenu;
    Menu sideMenu;
    LocalDate current;
    DrawerLayout drawer;
    private DrawerArrowDrawable drawerArrowDrawable;
    private float offset;
    private boolean flipped;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                State.print(ex);
                System.exit(1);
            }
        });

        super.onCreate(savedInstanceState);
        config = AppConfig.get(this);
        id = config.getId(getIntent().getStringExtra(Names.ID));
        car_config = CarConfig.get(this, id);
        state = CarState.get(this, id);

        setContentView(R.layout.main);
        current = new LocalDate();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ImageView imageView = (ImageView) findViewById(R.id.drawer_indicator);
        final Resources resources = getResources();

        drawerArrowDrawable = new DrawerArrowDrawable(resources);
        drawerArrowDrawable.setStrokeColor(getResources().getColor(android.R.color.white));
        imageView.setImageDrawable(drawerArrowDrawable);

        drawer.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                offset = slideOffset;

                // Sometimes slideOffset ends up so close to but not quite 1 or 0.
                if (slideOffset >= .995) {
                    flipped = true;
                    drawerArrowDrawable.setFlip(flipped);
                } else if (slideOffset <= .005) {
                    flipped = false;
                    drawerArrowDrawable.setFlip(flipped);
                }

                drawerArrowDrawable.setParameter(offset);
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawer.isDrawerVisible(START)) {
                    drawer.closeDrawer(START);
                } else {
                    drawer.openDrawer(START);
                }
            }
        });

        setupActionBar();
        setSideMenu();

        vPager = (ViewPager) findViewById(R.id.pager);
        vPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setViewPager(vPager);
        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                updateMenu();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        vPager.setCurrentItem(getPagePosition(PAGE_STATE));

        if (car_config.getAuth().equals("")) {
            Intent intent = new Intent(this, AuthDialog.class);
            intent.putExtra(Names.ID, id);
            startActivityForResult(intent, DO_AUTH);
            return;
        }
        checkCaps();
        if (checkPhone())
            return;
    }

    @Override
    protected void onDestroy() {
        AppConfig.save(this);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DO_AUTH) {
            CarConfig carConfig = CarConfig.get(this, id);
            if (carConfig.getKey().equals("")) {
                finish();
                return;
            }
            updatePages();
            if (checkPhone())
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        topSubMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.date);
        MainFragment fragment = getFragment(0);
        if (fragment.isShowDate()) {
            item.setTitle(current.toString("d MMMM"));
        } else {
            menu.removeItem(R.id.date);
        }
        Menu sub_menu = fragment.menu();
        if (sub_menu != null) {
            for (int i = 0; i < sub_menu.size(); i++) {
                MenuItem it = sub_menu.getItem(i);
                menu.add(it.getGroupId(), it.getItemId(), it.getOrder(), it.getTitle());
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MainFragment fragment = getFragment(0);
        if ((fragment != null) && fragment.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.date: {
                final CalendarDatePickerDialog dialog = new CalendarDatePickerDialog() {
                    @Override
                    public void onDayOfMonthSelected(int year, int month, int day) {
                        super.onDayOfMonthSelected(year, month, day);
                        getView().findViewById(R.id.done).performClick();
                    }
                };
                dialog.initialize(new CalendarDatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(CalendarDatePickerDialog calendarDatePickerDialog, int i, int i2, int i3) {
                        current = new LocalDate(i, i2 + 1, i3);
                        updateMenu();
                        MainFragment fragment = getFragment(-1);
                        if (fragment != null)
                            fragment.changeDate();
                        fragment = getFragment(0);
                        if (fragment != null)
                            fragment.changeDate();
                        fragment = getFragment(1);
                        if (fragment != null)
                            fragment.changeDate();

                    }
                }, current.getYear(), current.getMonthOfYear() - 1, current.getDayOfMonth());
                dialog.show(getSupportFragmentManager(), "DATE_PICKER_TAG");
                return true;
            }
            case R.id.about:
                startActivity(new Intent(this, About.class));
                return true;
            case R.id.passwd:
                startActivity(new Intent(this, SetPassword.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void setSideMenu() {
        PopupMenu p = new PopupMenu(this, null);
        sideMenu = p.getMenu();
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sidebar, sideMenu);
        ListView lMenu = (ListView) findViewById(R.id.sidemenu);
        lMenu.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return sideMenu.size();
            }

            @Override
            public Object getItem(int position) {
                return sideMenu.getItem(position);
            }

            @Override
            public long getItemId(int position) {
                return sideMenu.getItem(position).getItemId();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.menu_item, null);
                }
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(sideMenu.getItem(position).getTitle());
                return v;
            }
        });
        lMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                drawer.closeDrawer(START);
                onOptionsItemSelected(sideMenu.getItem(position));
            }
        });
    }

    void updateMenu() {
        if (topSubMenu == null)
            return;
        topSubMenu.clear();
        onCreateOptionsMenu(topSubMenu);
    }

    boolean checkPhone() {
        CarState state = CarState.get(this, id);
        if (!state.isUse_phone() || !state.getPhone().equals("") || !State.hasTelephony(this))
            return false;
        Intent intent = new Intent(this, PhoneDialog.class);
        intent.putExtra(Names.ID, id);
        startActivityForResult(intent, DO_PHONE);
        return true;
    }

    MainFragment getFragment(int position) {
        FragmentStatePagerAdapter adapter = (FragmentStatePagerAdapter) vPager.getAdapter();
        return (MainFragment) adapter.instantiateItem(vPager, vPager.getCurrentItem() + position);
    }

    void setupActionBar() {
        Spinner spinner = (Spinner) findViewById(R.id.spinner_nav);
        TextView title = (TextView) findViewById(R.id.title);

        final String[] cars = config.getCars();

        if (cars.length <= 1) {
            spinner.setVisibility(View.GONE);
            title.setVisibility(View.VISIBLE);
            return;
        }
        spinner.setVisibility(View.VISIBLE);
        title.setVisibility(View.GONE);
        spinner.setAdapter(new BaseAdapter() {
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
                return getView(position, convertView, R.layout.car_list_item);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return getView(position, convertView, R.layout.car_list_dropdown_item);
            }

            public View getView(int position, View convertView, int layout_id) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(layout_id, null);
                }
                TextView tv = (TextView) v.findViewById(R.id.name);
                CarConfig config = CarConfig.get(MainActivity.this, cars[position]);
                tv.setText(config.getName(MainActivity.this, cars[position]));
                return v;
            }

        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }


    boolean isShowPage(int id) {
        switch (id) {
            case PAGE_PHOTO:
                return state.isShow_photo();
            case PAGE_ACTIONS:
            case PAGE_STATE:
            case PAGE_EVENT:
                return true;
            case PAGE_TRACK:
            case PAGE_STAT:
                return state.isShow_tracks();
        }
        return false;
    }

    int getPageId(int n) {
        int last = 0;
        for (int i = 0; i < 6; i++) {
            if (!isShowPage(i))
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
            if (isShowPage(i))
                pos++;
        }
        return pos;
    }

    void checkCaps() {
        final String version = State.getVersion(this);
        Date now = new Date();
        final long time = now.getTime();
        if (version.equals(state.getVersion()) && (state.getCheck_time() > time))
            return;
        HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                if (CarState.update(state, res.get("caps").asObject()))
                    updatePages();
                car_config.update(car_config, res);
                state.setCheck_time(time + 86400000);
                state.setVersion(version);
            }

            @Override
            void error() {

            }
        };
        KeyParam skey = new KeyParam();
        skey.skey = car_config.getKey();
        task.execute("/caps", skey);
    }

    void updatePages() {
        int id = getPageId(vPager.getCurrentItem());
        vPager.getAdapter().notifyDataSetChanged();
        if (tabs != null)
            tabs.notifyDataSetChanged();
        vPager.setCurrentItem(getPagePosition(id));
    }

    static class KeyParam {
        String skey;
    }

    class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Log.v("v", "getItem " + i + ", " + getPageId(i));
            MainFragment fragment = null;
            switch (getPageId(i)) {
                case PAGE_PHOTO:
                    fragment = new PhotoFragment();
                    break;
                case PAGE_ACTIONS:
                    fragment = new ActionFragment();
                    break;
                case PAGE_STATE:
                    fragment = new StateFragment();
                    break;
                case PAGE_EVENT:
                    fragment = new EventsFragment();
                    break;
                case PAGE_TRACK:
                    fragment = new TracksFragment();
                    break;
                case PAGE_STAT:
                    fragment = new StatFragment();
                    break;
            }
            return fragment;
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
}
