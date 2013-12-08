package net.ugona.plus;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import org.joda.time.LocalDateTime;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CarWidget extends AppWidgetProvider {

    static CarDrawable drawable;

    static final int STATE_UPDATE = 1;
    static final int STATE_ERROR = 2;

    static Map<String, Integer> states;

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
            }
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateWidget(context, appWidgetManager, appWidgetId);
    }

    void updateWidgets(Context context, String car_id, boolean sendtUpdate) {
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
            if (sendtUpdate && update) {
                Intent i = new Intent(context, WidgetService.class);
                i.setAction(WidgetService.ACTION_UPDATE);
                context.startService(i);
            }
        }
    }

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

    void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetID) {

        boolean progress = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD);

        int rows = 3;
        try {
            Bundle options = appWidgetManager.getAppWidgetOptions(widgetID);
            int maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
            if (maxHeight < 70)
                rows = 2;
            if (maxHeight > 100)
                rows = 4;
            State.appendLog(maxHeight + ", " + rows);
        } catch (Exception ex) {
            // ignore
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int theme = preferences.getInt(Names.THEME + widgetID, 0);
        if ((theme < 0) || (theme >= id_layout.length))
            theme = 0;
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), progress ? id_layout[theme] : id_layout_22[theme]);

        String car_id = Preferences.getCar(preferences, preferences.getString(Names.WIDGET + widgetID, ""));
        int transparency = preferences.getInt(Names.TRANSPARENCY + widgetID, 0);
        widgetView.setInt(R.id.bg, "setAlpha", transparency);
        if (transparency > 0)
            widgetView.setImageViewResource(R.id.bg, id_bg[theme]);

        Intent configIntent = new Intent(context, WidgetService.class);
        configIntent.putExtra(Names.ID, car_id);
        configIntent.setAction(WidgetService.ACTION_SHOW);
        Uri data = Uri.withAppendedPath(Uri.parse("http://widget/id/"), String.valueOf(widgetID));
        configIntent.setData(data);
        PendingIntent pIntent = PendingIntent.getService(context, widgetID, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        widgetView.setOnClickPendingIntent(R.id.widget, pIntent);

        configIntent = new Intent(context, WidgetService.class);
        configIntent.putExtra(Names.ID, car_id);
        configIntent.setAction(WidgetService.ACTION_START);
        configIntent.setData(data);
        pIntent = PendingIntent.getService(context, widgetID, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        widgetView.setOnClickPendingIntent(R.id.update_block, pIntent);

        long last = preferences.getLong(Names.EVENT_TIME + car_id, 0);
        Date now = new Date();
        if (last > now.getTime() - 24 * 60 * 60 * 1000) {
            LocalDateTime d = new LocalDateTime(last);
            widgetView.setTextViewText(R.id.last, d.toString("HH:mm"));
        } else {
            widgetView.setTextViewText(R.id.last, "??:??");
        }

        int show_count = 1;
        widgetView.setTextViewText(R.id.voltage, preferences.getString(Names.VOLTAGE_MAIN + car_id, "--") + " V");

        String temperature = Preferences.getTemperature(preferences, car_id);
        if (temperature == null) {
            widgetView.setViewVisibility(R.id.temperature_block, View.GONE);
        } else {
            widgetView.setTextViewText(R.id.temperature, temperature);
            widgetView.setViewVisibility(R.id.temperature_block, View.VISIBLE);
            show_count++;
        }

        boolean show_balance = false;
        if (show_count < rows)
            show_balance = preferences.getBoolean(Names.SHOW_BALANCE + car_id, true);
        if (show_balance) {
            widgetView.setTextViewText(R.id.balance, preferences.getString(Names.BALANCE + car_id, "---.--"));
            int balance_limit = preferences.getInt(Names.LIMIT + car_id, 50);
            widgetView.setInt(R.id.balance, "setTextColor", context.getResources().getColor(id_color[theme]));
            if (balance_limit >= 0) {
                try {
                    double value = Double.parseDouble(preferences.getString(Names.BALANCE + car_id, ""));
                    if (value <= balance_limit)
                        widgetView.setInt(R.id.balance, "setTextColor", context.getResources().getColor(R.color.error));
                } catch (Exception ex) {
                    // ignore
                }
            }
            show_count++;
        }
        widgetView.setViewVisibility(R.id.balance_block, show_balance ? View.VISIBLE : View.GONE);

        boolean show_reserve = (show_count < rows);
        if (show_reserve)
            widgetView.setTextViewText(R.id.reserve, preferences.getString(Names.VOLTAGE_RESERVED + car_id, "--") + " V");
        widgetView.setViewVisibility(R.id.reserve_block, show_reserve ? View.VISIBLE : View.GONE);

        Cars.Car[] cars = Cars.getCars(context);
        if (cars.length > 1) {
            for (Cars.Car car : cars) {
                if (car.id.equals(car_id)) {
                    widgetView.setTextViewText(R.id.name, car.name);
                }
            }
        }

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

        Bitmap bmp = drawable.getBitmap(context, car_id);
        if (bmp != null)
            widgetView.setImageViewBitmap(R.id.car, bmp);

        appWidgetManager.updateAppWidget(widgetID, widgetView);
    }

}
