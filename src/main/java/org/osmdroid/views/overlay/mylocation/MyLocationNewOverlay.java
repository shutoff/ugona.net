package org.osmdroid.views.overlay.mylocation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.location.Location;
import android.util.FloatMath;
import android.view.MotionEvent;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.api.IMapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.TileSystem;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.Overlay.Snappable;
import org.osmdroid.views.overlay.SafeDrawOverlay;
import org.osmdroid.views.safecanvas.ISafeCanvas;
import org.osmdroid.views.safecanvas.SafePaint;
import org.osmdroid.views.util.constants.MapViewConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * @author Marc Kurtz
 * @author Manuel Stahl
 */
public class MyLocationNewOverlay extends SafeDrawOverlay implements IMyLocationConsumer, Snappable {
    public static final int MENU_MY_LOCATION = getSafeMenuId();

    // ===========================================================
    // Constants
    // ===========================================================

    // ===========================================================
    // Fields
    // ===========================================================
    private static final Logger logger = LoggerFactory.getLogger(MyLocationNewOverlay.class);
    protected final SafePaint mPaint = new SafePaint();
    protected final SafePaint mCirclePaint = new SafePaint();
    protected final Bitmap mPersonBitmap;
    protected final MapView mMapView;
    /**
     * Coordinates the feet of the person are located scaled for display density.
     */
    protected final PointF mPersonHotspot;
    private final IMapController mMapController;
    private final LinkedList<Runnable> mRunOnFirstFix = new LinkedList<Runnable>();
    private final Point mMapCoords = new Point();
    private final GeoPoint mGeoPoint = new GeoPoint(0, 0); // for reuse
    // to avoid allocations during onDraw
    private final float[] mMatrixValues = new float[9];
    private final Matrix mMatrix = new Matrix();
    private final Rect mMyLocationRect = new Rect();
    private final Rect mMyLocationPreviousRect = new Rect();
    public IMyLocationProvider mMyLocationProvider;
    protected boolean mIsFollowing = false; // follow location updates
    protected boolean mDrawAccuracyEnabled = true;
    private Location mLocation;
    private boolean mIsLocationEnabled = false;
    private boolean mOptionsMenuEnabled = true;

    // ===========================================================
    // Constructors
    // ===========================================================

    public MyLocationNewOverlay(Context context, MapView mapView) {
        this(context, new GpsMyLocationProvider(context), mapView);
    }

    public MyLocationNewOverlay(Context context, IMyLocationProvider myLocationProvider,
                                MapView mapView) {
        this(myLocationProvider, mapView, new DefaultResourceProxyImpl(context));
    }

    public MyLocationNewOverlay(IMyLocationProvider myLocationProvider, MapView mapView,
                                ResourceProxy resourceProxy) {
        super(resourceProxy);

        mMapView = mapView;
        mMapController = mapView.getController();
        mCirclePaint.setARGB(0, 100, 100, 255);
        mCirclePaint.setAntiAlias(true);

        mPersonBitmap = mResourceProxy.getBitmap(ResourceProxy.bitmap.person);

        // Calculate position of person icon's feet, scaled to screen density
        mPersonHotspot = new PointF(24.0f * mScale + 0.5f, 39.0f * mScale + 0.5f);

        setMyLocationProvider(myLocationProvider);
    }

    @Override
    public void onDetach(MapView mapView) {
        this.disableMyLocation();
        super.onDetach(mapView);
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    /**
     * If enabled, an accuracy circle will be drawn around your current position.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isDrawAccuracyEnabled() {
        return mDrawAccuracyEnabled;
    }

    /**
     * If enabled, an accuracy circle will be drawn around your current position.
     *
     * @param drawAccuracyEnabled whether the accuracy circle will be enabled
     */
    public void setDrawAccuracyEnabled(final boolean drawAccuracyEnabled) {
        mDrawAccuracyEnabled = drawAccuracyEnabled;
    }

    public IMyLocationProvider getMyLocationProvider() {
        return mMyLocationProvider;
    }

    protected void setMyLocationProvider(IMyLocationProvider myLocationProvider) {
        if (myLocationProvider == null)
            throw new RuntimeException(
                    "You must pass an IMyLocationProvider to setMyLocationProvider()");

        if (mMyLocationProvider != null)
            mMyLocationProvider.stopLocationProvider();

        mMyLocationProvider = myLocationProvider;
    }

    public void setPersonHotspot(float x, float y) {
        mPersonHotspot.set(x, y);
    }

    protected void drawMyLocation(final ISafeCanvas canvas, final MapView mapView,
                                  final Location lastFix) {
        final Projection pj = mapView.getProjection();

        Point mTempPoint = new Point();
        Point screenPoint = pj.toPixels(new GeoPoint(lastFix), mTempPoint);

        if (mDrawAccuracyEnabled) {
            final float radius = lastFix.getAccuracy()
                    / (float) TileSystem.GroundResolution(lastFix.getLatitude(),
                    mapView.getZoomLevel());

            mCirclePaint.setAlpha(50);
            mCirclePaint.setStyle(Style.FILL);
            canvas.drawCircle(screenPoint.x, screenPoint.y, radius, mCirclePaint);

            mCirclePaint.setAlpha(150);
            mCirclePaint.setStyle(Style.STROKE);
            canvas.drawCircle(screenPoint.x, screenPoint.y, radius, mCirclePaint);
        }
        canvas.drawBitmap(mPersonBitmap, screenPoint.x - mPersonHotspot.x, screenPoint.y - mPersonHotspot.y, null);
    }

    protected Rect getMyLocationDrawingBounds(int zoomLevel, Location lastFix, Rect reuse) {
        if (reuse == null)
            reuse = new Rect();

        final int zoomDiff = MapViewConstants.MAXIMUM_ZOOMLEVEL - zoomLevel;
        final int posX = mMapCoords.x >> zoomDiff;
        final int posY = mMapCoords.y >> zoomDiff;

        reuse.set(posX, posY, posX + mPersonBitmap.getWidth(), posY + mPersonBitmap.getHeight());
        reuse.offset((int) (-mPersonHotspot.x + 0.5f), (int) (-mPersonHotspot.y + 0.5f));

        // Add in the accuracy circle if enabled
        if (mDrawAccuracyEnabled) {
            final int radius = (int) FloatMath.ceil(lastFix.getAccuracy()
                    / (float) TileSystem.GroundResolution(lastFix.getLatitude(), zoomLevel));
            reuse.union(posX - radius, posY - radius, posX + radius, posY + radius);
            final int strokeWidth = (int) FloatMath.ceil(mCirclePaint.getStrokeWidth() == 0 ? 1
                    : mCirclePaint.getStrokeWidth());
            reuse.inset(-strokeWidth, -strokeWidth);
        }

        return reuse;
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    protected void drawSafe(ISafeCanvas canvas, MapView mapView, boolean shadow) {
        if (shadow)
            return;

        if (mLocation != null && isMyLocationEnabled()) {
            drawMyLocation(canvas, mapView, mLocation);
        }
    }

    @Override
    public boolean onSnapToItem(final int x, final int y, final Point snapPoint,
                                final IMapView mapView) {
        if (this.mLocation != null) {
            snapPoint.x = mMapCoords.x;
            snapPoint.y = mMapCoords.y;
            final double xDiff = x - mMapCoords.x;
            final double yDiff = y - mMapCoords.y;
            final boolean snap = xDiff * xDiff + yDiff * yDiff < 64;
            if (DEBUGMODE) {
                logger.debug("snap=" + snap);
            }
            return snap;
        } else {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event, final MapView mapView) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            this.disableFollowLocation();
        }

        return super.onTouchEvent(event, mapView);
    }

    // ===========================================================
    // Methods
    // ===========================================================

    /**
     * Return a GeoPoint of the last known location, or null if not known.
     */
    public GeoPoint getMyLocation() {
        if (mLocation == null) {
            return null;
        } else {
            return new GeoPoint(mLocation);
        }
    }

    public Location getLastFix() {
        return mLocation;
    }

    /**
     * Enables "follow" functionality. The map will center on your current location and
     * automatically scroll as you move. Scrolling the map in the UI will disable.
     */
    public void enableFollowLocation() {
        mIsFollowing = true;

        // set initial location when enabled
        if (isMyLocationEnabled()) {
            mLocation = mMyLocationProvider.getLastKnownLocation();
            if (mLocation != null) {
                TileSystem.LatLongToPixelXY(mLocation.getLatitude(), mLocation.getLongitude(),
                        MapViewConstants.MAXIMUM_ZOOMLEVEL, mMapCoords);
                final int worldSize_2 = TileSystem.MapSize(MapViewConstants.MAXIMUM_ZOOMLEVEL) / 2;
                mMapCoords.offset(-worldSize_2, -worldSize_2);
                mMapController.animateTo(new GeoPoint(mLocation));
            }
        }

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView.postInvalidate();
        }
    }

    /**
     * Disables "follow" functionality.
     */
    public void disableFollowLocation() {
        mIsFollowing = false;
    }

    /**
     * If enabled, the map will center on your current location and automatically scroll as you
     * move. Scrolling the map in the UI will disable.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isFollowLocationEnabled() {
        return mIsFollowing;
    }

    @Override
    public void onLocationChanged(Location location, IMyLocationProvider source) {
        // If we had a previous location, let's get those bounds
        Location oldLocation = mLocation;
        if (oldLocation != null) {
            this.getMyLocationDrawingBounds(mMapView.getZoomLevel(), oldLocation,
                    mMyLocationPreviousRect);
        }

        mLocation = location;
        mMapCoords.set(0, 0);

        if (mLocation != null) {
            TileSystem.LatLongToPixelXY(mLocation.getLatitude(), mLocation.getLongitude(),
                    MapViewConstants.MAXIMUM_ZOOMLEVEL, mMapCoords);
            final int worldSize_2 = TileSystem.MapSize(MapViewConstants.MAXIMUM_ZOOMLEVEL) / 2;
            mMapCoords.offset(-worldSize_2, -worldSize_2);

            if (mIsFollowing) {
                mGeoPoint.setLatitudeE6((int) (mLocation.getLatitude() * 1E6));
                mGeoPoint.setLongitudeE6((int) (mLocation.getLongitude() * 1E6));
                mMapController.animateTo(mGeoPoint);
            } else {
                // Get new drawing bounds
                this.getMyLocationDrawingBounds(mMapView.getZoomLevel(), mLocation, mMyLocationRect);

                // If we had a previous location, merge in those bounds too
                if (oldLocation != null) {
                    mMyLocationRect.union(mMyLocationPreviousRect);
                }

                final int left = mMyLocationRect.left;
                final int top = mMyLocationRect.top;
                final int right = mMyLocationRect.right;
                final int bottom = mMyLocationRect.bottom;

                // Invalidate the bounds
                mMapView.post(new Runnable() {
                    @Override
                    public void run() {
                        mMapView.invalidateMapCoordinates(left, top, right, bottom);
                    }
                });
            }
        }

        for (final Runnable runnable : mRunOnFirstFix) {
            new Thread(runnable).start();
        }
        mRunOnFirstFix.clear();
    }

    /**
     * Enable receiving location updates from the provided IMyLocationProvider and show your
     * location on the maps. You will likely want to call enableMyLocation() from your Activity's
     * Activity.onResume() method, to enable the features of this overlay. Remember to call the
     * corresponding disableMyLocation() in your Activity's Activity.onPause() method to turn off
     * updates when in the background.
     */
    public boolean enableMyLocation(boolean gps_enabled, boolean net_enabled) {
        if (mIsLocationEnabled)
            mMyLocationProvider.stopLocationProvider();

        boolean result = mMyLocationProvider.startLocationProvider(this, gps_enabled, net_enabled);
        mIsLocationEnabled = result;

        // set initial location when enabled
        if (result && isFollowLocationEnabled()) {
            mLocation = mMyLocationProvider.getLastKnownLocation();
            if (mLocation != null) {
                TileSystem.LatLongToPixelXY(mLocation.getLatitude(), mLocation.getLongitude(),
                        MapViewConstants.MAXIMUM_ZOOMLEVEL, mMapCoords);
                final int worldSize_2 = TileSystem.MapSize(MapViewConstants.MAXIMUM_ZOOMLEVEL) / 2;
                mMapCoords.offset(-worldSize_2, -worldSize_2);
                mMapController.animateTo(new GeoPoint(mLocation));
            }
        }

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView.postInvalidate();
        }

        return result;
    }

    /**
     * Disable location updates
     */
    public void disableMyLocation() {
        mIsLocationEnabled = false;

        if (mMyLocationProvider != null) {
            mMyLocationProvider.stopLocationProvider();
        }

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView.postInvalidate();
        }
    }

    /**
     * If enabled, the map is receiving location updates and drawing your location on the map.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isMyLocationEnabled() {
        return mIsLocationEnabled;
    }

    public boolean runOnFirstFix(final Runnable runnable) {
        if (mMyLocationProvider != null && mLocation != null) {
            new Thread(runnable).start();
            return true;
        } else {
            mRunOnFirstFix.addLast(runnable);
            return false;
        }
    }

}
