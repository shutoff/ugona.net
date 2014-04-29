package net.ugona.plus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapView extends org.osmdroid.views.MapView {

    SharedPreferences preferences;

    MyLocationNewOverlay mLocationOverlay;

    public MapView(Context context, final MapTileProviderBase tileProvider) {
        super(context, 256, new ResourceProxyImpl(context.getApplicationContext()), tileProvider);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        setUseSafeCanvas(true);

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), this, getResourceProxy());
        getOverlays().add(mLocationOverlay);

        setBackgroundColor(getResources().getColor(R.color.caldroid_gray));
    }

    static ITileSource createTileSource(SharedPreferences preferences) {
        if (preferences.getString("map_type", "").equals("OSM")) {
            final String[] tiles_urls = {
                    "http://otile1.mqcdn.com/tiles/1.0.0/osm/"
            };
            return new XYTileSource("mqcdn", ResourceProxy.string.mapnik, 1, 18, 256, ".png", tiles_urls);
        }
        final String[] tiles_urls = {
                "http://mt0.google.com/vt/lyrs=m&hl=ru&x=%s&y=%s&z=%s&s=Galileo",
                "http://mt1.google.com/vt/lyrs=m&hl=ru&x=%s&y=%s&z=%s&s=Galileo",
                "http://mt2.google.com/vt/lyrs=m&hl=ru&x=%s&y=%s&z=%s&s=Galileo",
                "http://mt3.google.com/vt/lyrs=m&hl=ru&x=%s&y=%s&z=%s&s=Galileo"
        };
        return new myTileSource("google", ResourceProxy.string.mapnik, 1, 18, 256, ".png", tiles_urls);
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
