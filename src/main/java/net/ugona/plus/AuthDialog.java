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
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Locale;
import java.util.Vector;

import javax.crypto.Cipher;

public class AuthDialog extends Activity {

    final static String URL_KEY = "https://car-online.ugona.net/key?auth=$1";
    final static String URL_PROFILE = "https://car-online.ugona.net/version?skey=$1";
    EditText edLogin;
    EditText edPasswd;
    EditText edNumber;
    TextView tvError;
    boolean show_auth;
    boolean show_phone;
    String car_id;
    SharedPreferences preferences;

    static String crypt(String data) {
        try {
            String key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApZ3oK9Ia0HdUFQ3iP6/OP94MrlnYhnV5RadTkHJsS+KxJshy81psMcFgI0/FYPpV3B6arQk9wJ7+NMj4kpnToxyVALwYNpT4/2+CN7igN48dZ62DflP7h6lDsLS0Mksly+LEKCrZiT4tkHLyAVI5HQekxfi9b+oVI9Rkp7CkKqXwVruRykaRczV/mZKT5IulPe4gIy8yDf6z6IJt84qfKMq47fbHRfiQdV0WlBP023fTBaLDqQO9FBmL8uNC9AkQAdjZo30j3mpcpCb4X9RiB7Hf1hczBLmCL9kQLZBkSdGLiwbeamDVhthuAvn4K2CFoXUGwmwSja6DZJSfU69+awIDAQAB";
            byte[] sigBytes = Base64.decode(key, Base64.DEFAULT);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = factory.generatePublic(new X509EncodedKeySpec(sigBytes));
            byte[] byteData = data.getBytes();
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedByteData = cipher.doFinal(byteData);
            String s = Base64.encodeToString(encryptedByteData, Base64.NO_PADDING);
            return s;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

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

        show_auth = getIntent().getBooleanExtra(Names.AUTH, false);
        show_phone = getIntent().getBooleanExtra(Names.CAR_PHONE, false);

        if (show_auth || show_phone) {
            car_id = getIntent().getStringExtra(Names.ID);
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
        if (show_auth)
            builder.setMessage(R.string.auth_summary);
        final AlertDialog dialog = builder.create();
        dialog.show();
        edNumber = (EditText) dialog.findViewById(R.id.number);
        edLogin = (EditText) dialog.findViewById(R.id.login);
        edPasswd = (EditText) dialog.findViewById(R.id.passwd);
        tvError = (TextView) dialog.findViewById(R.id.error);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        final Button btnOk = dialog.getButton(Dialog.BUTTON_POSITIVE);
        btnOk.setEnabled(false);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = getIntent();
                if (show_auth) {
                    final ProgressDialog dlgCheck = new ProgressDialog(AuthDialog.this);
                    dlgCheck.setMessage(getString(R.string.check_auth));
                    dlgCheck.show();

                    final String login = edLogin.getText().toString();
                    final String pass = edPasswd.getText().toString();
                    final String auth = crypt(login + "\0" + pass);

                    HttpTask apiTask = new HttpTask() {
                        @Override
                        void result(JsonObject res) throws ParseException {
                            String key = res.get("key").asString();
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putString(Names.CAR_KEY + car_id, key);
                            ed.putString(Names.LOGIN + car_id, login);
                            ed.putString(Names.AUTH + car_id, auth);
                            ed.remove(Names.GCM_TIME);
                            String[] cars = preferences.getString(Names.CARS, "").split(",");
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
                                        ed.putString(Names.CAR_PHONE + car_id, number);
                                    }
                                    String ver = res.get("version").asString();
                                    ed.putString(Names.VERSION + car_id, ver);
                                    if (ver.toUpperCase().substring(0, 5).equals("MS-TR"))
                                        ed.putBoolean(Names.POINTER + car_id, true);
                                    ed.commit();
                                    setResult(RESULT_OK, getIntent());
                                    dlgCheck.dismiss();
                                    dialog.dismiss();
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
                                tvError.setText(R.string.auth_error);
                                tvError.setVisibility(View.VISIBLE);
                                dlgCheck.dismiss();
                            } catch (Exception ex) {
                                // ignore
                            }
                        }
                    };

                    apiTask.execute(URL_KEY, auth);
                    return;
                }
                if (show_phone) {
                    String number = edNumber.getText().toString();
                    if (!State.isDebug())
                        number = Phones.formatPhoneNumber(number);
                    if (car_id == null) {
                        i.putExtra(Names.CAR_PHONE, number);
                    } else {
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.putString(Names.CAR_PHONE + car_id, number);
                        ed.commit();
                    }
                }
                setResult(RESULT_OK, i);
                dialog.dismiss();
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
            edLogin.setText(preferences.getString(Names.LOGIN + car_id, ""));
            edLogin.addTextChangedListener(watcher);
            edPasswd.addTextChangedListener(watcher);
            edLogin.requestFocus();
        } else {
            dialog.findViewById(R.id.login_row).setVisibility(View.GONE);
            dialog.findViewById(R.id.passwd_row).setVisibility(View.GONE);
        }

        if (show_phone) {
            if (car_id != null)
                edNumber.setText(preferences.getString(Names.CAR_PHONE + car_id, ""));
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
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, 1);
            }
        });
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
