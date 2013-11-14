package net.ugona.plus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class TracksFragment extends Fragment
        implements MainActivity.DateChangeListener {

    final String DATE = "tracks_date";
    final String TRACK = "track";

    SharedPreferences preferences;
    String api_key;

    LocalDate current;
    String car_id;
    int track_id;

    Vector<Tracks.Track> tracks;
    Map<Long, Integer> events;

    boolean loaded;
    int progress;

    TextView tvSummary;
    View vError;
    View vSpace;
    ProgressBar prgFirst;
    ProgressBar prgMain;
    TextView tvLoading;
    ListView lvTracks;

    final static String TELEMETRY = "http://api.car-online.ru/v2?get=telemetry&skey=$1&begin=$2&end=$3&content=json";
    final static String EVENTS = "http://api.car-online.ru/v2?get=events&skey=$1&begin=$2&end=$3&content=json";
    final static String GPSLIST = "http://api.car-online.ru/v2?get=gpslist&skey=$1&begin=$2&end=$3&content=json";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tracks, container, false);
        if (current == null)
            current = new LocalDate();
        if (savedInstanceState != null) {
            car_id = savedInstanceState.getString(Names.ID);
            current = new LocalDate(savedInstanceState.getLong(DATE));
            byte[] track_data = savedInstanceState.getByteArray(TRACK);
            if (track_data != null) {
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(track_data);
                    ObjectInput in = new ObjectInputStream(bis);
                    tracks = (Vector<Tracks.Track>) in.readObject();
                    in.close();
                    bis.close();
                    loaded = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        api_key = preferences.getString(Names.CAR_KEY + car_id, "");

        tvSummary = (TextView) v.findViewById(R.id.summary);
        lvTracks = (ListView) v.findViewById(R.id.tracks);
        prgFirst = (ProgressBar) v.findViewById(R.id.first_progress);
        prgMain = (ProgressBar) v.findViewById(R.id.progress);
        tvLoading = (TextView) v.findViewById(R.id.loading);
        vError = v.findViewById(R.id.error);
        vSpace = v.findViewById(R.id.space);

        vError.setClickable(true);
        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateChanged(current);
            }
        });

        tvSummary.setClickable(true);
        tvSummary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loaded)
                    showDay();
            }
        });

        lvTracks.setClickable(true);
        lvTracks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TracksAdapter adapter = (TracksAdapter) lvTracks.getAdapter();
                if (adapter.selected == position) {
                    showTrack(position);
                    return;
                }
                adapter.selected = position;
                adapter.notifyDataSetChanged();
            }
        });

        if (loaded) {
            all_done();
        } else {
            DataFetcher fetcher = new DataFetcher();
            fetcher.update(current);
        }
        return v;
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
                out.writeObject(tracks);
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
            if ((data != null) && (data.length < 500000))
                outState.putByteArray(TRACK, data);
        }
    }

    @Override
    public void dateChanged(LocalDate date) {
        current = date;
        tvSummary.setText("");
        vError.setVisibility(View.GONE);
        lvTracks.setVisibility(View.GONE);
        vSpace.setVisibility(View.VISIBLE);
        tvLoading.setVisibility(View.VISIBLE);
        prgFirst.setVisibility(View.VISIBLE);
        prgMain.setVisibility(View.VISIBLE);
        prgMain.setProgress(0);
        DataFetcher fetcher = new DataFetcher();
        fetcher.update(current);
    }

    void showError() {
        Activity activity = getActivity();
        if (activity == null)
            return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                vError.setVisibility(View.VISIBLE);
                lvTracks.setVisibility(View.GONE);
                vSpace.setVisibility(View.VISIBLE);
                tvLoading.setVisibility(View.GONE);
                prgFirst.setVisibility(View.GONE);
                prgMain.setVisibility(View.GONE);
            }
        });
    }

    void showTrack(int index) {
        Intent intent = new Intent(getActivity(), TrackView.class);
        Tracks.Track track = tracks.get(index);
        Vector<Tracks.Track> track1 = new Vector<Tracks.Track>();
        track1.add(track);
        if (!setTrack(track1, intent))
            return;
        LocalDateTime begin = new LocalDateTime(track.begin);
        LocalDateTime end = new LocalDateTime(track.end);
        intent.putExtra(Names.TITLE, begin.toString("d MMMM HH:mm") + "-" + end.toString("HH:mm"));
        startActivity(intent);
    }

    void showDay() {
        Intent intent = new Intent(getActivity(), TrackView.class);
        if (!setTrack(tracks, intent))
            return;
        intent.putExtra(Names.TITLE, current.toString("d MMMM"));
        startActivity(intent);
    }

    boolean setTrack(Vector<Tracks.Track> tracks, Intent intent) {
        byte[] data = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(tracks);
            data = bos.toByteArray();
            out.close();
            bos.close();
        } catch (Exception ex) {
            // ignore
        }
        if (data == null)
            return false;
        if (data.length > 500000) {
            try {
                File outputDir = getActivity().getCacheDir();
                File file = File.createTempFile("track", "dat", outputDir);
                FileOutputStream f = new FileOutputStream(file);
                f.write(data);
                intent.putExtra(Names.TRACK_FILE, file.getAbsolutePath());
                f.close();
            } catch (Exception ex) {
                return false;
            }
        } else {
            intent.putExtra(Names.TRACK, data);
        }
        return true;
    }

    void all_done() {
        prgFirst.setVisibility(View.GONE);
        tvSummary.setVisibility(View.VISIBLE);
        if (tracks.size() == 0) {
            tvSummary.setText(getString(R.string.no_data));
            prgMain.setVisibility(View.GONE);
            tvLoading.setVisibility(View.GONE);
            return;
        }
        double mileage = 0;
        double max_speed = 0;
        long time = 0;
        long start = current.toDateTime(new LocalTime(0, 0)).toDate().getTime();
        LocalDate next = current.plusDays(1);
        long finish = next.toDateTime(new LocalTime(0, 0)).toDate().getTime();
        for (Tracks.Track track : tracks) {
            long begin = track.begin;
            if (begin < start)
                begin = start;
            long end = track.end;
            if (end > finish)
                end = finish;
            time += (end - begin);
            if (track.day_max_speed > max_speed)
                max_speed = track.day_max_speed;
            mileage += track.day_mileage;
        }
        double avg_speed = mileage * 3600000. / time;
        String status = getString(R.string.status);
        status = String.format(status, mileage, timeFormat((int) (time / 60000)), avg_speed, max_speed);
        tvSummary.setText(status);

        tvLoading.setVisibility(View.GONE);
        prgMain.setVisibility(View.GONE);
        vSpace.setVisibility(View.GONE);
        lvTracks.setVisibility(View.VISIBLE);
        lvTracks.setAdapter(new TracksAdapter());

        loaded = true;
    }

    class DataFetcher extends HttpTask {

        LocalDate date;

        boolean noData() {
            return false;
        }

        @Override
        void result(JSONObject data) throws JSONException {
            if (!current.equals(date))
                return;
            tracks = new Vector<Tracks.Track>();
            events = new HashMap<Long, Integer>();
            int ways = data.getInt("waysCount");
            if ((ways == 0) && noData())
                return;
            prgFirst.setVisibility(View.GONE);
            tvSummary.setVisibility(View.VISIBLE);
            if (ways == 0) {
                all_done();
                return;
            }
            prgMain.setMax(ways * 2 + 5);
            progress = 1;
            prgMain.setProgress(1);
            double mileage = data.getDouble("mileage") / 1000;
            double avg_speed = data.getDouble("averageSpeed");
            double max_speed = data.getDouble("maxSpeed");
            int engine_time = data.getInt("engineTime") / 60000;
            String status = getString(R.string.status);
            status = String.format(status, mileage, timeFormat(engine_time), avg_speed, max_speed);
            tvSummary.setText(status);
            TracksFetcher tracksFetcher = new TracksFetcher();
            tracksFetcher.update();
        }

        @Override
        void error() {
            showError();
        }

        void update(LocalDate d) {
            date = d;
            current = d;
            loaded = false;
            DateTime start = date.toDateTime(new LocalTime(0, 0));
            LocalDate next = date.plusDays(1);
            DateTime finish = next.toDateTime(new LocalTime(0, 0));
            execute(TELEMETRY,
                    api_key,
                    start.toDate().getTime() + "",
                    finish.toDate().getTime() + "");
        }
    }

    class TracksFetcher extends HttpTask {
        int id;

        @Override
        void result(JSONObject res) throws JSONException {
            if (id != track_id)
                return;
            JSONArray list = res.getJSONArray("events");
            int first_type = 0;
            int last_type = 0;
            long first_time = end_time;
            long last_time = start_time;
            for (int i = list.length() - 1; i >= 0; i--) {
                JSONObject e = list.getJSONObject(i);
                int type = e.getInt("eventType");
                if ((type == 37) || (type == 38) || (type == 39)) {

                    if ((type == 37) || (type == 38)) {
                        Log.v("event", type + " " + e.getLong("eventId"));
                    }

                    events.put(e.getLong("eventId"), type);
                    long time = e.getLong("eventTime");
                    if (time < first_time) {
                        first_time = time;
                        first_type = type;
                    }
                    if (time > last_time) {
                        last_time = time;
                        last_type = type;
                    }
                }
            }
            long time = last_time + 600000;
            if (last_type == 38)
                time = last_time + 60000;
            boolean next = time > end_time;

            time = first_time - 600000;
            if (first_type == 37)
                time = first_time - 60000;
            if (time < start_time) {
                PrevTracksFetcher fetcher = new PrevTracksFetcher(next);
                fetcher.update(start_time, end_time);
                return;
            }
            if (next) {
                NextTracksFetcher fetcher = new NextTracksFetcher();
                fetcher.update(start_time, end_time);
                return;
            }

            prgMain.setProgress(++progress);
            TrackDataFetcher fetcher = new TrackDataFetcher();
            fetcher.update(id, start_time, end_time);
        }

        @Override
        void error() {
            showError();
        }

        void update() {
            id = ++track_id;
            DateTime start = current.toDateTime(new LocalTime(0, 0));
            LocalDate next = current.plusDays(1);
            DateTime finish = next.toDateTime(new LocalTime(0, 0));
            start_time = start.toDate().getTime();
            end_time = finish.toDate().getTime();
            execute(EVENTS, api_key, start_time + "", end_time + "");
        }

        long start_time;
        long end_time;
    }

    class PrevTracksFetcher extends HttpTask {
        int id;
        boolean next;

        PrevTracksFetcher(boolean do_next) {
            next = do_next;
        }

        @Override
        void result(JSONObject res) throws JSONException {
            if (id != track_id)
                return;
            JSONArray list = res.getJSONArray("events");
            if (list.length() > 0) {
                Vector<EventInfo> es = new Vector<EventInfo>();
                for (int i = list.length() - 1; i >= 0; i--) {
                    JSONObject e = list.getJSONObject(i);
                    int type = e.getInt("eventType");
                    if ((type == 37) || (type == 38) || (type == 39)) {
                        EventInfo event = new EventInfo();
                        event.time = e.getLong("eventTime");
                        event.id = e.getLong("eventId");
                        event.type = type;
                        es.add(event);
                    }
                }
                Collections.sort(es, new Comparator<EventInfo>() {
                    @Override
                    public int compare(EventInfo lhs, EventInfo rhs) {
                        if (lhs.time < rhs.time)
                            return -1;
                        if (lhs.time > rhs.time)
                            return 1;
                        return 0;
                    }
                });
                long last_time = start_time;
                for (int i = es.size() - 1; i >= 0; i--) {
                    EventInfo e = es.get(i);
                    long time = e.time + 600000;
                    if (e.type == 38)
                        time = e.time + 60000;
                    if (time < last_time)
                        break;
                    last_time = e.time;
                    start_time = e.time;
                    events.put(e.id, e.type);
                }
            }
            prgMain.setProgress(++progress);
            if (next) {
                NextTracksFetcher fetcher = new NextTracksFetcher();
                fetcher.update(start_time, end_time);
                return;
            }
            TrackDataFetcher fetcher = new TrackDataFetcher();
            fetcher.update(id, start_time, end_time);
        }

        @Override
        void error() {
            showError();
        }

        void update(long start, long end) {
            id = ++track_id;
            start_time = start;
            end_time = end;
            execute(EVENTS,
                    api_key,
                    (start_time - 86400000) + "",
                    start_time + "");
        }

        long start_time;
        long end_time;
    }

    class NextTracksFetcher extends HttpTask {
        int id;

        @Override
        void result(JSONObject res) throws JSONException {
            if (id != track_id)
                return;
            JSONArray list = res.getJSONArray("events");
            if (list.length() > 0) {
                Vector<EventInfo> es = new Vector<EventInfo>();
                for (int i = list.length() - 1; i >= 0; i--) {
                    JSONObject e = list.getJSONObject(i);
                    int type = e.getInt("eventType");
                    if ((type == 37) || (type == 38) || (type == 39)) {
                        EventInfo event = new EventInfo();
                        event.time = e.getLong("eventTime");
                        event.id = e.getLong("eventId");
                        event.type = type;
                        es.add(event);
                    }
                }
                Collections.sort(es, new Comparator<EventInfo>() {
                    @Override
                    public int compare(EventInfo lhs, EventInfo rhs) {
                        if (lhs.time < rhs.time)
                            return -1;
                        if (lhs.time > rhs.time)
                            return 1;
                        return 0;
                    }
                });
                long first_time = end_time;
                for (int i = 0; i < es.size(); i++) {
                    EventInfo e = es.get(i);
                    long time = e.time - 600000;
                    if (e.type == 38)
                        time = e.time - 60000;
                    if (time > first_time)
                        break;
                    first_time = e.time;
                    end_time = e.time;
                    events.put(e.id, e.type);
                }

            }
            prgMain.setProgress(++progress);
            TrackDataFetcher fetcher = new TrackDataFetcher();
            fetcher.update(id, start_time, end_time);
        }

        @Override
        void error() {
            showError();
        }

        void update(long start, long end) {
            id = ++track_id;
            start_time = start;
            end_time = end;
            execute(EVENTS,
                    api_key,
                    end + "",
                    (end + 86400000) + "");
        }

        long start_time;
        long end_time;
    }

    class TrackDataFetcher extends HttpTask {
        int id;

        @Override
        void result(JSONObject res) throws JSONException {
            if (id != track_id)
                return;

            Vector<PointInfo> points = new Vector<PointInfo>();

            JSONArray list = res.getJSONArray("gpslist");
            for (int i = list.length() - 1; i >= 0; i--) {
                JSONObject p = list.getJSONObject(i);
                long id = p.getLong("eventId");
                if (!events.containsKey(id))
                    continue;
                int type = events.get(id);

                if ((type == 37) || (type == 38)) {
                    Log.v("t", type + " " + p.toString());
                }

                events.remove(id);
                if (!p.getBoolean("valid"))
                    continue;

                Tracks.Point point = new Tracks.Point();
                point.speed = p.getDouble("speed");
                point.latitude = p.getDouble("latitude");
                point.longitude = p.getDouble("longitude");
                point.time = p.getLong("gpsTime");

                PointInfo pi = new PointInfo();
                pi.point = point;
                pi.type = type;
                points.add(pi);
            }

            Collections.sort(points, new Comparator<PointInfo>() {
                @Override
                public int compare(PointInfo lhs, PointInfo rhs) {
                    if (lhs.point.time < rhs.point.time)
                        return -1;
                    if (lhs.point.time > rhs.point.time)
                        return 1;
                    return 0;
                }
            });

            long last_time = 0;
            int last_type = 0;
            for (PointInfo pi : points) {
                long prev_time = pi.point.time - 600000;
                if ((pi.type == 37) || (last_type == 38))
                    prev_time = pi.point.time - 60000;
                if (prev_time > last_time) {
                    Tracks.Track track = new Tracks.Track();
                    track.begin = pi.point.time;
                    track.end = pi.point.time;
                    track.track = new Vector<Tracks.Point>();
                    tracks.add(track);
                }
                if (tracks.size() == 0) {
                    Tracks.Track track = new Tracks.Track();
                    track.begin = pi.point.time;
                    track.end = pi.point.time;
                    track.track = new Vector<Tracks.Point>();
                    tracks.add(track);
                }
                Tracks.Track track = tracks.get(tracks.size() - 1);
                track.end = pi.point.time;
                track.track.add(pi.point);
                last_time = pi.point.time;
                last_type = pi.type;
            }

            for (int i = 0; i < tracks.size(); i++) {
                Vector<Tracks.Point> track = tracks.get(i).track;
                while (track.size() > 2) {
                    Tracks.Point p1 = track.get(0);
                    Tracks.Point p2 = track.get(1);
                    if ((p1.speed > 0) || (p2.speed > 0))
                        break;
                    track.remove(0);
                }
                for (int n = track.size() - 1; n > 0; n--) {
                    Tracks.Point p1 = track.get(n);
                    Tracks.Point p2 = track.get(n - 1);
                    if ((p1.speed > 0) || (p2.speed > 0))
                        break;
                    track.remove(n);
                }
                if (track.size() <= 2) {
                    tracks.remove(i);
                    i--;
                    continue;
                }
                double distance = 0;
                double day_distance = 0;
                double max_speed = 0;
                double day_max_speed = 0;
                long start = current.toDateTime(new LocalTime(0, 0)).toDate().getTime();
                LocalDate next = current.plusDays(1);
                long finish = next.toDateTime(new LocalTime(0, 0)).toDate().getTime();
                for (int n = 0; n < track.size() - 1; n++) {
                    Tracks.Point p1 = track.get(n);
                    Tracks.Point p2 = track.get(n + 1);
                    if (p1.time >= p2.time) {
                        track.remove(n);
                        n--;
                        continue;
                    }
                    double d = Address.calc_distance(p1.latitude, p1.longitude, p2.latitude, p2.longitude);
                    distance += d;
                    if (p2.speed > max_speed)
                        max_speed = p2.speed;
                    if ((p1.time >= start) && (p2.time <= finish)) {
                        day_distance += d;
                        if (p2.speed > day_max_speed)
                            day_max_speed = p2.speed;
                    }
                }
                Tracks.Track t = tracks.get(i);
                t.track = track;
                t.mileage = distance / 1000.;
                t.max_speed = max_speed;
                t.begin = track.get(0).time;
                t.end = track.get(track.size() - 1).time;
                t.avg_speed = distance * 3600. / (t.end - t.begin);
                t.day_mileage = day_distance / 1000.;
                t.day_max_speed = day_max_speed;
            }

            prgMain.setProgress(++progress);
            if (tracks.size() == 0) {
                all_done();
                return;
            }

            TrackStartPositionFetcher fetcher = new TrackStartPositionFetcher();
            fetcher.update(id, 0);
        }

        @Override
        void error() {
            showError();
        }

        void update(int track_id, long begin, long end) {
            id = track_id;
            execute(GPSLIST, api_key, begin + "", end + "");
        }
    }

    abstract class TrackPositionFetcher extends AddressRequest {

        int id;
        int pos;

        abstract Tracks.Point getPoint(Tracks.Track track);

        abstract TrackPositionFetcher create();

        abstract void process(String[] address);

        abstract void done();

        @Override
        void addressResult(String[] address) {
            if (id != track_id)
                return;
            if (address == null) {
                showError();
                return;
            }

            process(address);
            prgMain.setProgress(++progress);

            if (++pos >= tracks.size()) {
                // All tracks done
                done();
                return;
            }
            TrackPositionFetcher fetcher = create();
            fetcher.update(id, pos);
        }

        void update(int track_id, int track_pos) {
            id = track_id;
            pos = track_pos;
            Tracks.Track track = tracks.get(pos);
            Tracks.Point p = getPoint(track);
            getAddress(preferences, p.latitude + "", p.longitude + "");
        }

    }

    class TrackStartPositionFetcher extends TrackPositionFetcher {

        @Override
        Tracks.Point getPoint(Tracks.Track track) {
            return track.track.get(0);
        }

        @Override
        TrackPositionFetcher create() {
            return new TrackStartPositionFetcher();
        }

        @Override
        void process(String[] parts) {
            String address = parts[0];
            for (int i = 1; i < parts.length; i++)
                address += ", " + parts[i];
            tracks.get(pos).start = address;
        }

        @Override
        void done() {
            TrackPositionFetcher fetcher = new TrackEndPositionFetcher();
            fetcher.update(id, 0);
        }
    }

    class TrackEndPositionFetcher extends TrackPositionFetcher {

        @Override
        Tracks.Point getPoint(Tracks.Track track) {
            return track.track.get(track.track.size() - 1);
        }

        @Override
        TrackPositionFetcher create() {
            return new TrackEndPositionFetcher();
        }

        @Override
        void process(String[] finish_parts) {
            Tracks.Track track = tracks.get(pos);

            String[] start_parts = track.start.split(", ");

            int s = start_parts.length - 1;
            int f = finish_parts.length - 1;

            while ((s > 2) && (f > 2)) {
                if (!start_parts[s].equals(finish_parts[f]))
                    break;
                s--;
                f--;
            }

            String address = start_parts[0];
            for (int i = 1; i < s; i++) {
                address += ", " + start_parts[i];
            }
            track.start = address;

            address = finish_parts[0];
            for (int i = 1; i < f; i++) {
                address += ", " + finish_parts[i];
            }
            track.finish = address;
        }

        @Override
        void done() {
            all_done();
        }
    }


    class TracksAdapter extends BaseAdapter {

        int selected;

        TracksAdapter() {
            selected = -1;
        }

        @Override
        public int getCount() {
            return tracks.size();
        }

        @Override
        public Object getItem(int position) {
            return tracks.get(position);
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
            TextView tvTitle = (TextView) v.findViewById(R.id.title);
            Tracks.Track track = (Tracks.Track) getItem(position);
            LocalDateTime begin = new LocalDateTime(track.begin);
            LocalDateTime end = new LocalDateTime(track.end);
            tvTitle.setText(begin.toString("HH:mm") + "-" + end.toString("HH:mm"));
            TextView tvMileage = (TextView) v.findViewById(R.id.mileage);
            String s = String.format(getString(R.string.mileage), track.mileage);
            tvMileage.setText(s);
            TextView tvAddress = (TextView) v.findViewById(R.id.address);
            tvAddress.setText(track.start + " - " + track.finish);
            TextView tvStatus = (TextView) v.findViewById(R.id.status);
            String text = "";
            if (position == selected) {
                text = String.format(getString(R.string.short_status),
                        timeFormat((int) ((track.end - track.begin) / 60000)),
                        track.avg_speed,
                        track.max_speed);
                tvTitle.setTypeface(null, Typeface.BOLD);
                tvMileage.setTypeface(null, Typeface.BOLD);
            } else {
                tvTitle.setTypeface(null, Typeface.NORMAL);
                tvMileage.setTypeface(null, Typeface.NORMAL);
            }
            tvStatus.setText(text);
            return v;
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

    static class PointInfo {
        Tracks.Point point;
        int type;
    }

    static class EventInfo {
        long id;
        long time;
        int type;
    }

}
