package net.ugona.plus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

public class EventsFragment extends MainFragment {

    static final String DATE = "date";
    static final String EVENTS_DATA = "data";
    static int[] ids = {R.id.level1, R.id.level2, R.id.level3, R.id.level4};
    boolean loaded;
    long current_id;
    int current_state;
    int filter;
    Vector<Event> events;
    Vector<Event> filtered;
    Event firstEvent;
    HoursList vEvents;
    TextView tvNoEvents;
    View vError;
    CarState state;
    DataFetcher fetcher;
    BroadcastReceiver br;
    boolean isButton;

    @Override
    int layout() {
        return R.layout.events;
    }

    @Override
    boolean isShowDate() {
        return true;
    }

    @Override
    void changeDate() {
        onRefresh();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (isButton)
            return;
        CarState carState = CarState.get(getActivity(), id());
        inflater.inflate(R.menu.events, menu);
        String[] levels = carState.getEvents();
        int len = 0;
        if (levels != null)
            len = levels.length;
        if (len > 4)
            len = 4;
        int i;
        for (i = 0; i < len; i++) {
            MenuItem item = menu.findItem(ids[i]);
            item.setTitle(levels[i]);
            item.setChecked((filter & (1 << i)) != 0);
        }
        for (; i < 4; i++) {
            menu.removeItem(ids[i]);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.getLong(DATE) == date()) {
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
        }

        vEvents = (HoursList) v.findViewById(R.id.events);
        vEvents.disableDivider();
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
                    Intent intent = new Intent(getActivity(), MapEventActivity.class);
                    String[] point = e.point.split(";");
                    if (point.length < 2)
                        return;
                    String point_data = point[0] + ";" + point[1] + ";" + e.course + ";" + info + e.address;
                    if (point.length > 2)
                        point_data += ";" + point[2];
                    intent.putExtra(Names.POINT_DATA, point_data);
                    intent.putExtra(Names.TITLE, State.formatTime(getActivity(), e.time));
                    intent.putExtra(Names.ID, id());
                    getActivity().startActivity(intent);
                    return;
                }
                current_id = e.id;
                if ((e.text != null) && (e.point != null) && (e.address != null)) {
                    current_state = 0;
                    vEvents.notifyChanges();
                    return;
                }
                current_state = 1;
                vEvents.notifyChanges();
                new EventRequest(e.id, e.time, e.type);
            }
        });

        tvNoEvents = (TextView) v.findViewById(R.id.no_events);
        vError = v.findViewById(R.id.error);
        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });

        filtered = new Vector<Event>();
        if (events == null)
            events = new Vector<Event>();

        state = CarState.get(getActivity(), id());
        if (state.isPointer()) {
            filter = 7;
            View vFooter = v.findViewById(R.id.footer);
            if (vFooter != null)
                vFooter.setVisibility(View.GONE);
        } else {
            CarConfig config = CarConfig.get(getActivity(), id());
            filter = config.getEvent_filter();
            CarState carState = CarState.get(getActivity(), id());
            String[] levels = carState.getEvents();
            int len = 0;
            if (levels != null)
                len = levels.length;
            if (len > 4)
                len = 4;
            int i;
            for (i = 0; i < len; i++) {
                Button button = (Button) v.findViewById(ids[i]);
                if (button == null)
                    continue;
                button.setText(levels[i]);
                button.setVisibility(View.VISIBLE);
                setupButton(v, ids[i], 1 << i);
            }
            vEvents.setListener(new HoursList.Listener() {
                @Override
                public int setHour(int h) {
                    int i;
                    Calendar calendar = Calendar.getInstance();
                    for (i = 0; i < filtered.size(); i++) {
                        Event e = filtered.get(i);
                        calendar.setTimeInMillis(e.time);
                        if (calendar.get(Calendar.HOUR_OF_DAY) < h)
                            break;
                    }
                    i--;
                    if (i < 0)
                        i = 0;
                    return i;
                }
            });
        }

        isButton = (v.findViewById(R.id.level1) != null);

        if (loaded) {
            filterEvents(false);
        } else {
            onRefresh();
        }

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    if (vError.getVisibility() == View.VISIBLE)
                        onRefresh();
                }
                if (!id().equals(intent.getStringExtra(Names.ID)))
                    return;
                try {
                    if (intent.getAction().equals(Names.UPDATED)) {
                        Calendar today = Calendar.getInstance();
                        Calendar d = Calendar.getInstance();
                        d.setTimeInMillis(date());
                        if (loaded &&
                                ((today.get(Calendar.DAY_OF_YEAR) != d.get(Calendar.DAY_OF_YEAR)) ||
                                        (today.get(Calendar.YEAR) != d.get(Calendar.YEAR))))
                            return;
                        if (fetcher != null)
                            return;
                        fetcher = new DataFetcher();
                        fetcher.no_reload = true;
                        fetcher.update();
                    }
                    if (intent.getAction().equals(Names.CONFIG_CHANGED)) {
                        if (fetcher != null)
                            fetcher.cancel();
                        fetcher = new DataFetcher();
                        fetcher.no_reload = true;
                        fetcher.update();
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
        };
        IntentFilter intFilter = new IntentFilter(Names.UPDATED);
        intFilter.addAction(Names.CONFIG_CHANGED);
        intFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(br, intFilter);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(br);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.level1:
                toggle(1);
                return true;
            case R.id.level2:
                toggle(2);
                return true;
            case R.id.level3:
                toggle(4);
                return true;
            case R.id.level4:
                toggle(8);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DATE, date());
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
    public void onRefresh() {
        super.onRefresh();
        vError.setVisibility(View.GONE);
        if (fetcher != null)
            fetcher.cancel();
        fetcher = new DataFetcher();
        loaded = false;
        fetcher = new DataFetcher();
        fetcher.update();
    }

    void setupButton(View v, int id, int mask) {
        Button btn = (Button) v.findViewById(id);
        if (btn == null)
            return;
        if ((mask & filter) != 0) {
            btn.setBackgroundResource(R.drawable.pressed);
            btn.setTextColor(getResources().getColor(R.color.main));
        }
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
            btn.setTextColor(getResources().getColor(R.color.main));
        } else {
            filter &= ~mask;
            btn.setBackgroundResource(R.drawable.button);
            btn.setTextColor(getResources().getColor(android.R.color.white));
        }
        CarConfig config = CarConfig.get(getActivity(), id());
        config.setEvent_filter(filter);
        if (loaded)
            filterEvents(false);
    }

    void toggle(int mask) {
        if ((filter & mask) == 0) {
            filter |= mask;
        } else {
            filter &= ~mask;
        }
        CarConfig config = CarConfig.get(getActivity(), id());
        config.setEvent_filter(filter);
        if (loaded)
            filterEvents(false);
        MainActivity activity = (MainActivity) getActivity();
        activity.updateMenu();
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
            if (!no_reload)
                current_id = 0;
            if (vEvents.getAdapter() == null) {
                vEvents.setAdapter(new EventsAdapter());
            } else {
                vEvents.notifyChanges();
            }
            vEvents.setVisibility(View.VISIBLE);
            tvNoEvents.setVisibility(View.GONE);
        } else {
            tvNoEvents.setText(getString(R.string.no_events));
            tvNoEvents.setVisibility(View.VISIBLE);
            vEvents.setVisibility(View.GONE);
        }
        vError.setVisibility(View.GONE);
    }

    static class Event implements Serializable {
        long time;
        long id;
        long type;
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

    static class EventsParams implements Serializable {
        String skey;
        long begin;
        long end;
        int level;
        Integer first;
        Integer pointer;
        String lang;
    }

    static class EventParams implements Serializable {
        String skey;
        long id;
        long time;
        long type;
        String lang;
    }

    class DataFetcher extends HttpTask {

        boolean no_reload;
        boolean first;

        @Override
        void result(JsonObject data) throws ParseException {
            if (getActivity() == null)
                return;
            done();
            @SuppressLint("UseSparseArrays") Map<Long, Event> eventData = new HashMap<Long, Event>();
            for (Event e : events) {
                if (e.address == null)
                    continue;
                eventData.put(e.id, e);
            }
            if ((firstEvent != null) && (firstEvent.address != null)) {
                eventData.put(firstEvent.id, firstEvent);
            }
            events.clear();
            Map<Long, EventType> event_types = new HashMap<Long, EventType>();
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
                event_types.put(type.get("type").asLong(), et);
            }
            JsonArray res = data.get("events").asArray();
            if (!first)
                firstEvent = null;
            for (int i = 0; i < res.size(); i++) {
                JsonObject event = res.get(i).asObject();
                long id = i + 1;
                JsonValue idValue = event.get("id");
                if (idValue != null)
                    id = idValue.asLong();
                Event e = new Event();
                e.id = id;
                e.type = event.get("type").asLong();
                e.time = event.get("time").asLong();
                if (event_types.containsKey(e.type)) {
                    JsonValue vData = event.get("data");
                    JsonArray aData = null;
                    if (vData != null)
                        aData = vData.asArray();
                    EventType et = event_types.get(e.type);
                    String text = et.name;
                    for (int s = 0; ; s++) {
                        String pat = "{" + s + "}";
                        int pos = text.indexOf(pat);
                        if (pos < 0)
                            break;
                        String subst = "";
                        if ((aData != null) && (s < aData.size()))
                            subst = aData.get(s).asString();
                        text = text.replace(pat, subst);
                    }
                    text = text.replaceAll("\\|", "\n");
                    int pos = text.indexOf("\n");
                    if (pos < 0) {
                        e.title = text;
                    } else {
                        e.title = text.substring(0, pos);
                        e.text = text.substring(pos + 1);
                    }
                    e.level = et.level;
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
            loaded = true;
            filterEvents(no_reload);
        }

        @Override
        void error() {
            if (getActivity() == null)
                return;
            if (!no_reload) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvNoEvents.setVisibility(View.GONE);
                            vEvents.setVisibility(View.GONE);
                            vError.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
            done();
        }

        void done() {
            if (fetcher != this)
                return;
            fetcher = null;
            refreshDone();
        }

        void update() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long start = calendar.getTimeInMillis();
            calendar.add(Calendar.DATE, 1);
            long finish = calendar.getTimeInMillis();
            if (state.isPointer()) {
                calendar.add(Calendar.DATE, -30);
                start = calendar.getTimeInMillis();
            }
            first = finish > new Date().getTime();
            EventsParams params = new EventsParams();
            CarConfig config = CarConfig.get(getActivity(), id());
            params.skey = config.getKey();
            params.begin = start;
            params.end = finish;
            if (params.skey.equals("")) {
                fetcher = null;
                refreshDone();
                return;
            }
            if (first)
                params.first = 1;
            if (state.isPointer())
                params.pointer = 1;
            params.level = 4;
            params.lang = Locale.getDefault().getLanguage();
            execute("/events", params);
        }
    }

    class EventRequest extends HttpTask {

        long event_id;
        long event_time;

        EventRequest(long id, long time, long type) {
            event_id = id;
            event_time = time;
            EventParams params = new EventParams();
            CarConfig config = CarConfig.get(getActivity(), id());
            params.skey = config.getKey();
            params.id = id;
            params.time = time;
            params.type = type;
            params.lang = Locale.getDefault().getLanguage();
            execute("/event", params);
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
                    CarConfig config = CarConfig.get(getActivity(), id());
                    val += config.getVoltage_shift() / 20.;
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
                String[] temp = temperature.asString().split(";");
                for (String t : temp) {
                    String[] parts = t.split(":");
                    switch (Integer.parseInt(parts[0])) {
                        case 0:
                            data += getString(R.string.temperature);
                            break;
                        case 1:
                            data += getString(R.string.temp_engine);
                            break;
                        case 2:
                            data += getString(R.string.temp_salon);
                            break;
                        case 3:
                            data += getString(R.string.temp_ext);
                            break;
                        default:
                            data += getString(R.string.temperature);
                            if (!parts[2].equals("")) {
                                data += " (";
                                data += getString(R.string.sensor);
                                data += " ";
                                data += parts[2];
                                data += ")";
                            }
                    }
                    data += ": " + parts[1] + " \u00B0C\n";
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
                        String addr = lat + "," + lng;
                        if (res != null)
                            addr += "\n" + res;
                        String course = null;
                        if (course_value != null)
                            course = course_value.asInt() + "";
                        setAddress(event_data, addr, lat + ";" + lng, course);
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
                        String addr = "MCC: " + gsm.get("cc").asInt();
                        addr += " NC: " + gsm.get("nc").asInt();
                        addr += " LAC: " + gsm.get("lac").asInt();
                        addr += " CID: " + gsm.get("cid").asInt();
                        if (res != null)
                            addr += "\n" + res;
                        setAddress(event_data, addr, lat + ";" + lng + ";" + gsm.get("sector").asString(), null);
                    }
                });
                return;
            }
            setAddress(event_data, "", "", null);
        }

        String temp(JsonObject t, int sensor) {
            JsonValue temp = t.get("t" + sensor);
            if (temp == null)
                return "";
            int value = temp.asInt();
            CarConfig config = CarConfig.get(getActivity(), id());
            String[] temp_config = config.getTemp_settings().split(",");
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
            return value + " \u00B0C";
        }

        @Override
        void error() {
            if (event_id == current_id) {
                current_state = 2;
                vEvents.notifyChanges();
            }
        }

        void setAddress(String text, String address, String point, String course) {
            if (text.equals(""))
                text = null;
            for (Event e : filtered) {
                if (e.id == event_id) {
                    if (text != null) {
                        e.text = text;
                    } else {
                        if (e.text == null)
                            e.text = "";
                    }
                    e.address = address;
                    e.point = point;
                    e.course = course;
                }
            }
            if (event_id != current_id)
                return;
            current_state = 0;
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
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                v = inflater.inflate(R.layout.event_item, null);
            }
            Event e = filtered.get(position);
            TextView tvName = (TextView) v.findViewById(R.id.name);
            TextView tvTime = (TextView) v.findViewById(R.id.time);
            ImageView icon = (ImageView) v.findViewById(R.id.icon);
            if (state.isPointer()) {
                DateFormat df = android.text.format.DateFormat.getDateFormat(getActivity());
                tvTime.setText(df.format(e.time) + " " + State.formatTime(getActivity(), e.time));
            } else {
                tvTime.setText(State.formatTime(getActivity(), e.time));
            }
            if (e.icon != 0) {
                icon.setImageResource(e.icon);
            } else {
                icon.setImageResource(R.drawable.e_system);
            }
            tvName.setText(e.title);
            View progress = v.findViewById(R.id.progress);
            TextView tvAddress = (TextView) v.findViewById(R.id.address);
            if (e.id == current_id) {
                if (current_state == 1) {
                    progress.setVisibility(View.VISIBLE);
                    tvAddress.setVisibility(View.GONE);
                } else if (current_state == 2) {
                    tvAddress.setText(R.string.error_load);
                    progress.setVisibility(View.GONE);
                    tvAddress.setVisibility(View.VISIBLE);
                } else {
                    String text = "";
                    if (e.text != null)
                        text += e.text;
                    if (e.address != null) {
                        if (!text.equals(""))
                            text += "\n";
                        text += e.address;
                    }
                    tvAddress.setText(text);
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
