package net.ugona.plus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
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
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.SafeDrawOverlay;
import org.osmdroid.views.safecanvas.ISafeCanvas;
import org.osmdroid.views.safecanvas.SafePaint;
import org.osmdroid.views.safecanvas.SafeTranslatedPath;

import java.util.ArrayList;
import java.util.Vector;

public abstract class MapActivity extends ActionBarActivity {

    static final String TRAFFIC = "traffic";
    SharedPreferences preferences;
    FrameLayout holder;
    Menu topSubMenu;
    LocationManager locationManager;
    private MapView mMapView;

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
        boolean isOSM = preferences.getString("map_type", "OSM").equals("OSM");
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

    MapView getMapView() {
        return mMapView;
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
                final Rect rc = new Rect(myLocation.getLatitudeE6(), myLocation.getLongitudeE6(), myLocation.getLatitudeE6(), myLocation.getLongitudeE6());
                updateLocation(rc);
                mMapView.fitToRect(new GeoPoint(rc.left, rc.top), new GeoPoint(rc.right, rc.bottom), 0.9);
                break;
            }
            case R.id.google: {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.MAP_TYPE, "Google");
                ed.commit();
                updateMenu();
                mMapView.setTileSource(MapView.createTileSource(this, preferences));
                break;
            }
            case R.id.osm: {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.MAP_TYPE, "OSM");
                ed.commit();
                updateMenu();
                mMapView.setTileSource(MapView.createTileSource(this, preferences));
                break;
            }
        }
        return false;
    }

    void updateLocation(final Rect rc) {

    }

    void updateLocation(final Rect rc, MyOverlayItem item) {
        if (item.zone != null) {
            GeoPoint p1 = new GeoPoint(item.min_lat, item.min_lon);
            if (rc.left > p1.getLatitudeE6())
                rc.left = p1.getLatitudeE6();
            if (rc.bottom > p1.getLongitudeE6())
                rc.bottom = p1.getLongitudeE6();
            GeoPoint p2 = new GeoPoint(item.max_lat, item.max_lon);
            if (rc.right < p2.getLatitudeE6())
                rc.right = p2.getLatitudeE6();
            if (rc.top < p2.getLongitudeE6())
                rc.top = p2.getLongitudeE6();
        } else {
            if (rc.left > item.getPoint().getLatitudeE6())
                rc.left = item.getPoint().getLatitudeE6();
            if (rc.top > item.getPoint().getLongitudeE6())
                rc.top = item.getPoint().getLongitudeE6();
            if (rc.right < item.getPoint().getLatitudeE6())
                rc.right = item.getPoint().getLatitudeE6();
            if (rc.bottom < item.getPoint().getLongitudeE6())
                rc.bottom = item.getPoint().getLongitudeE6();
        }
    }

    void initUI() {
        holder = (FrameLayout) findViewById(R.id.webview);
        holder.setBackgroundColor(getResources().getColor(R.color.caldroid_gray));
        if (mMapView == null) {
            final IRegisterReceiver registerReceiver = new SimpleRegisterReceiver(getApplicationContext());
            ITileSource tileSource = MapView.createTileSource(this, preferences);

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
                    initMap(mMapView);
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

    abstract void initMap(MapView mapView);

    abstract int menuId();

    static class TrackPoint {
        GeoPoint point;
        int speed;
        long time;
    }

    static class ItemsOverlay<Item extends OverlayItem> extends ItemizedOverlayWithFocus<Item> {

        public final int mMarkerFocusedBackgroundColor = Color.rgb(255, 255, 200);
        private final Point mFocusedScreenCoords = new Point();
        private final Rect mRect = new Rect();
        public int DESCRIPTION_BOX_PADDING = 3;
        public int DESCRIPTION_BOX_CORNERWIDTH = 3;
        public int DESCRIPTION_LINE_HEIGHT = 12;
        public int DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT = 2;
        protected int DESCRIPTION_MAXWIDTH = 200;
        DisplayMetrics displayMetrics;
        Rect baloonRect;

        public ItemsOverlay(MapActivity activity) {
            super(new Vector<Item>(), new OnItemGestureListener<Item>() {
                @Override
                public boolean onItemSingleTapUp(int index, Item item) {
                    return true;
                }

                @Override
                public boolean onItemLongPress(int index, Item item) {
                    return false;
                }
            }, activity.mMapView.getResourceProxy());

            displayMetrics = activity.getResources().getDisplayMetrics();
            float density = displayMetrics.density;

            DESCRIPTION_BOX_PADDING = (int) (6 * density);
            DESCRIPTION_BOX_CORNERWIDTH = (int) (3 * density);

            DESCRIPTION_LINE_HEIGHT = (int) (12 * density);
            DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT = (int) (2 * density);

            int snippet_pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    14, displayMetrics);
            int title_pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    16, displayMetrics);
            mDescriptionPaint.setTextSize(snippet_pixel);
            mTitlePaint.setTextSize(title_pixel);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event, org.osmdroid.views.MapView mapView) {
            int focusedIndex = mFocusedItemIndex;
            if (focusedIndex != NOT_SET) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                if ((baloonRect != null) && baloonRect.contains(x, y))
                    return true;
            }
            boolean res = super.onSingleTapConfirmed(event, mapView);
            if (mFocusedItemIndex != focusedIndex) {
                onChangeFocus();
                return true;
            }
            Log.v("v", res + "," + mFocusedItemIndex);
            if (!res && (mFocusedItemIndex != NOT_SET)) {
                unSetFocusedItem();
                mapView.invalidate();
            }
            return res;
        }

        void onChangeFocus() {

        }

        @Override
        protected void drawFocusedItem(Canvas c, org.osmdroid.views.MapView osmv) {
            final Item focusedItem = super.mItemList.get(this.mFocusedItemIndex);
            Drawable markerFocusedBase = focusedItem.getMarker(OverlayItem.ITEM_STATE_FOCUSED_MASK);
            if (markerFocusedBase == null) {
                markerFocusedBase = this.mMarkerFocusedBase;
            }

            DESCRIPTION_MAXWIDTH = displayMetrics.widthPixels * 3 / 4;

		/* Calculate and set the bounds of the marker. */
            osmv.getProjection().toMapPixels(focusedItem.getPoint(), mFocusedScreenCoords);

            markerFocusedBase.copyBounds(mRect);
            mRect.offset(mFocusedScreenCoords.x, mFocusedScreenCoords.y);

		/* Strings of the OverlayItem, we need. */
            final String itemTitle = (focusedItem.getTitle() == null) ? "" : focusedItem
                    .getTitle();
            final String itemDescription = (focusedItem.getSnippet() == null) ? "" : focusedItem
                    .getSnippet();

		/*
         * Store the width needed for each char in the description to a float array. This is pretty
		 * efficient.
		 */
            float[] widths = new float[itemDescription.length()];
            this.mDescriptionPaint.getTextWidths(itemDescription, widths);

            StringBuilder sb = new StringBuilder();
            int maxWidth = 0;
            int curLineWidth = 0;
            int lastStop = 0;
            int i;
            int lastwhitespace = 0;
        /*
         * Loop through the charwidth array and harshly insert a linebreak, when the width gets
		 * bigger than DESCRIPTION_MAXWIDTH.
		 */
            for (i = 0; i < widths.length; i++) {
                if (itemDescription.charAt(i) == '\n') {
                    sb.append(itemDescription.subSequence(lastStop, i));
                    sb.append('\n');

                    lastStop = i + 1;
                    lastwhitespace = i + 1;
                    maxWidth = Math.max(maxWidth, curLineWidth);
                    curLineWidth = 0;
                    continue;
                }

                if (Character.isWhitespace(itemDescription.charAt(i))) {
                    lastwhitespace = i;
                }

                final float charwidth = widths[i];

                if (curLineWidth + charwidth > DESCRIPTION_MAXWIDTH) {
                    if (lastStop == lastwhitespace) {
                        i--;
                    } else {
                        i = lastwhitespace;
                    }

                    sb.append(itemDescription.subSequence(lastStop, i));
                    sb.append('\n');

                    lastStop = i;
                    maxWidth = Math.max(maxWidth, curLineWidth);
                    curLineWidth = 0;
                }

                curLineWidth += charwidth;
            }
        /* Add the last line to the rest to the buffer. */
            if (i != lastStop) {
                final String rest = itemDescription.substring(lastStop, i);
                maxWidth = Math.max(maxWidth, (int) this.mDescriptionPaint.measureText(rest));
                sb.append(rest);
            }
            final String[] lines = sb.toString().split("\n");

            widths = new float[itemTitle.length()];
            this.mTitlePaint.getTextWidths(itemTitle, widths);

            sb = new StringBuilder();
            curLineWidth = 0;
            lastStop = 0;
            lastwhitespace = 0;

        /*
         * Loop through the charwidth array and harshly insert a linebreak, when the width gets
		 * bigger than DESCRIPTION_MAXWIDTH.
		 */
            for (i = 0; i < widths.length; i++) {
                if (itemTitle.charAt(i) == '\n') {
                    sb.append(itemTitle.subSequence(lastStop, i));
                    sb.append('\n');

                    lastStop = i + 1;
                    lastwhitespace = i + 1;
                    maxWidth = Math.max(maxWidth, curLineWidth);
                    curLineWidth = 0;
                    continue;
                }

                if (Character.isWhitespace(itemTitle.charAt(i))) {
                    lastwhitespace = i;
                }

                final float charwidth = widths[i];

                if (curLineWidth + charwidth > DESCRIPTION_MAXWIDTH) {
                    if (lastStop == lastwhitespace) {
                        i--;
                    } else {
                        i = lastwhitespace;
                    }

                    sb.append(itemTitle.subSequence(lastStop, i));
                    sb.append('\n');

                    lastStop = i;
                    maxWidth = Math.max(maxWidth, curLineWidth);
                    curLineWidth = 0;
                }

                curLineWidth += charwidth;
            }
        /* Add the last line to the rest to the buffer. */
            if (i != lastStop) {
                final String rest = itemTitle.substring(lastStop, i);
                maxWidth = Math.max(maxWidth, (int) this.mTitlePaint.measureText(rest));
                sb.append(rest);
            }
            final String[] title_lines = sb.toString().split("\n");

            Rect bounds = new Rect();
            mTitlePaint.getTextBounds("a", 0, 1, bounds);
            final int titleLineHeight = bounds.height() + DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT * 2;
            mDescriptionPaint.getTextBounds("a", 0, 1, bounds);
            final int snippetLineHeight = bounds.height() + DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT * 2;

            int totalHeight = lines.length * snippetLineHeight;
            if (!itemTitle.equals(""))
                totalHeight += DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT + title_lines.length * titleLineHeight;

            final int descWidth = Math.min(maxWidth, DESCRIPTION_MAXWIDTH);

		/* Calculate the bounds of the Description box that needs to be drawn. */
            final int descBoxLeft = mRect.left - descWidth / 2 - DESCRIPTION_BOX_PADDING
                    + mRect.width() / 2;
            final int descBoxRight = descBoxLeft + descWidth + 2 * DESCRIPTION_BOX_PADDING;
            final int descBoxBottom = mRect.top;
            final int descBoxTop = descBoxBottom - totalHeight - 2 * DESCRIPTION_BOX_PADDING;

		/* Twice draw a RoundRect, once in black with 1px as a small border. */
            this.mMarkerBackgroundPaint.setColor(Color.BLACK);
            c.drawRoundRect(new RectF(descBoxLeft - 1, descBoxTop - 1, descBoxRight + 1,
                            descBoxBottom + 1), DESCRIPTION_BOX_CORNERWIDTH, DESCRIPTION_BOX_CORNERWIDTH,
                    this.mDescriptionPaint
            );
            this.mMarkerBackgroundPaint.setColor(this.mMarkerFocusedBackgroundColor);
            baloonRect = new Rect(descBoxLeft, descBoxTop, descBoxRight, descBoxBottom);
            final Rect screenRect = osmv.getProjection().getIntrinsicScreenRect();
            baloonRect.offset(-screenRect.left, -screenRect.top);
            c.drawRoundRect(new RectF(descBoxLeft, descBoxTop, descBoxRight, descBoxBottom),
                    DESCRIPTION_BOX_CORNERWIDTH, DESCRIPTION_BOX_CORNERWIDTH,
                    this.mMarkerBackgroundPaint);

            final int descLeft = descBoxLeft + DESCRIPTION_BOX_PADDING;
            int descTextLineBottom = descBoxBottom - DESCRIPTION_BOX_PADDING;

		/* Draw all the lines of the description. */
            for (int j = lines.length - 1; j >= 0; j--) {
                c.drawText(lines[j].trim(), descLeft, descTextLineBottom, this.mDescriptionPaint);
                descTextLineBottom -= snippetLineHeight;
            }
		/* Draw the title. */
            descTextLineBottom -= DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT;
            for (int j = title_lines.length - 1; j >= 0; j--) {
                c.drawText(title_lines[j].trim(), descLeft, descTextLineBottom, this.mTitlePaint);
                descTextLineBottom -= titleLineHeight;
            }

		/*
		 * Finally draw the marker base. This is done in the end to make it look better.
		 */
            Overlay.drawAt(c, markerFocusedBase, mFocusedScreenCoords.x, mFocusedScreenCoords.y, false, osmv.getMapOrientation());
        }
    }

    static class TrackOverlay extends SafeDrawOverlay {

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
        private final SafeTranslatedPath mPath = new SafeTranslatedPath();
        private final Point mTempPoint1 = new Point();
        private final Point mTempPoint2 = new Point();
        protected SafePaint mPaint = new SafePaint();
        double min_lat;
        double max_lat;
        double min_lon;
        double max_lon;
        Vector<ArrayList<TrackPoint>> tracks;

        boolean show_speed;

        public TrackOverlay(Context ctx) {
            super(ctx);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
            show_speed = preferences.getBoolean(TRAFFIC, true);
            DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
            mPaint.setStrokeWidth((int) (displayMetrics.density * 4));
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setAntiAlias(true);
        }

        @Override
        public boolean isHardwareAccelerated() {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e, org.osmdroid.views.MapView mapView) {
            if (tracks != null) {
                int x = (int) e.getX();
                int y = (int) e.getY();
                final org.osmdroid.views.MapView.Projection pj = mapView.getProjection();
                final Rect screenRect = pj.getIntrinsicScreenRect();
                int min_dist = 10000;
                int min_pos = 0;
                int min_index = 0;
                int min_track = 0;

                int track_index = 0;
                for (ArrayList<TrackPoint> track : tracks) {
                    final int size = track.size();
                    if (size < 2)
                        continue;
                    TrackPoint p0 = track.get(0);
                    Point s0 = pj.toPixels(p0.point, mTempPoint1);

                    for (int i = 1; i < track.size(); i++) {
                        TrackPoint p = track.get(i);
                        Point s = pj.toPixels(p.point, mTempPoint2);
                        if ((s0.x == s.x) && (s0.y == s.y))
                            continue;
                        int ax = s0.x - screenRect.left;
                        int ay = s0.y - screenRect.top;
                        int bx = s.x - screenRect.left;
                        int by = s.y - screenRect.top;
                        int cax = x - ax;
                        int cay = y - ay;
                        int bax = bx - ax;
                        int bay = by - ay;
                        int pp = cax * bax + cay * bay;
                        int dist;
                        int pos;
                        if (pp <= 0) {
                            dist = cax * cax + cay * cay;
                            pos = 0;
                        } else {
                            int l = bax * bax + bay * bay;
                            if (pp >= l) {
                                int cbx = x - bx;
                                int cby = y - by;
                                dist = cbx * cbx + cby * cby;
                                pos = 1000;
                            } else {
                                pos = 1000 * pp / l;
                                bax = bax * pp / l;
                                bay = bay * pp / l;
                                cax -= bax;
                                cay -= bay;
                                dist = cax * cax + cay * cay;
                            }
                        }
                        if (dist < min_dist) {
                            min_dist = dist;
                            min_pos = pos;
                            min_index = i - 1;
                            min_track = track_index;
                        }
                        s0.x = s.x;
                        s0.y = s.y;
                    }
                    track_index++;
                }
                showBaloon(min_dist, min_track, min_index, min_pos);
            }
            return super.onSingleTapConfirmed(e, mapView);
        }

        @Override
        protected void drawSafe(ISafeCanvas canvas, org.osmdroid.views.MapView mapView, boolean shadow) {
            if (shadow)
                return;

            if (tracks == null)
                return;

            final org.osmdroid.views.MapView.Projection pj = mapView.getProjection();
            Rect screenRect = pj.getScreenRect();

            for (ArrayList<TrackPoint> track : tracks) {

                final int size = track.size();
                if (size < 2)
                    continue;

                Point screenPoint0 = null; // points on screen
                Point screenPoint1;
                GeoPoint projectedPoint0; // points from the points list
                GeoPoint projectedPoint1;

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
                        mPath.moveTo(screenPoint0.x - screenRect.left, screenPoint0.y - screenRect.top);
                    }

                    screenPoint1 = pj.toPixels(projectedPoint1, mTempPoint2);

                    // skip this point, too close to previous point
                    if (Math.abs(screenPoint1.x - screenPoint0.x) + Math.abs(screenPoint1.y - screenPoint0.y) <= 1) {
                        continue;
                    }

                    mPath.lineTo(screenPoint1.x - screenRect.left, screenPoint1.y - screenRect.top);
                    int new_color = getColor(track.get(i).speed);
                    if (new_color != color) {
                        mPaint.setColor(colors[color]);
                        canvas.drawPath(mPath, mPaint);
                        mPath.rewind();
                        mPath.moveTo(screenPoint1.x - screenRect.left, screenPoint1.y - screenRect.top);
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

        void showBaloon(int dist, int track_index, int point_index, int pos) {
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

        void clear(MapView mapView) {
            if (tracks == null)
                return;
            tracks = null;
            mapView.invalidate();
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

    static class LocationOverlay extends ItemsOverlay<MyOverlayItem> {

        protected final Bitmap mDirectionArrowBitmap;
        protected final Bitmap mCurrentArrowBitmap;
        protected final double mDirectionArrowCenterX;
        protected final double mDirectionArrowCenterY;
        protected final SafePaint mPaint = new SafePaint();
        String car_id;

        public LocationOverlay(MapActivity activity) {
            super(activity);

            mDirectionArrowBitmap = mResourceProxy.getBitmap(ResourceProxy.bitmap.direction_arrow);
            mCurrentArrowBitmap = mResourceProxy.getBitmap(ResourceProxy.bitmap.cur_arrow);

            mDirectionArrowCenterX = mDirectionArrowBitmap.getWidth() / 2.0 - 0.5;
            mDirectionArrowCenterY = mDirectionArrowBitmap.getHeight() / 2.0 - 0.5;
        }

        @Override
        public boolean isHardwareAccelerated() {
            return false;
        }

        @Override
        protected boolean hitTest(org.osmdroid.views.MapView mapView, MyOverlayItem item, Drawable marker, int hitX, int hitY) {
            if (item.zone == null) {
                double x = hitX;
                double y = hitY;
                double r = mDirectionArrowBitmap.getWidth() / 2;
                boolean res = x * x + y * y <= r * r;
                return res;
            }
            final org.osmdroid.views.MapView.Projection pj = mapView.getProjection();
            Point tempPoint = new Point();
            pj.toPixels(item.getPoint(), tempPoint);
            Rect screenRect = pj.getScreenRect();

            IGeoPoint p = pj.fromPixels(hitX + tempPoint.x - screenRect.left, hitY + tempPoint.y - screenRect.top);
            return (p.getLatitude() >= item.min_lat) && (p.getLatitude() <= item.max_lat) &&
                    (p.getLongitude() >= item.min_lon) && (p.getLongitude() <= item.max_lon);
        }

        @Override
        protected void onDrawItem(org.osmdroid.views.MapView mapView, final ISafeCanvas canvas, final MyOverlayItem item, final Point curScreenCoords, final float aMapOrientation) {

            final org.osmdroid.views.MapView.Projection pj = mapView.getProjection();
            Rect screenRect = pj.getScreenRect();

            if (item.zone != null) {

                Point screenPoint0 = null; // points on screen
                Point screenPoint1;
                GeoPoint projectedPoint0; // points from the points list
                GeoPoint projectedPoint1;

                int size = item.zone.size();

                SafeTranslatedPath mPath = new SafeTranslatedPath();

                Point mTempPoint1 = new Point();
                Point mTempPoint2 = new Point();

                mPath.rewind();

                projectedPoint0 = item.zone.get(size - 1);

                for (int i = size - 2; i >= 0; i--) {
                    // compute next points
                    projectedPoint1 = item.zone.get(i);

                    // the starting point may be not calculated, because previous segment was out of clip
                    // bounds
                    if (screenPoint0 == null) {
                        screenPoint0 = pj.toPixels(projectedPoint0, mTempPoint1);
                        mPath.moveTo(screenPoint0.x - screenRect.left, screenPoint0.y - screenRect.top);
                    }

                    screenPoint1 = pj.toPixels(projectedPoint1, mTempPoint2);

                    // skip this point, too close to previous point
                    if (Math.abs(screenPoint1.x - screenPoint0.x) + Math.abs(screenPoint1.y - screenPoint0.y) <= 1) {
                        continue;
                    }

                    mPath.lineTo(screenPoint1.x - screenRect.left, screenPoint1.y - screenRect.top);

                    // update starting point to next position
                    projectedPoint0 = projectedPoint1;
                    screenPoint0.x = screenPoint1.x;
                    screenPoint0.y = screenPoint1.y;
                }
                mPath.close();

                SafePaint mPaint = new SafePaint();
                if (item.getUid().equals(car_id)) {
                    mPaint.setColor(Color.rgb(255, 0, 0));
                } else {
                    mPaint.setColor(Color.rgb(0, 0, 255));
                }
                mPaint.setStrokeWidth(4);
                mPaint.setAlpha(20);
                mPaint.setStyle(Paint.Style.FILL);
                canvas.drawPath(mPath, mPaint);
                mPaint.setAlpha(80);
                mPaint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(mPath, mPaint);
                return;
            }

            Point mTempPoint = new Point();
            Point screenPoint = pj.toPixels(item.getPoint(), mTempPoint);
            canvas.save();
            canvas.rotate(item.mBearing, screenPoint.x, screenPoint.y);
            Bitmap bitmap = (item.getUid().equals(car_id)) ? mCurrentArrowBitmap : mDirectionArrowBitmap;
            canvas.drawBitmap(bitmap, screenPoint.x - mDirectionArrowCenterX, screenPoint.y - mDirectionArrowCenterY, null);
            canvas.restore();
        }

    }

    class MyOverlayItem extends OverlayItem {

        int mBearing;
        ArrayList<GeoPoint> zone;
        double min_lat;
        double min_lon;
        double max_lat;
        double max_lon;

        public MyOverlayItem(String aUid) {
            super(aUid, null, null, null);
        }

        void set(String title, String snippet, GeoPoint point, int bearing) {
            mTitle = title;
            mSnippet = snippet;
            mGeoPoint = point;
            mBearing = bearing;
        }

        void setZone(String s) {
            if (s == null) {
                zone = null;
                return;
            }
            min_lat = 180;
            min_lon = 180;
            max_lat = -180;
            max_lon = -180;
            String points[] = s.split("_");

            zone = new ArrayList<GeoPoint>();
            for (String point : points) {
                try {
                    String[] p = point.split(",");
                    double p_lat = Double.parseDouble(p[0]);
                    double p_lon = Double.parseDouble(p[1]);
                    if (p_lat < min_lat)
                        min_lat = p_lat;
                    if (p_lat > max_lat)
                        max_lat = p_lat;
                    if (p_lon < min_lon)
                        min_lon = p_lon;
                    if (p_lon > max_lon)
                        max_lon = p_lon;
                    zone.add(new GeoPoint(p_lat, p_lon));
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
    }

}
