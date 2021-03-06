package net.ugona.plus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Vector;

public class TracksFragment extends MainFragment {

    static final String DATE = "date";
    static final String SELECTED = "selected";
    static final String TRACK = "track";

    TextView tvSummary;
    View vError;
    HoursList vTracks;
    CenteredScrollView vSummary;
    BroadcastReceiver br;

    boolean loaded;
    Vector<Track> tracks;
    Vector<Track> works;
    long engine_time;
    long selected;

    TracksFetcher fetcher;
    int fetcher_id;

    @Override
    int layout() {
        return R.layout.tracks;
    }

    @Override
    boolean isShowDate() {
        return true;
    }

    @Override
    void changeDate() {
        vError.setVisibility(View.GONE);
        vTracks.setVisibility(View.GONE);
        tvSummary.setText("");
        onRefresh();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        tvSummary = (TextView) v.findViewById(R.id.summary);
        vTracks = (HoursList) v.findViewById(R.id.tracks);
        vError = v.findViewById(R.id.error);
        vSummary = (CenteredScrollView) v.findViewById(R.id.summary_view);

        vError.setClickable(true);
        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
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

        vTracks.disableDivider();
        vTracks.setListener(new HoursList.Listener() {
            @Override
            public int setHour(int h) {
                int i;
                Calendar calendar = Calendar.getInstance();
                for (i = 0; i < tracks.size(); i++) {
                    Track t = tracks.get(i);
                    calendar.setTimeInMillis(t.begin);
                    if (calendar.get(Calendar.HOUR_OF_DAY) < h)
                        break;
                }
                i--;
                if (i < 0)
                    i = 0;
                return i;
            }
        });
        vTracks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (selected == position) {
                    showTrack(position);
                    return;
                }
                selected = position;
                vTracks.notifyChanges();
            }
        });

        selected = -1;
        if (savedInstanceState != null) {
            long current = savedInstanceState.getLong(DATE);
            if (current == date()) {
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
        }

        if (loaded) {
            vTracks.notifyChanges();
            tracks_done();
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
            }
        };
        IntentFilter intFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(br, intFilter);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(br);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(SELECTED, selected);
        outState.putLong(DATE, date());
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

    void showTrack(int index) {
        Track track = tracks.get(index);
        Vector<Track> tracks = new Vector<Track>();
        tracks.add(track);
        DateFormat df = android.text.format.DateFormat.getMediumDateFormat(getActivity());
        DateFormat tf = android.text.format.DateFormat.getTimeFormat(getActivity());
        String title = df.format(track.begin) + " " + tf.format(track.begin) + "-" + tf.format(track.end);
        showTracks(tracks, title);
    }

    void showDay() {
        if (tracks.size() == 0)
            return;
        DateFormat df = android.text.format.DateFormat.getMediumDateFormat(getActivity());
        String title = df.format(date());
        showTracks(tracks, title);
    }

    void showTracks(Vector<Track> tracks, String title) {
        Intent intent = new Intent(getActivity(), TrackView.class);
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
            return;
        if (data.length > 500000) {
            try {
                File outputDir = getActivity().getCacheDir();
                File file = File.createTempFile("track", "dat", outputDir);
                FileOutputStream f = new FileOutputStream(file);
                f.write(data);
                intent.putExtra(Names.TRACK_FILE, file.getAbsolutePath());
                f.close();
            } catch (Exception ex) {
                return;
            }
        } else {
            intent.putExtra(Names.TRACK, data);
        }
        intent.putExtra(Names.TITLE, title);
        getActivity().startActivity(intent);
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        vError.setVisibility(View.GONE);
        if (fetcher != null)
            fetcher.cancel();
        selected = -1;
        fetcher = new TracksFetcher();
        fetcher.update();
    }

    @Override
    void refreshDone() {
        super.refreshDone();
        fetcher = null;
    }

    void setStatus(Vector<Track> tracks) {
        if (tracks.size() == 0) {
            tvSummary.setText("");
            return;
        }
        double mileage = 0;
        int max_speed = 0;
        long time = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long start = calendar.getTimeInMillis();
        calendar.add(Calendar.DATE, 1);
        long finish = calendar.getTimeInMillis();
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
        NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(2);
        String s = formatter.format(mileage);
        status = String.format(status, s, timeFormat((int) (time / 60000)), (float) avg_speed, max_speed);
        if (engine_time >= 60) {
            status += "\n";
            status += getString(R.string.engine_time);
            status += ": |";
            status += timeFormat((int) (engine_time / 60));
        }
        tvSummary.setText(State.createSpans(status, vSummary.selectedColor, true));
    }

    void tracks_done() {
        if (getActivity() == null)
            return;
        setStatus(tracks);
        vError.setVisibility(View.GONE);
        vTracks.setVisibility(View.VISIBLE);
        if (vTracks.getAdapter() != null) {
            BaseAdapter adapter = (BaseAdapter) vTracks.getAdapter();
            adapter.notifyDataSetChanged();
        } else {
            vTracks.setAdapter(new TracksAdapter());
        }
        refreshDone();
        loaded = true;
    }

    static class TrackParams implements Serializable {
        String skey;
        long begin;
        long end;
    }

    class TracksFetcher extends HttpTask {

        @Override
        void result(JsonObject res) throws ParseException {
            if (getActivity() == null)
                return;
            works = new Vector<Track>();
            JsonArray list = res.get("tracks").asArray();
            for (int i = list.size() - 1; i >= 0; i--) {
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
                works.add(track);
            }
            engine_time = 0;
            JsonValue engine = res.get("engine_time");
            if (engine != null)
                engine_time = engine.asLong();

            if (works.size() == 0) {
                tracks = works;
                tracks_done();
                return;
            }
            setStatus(works);
            TrackStartPositionFetcher fetcher = new TrackStartPositionFetcher(++fetcher_id);
            fetcher.update(0);
        }

        @Override
        void error() {
            if (getActivity() == null)
                return;
            Activity activity = getActivity();
            if (activity == null)
                return;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    vError.setVisibility(View.VISIBLE);
                    vTracks.setVisibility(View.GONE);
                }
            });
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
            TrackParams params = new TrackParams();
            params.begin = start;
            params.end = finish;
            CarConfig config = CarConfig.get(getActivity(), id());
            params.skey = config.getKey();
            execute("/tracks", params);
        }
    }

    abstract class TrackPositionFetcher implements Address.Answer {

        Handler handler;

        int pos;
        int id;

        TrackPositionFetcher(int id) {
            handler = new Handler();
            this.id = id;
        }

        abstract Track.Point getPoint(Track track);

        abstract TrackPositionFetcher create(int id);

        abstract void process(String address);

        abstract void done();

        @Override
        public void result(final String address) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    process_address(address);
                }
            });
        }

        public void process_address(String address) {
            if (getActivity() == null)
                return;
            if (id != fetcher_id)
                return;
            if (address == null) {
                Track track = works.get(pos);
                Track.Point p = getPoint(track);
                address = p.latitude + "," + p.longitude;
            }
            process(address);
            if (++pos >= works.size()) {
                done();
                return;
            }
            TrackPositionFetcher fetcher = create(id);
            fetcher.update(pos);
        }


        void update(int track_pos) {
            pos = track_pos;
            Track track = works.get(pos);
            Track.Point p = getPoint(track);
            if (getActivity() == null)
                return;
            Address.get(getActivity(), p.latitude, p.longitude, this);
        }

    }

    class TrackStartPositionFetcher extends TrackPositionFetcher {

        TrackStartPositionFetcher(int id) {
            super(id);
        }

        @Override
        Track.Point getPoint(Track track) {
            String[] points = track.track.split("\\|");
            return new Track.Point(points[0]);
        }

        @Override
        TrackPositionFetcher create(int id) {
            return new TrackStartPositionFetcher(id);
        }

        @Override
        void process(String address) {
            works.get(pos).start = address;
        }

        @Override
        void done() {
            TrackPositionFetcher fetcher = new TrackEndPositionFetcher(id);
            fetcher.update(0);
        }
    }

    class TrackEndPositionFetcher extends TrackPositionFetcher {

        TrackEndPositionFetcher(int id) {
            super(id);
        }

        @Override
        Track.Point getPoint(Track track) {
            String[] points = track.track.split("\\|");
            return new Track.Point(points[points.length - 1]);
        }

        @Override
        TrackPositionFetcher create(int id) {
            return new TrackEndPositionFetcher(id);
        }

        @Override
        void process(String finish_address) {
            if (finish_address == null)
                return;

            Track track = works.get(pos);
            if (track.start == null)
                return;

            String[] start_parts = track.start.split(", ");
            String[] finish_parts = finish_address.split(", ");

            int s = start_parts.length - 1;
            int f = finish_parts.length - 1;

            while ((s > 0) && (f > 0)) {
                if (!start_parts[s].equals(finish_parts[f]))
                    break;
                s--;
                f--;
            }

            String address = start_parts[0];
            for (int i = 1; i <= s; i++) {
                address += ", " + start_parts[i];
            }
            track.start = address;

            address = finish_parts[0];
            for (int i = 1; i <= f; i++) {
                address += ", " + finish_parts[i];
            }
            track.finish = address;

            while ((track.start.length() < 15) || (track.finish.length() < 15)) {
                f++;
                if (f >= finish_parts.length)
                    break;
                String p = finish_parts[f];
                track.start += ", " + p;
                track.finish += ", " + p;
            }
        }

        @Override
        void done() {
            tracks = works;
            tracks_done();
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
                LayoutInflater inflater = LayoutInflater.from(getActivity());
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
            if (position == selected) {
                String text = String.format(getString(R.string.short_status),
                        timeFormat((int) ((track.end - track.begin) / 60000)),
                        (float) track.avg_speed,
                        track.max_speed);
                tvStatus.setText(State.createSpans(text, getResources().getColor(R.color.highlighted), true));
                tvStatus.setVisibility(View.VISIBLE);
            } else {
                tvStatus.setText("");
                tvStatus.setVisibility(View.GONE);
            }
            return v;
        }

    }

}
