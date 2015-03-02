package net.ugona.plus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Vector;

public class TracksFragment extends MainFragment {

    static final String DATE = "date";
    static final String SELECTED = "selected";
    static final String TRACK = "track";

    TextView tvSummary;
    View vError;
    View vSpace;
    ProgressBar prgFirst;
    ProgressBar prgMain;
    TextView tvLoading;
    HoursList vTracks;

    boolean loaded;
    Vector<Track> tracks;
    long engine_time;
    long selected;
    int progress;

    TracksFetcher fetcher;

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
        refresh();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        tvSummary = (TextView) v.findViewById(R.id.summary);
        vTracks = (HoursList) v.findViewById(R.id.tracks);
        prgFirst = (ProgressBar) v.findViewById(R.id.first_progress);
        prgMain = (ProgressBar) v.findViewById(R.id.progress);
        tvLoading = (TextView) v.findViewById(R.id.loading);
        vError = v.findViewById(R.id.error);
        vSpace = v.findViewById(R.id.space);

        vError.setClickable(true);
        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
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

        vTracks.setListener(new HoursList.Listener() {
            @Override
            public int setHour(int h) {
                int i;
                for (i = 0; i < tracks.size(); i++) {
                    Track t = tracks.get(i);
                    LocalTime time = new LocalTime(t.begin);
                    int th = time.getHourOfDay();
                    if (th < h)
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
            if (current == date().toDate().getTime()) {
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
            all_done();
        } else {
            refresh();
        }

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(SELECTED, selected);
        outState.putLong(DATE, date().toDate().getTime());
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
        Intent intent = new Intent(getActivity(), TrackView.class);
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
        Intent intent = new Intent(getActivity(), TrackView.class);
        if (!setTrack(tracks, intent))
            return;
        DateFormat df = android.text.format.DateFormat.getMediumDateFormat(getActivity());
        intent.putExtra(Names.TITLE, df.format(date().toDate()));
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

    void refresh() {
        if (fetcher != null)
            fetcher.cancel();
        tvSummary.setText("");
        vError.setVisibility(View.GONE);
        vTracks.setVisibility(View.GONE);
        vSpace.setVisibility(View.VISIBLE);
        tvLoading.setVisibility(View.VISIBLE);
        prgFirst.setVisibility(View.VISIBLE);
        prgMain.setVisibility(View.VISIBLE);
        prgMain.setProgress(0);
        selected = -1;
        fetcher = new TracksFetcher();
        fetcher.update();
    }

    @Override
    void refreshDone() {
        super.refreshDone();
        fetcher = null;
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
        long start = date().toDateTime(new LocalTime(0, 0)).toDate().getTime();
        LocalDate next = date().plusDays(1);
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
        if (engine_time >= 60) {
            status += "\n";
            status += getString(R.string.engine_time);
            status += ": ";
            status += timeFormat((int) (engine_time / 60));
        }
        tvSummary.setText(status);
        refreshDone();
    }

    void all_done() {
        if (getActivity() == null)
            return;
        prgFirst.setVisibility(View.GONE);
        tvLoading.setVisibility(View.GONE);
        prgMain.setVisibility(View.GONE);
        vSpace.setVisibility(View.GONE);
        vTracks.setVisibility(View.VISIBLE);
        vTracks.setAdapter(new TracksAdapter());
        loaded = true;
    }

    static class TrackParams {
        String skey;
        long begin;
        long end;
    }

    class TracksFetcher extends HttpTask {

        @Override
        void result(JsonObject res) throws ParseException {
            if (getActivity() == null)
                return;
            tracks = new Vector<Track>();
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
                tracks.add(track);
            }
            engine_time = 0;
            JsonValue engine = res.get("engine_time");
            if (engine != null)
                engine_time = engine.asLong();

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
            Activity activity = getActivity();
            if (activity == null)
                return;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    vError.setVisibility(View.VISIBLE);
                    vTracks.setVisibility(View.GONE);
                    vSpace.setVisibility(View.VISIBLE);
                    tvLoading.setVisibility(View.GONE);
                    prgFirst.setVisibility(View.GONE);
                    prgMain.setVisibility(View.GONE);
                }
            });
            refreshDone();
        }

        void update() {
            LocalDate current = date();
            DateTime start = current.toDateTime(new LocalTime(0, 0));
            LocalDate next = current.plusDays(1);
            DateTime finish = next.toDateTime(new LocalTime(0, 0));
            TrackParams params = new TrackParams();
            params.begin = start.toDate().getTime();
            params.end = finish.toDate().getTime();
            CarConfig config = CarConfig.get(getActivity(), id());
            params.skey = config.getKey();
            execute("/tracks", params);
        }
    }

    abstract class TrackPositionFetcher implements Address.Answer {

        Handler handler;

        int pos;

        TrackPositionFetcher() {
            handler = new Handler();
        }

        abstract Track.Point getPoint(Track track);

        abstract TrackPositionFetcher create();

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
            if (address == null) {
                Track track = tracks.get(pos);
                Track.Point p = getPoint(track);
                address = p.latitude + "," + p.longitude;
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
            Address.get(getActivity(), p.latitude, p.longitude, this, true);
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
                return;

            Track track = tracks.get(pos);

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
