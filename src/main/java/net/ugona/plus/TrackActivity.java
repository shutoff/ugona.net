package net.ugona.plus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewConfiguration;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.util.Vector;

public class TrackActivity extends MapActivity {

    TrackOverlay mTrackOverlay;
    Vector<Track> tracks;

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
        super.onCreate(savedInstanceState);
        setTitle(getIntent().getStringExtra(Names.TITLE));
    }

    @Override
    void initMap(IMapController controller) {
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

        mTrackOverlay = new TrackOverlay(this);
        for (Track track : tracks) {
            mTrackOverlay.add(track.track);
        }

        mMapView.getController().setZoom(mMapView.getMaxZoomLevel());
        mMapView.fitToRect(new GeoPoint(mTrackOverlay.min_lat, mTrackOverlay.min_lon), new GeoPoint(mTrackOverlay.max_lat, mTrackOverlay.max_lon), 750);

        mMapView.getOverlays().add(mTrackOverlay);
    }

    @Override
    int menuId() {
        return R.menu.track;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.traffic:
                mTrackOverlay.show_speed = !mTrackOverlay.show_speed;
                SharedPreferences.Editor ed = preferences.edit();
                ed.putBoolean(TRAFFIC, !preferences.getBoolean(TRAFFIC, mTrackOverlay.show_speed));
                ed.commit();
                updateMenu();
                mMapView.invalidate();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
