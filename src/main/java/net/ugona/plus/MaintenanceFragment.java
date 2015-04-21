package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

public class MaintenanceFragment
        extends MainFragment
        implements View.OnClickListener,
        AdapterView.OnItemClickListener {

    static final String DATA = "data";
    static final String PRESET = "preset";

    static final int DO_DELETE = 1;
    static final int DO_ITEM = 2;

    View vError;
    HoursList vList;

    DataFetcher data_fetcher;

    Vector<Preset> presets;
    Vector<Maintenance> maintenances;

    @Override
    int layout() {
        return R.layout.tracks;
    }

    @Override
    String getTitle() {
        return getString(R.string.maintenance);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        v.findViewById(R.id.summary).setVisibility(View.GONE);

        View vFooter = v.findViewById(R.id.footer);
        if (vFooter != null)
            vFooter.setVisibility(View.GONE);

        vList = (HoursList) v.findViewById(R.id.tracks);
        vError = v.findViewById(R.id.error);

        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });

        if (savedInstanceState != null) {
            byte[] data = savedInstanceState.getByteArray(DATA);
            if (data != null) {
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    ObjectInput in = new ObjectInputStream(bis);
                    maintenances = (Vector<Maintenance>) in.readObject();
                } catch (Exception ex) {
                    // ignore
                }
            }
            data = savedInstanceState.getByteArray(PRESET);
            if (data != null) {
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    ObjectInput in = new ObjectInputStream(bis);
                    presets = (Vector<Preset>) in.readObject();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        if (maintenances == null) {
            onRefresh();
        } else {
            done();
        }

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (maintenances != null) {
            byte[] data = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(maintenances);
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
            outState.putByteArray(DATA, data);
        }
        if (presets != null) {
            byte[] data = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(presets);
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
            outState.putByteArray(PRESET, data);
        }
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        vError.setVisibility(View.GONE);
        Params param = new Params();
        refresh(param);
    }

    void refresh(Params param) {
        vList.setVisibility(View.GONE);
        vError.setVisibility(View.GONE);
        if (data_fetcher != null)
            data_fetcher.cancel();
        data_fetcher = new DataFetcher();
        data_fetcher.update(param);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if ((requestCode == DO_DELETE) && (resultCode == Activity.RESULT_OK)) {
            ParamsDelete params = new ParamsDelete();
            params.set = Integer.parseInt(intent.getStringExtra(Names.ID));
            refresh(params);
            return;
        }
        if (resultCode == Activity.RESULT_OK) {
            int pos = requestCode - DO_ITEM;
            if ((pos >= 0) && (pos <= maintenances.size())) {
                byte[] data = intent.getByteArrayExtra(DATA);
                if (data != null) {
                    try {
                        ByteArrayInputStream bis = new ByteArrayInputStream(data);
                        ObjectInput in = new ObjectInputStream(bis);
                        Maintenance m = (Maintenance) in.readObject();
                        ParamsAdd params = null;
                        if (pos < maintenances.size()) {
                            ParamsSet paramsSet = new ParamsSet();
                            paramsSet.set = m.id;
                            params = paramsSet;
                        } else {
                            params = new ParamsAdd();
                        }
                        params.name = m.name;
                        params.mileage = m.mileage;
                        params.mototime = m.mototime;
                        params.period = m.period;
                        params.last = m.last;
                        refresh(params);
                        return;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    void done() {
        vList.setVisibility(View.VISIBLE);
        vError.setVisibility(View.GONE);
        refreshDone();
        vList.setOnItemClickListener(this);
        vList.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                if (maintenances.size() == 0)
                    return 2;
                return maintenances.size() + 1;
            }

            @Override
            public Object getItem(int position) {
                if (position < maintenances.size())
                    return maintenances.get(position);
                return null;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.maintenance_item, null);
                }
                if (position < maintenances.size()) {
                    v.findViewById(R.id.info_block).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.add).setVisibility(View.GONE);
                    v.findViewById(R.id.add_info).setVisibility(View.GONE);
                    Maintenance m = maintenances.get(position);
                    TextView tv = (TextView) v.findViewById(R.id.name);
                    tv.setText(m.name);
                    String info = null;
                    if (m.mileage > 0)
                        info = String.format("%,d", m.mileage) + " " + getString(R.string.km);
                    if (m.mototime > 0) {
                        String s = getString(R.string.mototime) + " " + State.getPlural(getActivity(), R.plurals.hours, m.mototime);
                        if (info != null) {
                            info += " " + getString(R.string.or) + " " + s;
                        } else {
                            info = s;
                        }
                    }
                    if (m.period > 0) {
                        if (info != null) {
                            info += " " + getString(R.string.or) + " " + period(m.period);
                        } else {
                            info = period(m.period);
                        }
                    }
                    TextView vInfo = (TextView) v.findViewById(R.id.info);
                    if (info != null) {
                        vInfo.setVisibility(View.VISIBLE);
                        vInfo.setText(info);
                    } else {
                        vInfo.setVisibility(View.GONE);
                    }
                    TextView vMileageLeft = (TextView) v.findViewById(R.id.mileage_left);
                    if (m.mileage > 0) {
                        double delta = m.mileage - m.current;
                        String s = getString(R.string.left);
                        if (delta < 50) {
                            vMileageLeft.setTextColor(getResources().getColor(R.color.error));
                        } else if (delta < 500) {
                            vMileageLeft.setTextColor(getResources().getColor(R.color.neutral));
                        } else {
                            vMileageLeft.setTextColor(getResources().getColor(R.color.text_dark));
                        }
                        if (delta < 0) {
                            delta = -delta;
                            s = getString(R.string.rerun);
                        }
                        if (delta > 10) {
                            double k = Math.floor(Math.log10(delta));
                            if (k < 2)
                                k = 2;
                            k = Math.pow(10, k) / 2;
                            delta = Math.round(Math.round(delta / k) * k);
                        }
                        s += " " + String.format("%,d", (long) delta) + " " + getString(R.string.km);
                        vMileageLeft.setText(s);
                        vMileageLeft.setVisibility(View.VISIBLE);
                    } else {
                        vMileageLeft.setVisibility(View.GONE);
                    }
                    TextView vMotoTimeLeft = (TextView) v.findViewById(R.id.mototime_left);
                    if (m.mototime > 0) {
                        int delta = m.mototime - m.current_time;
                        String s = getString(R.string.left);
                        if (delta < 2) {
                            vMotoTimeLeft.setTextColor(getResources().getColor(R.color.error));
                        } else if (delta < 50) {
                            vMotoTimeLeft.setTextColor(getResources().getColor(R.color.neutral));
                        } else {
                            vMotoTimeLeft.setTextColor(getResources().getColor(R.color.text_dark));
                        }
                        if (delta < 0) {
                            delta = -delta;
                            s = getString(R.string.rerun);
                        }
                        s += " ";
                        s += State.getPlural(getActivity(), R.plurals.hours, delta);
                        vMotoTimeLeft.setText(s);
                        vMotoTimeLeft.setVisibility(View.VISIBLE);
                    } else {
                        vMotoTimeLeft.setVisibility(View.GONE);
                    }
                    TextView vTimeLeft = (TextView) v.findViewById(R.id.time_left);
                    if ((m.period > 0) && (m.last != 0)) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(new Date(m.last * 1000));
                        cal.add(Calendar.MONTH, m.period);
                        Date end = cal.getTime();
                        double delta = (end.getTime() - new Date().getTime()) / 86400000;
                        String s = getString(R.string.left);
                        if (delta < 2) {
                            vTimeLeft.setTextColor(getResources().getColor(R.color.error));
                        } else if (delta < 15) {
                            vTimeLeft.setTextColor(getResources().getColor(R.color.neutral));
                        } else {
                            vTimeLeft.setTextColor(getResources().getColor(R.color.text_dark));
                        }
                        if (delta < 0) {
                            s = getString(R.string.delay);
                            delta = -delta;
                        }
                        s += " ";
                        double month = delta / 30;
                        if (month >= 12) {
                            int years = (int) Math.round(delta / 365.25);
                            s += State.getPlural(getActivity(), R.plurals.years, years);
                        } else if (month < 1) {
                            int days = (int) Math.round(delta);
                            s += State.getPlural(getActivity(), R.plurals.days, days);
                        } else {
                            int months = (int) Math.round(delta / 30);
                            s += State.getPlural(getActivity(), R.plurals.months, months);
                        }
                        vTimeLeft.setText(s);
                        vTimeLeft.setVisibility(View.VISIBLE);
                    } else {
                        vTimeLeft.setVisibility(View.GONE);
                    }
                    View vDelete = v.findViewById(R.id.delete);
                    vDelete.setTag(position);
                    vDelete.setOnClickListener(MaintenanceFragment.this);
                } else if (position > maintenances.size()) {
                    v.findViewById(R.id.info_block).setVisibility(View.GONE);
                    v.findViewById(R.id.add_info).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.add).setVisibility(View.GONE);
                } else {
                    v.findViewById(R.id.info_block).setVisibility(View.GONE);
                    v.findViewById(R.id.add_info).setVisibility(View.GONE);
                    v.findViewById(R.id.add).setVisibility(View.VISIBLE);
                }
                return v;
            }
        });
    }

    String period(int n) {
        if (n == 0)
            return "";
        if ((n % 12) == 0)
            return State.getPlural(getActivity(), R.plurals.years, n / 12);
        return State.getPlural(getActivity(), R.plurals.months, n);
    }

    @Override
    public void onClick(View v) {
        int position = (Integer) v.getTag();
        if (position >= maintenances.size())
            return;
        ;
        Alert alert = new Alert();
        Bundle args = new Bundle();
        args.putString(Names.TITLE, getString(R.string.delete));
        args.putString(Names.MESSAGE, String.format(getString(R.string.delete_car), maintenances.get(position).name));
        args.putString(Names.ID, maintenances.get(position).id + "");
        alert.setArguments(args);
        alert.setTargetFragment(this, DO_DELETE);
        alert.show(getFragmentManager(), "alert");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if ((maintenances == null) || (presets == null))
            return;
        Bundle args = new Bundle();
        if (presets != null) {
            byte[] data = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(presets);
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
            args.putByteArray(PRESET, data);
        }
        if (position < maintenances.size()) {
            byte[] data = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(maintenances.get(position));
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
            args.putByteArray(DATA, data);
        }
        MaintenanceDialog dialog = new MaintenanceDialog();
        dialog.setArguments(args);
        dialog.setTargetFragment(this, DO_ITEM + position);
        dialog.show(getActivity().getSupportFragmentManager(), "item");
    }

    static class Params implements Serializable {
        String skey;
        String lang;
    }

    static class ParamsDelete extends Params {
        int set;
    }

    static class ParamsAdd extends Params {
        String name;
        int period;
        int mileage;
        int mototime;
        long last;
    }

    static class ParamsSet extends ParamsAdd {
        int set;
    }

    static class Preset implements Serializable {
        String name;
        int period;
        int distance;
    }

    static class Maintenance implements Serializable {
        int id;
        String name;
        int period;
        int mileage;
        int mototime;
        int current;
        int current_time;
        long last;
    }

    class DataFetcher extends HttpTask {

        @Override
        void result(JsonObject res) throws ParseException {
            maintenances = new Vector<>();
            JsonArray arr = res.get("data").asArray();
            for (int i = 0; i < arr.size(); i++) {
                Maintenance m = new Maintenance();
                Config.update(m, arr.get(i).asObject());
                maintenances.add(m);
            }
            presets = new Vector<>();
            arr = res.get("preset").asArray();
            for (int i = 0; i < arr.size(); i++) {
                Preset p = new Preset();
                Config.update(p, arr.get(i).asObject());
                presets.add(p);
            }
            done();
        }

        @Override
        void error() {
            vList.setVisibility(View.GONE);
            vError.setVisibility(View.VISIBLE);
            refreshDone();
        }

        void update(Params param) {
            CarConfig config = CarConfig.get(getActivity(), id());
            param.skey = config.getKey();
            param.lang = Locale.getDefault().getLanguage();
            execute("/maintenance", param);
        }
    }

}
