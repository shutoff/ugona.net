package net.ugona.plus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.romorama.caldroid.CaldroidFragment;
import com.romorama.caldroid.CaldroidListener;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.Date;
import java.util.Vector;

public class HistoryActivity extends ActionBarActivity {

    FrameLayout holder;
    History mPlot;
    Menu topSubMenu;
    CaldroidFragment caldroidFragment;
    TypesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        mPlot = (History) getLastCustomNonConfigurationInstance();
        initUI();
        adapter = new TypesAdapter();
    }


    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        Object res = mPlot;
        if (mPlot != null) {
            holder.removeView(mPlot);
            mPlot = null;
        }
        return res;
    }

    @Override
    protected void onStart() {
        super.onStart();
        setActionBar();
    }

    void initUI() {
        holder = (FrameLayout) findViewById(R.id.webview);
        holder.setBackgroundColor(getResources().getColor(R.color.caldroid_gray));
        if (mPlot == null) {
            mPlot = new History(this, getIntent().getStringExtra(Names.ID), getIntent().getStringExtra(Names.STATE));
            mPlot.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        holder.addView(mPlot);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        topSubMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history, menu);
        MenuItem item = menu.findItem(R.id.date);
        item.setTitle(mPlot.mHistory.current.toString("d MMMM"));
        if (!State.hasTelephony(this))
            menu.removeItem(R.id.passwd);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.date: {
                caldroidFragment = new CaldroidFragment() {

                    @Override
                    public void onAttach(Activity activity) {
                        super.onAttach(activity);
                        CaldroidListener listener = new CaldroidListener() {

                            @Override
                            public void onSelectDate(Date date, View view) {
                                mPlot.changeDate(new LocalDate(date));
                                caldroidFragment.dismiss();
                                updateMenu();
                            }
                        };

                        setCaldroidListener(listener);
                    }

                };
                Bundle args = new Bundle();
                args.putString(CaldroidFragment.DIALOG_TITLE, getString(R.string.day));
                LocalDate current = mPlot.mHistory.current;
                args.putInt(CaldroidFragment.MONTH, current.getMonthOfYear());
                args.putInt(CaldroidFragment.YEAR, current.getYear());
                args.putInt(CaldroidFragment.START_DAY_OF_WEEK, 1);
                caldroidFragment.setArguments(args);
                LocalDateTime now = new LocalDateTime();
                caldroidFragment.setMaxDate(now.toDate());
                long first = mPlot.mHistory.preferences.getLong(Names.Car.FIRST_TIME + mPlot.mHistory.car_id, 0);
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

    void updateMenu() {
        if (topSubMenu == null)
            return;
        topSubMenu.clear();
        onCreateOptionsMenu(topSubMenu);
    }

    void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        actionBar.setListNavigationCallbacks(adapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int i, long l) {
                mPlot.setType(adapter.types.get(i).type);
                return true;
            }
        });
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        for (int i = 0; i < adapter.types.size(); i++) {
            if (adapter.types.get(i).type.equals(mPlot.mHistory.type)) {
                actionBar.setSelectedNavigationItem(i);
                break;
            }
        }
        setTitle("");
    }

    static class Type {
        String type;
        String name;

        Type(String t, String n) {
            type = t;
            name = n;
        }
    }

    static class History extends FrameLayout implements HistoryView.HistoryViewListener {

        HistoryView mHistory;
        TextView tvNoData;
        View vProgress;
        View vError;

        public History(Context context, String car_id, String type) {
            super(context);
            addView(inflate(context, R.layout.history, null));
            tvNoData = (TextView) findViewById(R.id.no_data);
            vProgress = findViewById(R.id.progress);
            vError = findViewById(R.id.error);
            mHistory = (HistoryView) findViewById(R.id.history);
            mHistory.mListener = this;
            mHistory.init(car_id, type, new LocalDate());
            vError.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    vError.setVisibility(GONE);
                    vProgress.setVisibility(VISIBLE);
                    mHistory.loadData();
                }
            });
        }

        void setType(String type) {
            if (type.equals(mHistory.type))
                return;
            mHistory.setVisibility(GONE);
            tvNoData.setVisibility(GONE);
            vError.setVisibility(GONE);
            vProgress.setVisibility(VISIBLE);
            mHistory.init(mHistory.car_id, type, mHistory.current);
        }


        void changeDate(LocalDate c) {
            mHistory.setVisibility(GONE);
            tvNoData.setVisibility(GONE);
            vError.setVisibility(GONE);
            vProgress.setVisibility(VISIBLE);
            mHistory.init(mHistory.car_id, mHistory.type, c);
        }

        @Override
        public void dataReady() {
            vProgress.setVisibility(GONE);
            mHistory.setVisibility(VISIBLE);
        }

        @Override
        public void noData() {
            vProgress.setVisibility(GONE);
            tvNoData.setVisibility(VISIBLE);
        }

        @Override
        public void errorLoading() {
            vProgress.setVisibility(GONE);
            vError.setVisibility(VISIBLE);
        }
    }

    class TypesAdapter extends BaseAdapter {

        Vector<Type> types;

        TypesAdapter() {
            super();
            init();
        }

        @Override
        public int getCount() {
            return types.size();
        }

        @Override
        public Object getItem(int i) {
            return types.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getBaseContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.car_list_item, null);
            }
            TextView tv = (TextView) v.findViewById(R.id.name);
            tv.setText(types.get(i).name);
            return v;
        }

        @Override
        public View getDropDownView(int i, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getBaseContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.car_list_dropdown_item, null);
            }
            TextView tv = (TextView) v.findViewById(R.id.name);
            tv.setText(types.get(i).name);
            return v;
        }

        void init() {
            types = new Vector<Type>();
            String car_id = mPlot.mHistory.car_id;
            Cars.Car[] cars = Cars.getCars(HistoryActivity.this);
            String title = "";
            if (cars.length > 1) {
                for (Cars.Car car : cars) {
                    if (car.id.equals(mPlot.mHistory.car_id))
                        title = car.name + ": ";
                }
            }
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(HistoryActivity.this);
            try {
                if (Double.parseDouble(preferences.getString(Names.Car.VOLTAGE_MAIN + car_id, "")) > 0)
                    types.add(new Type("voltage", title + getString(R.string.voltage)));
            } catch (Exception ex) {
                // ignore
            }
            try {
                if (Double.parseDouble(preferences.getString(Names.Car.VOLTAGE_MAIN + car_id, "")) > 0)
                    types.add(new Type("reserved", title + getString(R.string.reserved)));
            } catch (Exception ex) {
                // ignore
            }
            if (preferences.getBoolean(Names.Car.SHOW_BALANCE + car_id, true))
                types.add(new Type("balance", title + getString(R.string.balance)));

            Preferences.TempConfig config = Preferences.getTemperatureConfig(preferences, car_id, 1);
            if (config != null)
                types.add(new Type("t1", title + getString(R.string.temperature)));
            config = Preferences.getTemperatureConfig(preferences, car_id, 2);
            if (config != null)
                types.add(new Type("t2", title + getString(R.string.temperature) + " (" + getString(R.string.sensor) + ": " + config.where + ")"));
            config = Preferences.getTemperatureConfig(preferences, car_id, 3);
            if (config != null)
                types.add(new Type("t3", title + getString(R.string.temperature) + " (" + getString(R.string.sensor) + ": " + config.where + ")"));
            config = Preferences.getTemperatureConfig(preferences, car_id, -1);
            if (config != null)
                types.add(new Type("t-1", title + getString(R.string.temperature) + " (" + getString(R.string.sensor) + ": " + config.where + ")"));
            config = Preferences.getTemperatureConfig(preferences, car_id, -2);
            if (config != null)
                types.add(new Type("t-2", title + getString(R.string.temperature) + " (" + getString(R.string.sensor) + ": " + config.where + ")"));
            config = Preferences.getTemperatureConfig(preferences, car_id, -3);
            if (config != null)
                types.add(new Type("t-3", title + getString(R.string.temperature) + " (" + getString(R.string.sensor) + ": " + config.where + ")"));

        }
    }

}
