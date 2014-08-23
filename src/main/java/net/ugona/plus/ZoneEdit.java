package net.ugona.plus;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZoneEdit extends MapActivity {

    static Pattern zonePat = Pattern.compile("[A-Za-z0-9]+");
    EditText etName;
    CheckBox chkSms;
    SettingActivity.Zone zone;
    boolean clear_zone;
    boolean confirm;

    @Override
    int menuId() {
        return R.menu.zone;
    }

    @Override
    MapActivity.JsInterface js() {
        return new JsInterface();
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
        etName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b)
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            }
        });
        chkSms.setChecked(zone.sms);
        InputFilter[] filters = {
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        String s = source.subSequence(start, end).toString();
                        Matcher matcher = zonePat.matcher(s);
                        if (matcher.matches())
                            return null;
                        Toast toast = Toast.makeText(ZoneEdit.this, R.string.zone_bad_char, Toast.LENGTH_LONG);
                        toast.show();
                        return "";
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

    class JsInterface extends MapActivity.JsInterface {

        @JavascriptInterface
        public String init() {
            return super.init() + "\nshowZone()";
        }

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
