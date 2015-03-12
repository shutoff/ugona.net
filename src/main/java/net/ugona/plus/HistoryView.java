package net.ugona.plus;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;

import com.androidplot.ui.YLayoutStyle;
import com.androidplot.ui.YPositionMetric;
import com.androidplot.util.FontUtils;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.AxisValueLabelFormatter;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XValueMarker;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import org.joda.time.LocalDate;

import java.io.Serializable;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Set;
import java.util.Vector;

public class HistoryView extends com.androidplot.xy.XYPlot implements View.OnTouchListener {

    final static long LOAD_INTERVAL = 5 * 86400 * 1000;
    // Definition of the touch states
    static final int NONE = 0;
    int mode = NONE;
    static final int ONE_FINGER_DRAG = 1;
    static final int TWO_FINGERS_DRAG = 2;
    String car_id;
    String type;
    java.text.DateFormat time_format;
    java.text.DateFormat date_format;
    HistoryViewListener mListener;
    long minX;
    long maxX;
    long screenMinX;
    long screenMaxX;
    int v10;
    PointF firstFinger;
    float lastScrolling;
    float distBetweenFingers;
    float lastZooming;
    boolean zoomChanged;
    Paint markerPaint;
    Paint markerTextPaint;
    LocalDate current;
    CarConfig config;
    LineAndPointFormatter formatter;
    Paint mainLabelPaint;

    public HistoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        time_format = android.text.format.DateFormat.getTimeFormat(context);
        date_format = android.text.format.DateFormat.getMediumDateFormat(context);
        getGraphWidget().getRangeLabelPaint().setTextSize(PixelUtils.dpToPix(12));
        getGraphWidget().getRangeOriginLabelPaint().setTextSize(PixelUtils.dpToPix(12));
        getGraphWidget().getDomainLabelPaint().setTextSize(PixelUtils.dpToPix(12));
        getGraphWidget().getDomainOriginLabelPaint().setTextSize(PixelUtils.dpToPix(12));
        setDomainValueFormat(new DateFormat());
        setRangeValueFormat(new ValueFormat());
        getLegendWidget().setVisible(false);
        getGraphWidget().setMarginBottom(PixelUtils.dpToPix(16));
        getGraphWidget().setMarginLeft(PixelUtils.dpToPix(14));
        getGraphWidget().addDomainAxisValueLabelRegion(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, new AxisValueLabelFormatter() {
            @Override
            public boolean isMain(double value) {
                Date d = new Date((long) value * 1000);
                return d.getHours() == 0;
            }

            @Override
            public void paint(Paint p, double value) {
                if (isMain(value))
                    p.setTextSize(mainLabelPaint.getTextSize());
            }
        });

        Typeface typeface = Font.getFont(context, "Exo2-Regular");
        Typeface typeface_bold = Font.getFont(context, "Exo2-Bold");

        markerPaint = new Paint();
        markerPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setColor(Color.rgb(192, 0, 0));
        markerPaint.setStrokeWidth(PixelUtils.dpToPix(0.5f));
        markerPaint.setTextSize(PixelUtils.dpToPix(15));
        markerPaint.setTypeface(typeface);

        markerTextPaint = new Paint();
        markerTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        markerTextPaint.setColor(Color.rgb(192, 0, 0));
        markerTextPaint.setStrokeWidth(PixelUtils.dpToPix(1f));
        markerTextPaint.setTextSize(PixelUtils.dpToPix(15));
        markerTextPaint.setStyle(Paint.Style.STROKE);
        markerTextPaint.setTypeface(typeface);

        TypedArray array = context.getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.colorBackground,
                android.R.attr.textColorPrimary,
        });
        int backgroundColor = array.getColor(0, 0xFF00FF);
        array.recycle();

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.FILL);
        setBackgroundPaint(backgroundPaint);

        XYGraphWidget widget = getGraphWidget();
        widget.setBackgroundPaint(backgroundPaint);
        widget.setGridBackgroundPaint(backgroundPaint);

        Paint gridPaint = new Paint();
        gridPaint.setColor(context.getResources().getColor(android.R.color.darker_gray));
        gridPaint.setAntiAlias(true);
        gridPaint.setStyle(Paint.Style.STROKE);

        Paint rangeLabelPaint = new Paint();
        rangeLabelPaint.setColor(context.getResources().getColor(R.color.text_dark));
        rangeLabelPaint.setAntiAlias(true);
        rangeLabelPaint.setStyle(Paint.Style.STROKE);
        rangeLabelPaint.setTextSize(PixelUtils.dpToPix(13));
        rangeLabelPaint.setTextAlign(Paint.Align.RIGHT);
        rangeLabelPaint.setTypeface(typeface);

        Paint domainLabelPaint = new Paint();
        domainLabelPaint.setColor(context.getResources().getColor(R.color.text_dark));
        domainLabelPaint.setAntiAlias(true);
        domainLabelPaint.setStyle(Paint.Style.STROKE);
        domainLabelPaint.setTextSize(PixelUtils.dpToPix(12));
        domainLabelPaint.setTextAlign(Paint.Align.CENTER);
        domainLabelPaint.setTypeface(typeface);

        mainLabelPaint = new Paint();
        mainLabelPaint.setColor(context.getResources().getColor(R.color.text_dark));
        mainLabelPaint.setAntiAlias(true);
        mainLabelPaint.setStyle(Paint.Style.STROKE);
        mainLabelPaint.setTextSize(PixelUtils.dpToPix(14));
        mainLabelPaint.setTextAlign(Paint.Align.CENTER);
        mainLabelPaint.setTypeface(typeface_bold);


        widget.setRangeGridLinePaint(gridPaint);
        widget.setDomainGridLinePaint(gridPaint);
        widget.setRangeSubGridLinePaint(gridPaint);
        widget.setDomainOriginLinePaint(gridPaint);
        widget.setRangeOriginLinePaint(gridPaint);
        widget.setDomainOriginLinePaint(domainLabelPaint);
        widget.setRangeOriginLinePaint(gridPaint);
        widget.setDomainOriginLabelPaint(domainLabelPaint);
        widget.setRangeOriginLabelPaint(rangeLabelPaint);
        widget.setDomainLabelPaint(domainLabelPaint);
        widget.setRangeLabelPaint(rangeLabelPaint);


        int color = getResources().getColor(R.color.main);
//        final int fill_color = Color.argb(192, Color.red(color), Color.green(color), Color.blue(color));
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 2;
        final int fill_color = Color.HSVToColor(hsv);

        formatter = new LineAndPointFormatter(color, null, fill_color, null) {
            @Override
            public void fillPath(Canvas canvas, Path path) {
                Region clip = new Region(0, 0, getWidth(), getHeight());
                Region region = new Region();
                region.setPath(path, clip);
                Rect bounds = region.getBounds();
                Paint fillPaint = formatter.getFillPaint();
                fillPaint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.left, bounds.bottom, fill_color, Color.WHITE, LinearGradient.TileMode.MIRROR));
                super.fillPath(canvas, path);
            }
        };

        setOnTouchListener(this);
    }

    public void init(Context context, String id, String t, LocalDate c) {
        car_id = id;
        type = t;
        current = c;
        config = CarConfig.get(context, id);

        loadData();
    }

    void loadData() {
        Set<XYSeries> seriesSet = getSeriesSet();
        for (XYSeries series : seriesSet) {
            removeSeries(series);
        }
        new FetchTask();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // Start gesture
                firstFinger = new PointF(event.getX(), event.getY());
                mode = ONE_FINGER_DRAG;
                zoomChanged = false;
                try {
                    double current = getGraphWidget().getXVal(event.getX());
                    XYSeries series = getSeriesSet().iterator().next();
                    int i;
                    for (i = 0; i < series.size(); i++) {
                        if (series.getX(i).longValue() > current)
                            break;
                    }
                    if ((i > 0) && (i < series.size())) {
                        long t1 = series.getX(i - 1).longValue();
                        long t2 = series.getX(i).longValue();
                        if (current - t1 < t2 - current)
                            i--;
                        Date d = new Date(series.getX(i).longValue() * 1000);
                        String text = time_format.format(d);
                        text += " ";
                        text += String.format("%.2f", series.getY(i));
                        text += getUnits();
                        float yPos = getGraphWidget().getYPix(series.getY(i).floatValue());
                        addMarker(new XValueMarker(series.getX(i), text, new YPositionMetric(yPos, YLayoutStyle.ABSOLUTE_FROM_TOP), markerPaint, markerTextPaint) {
                            @Override
                            public void drawText(Canvas canvas, String text, float xPix, RectF paddedGridRect) {
                                float yPix = getTextPosition().getPixelValue(
                                        paddedGridRect.height());
                                yPix += paddedGridRect.top;
                                Paint textPaint = getGraphWidget().getDomainLabelPaint();
                                canvas.drawCircle(xPix, yPix, PixelUtils.dpToPix(4), getBackgroundPaint());
                                canvas.drawCircle(xPix, yPix, PixelUtils.dpToPix(4), textPaint);
                                RectF textRect = new RectF(FontUtils.getStringDimensions(text, getTextPaint()));
                                float margin = PixelUtils.dpToPix(2);
                                float text_h = textRect.height() + margin * 2;
                                float text_w = textRect.width() + margin * 2;
                                boolean bUp = true;
                                float marker_y = PixelUtils.dpToPix(10);
                                float text_y = yPix - text_h - marker_y;
                                if (text_y < paddedGridRect.top + margin) {
                                    bUp = false;
                                    text_y = (int) yPix + marker_y;
                                }
                                float text_x = xPix - text_w / 2;
                                if (text_x < paddedGridRect.left + 2)
                                    text_x = paddedGridRect.left + 2;
                                if (text_x + text_w > paddedGridRect.right - 2)
                                    text_x = paddedGridRect.right - text_w - 2;
                                textRect = new RectF(text_x, text_y, text_x + text_w, text_y + text_h);
                                float radius = PixelUtils.dpToPix(2);
                                Paint p = new Paint();
                                p.setColor(Color.WHITE);
                                p.setShadowLayer(10, 5, 5, Color.BLACK);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                                    setLayerType(LAYER_TYPE_SOFTWARE, p);
                                Path pt = new Path();
                                pt.addRoundRect(textRect, radius, radius, Path.Direction.CW);
                                canvas.drawPath(pt, p);
                                canvas.drawText(text, textRect.centerX(), textRect.centerY() + margin, textPaint);
                            }
                        });
                        postInvalidate();
                    }
                } catch (Exception ex) {
                    // ignore
                }
                break;

            case MotionEvent.ACTION_UP:
                if (zoomChanged)
                    setDomainSteps();
                removeXMarkers();
                postInvalidate();
                break;

            case MotionEvent.ACTION_POINTER_DOWN: // second finger
                if (event.getPointerCount() > 1) {
                    distBetweenFingers = spacing(event);
                    // the distance check is done to avoid false alarms
                    if (distBetweenFingers > 5f) {
                        mode = TWO_FINGERS_DRAG;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == ONE_FINGER_DRAG) {
                    PointF oldFirstFinger = firstFinger;
                    firstFinger = new PointF(event.getX(), event.getY());
                    lastScrolling = oldFirstFinger.x - firstFinger.x;
                    scroll(lastScrolling);
                } else if ((mode == TWO_FINGERS_DRAG) && (event.getPointerCount() > 1)) {
                    float oldDist = distBetweenFingers;
                    distBetweenFingers = spacing(event);
                    lastZooming = oldDist / distBetweenFingers;
                    zoom(lastZooming);
                }
                break;
        }
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }


    private void zoom(float scale) {
        double cur_scale = (screenMaxX - screenMinX) / 86400.;
        double new_scale = cur_scale * scale;
        if (new_scale > 3)
            new_scale = 3;
        if (new_scale < 0.05)
            new_scale = 0.05;
        if (new_scale > (maxX - minX) / 86400.)
            new_scale = (maxX - minX) / 86400.;
        if (new_scale == cur_scale)
            return;
        new_scale = new_scale * 43200;
        long centerX = (screenMinX + screenMaxX) / 2;
        screenMinX = centerX - (long) new_scale;
        screenMaxX = centerX + (long) new_scale;
        if (screenMinX < minX) {
            long d = minX - screenMinX;
            screenMinX += d;
            screenMaxX += d;
        }
        if (screenMaxX > maxX) {
            long d = screenMaxX - maxX;
            screenMaxX -= d;
            screenMinX -= d;
        }
        setDomainBoundaries(screenMinX, screenMaxX, BoundaryMode.FIXED);
        postInvalidate();
        zoomChanged = true;
    }

    private void scroll(float pan) {
        long delta = (long) ((screenMaxX - screenMinX) / getWidth() * pan);
        if (screenMaxX + delta > maxX)
            delta = maxX - screenMaxX;
        if (screenMinX + delta < minX)
            delta = minX - screenMinX;
        if (delta == 0)
            return;
        screenMinX += delta;
        screenMaxX += delta;
        setDomainBoundaries(screenMinX, screenMaxX, BoundaryMode.FIXED);
        postInvalidate();
    }

    String getUnits() {
        if (type.equals("voltage") || type.equals("reserved"))
            return "V";
        if (type.substring(0, 1).equals("t"))
            return "\u00B0C";
        return "";
    }

    void setDomainSteps() {
        int hours = (int) Math.ceil((screenMaxX - screenMinX) / 3600. / 5);
        if (hours < 1)
            hours = 1;
        if (hours > 24)
            hours = 24;
        switch (hours) {
            case 5:
                hours = 6;
                break;
            case 7:
                hours = 8;
                break;
            case 9:
            case 10:
            case 11:
                hours = 12;
                break;
            case 13:
            case 14:
                hours = 12;
                break;
        }
        if ((hours > 12) && (hours < 24))
            hours = 24;

        long domain_step = hours * 3600;
        setDomainStep(XYStepMode.INCREMENT_BY_VAL, domain_step);
        Date d = new Date((long) Math.ceil(screenMinX / domain_step) * domain_step * 1000);
        d.setHours(0);
        setUserDomainOrigin(d.getTime() / 1000);
    }

    static interface HistoryViewListener {
        void dataReady();

        void noData();

        void errorLoading();
    }

    static class Data implements Serializable {
        long t;
        double v;
    }

    static class HistoryParams implements Serializable {
        String skey;
        long begin;
        long end;
        String type;
    }

    class FetchTask extends HttpTask {

        String m_id;
        String m_type;
        LocalDate m_date;
        double delta;

        FetchTask() {
            m_id = car_id;
            m_type = type;
            m_date = current;

            HistoryParams params = new HistoryParams();
            params.skey = config.getKey();
            params.type = type;
            params.end = current.toDate().getTime() + 86400000;
            params.begin = params.end - LOAD_INTERVAL;

            execute("/history", params);
        }

        @Override
        void result(JsonObject res) throws ParseException {

            if (!m_id.equals(car_id) || !m_type.equals(type) || !m_date.equals(current))
                return;

            final JsonArray data_array = res.get("res").asArray();
            final Vector<Data> data = new Vector<Data>();
            for (int i = 0; i < data_array.size(); i++) {
                Data d = new Data();
                Config.update(d, data_array.get(i).asObject());
                d.t /= 1000;
                d.v += delta;
                data.add(d);
            }
            if (data.size() == 0) {
                if (mListener != null)
                    mListener.noData();
                return;
            }

            XYSeries series = new XYSeries() {
                @Override
                public int size() {
                    return data.size();
                }

                @Override
                public Number getX(int i) {
                    return data.get(i).t;
                }

                @Override
                public Number getY(int i) {
                    return data.get(i).v;
                }

                @Override
                public String getTitle() {
                    return "";
                }
            };
            if (series.size() == 0)
                return;
            maxX = series.getX(series.size() - 1).longValue();
            minX = series.getX(0).longValue();

            screenMaxX = maxX;
            screenMinX = minX;
            if (screenMinX < screenMaxX - 86400)
                screenMinX = screenMaxX - 86400;

            double min_value = data.get(data.size() - 1).v;
            double max_value = min_value;
            for (Data d : data) {
                double v = d.v;
                if (v < min_value)
                    min_value = v;
                if (v > max_value)
                    max_value = v;
            }
            double dv = max_value - min_value;
            if (dv < 0.1)
                dv = 0.1;
            double pmv = min_value;
            min_value -= dv / 4;
            if ((min_value < 0) && (pmv >= 0))
                min_value = 0;
            max_value += dv / 4;

            addSeries(series, formatter);

            setDomainSteps();
            setDomainBoundaries(screenMinX, screenMaxX, BoundaryMode.FIXED);

            double delta_val = (max_value - min_value) / 2.2;
            if (delta_val < 0.01)
                delta_val = 0.01;
            v10 = (int) Math.floor(Math.log10(delta_val));
            double val_step = Math.pow(10, (double) v10);
            setRangeStep(XYStepMode.INCREMENT_BY_VAL, val_step);
            setUserRangeOrigin(Math.ceil(min_value / val_step) * val_step);

            setRangeBoundaries(min_value, max_value, BoundaryMode.FIXED);

            redraw();
            if (mListener != null)
                mListener.dataReady();
        }

        @Override
        void error() {
            if (!m_id.equals(car_id) || !m_type.equals(type) || !m_date.equals(current))
                return;
            mListener.errorLoading();
        }
    }

    class DateFormat extends Format {

        @Override
        public StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
            double v = (Double) object * 1000;
            Date d = new Date((long) v);
            if (d.getHours() == 0) {
                buffer.append(String.format("%02d.%02d", d.getDate(), d.getMonth() + 1));
            } else {
                buffer.append(time_format.format(d));
            }
            return buffer;
        }

        @Override
        public Object parseObject(String string, ParsePosition position) {
            return null;
        }
    }

    class ValueFormat extends Format {

        @Override
        public StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
            double v = (Double) object;
            if (v10 >= 0) {
                buffer.append(String.format("%,d", (int) v));
            } else {
                String format = "%1$,." + (-v10) + "f";
                buffer.append(String.format(format, v));
            }
            buffer.append(getUnits());
            return buffer;
        }

        @Override
        public Object parseObject(String string, ParsePosition position) {
            return null;
        }
    }
}
