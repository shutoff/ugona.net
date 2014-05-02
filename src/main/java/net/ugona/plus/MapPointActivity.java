package net.ugona.plus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import org.joda.time.LocalDateTime;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.TileSystem;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.safecanvas.ISafeCanvas;
import org.osmdroid.views.safecanvas.SafePaint;
import org.osmdroid.views.safecanvas.SafeTranslatedPath;
import org.osmdroid.views.util.constants.MapViewConstants;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class MapPointActivity extends MapActivity {

    static final int REQUEST_ALARM = 4000;
    static final int UPDATE_INTERVAL = 30 * 1000;

    final static String URL_TRACKS = "https://car-online.ugona.net/tracks?skey=$1&begin=$2&end=$3";

    String car_id;
    String point_data;
    Map<String, String> times;
    DateFormat df;
    DateFormat tf;
    Cars.Car[] cars;
    LocationOverlay mMyLocationOverlay;
    TrackOverlay mTrackOverlay;

    AlarmManager alarmMgr;
    PendingIntent pi;
    BroadcastReceiver br;

    boolean active;
    HttpTask trackTask;

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

        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = createPendingResult(REQUEST_ALARM, new Intent(), 0);
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateTrack();
                stopTimer();
                startTimer(false);
            }
        };
        registerReceiver(br, new IntentFilter(FetchService.ACTION_UPDATE));
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
        startTimer(true);
        setActionBar();
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
        stopTimer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ALARM) {
            Intent intent = new Intent(this, FetchService.class);
            intent.putExtra(Names.ID, car_id);
            startService(intent);
        }
    }

    void startTimer(boolean now) {
        if (!active)
            return;
        alarmMgr.setInexactRepeating(AlarmManager.RTC,
                System.currentTimeMillis() + (now ? 0 : UPDATE_INTERVAL), UPDATE_INTERVAL, pi);
    }

    void stopTimer() {
        alarmMgr.cancel(pi);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    int menuId() {
        return R.menu.map;
    }

    @Override
    void initMap(IMapController controller) {

        final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        if (cars == null)
            cars = Cars.getCars(this);

        mTrackOverlay = new TrackOverlay(this);
        mMapView.getOverlays().add(mTrackOverlay);

        mMyLocationOverlay = new LocationOverlay(mMapView, this);

        mMyLocationOverlay.setFocusItemsOnTap(true);
        mMyLocationOverlay.setFocusedItem(mMyLocationOverlay.find(car_id));

        mMapView.getOverlays().add(mMyLocationOverlay);

        controller.setZoom(16);
        int selected = mMyLocationOverlay.find(car_id);
        if (selected >= 0)
            controller.setCenter(mMyLocationOverlay.getItem(selected).getPoint());
        updateTrack();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.map) {
            int current = mMyLocationOverlay.find(car_id);
            if (current < 0)
                return true;
            mMyLocationOverlay.setFocusedItem(current);
            MyOverlayItem i = mMyLocationOverlay.getItem(current);
            mMapView.getController().setCenter(i.getPoint());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void updateTrack() {
        if (trackTask != null)
            return;

        boolean engine = preferences.getBoolean(Names.Car.INPUT3 + car_id, false) || preferences.getBoolean(Names.Car.ZONE_IGNITION + car_id, false);
        boolean az = preferences.getBoolean(Names.Car.AZ + car_id, false);
        if (!engine || az) {
            mTrackOverlay.clear();
            return;
        }

        trackTask = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                JsonArray list = res.get("tracks").asArray();
                if (list.size() > 0) {
                    JsonObject v = list.get(list.size() - 1).asObject();
                    mTrackOverlay.clear();
                    mTrackOverlay.add(v.get("track").asString());
                    mMapView.invalidate();
                }
                trackTask = null;
            }

            @Override
            void error() {
                trackTask = null;
            }
        };
        long end = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
        trackTask.execute(URL_TRACKS, preferences.getString(Names.Car.CAR_KEY + car_id, ""), end - 86400000, end);

    }

    boolean updateItem(MyOverlayItem item) {
        double lat = preferences.getFloat(Names.Car.LAT + item.getUid(), 0);
        double lng = preferences.getFloat(Names.Car.LNG + item.getUid(), 0);
        item.zone = null;
        if ((lat == 0) || (lng == 0)) {
            String zone = preferences.getString(Names.Car.GSM_ZONE + item.getUid(), "");
            if (zone.equals(""))
                return false;
            String points[] = zone.split("_");
            double min_lat = 180;
            double max_lat = -180;
            double min_lon = 180;
            double max_lon = -180;
            for (String point : points) {
                try {
                    String[] p = point.split(",");
                    double p_lat = Double.parseDouble(p[0]);
                    double p_lon = Double.parseDouble(p[1]);
                    if (p_lat > max_lat)
                        max_lat = p_lat;
                    if (p_lat < min_lat)
                        min_lat = p_lat;
                    if (p_lon > max_lon)
                        max_lon = p_lon;
                    if (p_lon < min_lon)
                        min_lon = p_lon;
                } catch (Exception ex) {
                    // ignore
                }
            }
            lat = ((min_lat + max_lat) / 2);
            lng = ((min_lon + max_lon) / 2);
            item.zone = zone;
        }
        String title = "";
        String speed = "";
        if (preferences.getBoolean(Names.Car.POINTER + car_id, false)) {
            long last_stand = preferences.getLong(Names.Car.EVENT_TIME + item.getUid(), 0);
            if (last_stand > 0)
                title += df.format(last_stand) + " " + tf.format(last_stand);
        } else {
            long last_stand = preferences.getLong(Names.Car.LAST_STAND + item.getUid(), 0);
            if (last_stand < 0) {
                double s = preferences.getFloat(Names.Car.SPEED + item.getUid(), 0);
                if (s > 0)
                    speed = String.format(getString(R.string.speed), s);
                last_stand = preferences.getLong(Names.Car.EVENT_TIME + item.getUid(), 0);
            }
            if (last_stand > 0) {
                LocalDateTime stand = new LocalDateTime(last_stand);
                LocalDateTime now = new LocalDateTime();
                if (stand.toLocalDate().equals(now.toLocalDate())) {
                    title += tf.format(last_stand);
                } else {
                    title += df.format(last_stand) + " " + tf.format(last_stand);
                }
            }
        }

        String data = Math.round(lat * 10000) / 10000. + "," + Math.round(lng * 10000) / 10000.;
        String address = Address.getAddress(getBaseContext(), lat, lng);
        if (address != null) {
            String[] parts = address.split(", ");
            if (parts.length >= 3) {
                address = parts[0] + ", " + parts[1] + "\n" + parts[2];
                for (int n = 3; n < parts.length; n++)
                    address += ", " + parts[n];
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
        if (!speed.equals(""))
            data += "\n" + speed;
        for (Cars.Car car : cars) {
            if (!car.id.equals(item.getUid()))
                continue;
            if (cars.length > 1)
                title += " " + car.name;
            item.set(title, data, new GeoPoint(lat, lng), preferences.getInt(Names.Car.COURSE + car.id, 0));
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
                    car_id = cars[i].id;
                    int current = mMyLocationOverlay.find(car_id);
                    if (current < 0)
                        return true;
                    mMyLocationOverlay.setFocusedItem(current);
                    MyOverlayItem item = mMyLocationOverlay.getItem(current);
                    mMapView.getController().setCenter(item.getPoint());
                    trackTask = null;
                    mTrackOverlay.clear();
                    updateTrack();
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

        int mBearing;
        String zone;

        public MyOverlayItem(String aUid) {
            super(aUid, null, null, null);
        }

        void set(String title, String snippet, GeoPoint point, int bearing) {
            mTitle = title;
            mSnippet = snippet;
            mGeoPoint = point;
            mBearing = bearing;
        }
    }

    class LocationOverlay extends ItemizedOverlayWithFocus<MyOverlayItem> {

        public final int mMarkerFocusedBackgroundColor = Color.rgb(255, 255, 200);
        protected final Bitmap mDirectionArrowBitmap;
        protected final double mDirectionArrowCenterX;
        protected final double mDirectionArrowCenterY;
        protected final SafePaint mPaint = new SafePaint();
        private final float[] mMatrixValues = new float[9];
        private final Matrix mMatrix = new Matrix();
        private final Point mFocusedScreenCoords = new Point();
        private final Rect mRect = new Rect();
        public int DESCRIPTION_BOX_PADDING = 3;
        public int DESCRIPTION_BOX_CORNERWIDTH = 3;

        public int DESCRIPTION_LINE_HEIGHT = 12;
        public int DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT = 2;

        protected int DESCRIPTION_MAXWIDTH = 200;
        MapView mapView;
        DisplayMetrics displayMetrics;

        public LocationOverlay(MapView map, Context context) {
            super(new Vector<MyOverlayItem>(),
                    mMapView.getResourceProxy().getDrawable(ResourceProxy.bitmap.marker_default),
                    null, NOT_SET,
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
            mapView = map;

            displayMetrics = context.getResources().getDisplayMetrics();
            float density = displayMetrics.density;

            DESCRIPTION_BOX_PADDING = (int) (6 * density);
            DESCRIPTION_BOX_CORNERWIDTH = (int) (3 * density);

            DESCRIPTION_LINE_HEIGHT = (int) (12 * density);
            DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT = (int) (2 * density);

            mDirectionArrowBitmap = mResourceProxy.getBitmap(ResourceProxy.bitmap.direction_arrow);

            mDirectionArrowCenterX = mDirectionArrowBitmap.getWidth() / 2.0 - 0.5;
            mDirectionArrowCenterY = mDirectionArrowBitmap.getHeight() / 2.0 - 0.5;

            int snippet_pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    12, displayMetrics);
            int title_pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    14, displayMetrics);
            mDescriptionPaint.setTextSize(snippet_pixel);
            mTitlePaint.setTextSize(title_pixel);

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


        @Override
        protected void drawFocusedItem(Canvas c, MapView osmv) {
            final MyOverlayItem focusedItem = super.mItemList.get(this.mFocusedItemIndex);
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
            final float[] widths = new float[itemDescription.length()];
            this.mDescriptionPaint.getTextWidths(itemDescription, widths);

            final StringBuilder sb = new StringBuilder();
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

		/*
		 * The title also needs to be taken into consideration for the width calculation.
		 */
            final int titleWidth = (int) this.mTitlePaint.measureText(itemTitle);

            Rect bounds = new Rect();
            mTitlePaint.getTextBounds("a", 0, 1, bounds);
            final int titleLineHeight = bounds.height() + DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT * 2;
            mDescriptionPaint.getTextBounds("a", 0, 1, bounds);
            final int snippetLineHeight = bounds.height() + DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT * 2;

            int totalHeight = lines.length * snippetLineHeight;
            if (!itemTitle.equals(""))
                totalHeight += DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT + titleLineHeight;

            maxWidth = Math.max(maxWidth, titleWidth);
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
            c.drawText(itemTitle, descLeft, descTextLineBottom - DESCRIPTION_TITLE_EXTRA_LINE_HEIGHT,
                    this.mTitlePaint);
		/*
		 * Finally draw the marker base. This is done in the end to make it look better.
		 */
            Overlay.drawAt(c, markerFocusedBase, mFocusedScreenCoords.x, mFocusedScreenCoords.y, false, osmv.getMapOrientation());
        }


        protected void onDrawItem(final ISafeCanvas canvas, final MyOverlayItem item, final Point curScreenCoords, final float aMapOrientation) {

            final MapView.Projection pj = mapView.getProjection();
            if (item.zone != null) {

                String points[] = item.zone.split("_");

                ArrayList<Point> mPoints = new ArrayList<Point>();

                Point mapCoords = new Point();
                for (String point : points) {
                    try {
                        String[] p = point.split(",");
                        double p_lat = Double.parseDouble(p[0]);
                        double p_lon = Double.parseDouble(p[1]);
                        pj.toMapPixelsProjected((int) (p_lat * 1000000), (int) (p_lon * 1000000), mapCoords);
                        mPoints.add(mapCoords);
                    } catch (Exception ex) {
                        // ignore
                    }
                }

                Point screenPoint0 = null; // points on screen
                Point screenPoint1;
                Point projectedPoint0; // points from the points list
                Point projectedPoint1;

                // clipping rectangle in the intermediate projection, to avoid performing projection.
                final Rect clipBounds = pj.fromPixelsToProjected(pj.getScreenRect());
                int size = mPoints.size();

                SafeTranslatedPath mPath = new SafeTranslatedPath();
                Rect mLineBounds = new Rect();

                Point mTempPoint1 = new Point();
                Point mTempPoint2 = new Point();

                mPath.rewind();
                projectedPoint0 = mPoints.get(size - 1);
                mLineBounds.set(projectedPoint0.x, projectedPoint0.y, projectedPoint0.x, projectedPoint0.y);

                for (int i = size - 2; i >= 0; i--) {
                    // compute next points
                    projectedPoint1 = mPoints.get(i);
                    mLineBounds.union(projectedPoint1.x, projectedPoint1.y);

/*
                    if (!Rect.intersects(clipBounds, mLineBounds)) {
                        // skip this line, move to next point
                        projectedPoint0 = projectedPoint1;
                        screenPoint0 = null;
                        continue;
                    }
*/

                    // the starting point may be not calculated, because previous segment was out of clip
                    // bounds
                    if (screenPoint0 == null) {
                        screenPoint0 = pj.toMapPixelsTranslated(projectedPoint0, mTempPoint1);
                        mPath.moveTo(screenPoint0.x, screenPoint0.y);
                    }

                    screenPoint1 = pj.toMapPixelsTranslated(projectedPoint1, mTempPoint2);

                    // skip this point, too close to previous point
                    if (Math.abs(screenPoint1.x - screenPoint0.x) + Math.abs(screenPoint1.y - screenPoint0.y) <= 1) {
                        continue;
                    }

                    mPath.lineTo(screenPoint1.x, screenPoint1.y);

                    // update starting point to next position
                    projectedPoint0 = projectedPoint1;
                    screenPoint0.x = screenPoint1.x;
                    screenPoint0.y = screenPoint1.y;
                    mLineBounds.set(projectedPoint0.x, projectedPoint0.y, projectedPoint0.x, projectedPoint0.y);
                }

                SafePaint mPaint = new SafePaint();
                mPaint.setColor(Color.rgb(256, 0, 0));
                mPaint.setStrokeWidth(2);
                mPaint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(mPath, mPaint);
                return;
            }

            Point mapCoords = new Point();
            TileSystem.LatLongToPixelXY(item.getPoint().getLatitude(), item.getPoint().getLongitude(),
                    MapViewConstants.MAXIMUM_ZOOMLEVEL, mapCoords);
            final int worldSize_2 = TileSystem.MapSize(MapViewConstants.MAXIMUM_ZOOMLEVEL) / 2;
            mapCoords.offset(-worldSize_2, -worldSize_2);

            final int zoomDiff = MapViewConstants.MAXIMUM_ZOOMLEVEL - pj.getZoomLevel();

            canvas.getMatrix(mMatrix);
            mMatrix.getValues(mMatrixValues);

            float scaleX = (float) Math.sqrt(mMatrixValues[Matrix.MSCALE_X]
                    * mMatrixValues[Matrix.MSCALE_X] + mMatrixValues[Matrix.MSKEW_Y]
                    * mMatrixValues[Matrix.MSKEW_Y]);
            float scaleY = (float) Math.sqrt(mMatrixValues[Matrix.MSCALE_Y]
                    * mMatrixValues[Matrix.MSCALE_Y] + mMatrixValues[Matrix.MSKEW_X]
                    * mMatrixValues[Matrix.MSKEW_X]);
            final double x = mapCoords.x >> zoomDiff;
            final double y = mapCoords.y >> zoomDiff;

            canvas.save();
            canvas.rotate(item.mBearing, x, y);
            canvas.scale(1 / scaleX, 1 / scaleY, x, y);
            canvas.drawBitmap(mDirectionArrowBitmap, x - mDirectionArrowCenterX, y
                    - mDirectionArrowCenterY, mPaint);
            canvas.restore();
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
