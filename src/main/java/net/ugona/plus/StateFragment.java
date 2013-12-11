package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.LocalDateTime;

import java.text.DateFormat;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StateFragment extends Fragment
        implements View.OnTouchListener {

    String car_id;

    SharedPreferences preferences;

    ImageView imgCar;
    ImageView imgEngine;

    View vAddress;
    TextView tvTime;
    TextView tvLocation;
    TextView tvAddress2;
    TextView tvAddress3;

    TextView tvLast;
    TextView tvVoltage;
    TextView tvReserve;
    TextView tvBalance;
    TextView tvTemperature;
    TextView tvError;
    View vTemperature;
    View vError;
    ImageView imgRefresh;
    ProgressBar prgUpdate;

    View vMotor;
    View vRele;
    View vBlock;
    View vValet;
    View vPhone;

    View pMotor;
    View pRele;
    View pBlock;
    View pValet;

    ImageView ivMotor;
    ImageView ivValet;

    View mValet;
    View mNet;

    View balanceBlock;
    View vTime;

    CarDrawable drawable;
    BroadcastReceiver br;

    ActionFragment.ActionAdapter adapter;

    boolean pointer;

    static Pattern number_pattern = Pattern.compile("^[0-9]+ ?");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final Context context = getActivity();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (savedInstanceState != null)
            car_id = savedInstanceState.getString(Names.ID);

        pointer = preferences.getBoolean(Names.POINTER + car_id, false);

        View v = inflater.inflate(pointer ? R.layout.pointer : R.layout.state, container, false);

        imgCar = (ImageView) v.findViewById(R.id.car);
        imgEngine = (ImageView) v.findViewById(R.id.engine);

        vAddress = v.findViewById(R.id.address);
        tvTime = (TextView) v.findViewById(R.id.addr_time);
        tvLocation = (TextView) v.findViewById(R.id.location);
        tvAddress2 = (TextView) v.findViewById(R.id.address2);

        tvAddress3 = (TextView) v.findViewById(R.id.address3);
        tvLast = (TextView) v.findViewById(R.id.last);
        tvVoltage = (TextView) v.findViewById(R.id.voltage);
        tvReserve = (TextView) v.findViewById(R.id.reserve);
        tvBalance = (TextView) v.findViewById(R.id.balance);
        tvTemperature = (TextView) v.findViewById(R.id.temperature);
        vTemperature = v.findViewById(R.id.temperature_block);

        vMotor = v.findViewById(R.id.motor);
        vRele = v.findViewById(R.id.rele);
        vBlock = v.findViewById(R.id.block);
        vValet = v.findViewById(R.id.valet);
        vPhone = v.findViewById(R.id.phone);

        pMotor = v.findViewById(R.id.motor_prg);
        pRele = v.findViewById(R.id.rele_prg);
        pBlock = v.findViewById(R.id.block_prg);
        pValet = v.findViewById(R.id.valet_prg);

        ivMotor = (ImageView) v.findViewById(R.id.motor_img);
        ivValet = (ImageView) v.findViewById(R.id.valet_img);

        mValet = v.findViewById(R.id.valet_warning);
        mNet = v.findViewById(R.id.net_warning);

        if (!pointer) {
            vMotor.setOnTouchListener(this);
            vRele.setOnTouchListener(this);
            vBlock.setOnTouchListener(this);
            vValet.setOnTouchListener(this);
            vPhone.setOnTouchListener(this);
        }

        balanceBlock = v.findViewById(R.id.balance_block);

        tvError = (TextView) v.findViewById(R.id.error_text);
        vError = v.findViewById(R.id.error);
        vError.setVisibility(View.GONE);

        imgRefresh = (ImageView) v.findViewById(R.id.refresh);
        prgUpdate = (ProgressBar) v.findViewById(R.id.update);
        prgUpdate.setVisibility(View.GONE);

        vTime = v.findViewById(R.id.time);

        vAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMap();
            }
        });
        View v_addr = v.findViewById(R.id.addr_data);
        v_addr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMap();
            }
        });

        vTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startUpdate(context);
            }
        });

        drawable = new CarDrawable();

        update(context);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                if (intent.getAction().equals(FetchService.ACTION_UPDATE_FORCE)) {
                    update(context);
                    return;
                }
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    updateNetStatus(context);
                    return;
                }
                if (!car_id.equals(intent.getStringExtra(Names.ID)))
                    return;
                if (intent.getAction().equals(FetchService.ACTION_UPDATE)) {
                    vError.setVisibility(View.GONE);
                    imgRefresh.setVisibility(View.VISIBLE);
                    prgUpdate.setVisibility(View.GONE);
                    update(context);
                }
                if (intent.getAction().equals(SmsMonitor.SMS_SEND)) {
                    update(context);
                }
                if (intent.getAction().equals(SmsMonitor.SMS_ANSWER)) {
                    update(context);
                }
                if (intent.getAction().equals(FetchService.ACTION_START)) {
                    prgUpdate.setVisibility(View.VISIBLE);
                    imgRefresh.setVisibility(View.GONE);
                }
                if (intent.getAction().equals(FetchService.ACTION_NOUPDATE)) {
                    vError.setVisibility(View.GONE);
                    imgRefresh.setVisibility(View.VISIBLE);
                    prgUpdate.setVisibility(View.GONE);
                }
                if (intent.getAction().equals(FetchService.ACTION_ERROR)) {
                    String error_text = intent.getStringExtra(Names.ERROR);
                    if (error_text == null)
                        error_text = getString(R.string.data_error);
                    tvError.setText(error_text);
                    vError.setVisibility(View.VISIBLE);
                    imgRefresh.setVisibility(View.VISIBLE);
                    prgUpdate.setVisibility(View.GONE);
                }
            }
        };
        IntentFilter intFilter = new IntentFilter(FetchService.ACTION_UPDATE);
        intFilter.addAction(FetchService.ACTION_NOUPDATE);
        intFilter.addAction(FetchService.ACTION_ERROR);
        intFilter.addAction(FetchService.ACTION_UPDATE_FORCE);
        intFilter.addAction(SmsMonitor.SMS_SEND);
        intFilter.addAction(SmsMonitor.SMS_ANSWER);
        intFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(br, intFilter);

        if (FetchService.isProcessed(car_id)) {
            prgUpdate.setVisibility(View.VISIBLE);
            imgRefresh.setVisibility(View.GONE);
        }

        if (pointer) {
            ListView lvActions = (ListView) v.findViewById(R.id.actions);
            adapter = new ActionFragment.ActionAdapter(car_id);
            adapter.actions = new Vector<ActionFragment.Action>();
            for (ActionFragment.Action action : ActionFragment.pointer_actions) {
                adapter.actions.add(action);
            }
            adapter.attach(getActivity(), lvActions);
        }

        return v;
    }

    @Override
    public void onDestroyView() {
        if (adapter != null)
            adapter.detach(getActivity());
        getActivity().unregisterReceiver(br);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
    }

    void update(Context context) {
        long last = preferences.getLong(Names.EVENT_TIME + car_id, 0);
        if (last != 0) {
            DateFormat df = android.text.format.DateFormat.getDateFormat(context);
            DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
            tvLast.setText(df.format(last) + " " + tf.format(last));
        } else {
            tvLast.setText(getString(R.string.unknown));
        }
        tvVoltage.setText(preferences.getString(Names.VOLTAGE_MAIN + car_id, "?") + " V");
        updateNetStatus(context);

        String lat = preferences.getString(Names.LATITUDE + car_id, "");
        String lon = preferences.getString(Names.LONGITUDE + car_id, "");
        String addr = "";
        String location = "";
        if (lat.equals("") || lon.equals("")) {
            String gsm = preferences.getString(Names.GSM + car_id, "");
            if (!gsm.equals("")) {
                String[] parts = gsm.split(" ");
                location = parts[0] + "-" + parts[1] + " LAC:" + parts[2] + " CID:" + parts[3];
                addr = preferences.getString(Names.ADDRESS + car_id, "");
            }
        } else {
            location = preferences.getString(Names.LATITUDE + car_id, "") + " ";
            location += preferences.getString(Names.LONGITUDE + car_id, "");
            addr = Address.getAddress(context, car_id);
        }
        tvLocation.setText(location);
        String parts[] = addr.split(", ");
        addr = parts[0];
        int start = 2;
        if (parts.length > 1)
            addr += ", " + parts[1];
        if (parts.length > 2) {
            Matcher matcher = number_pattern.matcher(parts[2]);
            if (matcher.matches()) {
                addr += ", " + parts[2];
                start++;
            }
        }
        tvAddress2.setText(addr);
        addr = "";
        for (int i = start; i < parts.length; i++) {
            if (!addr.equals(""))
                addr += ", ";
            addr += parts[i];
        }
        tvAddress3.setText(addr);

        if (pointer)
            return;

        tvReserve.setText(preferences.getString(Names.VOLTAGE_RESERVED + car_id, "?") + " V");
        tvBalance.setText(preferences.getString(Names.BALANCE + car_id, "?"));

        int balance_limit = preferences.getInt(Names.LIMIT + car_id, 50);
        tvBalance.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
        if (balance_limit >= 0) {
            try {
                double balance = Double.parseDouble(preferences.getString(Names.BALANCE + car_id, "?"));
                if (balance <= balance_limit)
                    tvBalance.setTextColor(getResources().getColor(R.color.error));
            } catch (Exception ex) {
                // ignore
            }
        }

        String temperature = Preferences.getTemperature(preferences, car_id);
        if (temperature == null) {
            vTemperature.setVisibility(View.GONE);
        } else {
            tvTemperature.setText(temperature);
            vTemperature.setVisibility(View.VISIBLE);
        }

        Drawable d = drawable.getDrawable(context, car_id);
        if (d != null)
            imgCar.setImageDrawable(d);
        String time = "";
        long last_stand = preferences.getLong(Names.LAST_STAND + car_id, 0);
        if (last_stand > 0) {
            LocalDateTime stand = new LocalDateTime(last_stand);
            LocalDateTime now = new LocalDateTime();
            if (stand.toLocalDate().equals(now.toLocalDate())) {
                DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
                time = tf.format(last_stand);
            } else {
                DateFormat df = android.text.format.DateFormat.getDateFormat(context);
                DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
                time = df.format(last_stand) + " " + tf.format(last_stand);
            }
            time += " ";
        } else if (last_stand < 0) {
            String speed = preferences.getString(Names.SPEED + car_id, "");
            try {
                double s = Double.parseDouble(speed);
                if (s > 0)
                    time += " " + speed + " " + getString(R.string.kmh) + " ";
            } catch (Exception ex) {
                // ignore
            }
        }
        tvTime.setText(time);

        int commands = State.getCommands(preferences, car_id);

        int n_buttons = 0;
        if (State.hasTelephony(context) && ((commands & State.CMD_AZ) != 0)) {
            vMotor.setVisibility(View.VISIBLE);
            if (preferences.getBoolean(Names.AZ + car_id, false)) {
                ivMotor.setImageResource(R.drawable.icon_motor_off);
                pMotor.setVisibility(SmsMonitor.isProcessed(car_id, R.string.motor_off) ? View.VISIBLE : View.GONE);
            } else {
                ivMotor.setImageResource(R.drawable.icon_motor_on);
                pMotor.setVisibility(SmsMonitor.isProcessed(car_id, R.string.motor_on) ? View.VISIBLE : View.GONE);
            }
            n_buttons++;
        } else {
            vMotor.setVisibility(View.GONE);
        }
        if (State.hasTelephony(context) && ((commands & State.CMD_RELE) != 0)) {
            vRele.setVisibility(View.VISIBLE);
            pRele.setVisibility(SmsMonitor.isProcessed(car_id, R.string.rele) ? View.VISIBLE : View.GONE);
            n_buttons++;
        } else {
            vRele.setVisibility(View.GONE);
        }
        if (State.hasTelephony(context) && ((commands & State.CMD_BLOCK) != 0) &&
                (preferences.getBoolean(Names.INPUT3 + car_id, false) || preferences.getBoolean(Names.ZONE_IGNITION + car_id, false)) &&
                !preferences.getBoolean(Names.GUARD + car_id, false) &&
                !preferences.getBoolean(Names.AZ + car_id, false) &&
                !(!preferences.getBoolean(Names.GUARD0 + car_id, false) && preferences.getBoolean(Names.GUARD1 + car_id, false))) {
            vBlock.setVisibility(View.VISIBLE);
            pBlock.setVisibility(SmsMonitor.isProcessed(car_id, R.string.block) ? View.VISIBLE : View.GONE);
            n_buttons++;
        } else {
            vBlock.setVisibility(View.GONE);
        }
        boolean valet = preferences.getBoolean(Names.GUARD0 + car_id, false) && !preferences.getBoolean(Names.GUARD1 + car_id, false);
        if (State.hasTelephony(context) && (n_buttons < 3) && ((commands & State.CMD_VALET) != 0)) {
            if (valet) {
                ivValet.setImageResource(R.drawable.icon_valet_off);
                pValet.setVisibility(SmsMonitor.isProcessed(car_id, R.string.valet_off) ? View.VISIBLE : View.GONE);
            } else {
                ivValet.setImageResource(R.drawable.icon_valet_on);
                pValet.setVisibility(SmsMonitor.isProcessed(car_id, R.string.valet_on) ? View.VISIBLE : View.GONE);
            }
            vValet.setVisibility(View.VISIBLE);
        } else {
            vValet.setVisibility(View.GONE);
            AnimationDrawable animation = (AnimationDrawable) imgEngine.getDrawable();
            animation.stop();
        }
        if (State.hasTelephony(context) && (n_buttons < 3) && ((commands & State.CMD_CALL) != 0)) {
            vPhone.setVisibility(View.VISIBLE);
        } else {
            vPhone.setVisibility(View.GONE);
        }

        balanceBlock.setVisibility(preferences.getBoolean(Names.SHOW_BALANCE + car_id, true) ? View.VISIBLE : View.GONE);

        if (preferences.getBoolean(Names.AZ + car_id, false) && preferences.getBoolean(Names.GUARD + car_id, false)) {
            if (preferences.getBoolean(Names.GUARD0 + car_id, false) && preferences.getBoolean(Names.GUARD1 + car_id, false)) {
                imgEngine.setImageResource(R.drawable.engine_blue);
            } else {
                imgEngine.setImageResource(R.drawable.engine);
            }
            imgEngine.setVisibility(View.VISIBLE);
            startAnimation();
        } else {
            imgEngine.setVisibility(View.GONE);
        }

        mValet.setVisibility(valet ? View.VISIBLE : View.GONE);
    }

    void updateNetStatus(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        mNet.setVisibility(isConnected ? View.GONE : View.VISIBLE);
    }

    void startUpdate(Context context) {
        Intent intent = new Intent(context, FetchService.class);
        intent.putExtra(Names.ID, car_id);
        context.startService(intent);
        vError.setVisibility(View.GONE);
        imgRefresh.setVisibility(View.GONE);
        prgUpdate.setVisibility(View.VISIBLE);
    }

    void showMap() {
        if ((preferences.getString(Names.LATITUDE + car_id, "").equals("") ||
                preferences.getString(Names.LONGITUDE + car_id, "").equals("")) &&
                preferences.getString(Names.GSM_ZONE + car_id, "").equals("")) {
            Toast toast = Toast.makeText(getActivity(), R.string.no_location, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        Intent intent = new Intent(getActivity(), MapView.class);
        intent.putExtra(Names.ID, car_id);
        startActivity(intent);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.setBackgroundResource(R.drawable.button_pressed);
                return true;
            case MotionEvent.ACTION_UP:
                if (v == vMotor) {
                    if (preferences.getBoolean(Names.AZ + car_id, false)) {
                        if (SmsMonitor.isProcessed(car_id, R.string.motor_off)) {
                            SmsMonitor.cancelSMS(getActivity(), car_id, R.string.motor_off);
                            update(getActivity());
                        } else {
                            Actions.motor_off(getActivity(), car_id);
                        }
                    } else {
                        if (SmsMonitor.isProcessed(car_id, R.string.motor_on)) {
                            SmsMonitor.cancelSMS(getActivity(), car_id, R.string.motor_on);
                            update(getActivity());
                        } else {
                            Actions.motor_on(getActivity(), car_id);
                        }
                    }
                }
                if (v == vRele) {
                    if (SmsMonitor.isProcessed(car_id, R.string.rele)) {
                        SmsMonitor.cancelSMS(getActivity(), car_id, R.string.rele);
                        update(getActivity());
                    } else {
                        Actions.rele1(getActivity(), car_id);
                    }
                }
                if (v == vBlock) {
                    if (SmsMonitor.isProcessed(car_id, R.string.block)) {
                        SmsMonitor.cancelSMS(getActivity(), car_id, R.string.block);
                        update(getActivity());
                    } else {
                        Actions.block_motor(getActivity(), car_id);
                    }
                }
                if (v == vValet) {
                    if (preferences.getBoolean(Names.GUARD0 + car_id, false) && !preferences.getBoolean(Names.GUARD1, false)) {
                        if (SmsMonitor.isProcessed(car_id, R.string.valet_off)) {
                            SmsMonitor.cancelSMS(getActivity(), car_id, R.string.valet_off);
                            update(getActivity());
                        } else {
                            Actions.valet_off(getActivity(), car_id);
                        }
                    } else {
                        if (SmsMonitor.isProcessed(car_id, R.string.valet_on)) {
                            SmsMonitor.cancelSMS(getActivity(), car_id, R.string.valet_on);
                            update(getActivity());
                        } else {
                            Actions.valet_on(getActivity(), car_id);
                        }
                    }
                }
                if (v == vPhone) {
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + preferences.getString(Names.CAR_PHONE + car_id, "")));
                    startActivity(intent);
                }
            case MotionEvent.ACTION_CANCEL:
                v.setBackgroundResource(R.drawable.button_normal);
                return true;
        }
        return false;
    }

    void startAnimation() {
        if (imgEngine == null)
            return;
        if (imgEngine.getVisibility() == View.GONE)
            return;
        AnimationDrawable animation = (AnimationDrawable) imgEngine.getDrawable();
        animation.start();
    }
}
