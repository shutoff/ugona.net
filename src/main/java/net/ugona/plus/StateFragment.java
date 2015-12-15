package net.ugona.plus;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import java.util.Date;
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
    static final int[] info_id = {
            R.id.car_img1,
            R.id.car_img2,
            R.id.car_img3
    };

    Indicator iGsm;
    Indicator iVoltage;
    Indicator iReserve;
    Indicator iBalance;
    Indicator iFuel;
    Indicator iCard;
    CarView vCar;
    TextView tvAddress;
    TextView tvTime;
    TextView tvError;
    ImageView vFab;
    Handler handler;
    BroadcastReceiver br;
    ImageView ivRefresh;
    View vProgress;
    View vFabProgress;
    boolean longTap;
    String pkg;
    Indicator[] temp_indicators;
    CenteredScrollView vAddressView;
    String error;
    ImageView vPointers;
    TextView[] tvPointers;
    HashSet<String> pointer_errors;
    View vInfo;
    ImageView[] ivInfo;
    TextView tvInfo;
    TextView tvMaintenance;

    @Override
    int layout() {
        return R.layout.state;
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        refreshDone();
        refresh();
    }

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
        iCard = (Indicator) v.findViewById(R.id.card);
        vCar = (CarView) v.findViewById(R.id.car);
        tvAddress = (TextView) v.findViewById(R.id.address);
        tvTime = (TextView) v.findViewById(R.id.time);
        tvError = (TextView) v.findViewById(R.id.error);
        ivRefresh = (ImageView) v.findViewById(R.id.img_progress);
        vProgress = v.findViewById(R.id.upd_progress);
        vFab = (ImageView) v.findViewById(R.id.fab);
        vFabProgress = v.findViewById(R.id.fab_progress);
        vAddressView = (CenteredScrollView) v.findViewById(R.id.address_view);

        View.OnClickListener pointerClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pointer = (Integer) v.getTag();
                CarConfig config = CarConfig.get(getActivity(), id());
                int[] pointers = config.getPointers();
                if (pointers == null)
                    return;
                if (pointer >= pointers.length)
                    return;
                PointerFragment fragment = new PointerFragment();
                Bundle args = new Bundle();
                args.putString(Names.ID, id() + "_" + pointers[pointer]);
                fragment.setArguments(args);
                MainActivity activity = (MainActivity) getActivity();
                activity.setFragment(fragment);
            }
        };

        vPointers = (ImageView) v.findViewById(R.id.pointer);
        vPointers.setTag(0);
        vPointers.setOnClickListener(pointerClick);
        tvPointers = new TextView[2];
        tvPointers[0] = (TextView) v.findViewById(R.id.pointer1);
        tvPointers[0].setTag(0);
        tvPointers[0].setOnClickListener(pointerClick);
        tvPointers[1] = (TextView) v.findViewById(R.id.pointer2);
        tvPointers[1].setTag(1);
        tvPointers[1].setOnClickListener(pointerClick);
        handler = new Handler();
        pkg = getActivity().getPackageName();

        temp_indicators = new Indicator[temp_id.length];
        for (int i = 0; i < temp_id.length; i++) {
            temp_indicators[i] = (Indicator) v.findViewById(temp_id[i]);
        }

        vInfo = v.findViewById(R.id.car_info);
        tvInfo = (TextView) v.findViewById(R.id.car_text);
        ivInfo = new ImageView[info_id.length];
        for (int i = 0; i < info_id.length; i++) {
            ivInfo[i] = (ImageView) v.findViewById(info_id[i]);
        }

        tvMaintenance = (TextView) v.findViewById(R.id.maintenance);
        tvMaintenance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity activity = (MainActivity) getActivity();
                activity.setFragment(new MaintenanceFragment());
            }
        });

        IndicatorsView indicatorsView = (IndicatorsView) v.findViewById(R.id.indicators);
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

        View vStatus = v.findViewById(R.id.status);
        if (vStatus != null) {
            vStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CarState carState = CarState.get(getActivity(), id());
                    if (carState.getGps().equals(""))
                        return;
                    Intent intent = new Intent(getActivity(), MapPointActivity.class);
                    intent.putExtra(Names.ID, id());
                    startActivity(intent);
                }
            });
        }
        v.findViewById(R.id.address_block).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CarState carState = CarState.get(getActivity(), id());
                if (carState.getGps().equals(""))
                    return;
                Intent intent = new Intent(getActivity(), MapPointActivity.class);
                intent.putExtra(Names.ID, id());
                startActivity(intent);
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
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    update();
                    return;
                }
                if (intent.getAction().equals(Names.ADDRESS_UPDATE)) {
                    update();
                    return;
                }
                String upd_id = intent.getStringExtra(Names.ID);
                if (upd_id == null)
                    return;
                String[] upd = upd_id.split("_");
                if (!upd[0].equals(id()) && !upd_id.equals(id()))
                    return;

                if (!upd_id.equals(id())) {
                    if (intent.getAction().equals(Names.ERROR)) {
                        if (pointer_errors == null)
                            pointer_errors = new HashSet<>();
                        pointer_errors.add(upd[1]);
                        update();
                        return;
                    }
                    if (pointer_errors == null)
                        return;
                    if (intent.getAction().equals(Names.UPDATED) || intent.getAction().equals(Names.NO_UPDATED)) {
                        pointer_errors.remove(upd[1]);
                        update();
                    }
                    return;
                }

                if (intent.getAction().equals(Names.ERROR)) {
                    error = intent.getStringExtra(Names.ERROR);
                    if (error == null)
                        error = getString(R.string.data_error);
                    update();
                    if (error.equals("Auth error")) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity activity = (MainActivity) getActivity();
                                if (activity == null)
                                    return;
                                if (activity.isFragmentShow("auth_dialog"))
                                    return;
                                AuthDialog authDialog = new AuthDialog();
                                Bundle args = new Bundle();
                                args.putString(Names.ID, id());
                                authDialog.setArguments(args);
                                authDialog.show(getParentFragment().getFragmentManager(), "auth_dialog");
                            }
                        });
                    }
                }
                if (intent.getAction().equals(Names.UPDATED_THEME)) {
                    CarState state = CarState.get(context, id());
                    CarConfig config = CarConfig.get(context, id());
                    vCar.forceUpdate(state, config);
                }
                if (intent.getAction().equals(Names.UPDATED))
                    error = null;
                if (intent.getAction().equals(Names.NO_UPDATED))
                    error = null;
                update();
            }
        };
        IntentFilter intFilter = new IntentFilter(Names.UPDATED);
        intFilter.addAction(Names.UPDATED_THEME);
        intFilter.addAction(Names.ERROR);
        intFilter.addAction(Names.START_UPDATE);
        intFilter.addAction(Names.NO_UPDATED);
        intFilter.addAction(Names.COMMANDS);
        intFilter.addAction(Names.CONFIG_CHANGED);
        intFilter.addAction(Names.ADDRESS_UPDATE);
        intFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(br, intFilter);

        Intent intent = new Intent(getActivity(), FetchService.class);
        intent.setAction(FetchService.ACTION_UPDATE);
        intent.putExtra(Names.ID, id());
        getActivity().startService(intent);

        CarConfig config = CarConfig.get(getActivity(), id());
        int[] pointers = config.getPointers();
        if (pointers != null) {
            for (int pointer : pointers) {
                Intent i = new Intent(getActivity(), FetchService.class);
                i.setAction(FetchService.ACTION_UPDATE);
                i.putExtra(Names.ID, id() + "_" + pointer);
                getActivity().startService(i);
            }
        }
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

        String error_text = error;
        if (error_text == null) {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if ((activeNetwork == null) || !activeNetwork.isConnectedOrConnecting())
                error_text = getString(R.string.net_warning);
        }
        if (error_text == null) {
            tvError.setVisibility(View.GONE);
        } else {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(error_text);
        }

        NumberFormat formatter = NumberFormat.getInstance(getResources().getConfiguration().locale);
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(2);

        double power = state.getPower();
        if (power == 0) {
            if (state.getPower_state() == 1) {
                iVoltage.setVisibility(View.VISIBLE);
                iVoltage.setText(getString(R.string.on));
            } else {
                iVoltage.setVisibility(View.GONE);
            }
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
        double card_voltage = state.getCard_voltage();
        if (card_voltage == 0) {
            iCard.setVisibility(View.GONE);
        } else {
            iCard.setVisibility(View.VISIBLE);
            iCard.setText(formatter.format(card_voltage) + " V");
        }

        String balance = state.getBalance();
        if (balance.equals("") || !config.isShowBalance()) {
            iBalance.setVisibility(View.GONE);
        } else {
            iBalance.setVisibility(View.VISIBLE);
            try {
                NumberFormat nf = NumberFormat.getCurrencyInstance();
                double value = Double.parseDouble(balance);
                double abs_value = Math.abs(value);
                String str = nf.format(value);
                Currency currency = Currency.getInstance(Locale.getDefault());
                str = str.replace(currency.getSymbol(), "");
                if (abs_value > 10000) {
                    NumberFormat f = NumberFormat.getInstance(getResources().getConfiguration().locale);
                    str = f.format(Math.round(value));
                }
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

        vCar.update(state, config);
        String[] states = vCar.carImage.state.split("\\|");
        if (states.length > 1) {
            String[] pictures = states[1].split(";");
            int n = 0;
            for (String picture : pictures) {
                int id = getActivity().getResources().getIdentifier(picture, "drawable", pkg);
                if (id == 0)
                    continue;
                ivInfo[n].setImageResource(id);
                ivInfo[n].setVisibility(View.VISIBLE);
                if (++n >= ivInfo.length)
                    break;
            }
            if (n > 0)
                vInfo.setVisibility(View.VISIBLE);
            for (; n < ivInfo.length; n++)
                ivInfo[n].setVisibility(View.GONE);
            int id = 0;
            if (states.length > 2)
                id = getResources().getIdentifier(states[2], "string", pkg);
            if (id == 0) {
                tvInfo.setVisibility(View.GONE);
            } else {
                tvInfo.setText(id);
                tvInfo.setVisibility(View.VISIBLE);
            }
        } else {
            vInfo.setVisibility(View.GONE);
        }

        if (state.getTime() > 0) {
            if (state.isOnline() && ((state.getTime() + 180000) > new Date().getTime())) {
                tvTime.setText(getString(R.string.online));
            }else{
                tvTime.setText(State.formatDateTime(getActivity(), state.getTime()));
            }
        } else {
            tvTime.setText("???");
        }
        StringBuilder text = new StringBuilder();
        if (!state.isIgnition() || state.isAz()) {
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
                final double lat = Double.parseDouble(g[0]);
                final double lng = Double.parseDouble(g[1]);
                if (!text.toString().equals(""))
                    text.append(" ");
                if (state.isGps_valid()) {
                    text.append(lat);
                    text.append(", ");
                    text.append(lng);
                } else if (!state.getGsm().equals("")) {
                    String[] parts = state.getGsm().split(",");
                    text.append(parts[0]);
                    text.append("-");
                    text.append(parts[1]);
                    text.append(" LAC: ");
                    text.append(parts[2]);
                    text.append(" CID: ");
                    text.append(parts[3]);
                }
                String address = state.getAddress(getActivity());
                if (address != null) {
                    text.append("\n|");
                    String[] parts = address.split(", ");
                    text.append(parts[0]);
                    if (parts.length > 1) {
                        text.append("|\n");
                        text.append(parts[1]);
                        for (int i = 2; i < parts.length; i++) {
                            text.append(", ");
                            text.append(parts[i]);
                        }
                    }
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        tvAddress.setText(State.createSpans(text.toString(), vAddressView.selectedColor, true));
        Vector<CarConfig.Command> fab = getFabCommands();
        if (fab.size() > 0) {
            vFab.setVisibility(View.VISIBLE);
            boolean more = true;
            if ((fab.size() == 1) && (fab.get(0).icon != null) && !fab.get(0).icon.equals("blocking")) {
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

        int[] pointers = config.getPointers();
        int len = 0;
        if (pointers != null)
            len = pointers.length;
        if (len > tvPointers.length)
            len = tvPointers.length;
        int i = 0;
        boolean error = false;
        if (len > 0) {
            vPointers.setVisibility(View.VISIBLE);
            for (i = 0; i < len; i++) {
                CarState pointerState = CarState.get(getActivity(), id() + "_" + pointers[i]);
                if ((pointer_errors != null) && pointer_errors.contains(pointers[i] + ""))
                    error = true;
                tvPointers[i].setVisibility(View.VISIBLE);
                if (pointerState.getTime() > 0) {
                    tvPointers[i].setText(State.formatDateTime(getActivity(), pointerState.getTime()));
                    if (state.getTime() - pointerState.getTime() > 86400000 * 3)
                        error = true;
                } else {
                    tvPointers[i].setText("???");
                    error = true;
                }
            }
            vPointers.setImageResource(error ? R.drawable.pointer_error : R.drawable.pointer);
        } else {
            vPointers.setVisibility(View.GONE);
        }
        for (; i < tvPointers.length; i++) {
            tvPointers[i].setVisibility(View.GONE);
        }

        String s = null;
        int days = config.getLeftDays();
        int mileage = config.getLeftMileage();
        if (days < 16) {
            s = config.getMaintenance() + "\n";
            tvMaintenance.setTextColor(getResources().getColor((days < 1) ? R.color.error : R.color.neutral));
            if (days >= 0) {
                s += getString(R.string.left);
            } else {
                s += getString(R.string.delay);
                days = -days;
            }
            s += " ";
            if (days < 30) {
                s += State.getPlural(getActivity(), R.plurals.days, days);
            } else {
                int month = (int) Math.round(days / 30.);
                if (month < 12) {
                    s += State.getPlural(getActivity(), R.plurals.months, month);
                } else {
                    int year = (int) Math.round(days / 365.25);
                    s += State.getPlural(getActivity(), R.plurals.years, year);
                }
            }
            tvMaintenance.setText(s);
            tvMaintenance.setVisibility(View.VISIBLE);
        } else if (mileage <= 500) {
            s = config.getMaintenance() + "\n";
            tvMaintenance.setTextColor(getResources().getColor((mileage < 50) ? R.color.error : R.color.neutral));
            if (mileage >= 0) {
                s += getString(R.string.left);
            } else {
                s += getString(R.string.rerun);
                mileage = -mileage;
            }
            s += String.format(" %,d ", mileage) + getString(R.string.km);
            tvMaintenance.setText(s);
            tvMaintenance.setVisibility(View.VISIBLE);
        } else {
            tvMaintenance.setVisibility(View.GONE);
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
        int[] selected = config.getFab();
        CarConfig.Command[] cmds = config.getCmd();
        Set<String> enabled = new HashSet<>();
        Vector<CarConfig.Command> res = new Vector<>();
        if (cmds == null)
            return res;
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
