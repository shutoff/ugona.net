package net.ugona.plus;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Vector;

public class WidgetsFragment extends MainFragment {

    ListView vList;
    Vector<Item> items;

    @Override
    int layout() {
        return R.layout.settings;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vList = (ListView) v.findViewById(R.id.list);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity());
        if (appWidgetManager != null) {
            items = new Vector<Item>();
            ComponentName thisAppWidget = new ComponentName(
                    getActivity().getPackageName(), Widget.class.getName());
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            AppWidgetHost appWidgetHost = null;
            for (int appWidgetID : ids) {
                AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetID);
                if (info == null)
                    continue;
                String id = preferences.getString(Names.WIDGET + appWidgetID, "-");
                if (id.equals("-")) {
                    try {
                        if (appWidgetHost == null)
                            appWidgetHost = new AppWidgetHost(getActivity(), 1);
                        appWidgetHost.deleteAppWidgetId(appWidgetID);
                    } catch (Exception ex) {
                        // ignore
                    }
                    continue;
                }
                if (id.equals(id())) {
                    Item item = new Item();
                    item.id = appWidgetID;
                    items.add(item);
                }
            }
            if (items.size() == 0) {
                Item item = new Item();
                item.text = getString(R.string.no_widgets);
                items.add(item);
            }
        }

        vList.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return items.size();
            }

            @Override
            public Object getItem(int position) {
                return items.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.widget_item, null);
                }
                Item item = items.get(position);
                if (item.text != null) {
                    v.findViewById(R.id.block_widget).setVisibility(View.GONE);
                    TextView tv = (TextView) v.findViewById(R.id.text);
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(item.text);
                } else {
                    v.findViewById(R.id.block_widget).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.text).setVisibility(View.GONE);
                    Spinner vTheme = (Spinner) v.findViewById(R.id.theme);
                    vTheme.setAdapter(new ConfigWidget.ThemeAdapter(vTheme));
                    final int widget_id = item.id;
                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    int theme = preferences.getInt(Names.THEME + item.id, 0);
                    vTheme.setSelection(theme);
                    vTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putInt(Names.THEME + widget_id, position);
                            ed.commit();
                            Intent intent = new Intent(WidgetService.ACTION_SCREEN);
                            getActivity().sendBroadcast(intent);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });

                    final SeekBar sbTransparency = (SeekBar) v.findViewById(R.id.background);
                    sbTransparency.setProgress(preferences.getInt(Names.TRANSPARENCY + item.id, 0));
                    sbTransparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putInt(Names.TRANSPARENCY + widget_id, progress);
                            ed.commit();
                            Intent intent = new Intent(WidgetService.ACTION_SCREEN);
                            getActivity().sendBroadcast(intent);
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    });
                }
                return v;
            }
        });

        return v;
    }

    class Item {
        int id;
        String text;
    }
}
