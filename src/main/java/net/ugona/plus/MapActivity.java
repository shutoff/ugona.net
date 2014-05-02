package net.ugona.plus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;
import org.osmdroid.tileprovider.modules.TileWriter;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;

public abstract class MapActivity extends ActionBarActivity {

    SharedPreferences preferences;

    FrameLayout holder;
    MapView mMapView;

    Menu topSubMenu;

    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.webview);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mMapView = (MapView) getLastCustomNonConfigurationInstance();
        initUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        topSubMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId(), menu);
        boolean isOSM = preferences.getString("map_type", "").equals("OSM");
        menu.findItem(R.id.google).setTitle(getCheckedText(R.string.google, !isOSM));
        menu.findItem(R.id.osm).setTitle(getCheckedText(R.string.osm, isOSM));
        return super.onCreateOptionsMenu(menu);
    }

    void updateMenu() {
        topSubMenu.clear();
        onCreateOptionsMenu(topSubMenu);
    }

    String getCheckedText(int id, boolean check) {
        String check_mark = check ? "\u2714" : "";
        return check_mark + getString(id);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.my: {
                boolean gps_enabled = false;
                IGeoPoint myLocation = mMapView.getMyLocation();
                try {
                    gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                } catch (Exception ex) {
                    // ignore
                }
                if (!gps_enabled) {
                    AlertDialog.Builder ad = new AlertDialog.Builder(this);
                    ad.setTitle(R.string.no_gps_title);
                    ad.setMessage((myLocation == null) ? R.string.no_gps_message : R.string.net_gps_message);
                    ad.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    });
                    ad.setNegativeButton(R.string.cancel, null);
                    ad.show();
                    if (myLocation == null)
                        return true;
                } else if (myLocation == null) {
                    Toast toast = Toast.makeText(this, R.string.no_location, Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                }
                mMapView.getController().setCenter(myLocation);
                break;
            }
            case R.id.google: {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.MAP_TYPE, "Google");
                ed.commit();
                updateMenu();
                mMapView.setTileSource(MapView.createTileSource(preferences));
                break;
            }
            case R.id.osm: {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.MAP_TYPE, "OSM");
                ed.commit();
                updateMenu();
                mMapView.setTileSource(MapView.createTileSource(preferences));
                break;
            }
        }
        return false;
    }

    void initUI() {
        holder = (FrameLayout) findViewById(R.id.webview);
        holder.setBackgroundColor(getResources().getColor(R.color.caldroid_gray));
        if (mMapView == null) {
            final IRegisterReceiver registerReceiver = new SimpleRegisterReceiver(getApplicationContext());
            ITileSource tileSource = MapView.createTileSource(preferences);

            final TileWriter tileWriter = new TileWriter();
            final MapTileFilesystemProvider fileSystemProvider = new MapTileFilesystemProvider(registerReceiver, tileSource);

            final NetworkAvailabliltyCheck networkAvailabliltyCheck = new NetworkAvailabliltyCheck(getApplicationContext());
            final MapTileDownloader downloaderProvider = new MapTileDownloader(tileSource, tileWriter, networkAvailabliltyCheck);
            final MapTileProviderArray tileProvider = new MapTileProviderArray(tileSource, registerReceiver, new MapTileModuleProviderBase[]{fileSystemProvider, downloaderProvider});

            mMapView = new MapView(this, tileProvider);
            mMapView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mMapView.setAfterLayout(new Runnable() {
                @Override
                public void run() {
                    initMap(mMapView.getController());
                }
            });

        }
        holder.addView(mMapView);
    }

    @Override
    protected void onDestroy() {
        if (mMapView != null)
            mMapView.onDetach();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        Object res = mMapView;
        if (mMapView != null) {
            holder.removeView(mMapView);
            mMapView = null;
        }
        return res;
    }

    abstract void initMap(IMapController controller);

    abstract int menuId();

    static class TrackPoint {
        GeoPoint point;
        int speed;
        long time;
    }

    class TrackOverlay extends Overlay {

        private final Path mPath = new Path();
        private final Point mTempPoint1 = new Point();
        private final Point mTempPoint2 = new Point();
        protected Paint mPaint = new Paint();
        double min_lat;
        double max_lat;
        double min_lon;
        double max_lon;
        private ArrayList<TrackPoint> mPoints;

        public TrackOverlay(Context ctx) {
            super(ctx);
        }

        @Override
        public boolean isHardwareAccelerated() {
            return false;
        }

        @Override
        protected void draw(Canvas canvas, org.osmdroid.views.MapView mapView, boolean shadow) {
            if (shadow)
                return;

            if (mPoints == null)
                return;

            final int size = mPoints.size();
            if (size < 2) {
                // nothing to paint
                return;
            }

            final org.osmdroid.views.MapView.Projection pj = mapView.getProjection();

            Point screenPoint0 = null; // points on screen
            Point screenPoint1;
            GeoPoint projectedPoint0; // points from the points list
            GeoPoint projectedPoint1;

            mPath.rewind();
            projectedPoint0 = mPoints.get(size - 1).point;

            for (int i = size - 2; i >= 0; i--) {
                // compute next points
                projectedPoint1 = mPoints.get(i).point;

                // the starting point may be not calculated, because previous segment was out of clip
                // bounds
                if (screenPoint0 == null) {
                    screenPoint0 = pj.toPixels(projectedPoint0, mTempPoint1);
                    mPath.moveTo(screenPoint0.x, screenPoint0.y);
                }

                screenPoint1 = pj.toPixels(projectedPoint1, mTempPoint2);

                // skip this point, too close to previous point
                if (Math.abs(screenPoint1.x - screenPoint0.x) + Math.abs(screenPoint1.y - screenPoint0.y) <= 1) {
                    continue;
                }

                mPath.lineTo(screenPoint1.x, screenPoint1.y);

                // update starting point to next position
                projectedPoint0 = projectedPoint1;
                screenPoint0.x = screenPoint1.x;
                screenPoint0.y = screenPoint1.y;
            }

            mPaint.setColor(Color.rgb(0, 0, 128));
            mPaint.setStrokeWidth(4);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setPathEffect(new CornerPathEffect(10));
            mPaint.setAntiAlias(true);
            canvas.drawPath(mPath, mPaint);

            BoundingBoxE6 box = mMapView.getBoundingBox();
        }

        void clear() {
            if (mPoints == null)
                return;
            mPoints = null;
            mMapView.invalidate();
        }

        void set(String track) {
            mPoints = new ArrayList<TrackPoint>();
            String[] points = track.split("\\|");
            min_lat = 180;
            min_lon = 180;
            max_lat = -180;
            max_lon = -180;
            for (String p : points) {
                String[] parts = p.split(",");
                if (parts.length != 4)
                    continue;
                TrackPoint point = new TrackPoint();
                try {
                    double lat = Double.parseDouble(parts[0]);
                    double lon = Double.parseDouble(parts[1]);
                    if (lat < min_lat)
                        min_lat = lat;
                    if (lat > max_lat)
                        max_lat = lat;
                    if (lon < min_lon)
                        min_lon = lon;
                    if (lon > max_lon)
                        max_lon = lon;
                    point.point = new GeoPoint(lat, lon);
                    point.speed = Integer.parseInt(parts[2]);
                    point.time = Long.parseLong(parts[3]);
                    mPoints.add(point);
                } catch (Exception ex) {
                    // ignore
                }
            }

            mMapView.invalidate();
        }
    }
}
