package net.ugona.plus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class EventsFragment extends Fragment
        implements MainActivity.DateChangeListener, OnRefreshListener {

    final static String URL_EVENTS = "/events?skey=$1&begin=$2&end=$3&first=$4&pointer=$5&auth=$6&lang=$7";
    final static String URL_EVENT = "/event?skey=$1&id=$2&time=$3";
    static final String FILTER = "filter";
    static final String DATE = "events_date";
    static final String EVENTS_DATA = "events";
    String car_id;
    String api_key;
    Vector<Event> events;
    Vector<Event> filtered;
    Event firstEvent;
    SharedPreferences preferences;
    int filter;
    boolean error;
    boolean loaded;
    boolean no_events;
    boolean pointer;
    HoursList vEvents;
    TextView tvNoEvents;
    View vProgress;
    View vError;
    long current_id;
    LocalDate current;
    BroadcastReceiver br;
    DataFetcher fetcher;
    PullToRefreshLayout mPullToRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.events, container, false);

        if (current == null)
            current = new LocalDate();
        if (savedInstanceState != null) {
            car_id = savedInstanceState.getString(Names.ID);
            current = new LocalDate(savedInstanceState.getLong(DATE));
            byte[] track_data = savedInstanceState.getByteArray(EVENTS_DATA);
            if (track_data != null) {
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(track_data);
                    ObjectInput in = new ObjectInputStream(bis);
                    events = (Vector<Event>) in.readObject();
                    firstEvent = (Event) in.readObject();
                    in.close();
                    bis.close();
                    loaded = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        vEvents = (HoursList) v.findViewById(R.id.events);
        vEvents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Event e = filtered.get(position);
                if (e.id == current_id) {
                    if (e.point == null)
                        return;
                    String info = State.formatTime(getActivity(), e.time) + " ";
                    info += e.title + "\n";
                    if (e.text != null) {
                        info += e.text + "\n";
                    }
                    Intent i = new Intent(getActivity(), MapEventActivity.class);
                    String[] point = e.point.split(";");
                    if (point.length < 2)
                        return;
                    String point_data = point[0] + ";" + point[1] + ";" + e.course + ";" + info + e.address;
                    if (point.length > 2)
                        point_data += ";" + point[2];
                    i.putExtra(Names.POINT_DATA, point_data);
                    i.putExtra(Names.ID, car_id);
                    startActivity(i);
                    return;
                }
                current_id = e.id;
                vEvents.notifyChanges();
                new EventRequest(e.id, e.time, e.type);
            }
        });

        tvNoEvents = (TextView) v.findViewById(R.id.no_events);
        vProgress = v.findViewById(R.id.progress);
        vError = v.findViewById(R.id.error);
        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateChanged(current);
            }
        });

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        api_key = preferences.getString(Names.Car.CAR_KEY + car_id, "");
        pointer = preferences.getBoolean(Names.Car.POINTER + car_id, false);

        filtered = new Vector<Event>();
        if (events == null)
            events = new Vector<Event>();

        if (pointer) {
            filter = 7;
            v.findViewById(R.id.actions).setVisibility(View.GONE);
            v.findViewById(R.id.contacts).setVisibility(View.GONE);
            v.findViewById(R.id.system).setVisibility(View.GONE);
            View vLogo = v.findViewById(R.id.logo);
            if (vLogo != null) {
                vLogo.setVisibility(View.VISIBLE);
                vLogo.setClickable(true);
                vLogo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), About.class);
                        startActivity(intent);
                    }
                });
            }
        } else {
            filter = preferences.getInt(FILTER, 3);
            setupButton(v, R.id.actions, 1);
            setupButton(v, R.id.contacts, 2);
            setupButton(v, R.id.system, 4);
            vEvents.setListener(new HoursList.Listener() {
                @Override
                public int setHour(int h) {
                    int i;
                    for (i = 0; i < filtered.size(); i++) {
                        Event e = filtered.get(i);
                        LocalTime time = new LocalTime(e.time);
                        if (time.getHourOfDay() < h)
                            break;
                    }
                    i--;
                    if (i < 0)
                        i = 0;
                    return i;
                }
            });
        }

        if (loaded) {
            filterEvents(false);
            vProgress.setVisibility(View.GONE);
        } else {
            fetcher = new DataFetcher();
            error = false;
            loaded = false;
            fetcher.update();
        }

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                if (!car_id.equals(intent.getStringExtra(Names.ID)))
                    return;
                if (intent.getAction().equals(FetchService.ACTION_UPDATE)) {
                    LocalDate today = new LocalDate();
                    if (!today.equals(current))
                        return;
                    if (fetcher != null)
                        return;
                    fetcher = new DataFetcher();
                    fetcher.no_reload = true;
                    fetcher.update();
                }
                if (intent.getAction().equals(FetchService.ACTION_UPDATE))
                    api_key = preferences.getString(Names.Car.CAR_KEY + car_id, "");
            }
        };
        IntentFilter intFilter = new IntentFilter(FetchService.ACTION_UPDATE);
        intFilter.addAction(FetchService.ACTION_UPDATE_FORCE);
        getActivity().registerReceiver(br, intFilter);

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
        mPullToRefreshLayout.setPullEnabled(current.equals(new LocalDate()));
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(br);
        super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        MainActivity mainActivity = (MainActivity) activity;
        mainActivity.registerDateListener(this);
    }

    @Override
    public void onDestroy() {
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.unregisterDateListener(this);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
        outState.putLong(DATE, current.toDate().getTime());
        if (loaded) {
            byte[] data = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(events);
                out.writeObject(firstEvent);
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
            if ((data != null) && (data.length < 500000))
                outState.putByteArray(EVENTS_DATA, data);
        }
    }

    @Override
    public void dateChanged(LocalDate date) {
        current = date;
        vProgress.setVisibility(View.VISIBLE);
        vEvents.setVisibility(View.GONE);
        tvNoEvents.setVisibility(View.GONE);
        vError.setVisibility(View.GONE);
        fetcher = new DataFetcher();
        error = false;
        loaded = false;
        fetcher.update();
        mPullToRefreshLayout.setPullEnabled(current.equals(new LocalDate()));
    }

    void setupButton(View v, int id, int mask) {
        Button btn = (Button) v.findViewById(id);
        if ((mask & filter) != 0)
            btn.setBackgroundResource(R.drawable.pressed);
        btn.setTag(mask);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleButton(v);
            }
        });
    }

    void toggleButton(View v) {
        Button btn = (Button) v;
        int mask = (Integer) btn.getTag();
        if ((filter & mask) == 0) {
            filter |= mask;
            btn.setBackgroundResource(R.drawable.pressed);
        } else {
            filter &= ~mask;
            btn.setBackgroundResource(R.drawable.button);
        }
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(FILTER, filter);
        ed.commit();
        if (!error)
            filterEvents(false);
    }

    void filterEvents(boolean no_reload) {
        if (!loaded)
            return;
        filtered.clear();
        if (firstEvent != null)
            filtered.add(firstEvent);
        for (Event e : events) {
            if ((e.level == 0) || ((e.level & filter) != 0))
                filtered.add(e);
        }
        if (filtered.size() > 0) {
            if (no_events || !no_reload) {
                current_id = 0;
                vEvents.setAdapter(new EventsAdapter());
                vEvents.setVisibility(View.VISIBLE);
                tvNoEvents.setVisibility(View.GONE);
            } else {
                vEvents.notifyChanges();
            }
            no_events = false;
        } else {
            tvNoEvents.setText(getString(R.string.no_events));
            tvNoEvents.setVisibility(View.VISIBLE);
            vEvents.setVisibility(View.GONE);
            no_events = true;
        }
    }

    void showError() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvNoEvents.setVisibility(View.GONE);
                    vProgress.setVisibility(View.GONE);
                    vError.setVisibility(View.VISIBLE);
                    error = true;
                }
            });
        }
    }

    @Override
    public void onRefreshStarted(View view) {
        if (fetcher != null)
            return;
        fetcher = new DataFetcher();
        fetcher.no_reload = true;
        fetcher.update();
    }

    static class Event implements Serializable {
        long time;
        long id;
        int type;
        int level;
        int icon;
        String title;
        String text;
        String point;
        String course;
        String address;
    }

    static class EventType {
        String name;
        int icon;
        int level;
    }

    class DataFetcher extends HttpTask {

        LocalDate date;
        boolean no_reload;
        boolean first;

        @Override
        void result(JsonObject data) throws ParseException {
            if (getActivity() == null)
                return;
            done();
            if (!current.equals(date))
                return;
            Map<Long, Event> eventData = new HashMap<Long, Event>();
            for (Event e : events) {
                if (e.address == null)
                    continue;
                eventData.put(e.id, e);
            }
            if ((firstEvent != null) && (firstEvent.address != null)) {
                eventData.put(firstEvent.id, firstEvent);
            }
            events.clear();
            Map<Integer, EventType> event_types = new HashMap<Integer, EventType>();
            JsonArray types = data.get("types").asArray();
            for (int i = 0; i < types.size(); i++) {
                JsonObject type = types.get(i).asObject();
                EventType et = new EventType();
                et.name = type.get("name").asString();
                et.icon = R.drawable.e_system;
                JsonValue v = type.get("icon");
                if (v != null) {
                    try {
                        Context context = getActivity();
                        et.icon = context.getResources().getIdentifier("e_" + v.asString(), "drawable", context.getPackageName());
                    } catch (Exception ex) {
                        // ignore
                    }
                }
                int level = type.get("level").asInt();
                if (level != 0)
                    level = 1 << (level - 1);
                et.level = level;
                event_types.put(type.get("type").asInt(), et);
            }
            JsonArray res = data.get("events").asArray();
            if (!first)
                firstEvent = null;
            for (int i = 0; i < res.size(); i++) {
                JsonObject event = res.get(i).asObject();
                long id = event.get("id").asLong();
                Event e = new Event();
                e.id = id;
                e.type = event.get("type").asInt();
                e.time = event.get("time").asLong();
                if (event_types.containsKey(e.type)) {
                    String[] e_data = null;
                    JsonValue vData = event.get("data");
                    if (vData != null)
                        e_data = vData.asString().split("\\|");
                    EventType et = event_types.get(e.type);
                    String text = et.name;
                    for (int s = 0; ; s++) {
                        String pat = "{" + s + "}";
                        int pos = text.indexOf(pat);
                        if (pos < 0)
                            break;
                        String subst = "";
                        if ((e_data != null) && (s < e_data.length))
                            subst = e_data[s];
                        text = text.replace(pat, subst);
                    }
                    int pos = text.indexOf("\n");
                    if (pos < 0) {
                        e.title = text;
                    } else {
                        e.title = text.substring(0, pos);
                        e.text = text.substring(pos + 1);
                    }
                    if (et.level > 0)
                        e.level = 1 << (et.level - 1);
                    e.icon = et.icon;
                } else {
                    e.title = getString(R.string.event) + " #" + e.type;
                    e.level = 4;
                }
                Event ee = eventData.get(id);
                if (ee != null) {
                    e.address = ee.address;
                    e.point = ee.point;
                    e.course = ee.course;
                }
                if (first) {
                    firstEvent = e;
                    first = false;
                    continue;
                }
                events.add(e);
            }
            error = false;
            loaded = true;
            filterEvents(no_reload);
            vProgress.setVisibility(View.GONE);
        }

        @Override
        void error() {
            if (getActivity() == null)
                return;
            if (!no_reload)
                showError();
            done();
        }

        void done() {
            if (fetcher != this)
                return;
            mPullToRefreshLayout.setRefreshComplete();
            fetcher = null;
        }

        void update() {
            date = current;
            DateTime start = date.toDateTime(new LocalTime(0, 0));
            LocalDate next = date.plusDays(1);
            DateTime finish = next.toDateTime(new LocalTime(0, 0));
            if (pointer) {
                finish = new DateTime();
                start = finish.minusDays(30);
            }
            LocalDate today = new LocalDate();
            first = today.equals(current);
            execute(URL_EVENTS,
                    api_key,
                    start.toDate().getTime(),
                    finish.toDate().getTime(),
                    first,
                    pointer,
                    preferences.getString(Names.Car.AUTH + car_id, ""),
                    Locale.getDefault().getLanguage());
        }
    }

    class EventRequest extends HttpTask {

        long event_id;
        long event_time;

        EventRequest(long id, long time, int type) {
            event_id = id;
            event_time = time;
            if ((type == 88) || (type == 140) || (type == -116) || (type == 121) || (type == 122) || (type == 123) || (type == 124) || (type == 125)) {
                String auth = preferences.getString(Names.Car.AUTH + car_id, "");
                execute(URL_EVENT, api_key, id, time, "type", type, "auth", auth);
                return;
            }
            execute(URL_EVENT, api_key, id, time);
        }

        @Override
        void result(JsonObject res) throws ParseException {
            if (getActivity() == null)
                return;
            final JsonValue text = res.get("text");
            String data = "";
            JsonValue voltage = res.get("voltage");
            if (voltage != null) {
                JsonObject v = voltage.asObject();
                JsonValue main = v.get("main");
                if (main != null) {
                    double val = main.asDouble();
                    val += preferences.getInt(Names.Car.VOLTAGE_SHIFT + car_id, 0) / 20.;
                    data += getString(R.string.voltage) + ": " + String.format("%.2f", val) + "V\n";
                }
                JsonValue reserved = v.get("reserved");
                if (reserved != null)
                    data += getString(R.string.reserved) + ": " + reserved.asDouble() + "V\n";
            }
            JsonValue balance = res.get("balance");
            if (balance != null)
                data += getString(R.string.balance) + ": " + String.format("%.2f", balance.asDouble()) + "\n";
            JsonValue temperature = res.get("temperature");
            if (temperature != null) {
                JsonObject t = temperature.asObject();
                data += getString(R.string.temperature);
                if (t.get("t1") != null)
                    data += ": " + temp(t, 1) + "\n";
                for (String name : t.names()) {
                    try {
                        int sensor = Integer.parseInt(name.substring(1));
                        if (sensor == 1)
                            continue;
                        data += getString(R.string.sensor) + ": " + temp(t, sensor);
                    } catch (Exception ex) {
                        //ignore
                    }
                }
            }
            JsonValue vLevel = res.get("card_level");
            JsonValue vVoltage = res.get("card_voltage");
            if ((vLevel != null) && (vVoltage != null)) {
                String f = getString(R.string.card_info);
                data += String.format(f, vLevel.asInt(), vVoltage.asFloat());
                data += "\n";
            }
            if (text != null) {
                data += text.asString();
                data += "\n";
            }

            final String event_data = data;

            JsonValue value = res.get("gps");
            if (value != null) {
                JsonObject gps = value.asObject();
                final double lat = gps.get("lat").asDouble();
                final double lng = gps.get("lng").asDouble();
                final JsonValue course_value = gps.get("course");
                Address.Answer answer = new Address.Answer() {
                    @Override
                    public void result(String res) {
                        String addr = event_data;
                        if (text != null)
                            addr = text.asString() + "\n\n";
                        addr += lat + "," + lng;
                        if (res != null)
                            addr += "\n" + res;
                        String course = null;
                        if (course_value != null)
                            course = course_value.asInt() + "";
                        setAddress(addr, lat + ";" + lng, course);
                    }
                };
                Address.get(getActivity(), lat, lng, answer);
                return;
            }
            value = res.get("gsm");
            if (value != null) {
                final JsonObject gsm = value.asObject();
                final double lat = gsm.get("lat").asDouble();
                final double lng = gsm.get("lng").asDouble();
                Address.get(getActivity(), lat, lng, new Address.Answer() {
                    @Override
                    public void result(String res) {
                        String addr = event_data;
                        if (text != null)
                            addr = text.asString() + "\n\n";
                        addr += "MCC: " + gsm.get("cc").asInt();
                        addr += " NC: " + gsm.get("nc").asInt();
                        addr += " LAC: " + gsm.get("lac").asInt();
                        addr += " CID: " + gsm.get("cid").asInt();
                        if (res != null)
                            addr += "\n" + res;
                        setAddress(addr, lat + ";" + lng + ";" + gsm.get("sector").asString(), null);
                    }
                });
                return;
            }
            String addr = event_data;
            setAddress(addr, "", null);
        }

        String temp(JsonObject t, int sensor) {
            JsonValue temp = t.get("t" + sensor);
            if (temp == null)
                return "";
            int value = temp.asInt();
            if (sensor == 1) {
                value += preferences.getInt(Names.Car.TEMP_SIFT + car_id, 0);
            } else {
                String[] temp_config = preferences.getString(Names.Car.TEMP_SETTINGS + car_id, "").split(",");
                for (String s : temp_config) {
                    String[] data = s.split(":");
                    if (data.length != 3)
                        continue;
                    try {
                        if (Integer.parseInt(data[0]) == sensor) {
                            value += Integer.parseInt(data[1]);
                            break;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            return value + " \u00B0C";
        }

        @Override
        void error() {
            if (getActivity() == null)
                return;
            setAddress(getString(R.string.error_load), null, null);
        }

        void setAddress(String result, String point, String course) {
            for (Event e : filtered) {
                if (e.id == event_id) {
                    e.address = result;
                    e.point = point;
                    e.course = course;
                }
            }
            if (event_id != current_id)
                return;
            vEvents.notifyChanges();
        }
    }

    class EventsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return filtered.size();
        }

        @Override
        public Object getItem(int position) {
            return filtered.get(position);
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
                v = inflater.inflate(R.layout.event_item, null);
            }
            Event e = filtered.get(position);
            TextView tvName = (TextView) v.findViewById(R.id.name);
            TextView tvTime = (TextView) v.findViewById(R.id.time);
            ImageView icon = (ImageView) v.findViewById(R.id.icon);
            if (pointer) {
                DateFormat df = android.text.format.DateFormat.getDateFormat(getActivity());
                tvTime.setText(df.format(e.time) + " " + State.formatTime(getActivity(), e.time));
            } else {
                tvTime.setText(State.formatTime(getActivity(), e.time));
            }
            if (e.icon != 0) {
                icon.setImageResource(e.icon);
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.GONE);
            }
            tvName.setText(e.title);
            View progress = v.findViewById(R.id.progress);
            TextView tvAddress = (TextView) v.findViewById(R.id.address);
            if (e.id == current_id) {
                if (e.address == null) {
                    progress.setVisibility(View.VISIBLE);
                    tvAddress.setVisibility(View.GONE);
                } else {
                    tvAddress.setText(e.address);
                    progress.setVisibility(View.GONE);
                    tvAddress.setVisibility(View.VISIBLE);
                }
            } else {
                progress.setVisibility(View.GONE);
                tvAddress.setVisibility(View.GONE);
            }
            return v;
        }
    }

}
