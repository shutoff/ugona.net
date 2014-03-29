package net.ugona.plus;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.CheckBox;
import android.widget.EditText;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class ZoneEdit extends GpsActivity {

    EditText etName;
    CheckBox chkSms;
    SettingActivity.Zone zone;
    boolean clear_zone;
    boolean confirm;

    @Override
    String loadURL() {
        webView.addJavascriptInterface(new JsInterface(), "android");
        return getURL();
    }

    String getURL() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getString("map_type", "").equals("OSM"))
            return "file:///android_asset/html/ozone.html";
        return "file:///android_asset/html/zone.html";
    }

    @Override
    int menuId() {
        return R.menu.zone;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setResult(RESULT_CANCELED);
        byte[] data;
        if (savedInstanceState != null) {
            data = savedInstanceState.getByteArray(Names.TRACK);
        } else {
            data = getIntent().getByteArrayExtra(Names.TRACK);
        }
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInput in = new ObjectInputStream(bis);
            zone = (SettingActivity.Zone) in.readObject();
        } catch (Exception ex) {
            // ignore
        }

        super.onCreate(savedInstanceState);
        findViewById(R.id.zone_info).setVisibility(View.VISIBLE);
        etName = (EditText) findViewById(R.id.name_edit);
        chkSms = (CheckBox) findViewById(R.id.sms_check);
        etName.setText(zone.name);
        chkSms.setChecked(zone.sms);
        InputFilter[] filters = {
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        for (int i = start; i < end; i++) {
                            if (!Character.isLetterOrDigit(source.charAt(i))) {
                                return "";
                            }
                        }
                        return null;
                    }
                }
        };
        etName.setFilters(filters);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        setZone();
        super.onSaveInstanceState(outState);
        byte[] data = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(zone);
            data = bos.toByteArray();
            out.close();
            bos.close();
        } catch (Exception ex) {
            // ignore
        }
        if (data != null)
            outState.putByteArray(Names.TRACK, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete) {
            clear_zone = true;
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        if (clear_zone) {
            if (!confirm && !zone._name.equals("")) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.zone_remove)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                confirm = true;
                                finish();
                            }
                        })
                        .create();
                dialog.show();
                return;
            }
            zone.name = "";
        } else {
            setZone();
            if (zone.name.equals("")) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.zone_name)
                        .setMessage(R.string.zone_name_msg)
                        .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                etName.requestFocus();
                            }
                        })
                        .create();
                dialog.show();
                return;
            }
        }
        Intent i = getIntent();
        try {
            byte[] data = null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(zone);
            data = bos.toByteArray();
            out.close();
            bos.close();
            i.putExtra(Names.TRACK, data);
        } catch (Exception ex) {
            // ignore
        }
        setResult(RESULT_OK, i);
        super.finish();
    }

    void setZone() {
        zone.name = etName.getText().toString();
        zone.sms = chkSms.isChecked();
    }

    class JsInterface extends GpsActivity.JsInterface {

        @JavascriptInterface
        public String getZone() {
            return zone.lat1 + "," + zone.lng1 + "," + zone.lat2 + "," + zone.lng2;
        }

        @JavascriptInterface
        public void setZone(String data) {
            String[] val = data.split(",");
            try {
                zone.lat1 = Double.parseDouble(val[0]);
                zone.lng1 = Double.parseDouble(val[1]);
                zone.lat2 = Double.parseDouble(val[2]);
                zone.lng2 = Double.parseDouble(val[3]);
            } catch (Exception ex) {
                // ignore
            }
        }
    }

}
