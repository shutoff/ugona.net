package net.ugona.plus;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

public class StateFragment
        extends MainFragment
        implements View.OnClickListener {

    static final int[] temp_id = {
            R.id.temp,
            R.id.temp_engine,
            R.id.temp_salon,
            R.id.temp_ext,
            R.id.temp1,
            R.id.temp2
    };
    Indicator iGsm;
    Indicator iVoltage;
    Indicator iReserve;
    Indicator iBalance;
    Indicator iFuel;
    CarView vCar;
    TextView tvAddress;
    TextView tvTime;
    ImageView vFab;
    Handler handler;
    BroadcastReceiver br;
    ImageView ivRefresh;
    View vProgress;
    View vFabProgress;
    boolean is_address;
    boolean longTap;
    String pkg;
    Indicator[] temp_indicators;
    View vAddressView;

    @Override
    int layout() {
        return R.layout.state;
    }

    @Override
    void refresh() {
        Intent intent = new Intent(getActivity(), FetchService.class);
        intent.setAction(Names.START_UPDATE);
        intent.putExtra(Names.ID, id());
        intent.putExtra(Names.CONNECT, true);
        getActivity().startService(intent);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup v = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        iGsm = (Indicator) v.findViewById(R.id.gsm);
        iVoltage = (Indicator) v.findViewById(R.id.voltage);
        iReserve = (Indicator) v.findViewById(R.id.reserve);
        iBalance = (Indicator) v.findViewById(R.id.balance);
        iFuel = (Indicator) v.findViewById(R.id.fuel);
        vCar = (CarView) v.findViewById(R.id.car);
        tvAddress = (TextView) v.findViewById(R.id.address);
        tvTime = (TextView) v.findViewById(R.id.time);
        ivRefresh = (ImageView) v.findViewById(R.id.img_progress);
        vProgress = v.findViewById(R.id.upd_progress);
        vFab = (ImageView) v.findViewById(R.id.fab);
        vFabProgress = v.findViewById(R.id.fab_progress);
        vAddressView = v.findViewById(R.id.address_view);
        handler = new Handler();
        pkg = getActivity().getPackageName();

        temp_indicators = new Indicator[temp_id.length];
        for (int i = 0; i < temp_id.length; i++) {
            temp_indicators[i] = (Indicator) v.findViewById(temp_id[i]);
        }

        IndicatorsView indicatorsView = (IndicatorsView) v.findViewById(R.id.indocators);
        View vRightArrow = v.findViewById(R.id.ind_right);
        indicatorsView.setArrows(vRightArrow);

        vFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Vector<CarConfig.Command> fab = getFabCommands();
                if ((fab.size() == 1) && !fab.get(0).icon.equals("blocking")) {
                    final CarConfig.Command cmd = fab.get(0);
                    if (Commands.isProcessed(id(), cmd)) {
                        Commands.cancel(getActivity(), id(), cmd);
                        return;
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            SendCommandFragment fragment = new SendCommandFragment();
                            Bundle args = new Bundle();
                            args.putString(Names.ID, id());
                            args.putInt(Names.COMMAND, cmd.id);
                            args.putBoolean(Names.ROUTE, longTap);
                            fragment.setArguments(args);
                            fragment.show(getActivity().getSupportFragmentManager(), "send");
                            longTap = false;
                        }
                    });
                    return;
                }
                longTap = false;
                MainActivity activity = (MainActivity) getActivity();
                Dialog dialog = new FABCommands((MainActivity) activity, fab, id());
                dialog.show();
            }
        });
        vFab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(700);
                } catch (Exception ex) {
                    // ignore
                }
                longTap = true;
                return false;
            }
        });

        v.findViewById(R.id.status).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        v.findViewById(R.id.refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        State.forEachViews(v, new State.ViewCallback() {
            @Override
            public void view(View v) {
                if (v instanceof Indicator)
                    v.setOnClickListener(StateFragment.this);
            }
        });

        update();
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                if (!id().equals(intent.getStringExtra(Names.ID)))
                    return;
                update();
                if (!intent.getAction().equals(Names.START_UPDATE))
                    refreshDone();
            }
        };
        IntentFilter intFilter = new IntentFilter(Names.UPDATED);
        intFilter.addAction(Names.ERROR);
        intFilter.addAction(Names.START_UPDATE);
        intFilter.addAction(Names.NO_UPDATED);
        intFilter.addAction(Names.COMMANDS);
        intFilter.addAction(Names.CONFIG_CHANGED);
        getActivity().registerReceiver(br, intFilter);
        Intent intent = new Intent(getActivity(), FetchService.class);
        intent.setAction(FetchService.ACTION_UPDATE);
        intent.putExtra(Names.ID, id());
        getActivity().startService(intent);
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
        CarConfig config = CarConfig.get(context, id());
        if (FetchService.isProcessed(id())) {
            vProgress.setVisibility(View.VISIBLE);
            ivRefresh.setImageResource(R.drawable.refresh);
        } else {
            vProgress.setVisibility(View.INVISIBLE);
            ivRefresh.setImageResource(state.isOnline() ? R.drawable.refresh_on : R.drawable.refresh_off);
        }

        NumberFormat formatter = NumberFormat.getInstance(getResources().getConfiguration().locale);
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(2);

        double power = state.getPower();
        if (power == 0) {
            iVoltage.setVisibility(View.GONE);
        } else {
            iVoltage.setVisibility(View.VISIBLE);
            iVoltage.setText(formatter.format(power) + " V");
            setPowerState(iVoltage, state.getPower_state());
        }
        double reserved = state.getReserved();
        if (reserved == 0) {
            iReserve.setVisibility(View.GONE);
        } else {
            iReserve.setVisibility(View.VISIBLE);
            iReserve.setText(formatter.format(reserved) + " V");
            setPowerState(iReserve, state.getReserved_state());
        }
        String balance = state.getBalance();
        if (balance.equals("") || config.isHideBalance()) {
            iBalance.setVisibility(View.GONE);
        } else {
            iBalance.setVisibility(View.VISIBLE);
            try {
                NumberFormat nf = NumberFormat.getCurrencyInstance();
                double value = Double.parseDouble(balance);
                String str = nf.format(value);
                Currency currency = Currency.getInstance(Locale.getDefault());
                str = str.replace(currency.getSymbol(), "");
                iBalance.setText(str);
                int balance_limit = config.getBalance_limit();
                if (value <= balance_limit) {
                    iBalance.setTextColor(getResources().getColor(R.color.error));
                } else {
                    iBalance.setTextColor(getResources().getColor(R.color.indiicator));
                }
            } catch (Exception ex) {
                iBalance.setText(balance);
                iBalance.setTextColor(getResources().getColor(R.color.indiicator));
            }
        }
        int gsm_level = state.getGsm_level();
        if (gsm_level == 0) {
            iGsm.setVisibility(View.GONE);
        } else {
            iGsm.setVisibility(View.VISIBLE);
            iGsm.setText(gsm_level + " dBm");
            String pict = "ind02_" + State.GsmLevel(gsm_level);
            iGsm.setImage(getResources().getIdentifier(pict, "drawable", getActivity().getPackageName()));
        }
        SparseIntArray temps = new SparseIntArray();
        String temperature = state.getTemperature();
        if (temperature != null) {
            String[] parts = temperature.split(",");
            for (String p : parts) {
                String[] v = p.split(":");
                try {
                    int pos = Integer.parseInt(v[0]);
                    int val = Integer.parseInt(v[1]);
                    temps.put(pos, val);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        for (int i = 0; i < temp_indicators.length; i++) {
            int v = temps.get(i, -100);
            if (v == -100) {
                temp_indicators[i].setVisibility(View.GONE);
                continue;
            }
            temp_indicators[i].setVisibility(View.VISIBLE);
            temp_indicators[i].setText(v + " \u00B0C");
        }

        if (state.getFuel() > 0) {
            iFuel.setVisibility(View.VISIBLE);
            formatter = NumberFormat.getInstance(getResources().getConfiguration().locale);
            iFuel.setText(formatter.format(state.getFuel()) + " L");
        } else {
            iFuel.setVisibility(View.GONE);
        }

        vCar.update(state);
        if (state.getTime() > 0) {
            tvTime.setText(State.formatDateTime(getActivity(), state.getTime()));
        } else {
            tvTime.setText("???");
        }
        StringBuilder text = new StringBuilder();
        if (!state.isIgnition()) {
            long last = state.getLast_stand();
            if (last > 0) {
                text.append("|");
                text.append(State.formatDateTime(getActivity(), last));
                text.append("|");
            }
        } else if (state.isGps_valid()) {
            text.append("|");
            text.append(state.getSpeed());
            text.append(" ");
            text.append(getString(R.string.kmh));
            text.append("|");
        }

        String gps = state.getGps();
        if (gps != null) {
            String[] g = gps.split(",");
            try {
                double lat = Double.parseDouble(g[0]);
                double lng = Double.parseDouble(g[1]);
                is_address = true;
                String address = Address.get(getActivity(), lat, lng, new Address.Answer() {
                    @Override
                    public void result(String address) {
                        if ((address == null) || is_address)
                            return;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                update();
                            }
                        });
                    }
                });
                is_address = false;
                if (!text.toString().equals(""))
                    text.append(" ");
                text.append(lat);
                text.append(", ");
                text.append(lng);
                if (address != null) {
                    text.append("\n|");
                    String[] parts = address.split(", ");
                    text.append(parts[0]);
                    if (parts.length > 1) {
                        text.append(", ");
                        text.append(parts[1]);
                    }
                    if (parts.length > 2) {
                        text.append("|\n");
                        text.append(parts[2]);
                        for (int i = 3; i < parts.length; i++) {
                            text.append(", ");
                            text.append(parts[i]);
                        }
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        tvAddress.setText(State.createSpans(text.toString(), getResources().getColor(android.R.color.white), true));
        Vector<CarConfig.Command> fab = getFabCommands();
        if (fab.size() > 0) {
            vFab.setVisibility(View.VISIBLE);
            boolean more = true;
            if ((fab.size() == 1) && !fab.get(0).icon.equals("blocking")) {
                CarConfig.Command cmd = fab.get(0);
                int res_id = getResources().getIdentifier("b_" + cmd.icon, "drawable", pkg);
                if (res_id != 0) {
                    vFab.setImageResource(res_id);
                    more = false;
                    vFabProgress.setVisibility(Commands.isProcessed(id(), cmd) ? View.VISIBLE : View.GONE);
                }
            }
            if (more) {
                vFab.setImageResource(R.drawable.fab_more);
                vFabProgress.setVisibility(View.GONE);
            }
        } else {
            vFab.setVisibility(View.GONE);
            vFabProgress.setVisibility(View.GONE);
        }
    }

    void setPowerState(Indicator ind, int state) {
        int color = R.color.indiicator;
        switch (state) {
            case 0:
                color = R.color.error;
                break;
            case 2:
                color = R.color.neutral;
                break;
        }
        ind.setTextColor(getResources().getColor(color));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.voltage:
            case R.id.reserve:
            case R.id.balance:
            case R.id.temp:
            case R.id.temp_engine:
            case R.id.temp_salon:
            case R.id.temp_ext:
            case R.id.temp1:
            case R.id.temp2:
            case R.id.fuel:
                CarState state = CarState.get(getActivity(), id());
                if (!state.isHistory())
                    return;
                Bundle args = new Bundle();
                args.putString(Names.ID, id());
                args.putInt(Names.TITLE, v.getId());
                HistoryFragment fragment = new HistoryFragment();
                fragment.setArguments(args);
                MainActivity activity = (MainActivity) getActivity();
                activity.setFragment(fragment);
        }
    }

    Vector<CarConfig.Command> getFabCommands() {
        CarConfig config = CarConfig.get(getActivity(), id());
        CarState state = CarState.get(getActivity(), id());
        int[] selected = config.getCommands();
        CarConfig.Command[] cmds = config.getCmd();
        Set<String> enabled = new HashSet<>();
        Vector<CarConfig.Command> res = new Vector<>();
        for (CarConfig.Command cmd : cmds) {
            boolean enable = false;
            for (int s : selected) {
                if (cmd.id == s) {
                    enable = true;
                    break;
                }
            }
            String group = cmd.group;
            if ((group != null) && group.equals(""))
                group = null;
            if (group != null) {
                if (enable) {
                    enabled.add(group);
                } else {
                    enable = enabled.contains(group);
                }
            }
            if (!enable)
                enable = cmd.always;
            if (!enable)
                continue;
            if (cmd.condition != null) {
                if (!State.checkCondition(cmd.condition, state))
                    continue;
            }
            res.add(cmd);
        }
        return res;
    }

}
