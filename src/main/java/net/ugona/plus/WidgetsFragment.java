package net.ugona.plus;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Vector;

public class WidgetsFragment
        extends MainFragment
        implements View.OnClickListener,
        AdapterView.OnItemClickListener {

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
        vList.setOnItemClickListener(this);

        fill();
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
                final Item item = items.get(position);
                if (item.id != 0) {
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

                    final CheckBox chkName = (CheckBox) v.findViewById(R.id.name);
                    chkName.setChecked(preferences.getBoolean(Names.SHOW_NAME + item.id, false));
                    chkName.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putBoolean(Names.SHOW_NAME + item.id, isChecked);
                            ed.commit();
                            Intent intent = new Intent(WidgetService.ACTION_SCREEN);
                            getActivity().sendBroadcast(intent);
                        }
                    });

                    View vDelete = v.findViewById(R.id.delete_widget);
                    vDelete.setTag(item.id);
                    vDelete.setOnClickListener(WidgetsFragment.this);
                } else {
                    v.findViewById(R.id.block_widget).setVisibility(View.GONE);
                }
                TextView tv = (TextView) v.findViewById(R.id.text);
                if (item.text != null) {
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(item.text);
                } else {
                    tv.setVisibility(View.GONE);
                }
                tv = (TextView) v.findViewById(R.id.title);
                if (item.title != null) {
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(item.title);
                } else {
                    tv.setVisibility(View.GONE);
                }

                return v;
            }
        });

        return v;
    }

    int addWidgets(String widgetClass, boolean isLock) {
        int res = 0;
        ComponentName thisAppWidget = new ComponentName(
                getActivity().getPackageName(), widgetClass);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity());
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
                if (isLock)
                    item.text = getString(R.string.lock_widget);
                items.add(item);
                res++;
            }
        }
        return res;
    }

    void fill() {
        items = new Vector<Item>();
        addWidgets(Widget.class.getName(), false);
        addWidgets(CompactWidget.class.getName(), false);
        if (items.size() == 0) {
            Item item = new Item();
            item.text = getString(R.string.no_widgets);
            items.add(item);
        }
        if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)) {
            int locks = addWidgets(LockWidget.class.getName(), true);
            if (locks == 0) {
                Item item = new Item();
                item.title = getString(R.string.lock_widget);
                item.text = getString(R.string.add_lock_widget);
                item.run = new Runnable() {
                    @Override
                    public void run() {
                        String id = "0a9igNb8530";
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
                            startActivity(intent);
                        } catch (ActivityNotFoundException ex) {
                            Intent intent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("http://www.youtube.com/watch?v=" + id));
                            startActivity(intent);
                        }
                    }
                };
                items.add(item);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int widget_id = (Integer) v.getTag();
        try {
            AppWidgetHost appWidgetHost = new AppWidgetHost(getActivity(), 1);
            appWidgetHost.deleteAppWidgetId(widget_id);
        } catch (Exception ex) {
            // ignore
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor ed = preferences.edit();
        ed.remove(Names.WIDGET + widget_id);
        ed.commit();
        fill();
        BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position >= items.size())
            return;
        Runnable run = items.get(position).run;
        if (run == null)
            return;
        run.run();
    }

    class Item {
        int id;
        String title;
        String text;
        Runnable run;
    }
}
