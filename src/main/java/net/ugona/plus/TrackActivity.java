package net.ugona.plus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.widget.Toast;

import org.joda.time.LocalDateTime;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.OverlayItem;

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

public class TrackActivity extends MapActivity {

    Vector<Track> tracks;
    OverlayItem mTrackItem;
    int track_limit;

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
        float density = getResources().getDisplayMetrics().density;
        track_limit = (int) (density * density * 64);
        super.onCreate(savedInstanceState);
        setTitle(getIntent().getStringExtra(Names.TITLE));
    }

    @Override
    void initMap(final MapView mapView) {
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

        final ItemsOverlay<OverlayItem> pointsOverlay = new ItemsOverlay<OverlayItem>(this) {
            @Override
            void onChangeFocus() {
                if (getFocusedItem() != mTrackItem) {
                    removeItem(mTrackItem);
                    mTrackItem = null;
                    mapView.invalidate();
                }
            }
        };
        Vector<Track.Marker> markers = new Vector<Track.Marker>();
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
            for (Track.Marker marker : markers) {
                StringBuilder title = new StringBuilder();
                for (Track.TimeInterval interval : marker.times) {
                    if (interval.begin > 0) {
                        DateFormat tf = android.text.format.DateFormat.getTimeFormat(this);
                        title.append(tf.format(interval.begin));
                        if (interval.end > 0)
                            title.append("-");
                    }
                    if (interval.end > 0) {
                        DateFormat tf = android.text.format.DateFormat.getTimeFormat(this);
                        title.append(tf.format(interval.end));
                    }
                    title.append(" ");
                }
                OverlayItem item = new OverlayItem(title.toString(), marker.address, new GeoPoint(marker.latitude, marker.longitude));
                pointsOverlay.addItem(item);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        pointsOverlay.setFocusItemsOnTap(true);

        TrackOverlay trackOverlay = new TrackOverlay(this) {
            @Override
            void showBaloon(int dist, int track_index, int point_index, int pos) {
                if (dist > track_limit) {
                    if (mTrackItem == null)
                        return;
                    if (pointsOverlay.getFocusedItem() == mTrackItem)
                        pointsOverlay.unSetFocusedItem();
                    pointsOverlay.removeItem(mTrackItem);
                    mTrackItem = null;
                    mapView.invalidate();
                    return;
                }
                if (tracks == null)
                    return;
                TrackPoint p1 = tracks.get(track_index).get(point_index);
                TrackPoint p2 = tracks.get(track_index).get(point_index + 1);
                int lat1 = p1.point.getLatitudeE6();
                int lon1 = p1.point.getLongitudeE6();
                int lat2 = p2.point.getLatitudeE6();
                int lon2 = p2.point.getLongitudeE6();
                DateFormat tf = android.text.format.DateFormat.getTimeFormat(mapView.getContext());
                String title = tf.format(p1.time + (p2.time - p1.time) * pos / 1000);
                String snippet = (p1.speed + (p2.speed - p1.speed) * pos / 1000) + " " + mapView.getContext().getString(R.string.kmh);
                if (mTrackItem != null)
                    pointsOverlay.removeItem(mTrackItem);
                mTrackItem = new OverlayItem(title, snippet, new GeoPoint(lat1 + (lat2 - lat1) * pos / 1000, lon1 + (lon2 - lon1) * pos / 1000));
                pointsOverlay.addItem(mTrackItem);
                pointsOverlay.setFocusedItem(pointsOverlay.size() - 1);
                mapView.invalidate();
            }
        };
        for (Track track : tracks) {
            trackOverlay.add(track.track);
        }

        mapView.getOverlays().add(trackOverlay);
        mapView.getOverlays().add(pointsOverlay);
        mapView.mPointsOverlay = pointsOverlay;
        mapView.mTrackOverlay = trackOverlay;

        mapView.getController().setZoom(mapView.getMaxZoomLevel());
        mapView.fitToRect(new GeoPoint(trackOverlay.min_lat, trackOverlay.min_lon), new GeoPoint(trackOverlay.max_lat, trackOverlay.max_lon), 0.75);
    }

    @Override
    void updateLocation(Rect rc) {
        TrackOverlay trackOverlay = (TrackOverlay) getMapView().mTrackOverlay;
        if (trackOverlay.min_lat < rc.left)
            rc.left = (int) (trackOverlay.min_lat * 1000000);
        if (trackOverlay.min_lon < rc.top)
            rc.top = (int) (trackOverlay.min_lon * 1000000);
        if (trackOverlay.max_lat < rc.right)
            rc.right = (int) (trackOverlay.max_lat * 1000000);
        if (trackOverlay.max_lon < rc.bottom)
            rc.bottom = (int) (trackOverlay.max_lon * 1000000);
    }

    @Override
    int menuId() {
        return R.menu.track;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.traffic:
                MapView mapView = getMapView();
                TrackOverlay trackOverlay = (TrackOverlay) mapView.mTrackOverlay;
                trackOverlay.show_speed = !trackOverlay.show_speed;
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(TRAFFIC, !preferences.getBoolean(TRAFFIC, trackOverlay.show_speed));
                ed.commit();
                updateMenu();
                mapView.invalidate();
                return true;
            case R.id.save:
                saveTrack(true);
                return true;
            case R.id.share:
                shareTrack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    File saveTrack(boolean show_toast) {
        BoundingBoxE6 box = getMapView().getBoundingBox();
        double min_lat = box.getLatSouthE6() / 1000000.;
        double max_lat = box.getLatNorthE6() / 1000000.;
        double min_lon = box.getLonEastE6() / 1000000.;
        double max_lon = box.getLonWestE6() / 1000000.;
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

    void shareTrack() {
        File out = saveTrack(false);
        if (out == null)
            return;
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(out));
        shareIntent.setType("application/gpx+xml");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
    }
}
