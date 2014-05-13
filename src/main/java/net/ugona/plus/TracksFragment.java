package net.ugona.plus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Vector;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class TracksFragment extends Fragment
        implements MainActivity.DateChangeListener, OnRefreshListener {

    final static String URL_TRACKS = "https://car-online.ugona.net/tracks?skey=$1&begin=$2&end=$3";
    final String DATE = "tracks_date";
    final String TRACK = "track";
    final String SELECTED = "selected";
    SharedPreferences preferences;
    String api_key;
    LocalDate current;
    String car_id;
    Vector<Track> tracks;
    boolean loaded;
    int progress;
    long selected;
    TextView tvSummary;
    View vError;
    View vSpace;
    ProgressBar prgFirst;
    ProgressBar prgMain;
    TextView tvLoading;
    ListView lvTracks;
    BroadcastReceiver br;
    PullToRefreshLayout mPullToRefreshLayout;
    TracksFetcher fetcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tracks, container, false);
        if (current == null)
            current = new LocalDate();
        selected = -1;
        if (savedInstanceState != null) {
            car_id = savedInstanceState.getString(Names.ID);
            current = new LocalDate(savedInstanceState.getLong(DATE));
            selected = savedInstanceState.getLong(SELECTED);
            byte[] track_data = savedInstanceState.getByteArray(TRACK);
            if (track_data != null) {
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(track_data);
                    ObjectInput in = new ObjectInputStream(bis);
                    tracks = (Vector<Track>) in.readObject();
                    in.close();
                    bis.close();
                    loaded = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        api_key = preferences.getString(Names.Car.CAR_KEY + car_id, "");

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
                if (selected == position) {
                    showTrack(position);
                    return;
                }
                selected = position;
                adapter.notifyDataSetChanged();
            }
        });

        if (loaded) {
            tracks_done();
            all_done();
        } else {
            fetcher = new TracksFetcher();
            fetcher.update();
        }
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                if (!car_id.equals(intent.getStringExtra(Names.ID)))
                    return;
                if (intent.getAction().equals(FetchService.ACTION_UPDATE))
                    api_key = preferences.getString(Names.Car.CAR_KEY + car_id, "");
            }
        };
        IntentFilter intFilter = new IntentFilter(FetchService.ACTION_UPDATE_FORCE);
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        MainActivity mainActivity = (MainActivity) activity;
        mainActivity.registerDateListener(this);
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(br);
        super.onDestroyView();
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
        outState.putLong(SELECTED, selected);
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
        fetcher = new TracksFetcher();
        fetcher.update();
        selected = -1;
        mPullToRefreshLayout.setPullEnabled(current.equals(new LocalDate()));
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
        Intent intent = new Intent(getActivity(), TrackActivity.class);
        Track track = tracks.get(index);
        Vector<Track> track1 = new Vector<Track>();
        track1.add(track);
        if (!setTrack(track1, intent))
            return;
        DateFormat df = android.text.format.DateFormat.getMediumDateFormat(getActivity());
        DateFormat tf = android.text.format.DateFormat.getTimeFormat(getActivity());
        intent.putExtra(Names.TITLE, df.format(track.begin) + " " + tf.format(track.begin) + "-" + tf.format(track.end));
        startActivity(intent);
    }

    void showDay() {
        if (tracks.size() == 0)
            return;
        Intent intent = new Intent(getActivity(), TrackActivity.class);
        if (!setTrack(tracks, intent))
            return;
        DateFormat df = android.text.format.DateFormat.getMediumDateFormat(getActivity());
        intent.putExtra(Names.TITLE, df.format(current.toDate()));
        startActivity(intent);
    }

    boolean setTrack(Vector<Track> tracks, Intent intent) {
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

    void tracks_done() {
        if (getActivity() == null)
            return;
        tvSummary.setVisibility(View.VISIBLE);
        if (tracks.size() == 0) {
            tvSummary.setText(getString(R.string.no_data));
            prgMain.setVisibility(View.GONE);
            tvLoading.setVisibility(View.GONE);
            return;
        }
        double mileage = 0;
        int max_speed = 0;
        long time = 0;
        long start = current.toDateTime(new LocalTime(0, 0)).toDate().getTime();
        LocalDate next = current.plusDays(1);
        long finish = next.toDateTime(new LocalTime(0, 0)).toDate().getTime();
        for (Track track : tracks) {
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
        NumberFormat formatter = NumberFormat.getInstance(getResources().getConfiguration().locale);
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(2);
        String s = formatter.format(mileage);
        status = String.format(status, s, timeFormat((int) (time / 60000)), (float) avg_speed, max_speed);
        tvSummary.setText(status);
    }

    void all_done() {
        if (getActivity() == null)
            return;
        prgFirst.setVisibility(View.GONE);
        tvLoading.setVisibility(View.GONE);
        prgMain.setVisibility(View.GONE);
        vSpace.setVisibility(View.GONE);
        lvTracks.setVisibility(View.VISIBLE);
        lvTracks.setAdapter(new TracksAdapter());
        loaded = true;
    }

    @Override
    public void onRefreshStarted(View view) {
        if (fetcher != null)
            return;
        fetcher = new TracksFetcher();
        fetcher.no_reload = true;
        fetcher.update();
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

    class TracksFetcher extends HttpTask {
        LocalDate date;
        boolean no_reload;
        long start_time;
        long end_time;

        @Override
        void result(JsonObject res) throws ParseException {
            if (getActivity() == null)
                return;
            done();
            if (date != current)
                return;
            tracks = new Vector<Track>();
            JsonArray list = res.get("tracks").asArray();
            for (int i = 0; i < list.size(); i++) {
                JsonObject v = list.get(i).asObject();
                Track track = new Track();
                track.track = v.get("track").asString();
                track.mileage = v.get("mileage").asFloat();
                track.max_speed = v.get("max_speed").asInt();
                track.begin = v.get("begin").asLong();
                track.end = v.get("end").asLong();
                track.avg_speed = v.get("avg_speed").asFloat();
                track.day_mileage = v.get("day_mileage").asFloat();
                track.day_max_speed = v.get("day_max_speed").asInt();
                tracks.add(track);
            }

            prgMain.setProgress(++progress);
            if (tracks.size() == 0) {
                tracks_done();
                all_done();
                return;
            }
            prgMain.setMax(tracks.size() * 2 + progress);

            tracks_done();

            TrackStartPositionFetcher fetcher = new TrackStartPositionFetcher();
            fetcher.update(0);
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
            fetcher = null;
            mPullToRefreshLayout.setRefreshComplete();
        }

        void update() {
            date = current;
            DateTime start = current.toDateTime(new LocalTime(0, 0));
            LocalDate next = current.plusDays(1);
            DateTime finish = next.toDateTime(new LocalTime(0, 0));
            start_time = start.toDate().getTime();
            end_time = finish.toDate().getTime();
            execute(URL_TRACKS, api_key, start_time, end_time);
        }
    }

    abstract class TrackPositionFetcher extends Address {

        int pos;

        abstract Track.Point getPoint(Track track);

        abstract TrackPositionFetcher create();

        abstract void process(String address);

        abstract void done();

        @Override
        void result(String address) {
            if (getActivity() == null)
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
            fetcher.update(pos);
        }

        void update(int track_pos) {
            pos = track_pos;
            Track track = tracks.get(pos);
            Track.Point p = getPoint(track);
            if (getActivity() == null)
                return;
            get(getActivity(), p.latitude, p.longitude);
        }

    }

    class TrackStartPositionFetcher extends TrackPositionFetcher {

        @Override
        Track.Point getPoint(Track track) {
            String[] points = track.track.split("\\|");
            return new Track.Point(points[0]);
        }

        @Override
        TrackPositionFetcher create() {
            return new TrackStartPositionFetcher();
        }

        @Override
        void process(String address) {
            tracks.get(pos).start = address;
        }

        @Override
        void done() {
            TrackPositionFetcher fetcher = new TrackEndPositionFetcher();
            fetcher.update(0);
        }
    }

    class TrackEndPositionFetcher extends TrackPositionFetcher {

        @Override
        Track.Point getPoint(Track track) {
            String[] points = track.track.split("\\|");
            return new Track.Point(points[points.length - 1]);
        }

        @Override
        TrackPositionFetcher create() {
            return new TrackEndPositionFetcher();
        }

        @Override
        void process(String finish_address) {
            if (finish_address == null)
                showError();

            Track track = tracks.get(pos);

            String[] start_parts = track.start.split(", ");
            String[] finish_parts = finish_address.split(", ");

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

        TracksAdapter() {
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
            Track track = (Track) getItem(position);
            DateFormat tf = android.text.format.DateFormat.getTimeFormat(getActivity());
            tvTitle.setText(tf.format(track.begin) + "-" + tf.format(track.end));
            TextView tvMileage = (TextView) v.findViewById(R.id.mileage);
            String s = String.format(getString(R.string.mileage), (float) track.mileage);
            tvMileage.setText(s);
            TextView tvAddress = (TextView) v.findViewById(R.id.address);
            tvAddress.setText(track.start + " - " + track.finish);
            TextView tvStatus = (TextView) v.findViewById(R.id.status);
            String text = "";
            if (position == selected) {
                text = String.format(getString(R.string.short_status),
                        timeFormat((int) ((track.end - track.begin) / 60000)),
                        (float) track.avg_speed,
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

}
