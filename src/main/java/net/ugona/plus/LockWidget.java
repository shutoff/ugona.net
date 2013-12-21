package net.ugona.plus;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LockWidget extends CarWidget {

    final static String TRAFFIC_URL = "http://api-maps.yandex.ru/services/traffic-info/1.0/?format=json&lang=ru-RU'";

    int getLayoutId(int theme) {
        return R.layout.lock_widget;
    }

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

    @Override
    void postUpdate(Context context, RemoteViews widgetView, int widgetID) {
        super.postUpdate(context, widgetView, widgetID);
        long now = new Date().getTime();
        DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
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
            if (time + 30 * 60000 < now) {
                widgetView.setImageViewResource(R.id.traffic, R.drawable.gray);
            } else {
                widgetView.setImageViewResource(R.id.traffic, trafic_pict[level - 1]);
            }
        }
        if (time + 3 * 60000 >= now)
            return;
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if ((activeNetwork == null) || !activeNetwork.isConnected())
            return;
        String id = preferences.getString(Names.WIDGET + widgetID, "");
        String car_id = Preferences.getCar(preferences, id);
        double lat = 0;
        double lon = 0;
        try {
            lat = Double.parseDouble(preferences.getString(Names.LATITUDE + car_id, ""));
            lon = Double.parseDouble(preferences.getString(Names.LONGITUDE + car_id, ""));
        } catch (Exception ex) {
            // ignore
        }
        if ((lat == 0) && (lon == 0))
            return;
        new TrafficRequest(context, lat, lon, widgetID);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if ((action != null) && action.equals(WidgetService.ACTION_SCREEN)) {
                ComponentName thisAppWidget = new ComponentName(
                        context.getPackageName(), getClass().getName());
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                if (appWidgetManager != null) {
                    int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
                    for (int appWidgetID : ids) {
                        updateWidget(context, appWidgetManager, appWidgetID);
                    }
                }
            }
            if ((action != null) && action.equals(WidgetService.ACTION_SCREEN)) {
                ComponentName thisAppWidget = new ComponentName(
                        context.getPackageName(), getClass().getName());
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                if (appWidgetManager != null) {
                    int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    long now = new Date().getTime();
                    for (int appWidgetID : ids) {
                        if (preferences.getLong(Names.TRAFIC_TIME, 0) + 3 * 60000 < now)
                            updateWidget(context, appWidgetManager, appWidgetID);
                    }
                }
            }
        }
        super.onReceive(context, intent);
    }

    String widgetClass() {
        return getClass().getName();
    }

    class TrafficRequest extends HttpTask {

        double m_lat;
        double m_lon;
        int m_id;
        Context m_context;

        TrafficRequest(Context context, double lat, double lon, int widgetId) {
            m_context = context;
            m_lat = lat;
            m_lon = lon;
            m_id = widgetId;
            execute(TRAFFIC_URL);
        }

        @Override
        void result(JsonObject result) throws ParseException {
            result = result.get("GeoObjectCollection").asObject();
            JsonArray data = result.get("features").asArray();
            int length = data.size();
            int res = 0;
            for (int i = 0; i < length; i++) {
                result = data.get(i).asObject();
                JsonObject jams = result.get("properties").asObject();
                jams = jams.get("JamsMetaData").asObject();
                JsonValue lvl = jams.get("level");
                if (lvl == null)
                    continue;
                int level = lvl.asInt();
                result = result.get("geometry").asObject();
                JsonArray coord = result.get("coordinates").asArray();
                double lat = coord.get(1).asDouble();
                double lon = coord.get(0).asDouble();
                double d = Address.calc_distance(lat, lon, m_lat, m_lon);
                if (d < 80000) {
                    res = level + 1;
                    break;
                }
            }
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

        }

    }
}
