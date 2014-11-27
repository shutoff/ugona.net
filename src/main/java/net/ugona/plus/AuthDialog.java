package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Date;
import java.util.Locale;
import java.util.Vector;

public class AuthDialog extends Activity {

    final static String URL_KEY = "/key?login=$1&password=$2";
    final static String URL_PROFILE = "/version?skey=$1";
    final static String URL_PHOTOS = "/photos?skey=$1&begin=$2";
    static boolean is_show;
    EditText edLogin;
    EditText edPasswd;
    EditText edNumber;
    TextView tvError;
    boolean show_auth;
    boolean show_phone;
    String car_id;
    SharedPreferences preferences;
    AlertDialog dialog;
    int bad_count;
    int max_count;

    static boolean isValidPhoneNumber(String number) {
        if (State.isDebug())
            return true;
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber n = phoneUtil.parse(number, Locale.getDefault().getCountry());
            return phoneUtil.isValidNumber(n);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        is_show = true;

        show_auth = getIntent().getBooleanExtra(Names.Car.AUTH, false);
        show_phone = getIntent().getBooleanExtra(Names.Car.CAR_PHONE, false);

        if (show_auth || show_phone) {
            car_id = getIntent().getStringExtra(Names.ID);
            max_count = getIntent().getIntExtra(Names.ERROR, 0);
        } else {
            show_phone = true;
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(show_auth ? R.string.auth : R.string.phone_number)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(inflater.inflate(R.layout.apikeydialog, null));
        if (getIntent().getBooleanExtra(Names.Car.CAR_NAME, false)) {
            builder.setNeutralButton(R.string.demo_mode, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    auth("demo", "demo", "+7(495)995-30-54");
                }
            });
        }
        if (show_auth)
            builder.setMessage(R.string.auth_summary);
        dialog = builder.create();
        dialog.show();
        edNumber = (EditText) dialog.findViewById(R.id.number);
        edLogin = (EditText) dialog.findViewById(R.id.login);
        edPasswd = (EditText) dialog.findViewById(R.id.passwd);
        tvError = (TextView) dialog.findViewById(R.id.error);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                is_show = false;
                if ((max_count > 0) && (bad_count > max_count)) {
                    AlertDialog bad = new AlertDialog.Builder(AuthDialog.this)
                            .setTitle(R.string.auth_error)
                            .setMessage(R.string.auth_message)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.send_msg, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("text/html");
                                    intent.putExtra(Intent.EXTRA_EMAIL, "info@ugona.net");
                                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.auth_error));
                                    startActivity(Intent.createChooser(intent, getString(R.string.send_msg)));
                                }
                            })
                            .create();
                    bad.show();
                    bad.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    });
                    return;
                }
                finish();
            }
        });
        final Button btnOk = dialog.getButton(Dialog.BUTTON_POSITIVE);
        btnOk.setEnabled(false);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String login = null;
                String pass = null;
                String phone = null;
                if (show_auth) {
                    login = edLogin.getText().toString();
                    pass = edPasswd.getText().toString();
                }
                if (show_phone) {
                    phone = edNumber.getText().toString();
                }
                auth(login, pass, phone);
            }
        });

        final CheckBox chkPswd = (CheckBox) dialog.findViewById(R.id.show_password);
        final int initial_type = chkPswd.getInputType();
        chkPswd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int type = initial_type;
                if (isChecked) {
                    type |= InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                    chkPswd.setVisibility(View.GONE);
                }
                edPasswd.setInputType(type);
                edPasswd.setSelection(edPasswd.getText().length());
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean ok = true;
                if (show_auth)
                    ok = !edLogin.getText().toString().equals("") && !edPasswd.getText().toString().equals("");
                if (ok && show_phone && !isValidPhoneNumber(edNumber.getText().toString()))
                    ok = false;
                btnOk.setEnabled(ok);
                tvError.setVisibility(View.GONE);
            }
        };

        if (show_auth) {
            String login = preferences.getString(Names.Car.LOGIN + car_id, "");
            if (login.equals("demo"))
                login = "";
            edLogin.setText(login);
            edLogin.addTextChangedListener(watcher);
            edPasswd.addTextChangedListener(watcher);
            edLogin.requestFocus();
        } else {
            dialog.findViewById(R.id.login_row).setVisibility(View.GONE);
            dialog.findViewById(R.id.passwd_row).setVisibility(View.GONE);
        }

        if (show_phone) {
            if (car_id != null)
                edNumber.setText(preferences.getString(Names.Car.CAR_PHONE + car_id, ""));
            edNumber.addTextChangedListener(watcher);
            if (!show_auth)
                edNumber.requestFocus();
        } else {
            dialog.findViewById(R.id.phone_row).setVisibility(View.GONE);
        }

        View iv = dialog.findViewById(R.id.contacts);
        iv.setClickable(true);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    startActivityForResult(intent, 1);
                } catch (Exception ex) {
                }
            }
        });
    }

    void auth(final String login, final String pass, String phone) {

        Intent i = getIntent();
        if (phone != null) {
            String number = phone;
            if (!State.isDebug())
                number = Phones.formatPhoneNumber(number);
            if (car_id == null) {
                i.putExtra(Names.Car.CAR_PHONE, number);
            } else {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.Car.CAR_PHONE + car_id, number);
                ed.commit();
            }
        }

        if ((login == null) || (pass == null)) {
            setResult(RESULT_OK, i);
            dialog.dismiss();
            return;
        }

        final ProgressDialog dlgCheck = new ProgressDialog(AuthDialog.this);
        try {
            dlgCheck.setMessage(getString(R.string.check_auth));
            dlgCheck.show();
        } catch (Exception ex) {
        }

        HttpTask apiTask = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                final String key = res.get("key").asString();
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(Names.Car.CAR_KEY + car_id, key);
                ed.putString(Names.Car.LOGIN + car_id, login);
                ed.putString(Names.Car.AUTH + car_id, res.get("auth").asString());
                if (key.equals("demo")) {
                    ed.putString(Names.Car.CONTROL + car_id, "inet");
                    ed.putString(Names.Car.CAR_NAME + car_id, "Demo");
                }
                ed.remove(Names.GCM_TIME);
                ed.remove(Names.Car.EVENT_TIME + car_id);
                final String[] cars = preferences.getString(Names.CARS, "").split(",");
                boolean is_new = true;
                for (String car : cars) {
                    if (car.equals(car_id))
                        is_new = false;
                }
                if (is_new)
                    ed.putString(Names.CARS, preferences.getString(Names.CARS, "") + "," + car_id);
                ed.commit();

                HttpTask version = new HttpTask() {
                    @Override
                    void result(JsonObject res) throws ParseException {
                        SharedPreferences.Editor ed = preferences.edit();
                        if (show_phone) {
                            String number = edNumber.getText().toString();
                            if (!State.isDebug())
                                number = Phones.formatPhoneNumber(number);
                            ed.putString(Names.Car.CAR_PHONE + car_id, number);
                        }
                        String ver = res.get("version").asString();
                        ed.putString(Names.Car.VERSION + car_id, ver);
                        boolean pointer = false;
                        if (ver.toUpperCase().substring(0, 5).equals("MS-TR")) {
                            ed.putBoolean(Names.Car.POINTER + car_id, true);
                            pointer = true;
                        }
                        ed.commit();
                        setResult(RESULT_OK, getIntent());
                        dlgCheck.dismiss();
                        dialog.dismiss();
                        Intent intent = new Intent(FetchService.ACTION_UPDATE_FORCE);
                        intent.putExtra(Names.ID, car_id);
                        sendBroadcast(intent);
                        if (!pointer) {
                            boolean c1 = preferences.getBoolean(Names.Car.SHOW_PHOTO + car_id, false);
                            boolean c2 = preferences.getBoolean(Names.Car.SHOW_PHOTO + car_id, true);
                            if (!c1 && c2) {
                                HttpTask photo = new HttpTask() {
                                    @Override
                                    void result(JsonObject res) throws ParseException {
                                        JsonArray array = res.get("photos").asArray();
                                        SharedPreferences.Editor ed = preferences.edit();
                                        ed.putBoolean(Names.Car.SHOW_PHOTO + car_id, array.size() > 0);
                                        ed.commit();
                                        Intent intent = new Intent(FetchService.ACTION_UPDATE_FORCE);
                                        intent.putExtra(Names.ID, car_id);
                                        sendBroadcast(intent);
                                    }

                                    @Override
                                    void error() {

                                    }
                                };
                                Date now = new Date();
                                photo.execute(URL_PHOTOS, key, now.getTime() - 86400000 * 3);
                            }
                        }
                        if (!show_phone && State.hasTelephony(AuthDialog.this) && preferences.getString(Names.Car.CAR_PHONE, "").equals("")) {
                            Intent i = new Intent(AuthDialog.this, AuthDialog.class);
                            i.putExtra(Names.ID, car_id);
                            i.putExtra(Names.Car.CAR_PHONE, true);
                            startActivityForResult(i, 1000);
                        }
                    }

                    @Override
                    void error() {
                        tvError.setText(R.string.auth_error);
                        tvError.setVisibility(View.VISIBLE);
                        dlgCheck.dismiss();
                        dialog.dismiss();
                    }
                };
                version.execute(URL_PROFILE, key);
            }

            @Override
            void error() {
                try {
                    Toast toast = Toast.makeText(AuthDialog.this, getString(R.string.auth_error), Toast.LENGTH_LONG);
                    toast.show();
                    if (error_text.equals("Auth error")) {
                        bad_count++;
                        tvError.setText(R.string.auth_error);
                        edPasswd.setText("");
                        if ((max_count > 0) && (bad_count > max_count))
                            dialog.dismiss();
                    } else {
                        tvError.setText(getString(R.string.error) + "\n" + error_text);
                    }
                    tvError.setVisibility(View.VISIBLE);
                    dlgCheck.dismiss();

                } catch (Exception ex) {
                    // ignore
                }
            }
        };

        apiTask.execute(URL_KEY, login, pass);
        return;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((resultCode == RESULT_OK) && (requestCode == 1)) {
            final Vector<PhoneWithType> allNumbers = new Vector<PhoneWithType>();
            try {
                Uri result = data.getData();
                String id = result.getLastPathSegment();
                Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[]{id}, null);
                int phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA);
                int typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA2);

                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        PhoneWithType phone = new PhoneWithType();
                        phone.number = cursor.getString(phoneIdx);
                        phone.type = cursor.getInt(typeIdx);
                        allNumbers.add(phone);
                        cursor.moveToNext();
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
            if (allNumbers.size() == 0) {
                Toast toast = Toast.makeText(this, R.string.no_phone, Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            if (allNumbers.size() == 1) {
                edNumber.setText(allNumbers.get(0).number);
                return;
            }
            ListView list = new ListView(this);
            list.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return allNumbers.size();
                }

                @Override
                public Object getItem(int position) {
                    return allNumbers.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    if (v == null) {
                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        v = inflater.inflate(R.layout.number_item, null);
                    }
                    TextView tvNumber = (TextView) v.findViewById(R.id.number);
                    tvNumber.setText(allNumbers.get(position).number);
                    TextView tvType = (TextView) v.findViewById(R.id.type);
                    String type = "";
                    switch (allNumbers.get(position).type) {
                        case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                            type = getString(R.string.phone_home);
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                            type = getString(R.string.phone_work);
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                            type = getString(R.string.phone_mobile);
                            break;
                    }
                    tvType.setText(type);
                    return v;
                }
            });
            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.select_phone)
                    .setNegativeButton(R.string.cancel, null)
                    .setView(list)
                    .create();
            dialog.show();
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    edNumber.setText(allNumbers.get(position).number);
                    dialog.dismiss();
                }
            });
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    static class PhoneWithType {
        String number;
        int type;
    }
}
