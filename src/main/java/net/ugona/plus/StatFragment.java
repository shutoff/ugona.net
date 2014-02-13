package net.ugona.plus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Vector;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class StatFragment extends Fragment implements OnRefreshListener {

    final static String STAT_URL = "https://car-online.ugona.net/stat?skey=$1&tz=$2";

    String car_id;

    View vProgress;
    View vError;
    View vLoading;
    View vSpace;
    TextView tvSummary;
    ListView lvStat;

    Vector<Day> days;
    Vector<Day> stat;

    PullToRefreshLayout mPullToRefreshLayout;
    DataFetcher fetcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null)
            car_id = savedInstanceState.getString(Names.ID);
        View v = inflater.inflate(R.layout.tracks, container, false);
        vProgress = v.findViewById(R.id.progress);
        vProgress.setVisibility(View.GONE);
        vLoading = v.findViewById(R.id.loading);
        vProgress = v.findViewById(R.id.first_progress);
        vSpace = v.findViewById(R.id.space);
        vError = v.findViewById(R.id.error);
        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getData();
            }
        });
        tvSummary = (TextView) v.findViewById(R.id.summary);
        lvStat = (ListView) v.findViewById(R.id.tracks);
        getData();
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup viewGroup = (ViewGroup) view;
        mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());
        ActionBarPullToRefresh.from(getActivity())
                .insertLayoutInto(viewGroup)
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
        mPullToRefreshLayout.setPullEnabled(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
    }

    class DataFetcher extends HttpTask {
        @Override
        void result(JsonObject res) throws ParseException {
            done();
            days = new Vector<Day>();
            for (JsonObject.Member member : res) {
                try {
                    int year = Integer.parseInt(member.getName());
                    JsonObject year_data = member.getValue().asObject();
                    for (JsonObject.Member m : year_data) {
                        int month = Integer.parseInt(m.getName());
                        JsonArray month_data = m.getValue().asArray();
                        for (int i = 0; i < month_data.size(); i++) {
                            JsonObject data = month_data.get(i).asObject();
                            Day d = new Day();
                            d.year = year;
                            d.month = month;
                            d.day = data.get("d").asInt();
                            d.dist = data.get("s").asLong();
                            d.time = data.get("t").asLong();
                            d.speed = data.get("v").asInt();
                            days.add(d);
                        }
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
            stat = new Vector<Day>();
            Day cur = null;
            double dist = 0;
            long time = 0;
            int speed = 0;
            for (Day d : days) {
                if ((cur != null) && ((cur.year != d.year) || (cur.month != d.month))) {
                    stat.insertElementAt(cur, 0);
                    cur = null;
                }
                if (cur == null) {
                    cur = new Day();
                    cur.year = d.year;
                    cur.month = d.month;
                }
                cur.dist += d.dist;
                cur.time += d.time;
                if (d.speed > cur.speed)
                    cur.speed = d.speed;
                dist += d.dist;
                time += d.time;
                if (d.speed > speed)
                    speed = d.speed;
            }
            if (cur != null)
                stat.insertElementAt(cur, 0);
            double v = dist * 3.6 / time;
            String status = getString(R.string.status);
            tvSummary.setText(String.format(status, dist / 1000, timeFormat((int) (time / 60)), v, (double) speed));
            vProgress.setVisibility(View.GONE);
            vLoading.setVisibility(View.GONE);
            vError.setVisibility(View.GONE);
            vSpace.setVisibility(View.GONE);
            lvStat.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return stat.size();
                }

                @Override
                public Object getItem(int position) {
                    return stat.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    if (v == null) {
                        LayoutInflater inflater = (LayoutInflater) getActivity()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        v = inflater.inflate(R.layout.track_item, null);
                    }
                    Day d = stat.get(position);
                    TextView title = (TextView) v.findViewById(R.id.title);
                    if (d.level == 0)
                        title.setText(monthYear(d.year, d.month));
                    if (d.level == 1) {
                        LocalDate begin = new LocalDate(d.year, d.month + 1, d.day);
                        LocalDate end = begin.plusDays(6);
                        DateFormat df = android.text.format.DateFormat.getDateFormat(getActivity());
                        title.setText(df.format(begin.toDate()) + " - " + df.format(end.toDate()));
                    }
                    if (d.level == 2) {
                        LocalDate day = new LocalDate(d.year, d.month + 1, d.day);
                        DateFormat df = android.text.format.DateFormat.getDateFormat(getActivity());
                        title.setText(df.format(day.toDate()));
                    }
                    TextView tvMileage = (TextView) v.findViewById(R.id.mileage);
                    String s = String.format(getString(R.string.mileage), d.dist / 1000.);
                    tvMileage.setText(s);
                    double speed = d.dist * 3.6 / d.time;
                    String status = getString(R.string.short_status);
                    TextView text = (TextView) v.findViewById(R.id.address);
                    text.setText(String.format(status, timeFormat((int) (d.time / 60)), speed, (double) d.speed));
                    int p = text.getPaddingRight();
                    text.setPadding(p * (2 * d.level + 1), 0, p, 0);
                    text = (TextView) v.findViewById(R.id.status);
                    text.setVisibility(View.GONE);
                    return v;
                }
            });
            lvStat.setVisibility(View.VISIBLE);
            lvStat.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Day d = stat.get(position++);
                    if (d.level == 0) {
                        if ((position < stat.size()) && (stat.get(position).level == 1)) {
                            while (position < stat.size()) {
                                if (stat.get(position).level == 0)
                                    break;
                                stat.remove(position);
                            }
                            BaseAdapter adapter = (BaseAdapter) lvStat.getAdapter();
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        LocalDate begin = new LocalDate(d.year, d.month + 1, 1);
                        LocalDate end = begin.plusMonths(1);
                        while (begin.getDayOfWeek() != 1) {
                            begin = begin.minusDays(1);
                        }
                        while (end.getDayOfWeek() != 7) {
                            end = end.plusDays(1);
                        }
                        LocalDate end_week = begin.plusDays(6);
                        Day cur = null;
                        boolean week = false;
                        for (Day dd : days) {
                            if (dd.compare(begin) < 0)
                                continue;
                            if (dd.compare(end) > 0)
                                break;
                            if (dd.compare(end_week) > 0) {
                                if ((cur != null) && week)
                                    stat.insertElementAt(cur, position);
                                cur = null;
                            }
                            if (cur == null) {
                                begin = new LocalDate(dd.year, dd.month + 1, dd.day);
                                while (begin.getDayOfWeek() != 1) {
                                    begin = begin.minusDays(1);
                                }
                                end_week = begin.plusDays(6);
                                cur = new Day();
                                cur.year = begin.getYear();
                                cur.month = begin.getMonthOfYear() - 1;
                                cur.day = begin.getDayOfMonth();
                                cur.level = 1;
                                week = false;
                            }
                            cur.dist += dd.dist;
                            cur.time += dd.time;
                            if (dd.speed > cur.speed)
                                cur.speed = dd.speed;
                            if (d.month == dd.month)
                                week = true;
                        }
                        if ((cur != null) && week)
                            stat.insertElementAt(cur, position);
                        BaseAdapter adapter = (BaseAdapter) lvStat.getAdapter();
                        adapter.notifyDataSetChanged();
                    }
                    if (d.level == 1) {
                        if ((position < stat.size()) && (stat.get(position).level == 2)) {
                            while (position < stat.size()) {
                                if (stat.get(position).level < 2)
                                    break;
                                stat.remove(position);
                            }
                            BaseAdapter adapter = (BaseAdapter) lvStat.getAdapter();
                            adapter.notifyDataSetChanged();
                            return;
                        }
                        Day dm = null;
                        for (int p = position - 2; p >= 0; p--) {
                            dm = stat.get(p);
                            if (dm.level == 0)
                                break;
                        }
                        Day dw = stat.get(position - 1);
                        LocalDate begin = new LocalDate(dw.year, dw.month + 1, dw.day);
                        LocalDate end = begin.plusDays(6);
                        for (Day dd : days) {
                            if (dd.compare(begin) < 0)
                                continue;
                            if (dd.compare(end) > 0)
                                break;
                            if (dd.month != dm.month)
                                continue;
                            Day cur = new Day();
                            cur.year = dd.year;
                            cur.month = dd.month;
                            cur.day = dd.day;
                            cur.level = 2;
                            cur.dist = dd.dist;
                            cur.time = dd.time;
                            cur.speed = dd.speed;
                            stat.insertElementAt(cur, position);
                        }
                        BaseAdapter adapter = (BaseAdapter) lvStat.getAdapter();
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        void error() {
            if (!no_reload)
                showError();
            done();
        }

        boolean no_reload;

        void done() {
            if (fetcher != this)
                return;
            fetcher = null;
            mPullToRefreshLayout.setRefreshComplete();
        }

    }

    ;

    void getData() {
        fetcher = new DataFetcher();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String api_key = preferences.getString(Names.CAR_KEY + car_id, "");
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        fetcher.execute(STAT_URL, api_key, tz.getID());
        vProgress.setVisibility(View.VISIBLE);
        vLoading.setVisibility(View.VISIBLE);
        vError.setVisibility(View.GONE);
        lvStat.setVisibility(View.GONE);
    }

    void showError() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    vProgress.setVisibility(View.GONE);
                    vLoading.setVisibility(View.GONE);
                    vError.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    String timeFormat(int minutes) {
        if (minutes < 60) {
            String s = getString(R.string.m_format);
            return String.format(s, minutes);
        }
        int hours = minutes / 60;
        minutes -= hours * 60;
        String s = getString(R.string.hm_format);
        return String.format(s, hours, minutes);
    }

    @Override
    public void onRefreshStarted(View view) {
        if (fetcher != null)
            return;
        fetcher = new DataFetcher();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String api_key = preferences.getString(Names.CAR_KEY + car_id, "");
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        fetcher.execute(STAT_URL, api_key, tz.getID());
    }

    static class Day {
        int year;
        int month;
        int day;
        long dist;
        long time;
        int speed;
        int level;

        int compare(LocalDate d) {
            if (year < d.getYear())
                return -1;
            if (year > d.getYear())
                return 1;
            if (month < d.getMonthOfYear() - 1)
                return -1;
            if (month > d.getMonthOfYear() - 1)
                return 1;
            if (day < d.getDayOfMonth())
                return -1;
            if (day > d.getDayOfMonth())
                return 1;
            return 0;
        }

    }

    static String monthYear(int year, int month) {
        String s = new DateTime(year, month + 1, 1, 0, 0, 0, 0)
                .monthOfYear().getAsText().toUpperCase();
        s = s.replaceAll("\u0410\u042F$", "\u0410\u0419").replaceAll("\u042F$", "\u042C").replaceAll("\u0410$", "");
        s = s.substring(0, 1) + s.substring(1).toLowerCase();
        return s + " " + year;
    }
}
