package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
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

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import org.joda.time.LocalDate;

import java.io.Serializable;
import java.util.Date;

import static android.view.Gravity.START;

public class MainActivity extends ActionBarActivity {

    static final int DO_AUTH = 1;
    static final int DO_PHONE = 2;

    static final String TAG = "frag_tag";
    static final String PRIMARY = "prim_tag";
    static final String SPLASH = "splash_tag";

    String id;
    AppConfig config;
    CarState state;
    CarConfig car_config;

    Menu topSubMenu;
    Menu sideMenu;
    Menu fragmentMenu;

    LocalDate current;

    DrawerLayout drawer;
    ActionBarDrawerToggle drawerToggle;
    View vLogo;
    Spinner spinner;
    BroadcastReceiver br;

    private FragmentManager.OnBackStackChangedListener
            mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            setActionBarArrowDependingOnFragmentsBackStack();
        }
    };

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

        Font font = new Font("Exo2");
        font.install(this);

        super.onCreate(savedInstanceState);

        id = getIntent().getStringExtra(Names.ID);
        current = new LocalDate();

        if (savedInstanceState != null) {
            id = savedInstanceState.getString(Names.ID);
            current = new LocalDate(savedInstanceState.getLong(Names.DATE));
        }

        config = AppConfig.get(this);
        id = config.getId(id);
        car_config = CarConfig.get(this, id);
        state = CarState.get(this, id);

        setContentView(R.layout.main);
        vLogo = findViewById(R.id.logo);
        spinner = (Spinner) findViewById(R.id.spinner_nav);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawer,
                0,
                0
        ) {

            public void onDrawerClosed(View view) {
                setActionBarArrowDependingOnFragmentsBackStack();
            }

            public void onDrawerOpened(View drawerView) {
                drawerToggle.setDrawerIndicatorEnabled(true);
            }
        };
        drawer.setDrawerListener(drawerToggle);
        drawerToggle.setDrawerIndicatorEnabled(true);
        getSupportFragmentManager().addOnBackStackChangedListener(mOnBackStackChangedListener);

        setupActionBar();
        setSideMenu();

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                if (!id.equals(intent.getStringExtra(Names.ID)))
                    return;
                if (intent.getAction().equals(Names.CONFIG_CHANGED))
                    setSideMenu();
            }
        };
        IntentFilter intFilter = new IntentFilter(Names.CONFIG_CHANGED);
        registerReceiver(br, intFilter);

        if (savedInstanceState != null)
            return;

        if (car_config.getAuth().equals("")) {
            car_config = CarConfig.clear(this, id);
            Intent intent = new Intent(this, SplashActivity.class);
            intent.putExtra(Names.ID, id);
            startActivityForResult(intent, DO_AUTH);
            return;
        }

        setPrimary();

        checkCaps();
        if (checkPhone())
            return;
    }

    @Override
    protected void onDestroy() {
        getSupportFragmentManager().removeOnBackStackChangedListener(mOnBackStackChangedListener);
        AppConfig.save(this);
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, id);
        outState.putLong(Names.DATE, current.toDate().getTime());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
        setActionBarArrowDependingOnFragmentsBackStack();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DO_AUTH) {
            CarConfig carConfig = CarConfig.get(this, id);
            if (carConfig.getKey().equals("")) {
                finish();
                return;
            }
            setPrimary();
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
        MainFragment fragment = getFragment();
        if (fragment != null) {
            if (fragment.isShowDate()) {
                item.setTitle(current.toString("d MMMM"));
            } else {
                menu.removeItem(R.id.date);
            }
            if (fragmentMenu == null) {
                PopupMenu popupMenu = new PopupMenu(this, null);
                fragmentMenu = popupMenu.getMenu();
            }
            fragmentMenu.clear();
            fragment.onCreateOptionsMenu(fragmentMenu, getMenuInflater());
            for (int i = 0; i < fragmentMenu.size(); i++) {
                MenuItem it = fragmentMenu.getItem(i);
                menu.add(it.getGroupId(), it.getItemId(), it.getOrder(), it.getTitle());
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.isDrawerIndicatorEnabled() && drawerToggle.onOptionsItemSelected(item))
            return true;
        if (item.getItemId() == android.R.id.home &&
                getSupportFragmentManager().popBackStackImmediate())
            return true;

        MainFragment fragment = getFragment();
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
                        MainFragment fragment = getFragment();
                        if (fragment != null)
                            fragment.changeDate();
                    }
                }, current.getYear(), current.getMonthOfYear() - 1, current.getDayOfMonth());
                dialog.show(getSupportFragmentManager(), "DATE_PICKER_TAG");
                return true;
            }
            case R.id.about:
                setFragment(new AboutFragment());
                return true;
            case R.id.passwd:
                setFragment(new SetPassword());
                return true;
            case R.id.history:
                setFragment(new HistoryFragment());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setActionBarArrowDependingOnFragmentsBackStack() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            drawerToggle.setDrawerIndicatorEnabled(false);
        } else {
            drawerToggle.setDrawerIndicatorEnabled(true);
        }
        updateMenu();
        setupActionBar();
    }

    MainFragment getFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0)
            return (MainFragment) getSupportFragmentManager().findFragmentByTag(PRIMARY);
        String tag = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName();
        return (MainFragment) getSupportFragmentManager().findFragmentByTag(tag);
    }

    void setFragment(MainFragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.replace(R.id.fragment, fragment, TAG);
        ft.addToBackStack(TAG);
        ft.commit();
    }

    void setPrimary() {
        if (getFragment() != null)
            return;

        PrimaryFragment primaryFragment = new PrimaryFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_ENTER_MASK);
        ft.add(R.id.fragment, primaryFragment, PRIMARY);
        ft.commitAllowingStateLoss();
    }

    void setSideMenu() {
        PopupMenu p = new PopupMenu(this, null);
        sideMenu = p.getMenu();
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sidebar, sideMenu);

        CarState state = CarState.get(this, id);
        if (!state.isHistory())
            sideMenu.removeItem(R.id.history);

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
                MenuItem item = sideMenu.getItem(position);
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(item.getTitle());
                ImageView iv = (ImageView) v.findViewById(R.id.img);
                iv.setImageDrawable(item.getIcon());
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
        if (!state.isUse_phone() || !car_config.getPhone().equals("") || !State.hasTelephony(this))
            return false;
        Intent intent = new Intent(this, PhoneDialog.class);
        intent.putExtra(Names.ID, id);
        startActivityForResult(intent, DO_PHONE);
        return true;
    }

    void setupActionBar() {

        String title = "";
        MainFragment fragment = getFragment();
        if (fragment != null)
            title = fragment.getTitle();
        if (title != null) {
            spinner.setVisibility(View.GONE);
            if (title.equals("")) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                vLogo.setVisibility(View.VISIBLE);
                return;
            }
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            vLogo.setVisibility(View.GONE);
            setTitle(title);
            return;
        }

        final String[] cars = config.getCars();
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (cars.length <= 1) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            spinner.setVisibility(View.GONE);
            vLogo.setVisibility(View.VISIBLE);
            return;
        }
        vLogo.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
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

    void checkCaps() {
        final String version = State.getVersion(this);
        Date now = new Date();
        final long time = now.getTime();
        if (version.equals(state.getVersion()) && (state.getCheck_time() > time))
            return;
        HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                if (CarState.update(state, res.get("caps").asObject())) {
                    Intent intent = new Intent(Names.CONFIG_CHANGED);
                    intent.putExtra(Names.ID, id);
                    sendBroadcast(intent);
                }
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

    static class KeyParam implements Serializable {
        String skey;
    }

}
