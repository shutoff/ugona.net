package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.LocalDateTime;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StateFragment extends Fragment {

    String car_id;

    SharedPreferences preferences;

    ImageView imgCar;
    TextView tvAddress;
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

    ImageView ivMotor;
    ImageView ivRele;
    ImageView ivBlock;
    ImageView ivValet;
    View balanceBlock;
    View vTime;

    CarDrawable drawable;
    BroadcastReceiver br;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.state, container, false);

        if (savedInstanceState != null)
            car_id = savedInstanceState.getString(Names.ID);

        imgCar = (ImageView) v.findViewById(R.id.car);
        tvAddress = (TextView) v.findViewById(R.id.address);
        tvLast = (TextView) v.findViewById(R.id.last);
        tvVoltage = (TextView) v.findViewById(R.id.voltage);
        tvReserve = (TextView) v.findViewById(R.id.reserve);
        tvBalance = (TextView) v.findViewById(R.id.balance);
        tvTemperature = (TextView) v.findViewById(R.id.temperature);
        vTemperature = v.findViewById(R.id.temperature_block);

        ivMotor = (ImageView) v.findViewById(R.id.motor);
        ivRele = (ImageView) v.findViewById(R.id.rele);
        ivBlock = (ImageView) v.findViewById(R.id.block);
        ivValet = (ImageView) v.findViewById(R.id.valet);

        balanceBlock = v.findViewById(R.id.balance_block);

        tvError = (TextView) v.findViewById(R.id.error_text);
        vError = v.findViewById(R.id.error);
        vError.setVisibility(View.GONE);

        imgRefresh = (ImageView) v.findViewById(R.id.refresh);
        prgUpdate = (ProgressBar) v.findViewById(R.id.update);
        prgUpdate.setVisibility(View.GONE);

        vTime = v.findViewById(R.id.time);

        final Context context = getActivity();

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        ivMotor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (preferences.getBoolean(Names.INPUT3 + car_id, false)) {
                    Actions.motorOff(context, car_id);
                } else {
                    Actions.motorOn(context, car_id);
                }
            }
        });
        ivRele.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Actions.rele1(context, car_id);
            }
        });
        ivBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Actions.blockMotor(context, car_id);
            }
        });
        ivValet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Preferences.getValet(preferences, car_id)) {
                    Actions.valetOff(context, car_id);
                } else {
                    Actions.valetOn(context, car_id);
                }
            }
        });

        vTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startUpdate(context);
            }
        });

        drawable = new CarDrawable(context);
        imgCar.setImageDrawable(drawable.getDrawable());

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
                if (!car_id.equals(intent.getStringExtra(Names.ID)))
                    return;
                if (intent.getAction().equals(FetchService.ACTION_UPDATE)) {
                    vError.setVisibility(View.GONE);
                    imgRefresh.setVisibility(View.VISIBLE);
                    prgUpdate.setVisibility(View.GONE);
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
        context.registerReceiver(br, intFilter);

        return v;
    }

    @Override
    public void onDestroyView() {
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
            Date d = new Date(last);
            SimpleDateFormat sf = new SimpleDateFormat();
            tvLast.setText(sf.format(d));
        } else {
            tvLast.setText(getString(R.string.unknown));
        }
        tvVoltage.setText(preferences.getString(Names.VOLTAGE_MAIN + car_id, "?") + " V");
        tvReserve.setText(preferences.getString(Names.VOLTAGE_RESERVED + car_id, "?") + " V");
        tvBalance.setText(preferences.getString(Names.BALANCE + car_id, "?"));
        String temperature = Preferences.getTemperature(preferences, car_id);
        if (temperature == null) {
            vTemperature.setVisibility(View.GONE);
        } else {
            tvTemperature.setText(temperature);
            vTemperature.setVisibility(View.VISIBLE);
        }

        drawable.update(preferences, car_id);
        String address = "";
        long last_stand = preferences.getLong(Names.LAST_STAND + car_id, 0);
        if (last_stand > 0) {
            LocalDateTime stand = new LocalDateTime(last_stand);
            LocalDateTime now = new LocalDateTime();
            if (stand.toLocalDate().equals(now.toLocalDate())) {
                address = stand.toString("HH:mm");
            } else {
                address = stand.toString("d-MM-yy HH:mm");
            }
            address += " ";
        } else if (last_stand < 0) {
            String speed = preferences.getString(Names.SPEED + car_id, "");
            try {
                double s = Double.parseDouble(speed);
                if (s > 0)
                    address += String.format(getString(R.string.speed, speed)) + " ";
            } catch (Exception ex) {
                // ignore
            }
        }
        String lat = preferences.getString(Names.LATITUDE + car_id, "");
        String lon = preferences.getString(Names.LONGITUDE + car_id, "");
        if (lat.equals("") || lon.equals("")) {
            String gsm = preferences.getString(Names.GSM + car_id, "");
            if (!gsm.equals("")) {
                String[] parts = gsm.split(" ");
                address += "MCC: " + parts[0] + " NC: " + parts[1] + " LAC: " + parts[2] + " CID: " + parts[3];
                String addr = preferences.getString(Names.ADDRESS + car_id, "");
                if (!addr.equals(""))
                    address += "\n" + addr;
            }
        } else {
            address += preferences.getString(Names.LATITUDE + car_id, "") + " ";
            address += preferences.getString(Names.LONGITUDE + car_id, "") + "\n";
            address += Address.getAddress(context, car_id);
        }
        tvAddress.setText(address);
        tvAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMap();
            }
        });

        if (preferences.getBoolean(Names.CAR_AUTOSTART + car_id, false)) {
            ivMotor.setVisibility(View.VISIBLE);
            if (preferences.getBoolean(Names.INPUT3 + car_id, false)) {
                ivMotor.setImageResource(R.drawable.motor_off);
            } else {
                ivMotor.setImageResource(R.drawable.motor_on);
            }
        } else {
            ivMotor.setVisibility(View.GONE);
        }
        if (preferences.getBoolean(Names.CAR_RELE1 + car_id, false)) {
            ivRele.setVisibility(View.VISIBLE);
        } else {
            ivRele.setVisibility(View.GONE);
        }
        if (Preferences.getValet(preferences, car_id)) {
            ivValet.setImageResource(R.drawable.valet_btn_off);
        } else {
            ivValet.setImageResource(R.drawable.valet_btn_on);
        }
        if (preferences.getBoolean(Names.INPUT3 + car_id, false)) {
            ivBlock.setVisibility(View.VISIBLE);
        } else {
            ivBlock.setVisibility(View.GONE);
        }

        balanceBlock.setVisibility(preferences.getBoolean(Names.SHOW_BALANCE + car_id, true) ? View.VISIBLE : View.GONE);
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
}
