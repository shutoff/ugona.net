package net.ugona.plus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class ZoneEdit extends WebViewActivity {

    EditText etName;
    CheckBox chkSms;
    SharedPreferences preferences;
    SettingActivity.Zone zone;

    @Override
    String loadURL() {
        return "file:///android_asset/html/zone.html";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setResult(RESULT_CANCELED);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
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
        etName = (EditText) findViewById(R.id.name);
        chkSms = (CheckBox) findViewById(R.id.sms);
        etName.setText(zone.name);
        chkSms.setChecked(zone.sms);
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
    public void finish() {
        setZone();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.zone, menu);
        boolean isOSM = preferences.getString("map_type", "").equals("OSM");
        menu.findItem(R.id.google).setTitle(getCheckedText(R.string.google, !isOSM));
        menu.findItem(R.id.osm).setTitle(getCheckedText(R.string.osm, isOSM));
        return super.onCreateOptionsMenu(menu);
    }

    String getCheckedText(int id, boolean check) {
        String check_mark = check ? "\u2714" : "";
        return check_mark + getString(id);
    }

    void setZone() {
        zone.name = etName.getText().toString();
        zone.sms = chkSms.isChecked();
    }
}
