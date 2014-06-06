package net.ugona.plus;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.Vector;

public class WidgetsFragment extends SettingsFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        update();
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        update();
        super.onResume();
    }

    void update() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity());
        if (appWidgetManager != null) {
            items = new Vector<Item>();
            ComponentName thisAppWidget = new ComponentName(
                    getActivity().getPackageName(), CarWidget.class.getName());
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            for (int appWidgetID : ids) {
                if ((car_id == null) || preferences.getString(Names.WIDGET + appWidgetID, "").equals(car_id)) {
                    items.add(new WidgetItem("w: " + appWidgetID));
                }
            }

            thisAppWidget = new ComponentName(
                    getActivity().getPackageName(), CarLockWidget.class.getName());
            ids = appWidgetManager.getAppWidgetIds(thisAppWidget);
            for (int appWidgetID : ids) {
                if ((car_id == null) || preferences.getString(Names.WIDGET + appWidgetID, "").equals(car_id)) {
                    items.add(new WidgetItem("Lock: " + appWidgetID));
                }
            }
        }

    }

    class WidgetItem extends Item {

        WidgetItem(String n) {
            super(n);
        }
    }
}
