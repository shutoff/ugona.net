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

    SharedPreferences preferences;
    String car_id;
    int widgetID;
    Intent resultValue;
    int transparency;

    static final int CAR_CONFIG = 1000;

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
        ed.commit();
        setResult(RESULT_OK, resultValue);
    }
}
