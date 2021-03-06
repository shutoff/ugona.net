package net.ugona.plus;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
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
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.firebase.iid.FirebaseInstanceId;
import com.haibison.android.lockpattern.LockPatternActivity;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity
        extends AppCompatActivity {

    static final int DO_AUTH = 1;
    static final int REQUEST_CHECK_PATTERN = 2;
    static final int DO_START = 3;

    static final String PRIMARY = "primary";
    static final String FRAGMENT = "fragment";

    final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    static final String SENDER_ID = "915289471784";

    static Menu homeMenu;
    static Runnable password_request;

    static MainActivity foreground;

    String id;

    AppConfig config;
    CarState state;
    CarConfig car_config;
    Menu topSubMenu;
    Menu sideMenu;
    Menu fragmentMenu;
    long current;
    DrawerLayout drawer;
    ActionBarDrawerToggle drawerToggle;
    View vLogo;
    Spinner spinner;
    BroadcastReceiver br;
    Handler handler;
    boolean bActive;
    PendingIntent piRefresh;
    long start_time;

    private FragmentManager.OnBackStackChangedListener
            mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            setActionBarArrowDependingOnFragmentsBackStack();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                ex.printStackTrace();
                State.print(ex);
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                ErrorParam param = new ErrorParam();
                param.error = sw.toString();
                HttpTask task = new HttpTask() {
                    @Override
                    void result(JsonObject res) throws ParseException {
                        System.exit(1);
                    }

                    @Override
                    void error() {

                    }
                };
                task.execute("/error", param);
            }
        });

        Font font = new Font("Exo2");
        font.install(this);

        super.onCreate(savedInstanceState);

        handler = new Handler();

        Intent intent = getIntent();
        id = intent.getStringExtra(Names.ID);
        current = new Date().getTime();

        if (savedInstanceState != null) {
            id = savedInstanceState.getString(Names.ID);
            current = savedInstanceState.getLong(Names.DATE);
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
        drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

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

        if ((car_config.getKey().equals("")) ||
                (car_config.getAuth().equals("") &&
                        ((intent == null) || (intent.getStringExtra(Names.ID) == null)))) {
            Intent i = new Intent(this, SplashActivity.class);
            startActivityForResult(i, DO_AUTH);
            return;
        }

        AppConfig appConfig = AppConfig.get(this);
        if (appConfig.isStart_password()) {
            if (!appConfig.getPattern().equals("")) {
                Intent i = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null, this, LockPatternActivity.class);
                i.putExtra(LockPatternActivity.EXTRA_PATTERN, appConfig.getPattern().toCharArray());
                startActivityForResult(i, DO_START);
                return;
            }
            if (!config.getPassword().equals("")) {
                Intent i = new Intent(this, PasswordActivity.class);
                startActivityForResult(i, DO_START);
                return;
            }
        }

        Notification.clear(this, id);
        setPrimary();

        checkCaps(id);
    }

    @Override
    protected void onDestroy() {
        getSupportFragmentManager().removeOnBackStackChangedListener(mOnBackStackChangedListener);
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    public void onAttachedToWindow() {
        start_time = System.currentTimeMillis();
        super.onAttachedToWindow();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, id);
        outState.putLong(Names.DATE, current);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
        setActionBarArrowDependingOnFragmentsBackStack();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Notification.clear(this, id);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerGCM();
        bActive = true;
        Intent intent = new Intent(this, FetchService.class);
        intent.setAction(Names.START_UPDATE);
        intent.putExtra(Names.ID, id);
        startService(intent);
        foreground = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppConfig.save(this);
        bActive = false;
        foreground = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DO_START) {
            if (resultCode == RESULT_OK) {
                Notification.clear(this, id);
                setPrimary();
                checkCaps(id);
                return;
            }
            finish();
            return;
        }
        if (requestCode == DO_AUTH) {
            CarConfig carConfig = CarConfig.get(this, id);
            if (carConfig.getKey().equals("")) {
                finish();
                return;
            }
            setSideMenu();
            setPrimary();
            return;
        }
        if ((requestCode == REQUEST_CHECK_PATTERN) && (resultCode == RESULT_OK)) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    setFragment(new SetPassword());
                }
            });
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
                DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
                SimpleDateFormat sf = (SimpleDateFormat) df;
                String pat = sf.toLocalizedPattern();
                pat = pat.replaceAll("[yY]", "");
                pat = pat.replaceAll("^[^A-Za-z]+", "");
                pat = pat.replaceAll("[^A-Za-z]+$", "");
                pat = pat.replaceAll("N", "");
                try {
                    df = new SimpleDateFormat(pat);
                } catch (Exception ex) {
                    df = new SimpleDateFormat("d MMMM");
                }
                item.setTitle(df.format(current));
            } else {
                menu.removeItem(R.id.date);
            }
            if (fragmentMenu == null) {
                fragmentMenu = State.createMenu(this);
            }
            fragmentMenu.clear();
            fragment.onCreateOptionsMenu(fragmentMenu, getMenuInflater());
            for (int i = 0; i < fragmentMenu.size(); i++) {
                MenuItem it = fragmentMenu.getItem(i);
                MenuItem new_it = menu.add(it.getGroupId(), it.getItemId(), it.getOrder(), it.getTitle());
                if (it.isCheckable()) {
                    new_it.setCheckable(true);
                    new_it.setChecked(it.isChecked());
                }
                Drawable icon = it.getIcon();
                if (icon != null) {
                    new_it.setIcon(icon);
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                        new_it.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.isDrawerIndicatorEnabled() && drawerToggle.onOptionsItemSelected(item))
            return true;
        MainFragment fragment = getFragment();
        if ((fragment != null) && fragment.onOptionsItemSelected(item))
            return true;
        if (item.getItemId() == android.R.id.home) {
            if (getSupportFragmentManager().popBackStackImmediate())
                return true;
        }

        switch (item.getItemId()) {
            case R.id.date: {
                final CalendarDatePickerDialog dialog = new DatePicker();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(current);
                dialog.initialize(new CalendarDatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(CalendarDatePickerDialog calendarDatePickerDialog, int i, int i2, int i3) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(i, i2, i3);
                        current = calendar.getTimeInMillis();
                        updateMenu();
                        MainFragment fragment = getFragment();
                        if (fragment != null)
                            fragment.changeDate();
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                dialog.show(getSupportFragmentManager(), "DATE_PICKER_TAG");
                return true;
            }
            case R.id.about:
                setFragment(new AboutFragment());
                return true;
            case R.id.passwd:
                if (!config.getPattern().equals("")) {
                    Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                            this, LockPatternActivity.class);
                    intent.putExtra(LockPatternActivity.EXTRA_PATTERN, config.getPattern().toCharArray());
                    startActivityForResult(intent, REQUEST_CHECK_PATTERN);
                    return true;
                }
                setFragment(new SetPassword());
                return true;
            case R.id.charts:
                setFragment(new HistoryFragment());
                return true;
            case R.id.preferences:
                setFragment(new SettingsFragment());
                return true;
            case R.id.cars:
                setFragment(new CarsFragment());
                return true;
            case R.id.maintenance:
                setFragment(new MaintenanceFragment());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                drawer.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        }
        MainFragment fragment = getFragment();
        if (fragment != null) {
            if (homeMenu == null) {
                homeMenu = State.createMenu(this);
                getMenuInflater().inflate(R.menu.home, homeMenu);
            }
            MenuItem item = homeMenu.findItem(android.R.id.home);
            if (fragment.onOptionsItemSelected(item))
                return;
        }
        super.onBackPressed();
    }

    private void setActionBarArrowDependingOnFragmentsBackStack() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            drawerToggle.setDrawerIndicatorEnabled(false);
        } else {
            drawerToggle.setDrawerIndicatorEnabled(true);
        }
        updateMenu();
        setSideMenu();
        setupActionBar();
        AppConfig.save(this);
    }

    MainFragment getFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0)
            return (MainFragment) getSupportFragmentManager().findFragmentByTag(PRIMARY);
        String tag = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName();
        return (MainFragment) getSupportFragmentManager().findFragmentByTag(tag);
    }

    void setFragment(MainFragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment, fragment, FRAGMENT);
        ft.addToBackStack(FRAGMENT);
        ft.commit();
        setSideMenu();
    }

    boolean isFragmentShow(String tag) {
        return getSupportFragmentManager().findFragmentByTag(tag) != null;
    }

    void setPrimary() {
        if (getFragment() != null)
            return;

        PrimaryFragment primaryFragment = new PrimaryFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment, primaryFragment, PRIMARY);
        ft.commitAllowingStateLoss();
        setSideMenu();
    }

    void setSideMenu() {
        sideMenu = State.createMenu(this);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sidebar, sideMenu);

        if (car_config.getKey().equals("demo"))
            sideMenu.removeItem(R.id.cars);

        CarState state = CarState.get(this, id);
        if (!state.isHistory())
            sideMenu.removeItem(R.id.charts);

        MainFragment fragment = getFragment();
        if (fragment != null)
            fragment.setupSideMenu(sideMenu);

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
                    LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                    v = inflater.inflate(R.layout.menu_item, null);
                }
                MenuItem item = sideMenu.getItem(position);
                if (item.getGroupId() > 0) {
                    TextView tv = (TextView) v.findViewById(R.id.subname);
                    tv.setText(item.getTitle());
                    tv.setVisibility(View.VISIBLE);
                    v.findViewById(R.id.name).setVisibility(View.GONE);

                } else {
                    TextView tv = (TextView) v.findViewById(R.id.name);
                    tv.setText(item.getTitle());
                    tv.setVisibility(View.VISIBLE);
                    v.findViewById(R.id.subname).setVisibility(View.GONE);
                }
                ImageView iv = (ImageView) v.findViewById(R.id.img);
                iv.setImageDrawable(item.getIcon());
                return v;
            }
        });
        lMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawer.closeDrawer(GravityCompat.START);
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

    void setupActionBar() {
        String title = "";
        final MainFragment fragment = getFragment();
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

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        final Menu combo = fragment.combo();
        if (combo == null) {
            spinner.setVisibility(View.GONE);
            vLogo.setVisibility(View.VISIBLE);
            return;
        }

        vLogo.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
        spinner.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return combo.size();
            }

            @Override
            public Object getItem(int position) {
                return combo.getItem(position);
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
                if ((v != null) && !v.getTag().equals(layout_id))
                    v = null;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(getSupportActionBar().getThemedContext());
                    v = inflater.inflate(layout_id, null);
                    v.setTag(layout_id);
                }
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(combo.getItem(position).getTitle());
                v.setTag(layout_id);
                return v;
            }
        });
        int current = fragment.currentComboItem();
        for (int i = 0; i < combo.size(); i++) {
            if (combo.getItem(i).getItemId() == current) {
                spinner.setSelection(i);
                break;
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                fragment.onOptionsItemSelected(combo.getItem(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    void checkCaps(final String id) {
        final String version = State.getVersion(this);
        Date now = new Date();
        final long time = now.getTime();
        final CarConfig carConfig = CarConfig.get(this, id);
        final CarState state = CarState.get(this, id);
        if ((carConfig.getSettings() != null) && (carConfig.getSettings().length > 0)) {
            if (version.equals(state.getCheck_version()) && (state.getCheck_time() > time))
                return;
        }
        final Context context = this;
        HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                boolean changed = CarState.update(state, res.get("caps").asObject()) != null;
                if (Config.update(carConfig, res) != null) {
                    changed = true;
                    Intent intent = new Intent(Names.COMMANDS);
                    intent.putExtra(Names.ID, id);
                    sendBroadcast(intent);
                }
                if (changed) {
                    Intent intent = new Intent(Names.CONFIG_CHANGED);
                    intent.putExtra(Names.ID, id);
                    sendBroadcast(intent);
                }
                state.setCheck_time(time + 86400000);
                state.setCheck_version(version);
                config.save(context);
            }

            @Override
            void error() {

            }
        };
        KeyParam skey = new KeyParam();
        skey.skey = carConfig.getKey();
        task.execute("/caps", skey);
    }

    boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (config.isNo_google())
                return false;
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
                config.setNo_google(true);
            }
            return false;
        }
        return true;
    }

    String getAppVer() {
        try {
            PackageManager pkgManager = getPackageManager();
            PackageInfo info = pkgManager.getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    void registerGCM() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        if (apiAvailability.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS)
            return;
        long now = new Date().getTime();
        String version = getAppVer();
        if (!config.getGCM_version().equals(version))
            config.setGCM_time(0);
        if (now < config.getGCM_time())
            return;
        AppConfig config = AppConfig.get(this);
        try {
            String token = FirebaseInstanceId.getInstance().getToken();
            Reader reader = null;
            HttpURLConnection connection = null;
            JsonObject data = new JsonObject();
            data.add("reg", token);
            String[] cars = config.getCars();
            String d = null;
            JsonObject jCars = new JsonObject();
            for (String car : cars) {
                CarConfig carConfig = CarConfig.get(this, car);
                String key = carConfig.getKey();
                if (key.equals("") || (key.equals("demo")))
                    continue;
                JsonObject c = new JsonObject();
                c.add("id", car);
                c.add("phone", carConfig.getPhone());
                c.add("auth", carConfig.getAuth());
                jCars.add(key, c);
            }
            data.add("car_data", jCars);
            Calendar cal = Calendar.getInstance();
            TimeZone tz = cal.getTimeZone();
            data.add("tz", tz.getID());
            data.add("version", getAppVer());
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String id = "";
            try {
                id = tm.getDeviceId();
            } catch (Exception ex) {
                // ignore
            }
            if (id.equals("")) {
                try {
                    id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                } catch (Exception ex) {
                    // ignore
                }
            }
            if (!id.equals(""))
                data.add("uid", id);
            data.add("lang", Locale.getDefault().getLanguage());
            data.add("os", Build.VERSION.RELEASE);
            data.add("model", Build.MODEL);
            String phone = "";
            try {
                phone = tm.getLine1Number();
            } catch (Exception ex) {
                // ignore
            }
            if (!phone.equals(""))
                data.add("phone", phone);
            String url = PhoneSettings.get().getServer() + "/reg";
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), data.toString());
            Request request = new Request.Builder().url(url).post(body).build();
            Response response = HttpTask.client.newCall(request).execute();
            if (response.code() != HttpURLConnection.HTTP_OK)
                return;
            reader = response.body().charStream();
            JsonObject res = JsonValue.readFrom(reader).asObject();
            if (res.asObject().get("error") != null)
                return;
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        config.setGCM_time(new Date().getTime());
        config.setGCM_version(getAppVer());
    }

    void setCarId(String new_id) {
        if (id.equals(new_id))
            return;
        start_time = System.currentTimeMillis();
        config.setCurrent_id(new_id);
        config.save(this);
        id = new_id;
        car_config = CarConfig.get(this, id);
        Intent intent = new Intent(Names.CAR_CHANGED);
        sendBroadcast(intent);
        Notification.clear(this, id);
        checkCaps(id);
        setSideMenu();
    }

    static class KeyParam implements Serializable {
        String skey;
    }

    static class ErrorParam implements Serializable {
        String error;
    }
}

