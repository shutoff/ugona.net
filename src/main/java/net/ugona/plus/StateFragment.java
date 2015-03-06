package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StateFragment extends MainFragment {

    Indicator iGsm;
    Indicator iVoltage;
    Indicator iReserve;
    Indicator iBalance;
    Indicator iTemp;
    Indicator iTempEngine;
    Indicator iTempSalon;
    Indicator iFuel;
    Indicator iTemp1;
    Indicator iTemp2;

    CarView vCar;
    TextView tvAddress;
    TextView tvTime;

    Handler handler;
    BroadcastReceiver br;

    @Override
    int layout() {
        return R.layout.state;
    }

    @Override
    boolean canRefresh() {
        return true;
    }

    @Override
    void refresh() {
        super.refresh();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        iGsm = (Indicator) v.findViewById(R.id.gsm);
        iVoltage = (Indicator) v.findViewById(R.id.voltage);
        iReserve = (Indicator) v.findViewById(R.id.reserve);
        iBalance = (Indicator) v.findViewById(R.id.balance);
        iTemp = (Indicator) v.findViewById(R.id.temp);
        iTempEngine = (Indicator) v.findViewById(R.id.temp_engine);
        iTempSalon = (Indicator) v.findViewById(R.id.temp_salon);
        iFuel = (Indicator) v.findViewById(R.id.fuel);
        iTemp1 = (Indicator) v.findViewById(R.id.temp1);
        iTemp2 = (Indicator) v.findViewById(R.id.temp2);
        vCar = (CarView) v.findViewById(R.id.car);
        tvAddress = (TextView) v.findViewById(R.id.address);
        tvTime = (TextView) v.findViewById(R.id.time);
        handler = new Handler();
        update();
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                if (!id().equals(intent.getStringExtra(Names.ID)))
                    return;
                if (intent.getAction().equals(Names.CONFIG_CHANGED))
                    update();
            }
        };
        IntentFilter intFilter = new IntentFilter(Names.UPDATED);
        getActivity().registerReceiver(br, intFilter);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(br);
    }

    void update() {
        Context context = getActivity();
        if (context == null)
            return;
        CarState state = CarState.get(context, id());
        double power = state.getPower();
        if (power == 0) {
            iVoltage.setVisibility(View.GONE);
        } else {
            iVoltage.setVisibility(View.VISIBLE);
            iVoltage.setText(power + " V");
        }
        double reserved = state.getReserved();
        if (reserved == 0) {
            iReserve.setVisibility(View.GONE);
        } else {
            iReserve.setVisibility(View.VISIBLE);
            iReserve.setText(reserved + " V");
        }
        String balance = state.getBalance();
        if (balance.equals("")) {
            iBalance.setVisibility(View.GONE);
        } else {
            iBalance.setVisibility(View.VISIBLE);
            iBalance.setText(balance);
        }
        vCar.update(state);
        if (state.getTime() > 0) {
            tvTime.setText(State.formatDateTime(getActivity(), state.getTime()));
        } else {
            tvTime.setText("???");
        }
        String text = "";
        if (!state.isIgnition()) {
            long last = state.getLast_stand();
            if (last > 0)
                text = State.formatDateTime(getActivity(), last);
        }

        String gps = state.getGps();
        if (gps != null) {
            String[] g = gps.split(",");
            try {
                double lat = Double.parseDouble(g[0]);
                double lng = Double.parseDouble(g[1]);
                String address = Address.get(getActivity(), lat, lng, new Address.Answer() {
                    @Override
                    public void result(String address) {
                        if (address == null)
                            return;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                update();
                            }
                        });
                    }
                });
                if (!text.equals(""))
                    text += " ";
                text += lat + ", " + lng;
                if (address != null) {
                    text += "\n";
                    text += address;
                }
            } catch (Exception ex) {
                // ignroe
            }
        }
        tvAddress.setText(text);
    }
}
