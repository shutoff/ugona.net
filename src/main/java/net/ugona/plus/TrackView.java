package net.ugona.plus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannedString;
import android.view.Menu;
import android.view.MenuInflater;
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

public class TrackView extends WebViewActivity {

    SharedPreferences preferences;
    Vector<Tracks.Track> tracks;
    Menu topSubMenu;

    static String TRAFFIC = "traffic";

    class JsInterface {

        @JavascriptInterface
        public String getTrack() {
            Vector<Marker> markers = new Vector<Marker>();
            StringBuilder track_data = new StringBuilder();
            try {
                for (int i = 0; i < tracks.size(); i++) {
                    Tracks.Track track = tracks.get(i);
                    Tracks.Point start = track.track.get(0);
                    Tracks.Point finish = track.track.get(track.track.size() - 1);
                    int n_start = markers.size();
                    double d_best = 200.;
                    for (int n = 0; n < markers.size(); n++) {
                        Marker marker = markers.get(n);
                        double delta = Address.calc_distance(start.latitude, start.longitude, marker.latitude, marker.longitude);
                        if (delta < d_best) {
                            d_best = delta;
                            n_start = n;
                        }
                    }
                    if (n_start >= markers.size()) {
                        Marker marker = new Marker();
                        marker.latitude = start.latitude;
                        marker.longitude = start.longitude;
                        marker.address = track.start;
                        marker.times = new Vector<TimeInterval>();
                        markers.add(marker);
                    }
                    Marker marker = markers.get(n_start);
                    if ((marker.times.size() == 0) || (marker.times.get(marker.times.size() - 1).end > 0)) {
                        TimeInterval interval = new TimeInterval();
                        marker.times.add(interval);
                    }
                    marker.times.get(marker.times.size() - 1).end = track.begin;

                    if (i > 0) {
                        Tracks.Track prev = tracks.get(i - 1);
                        Tracks.Point last = prev.track.get(prev.track.size() - 1);
                        double delta = Address.calc_distance(start.latitude, start.longitude, last.latitude, last.longitude);
                        if (delta > 200)
                            track_data.append("|");
                    }
                    for (Tracks.Point p : track.track) {
                        track_data.append(p.latitude);
                        track_data.append(",");
                        track_data.append(p.longitude);
                        track_data.append(",");
                        track_data.append(p.speed);
                        track_data.append(",");
                        track_data.append(p.time);
                        track_data.append("|");
                    }

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
                        marker = new Marker();
                        marker.latitude = finish.latitude;
                        marker.longitude = finish.longitude;
                        marker.address = track.finish;
                        marker.times = new Vector<TimeInterval>();
                        markers.add(marker);
                    }
                    marker = markers.get(n_finish);
                    TimeInterval interval = new TimeInterval();
                    interval.begin = track.end;
                    marker.times.add(interval);
                }
                track_data.append("|");
                for (Marker marker : markers) {
                    track_data.append("|");
                    track_data.append(marker.latitude);
                    track_data.append(",");
                    track_data.append(marker.longitude);
                    track_data.append(",<b>");
                    for (TimeInterval interval : marker.times) {
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

        @JavascriptInterface
        public String kmh() {
            return getString(R.string.kmh);
        }

        @JavascriptInterface
        public String traffic() {
            return preferences.getBoolean(TRAFFIC, true) ? "1" : "";
        }
    }

    @Override
    String loadURL() {
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
            tracks = (Vector<Tracks.Track>) in.readObject();
            in.close();
            bis.close();
        } catch (Exception ex) {
            finish();
        }
        webView.addJavascriptInterface(new JsInterface(), "android");
        return getURL();
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
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(savedInstanceState);
        setTitle(getIntent().getStringExtra(Names.TITLE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        topSubMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.track, menu);
        menu.findItem(R.id.traffic).setTitle(getCheckedText(R.string.traffic, preferences.getBoolean(TRAFFIC, true)));
        boolean isOSM = preferences.getString(Names.MAP_TYPE, "").equals("OSM");
        menu.findItem(R.id.google).setTitle(getCheckedText(R.string.google, !isOSM));
        menu.findItem(R.id.osm).setTitle(getCheckedText(R.string.osm, isOSM));
        return super.onCreateOptionsMenu(menu);
    }

    String getCheckedText(int id, boolean check) {
        String check_mark = check ? "\u2714" : "";
        return check_mark + getString(id);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                webView.loadUrl("javascript:saveTrack()");
                break;
            case R.id.share:
                webView.loadUrl("javascript:shareTrack()");
                break;
            case R.id.traffic: {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(TRAFFIC, !preferences.getBoolean(TRAFFIC, true));
                ed.commit();
                updateMenu();
                webView.loadUrl(getURL());
                break;
            }
            case R.id.google: {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.MAP_TYPE, "Google");
                ed.commit();
                updateMenu();
                webView.loadUrl(getURL());
                break;
            }
            case R.id.osm: {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.MAP_TYPE, "OSM");
                ed.commit();
                updateMenu();
                webView.loadUrl(getURL());
                break;
            }
        }
        return false;
    }

    void updateMenu() {
        topSubMenu.clear();
        onCreateOptionsMenu(topSubMenu);
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
            for (Tracks.Track track : tracks) {
                for (Tracks.Point p : track.track) {
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
            for (Tracks.Track track : tracks) {
                for (Tracks.Point p : track.track) {
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

    String getURL() {
        if (preferences.getString(Names.MAP_TYPE, "").equals("OSM"))
            return "file:///android_asset/html/otrack.html";
        return "file:///android_asset/html/track.html";
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

    static class TimeInterval {
        long begin;
        long end;
    }

    static class Marker {
        double latitude;
        double longitude;
        String address;
        Vector<TimeInterval> times;
    }

}
