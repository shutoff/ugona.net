package net.ugona.plus;

import android.graphics.Rect;
import android.view.MenuItem;

import org.osmdroid.util.GeoPoint;

public class MapEventActivity extends MapActivity {

    @Override
    void initMap(MapView mapView) {
        String car_id = getIntent().getStringExtra(Names.ID);
        String data = getIntent().getStringExtra(Names.POINT_DATA);
        String[] parts = data.split(";");
        LocationOverlay pointsOverlay = new LocationOverlay(this);
        MyOverlayItem item = new MyOverlayItem("");
        String p = parts[3];
        int index = p.indexOf("\n");
        int course = 0;
        try {
            course = Integer.parseInt(parts[2]);
        } catch (Exception ex) {
            // ignore
        }
        GeoPoint point = new GeoPoint(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        item.set(p.substring(0, index), p.substring(index + 1), point, course);
        if (parts.length > 4)
            item.setZone(parts[4]);
        pointsOverlay.addItem(item);
        pointsOverlay.setFocusItemsOnTap(true);
        pointsOverlay.setFocusedItem(0);
        mapView.getOverlays().add(pointsOverlay);
        mapView.mPointsOverlay = pointsOverlay;
        mapView.getController().setZoom(16);
        mapView.getController().setCenter(point);
        if (item.zone != null)
            mapView.fitToRect(new GeoPoint(item.min_lat, item.min_lon), new GeoPoint(item.max_lat, item.max_lon), 0.7);
    }

    @Override
    void updateLocation(Rect rc) {
        LocationOverlay pointsOverlay = (LocationOverlay) getMapView().mPointsOverlay;
        MyOverlayItem item = pointsOverlay.getItem(0);
        updateLocation(rc, item);
    }

    @Override
    int menuId() {
        return R.menu.map;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.map) {
            LocationOverlay pointsOverlay = (LocationOverlay) getMapView().mPointsOverlay;
            MyOverlayItem i = pointsOverlay.getItem(0);
            getMapView().getController().setCenter(i.getPoint());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
