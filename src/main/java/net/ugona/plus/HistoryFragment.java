package net.ugona.plus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class HistoryFragment extends MainFragment implements HistoryView.HistoryViewListener {

    final static int[] temp = {
            R.id.temp,
            R.id.temp_engine,
            R.id.temp_salon,
            R.id.temp_ext,
            R.id.temp1,
            R.id.temp2
    };
    static TypesMap typesMap = new TypesMap();
    HistoryView vHistory;
    View vNoData;
    View vError;
    int type;

    @Override
    int layout() {
        return R.layout.history;
    }

    @Override
    boolean isShowDate() {
        return true;
    }

    @Override
    void changeDate() {
        vHistory.setVisibility(View.GONE);
        vNoData.setVisibility(View.GONE);
        onRefresh();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        vHistory = (HistoryView) v.findViewById(R.id.history);
        vNoData = v.findViewById(R.id.no_data);
        vError = v.findViewById(R.id.error);
        vHistory.mListener = this;
        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });
        Menu m = combo();
        if (m.findItem(type) == null)
            type = m.getItem(0).getItemId();
        onRefresh();
        return v;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Names.TITLE, type);
    }

    @Override
    public void dataReady() {
        vHistory.setVisibility(View.VISIBLE);
        vNoData.setVisibility(View.GONE);
        vError.setVisibility(View.GONE);
        refreshDone();
    }

    @Override
    public void noData() {
        vHistory.setVisibility(View.GONE);
        vNoData.setVisibility(View.VISIBLE);
        vError.setVisibility(View.GONE);
        refreshDone();
    }

    @Override
    public void errorLoading() {
        vHistory.setVisibility(View.GONE);
        vNoData.setVisibility(View.GONE);
        vError.setVisibility(View.VISIBLE);
        refreshDone();
    }

    void setArgs(Bundle args) {
        type = args.getInt(Names.TITLE, 0);
        if (vHistory != null) {
            vError.setVisibility(View.GONE);
            vHistory.init(getActivity(), id(), typesMap.get(type), date());
        }
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        Bundle args = new Bundle();
        args.putInt(Names.TITLE, type);
        setArgs(args);
    }

    Menu combo() {
        Menu menu = State.createMenu(getActivity());
        getActivity().getMenuInflater().inflate(R.menu.history, menu);
        CarState state = CarState.get(getActivity(), id());
        if (state.getPower() == 0)
            menu.removeItem(R.id.voltage);
        if (state.getReserved() == 0)
            menu.removeItem(R.id.reserve);
        if (state.getFuel() == 0)
            menu.removeItem(R.id.fuel);
        if (state.getBalance().equals(""))
            menu.removeItem(R.id.balance);
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
        for (int i = 0; i < temp.length; i++) {
            int v = temps.get(i, -100);
            if (v == -100) {
                menu.removeItem(temp[i]);
                continue;
            }
        }
        return menu;
    }

    @Override
    int currentComboItem() {
        return type;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Menu m = combo();
        if (m.findItem(item.getItemId()) != null) {
            if (type == item.getItemId())
                return true;
            type = item.getItemId();
            onRefresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static class TypesMap extends SparseArray<String> {
        TypesMap() {
            put(R.id.voltage, "voltage");
            put(R.id.reserve, "reserved");
            put(R.id.balance, "balance");
            put(R.id.fuel, "fuel");
            put(R.id.temp, "t_0");
            put(R.id.temp_engine, "t_1");
            put(R.id.temp_salon, "t_2");
            put(R.id.temp_ext, "t_3");
            put(R.id.temp1, "t_4");
            put(R.id.temp2, "t_5");
        }

    }

}
