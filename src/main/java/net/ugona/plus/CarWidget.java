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

    static final String HEIGHT = "Height_";

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
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        for (int id : appWidgetIds) {
            ed.remove(HEIGHT + id);
            ed.remove(Names.WIDGET + id);
        }
        ed.commit();
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
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(HEIGHT + appWidgetId, minHeight);
        ed.commit();
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

    void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetID) {

        boolean progress = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD);

        RemoteViews widgetView = new RemoteViews(context.getPackageName(), progress ? R.layout.widget : R.layout.widget_22);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String car_id = Preferences.getCar(preferences, preferences.getString(Names.WIDGET + widgetID, ""));
        int transparency = preferences.getInt(Names.TRANSPARENCY + widgetID, 0);
        widgetView.setInt(R.id.bg, "setAlpha", transparency);
        if (transparency > 0)
            widgetView.setImageViewResource(R.id.bg, R.drawable.widget);

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

        int show_count = 2;
        widgetView.setTextViewText(R.id.voltage, preferences.getString(Names.VOLTAGE_MAIN + car_id, "--") + " V");
        widgetView.setTextViewText(R.id.reserve, preferences.getString(Names.VOLTAGE_RESERVED + car_id, "--") + " V");
        widgetView.setTextViewText(R.id.balance, preferences.getString(Names.BALANCE + car_id, "---.--"));
        String temperature = Preferences.getTemperature(preferences, car_id);
        if (temperature == null) {
            widgetView.setViewVisibility(R.id.temperature_block, View.GONE);
        } else {
            widgetView.setTextViewText(R.id.temperature, temperature);
            widgetView.setViewVisibility(R.id.temperature_block, View.VISIBLE);
            show_count++;
        }

        int height = preferences.getInt(HEIGHT + widgetID, 40);
        boolean show_balance = preferences.getBoolean(Names.SHOW_BALANCE + car_id, true);
        if (show_balance)
            show_count++;

        int max_count = (height >= 60) ? 4 : 3;
        boolean show_reserve = show_count <= max_count;

        widgetView.setViewVisibility(R.id.reserve_block, show_reserve ? View.VISIBLE : View.GONE);
        widgetView.setViewVisibility(R.id.balance_block, show_balance ? View.VISIBLE : View.GONE);

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
