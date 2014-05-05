package net.ugona.plus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.SafeDrawOverlay;
import org.osmdroid.views.safecanvas.ISafeCanvas;
import org.osmdroid.views.safecanvas.SafePaint;
import org.osmdroid.views.safecanvas.SafeTranslatedPath;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZoneEdit extends MapActivity {

    static Pattern zonePat = Pattern.compile("[A-Za-z0-9]+");
    EditText etName;
    CheckBox chkSms;
    SettingActivity.Zone zone;
    boolean clear_zone;
    boolean confirm;

    @Override
    int menuId() {
        return R.menu.zone;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setResult(RESULT_CANCELED);
        byte[] data;
        if (savedInstanceState != null) {
            data = savedInstanceState.getByteArray(Names.TRACK);
        } else {
            data = getIntent().getByteArrayExtra(Names.TRACK);
        }
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInput in = new ObjectInputStream(bis);
            zone = (SettingActivity.Zone) in.readObject();
        } catch (Exception ex) {
            // ignore
        }

        super.onCreate(savedInstanceState);
        findViewById(R.id.zone_info).setVisibility(View.VISIBLE);
        etName = (EditText) findViewById(R.id.name_edit);
        chkSms = (CheckBox) findViewById(R.id.sms_check);
        etName.setText(zone.name);
        etName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b)
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            }
        });
        chkSms.setChecked(zone.sms);
        InputFilter[] filters = {
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        String s = source.subSequence(start, end).toString();
                        Matcher matcher = zonePat.matcher(s);
                        if (matcher.matches())
                            return null;
                        Toast toast = Toast.makeText(ZoneEdit.this, R.string.zone_bad_char, Toast.LENGTH_LONG);
                        toast.show();
                        return "";
                    }
                }
        };
        etName.setFilters(filters);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        setZone();
        super.onSaveInstanceState(outState);
        byte[] data = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(zone);
            data = bos.toByteArray();
            out.close();
            bos.close();
        } catch (Exception ex) {
            // ignore
        }
        if (data != null)
            outState.putByteArray(Names.TRACK, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete) {
            clear_zone = true;
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    void initMap(IMapController controller) {
        GeoPoint center = new GeoPoint((zone.lat1 + zone.lat2) / 2, (zone.lng1 + zone.lng2) / 2);
        controller.setCenter(center);
        controller.setZoom(16);
        mMapView.fitToRect(new GeoPoint(zone.lat1, zone.lng1), new GeoPoint(zone.lat2, zone.lng2), 0.5);
        mMapView.getOverlays().add(new RectOverlay(this));
    }

    @Override
    public void finish() {
        if (clear_zone) {
            if (!confirm && !zone._name.equals("")) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.zone_remove)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                confirm = true;
                                finish();
                            }
                        })
                        .create();
                dialog.show();
                return;
            }
            zone.name = "";
        } else {
            setZone();
            if (zone.name.equals("")) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.zone_name)
                        .setMessage(R.string.zone_name_msg)
                        .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                etName.requestFocus();
                            }
                        })
                        .create();
                dialog.show();
                return;
            }
        }
        Intent i = getIntent();
        try {
            byte[] data = null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(zone);
            data = bos.toByteArray();
            out.close();
            bos.close();
            i.putExtra(Names.TRACK, data);
        } catch (Exception ex) {
            // ignore
        }
        setResult(RESULT_OK, i);
        super.finish();
    }

    void setZone() {
        zone.name = etName.getText().toString();
        zone.sms = chkSms.isChecked();
    }

    class RectOverlay extends SafeDrawOverlay {

        SafePaint mPaint = new SafePaint();
        SafeTranslatedPath mPath = new SafeTranslatedPath();
        int width;

        float x1;
        float y1;
        float x2;
        float y2;
        int x_state;
        int y_state;

        public RectOverlay(Context ctx) {
            super(ctx);
            mPaint.setColor(Color.rgb(255, 0, 0));
            mPaint.setStrokeWidth(2);
            width = (int) (getResources().getDisplayMetrics().density * 6);
        }

        @Override
        protected void drawSafe(ISafeCanvas c, MapView mapView, boolean shadow) {
            if (shadow)
                return;
            final org.osmdroid.views.MapView.Projection pj = mapView.getProjection();
            Rect screenRect = pj.getScreenRect();
            Point p0 = new Point();
            pj.toPixels(new GeoPoint(zone.lat1, zone.lng1), p0);
            Point p1 = new Point();
            pj.toPixels(new GeoPoint(zone.lat2, zone.lng2), p1);
            int x1 = p0.x - screenRect.left;
            int y1 = p0.y - screenRect.top;
            int x2 = p1.x - screenRect.left;
            int y2 = p1.y - screenRect.top;
            mPath.rewind();
            mPath.moveTo(x1, y1);
            mPath.lineTo(x2, y1);
            mPath.lineTo(x2, y2);
            mPath.lineTo(x1, y2);
            mPath.close();
            mPaint.setStrokeWidth(4);
            mPaint.setAlpha(20);
            mPaint.setStyle(Paint.Style.FILL);
            c.drawPath(mPath, mPaint);
            mPaint.setAlpha(80);
            mPaint.setStyle(Paint.Style.STROKE);
            c.drawPath(mPath, mPaint);
        }

        @Override
        public boolean onDown(MotionEvent e, MapView mapView) {
            float x = e.getX();
            float y = e.getY();
            final org.osmdroid.views.MapView.Projection pj = mapView.getProjection();
            double min_lat = zone.lat1;
            double max_lat = zone.lat2;
            if (max_lat < min_lat) {
                min_lat = zone.lat2;
                max_lat = zone.lat1;
            }
            double min_lng = zone.lng1;
            double max_lng = zone.lng2;
            if (max_lng < min_lng) {
                min_lng = zone.lng2;
                max_lng = zone.lng1;
            }
            Rect screenRect = pj.getScreenRect();
            Point p0 = new Point();
            pj.toPixels(new GeoPoint(min_lat, min_lng), p0);
            Point p1 = new Point();
            pj.toPixels(new GeoPoint(max_lat, max_lng), p1);
            x1 = p0.x - screenRect.left;
            y1 = p1.y - screenRect.top;
            x2 = p1.x - screenRect.left;
            y2 = p0.y - screenRect.top;
            if ((x < x1 - width) || (x >= x2 + width)) {
                x_state = 0;
            } else if (x < x1 + width) {
                x_state = 1;
            } else if (x > x2 - width) {
                x_state = 2;
            } else {
                x_state = 3;
            }
            if ((y < y1 - width) || (y >= y2 + width)) {
                y_state = 0;
            } else if (y < y1 + width) {
                y_state = 1;
            } else if (y > y2 - width) {
                y_state = 2;
            } else {
                y_state = 3;
            }
            if ((x_state == 0) || (y_state == 0))
                return false;
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent pEvent1, MotionEvent pEvent2, float pDistanceX, float pDistanceY, MapView pMapView) {
            if ((x_state == 0) || (y_state == 0))
                return false;
            final org.osmdroid.views.MapView.Projection pj = pMapView.getProjection();
            if (x_state == 1) {
                x1 -= pDistanceX;
            } else if (x_state == 2) {
                x2 -= pDistanceX;
            } else if (x_state == 3) {
                x1 -= pDistanceX;
                x2 -= pDistanceX;
            }
            if (y_state == 1) {
                y1 -= pDistanceY;
            } else if (y_state == 2) {
                y2 -= pDistanceY;
            } else if (y_state == 3) {
                y1 -= pDistanceY;
                y2 -= pDistanceY;
            }
            IGeoPoint r = pj.fromPixels(x1, y1);
            zone.lat2 = r.getLatitude();
            zone.lng1 = r.getLongitude();
            r = pj.fromPixels(x2, y2);
            zone.lat1 = r.getLatitude();
            zone.lng2 = r.getLongitude();
            mMapView.postInvalidate();
            return true;
        }
    }

}
