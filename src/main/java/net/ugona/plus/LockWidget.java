package net.ugona.plus;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LockWidget extends CarWidget {

    int getLayoutId(int theme) {
        return R.layout.lock_widget;
    }

    @Override
    void update(Context context, RemoteViews widgetView) {
        super.update(context, widgetView);
        long now = new Date().getTime();
        DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
        widgetView.setTextViewText(R.id.time, tf.format(now));
        DateFormat df = android.text.format.DateFormat.getDateFormat(context);
        SimpleDateFormat sf = new SimpleDateFormat("E");
        widgetView.setTextViewText(R.id.date, sf.format(now) + " " + df.format(now));
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
        }
        super.onReceive(context, intent);
    }
}
