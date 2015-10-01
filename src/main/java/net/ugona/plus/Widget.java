package net.ugona.plus;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.RemoteViews;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Widget extends AppWidgetProvider {

    static final int STATE_UPDATE = 1;
    static final int STATE_ERROR = 2;

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

    static final int id_bg[] = {
            R.drawable.widget,
            R.drawable.widget_light
    };

    static final int id_color[] = {
            android.R.color.secondary_text_dark,
            R.color.text_dark
    };

    static final int[] id_gsm_level[] = {
            {
                    R.drawable.b_gsm_level0,
                    R.drawable.b_gsm_level1,
                    R.drawable.b_gsm_level2,
                    R.drawable.b_gsm_level3,
                    R.drawable.b_gsm_level4,
                    R.drawable.b_gsm_level5,
                    R.drawable.b_gsm_level
            },
            {
                    R.drawable.w_gsm_level0,
                    R.drawable.w_gsm_level1,
                    R.drawable.w_gsm_level2,
                    R.drawable.w_gsm_level3,
                    R.drawable.w_gsm_level4,
                    R.drawable.w_gsm_level5,
                    R.drawable.w_gsm_level
            }
    };
    static Map<String, Integer> widget_states;
    static SparseIntArray height_rows;
    static TrafficRequest request;
    static CarImage carImage;

    static int[] picts = {R.id.pict1, R.id.pict2, R.id.pict3};

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
                if (widget_states == null)
                    widget_states = new HashMap<String, Integer>();
                String car_id = intent.getStringExtra(Names.ID);
                String key = getClass().getName() + car_id;
                if (action.equalsIgnoreCase(Names.UPDATED)) {
                    widget_states.remove(key);
                    updateWidgets(context, car_id, false);
                }
                if (action.equalsIgnoreCase(Names.NO_UPDATED)) {
                    if (widget_states.containsKey(key)) {
                        widget_states.remove(key);
                        updateWidgets(context, car_id, false);
                    }
                }
                if (action.equalsIgnoreCase(Names.ERROR)) {
                    if (!widget_states.containsKey(key) || (widget_states.get(key) != STATE_ERROR)) {
                        widget_states.put(car_id, STATE_ERROR);
                        updateWidgets(context, car_id, false);
                    }
                }
                if (action.equalsIgnoreCase(Names.START_UPDATE)) {
                    if (!widget_states.containsKey(key) || (widget_states.get(key) != STATE_UPDATE)) {
                        widget_states.put(key, STATE_UPDATE);
                        updateWidgets(context, car_id, false);
                    }
                }
                if (action.equals(WidgetService.ACTION_SCREEN))
                    updateWidgets(context, null, true);
                if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
                    if ((activeNetwork == null) || !activeNetwork.isConnected())
                        return;
                    updateWidgets(context, null, true);
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
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null) {
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> updated = new HashSet<String>();
            for (int appWidgetID : ids) {
                String id = preferences.getString(Names.WIDGET + appWidgetID, "");
                if ((car_id == null) || id.equals(car_id)) {
                    updateWidget(context, appWidgetManager, appWidgetID);
                    if (sendUpdate) {
                        if (updated.contains(id))
                            continue;
                        updated.add(id);
                        Intent i = new Intent(context, WidgetService.class);
                        i.setAction(WidgetService.ACTION_UPDATE);
                        i.putExtra(Names.ID, id);
                        context.startService(i);
                    }
                }
            }
        }
    }

    int getLayoutId(Context context, int widgetId, int theme) {
        return id_layout[theme];
    }

    void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetID) {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int theme = preferences.getInt(Names.THEME + widgetID, 0);
            if ((theme < 0) || (theme >= id_layout.length))
                theme = 0;
            RemoteViews widgetView = new RemoteViews(context.getPackageName(), getLayoutId(context, widgetID, theme));

            String id = preferences.getString(Names.WIDGET + widgetID, "");
            AppConfig appConfig = AppConfig.get(context);
            String car_id = appConfig.getId(id);
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

            CarState carState = CarState.get(context, car_id);
            CarConfig carConfig = CarConfig.get(context, car_id);

            if (carImage == null)
                carImage = new CarImage(context, carConfig.getTheme());
            carImage.update(carState, isCompact());

            boolean show_name = preferences.getBoolean(Names.SHOW_NAME + widgetID, false);
            if (show_name) {
                widgetView.setTextViewText(R.id.name, carConfig.getName());
                widgetView.setViewVisibility(R.id.name, View.VISIBLE);
            } else {
                widgetView.setViewVisibility(R.id.name, View.GONE);
            }

            String[] ext = carImage.state.split("\\|");
            String car_state = URLEncoder.encode(ext[0], "utf-8");
            Uri uri = Uri.parse("content://net.ugona.plus.car/" + getTheme(carConfig) + "-" + car_state);
            widgetView.setImageViewUri(R.id.car, uri);

            int kn = 0;
            if (ext.length > 1) {
                String[] parts = ext[1].split(";");
                for (String part : parts) {
                    uri = Uri.parse("content://net.ugona.plus.car/__" + part);
                    int w_id = picts[kn++];
                    widgetView.setImageViewUri(w_id, uri);
                    widgetView.setViewVisibility(w_id, View.VISIBLE);
                    if (kn >= picts.length)
                        break;
                }
            }
            for (; kn < picts.length; kn++) {
                widgetView.setViewVisibility(picts[kn], View.GONE);
            }

            long last = carState.getTime();
            Date now = new Date();
            if (last > now.getTime() - 24 * 60 * 60 * 1000) {
                DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
                widgetView.setTextViewText(R.id.last, tf.format(last));
            } else {
                widgetView.setTextViewText(R.id.last, "??:??");
            }

            int show_count = 1;

            Double power = carState.getPower();
            if (power > 0) {
                widgetView.setTextViewText(R.id.voltage, power + " V");
                widgetView.setViewVisibility(R.id.voltage_block, View.VISIBLE);
                show_count++;
                setPowerState(context, widgetView, theme, R.id.voltage, carState.getPower_state());
            } else {
                widgetView.setViewVisibility(R.id.voltage_block, View.GONE);
            }

            String temperature = carState.getTemperature();
            if (temperature != null) {
                String[] parts = temperature.split(",");
                if (parts.length > 0) {
                    try {
                        String[] p = parts[0].split(":");
                        int t = Integer.parseInt(p[1]);
                        widgetView.setTextViewText(R.id.temperature1, t + " \u00B0C");
                        widgetView.setViewVisibility(R.id.temperature1_block, View.VISIBLE);
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            } else {
                widgetView.setViewVisibility(R.id.temperature1_block, View.GONE);
            }

            boolean show_balance = carConfig.isShowBalance();
            if (show_count >= getRows())
                show_balance = false;
            String balance = carState.getBalance();
            if (balance.equals(""))
                show_balance = false;
            if (show_balance) {
                int color = id_color[theme];
                try {
                    NumberFormat nf = NumberFormat.getCurrencyInstance();
                    double value = Double.parseDouble(balance);
                    double abs_value = Math.abs(value);
                    String str = nf.format(value);
                    Currency currency = Currency.getInstance(Locale.getDefault());
                    str = str.replace(currency.getSymbol(), "");
                    if (abs_value > 10000) {
                        NumberFormat f = NumberFormat.getInstance(context.getResources().getConfiguration().locale);
                        str = f.format(Math.round(value));
                    }
                    widgetView.setTextViewText(R.id.balance, str);
                    int balance_limit = carConfig.getBalance_limit();
                    if (value <= balance_limit) {
                        color = R.color.error;
                    }
                } catch (Exception ex) {
                    widgetView.setTextViewText(R.id.balance, balance);
                }
                widgetView.setViewVisibility(R.id.balance_block, View.VISIBLE);
                widgetView.setTextColor(R.id.balance, context.getResources().getColor(color));
                show_count++;
            } else {
                widgetView.setViewVisibility(R.id.balance_block, View.GONE);
            }

            double card_voltage = carState.getCard_voltage();
            if ((card_voltage > 0) && (show_count < getRows())) {
                widgetView.setTextViewText(R.id.card, card_voltage + " V");
                widgetView.setViewVisibility(R.id.card_block, View.VISIBLE);
                show_count++;
            } else {
                widgetView.setViewVisibility(R.id.card_block, View.GONE);
            }

            int gsm_level = carState.getGsm_level();
            boolean show_level = (show_count < getRows());
            if (gsm_level == 0)
                show_level = false;
            if (show_level) {
                int level = State.GsmLevel(gsm_level);
                widgetView.setImageViewResource(R.id.level_img, id_gsm_level[theme][level]);
                widgetView.setTextViewText(R.id.level, gsm_level + " dBm");
                widgetView.setViewVisibility(R.id.level_block, View.VISIBLE);
                show_count++;
            } else {
                widgetView.setViewVisibility(R.id.level_block, View.GONE);
            }

            boolean show_reserve = (show_count < getRows());
            double reserved = carState.getReserved();
            if (reserved == 0)
                show_reserve = false;

            if (show_reserve) {
                widgetView.setTextViewText(R.id.reserve, reserved + " V");
                widgetView.setViewVisibility(R.id.reserve_block, View.VISIBLE);
                setPowerState(context, widgetView, theme, R.id.reserve, carState.getReserved_state());
            } else {
                widgetView.setViewVisibility(R.id.reserve_block, View.GONE);
            }

            String key = getClass().getName() + car_id;
            if ((widget_states == null) || !widget_states.containsKey(key)) {
                widgetView.setViewVisibility(R.id.update, View.GONE);
                widgetView.setViewVisibility(R.id.refresh, View.VISIBLE);
                widgetView.setViewVisibility(R.id.error, View.GONE);
            } else {
                int state = widget_states.get(key);
                if (state == STATE_ERROR) {
                    widgetView.setViewVisibility(R.id.update, View.GONE);
                    widgetView.setViewVisibility(R.id.refresh, View.GONE);
                    widgetView.setViewVisibility(R.id.error, View.VISIBLE);
                } else {
                    widgetView.setViewVisibility(R.id.refresh, View.GONE);
                    widgetView.setViewVisibility(R.id.update, View.VISIBLE);
                    widgetView.setViewVisibility(R.id.error, View.GONE);
                }
            }

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

    String getTheme(CarConfig carConfig) {
        return carConfig.getTheme();
    }

    boolean isCompact() {
        return false;
    }

    int getRows() {
        return 6;
    }

    void setPowerState(Context context, RemoteViews widgetView, int theme, int resId, int state) {
        int color = id_color[theme];
        switch (state) {
            case 0:
                color = R.color.error;
                break;
            case 2:
                color = R.color.neutral;
                break;
        }
        widgetView.setTextColor(resId, context.getResources().getColor(color));
    }

    boolean isLockScreen(Context context, int widgetID) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            return false;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager == null)
            return false;
        try {
            Method method = AppWidgetManager.class.getMethod("getAppWidgetOptions", int.class);
            Bundle options = (Bundle) method.invoke(appWidgetManager, widgetID);
            return options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1) == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
        } catch (Exception ex) {
            // ignroe
        }
        return false;
    }

    void updateLockWidget(Context context, RemoteViews widgetView, int widgetID, String car_id) {
        DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
        Date now = new Date();
        widgetView.setTextViewText(R.id.time, tf.format(now));
        DateFormat df = android.text.format.DateFormat.getDateFormat(context);
        SimpleDateFormat sf = new SimpleDateFormat("E");
        widgetView.setTextViewText(R.id.date, sf.format(now) + " " + df.format(now));
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int level = preferences.getInt(Names.TRAFFIC_LEVEL + widgetID, 0);
        long time = preferences.getLong(Names.TRAFFIC_TIME + widgetID, 0);
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
        CarState carState = CarState.get(context, car_id);
        try {
            String[] gps = carState.getGps().split(",");
            lat = Double.parseDouble(gps[0]);
            lon = Double.parseDouble(gps[1]);
        } catch (Exception ex) {
            // ignore
        }
        if ((lat == 0) && (lon == 0))
            return;
        if (request != null)
            return;
        request = new TrafficRequest(context, lat, lon, widgetID);
    }

    static class TrafficParams implements Serializable {
        double lat;
        double lng;
    }

    class TrafficRequest extends HttpTask {

        int m_id;
        Context m_context;

        TrafficRequest(Context context, double lat, double lon, int widgetId) {
            m_context = context;
            m_id = widgetId;
            TrafficParams params = new TrafficParams();
            params.lat = lat;
            params.lng = lon;
            execute("/level", params);
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
            ed.putInt(Names.TRAFFIC_LEVEL + m_id, res);
            ed.putLong(Names.TRAFFIC_TIME + m_id, new Date().getTime());
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
