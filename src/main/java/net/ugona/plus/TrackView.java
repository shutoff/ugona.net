package net.ugona.plus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.SpannedString;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.joda.time.LocalDateTime;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.Vector;

public class TrackView extends MapActivity {

    Vector<Track> tracks;

    @Override
    int menuId() {
        return R.menu.track;
    }

    @Override
    MapActivity.JsInterface js() {
        return new JsInterface();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }
        try {
            byte[] track_data;
            String file_name = getIntent().getStringExtra(Names.TRACK_FILE);
            if (file_name != null) {
                File file = new File(file_name);
                FileInputStream in = new FileInputStream(file);
                track_data = new byte[(int) file.length()];
                in.read(track_data);
                in.close();
                file.delete();
            } else {
                track_data = getIntent().getByteArrayExtra(Names.TRACK);
            }
            ByteArrayInputStream bis = new ByteArrayInputStream(track_data);
            ObjectInput in = new ObjectInputStream(bis);
            tracks = (Vector<Track>) in.readObject();
            in.close();
            bis.close();
        } catch (Exception ex) {
            finish();
        }
        super.onCreate(savedInstanceState);
        setTitle(getIntent().getStringExtra(Names.TITLE));
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                webView.loadUrl("javascript:saveTrack()");
                break;
            case R.id.share:
                webView.loadUrl("javascript:shareTrack()");
                break;
        }
        return false;
    }

    File saveTrack(double min_lat, double max_lat, double min_lon, double max_lon, boolean show_toast) {
        try {
            File path = Environment.getExternalStorageDirectory();
            if (path == null)
                path = getFilesDir();
            path = new File(path, "Tracks");
            path.mkdirs();

            long begin = 0;
            long end = 0;
            for (Track track : tracks) {
                String[] points = track.track.split("\\|");
                for (String point : points) {
                    Track.Point p = new Track.Point(point);
                    if ((p.latitude < min_lat) || (p.latitude > max_lat) || (p.longitude < min_lon) || (p.longitude > max_lon))
                        continue;
                    if (begin == 0)
                        begin = p.time;
                    end = p.time;
                }
            }
            LocalDateTime d2 = new LocalDateTime(begin);
            LocalDateTime d1 = new LocalDateTime(end);

            String name = d2.toString("dd.MM.yy_HH.mm-") + d1.toString("HH.mm") + ".gpx";
            File out = new File(path, name);
            out.createNewFile();

            FileOutputStream f = new FileOutputStream(out);
            OutputStreamWriter ow = new OutputStreamWriter(f);
            BufferedWriter writer = new BufferedWriter(ow);

            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.append("<gpx\n");
            writer.append(" version=\"1.0\"\n");
            writer.append(" creator=\"ExpertGPS 1.1 - http://www.topografix.com\"\n");
            writer.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            writer.append(" xmlns=\"http://www.topografix.com/GPX/1/0\"\n");
            writer.append(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">\n");
            writer.append("<time>");
            LocalDateTime now = new LocalDateTime();
            writer.append(now.toString("yyyy-MM-dd'T'HH:mm:ss'Z"));
            writer.append("</time>\n");
            writer.append("<trk>\n");

            boolean trk = false;
            for (Track track : tracks) {
                String[] points = track.track.split("\\|");
                for (String point : points) {
                    Track.Point p = new Track.Point(point);
                    if ((p.latitude < min_lat) || (p.latitude > max_lat) || (p.longitude < min_lon) || (p.longitude > max_lon)) {
                        if (trk) {
                            trk = false;
                            writer.append("</trkseg>\n");
                        }
                        continue;
                    }
                    if (!trk) {
                        trk = true;
                        writer.append("<trkseg>\n");
                    }
                    writer.append("<trkpt lat=\"" + p.latitude + "\" lon=\"" + p.longitude + "\">\n");
                    LocalDateTime t = new LocalDateTime(p.time);
                    writer.append("<time>" + t.toString("yyyy-MM-dd'T'HH:mm:ss'Z") + "</time>\n");
                    writer.append("</trkpt>\n");
                }
                if (trk)
                    writer.append("</trkseg>");
            }
            writer.append("</trk>\n");
            writer.append("</gpx>");
            writer.close();
            if (show_toast) {
                Toast toast = Toast.makeText(this, getString(R.string.saved) + " " + out.toString(), Toast.LENGTH_LONG);
                toast.show();
            }
            return out;
        } catch (Exception ex) {
            Toast toast = Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }
        return null;
    }

    void shareTrack(double min_lat, double max_lat, double min_lon, double max_lon) {
        File out = saveTrack(min_lat, max_lat, min_lon, max_lon, false);
        if (out == null)
            return;
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(out));
        shareIntent.setType("application/gpx+xml");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
    }

    class JsInterface extends MapActivity.JsInterface {

        @JavascriptInterface
        public String init() {
            return super.init() + "\nshowTracks()";
        }

        @JavascriptInterface
        public String getTracks() {
            Vector<Track.Marker> markers = new Vector<Track.Marker>();
            StringBuilder track_data = new StringBuilder();
            try {
                for (int i = 0; i < tracks.size(); i++) {
                    Track track = tracks.get(i);
                    String[] points = track.track.split("\\|");
                    Track.Point start = new Track.Point(points[0]);
                    Track.Point finish = new Track.Point(points[points.length - 1]);
                    int n_start = markers.size();
                    double d_best = 200.;
                    for (int n = 0; n < markers.size(); n++) {
                        Track.Marker marker = markers.get(n);
                        double delta = Address.calc_distance(start.latitude, start.longitude, marker.latitude, marker.longitude);
                        if (delta < d_best) {
                            d_best = delta;
                            n_start = n;
                        }
                    }
                    if (n_start >= markers.size()) {
                        Track.Marker marker = new Track.Marker();
                        marker.latitude = start.latitude;
                        marker.longitude = start.longitude;
                        marker.address = track.start;
                        marker.times = new Vector<Track.TimeInterval>();
                        markers.add(marker);
                    }
                    Track.Marker marker = markers.get(n_start);
                    if ((marker.times.size() == 0) || (marker.times.get(marker.times.size() - 1).end > 0)) {
                        Track.TimeInterval interval = new Track.TimeInterval();
                        marker.times.add(interval);
                    }
                    marker.times.get(marker.times.size() - 1).end = track.begin;

                    if (i > 0) {
                        Track prev = tracks.get(i - 1);
                        points = prev.track.split("\\|");
                        Track.Point last = new Track.Point(points[points.length - 1]);
                        double delta = Address.calc_distance(start.latitude, start.longitude, last.latitude, last.longitude);
                        if (delta > 200)
                            track_data.append("|");
                    }
                    track_data.append(track.track);

                    int n_finish = markers.size();
                    d_best = 200;
                    for (int n = 0; n < markers.size(); n++) {
                        if (n == n_start)
                            continue;
                        marker = markers.get(n);
                        double delta = Address.calc_distance(finish.latitude, finish.longitude, marker.latitude, marker.longitude);
                        if (delta < d_best) {
                            n_finish = n;
                            d_best = delta;
                        }
                    }
                    if (n_finish >= markers.size()) {
                        marker = new Track.Marker();
                        marker.latitude = finish.latitude;
                        marker.longitude = finish.longitude;
                        marker.address = track.finish;
                        marker.times = new Vector<Track.TimeInterval>();
                        markers.add(marker);
                    }
                    marker = markers.get(n_finish);
                    Track.TimeInterval interval = new Track.TimeInterval();
                    interval.begin = track.end;
                    marker.times.add(interval);
                }
                track_data.append("|");
                for (Track.Marker marker : markers) {
                    track_data.append("|");
                    track_data.append(marker.latitude);
                    track_data.append(",");
                    track_data.append(marker.longitude);
                    track_data.append(",<b>");
                    for (Track.TimeInterval interval : marker.times) {
                        if (interval.begin > 0) {
                            DateFormat tf = android.text.format.DateFormat.getTimeFormat(TrackView.this);
                            track_data.append(tf.format(interval.begin));
                            if (interval.end > 0)
                                track_data.append("-");
                        }
                        if (interval.end > 0) {
                            DateFormat tf = android.text.format.DateFormat.getTimeFormat(TrackView.this);
                            track_data.append(tf.format(interval.end));
                        }
                        track_data.append(" ");
                    }
                    track_data.append("</b><br/>");
                    track_data.append(Html.toHtml(new SpannedString(marker.address))
                            .replaceAll(",", "&#x2C;")
                            .replaceAll("\\|", "&#x7C;"));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            String res = track_data.toString();
            return res;
        }

        @JavascriptInterface
        public void save(double min_lat, double max_lat, double min_lon, double max_lon) {
            saveTrack(min_lat, max_lat, min_lon, max_lon, true);
        }

        @JavascriptInterface
        public void share(double min_lat, double max_lat, double min_lon, double max_lon) {
            shareTrack(min_lat, max_lat, min_lon, max_lon);
        }

    }

}
