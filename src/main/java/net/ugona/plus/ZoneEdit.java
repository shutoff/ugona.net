package net.ugona.plus;

import android.os.Bundle;
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
