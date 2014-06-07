package net.ugona.plus;

import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Vector;

public class WidgetsFragment extends SettingsFragment {

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
                if (preferences.getString(Names.WIDGET + appWidgetID, "").equals(car_id)) {
                    items.add(new WidgetItem("", appWidgetID, false));
                }
            }
            if (items.size() == 0)
                items.add(new Item(getString(R.string.no_widgets)));

            thisAppWidget = new ComponentName(
                    getActivity().getPackageName(), CarLockWidget.class.getName());
            ids = appWidgetManager.getAppWidgetIds(thisAppWidget);
            boolean is_lock = false;
            for (int appWidgetID : ids) {
                if (preferences.getString(Names.WIDGET + appWidgetID, "").equals(car_id)) {
                    items.add(new WidgetItem(getString(R.string.lock_widget), appWidgetID, true));
                    is_lock = true;
                }
            }
            if (!is_lock && (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN)) {
                items.add(new Item(R.string.lock_widget, R.string.add_lock_widget) {
                    @Override
                    void click() {
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
                });
            }
        }
        super.update();
    }

    class WidgetItem extends Item {

        int widget_id;
        boolean lock_widget;


        WidgetItem(String n, int id, boolean lock) {
            super(n);
            widget_id = id;
            lock_widget = lock;
        }

        @Override
        void setView(View v) {
            super.setView(v);
            v.findViewById(R.id.block_widget).setVisibility(View.VISIBLE);
            final String[] themes = getResources().getStringArray(R.array.themes);

            final Spinner lvTheme = (Spinner) v.findViewById(R.id.theme);
            lvTheme.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return themes.length;
                }

                @Override
                public Object getItem(int position) {
                    return themes[position];
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    if (v == null) {
                        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        v = inflater.inflate(R.layout.list_item, null);
                    }
                    TextView tvName = (TextView) v.findViewById(R.id.name);
                    tvName.setText(themes[position]);
                    return v;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    if (v == null) {
                        LayoutInflater inflater = (LayoutInflater) getActivity()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        v = inflater.inflate(R.layout.list_dropdown_item, null);
                    }
                    TextView tvName = (TextView) v.findViewById(R.id.name);
                    tvName.setText(themes[position]);
                    return v;
                }
            });
            lvTheme.setSelection(preferences.getInt(Names.THEME + widget_id, 0));
            lvTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putInt(Names.THEME + widget_id, position);
                    ed.commit();
                    Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
                    i.putExtra(Names.ID, car_id);
                    getActivity().sendBroadcast(i);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            final SeekBar sbTransparency = (SeekBar) v.findViewById(R.id.background);
            sbTransparency.setProgress(preferences.getInt(Names.TRANSPARENCY + widget_id, 0));
            sbTransparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    SharedPreferences.Editor ed = preferences.edit();
                    ed.putInt(Names.TRANSPARENCY + widget_id, progress);
                    ed.commit();
                    Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
                    i.putExtra(Names.ID, car_id);
                    getActivity().sendBroadcast(i);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final CheckBox checkBoxName = (CheckBox) v.findViewById(R.id.show_name);
            if (lock_widget) {
                checkBoxName.setVisibility(View.GONE);
                v.findViewById(R.id.rows_block).setVisibility(View.GONE);
            } else {
                checkBoxName.setChecked(preferences.getBoolean(Names.SHOW_NAME + widget_id, true));
                checkBoxName.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putBoolean(Names.SHOW_NAME + widget_id, isChecked);
                        ed.commit();
                        Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
                        i.putExtra(Names.ID, car_id);
                        getActivity().sendBroadcast(i);
                    }
                });

                final Vector<Integer> rows = new Vector<Integer>();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    rows.add(0);
                rows.add(2);
                rows.add(3);
                rows.add(4);
                rows.add(5);

                final Spinner lvRows = (Spinner) v.findViewById(R.id.rows);
                lvRows.setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return rows.size();
                    }

                    @Override
                    public Object getItem(int position) {
                        return rows.get(position);
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View v = convertView;
                        if (v == null) {
                            LayoutInflater inflater = (LayoutInflater) getActivity()
                                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            v = inflater.inflate(R.layout.list_item, null);
                        }
                        TextView tvName = (TextView) v.findViewById(R.id.name);
                        int value = rows.get(position);
                        String str = value + "";
                        if (value == 0)
                            str = getString(R.string.auto);
                        tvName.setText(str);
                        return v;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        View v = convertView;
                        if (v == null) {
                            LayoutInflater inflater = (LayoutInflater) getActivity()
                                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            v = inflater.inflate(R.layout.list_dropdown_item, null);
                        }
                        TextView tvName = (TextView) v.findViewById(R.id.name);
                        int value = rows.get(position);
                        String str = value + "";
                        if (value == 0)
                            str = getString(R.string.auto);
                        tvName.setText(str);
                        return v;
                    }
                });
                int row = preferences.getInt(Names.ROWS + widget_id, 0);
                for (int i = 0; i < rows.size(); i++) {
                    if (rows.get(i) == row)
                        lvRows.setSelection(i);
                }
                lvRows.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putInt(Names.ROWS + widget_id, rows.get(position));
                        ed.commit();
                        Intent i = new Intent(FetchService.ACTION_UPDATE_FORCE);
                        i.putExtra(Names.ID, car_id);
                        getActivity().sendBroadcast(i);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }
        }
    }
}
