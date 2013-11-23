package net.ugona.plus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

public class WidgetService extends Service {

    static final int UPDATE_INTERVAL = 5 * 60 * 1000;

    static final String ACTION_STOP = "net.ugona.plus.WIDGET_STOP";
    static final String ACTION_UPDATE = "net.ugona.plus.WIDGET_UPDATE";
    static final String ACTION_SHOW = "net.ugona.plus.WIDGET_SHOW";
    static final String ACTION_START = "net.ugona.plus.WIDGET_START_UPDATE";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    PowerManager powerMgr;
    AlarmManager alarmMgr;

    PendingIntent pi;
    BroadcastReceiver br;

    @Override
    public void onCreate() {
        super.onCreate();
        powerMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        pi = PendingIntent.getService(this, 0, new Intent(this, WidgetService.class), 0);
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_ON)) {
                    stopTimer();
                    startTimer(true);
                }
                if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_OFF)) {
                    stopTimer();
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(br, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(ACTION_STOP)) {
                    stopTimer();
                    stopSelf();
                    return START_STICKY;
                }
                if (action.equals(ACTION_UPDATE)) {
                    stopTimer();
                    if (powerMgr.isScreenOn())
                        startTimer(false);
                    return START_STICKY;
                }
                if (action.equals(ACTION_SHOW)) {
                    Intent i = new Intent(this, MainActivity.class);
                    String id = intent.getStringExtra(Names.ID);
                    State.appendLog("from widget " + id);
                    i.putExtra(Names.ID, id);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    getApplicationContext().startActivity(i);
                    return START_STICKY;
                }
                if (action.equals(ACTION_START)) {
                    String car_id = intent.getStringExtra(Names.ID);
                    try {
                        Intent i = new Intent(action);
                        i.putExtra(Names.ID, car_id);
                        sendBroadcast(i);
                    } catch (Exception e) {
                        // ignore
                    }
                    Intent i = new Intent(this, FetchService.class);
                    i.putExtra(Names.ID, car_id);
                    startService(i);
                    return START_STICKY;
                }
            }
        }
        ComponentName thisAppWidget = new ComponentName(getPackageName(), CarWidget.class.getName());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        if (appWidgetManager != null) {
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            for (int appWidgetID : ids) {
                String car_id = preferences.getString(Names.WIDGET + appWidgetID, "");
                Intent i = new Intent(this, FetchService.class);
                i.putExtra(Names.ID, car_id);
                startService(i);
            }
        }
        return START_STICKY;
    }

    void startTimer(boolean now) {
        alarmMgr.setInexactRepeating(AlarmManager.RTC,
                System.currentTimeMillis() + (now ? 0 : UPDATE_INTERVAL), UPDATE_INTERVAL, pi);
    }

    void stopTimer() {
        alarmMgr.cancel(pi);
    }
}
