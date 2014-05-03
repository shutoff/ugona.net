package net.ugona.plus;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;

public class MapEventActivity extends MapActivity {

    LocationOverlay myLocationOverlay;

    @Override
    void initMap(IMapController controller) {
        String car_id = getIntent().getStringExtra(Names.ID);
        String data = getIntent().getStringExtra(Names.POINT_DATA);
        String[] parts = data.split(";");
        myLocationOverlay = new LocationOverlay(this);
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
        myLocationOverlay.addItem(item);
        myLocationOverlay.setFocusedItem(0);
        mMapView.getOverlays().add(myLocationOverlay);
        mMapView.getController().setZoom(16);
        mMapView.getController().setCenter(point);
        if (item.zone != null)
            mMapView.fitToRect(new GeoPoint(item.min_lat, item.min_lon), new GeoPoint(item.max_lat, item.max_lon), 700);
    }

    @Override
    int menuId() {
        return R.menu.map;
    }
}
