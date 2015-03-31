package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.EditText;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class ZoneEdit extends MapActivity {

    ZonesFragment.Zone zone;
    boolean bDelete;

    @Override
    int menuId() {
        return R.menu.zone_edit;
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
            zone = (ZonesFragment.Zone) in.readObject();
        } catch (Exception ex) {
            // ignore
        }
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        EditText editText = (EditText) findViewById(R.id.edit_nav);
        editText.setVisibility(View.VISIBLE);
        editText.setText(zone.name);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!super.onCreateOptionsMenu(menu))
            return false;
        menu.findItem(R.id.device).setChecked(zone.device);
        menu.findItem(R.id.sms).setChecked(zone.sms);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete) {
            bDelete = true;
            setResult(Activity.RESULT_OK);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.device) {
            zone.device = !zone.device;
            updateMenu();
        }
        if (item.getItemId() == R.id.sms) {
            zone.sms = !zone.sms;
            updateMenu();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        if (!bDelete) {
            EditText editText = (EditText) findViewById(R.id.edit_nav);
            zone.name = editText.getText().toString();
            Intent result = new Intent();
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
                result.putExtra(Names.TRACK, data);
            setResult(Activity.RESULT_OK, result);
        }
        super.finish();
    }

    @Override
    Js js() {
        return new JsInterface();
    }

    class JsInterface extends MapActivity.JsInterface {

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
