package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class SettingActivity extends ActionBarActivity {

    String car_id;
    SharedPreferences preferences;
    BroadcastReceiver br;

    int[] values;
    int[] old_values;

    boolean values_error;
    boolean az;
    boolean rele;

    ActionBar.TabListener tabListener;

    final static String UPDATE_SETTINGS = "net.ugona.plus.UPDATE_SETTINGS";

    final static String URL_SETTINGS = "https://car-online.ugona.net/settings?auth=$1";
    final static String URL_SET = "https://car-online.ugona.net/set?auth=$1&v=$2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager);

        if (savedInstanceState != null) {
            car_id = savedInstanceState.getString(Names.ID);
        } else {
            car_id = getIntent().getStringExtra(Names.ID);
            if (car_id == null)
                car_id = preferences.getString(Names.LAST, "");
        }

        final ViewPager pager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

            }
        };

        pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int pos) {
                ActionBar b = getSupportActionBar();
                try {
                    b.setSelectedNavigationItem(pos);
                    View action_bar_view = findViewById(getResources().getIdentifier("action_bar", "id", "android"));
                    Class<?> action_bar_class = action_bar_view.getClass();
                    Field tab_scroll_view_prop = action_bar_class.getDeclaredField("mTabScrollView");
                    tab_scroll_view_prop.setAccessible(true);
                    Object tab_scroll_view = tab_scroll_view_prop.get(action_bar_view);
                    if (tab_scroll_view == null) return;
                    Field spinner_prop = tab_scroll_view.getClass().getDeclaredField("mTabSpinner");
                    spinner_prop.setAccessible(true);
                    Object tab_spinner = spinner_prop.get(tab_scroll_view);
                    if (tab_spinner == null) return;
                    Method set_selection_method = tab_spinner.getClass().getSuperclass().getDeclaredMethod("setSelection", Integer.TYPE, Boolean.TYPE);
                    set_selection_method.invoke(tab_spinner, pos, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        int commands = State.getCommands(preferences, car_id);
        if ((commands & State.CMD_AZ) != 0)
            az = true;
        if ((commands & State.CMD_RELE) != 0)
            rele = true;

        for (int i = 0; i < adapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(adapter.getPageTitle(i))
                            .setTabListener(tabListener));
        }
        setResult(RESULT_CANCELED);

        if (savedInstanceState != null) {
            values = savedInstanceState.getIntArray("values");
            old_values = savedInstanceState.getIntArray("old_values");
        }

        if (values == null)
            updateSettings();

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (car_id.equals(intent.getStringExtra(Names.ID))) {
                    int commands = State.getCommands(preferences, car_id);
                    boolean new_az = (commands & State.CMD_AZ) != 0;
                    boolean new_rele = (commands & State.CMD_RELE) != 0;
                    if ((az == new_az) && (rele == new_rele))
                        return;
                    adapter.notifyDataSetChanged();
                    if (az != new_az) {
                        int index = 4;
                        if (new_az) {
                            actionBar.addTab(
                                    actionBar.newTab()
                                            .setText(adapter.getPageTitle(index))
                                            .setTabListener(tabListener), index);
                        } else {
                            actionBar.removeTabAt(index);
                        }
                        az = new_az;
                    }
                    if (rele != new_rele) {
                        int index = 4;
                        if (az)
                            index++;
                        if (new_az) {
                            actionBar.addTab(
                                    actionBar.newTab()
                                            .setText(adapter.getPageTitle(index))
                                            .setTabListener(tabListener), index);
                        } else {
                            actionBar.removeTabAt(index);
                        }
                        rele = new_rele;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(FetchService.ACTION_UPDATE_FORCE);
        registerReceiver(br, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (values != null) {
            outState.putIntArray("values", values);
            outState.putIntArray("old_values", old_values);
        }
    }

    void updateSettings() {
        values_error = false;
        HttpTask task = new HttpTask() {

            @Override
            void result(JsonObject res) throws ParseException {
                SharedPreferences.Editor ed = preferences.edit();
                values = new int[22];
                old_values = new int[22];
                for (int i = 0; i < 22; i++) {
                    int v = res.get("v" + i).asInt();
                    ed.putInt("V_" + i + "_" + car_id, v);
                    values[i] = v;
                    old_values[i] = v;
                }
                ed.commit();
                sendUpdate();
            }

            @Override
            void error() {
                values_error = true;
                sendUpdate();
            }
        };
        task.execute(URL_SETTINGS, preferences.getString(Names.AUTH + car_id, ""));
    }

    void sendUpdate() {
        Intent i = new Intent(UPDATE_SETTINGS);
        i.putExtra(Names.ID, car_id);
        sendBroadcast(i);
    }

    void update() {
        if (values == null) {
            if (values_error) {
                values_error = false;
                updateSettings();
                sendUpdate();
            }
            return;
        }
        String val = "";
        for (int i = 0; i < values.length; i++) {
            if (values[i] == old_values[i])
                continue;
            if (!val.equals(""))
                val += ",";
            val += i + "." + values[i];
        }
        if (val.equals(""))
            return;
        final String value = val;
        Actions.requestPassword(this, R.string.setup, R.string.setup_msg, new Runnable() {
            @Override
            public void run() {
                final ProgressDialog progressDialog = new ProgressDialog(SettingActivity.this);
                progressDialog.setMessage(getString(R.string.send_command));
                progressDialog.show();

                HttpTask task = new HttpTask() {

                    @Override
                    void result(JsonObject res) throws ParseException {
                        progressDialog.dismiss();
                        LayoutInflater inflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                        final AlertDialog dialog = new AlertDialog.Builder(SettingActivity.this)
                                .setTitle(R.string.setup)
                                .setView(inflater.inflate(R.layout.wait, null))
                                .setNegativeButton(R.string.ok, null)
                                .create();
                        dialog.show();
                        TextView tv = (TextView) dialog.findViewById(R.id.msg);
                        final int wait_time = preferences.getInt(Names.CAR_TIMER + car_id, 10);
                        String msg = getString(R.string.wait_msg).replace("$1", wait_time + "");
                        tv.setText(msg);
                        Button btnCall = (Button) dialog.findViewById(R.id.call);
                        btnCall.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                                Intent intent = new Intent(Intent.ACTION_CALL);
                                intent.setData(Uri.parse("tel:" + preferences.getString(Names.CAR_PHONE + car_id, "")));
                                startActivity(intent);
                            }
                        });
                        Button btnSms = (Button) dialog.findViewById(R.id.sms);
                        btnSms.setVisibility(View.GONE);
                    }

                    @Override
                    void error() {
                        progressDialog.dismiss();
                        Toast toast = Toast.makeText(SettingActivity.this, R.string.data_error, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                };
                task.execute(URL_SET, preferences.getString(Names.AUTH + car_id, ""), value);
            }
        });
    }

    public class PagerAdapter extends FragmentPagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            SettingsFragment res = null;
            switch (i) {
                case 0:
                    res = new AuthFragment();
                    break;
                case 1:
                    res = new NotificationFragment();
                    break;
                case 2:
                    res = new CommandsFragment();
                    break;
                case 3:
                    res = new DeviceSettingsFragment();
                    break;
                case 4:
                    if ((State.getCommands(preferences, car_id) & State.CMD_AZ) != 0) {
                        res = new AutoStartFragment();
                        break;
                    }
                case 5:
                    res = new HeaterFragment();
                    break;
            }
            if (res == null)
                return null;
            res.car_id = car_id;
            return res;
        }

        @Override
        public int getCount() {
            int count = 4;
            int commands = State.getCommands(preferences, car_id);
            if ((commands & State.CMD_AZ) != 0)
                count++;
            if ((commands & State.CMD_RELE) != 0)
                count++;
            return count;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.auth);
                case 1:
                    return getString(R.string.notifications);
                case 2:
                    return getString(R.string.commands);
                case 3:
                    return getString(R.string.device_settings);
                case 4:
                    if ((State.getCommands(preferences, car_id) & State.CMD_AZ) != 0)
                        return getString(R.string.autostart);
                case 5:
                    return getString(R.string.rele);
            }
            return null;
        }
    }

    final static String URL_KEY = "https://car-online.ugona.net/key?auth=$1";
    final static String URL_PROFILE = "https://car-online.ugona.net/version?skey=$1";
    final static String URL_PHOTOS = "https://car-online.ugona.net/photos?skey=$1&begin=$2";

    static boolean apiKeyShow = false;

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

    static void getApiKey(final Context context, final String car_id, final Runnable onDone, final Runnable onEnd) {
        if (apiKeyShow)
            return;
        apiKeyShow = true;

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.auth)
                .setMessage(R.string.auth_summary)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .setView(inflater.inflate(R.layout.apikeydialog, null))
                .create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                apiKeyShow = false;
                if (onEnd != null)
                    onEnd.run();
            }
        });

        final EditText edLogin = (EditText) dialog.findViewById(R.id.login);
        final EditText edPasswd = (EditText) dialog.findViewById(R.id.passwd);
        final TextView tvError = (TextView) dialog.findViewById(R.id.error);

        final Button btnSave = dialog.getButton(Dialog.BUTTON_POSITIVE);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                btnSave.setEnabled(!edLogin.getText().toString().equals("") && !edPasswd.getText().toString().equals(""));
                tvError.setVisibility(View.GONE);
            }
        };
        edLogin.addTextChangedListener(watcher);
        edPasswd.addTextChangedListener(watcher);
        btnSave.setEnabled(false);

        edLogin.setText(preferences.getString(Names.LOGIN + car_id, ""));

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog dlgCheck = new ProgressDialog(context);
                dlgCheck.setMessage(context.getString(R.string.check_auth));
                dlgCheck.show();

                final String login = edLogin.getText().toString();
                final String pass = edPasswd.getText().toString();
                final String auth = crypt(login + "\0" + pass);

                HttpTask apiTask = new HttpTask() {
                    @Override
                    void result(JsonObject res) throws ParseException {
                        dlgCheck.dismiss();
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
                        dialog.dismiss();
                        onDone.run();
                    }

                    @Override
                    void error() {
                        Toast toast = Toast.makeText(context, context.getString(R.string.auth_error), Toast.LENGTH_LONG);
                        toast.show();
                        tvError.setText(R.string.auth_error);
                        tvError.setVisibility(View.VISIBLE);
                        dlgCheck.dismiss();
                    }
                };

                apiTask.execute(URL_KEY, auth);
            }
        });
    }

}
