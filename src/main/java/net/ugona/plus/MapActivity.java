package net.ugona.plus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
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
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.Vector;

public abstract class MapActivity extends ActionBarActivity {

    static final String TRAFFIC = "traffic";
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
        MenuItem item = menu.findItem(R.id.traffic);
        if (item != null)
            item.setTitle(getCheckedText(R.string.traffic, preferences.getBoolean(TRAFFIC, true)));
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

        final int[] colors = {
                Color.rgb(0, 0, 128),
                Color.rgb(128, 0, 0),
                Color.rgb(192, 0, 0),
                Color.rgb(192, 64, 0),
                Color.rgb(192, 128, 0),
                Color.rgb(160, 128, 0),
                Color.rgb(64, 128, 0),
                Color.rgb(0, 160, 0),
                Color.rgb(0, 160, 32),
                Color.rgb(0, 160, 128),
        };
        final int[] speeds = {
                5,
                10,
                20,
                30,
                40,
                50,
                60,
                90,
        };
        private final Path mPath = new Path();
        private final Point mTempPoint1 = new Point();
        private final Point mTempPoint2 = new Point();
        protected Paint mPaint = new Paint();
        double min_lat;
        double max_lat;
        double min_lon;
        double max_lon;
        Vector<ArrayList<TrackPoint>> tracks;
        boolean show_speed;

        public TrackOverlay(Context ctx) {
            super(ctx);
            show_speed = preferences.getBoolean(TRAFFIC, true);
        }

        @Override
        public boolean isHardwareAccelerated() {
            return false;
        }

        @Override
        protected void draw(Canvas canvas, org.osmdroid.views.MapView mapView, boolean shadow) {
            if (shadow)
                return;

            if (tracks == null)
                return;

            for (ArrayList<TrackPoint> track : tracks) {

                final int size = track.size();
                if (size < 2)
                    continue;

                final org.osmdroid.views.MapView.Projection pj = mapView.getProjection();

                Point screenPoint0 = null; // points on screen
                Point screenPoint1;
                GeoPoint projectedPoint0; // points from the points list
                GeoPoint projectedPoint1;

                mPaint.setStrokeWidth(4);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setAntiAlias(true);

                mPath.rewind();
                TrackPoint p = track.get(size - 1);
                projectedPoint0 = p.point;
                int color = getColor(p.speed);

                for (int i = size - 2; i >= 0; i--) {
                    // compute next points
                    projectedPoint1 = track.get(i).point;

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
                    int new_color = getColor(track.get(i).speed);
                    if (new_color != color) {
                        mPaint.setColor(colors[color]);
                        canvas.drawPath(mPath, mPaint);
                        mPath.rewind();
                        mPath.moveTo(screenPoint1.x, screenPoint1.y);
                        color = new_color;
                    }

                    // update starting point to next position
                    projectedPoint0 = projectedPoint1;
                    screenPoint0.x = screenPoint1.x;
                    screenPoint0.y = screenPoint1.y;
                }

                mPaint.setColor(colors[color]);
                canvas.drawPath(mPath, mPaint);
            }
        }

        int getColor(int speed) {
            if (!show_speed)
                return 0;
            int res = 1;
            for (int s : speeds) {
                if (speed < s)
                    break;
                res++;
            }
            return res;
        }

        void clear() {
            if (tracks == null)
                return;
            tracks = null;
            mMapView.invalidate();
        }

        void add(String track) {
            if (tracks == null) {
                tracks = new Vector<ArrayList<TrackPoint>>();
                min_lat = 180;
                min_lon = 180;
                max_lat = -180;
                max_lon = -180;
            }
            ArrayList<TrackPoint> trackPoints = new ArrayList<TrackPoint>();
            String[] points = track.split("\\|");
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
                    trackPoints.add(point);
                } catch (Exception ex) {
                    // ignore
                }
            }
            tracks.add(trackPoints);
        }
    }
}
