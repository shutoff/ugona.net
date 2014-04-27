package net.ugona.plus;

import android.os.Bundle;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;

public class MapView extends MapActivity {

    String car_id;
    String point_data;
    Map<String, String> times;
    DateFormat df;
    DateFormat tf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        car_id = getIntent().getStringExtra(Names.ID);
        point_data = getIntent().getStringExtra(Names.POINT_DATA);
        times = new HashMap<String, String>();
        if (savedInstanceState != null) {
            String car_data = savedInstanceState.getString(Names.CARS);
            if (car_data != null) {
                String[] data = car_data.split("\\|");
                for (String d : data) {
                    String[] p = d.split(";");
                    times.put(p[0], p[1]);
                }
            }
        }

        df = android.text.format.DateFormat.getDateFormat(this);
        tf = android.text.format.DateFormat.getTimeFormat(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    void initMap(IMapController controller) {
        double lat = preferences.getFloat(Names.Car.LAT + car_id, 0);
        double lng = preferences.getFloat(Names.Car.LNG + car_id, 0);
        controller.setZoom(16);
        controller.setCenter(new GeoPoint(lat, lng));
    }

}
