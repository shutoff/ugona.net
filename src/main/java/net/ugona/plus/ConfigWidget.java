package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

public class ConfigWidget extends Activity {

    static final int CAR_CONFIG = 1000;
    SharedPreferences preferences;
    String car_id;
    int widgetID;
    Intent resultValue;
    int transparency;
    int theme;
    boolean lock_widget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);

        setResult(RESULT_CANCELED, resultValue);

        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        AppConfig appConfig = AppConfig.get(this);
        final String[] cars = appConfig.getCars();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.widget_config)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(inflater.inflate(R.layout.config_widget, null))
                .create();
        dialog.show();

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
                    LayoutInflater inflater = LayoutInflater.from(ConfigWidget.this);
                    v = inflater.inflate(R.layout.list_item, null);
                }
                TextView tvName = (TextView) v;
                CarConfig carConfig = CarConfig.get(ConfigWidget.this, cars[position]);
                String name = carConfig.getName();
                if (name.equals(""))
                    name = carConfig.getLogin();
                tvName.setText(name);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(ConfigWidget.this);
                    v = inflater.inflate(R.layout.list_dropdown_item, null);
                }
                TextView tvName = (TextView) v;
                CarConfig carConfig = CarConfig.get(ConfigWidget.this, cars[position]);
                String name = carConfig.getName();
                if (name.equals(""))
                    name = carConfig.getLogin();
                tvName.setText(name);
                return v;
            }
        });


        if (cars.length <= 1)
            lv.setVisibility(View.GONE);

        final Spinner lvTheme = (Spinner) dialog.findViewById(R.id.theme);
        lvTheme.setAdapter(new ThemeAdapter(this));

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
                car_id = cars[lv.getSelectedItemPosition()];
                transparency = sbTransparency.getProgress();
                theme = lvTheme.getSelectedItemPosition();
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
        ed.commit();
        setResult(RESULT_OK, resultValue);
    }

    static class ThemeAdapter extends BaseAdapter {

        final String[] themes;
        Context context;

        ThemeAdapter(Context context) {
            themes = context.getResources().getStringArray(R.array.themes);
            this.context = context;
        }

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
                LayoutInflater inflater = LayoutInflater.from(context);
                v = inflater.inflate(R.layout.list_item, null);
            }
            TextView tv = (TextView) v;
            tv.setText(themes[position]);
            return v;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                v = inflater.inflate(R.layout.list_dropdown_item, null);
            }
            TextView tv = (TextView) v;
            tv.setText(themes[position]);
            return v;
        }
    }
}
