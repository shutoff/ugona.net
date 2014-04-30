package net.ugona.plus;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class MapPointActivity extends MapActivity {

    String car_id;
    String point_data;
    Map<String, String> times;
    DateFormat df;
    DateFormat tf;
    Cars.Car[] cars;
    LocationOverlay mMyLocationOverlay;

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
    protected void onStart() {
        super.onStart();
        setActionBar();
    }

    @Override
    int menuId() {
        return R.menu.map;
    }

    @Override
    void initMap(IMapController controller) {
        double lat = preferences.getFloat(Names.Car.LAT + car_id, 0);
        double lng = preferences.getFloat(Names.Car.LNG + car_id, 0);
        controller.setZoom(16);
        controller.setCenter(new GeoPoint(lat, lng));
        final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        if (cars == null)
            cars = Cars.getCars(this);

        mMyLocationOverlay = new LocationOverlay();

        mMyLocationOverlay.setFocusItemsOnTap(true);
        mMyLocationOverlay.setFocusedItem(mMyLocationOverlay.find(car_id));

        mMapView.getOverlays().add(mMyLocationOverlay);
    }

    boolean updateItem(MyOverlayItem item) {
        double lat = preferences.getFloat(Names.Car.LAT + item.getUid(), 0);
        double lng = preferences.getFloat(Names.Car.LNG + item.getUid(), 0);
        if ((lat == 0) || (lng == 0))
            return false;
        String data = Math.round(lat * 10000) / 10000. + "," + Math.round(lng * 10000) / 10000.;
        String address = Address.getAddress(getBaseContext(), lat, lng);
        if (address != null) {
            String[] parts = address.split(", ");
            if (parts.length >= 3) {
                address = parts[0] + ", " + parts[1];
                for (int n = 2; n < parts.length; n++)
                    address += "\n" + parts[n];
            }
            data += "\n" + address;
        } else {
            Address addr = new Address() {
                @Override
                void result(String address) {
                    update();
                }
            };
            addr.get(this, lat, lng);
        }
        for (Cars.Car car : cars) {
            if (!car.id.equals(item.getUid()))
                continue;
            item.set((cars.length > 1) ? car.name : "", data, new GeoPoint(lat, lng));
            return true;
        }
        return false;
    }

    void update() {
        mMyLocationOverlay.update();
    }

    void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        cars = Cars.getCars(this);
        boolean found = false;
        for (Cars.Car car : cars) {
            if (car.id.equals(car_id)) {
                found = true;
                break;
            }
        }
        if (!found)
            cars = new Cars.Car[0];
        if (cars.length > 1) {
            String save_point_data = point_data;
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setListNavigationCallbacks(new CarsAdapter(), new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int i, long l) {
                    if (cars[i].id.equals(car_id))
                        return true;
                    return true;
                }
            });
            for (int i = 0; i < cars.length; i++) {
                if (cars[i].id.equals(car_id)) {
                    actionBar.setSelectedNavigationItem(i);
                    break;
                }
            }
            point_data = save_point_data;
            setTitle("");
        } else {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayUseLogoEnabled(false);
            setTitle(getString(R.string.app_name));
        }
    }

    class MyOverlayItem extends OverlayItem {

        public MyOverlayItem(String aUid) {
            super(aUid, null, null, null);
        }

        void set(String title, String snippet, GeoPoint point) {
            mTitle = title;
            mSnippet = snippet;
            mGeoPoint = point;
        }

    }

    class LocationOverlay extends ItemizedOverlayWithFocus<MyOverlayItem> {

        public LocationOverlay() {
            super(new Vector<MyOverlayItem>(),
                    mMapView.getResourceProxy().getDrawable(ResourceProxy.bitmap.marker_default), null,
                    getResources().getColor(R.color.caldroid_white),
                    new ItemizedIconOverlay.OnItemGestureListener<MyOverlayItem>() {
                        @Override
                        public boolean onItemSingleTapUp(final int index, final MyOverlayItem item) {
                            return true;
                        }

                        @Override
                        public boolean onItemLongPress(final int index, final MyOverlayItem item) {
                            return false;
                        }
                    }, mMapView.getResourceProxy()
            );
            for (Cars.Car car : cars) {
                MyOverlayItem item = new MyOverlayItem(car.id);
                if (updateItem(item))
                    mItemList.add(item);
            }
            populate();
        }

        void update() {
            for (MyOverlayItem item : mItemList) {
                if (!updateItem(item))
                    mItemList.remove(item);
            }
            populate();
        }

        int find(String id) {
            for (int i = 0; i < mItemList.size(); i++) {
                if (getItem(i).getUid().equals(id))
                    return i;
            }
            return -1;
        }
    }

    class CarsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return cars.length;
        }

        @Override
        public Object getItem(int position) {
            return cars[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getBaseContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.car_list_item, null);
            }
            TextView tv = (TextView) v.findViewById(R.id.name);
            tv.setText(cars[position].name);
            return v;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getBaseContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.car_list_dropdown_item, null);
            }
            TextView tv = (TextView) v.findViewById(R.id.name);
            tv.setText(cars[position].name);
            return v;
        }
    }

}
