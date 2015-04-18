package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

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

        LayoutInflater inflater = LayoutInflater.from(this);
        final Dialog dialog = new AlertDialogWrapper.Builder(this)
                .setTitle(R.string.widget_config)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(inflater.inflate(R.layout.config_widget, null))
                .create();
        dialog.show();

        final Spinner lv = (Spinner) dialog.findViewById(R.id.list);
        lv.setAdapter(new ArrayAdapter(lv) {
            @Override
            public int getCount() {
                return cars.length;
            }

            @Override
            public Object getItem(int position) {
                CarConfig carConfig = CarConfig.get(getBaseContext(), cars[position]);
                return carConfig.getName();
            }

        });


        if (cars.length <= 1)
            lv.setVisibility(View.GONE);

        final Spinner lvTheme = (Spinner) dialog.findViewById(R.id.theme);
        lvTheme.setAdapter(new ThemeAdapter(lvTheme));

        final SeekBar sbTransparency = (SeekBar) dialog.findViewById(R.id.background);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        MaterialDialog materialDialog = (MaterialDialog) dialog;
        final View btnOk = materialDialog.getActionButton(DialogAction.POSITIVE);
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

    static class ThemeAdapter extends ArrayAdapter {

        final String[] themes;

        ThemeAdapter(Spinner spinner) {
            super(spinner);
            themes = spinner.getContext().getResources().getStringArray(R.array.themes);
        }

        @Override
        public int getCount() {
            return themes.length;
        }

        @Override
        public Object getItem(int position) {
            return themes[position];
        }

    }
}
