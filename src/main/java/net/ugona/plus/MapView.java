package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapView extends org.osmdroid.views.MapView {

    SharedPreferences preferences;

    MyLocationNewOverlay mLocationOverlay;
    Overlay mTrackOverlay;
    Overlay mPointsOverlay;

    Runnable mAfterLayout;
    int layout_count;

    public MapView(Context context, final MapTileProviderBase tileProvider) {
        super(context, (int) (256 * context.getResources().getDisplayMetrics().density), new ResourceProxyImpl(context.getApplicationContext()), tileProvider);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        setUseSafeCanvas(true);

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), this, getResourceProxy());
        getOverlays().add(mLocationOverlay);

        setBackgroundColor(getResources().getColor(R.color.caldroid_gray));
    }

    static ITileSource createTileSource(Context ctx, SharedPreferences preferences) {
        if (preferences.getString("map_type", "OSM").equals("OSM")) {
            final String[] tiles_urls = {
                    "http://otile1.mqcdn.com/tiles/1.0.0/osm/"
            };
            return new XYTileSource("mqcdn", ResourceProxy.string.mapnik, 1, 17, (int) (256 * ctx.getResources().getDisplayMetrics().density), ".png", tiles_urls);
        }
        final String[] tiles_urls = {
                "http://mt0.google.com/vt/lyrs=m&hl=ru&x=%s&y=%s&z=%s&s=Galileo",
                "http://mt1.google.com/vt/lyrs=m&hl=ru&x=%s&y=%s&z=%s&s=Galileo",
                "http://mt2.google.com/vt/lyrs=m&hl=ru&x=%s&y=%s&z=%s&s=Galileo",
                "http://mt3.google.com/vt/lyrs=m&hl=ru&x=%s&y=%s&z=%s&s=Galileo"
        };
        return new myTileSource("google", ResourceProxy.string.mapnik, 1, 17, (int) (256 * ctx.getResources().getDisplayMetrics().density), ".png", tiles_urls);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mAfterLayout == null)
            return;
        if (--layout_count > 0)
            return;
        Runnable afterLayout = mAfterLayout;
        mAfterLayout = null;
        Handler handler = new Handler();
        handler.postDelayed(afterLayout, 500);
    }

    void setAfterLayout(Runnable afterLayout) {
        mAfterLayout = afterLayout;
        layout_count = 2;
    }

    void onResume() {
        mLocationOverlay.enableMyLocation();
        setBuiltInZoomControls(true);
        setMultiTouchControls(true);
    }

    void onPause() {
        mLocationOverlay.disableMyLocation();
        setBuiltInZoomControls(false);
        setMultiTouchControls(false);
    }

    IGeoPoint getMyLocation() {
        if (mLocationOverlay == null)
            return null;
        return mLocationOverlay.getMyLocation();
    }

    void fitToRect(IGeoPoint p1, IGeoPoint p2, double k) {
        int lat1 = p1.getLatitudeE6();
        int lat2 = p2.getLatitudeE6();
        if (lat1 > lat2) {
            int r = lat1;
            lat1 = lat2;
            lat2 = r;
        }

        int lon1 = p1.getLongitudeE6();
        int lon2 = p2.getLongitudeE6();
        if (lon1 > lon2) {
            int r = lon1;
            lon1 = lon2;
            lon2 = r;
        }
        int lat = (lat1 + lat2) / 2;
        int lon = (lon1 + lon2) / 2;
        int dlat = lat - lat1;
        dlat = (int) (dlat / k);
        int dlon = lon - lon1;
        dlon = (int) (dlon / k);

        zoomToBoundingBox(new BoundingBoxE6(lat + dlat, lon - dlon, lat - dlat, lon + dlon));
    }

    static public class myTileSource extends XYTileSource {

        public myTileSource(String aName, ResourceProxy.string aResourceId, int aZoomMinLevel, int aZoomMaxLevel, int aTileSizePixels, String aImageFilenameEnding, String[] aBaseUrl) {
            super(aName, aResourceId, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels, aImageFilenameEnding, aBaseUrl);
        }

        @Override
        public String getTileURLString(MapTile aTile) {
            return String.format(getBaseUrl(), aTile.getX(), aTile.getY(), aTile.getZoomLevel());
        }

    }

}
