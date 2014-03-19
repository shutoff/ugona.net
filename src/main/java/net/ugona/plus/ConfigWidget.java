package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Vector;

public class ConfigWidget extends Activity {

    static final int CAR_CONFIG = 1000;
    SharedPreferences preferences;
    String car_id;
    int widgetID;
    Intent resultValue;
    int transparency;
    int theme;
    int row;
    boolean show_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // формируем intent ответа
        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);

        // отрицательный ответ
        setResult(RESULT_CANCELED, resultValue);

        final Cars.Car[] cars = Cars.getCars(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        show_name = true;

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.widget_config)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(inflater.inflate(R.layout.config_widget, null))
                .create();
        dialog.show();

        int current = 2;
        final Vector<Integer> rows = new Vector<Integer>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            rows.add(0);
            current = 0;
        }
        rows.add(2);
        rows.add(3);
        rows.add(4);
        rows.add(5);

        final Spinner lvRows = (Spinner) dialog.findViewById(R.id.rows);
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
                    LayoutInflater inflater = (LayoutInflater) getBaseContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.car_key_item, null);
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
                    LayoutInflater inflater = (LayoutInflater) getBaseContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.car_key_item, null);
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
        lvRows.setSelection(current);

        final Spinner lv = (Spinner) dialog.findViewById(R.id.list);
        lv.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return cars.length;
            }

            @Override
            public Object getItem(int position) {
                return cars[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getBaseContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.car_key_item, null);
                }
                TextView tvName = (TextView) v.findViewById(R.id.name);
                tvName.setText(cars[position].name);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getBaseContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.car_key_item, null);
                }
                TextView tvName = (TextView) v.findViewById(R.id.name);
                tvName.setText(cars[position].name);
                return v;
            }
        });


        if (cars.length <= 1)
            lv.setVisibility(View.GONE);

        final String[] themes = getResources().getStringArray(R.array.themes);

        final Spinner lvTheme = (Spinner) dialog.findViewById(R.id.theme);
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
                    LayoutInflater inflater = (LayoutInflater) getBaseContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.car_key_item, null);
                }
                TextView tvName = (TextView) v.findViewById(R.id.name);
                tvName.setText(themes[position]);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = (LayoutInflater) getBaseContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.car_key_item, null);
                }
                TextView tvName = (TextView) v.findViewById(R.id.name);
                tvName.setText(themes[position]);
                return v;
            }
        });

        final SeekBar sbTransparency = (SeekBar) dialog.findViewById(R.id.background);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        final Button btnOk = dialog.getButton(Dialog.BUTTON_POSITIVE);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                car_id = cars[lv.getSelectedItemPosition()].id;
                transparency = sbTransparency.getProgress();
                theme = lvTheme.getSelectedItemPosition();
                row = rows.get(lvRows.getSelectedItemPosition());
                CheckBox checkBoxName = (CheckBox) dialog.findViewById(R.id.show_name);
                show_name = checkBoxName.isChecked();
                saveWidget();
                dialog.dismiss();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;
        switch (requestCode) {
            case CAR_CONFIG:
                if (preferences.getString(Names.CAR_KEY + car_id, "").length() > 0)
                    saveWidget();
                finish();
                break;
        }
    }

    void saveWidget() {
        SharedPreferences.Editor ed = preferences.edit();
        ed.putString(Names.WIDGET + widgetID, car_id);
        ed.putInt(Names.TRANSPARENCY + widgetID, transparency);
        ed.putInt(Names.THEME + widgetID, theme);
        ed.putInt(Names.ROWS + widgetID, row);
        ed.putBoolean(Names.SHOW_NAME + widgetID, show_name);
        ed.commit();
        setResult(RESULT_OK, resultValue);
    }
}
