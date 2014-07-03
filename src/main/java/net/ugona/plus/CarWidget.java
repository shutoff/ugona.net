package net.ugona.plus;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CarWidget extends AppWidgetProvider {

    static final int STATE_UPDATE = 1;
    static final int STATE_ERROR = 2;
    final static String URL_TRAFFIC = "https://car-online.ugona.net/level?lat=$1&lng=$2";
    final static int[] trafic_pict = {
            R.drawable.p0,
            R.drawable.p1,
            R.drawable.p2,
            R.drawable.p3,
            R.drawable.p4,
            R.drawable.p5,
            R.drawable.p6,
            R.drawable.p7,
            R.drawable.p8,
            R.drawable.p9,
            R.drawable.p10
    };
    static final int id_layout[] = {
            R.layout.widget,
            R.layout.widget_light
    };
    static final int id_layout_22[] = {
            R.layout.widget_22,
            R.layout.widget_light_22
    };
    static final int id_bg[] = {
            R.drawable.widget,
            R.drawable.widget_light
    };
    static final int id_color[] = {
            android.R.color.secondary_text_dark,
            R.color.caldroid_black
    };
    static final int id_lock_layout[] = {
            R.layout.lock_widget,
            R.layout.lock_widget_white
    };
    static final int[] id_gsm_level[] = {
            {
                    R.drawable.gsm_level0,
                    R.drawable.gsm_level1,
                    R.drawable.gsm_level2,
                    R.drawable.gsm_level3,
                    R.drawable.gsm_level4,
                    R.drawable.gsm_level5,
                    R.drawable.gsm_level
            },
            {
                    R.drawable.gsm_level0_white,
                    R.drawable.gsm_level1_white,
                    R.drawable.gsm_level2_white,
                    R.drawable.gsm_level3_white,
                    R.drawable.gsm_level4_white,
                    R.drawable.gsm_level5_white,
                    R.drawable.gsm_level_white
            }
    };
    static CarDrawable drawable;
    static Map<String, Integer> states;
    static Map<Integer, Integer> height_rows;
    static TrafficRequest request;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Intent intent = new Intent(context, WidgetService.class);
        context.startService(intent);
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Intent intent = new Intent(context, WidgetService.class);
        context.startService(intent);
        updateWidgets(context, null, true);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Intent i = new Intent(context, WidgetService.class);
        i.setAction(WidgetService.ACTION_STOP);
        context.startService(i);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (states == null)
                    states = new HashMap<String, Integer>();
                String car_id = intent.getStringExtra(Names.ID);
                if (action.equalsIgnoreCase(FetchService.ACTION_UPDATE)) {
                    states.remove(car_id);
                    updateWidgets(context, car_id, true);
                }
                if (action.equalsIgnoreCase(FetchService.ACTION_NOUPDATE)) {
                    if (states.containsKey(car_id)) {
                        states.remove(car_id);
                        updateWidgets(context, car_id, false);
                    }
                }
                if (action.equalsIgnoreCase(FetchService.ACTION_ERROR)) {
                    if (!states.containsKey(car_id) || (states.get(car_id) != STATE_ERROR)) {
                        states.put(car_id, STATE_ERROR);
                        updateWidgets(context, car_id, false);
                    }
                }
                if (action.equalsIgnoreCase(FetchService.ACTION_START)) {
                    if (!states.containsKey(car_id) || (states.get(car_id) != STATE_UPDATE)) {
                        states.put(car_id, STATE_UPDATE);
                        updateWidgets(context, car_id, false);
                    }
                }
                if (action.equalsIgnoreCase(FetchService.ACTION_UPDATE_FORCE)) {
                    updateWidgets(context, null, false);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    if (action.equals(WidgetService.ACTION_SCREEN))
                        updateLockWidgets(context);
                    if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
                        if ((activeNetwork == null) || !activeNetwork.isConnected())
                            return;
                        updateLockWidgets(context);
                    }
                }
            }
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateWidget(context, appWidgetManager, appWidgetId);
    }

    void updateWidgets(Context context, String car_id, boolean sendUpdate) {
        ComponentName thisAppWidget = new ComponentName(
                context.getPackageName(), getClass().getName());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null) {
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean update = false;
            for (int appWidgetID : ids) {
                if ((car_id == null) || preferences.getString(Names.WIDGET + appWidgetID, "").equals(car_id)) {
                    updateWidget(context, appWidgetManager, appWidgetID);
                    update = true;
                }
            }
            if (sendUpdate && update) {
                Intent i = new Intent(context, WidgetService.class);
                i.setAction(WidgetService.ACTION_UPDATE);
                context.startService(i);
            }
        }
    }

    void updateLockWidgets(Context context) {
        ComponentName thisAppWidget = new ComponentName(
                context.getPackageName(), getClass().getName());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null) {
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            for (int appWidgetID : ids) {
                Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetID);
                if (options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1) == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
                    updateWidget(context, appWidgetManager, appWidgetID);
            }
        }
    }

    int getLayoutHeight(Context context, int maxWidth, int id) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(id, null);
        v.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(widthMeasureSpec, heightMeasureSpec);
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        return v.getHeight();
    }

    int getLayoutId(Context context, int widgetId, int theme) {
        if (isLockScreen(context, widgetId))
            return id_lock_layout[theme];
        boolean progress = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD);
        return progress ? id_layout[theme] : id_layout_22[theme];
    }

    void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetID) {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int rows = preferences.getInt(Names.ROWS + widgetID, 0);
            boolean bigPict = false;
            if (rows == 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    Bundle options = appWidgetManager.getAppWidgetOptions(widgetID);
                    int maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
                    if (maxHeight > 0) {
                        if (height_rows == null)
                            height_rows = new HashMap<Integer, Integer>();
                        if (!height_rows.containsKey(maxHeight)) {
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_HOME);
                            float density = context.getResources().getDisplayMetrics().density;
                            int maxWidth = (int) (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) * density + 0.5);
                            int h = (int) (maxHeight * density - 0.5);
                            int h3 = getLayoutHeight(context, maxWidth, R.layout.widget3);
                            int h4 = getLayoutHeight(context, maxWidth, R.layout.widget4);
                            rows = 3;
                            if (h < h3)
                                rows = 2;
                            if (h > h4) {
                                rows = 5;
                            }
                            height_rows.put(maxHeight, rows);
                        }
                        rows = height_rows.get(maxHeight);
                        bigPict = (rows > 3);
                    }
                } else {
                    rows = 3;
                }
            }

            int theme = preferences.getInt(Names.THEME + widgetID, 0);
            if ((theme < 0) || (theme >= id_layout.length))
                theme = 0;
            RemoteViews widgetView = new RemoteViews(context.getPackageName(), getLayoutId(context, widgetID, theme));

            String id = preferences.getString(Names.WIDGET + widgetID, "");
            String car_id = Preferences.getCar(preferences, id);
            if (!id.equals(car_id)) {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.WIDGET + widgetID, car_id);
                ed.commit();
            }
            int transparency = preferences.getInt(Names.TRANSPARENCY + widgetID, 0);
            widgetView.setInt(R.id.bg, "setAlpha", transparency);
            if (transparency > 0)
                widgetView.setImageViewResource(R.id.bg, id_bg[theme]);

            Intent configIntent = new Intent(context, WidgetService.class);
            configIntent.putExtra(Names.ID, car_id);
            configIntent.setAction(WidgetService.ACTION_START);
            Uri data = Uri.withAppendedPath(Uri.parse("http://widget/update/"), String.valueOf(widgetID));
            configIntent.setData(data);
            PendingIntent pIntent = PendingIntent.getService(context, widgetID, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            widgetView.setOnClickPendingIntent(R.id.update_block, pIntent);

            long last = preferences.getLong(Names.Car.EVENT_TIME + car_id, 0);
            Date now = new Date();
            if (last > now.getTime() - 24 * 60 * 60 * 1000) {
                DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
                widgetView.setTextViewText(R.id.last, tf.format(last));
            } else {
                widgetView.setTextViewText(R.id.last, "??:??");
            }

            int show_count = 1;

            String voltage = preferences.getString(Names.Car.VOLTAGE_MAIN + car_id, "?");
            boolean normal = false;
            try {
                double v = Double.parseDouble(voltage);
                v += preferences.getInt(Names.Car.VOLTAGE_SHIFT + car_id, 0) / 20.;
                voltage = String.format("%.2f", v);
                if (v > 12.2)
                    normal = true;
            } catch (Exception ex) {
                // ignore
            }
            widgetView.setTextViewText(R.id.voltage, voltage + " V");
            if (!normal) {
                boolean az = preferences.getBoolean(Names.Car.AZ + car_id, false);
                boolean ignition = !az && (preferences.getBoolean(Names.Car.INPUT3 + car_id, false) || preferences.getBoolean(Names.Car.ZONE_IGNITION + car_id, false));
                if (az || ignition)
                    normal = true;
            }

            int v_color = normal ? context.getResources().getColor(id_color[theme]) : context.getResources().getColor(R.color.error);
            widgetView.setInt(R.id.voltage, "setTextColor", v_color);

            String temperature = Preferences.getTemperature(preferences, car_id, 1);
            if (temperature == null) {
                widgetView.setViewVisibility(R.id.temperature_block, View.GONE);
            } else {
                widgetView.setTextViewText(R.id.temperature, temperature);
                widgetView.setViewVisibility(R.id.temperature_block, View.VISIBLE);
                show_count++;
            }

            boolean show_balance = false;
            if (show_count < rows)
                show_balance = preferences.getBoolean(Names.Car.SHOW_BALANCE + car_id, true);
            if (show_balance) {
                String b = preferences.getString(Names.Car.BALANCE + car_id, "---.--");
                int balance_limit = 50;
                try {
                    balance_limit = preferences.getInt(Names.Car.LIMIT + car_id, 50);
                } catch (Exception ex) {
                    // ignore
                }
                widgetView.setInt(R.id.balance, "setTextColor", context.getResources().getColor(id_color[theme]));
                try {
                    double value = Double.parseDouble(preferences.getString(Names.Car.BALANCE + car_id, ""));
                    if ((value <= balance_limit) && (balance_limit >= 0))
                        widgetView.setInt(R.id.balance, "setTextColor", context.getResources().getColor(R.color.error));
                    b = String.format("%.2f", value);
                } catch (Exception ex) {
                    // ignore
                }
                widgetView.setTextViewText(R.id.balance, b);
                show_count++;
            }
            widgetView.setViewVisibility(R.id.balance_block, show_balance ? View.VISIBLE : View.GONE);
            widgetView.setViewVisibility(R.id.name, preferences.getBoolean(Names.SHOW_NAME + widgetID, true) && !isLockScreen(context, widgetID) ? View.VISIBLE : View.GONE);

            boolean show_level = (show_count < rows);
            if (show_level) {
                int level = preferences.getInt(Names.Car.GSM_DB + car_id, 0);
                if (level == 0) {
                    show_level = false;
                } else if (preferences.getLong(Names.Car.LOST + car_id, 0) > preferences.getLong(Names.Car.EVENT_TIME + car_id, 0)) {
                    widgetView.setImageViewResource(R.id.level_img, id_gsm_level[theme][6]);
                    widgetView.setTextViewText(R.id.level, "----");
                    widgetView.setInt(R.id.level, "setTextColor", context.getResources().getColor(R.color.error));
                } else {
                    int index = 0;
                    if (level > -51) {
                        index = 5;
                    } else if (level > -65) {
                        index = 4;
                    } else if (level > -77) {
                        index = 3;
                    } else if (level > -91) {
                        index = 2;
                    } else if (level > -105) {
                        index = 1;
                    }
                    widgetView.setImageViewResource(R.id.level_img, id_gsm_level[theme][index]);
                    widgetView.setTextViewText(R.id.level, level + " dBm");
                    widgetView.setInt(R.id.level, "setTextColor", context.getResources().getColor(id_color[theme]));
                }
                show_count++;
            }
            widgetView.setViewVisibility(R.id.level_block, show_level ? View.VISIBLE : View.GONE);

            boolean show_reserve = (show_count < rows);
            if (show_reserve) {
                String rv = preferences.getString(Names.Car.VOLTAGE_RESERVED + car_id, "?");
                try {
                    double v = Double.parseDouble(rv);
                    rv = String.format("%.2f", v);
                } catch (Exception ex) {
                    // ignore
                    show_reserve = false;
                }
                widgetView.setTextViewText(R.id.reserve, rv + " V");
                int r_color = preferences.getBoolean(Names.Car.RESERVE_NORMAL + car_id, true) ? context.getResources().getColor(id_color[theme]) : context.getResources().getColor(R.color.error);
                widgetView.setInt(R.id.reserve, "setTextColor", r_color);

            }
            widgetView.setViewVisibility(R.id.reserve_block, show_reserve ? View.VISIBLE : View.GONE);

            Cars.Car[] cars = Cars.getCars(context);
            if (cars.length > 1) {
                for (Cars.Car car : cars) {
                    if (car.id.equals(car_id)) {
                        widgetView.setTextViewText(R.id.name, car.name);
                    }
                }
            }

            boolean progress = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD);
            if ((states == null) || !states.containsKey(car_id)) {
                if (progress)
                    widgetView.setViewVisibility(R.id.update, View.GONE);
                widgetView.setViewVisibility(R.id.refresh, View.VISIBLE);
                widgetView.setViewVisibility(R.id.error, View.GONE);
            } else {
                int state = states.get(car_id);
                if (state == STATE_ERROR) {
                    if (progress)
                        widgetView.setViewVisibility(R.id.update, View.GONE);
                    widgetView.setViewVisibility(R.id.refresh, View.GONE);
                    widgetView.setViewVisibility(R.id.error, View.VISIBLE);
                } else {
                    if (progress) {
                        widgetView.setViewVisibility(R.id.refresh, View.GONE);
                        widgetView.setViewVisibility(R.id.update, View.VISIBLE);
                    } else {
                        widgetView.setViewVisibility(R.id.refresh, View.VISIBLE);
                    }
                    widgetView.setViewVisibility(R.id.error, View.GONE);
                }
            }

            if (drawable == null)
                drawable = new CarDrawable();

            Bitmap bmp = drawable.getBitmap(context, car_id, bigPict);
            if (bmp != null)
                widgetView.setImageViewBitmap(R.id.car, bmp);

            if (isLockScreen(context, widgetID)) {
                updateLockWidget(context, widgetView, widgetID, car_id);
            } else {
                configIntent = new Intent(context, WidgetService.class);
                configIntent.putExtra(Names.ID, car_id);
                configIntent.setAction(WidgetService.ACTION_SHOW);
                data = Uri.withAppendedPath(Uri.parse("http://widget/id/"), String.valueOf(widgetID));
                configIntent.setData(data);
                pIntent = PendingIntent.getService(context, widgetID, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                widgetView.setOnClickPendingIntent(R.id.widget, pIntent);
            }

            appWidgetManager.updateAppWidget(widgetID, widgetView);
        } catch (Exception ex) {
            // ignore
        }
    }

    boolean isLockScreen(Context context, int widgetID) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return false;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager == null)
            return false;
        Bundle options = appWidgetManager.getAppWidgetOptions(widgetID);
        return options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1) == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
    }

    void updateLockWidget(Context context, RemoteViews widgetView, int widgetID, String car_id) {
        DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
        Date now = new Date();
        widgetView.setTextViewText(R.id.time, tf.format(now));
        DateFormat df = android.text.format.DateFormat.getDateFormat(context);
        SimpleDateFormat sf = new SimpleDateFormat("E");
        widgetView.setTextViewText(R.id.date, sf.format(now) + " " + df.format(now));
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int level = preferences.getInt(Names.TRAFIC_LEVEL + widgetID, 0);
        long time = preferences.getLong(Names.TRAFIC_TIME + widgetID, 0);
        if (level == 0) {
            widgetView.setViewVisibility(R.id.traffic, View.GONE);
        } else {
            widgetView.setViewVisibility(R.id.traffic, View.VISIBLE);
            if (time + 30 * 60000 < now.getTime()) {
                widgetView.setImageViewResource(R.id.traffic, R.drawable.gray);
            } else {
                widgetView.setImageViewResource(R.id.traffic, trafic_pict[level - 1]);
            }
        }
        if (time + 3 * 60000 >= now.getTime())
            return;
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if ((activeNetwork == null) || !activeNetwork.isConnected())
            return;
        double lat = 0;
        double lon = 0;
        try {
            lat = (double) preferences.getFloat(Names.Car.LAT + car_id, 0);
            lon = (double) preferences.getFloat(Names.Car.LNG + car_id, 0);
        } catch (Exception ex) {
            // ignore
        }
        if ((lat == 0) && (lon == 0))
            return;
        if (request != null)
            return;
        request = new TrafficRequest(context, lat, lon, widgetID);
    }

    class TrafficRequest extends HttpTask {

        int m_id;
        Context m_context;

        TrafficRequest(Context context, double lat, double lon, int widgetId) {
            m_context = context;
            m_id = widgetId;
            execute(URL_TRAFFIC, lat, lon);
        }

        @Override
        void result(JsonObject result) throws ParseException {
            request = null;
            int res = 0;
            JsonValue level = result.get("lvl");
            if (level != null)
                res = level.asInt() + 1;
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(m_context);
            SharedPreferences.Editor ed = preferences.edit();
            ed.putInt(Names.TRAFIC_LEVEL + m_id, res);
            ed.putLong(Names.TRAFIC_TIME + m_id, new Date().getTime());
            ed.commit();
            Intent i = new Intent(WidgetService.ACTION_SCREEN);
            m_context.sendBroadcast(i);
        }

        @Override
        void error() {
            request = null;
        }

    }

}
