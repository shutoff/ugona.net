package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

public class StatFragment extends MainFragment {

    View vError;
    TextView tvSummary;
    HoursList vStat;
    CenteredScrollView vSummary;

    HttpTask fetcher;
    Vector<Day> days;
    Vector<Day> stat;

    static String monthYear(int year, int month) {
        Date d = new Date(year - 1900, month, 1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("LLLL", Locale.getDefault());
        return dateFormat.format(d) + " " + year;
    }

    @Override
    int layout() {
        return R.layout.tracks;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.stat, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vError = v.findViewById(R.id.error);
        vStat = (HoursList) v.findViewById(R.id.tracks);
        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });
        tvSummary = (TextView) v.findViewById(R.id.summary);
        vSummary = (CenteredScrollView) v.findViewById(R.id.summary_view);
        vStat.disableDivider();
        onRefresh();
        return v;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.recalc) {
            final Dialog dialog = new AlertDialogWrapper.Builder(getActivity())
                    .setTitle(R.string.recalc_stat)
                    .setMessage(R.string.recalc_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            refresh(true);
                        }
                    })
                    .create();
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        refresh(false);
    }

    void refresh(boolean recalc) {
        if (fetcher != null)
            fetcher.cancel();
        vError.setVisibility(View.GONE);
        fetcher = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                if (getActivity() == null)
                    return;
                fetcher = null;
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
                                if (data.get("s") != null) {
                                    d.dist = data.get("s").asLong();
                                    d.time = data.get("t").asLong();
                                    d.speed = data.get("v").asInt();
                                }
                                if (data.get("engine") != null)
                                    d.engine_time = data.get("engine").asLong();
                                days.add(d);
                            }
                        }
                    } catch (Exception ex) {
                        // ignore
                    }
                }
                Set<Integer> opened = new HashSet<Integer>();
                if (stat != null) {
                    for (int i = 1; i < stat.size(); i++) {
                        Day d = stat.get(i);
                        if (d.level == 0)
                            continue;
                        Day prev = stat.get(i - 1);
                        if ((d.level == 1) && (prev.level == 0))
                            opened.add(prev.id());
                        if ((d.level == 2) && (prev.level == 1))
                            opened.add(prev.id());
                    }
                }

                stat = new Vector<Day>();
                Day cur = null;
                double dist = 0;
                long time = 0;
                long engine_time = 0;
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
                    cur.engine_time += d.engine_time;
                    if (d.speed > cur.speed)
                        cur.speed = d.speed;
                    dist += d.dist;
                    time += d.time;
                    engine_time += d.engine_time;
                    if (d.speed > speed)
                        speed = d.speed;
                }
                if (cur != null)
                    stat.insertElementAt(cur, 0);

                for (int i = 0; i < stat.size(); i++) {
                    Day d = stat.get(i);
                    if ((d.level < 2) && opened.contains(d.id())) {
                        if (d.level == 0)
                            openMonth(d, i + 1);
                        if (d.level == 1)
                            openWeek(d, i + 1);
                    }
                }

                double v = dist * 3.6 / time;
                String status = getString(R.string.status);
                NumberFormat formatter = NumberFormat.getInstance(getResources().getConfiguration().locale);
                formatter.setMaximumFractionDigits(2);
                formatter.setMinimumFractionDigits(2);
                String s = formatter.format(dist / 1000.);
                String status_text = String.format(status, s, timeFormat((int) (time / 60)), (float) v, speed);
                if (engine_time >= 60) {
                    status_text += "\n";
                    status_text += getString(R.string.engine_time);
                    status_text += ": |";
                    status_text += timeFormat((int) (engine_time / 60));
                }
                tvSummary.setText(State.createSpans(status_text, vSummary.selectedColor, true));
                vError.setVisibility(View.GONE);
                vStat.setAdapter(new BaseAdapter() {
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
                            LayoutInflater inflater = LayoutInflater.from(getActivity());
                            v = inflater.inflate(R.layout.track_item, null);
                        }
                        Day d = stat.get(position);
                        TextView title = (TextView) v.findViewById(R.id.title);
                        if (d.level == 0)
                            title.setText(monthYear(d.year, d.month));
                        if (d.level == 1) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(d.year, d.month, d.day, 0, 0, 0);

                            DateFormat df = android.text.format.DateFormat.getDateFormat(getActivity());
                            String start = df.format(calendar.getTime());

                            calendar.add(Calendar.DATE, 6);
                            title.setText(start + " - " + df.format(calendar.getTime()));
                        }
                        if (d.level == 2) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(d.year, d.month, d.day);
                            DateFormat df = android.text.format.DateFormat.getDateFormat(getActivity());
                            title.setText(df.format(calendar.getTime()));
                        }
                        TextView tvMileage = (TextView) v.findViewById(R.id.mileage);
                        NumberFormat formatter = NumberFormat.getInstance(getResources().getConfiguration().locale);
                        formatter.setMaximumFractionDigits(2);
                        formatter.setMinimumFractionDigits(2);
                        String s = formatter.format(d.dist / 1000.) + " " + getString(R.string.km);
                        tvMileage.setText(s);
                        String status = getString(R.string.short_status);
                        TextView text = (TextView) v.findViewById(R.id.address);
                        String status_text = "";
                        if (d.time > 0) {
                            float speed = (float) (d.dist * 3.6 / d.time);
                            status_text = String.format(status, timeFormat((int) (d.time / 60)), speed, d.speed);
                        }
                        if (d.engine_time >= 60) {
                            status_text += "\n";
                            status_text += getString(R.string.engine_time);
                            status_text += ": |";
                            status_text += timeFormat((int) (d.engine_time / 60));
                        }
                        text.setText(State.createSpans(status_text, getResources().getColor(R.color.highlighted), true));
                        int p = text.getPaddingRight();
                        text.setPadding(p * 2 * d.level, 0, p, 0);
                        text = (TextView) v.findViewById(R.id.status);
                        text.setVisibility(View.GONE);
                        return v;
                    }
                });
                vStat.setVisibility(View.VISIBLE);
                vStat.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
                                vStat.notifyChanges();
                                return;
                            }
                            openMonth(d, position);
                            vStat.notifyChanges();
                        }
                        if (d.level == 1) {
                            if ((position < stat.size()) && (stat.get(position).level == 2)) {
                                while (position < stat.size()) {
                                    if (stat.get(position).level < 2)
                                        break;
                                    stat.remove(position);
                                }
                                vStat.notifyChanges();
                                return;
                            }
                            openWeek(d, position);
                            vStat.notifyChanges();
                        }
                    }
                });
                refreshDone();
            }

            @Override
            void error() {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            vStat.setVisibility(View.GONE);
                            vError.setVisibility(View.VISIBLE);
                            refreshDone();
                        }
                    });
                }
                fetcher = null;
            }
        };
        StatParam param = new StatParam();
        CarConfig config = CarConfig.get(getActivity(), id());
        param.skey = config.getKey();
        Calendar cal = Calendar.getInstance();
        param.tz = cal.getTimeZone().getID();
        if (recalc)
            param.recalc = 1;
        fetcher.execute("/stat", param);
    }

    void openMonth(Day d, int position) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(d.year, d.month, 1);
        while (calendar.get(Calendar.DAY_OF_WEEK) != calendar.getFirstDayOfWeek()) {
            calendar.add(Calendar.DATE, -1);
        }
        long begin = calendar.getTimeInMillis();
        calendar.set(d.year, d.month, 1);
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DATE, 1);
        while (calendar.get(Calendar.DAY_OF_WEEK) != calendar.getFirstDayOfWeek()) {
            calendar.add(Calendar.DATE, 1);
        }
        calendar.add(Calendar.DATE, -1);
        long end = calendar.getTimeInMillis();

        calendar.setTimeInMillis(begin);
        calendar.add(Calendar.DATE, 7);
        long end_week = calendar.getTimeInMillis();

        Day cur = null;
        boolean week = false;
        for (Day dd : days) {
            if (dd.compare(begin) < 0)
                continue;
            if (dd.compare(end) > 0)
                break;
            if (dd.compare(end_week) >= 0) {
                if ((cur != null) && week)
                    stat.insertElementAt(cur, position);
                cur = null;
                calendar.setTimeInMillis(end_week);
                calendar.add(Calendar.DATE, 7);
                end_week = calendar.getTimeInMillis();
            }
            if (cur == null) {
                calendar.set(dd.year, dd.month, dd.day);
                while (calendar.get(Calendar.DAY_OF_WEEK) != calendar.getFirstDayOfWeek()) {
                    calendar.add(Calendar.DATE, -1);
                }
                cur = new Day();
                cur.year = calendar.get(Calendar.YEAR);
                cur.month = calendar.get(Calendar.MONTH);
                cur.day = calendar.get(Calendar.DAY_OF_MONTH);
                cur.level = 1;
                week = false;
            }
            cur.dist += dd.dist;
            cur.time += dd.time;
            cur.engine_time += dd.engine_time;
            if (dd.speed > cur.speed)
                cur.speed = dd.speed;
            if (d.month == dd.month)
                week = true;
        }
        if ((cur != null) && week)
            stat.insertElementAt(cur, position);
    }

    void openWeek(Day d, int position) {
        Day dm = null;
        for (int p = position - 2; p >= 0; p--) {
            dm = stat.get(p);
            if (dm.level == 0)
                break;
        }
        Day dw = stat.get(position - 1);
        Calendar calendar = Calendar.getInstance();
        calendar.set(dw.year, dw.month, dw.day);
        long begin = calendar.getTimeInMillis();
        calendar.add(Calendar.DATE, 6);
        long end = calendar.getTimeInMillis();
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
            cur.engine_time = dd.engine_time;
            stat.insertElementAt(cur, position);
        }
    }

    static class StatParam implements Serializable {
        String skey;
        String tz;
        Integer recalc;
    }

    static class Day {
        int year;
        int month;
        int day;
        long dist;
        long time;
        int speed;
        long engine_time;
        int level;

        int compare(long d) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(d);
            if (year < calendar.get(Calendar.YEAR))
                return -1;
            if (year > calendar.get(Calendar.YEAR))
                return 1;
            if (month < calendar.get(Calendar.MONTH))
                return -1;
            if (month > calendar.get(Calendar.MONTH))
                return 1;
            if (day < calendar.get(Calendar.DAY_OF_MONTH))
                return -1;
            if (day > calendar.get(Calendar.DAY_OF_MONTH))
                return 1;
            return 0;
        }

        int id() {
            int res = year * 12 + month;
            if (level == 0)
                return res;
            res = res * 31 + day;
            return res;
        }
    }

}
