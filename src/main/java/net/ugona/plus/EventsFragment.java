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
import android.widget.ListView;
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
import java.util.Map;
import java.util.Vector;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class EventsFragment extends Fragment
        implements MainActivity.DateChangeListener, OnRefreshListener {

    final static String URL_EVENTS = "https://car-online.ugona.net/events?skey=$1&begin=$2&end=$3&first=$4&pointer=$5";
    final static String URL_EVENT = "https://car-online.ugona.net/event?skey=$1&id=$2&time=$3";

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

    ListView lvEvents;
    TextView tvNoEvents;
    View vProgress;
    View vError;

    long current_id;

    LocalDate current;
    BroadcastReceiver br;

    DataFetcher fetcher;

    static final String FILTER = "filter";
    static final String DATE = "events_date";
    static final String EVENTS_DATA = "events";

    static class EventType {
        EventType(int type_, int string_, int icon_) {
            type = type_;
            string = string_;
            icon = icon_;
            filter = 4;
        }

        EventType(int type_, int string_, int icon_, int filter_) {
            type = type_;
            string = string_;
            icon = icon_;
            filter = filter_;
        }

        int type;
        int string;
        int icon;
        int filter;
    }

    static EventType[] event_types = {
            new EventType(1, R.string.light_shock, R.drawable.light_shock, 0),
            new EventType(2, R.string.ext_zone, R.drawable.ext_zone, 0),
            new EventType(3, R.string.heavy_shock, R.drawable.heavy_shock, 0),
            new EventType(4, R.string.inner_zone, R.drawable.inner_zone, 0),
            new EventType(5, R.string.trunk_open, R.drawable.boot_open, 2),
            new EventType(6, R.string.hood_open, R.drawable.e_hood_open, 2),
            new EventType(7, R.string.door_open, R.drawable.door_open, 2),
            new EventType(8, R.string.tilt, R.drawable.tilt, 0),
            new EventType(9, R.string.ignition_on, R.drawable.ignition_on, 2),
            new EventType(10, R.string.access_on, R.drawable.access_on, 2),
            new EventType(11, R.string.input1_on, R.drawable.input1_on, 2),
            new EventType(12, R.string.input2_on, R.drawable.input2_on, 2),
            new EventType(13, R.string.input3_on, R.drawable.input3_on, 2),
            new EventType(14, R.string.input4_on, R.drawable.input4_on, 2),
            new EventType(15, R.string.trunk_close, R.drawable.boot_close, 2),
            new EventType(16, R.string.hood_close, R.drawable.e_hood_close, 2),
            new EventType(17, R.string.door_close, R.drawable.door_close, 2),
            new EventType(18, R.string.ignition_off, R.drawable.ignition_off, 2),
            new EventType(19, R.string.access_off, R.drawable.access_off, 2),
            new EventType(20, R.string.input1_off, R.drawable.input1_off, 2),
            new EventType(21, R.string.input2_off, R.drawable.input2_off, 2),
            new EventType(22, R.string.input3_off, R.drawable.input3_off, 2),
            new EventType(23, R.string.input4_off, R.drawable.input4_off, 2),
            new EventType(24, R.string.guard_on, R.drawable.guard_on, 1),
            new EventType(25, R.string.guard_off, R.drawable.guard_off, 1),
            new EventType(26, R.string.reset, R.drawable.reset),
            new EventType(27, R.string.main_power_on, R.drawable.main_power_off, 0),
            new EventType(28, R.string.main_power_off, R.drawable.main_power_off, 0),
            new EventType(29, R.string.reserve_power_on, R.drawable.reserve_power_off, 0),
            new EventType(30, R.string.reserve_power_off, R.drawable.reserve_power_off, 0),
            new EventType(31, R.string.gsm_recover, R.drawable.gsm_recover),
            new EventType(32, R.string.gsm_fail, R.drawable.gsm_fail),
            new EventType(33, R.string.gsm_new, R.drawable.gsm_new),
            new EventType(34, R.string.gps_recover, R.drawable.gps_recover),
            new EventType(35, R.string.gps_fail, R.drawable.gps_fail),
            new EventType(37, R.string.trace_start, R.drawable.trace_start),
            new EventType(38, R.string.trace_stop, R.drawable.trace_stop),
            new EventType(39, R.string.trace_point, R.drawable.trace),
            new EventType(41, R.string.timer_event, R.drawable.timer),
            new EventType(42, R.string.user_call, R.drawable.user_call, 1),
            new EventType(43, R.string.rogue, R.drawable.rogue, 0),
            new EventType(44, R.string.rogue_off, R.drawable.rogue, 0),
            new EventType(45, R.string.motor_start_azd, R.drawable.motor_start, 1),
            new EventType(46, R.string.motor_start, R.drawable.motor_start, 1),
            new EventType(47, R.string.motor_stop, R.drawable.motor_stop, 1),
            new EventType(48, R.string.motor_start_error, R.drawable.motor_start_error, 1),
            new EventType(49, R.string.alarm_boot, R.drawable.alarm_boot, 0),
            new EventType(50, R.string.alarm_hood, R.drawable.alarm_hood, 0),
            new EventType(51, R.string.alarm_door, R.drawable.alarm_door, 0),
            new EventType(52, R.string.ignition_lock, R.drawable.ignition_lock, 0),
            new EventType(53, R.string.alarm_accessories, R.drawable.alarm_accessories, 0),
            new EventType(54, R.string.alarm_input1, R.drawable.alarm_input1, 0),
            new EventType(55, R.string.alarm_input2, R.drawable.alarm_input2, 0),
            new EventType(56, R.string.alarm_input3, R.drawable.alarm_input3, 0),
            new EventType(57, R.string.alarm_input4, R.drawable.alarm_input4, 0),
            new EventType(58, R.string.sms_request, R.drawable.user_sms, 1),
            new EventType(59, R.string.reset_modem, R.drawable.reset_modem),
            new EventType(60, R.string.gprs_on, R.drawable.gprs_on),
            new EventType(61, R.string.gprs_off, R.drawable.gprs_off),
            new EventType(65, R.string.reset, R.drawable.reset),
            new EventType(66, R.string.gsm_register_fail, R.drawable.gsm_fail),
            new EventType(68, R.string.net_error, R.drawable.system),
            new EventType(72, R.string.net_error, R.drawable.system),
            new EventType(74, R.string.error_read, R.drawable.system),
            new EventType(75, R.string.net_error, R.drawable.system),
            new EventType(76, R.string.reset_modem, R.drawable.reset_modem),
            new EventType(77, R.string.reset_modem, R.drawable.reset_modem),
            new EventType(78, R.string.reset_modem, R.drawable.reset_modem),
            new EventType(79, R.string.reset_modem, R.drawable.reset_modem),
            new EventType(80, R.string.reset_modem, R.drawable.reset_modem),
            new EventType(85, R.string.sos, R.drawable.sos, 0),
            new EventType(86, R.string.zone_in, R.drawable.zone_in),
            new EventType(87, R.string.zone_out, R.drawable.zone_out),
            new EventType(88, R.string.incomming_sms, R.drawable.user_sms, 1),
            new EventType(89, R.string.request_photo, R.drawable.request_photo, 1),
            new EventType(90, R.string.till_start, R.drawable.till_start),
            new EventType(91, R.string.end_move, R.drawable.system),
            new EventType(94, R.string.temp_change, R.drawable.temperature),
            new EventType(98, R.string.data_transfer, R.drawable.system),
            new EventType(100, R.string.reset, R.drawable.reset),
            new EventType(101, R.string.reset, R.drawable.reset),
            new EventType(105, R.string.reset_modem, R.drawable.reset_modem),
            new EventType(106, R.string.reset_modem, R.drawable.reset_modem),
            new EventType(107, R.string.reset_modem, R.drawable.reset_modem),
            new EventType(108, R.string.reset_modem, R.drawable.reset_modem),
            new EventType(110, R.string.valet_off, R.drawable.valet_off, 1),
            new EventType(111, R.string.lock_off1, R.drawable.lockclose01, 1),
            new EventType(112, R.string.lock_off2, R.drawable.lockclose02, 1),
            new EventType(113, R.string.lock_off3, R.drawable.lockclose03, 1),
            new EventType(114, R.string.lock_off4, R.drawable.lockclose04, 1),
            new EventType(115, R.string.lock_off5, R.drawable.lockclose05, 1),
            new EventType(120, R.string.valet_on, R.drawable.valet_on, 1),
            new EventType(121, R.string.lock_on1, R.drawable.lockcopen01, 1),
            new EventType(122, R.string.lock_on2, R.drawable.lockcopen02, 1),
            new EventType(123, R.string.lock_on3, R.drawable.lockcopen03, 1),
            new EventType(124, R.string.lock_on4, R.drawable.lockcopen04, 1),
            new EventType(125, R.string.lock_on5, R.drawable.lockcopen05, 1),
            new EventType(127, R.string.brk_data, R.drawable.system),
            new EventType(128, R.string.input5_on, R.drawable.input5_on, 2),
            new EventType(129, R.string.input5_off, R.drawable.input5_off, 2),
            new EventType(130, R.string.voice, R.drawable.voice),
            new EventType(131, R.string.download_events, R.drawable.settings),
            new EventType(132, R.string.can_on, R.drawable.can, 1),
            new EventType(133, R.string.can_off, R.drawable.can, 1),
            new EventType(134, R.string.input6_on, R.drawable.input6_on, 2),
            new EventType(135, R.string.input6_off, R.drawable.input6_off, 2),
            new EventType(136, R.string.low_battery, R.drawable.system, 0),
            new EventType(137, R.string.download_settings, R.drawable.settings),
            new EventType(138, R.string.guard2_on, R.drawable.guard_on, 1),
            new EventType(139, R.string.guard2_off, R.drawable.guard_off, 1),
            new EventType(140, R.string.lan_change, R.drawable.system, 0),
            new EventType(141, R.string.command, R.drawable.system, 1),
            new EventType(142, R.string.brake, R.drawable.brake, 2),
            new EventType(145, R.string.brake_on, R.drawable.brake, 2),
            new EventType(146, R.string.brake_off, R.drawable.brake, 2),
            new EventType(293, R.string.sos, R.drawable.sos, 0),
    };

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

        lvEvents = (ListView) v.findViewById(R.id.events);
        lvEvents.setClickable(true);
        lvEvents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Event e = filtered.get(position);
                if (e.id == current_id) {
                    if (e.point == null)
                        return;
                    String info = "<b>" + State.formatTime(getActivity(), e.time) + " ";
                    boolean found = false;
                    for (EventType et : event_types) {
                        if (et.type == e.type) {
                            found = true;
                            info += getString(et.string);
                        }
                    }
                    if (!found)
                        info += getString(R.string.event) + " #" + e.type;
                    info += "</b><br/>";
                    Intent i = new Intent(getActivity(), MapView.class);
                    String[] point = e.point.split(";");
                    if (point.length < 2)
                        return;
                    String point_data = ";" + point[0] + ";" + point[1] + ";" + e.course + ";" + info + e.address.replace("\n", "<br/>");
                    if (point.length > 2)
                        point_data += ";" + point[2];
                    i.putExtra(Names.POINT_DATA, point_data);
                    i.putExtra(Names.ID, car_id);
                    startActivity(i);
                    return;
                }
                current_id = e.id;
                EventsAdapter adapter = (EventsAdapter) lvEvents.getAdapter();
                adapter.notifyDataSetChanged();
                new EventRequest(filtered.get(position).id, filtered.get(position).time);
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
        api_key = preferences.getString(Names.CAR_KEY + car_id, "");
        pointer = preferences.getBoolean(Names.POINTER + car_id, false);

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
                    api_key = preferences.getString(Names.CAR_KEY + car_id, "");
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
        lvEvents.setVisibility(View.GONE);
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

    boolean isShow(int type) {
        for (EventType et : event_types) {
            if (et.type == type) {
                if (et.filter == 0)
                    return true;
                return (et.filter & filter) != 0;
            }
        }
        return (filter & 4) != 0;
    }

    void filterEvents(boolean no_reload) {
        if (!loaded)
            return;
        filtered.clear();
        if (firstEvent != null)
            filtered.add(firstEvent);
        for (Event e : events) {
            if (isShow(e.type))
                filtered.add(e);
        }
        if (filtered.size() > 0) {
            if (no_events || !no_reload) {
                current_id = 0;
                lvEvents.setAdapter(new EventsAdapter());
                lvEvents.setVisibility(View.VISIBLE);
                tvNoEvents.setVisibility(View.GONE);
            } else {
                EventsAdapter adapter = (EventsAdapter) lvEvents.getAdapter();
                if (adapter != null)
                    adapter.notifyDataSetChanged();
            }
            no_events = false;
        } else {
            tvNoEvents.setText(getString(R.string.no_events));
            tvNoEvents.setVisibility(View.VISIBLE);
            lvEvents.setVisibility(View.GONE);
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

    class DataFetcher extends HttpTask {

        LocalDate date;
        boolean no_reload;
        boolean first;

        @Override
        void result(JsonObject data) throws ParseException {
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
            JsonArray res = data.get("events").asArray();
            if (!first)
                firstEvent = null;
            for (int i = 0; i < res.size(); i++) {
                JsonObject event = res.get(i).asObject();
                long id = event.get("id").asLong();
                Event e = new Event();
                e.type = event.get("type").asInt();
                e.time = event.get("time").asLong();
                e.id = id;
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
                    pointer);
        }
    }

    class EventRequest extends HttpTask {

        long event_id;
        long event_time;

        EventRequest(long id, long time) {
            event_id = id;
            event_time = time;
            execute(URL_EVENT, api_key, id, time);
        }

        @Override
        void result(JsonObject res) throws ParseException {
            JsonValue value = res.get("gps");
            if (value != null) {
                JsonObject gps = value.asObject();
                final double lat = gps.get("lat").asDouble();
                final double lng = gps.get("lng").asDouble();
                final JsonValue course_value = gps.get("course");
                Address request = new Address() {
                    @Override
                    void result(String res) {
                        String addr = lat + "," + lng;
                        if (res != null)
                            addr += "\n" + res;
                        String course = null;
                        if (course_value != null)
                            course = course_value.asInt() + "";
                        setAddress(addr, lat + ";" + lng, course);
                    }
                };
                request.get(getActivity(), lat, lng);
                return;
            }
            value = res.get("gsm");
            if (value != null) {
                final JsonObject gsm = value.asObject();
                final double lat = gsm.get("lat").asDouble();
                final double lng = gsm.get("lng").asDouble();
                Address request = new Address() {
                    @Override
                    void result(String res) {
                        String addr = "MCC: " + gsm.get("cc").asInt();
                        addr += " NC: " + gsm.get("nc").asInt();
                        addr += " LAC: " + gsm.get("lac").asInt();
                        addr += " CID: " + gsm.get("cid").asInt();
                        if (res != null)
                            addr += "\n" + res;
                        setAddress(addr, lat + ";" + lng + ";" + gsm.get("sector").asString(), null);
                    }
                };
                request.get(getActivity(), lat, lng);
                return;
            }
            setAddress("", "", null);
        }

        @Override
        void error() {
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
            EventsAdapter adapter = (EventsAdapter) lvEvents.getAdapter();
            adapter.notifyDataSetChanged();
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
            boolean found = false;
            for (EventType et : event_types) {
                if (et.type == e.type) {
                    found = true;
                    tvName.setText(getString(et.string));
                    icon.setVisibility(View.VISIBLE);
                    icon.setImageResource(et.icon);
                }
            }
            if (!found) {
                tvName.setText(getString(R.string.event) + " #" + e.type);
                icon.setVisibility(View.GONE);
            }
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

    @Override
    public void onRefreshStarted(View view) {
        if (fetcher != null)
            return;
        fetcher = new DataFetcher();
        fetcher.no_reload = true;
        fetcher.update();
    }

    static class Event implements Serializable {
        int type;
        long time;
        long id;
        String point;
        String course;
        String address;
    }

}
