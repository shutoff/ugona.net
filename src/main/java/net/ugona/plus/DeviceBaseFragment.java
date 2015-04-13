package net.ugona.plus;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.haibison.android.lockpattern.LockPatternActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public abstract class DeviceBaseFragment
        extends MainFragment
        implements AdapterView.OnItemClickListener {

    final static int REQUEST_CHECK_PATTERN = 200;

    final static String DATA = "data";
    final static String CHANGED = "changed";
    final static String TIMERS = "timers";
    static int[] wdays = {
            R.id.wday1,
            R.id.wday2,
            R.id.wday3,
            R.id.wday4,
            R.id.wday5,
            R.id.wday6,
            R.id.wday7
    };
    static int[] period_names = {
            R.string.once,
            R.string.hour,
            R.string.hour2,
            R.string.hour3,
            R.string.hour4,
            R.string.hour6,
            R.string.everyday,
            R.string.everyweek
    };
    HashMap<String, Object> settings;
    HashMap<String, Object> changed;
    ListView vList;
    View vError;
    Vector<Timer> timers;
    NumberFormat nf;
    NumberFormat df;

    abstract boolean filter(String id);

    @Override
    int layout() {
        return R.layout.settings;
    }

    int getTimer() {
        return 0;
    }

    boolean showTimers() {
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ok, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vList = (ListView) v.findViewById(R.id.list);
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
                    settings = (HashMap<String, Object>) in.readObject();
                    in.close();
                    bis.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            data = savedInstanceState.getByteArray(CHANGED);
            if (data != null) {
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    ObjectInput in = new ObjectInputStream(bis);
                    changed = (HashMap<String, Object>) in.readObject();
                    in.close();
                    bis.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            data = savedInstanceState.getByteArray(TIMERS);
            if (data != null) {
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    ObjectInput in = new ObjectInputStream(bis);
                    timers = (Vector<Timer>) in.readObject();
                    in.close();
                    bis.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        nf = NumberFormat.getInstance(Locale.getDefault());
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);

        df = NumberFormat.getInstance(Locale.getDefault());
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);

        if (settings == null) {
            onRefresh();
        } else {
            done();
        }
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (settings != null) {
            byte[] data = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(settings);
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
            if (data != null)
                outState.putByteArray(DATA, data);
        }
        if (changed != null) {
            byte[] data = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(changed);
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
            if (data != null)
                outState.putByteArray(CHANGED, data);
        }
        if (timers != null) {
            byte[] data = null;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(timers);
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
            if (data != null)
                outState.putByteArray(TIMERS, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if ((changed != null) && !changed.isEmpty())
                return true;
        }
        if (item.getItemId() == R.id.ok) {
            if ((changed == null) || changed.isEmpty()) {
                Toast toast = Toast.makeText(getActivity(), R.string.no_changed, Toast.LENGTH_LONG);
                toast.show();
                return true;
            }
            AppConfig config = AppConfig.get(getActivity());
            if (!config.getPattern().equals("")) {
                Intent intent = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                        getActivity(), LockPatternActivity.class);
                intent.putExtra(LockPatternActivity.EXTRA_PATTERN, config.getPattern().toCharArray());
                getParentFragment().startActivityForResult(intent, REQUEST_CHECK_PATTERN);
                return true;
            }
            if (config.getPassword().equals("")) {
                send_update();
                return true;
            }
            PasswordDialog passwordDialog = new PasswordDialog();
            Bundle args = new Bundle();
            args.putString(Names.MESSAGE, config.getPassword());
            passwordDialog.setArguments(args);
            passwordDialog.setTargetFragment(this, REQUEST_CHECK_PATTERN);
            passwordDialog.show(getActivity().getSupportFragmentManager(), "password");
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_CHECK_PATTERN) && (resultCode == Activity.RESULT_OK))
            send_update();
        super.onActivityResult(requestCode, resultCode, data);
    }

    void send_update() {
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.save_settings));
        dialog.show();
        HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                try {
                    if (changed != null) {
                        Set<Map.Entry<String, Object>> entries = changed.entrySet();
                        for (Map.Entry<String, Object> entry : entries) {
                            settings.put(entry.getKey(), entry.getValue());
                        }
                    }
                    changed = new HashMap<>();
                    BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
                    adapter.notifyDataSetChanged();

                    CarConfig config = CarConfig.get(getActivity(), id());
                    Config.update(config, res);
                    CarState state = CarState.get(getActivity(), id());
                    if (CarState.update(state, res.get("state").asObject()) != null) {
                        Intent intent = new Intent(Names.UPDATED);
                        intent.putExtra(Names.ID, id());
                        getActivity().sendBroadcast(intent);
                    }
                    JsonObject caps = res.get("caps").asObject();
                    boolean changed = CarState.update(state, caps.get("caps").asObject()) != null;
                    changed |= (CarState.update(config, caps) != null);
                    if (changed) {
                        Intent intent = new Intent(Names.CONFIG_CHANGED);
                        intent.putExtra(Names.ID, id());
                        getActivity().sendBroadcast(intent);
                    }
                    dialog.dismiss();
                } catch (Exception ex) {
                    // ignore
                }
            }

            @Override
            void error() {
                try {
                    dialog.dismiss();
                    Toast toast = Toast.makeText(getActivity(), R.string.save_error, Toast.LENGTH_LONG);
                    toast.show();
                } catch (Exception ex) {
                    // ignore
                }
            }
        };
        JsonObject params = new JsonObject();
        Set<Map.Entry<String, Object>> entries = changed.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            Object o = entry.getValue();
            if (o instanceof Integer) {
                params.add(entry.getKey(), ((Integer) o).intValue());
                continue;
            }
            if (o instanceof Boolean) {
                params.add(entry.getKey(), ((Boolean) o).booleanValue());
                continue;
            }
        }
        CarConfig config = CarConfig.get(getActivity(), id());
        params.add("skey", config.getKey());
        task.execute("/set", params);
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        vError.setVisibility(View.GONE);
        HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                JsonValue vTimers = res.get("timers");
                if (vTimers != null) {
                    timers = new Vector<>();
                    JsonArray aTimers = vTimers.asArray();
                    for (int i = 0; i < aTimers.size(); i++) {
                        Timer t = new Timer();
                        Config.update(t, aTimers.get(i).asObject());
                        timers.add(t);
                    }
                }
                List<String> names = res.names();
                settings = new HashMap<>();
                for (String name : names) {
                    JsonValue v = res.get(name);
                    Object r = null;
                    if (v.isBoolean())
                        r = (Boolean) v.asBoolean();
                    if (v.isNumber())
                        r = (Integer) v.asInt();
                    if (r == null)
                        continue;
                    settings.put(name, r);
                }
                done();
                refreshDone();
            }

            @Override
            void error() {
                vError.setVisibility(View.VISIBLE);
                vList.setVisibility(View.GONE);
                refreshDone();
            }
        };
        CarConfig config = CarConfig.get(getActivity(), id());
        Param param = new Param();
        param.skey = config.getKey();
        param.get_timers = getTimer();
        task.execute("/settings", param);
    }

    void done() {
        if (getActivity() == null)
            return;
        if (changed == null)
            changed = new HashMap<>();
        vError.setVisibility(View.GONE);
        vList.setVisibility(View.VISIBLE);
        fill();
    }

    void fill() {
        CarConfig config = CarConfig.get(getActivity(), id());
        Vector<CarConfig.Setting> settings = new Vector<>();
        for (CarConfig.Setting setting : config.getSettings()) {
            if (filter(setting.id))
                settings.add(setting);
        }
        vList.setAdapter(new SettingsAdapter(settings, showTimers()));
        vList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        SettingsAdapter adapter = (SettingsAdapter) vList.getAdapter();
        if (i < adapter.defs.size()) {
            CarConfig.Setting setting = adapter.defs.get(i);
            if (setting.cmd == null)
                return;
            Bundle args = new Bundle();
            args.putString(Names.ID, id());
            args.putString(Names.TITLE, setting.id);
            SmsSettingsFragment fragment = new SmsSettingsFragment();
            fragment.setArguments(args);
            fragment.show(getActivity().getSupportFragmentManager(), "sms");
        }
    }

    void onChanged(String id) {

    }

    static class Param implements Serializable {
        String skey;
        int get_timers;
    }

    static class Timer implements Serializable {
        int day;
        int hour;
        int min;
        int period;
        int param;
    }

    class SettingsAdapter extends BaseAdapter {

        Vector<CarConfig.Setting> defs;
        boolean show_timers;

        SettingsAdapter(Vector<CarConfig.Setting> settings, boolean show_timers) {
            this.defs = settings;
            this.show_timers = show_timers;
        }

        @Override
        public int getCount() {
            int res = defs.size();
            if (show_timers && (timers != null))
                res += timers.size() + 2;
            return res;
        }

        @Override
        public Object getItem(int position) {
            if (position < defs.size())
                return defs.get(position);
            position -= defs.size();
            if (position < timers.size())
                return timers.get(position);
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
                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.setting_item, null);
            }

            final TextView vTitle = (TextView) v.findViewById(R.id.title);
            final TextView vVal = (TextView) v.findViewById(R.id.value);
            final CheckBox vCheck = (CheckBox) v.findViewById(R.id.check);
            final TextView vText = (TextView) v.findViewById(R.id.text);
            SeekBar vSeek = (SeekBar) v.findViewById(R.id.seek);
            final Spinner vSpinner = (Spinner) v.findViewById(R.id.spinner);

            v.findViewById(R.id.add).setVisibility(View.GONE);
            v.findViewById(R.id.timer_block).setVisibility(View.GONE);

            if (position < defs.size()) {

                final CarConfig.Setting def = defs.get(position);
                boolean isChanged = true;
                Object val = changed.get(def.id);
                Object sVal = settings.get(def.id);
                if (val == null) {
                    isChanged = false;
                    val = sVal;
                }

                if ((def.min != null) && (def.max != null)) {
                    vTitle.setText(def.name);
                    vTitle.setVisibility(View.VISIBLE);
                    vTitle.setTextColor(getResources().getColor(isChanged ? R.color.main : R.color.text_dark));
                    vCheck.setVisibility(View.GONE);
                    vVal.setVisibility(View.VISIBLE);
                    vSeek.setVisibility(View.VISIBLE);
                    vSpinner.setVisibility(View.GONE);
                    vText.setVisibility(View.GONE);
                    vSeek.setMax(def.max - def.min);
                    int iVal = 0;
                    if ((val != null) && (val instanceof Integer))
                        iVal = (int) val;
                    if (iVal < def.min)
                        iVal = def.min;
                    if (iVal > def.max)
                        iVal = def.max;
                    if (sVal == null)
                        sVal = (Integer) iVal;
                    final int start_value = (Integer) sVal;
                    SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if ((progress != 0) || (def.zero == null)) {
                                double v = progress + def.min;
                                NumberFormat f = nf;
                                if (def.k > 0) {
                                    v = v * def.k;
                                    if (def.k < 1)
                                        f = df;
                                }
                                String val = f.format(v);
                                if (def.unit != null) {
                                    val += " ";
                                    val += def.unit;
                                }
                                vVal.setText(val);
                            } else {
                                vVal.setText(def.zero);
                            }
                            int new_val = def.min + progress;
                            if (new_val == start_value) {
                                changed.remove(def.id);
                                vTitle.setTextColor(getResources().getColor(R.color.text_dark));
                            } else {
                                changed.put(def.id, (Integer) new_val);
                                vTitle.setTextColor(getResources().getColor(R.color.main));
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    };
                    vSeek.setOnSeekBarChangeListener(listener);
                    vSeek.setProgress(iVal - def.min);
                    listener.onProgressChanged(vSeek, iVal - def.min, false);
                } else if (def.values != null) {
                    vTitle.setText(def.name);
                    vTitle.setVisibility(View.VISIBLE);
                    vTitle.setTextColor(getResources().getColor(isChanged ? R.color.main : R.color.text_dark));
                    vVal.setVisibility(View.GONE);
                    vCheck.setVisibility(View.GONE);
                    vSeek.setVisibility(View.GONE);
                    vSpinner.setVisibility(View.VISIBLE);
                    vText.setVisibility(View.GONE);
                    final String[] values = def.values.split("\\|");
                    vSpinner.setAdapter(new BaseAdapter() {
                        @Override
                        public int getCount() {
                            return values.length;
                        }

                        @Override
                        public Object getItem(int position) {
                            return values[position];
                        }

                        @Override
                        public long getItemId(int position) {
                            return position;
                        }

                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View v = convertView;
                            if (v == null) {
                                LayoutInflater inflater = (LayoutInflater) getActivity()
                                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                v = inflater.inflate(R.layout.list_item, null);
                            }
                            TextView tv = (TextView) v;
                            String[] val = values[position].split(":");
                            tv.setText(val[1]);
                            return v;
                        }

                        @Override
                        public View getDropDownView(int position, View convertView, ViewGroup parent) {
                            View v = convertView;
                            if (v == null) {
                                LayoutInflater inflater = (LayoutInflater) getActivity()
                                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                v = inflater.inflate(R.layout.list_dropdown_item, null);
                            }
                            TextView tv = (TextView) v;
                            String[] val = values[position].split(":");
                            tv.setText(val[1]);
                            String fontName = "Exo2-";
                            fontName += (position == vSpinner.getSelectedItemPosition()) ? "Medium" : "Light";
                            tv.setTypeface(Font.getFont(getActivity(), fontName));
                            return v;
                        }
                    });

                    int iVal = 0;
                    if ((val != null) && (val instanceof Integer))
                        iVal = (int) val;
                    if (sVal == null)
                        sVal = (Integer) iVal;
                    final int start_val = (Integer) sVal;
                    for (int i = 0; i < values.length; i++) {
                        int iv = Integer.parseInt(values[i].split(":")[0]);
                        if (iv == iVal) {
                            vSpinner.setSelection(i);
                            break;
                        }
                    }
                    vSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            int selected = Integer.parseInt(values[position].split(":")[0]);
                            if (selected == start_val) {
                                vTitle.setTextColor(getResources().getColor(R.color.text_dark));
                                changed.remove(def.id);
                            } else {
                                vTitle.setTextColor(getResources().getColor(R.color.main));
                                changed.put(def.id, (Integer) selected);
                            }
                            onChanged(def.id);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                } else if (def.cmd != null) {
                    vTitle.setText(def.name);
                    vCheck.setVisibility(View.GONE);
                    vTitle.setVisibility(View.VISIBLE);
                    vVal.setVisibility(View.GONE);
                    vSeek.setVisibility(View.GONE);
                    vSpinner.setVisibility(View.GONE);
                    String text = def.text;
                    if (text == null)
                        text = "";
                    if (text.equals("")) {
                        vText.setVisibility(View.GONE);
                    } else {
                        vText.setText(text);
                        vText.setVisibility(View.VISIBLE);
                    }
                } else {
                    vCheck.setText(def.name);
                    vCheck.setVisibility(View.VISIBLE);
                    vTitle.setVisibility(View.GONE);
                    vVal.setVisibility(View.GONE);
                    vSeek.setVisibility(View.GONE);
                    vSpinner.setVisibility(View.GONE);
                    vText.setVisibility(View.GONE);
                    boolean bChecked = false;
                    if ((val != null) && (val instanceof Boolean))
                        bChecked = (boolean) val;
                    if (sVal == null)
                        sVal = (Boolean) bChecked;
                    final boolean start_checked = (Boolean) sVal;
                    vCheck.setTextColor(getResources().getColor(isChanged ? R.color.main : R.color.text_dark));
                    vCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked == start_checked) {
                                vCheck.setTextColor(getResources().getColor(R.color.text_dark));
                                changed.remove(def.id);
                            } else {
                                vCheck.setTextColor(getResources().getColor(R.color.main));
                                changed.put(def.id, (Boolean) isChecked);
                            }
                            onChanged(def.id);
                        }
                    });
                    vCheck.setChecked(bChecked);
                }
            } else if (position == defs.size()) {
                vCheck.setVisibility(View.GONE);
                vTitle.setVisibility(View.VISIBLE);
                vVal.setVisibility(View.GONE);
                vSeek.setVisibility(View.GONE);
                vSpinner.setVisibility(View.GONE);
                vText.setVisibility(View.GONE);
                vTitle.setText(R.string.timers);
            } else {
                vCheck.setVisibility(View.GONE);
                vTitle.setVisibility(View.GONE);
                vVal.setVisibility(View.GONE);
                vSeek.setVisibility(View.GONE);
                vSpinner.setVisibility(View.GONE);
                vText.setVisibility(View.GONE);
                position -= defs.size() + 1;
                if (position < timers.size()) {
                    Timer t = timers.get(position);
                    v.findViewById(R.id.timer_block).setVisibility(View.VISIBLE);
                    Date d = new Date();
                    d.setHours(t.hour);
                    d.setMinutes(t.min);
                    d.setSeconds(0);
                    TextView tv = (TextView) v.findViewById(R.id.time);
                    tv.setText(State.shortFormatTime(getActivity(), d.getTime()));

                    Calendar calendar = Calendar.getInstance();
                    int first = calendar.getFirstDayOfWeek();
                    DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
                    String[] dayNames = symbols.getShortWeekdays();
                    for (int i = 0; i < 7; i++) {
                        int wd = (i + first + 6) % 7 + 1;
                        TextView tWday = (TextView) v.findViewById(wdays[i]);
                        tWday.setText(dayNames[wd]);
                        int color = R.color.text_dark;
                        if ((t.day & (1 << (wd % 7))) != 0)
                            color = R.color.main;
                        tWday.setTextColor(getResources().getColor(color));
                    }

                    tv = (TextView) v.findViewById(R.id.timer_period);
                    tv.setText(period_names[t.period]);

                } else {
                    v.findViewById(R.id.add).setVisibility(View.VISIBLE);
                }
            }
            return v;
        }
    }

}
