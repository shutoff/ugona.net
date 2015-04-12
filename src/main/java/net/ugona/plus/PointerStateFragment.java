package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class PointerStateFragment
        extends MainFragment
        implements AdapterView.OnItemClickListener {

    ListView vCmd;
    CenteredScrollView vAddressView;
    TextView tvAddress;
    BroadcastReceiver br;
    View vProgress;
    ImageView ivRefresh;
    TextView tvTime;
    TextView tvError;
    String error;
    Handler handler;
    Indicator iReserve;
    Indicator iBalance;

    @Override
    int layout() {
        return R.layout.pointer;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vCmd = (ListView) v.findViewById(R.id.cmd);
        vCmd.setAdapter(new ActionsAdapter(getActivity(), id()));
        vCmd.setOnItemClickListener(this);
        vCmd.setDivider(null);
        vCmd.setDividerHeight(0);
        tvAddress = (TextView) v.findViewById(R.id.address);
        vAddressView = (CenteredScrollView) v.findViewById(R.id.address_view);
        vProgress = v.findViewById(R.id.upd_progress);
        ivRefresh = (ImageView) v.findViewById(R.id.img_progress);
        tvTime = (TextView) v.findViewById(R.id.time);
        tvError = (TextView) v.findViewById(R.id.error);
        iReserve = (Indicator) v.findViewById(R.id.reserve);
        iBalance = (Indicator) v.findViewById(R.id.balance);
        handler = new Handler();
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
                if (!id().equals(intent.getStringExtra(Names.ID)))
                    return;
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
                if (intent.getAction().equals(Names.UPDATED))
                    error = null;
                if (intent.getAction().equals(Names.NO_UPDATED))
                    error = null;
                update();
            }
        };
        IntentFilter intFilter = new IntentFilter(Names.UPDATED);
        intFilter.addAction(Names.ERROR);
        intFilter.addAction(Names.START_UPDATE);
        intFilter.addAction(Names.NO_UPDATED);
        intFilter.addAction(Names.COMMANDS);
        intFilter.addAction(Names.CONFIG_CHANGED);
        intFilter.addAction(Names.ADDRESS_UPDATE);
        intFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(br, intFilter);
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
        IndicatorsView indicatorsView = (IndicatorsView) v.findViewById(R.id.indicators);
        View vRightArrow = v.findViewById(R.id.ind_right);
        indicatorsView.setArrows(vRightArrow);
        update();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(br);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ActionsAdapter actionsAdapter = (ActionsAdapter) vCmd.getAdapter();
        actionsAdapter.itemClick(position, false);
    }

    void update() {
        Context context = getActivity();
        if (context == null)
            return;

        CarState state = CarState.get(context, id());
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

        StringBuilder text = new StringBuilder();
        long last = state.getTime();
        if (last > 0) {
            text.append("|");
            text.append(State.formatDateTime(getActivity(), last));
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
        if (state.getTime() > 0) {
            tvTime.setText(State.formatDateTime(getActivity(), state.getTime()));
        } else {
            tvTime.setText("???");
        }
        NumberFormat formatter = NumberFormat.getInstance(getResources().getConfiguration().locale);
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(2);
        double reserved = state.getPower();
        if (reserved == 0) {
            iReserve.setVisibility(View.GONE);
        } else {
            iReserve.setVisibility(View.VISIBLE);
            iReserve.setText(formatter.format(reserved) + " V");
        }
        String balance = state.getBalance();
        if (balance.equals("")) {
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
            } catch (Exception ex) {
                iBalance.setText(balance);
                iBalance.setTextColor(getResources().getColor(R.color.indiicator));
            }
        }
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
}
