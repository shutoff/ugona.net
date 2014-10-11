package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.romorama.caldroid.CaldroidFragment;
import com.romorama.caldroid.CaldroidListener;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

public class MaintenanceActivity extends ActionBarActivity {

    final static String URL_MAINTENANCE = "/maintenance?skey=$1&lang=$2";
    static int[] periods = new int[]{
            0, 1, 3, 6, 12, 24, 36, 120
    };
    ListView vList;
    View vControl;
    View vProgress;
    TextView vText;
    View vRefresh;
    SharedPreferences preferences;
    String car_id;
    Vector<Preset> preset;
    Vector<Maintenance> items;
    Handler handler;
    CaldroidFragment caldroidFragment;
    Date current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maintenance);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        car_id = getIntent().getStringExtra(Names.ID);
        vList = (ListView) findViewById(R.id.list);
        vControl = findViewById(R.id.control_block);
        vProgress = findViewById(R.id.control_prg);
        vText = (TextView) findViewById(R.id.control_label);
        vRefresh = findViewById(R.id.control_img);
        setTitle(R.string.maintenance);
        handler = new Handler();
        load();

        vList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                Maintenance m = null;
                if (i < items.size())
                    m = items.get(i);
                if (m == null)
                    m = new Maintenance();
                final int id = m.id;
                AlertDialog.Builder builder = new AlertDialog.Builder(MaintenanceActivity.this)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, null)
                        .setView(inflater.inflate(R.layout.maintenance_edit, null));
                if (m.id > 0)
                    builder = builder.setNeutralButton(R.string.delete, null);
                final AlertDialog dialog = builder.create();
                dialog.show();
                final AutoCompleteTextView vName = (AutoCompleteTextView) dialog.findViewById(R.id.name);
                final EditText vMileage = (EditText) dialog.findViewById(R.id.mileage);
                final Spinner vPeriod = (Spinner) dialog.findViewById(R.id.period);
                vPeriod.setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return periods.length;
                    }

                    @Override
                    public Object getItem(int i) {
                        return periods[i];
                    }

                    @Override
                    public long getItemId(int i) {
                        return i;
                    }

                    @Override
                    public View getView(int i, View convertView, ViewGroup viewGroup) {
                        View v = convertView;
                        if (v == null) {
                            LayoutInflater inflater = (LayoutInflater) MaintenanceActivity.this
                                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            v = inflater.inflate(R.layout.car_list_item, null);
                        }
                        TextView vName = (TextView) v.findViewById(R.id.name);
                        vName.setText(period(periods[i]));
                        return v;
                    }

                    public View getDropDownView(int i, View convertView, ViewGroup viewGroup) {
                        View v = convertView;
                        if (v == null) {
                            LayoutInflater inflater = (LayoutInflater) MaintenanceActivity.this
                                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            v = inflater.inflate(R.layout.car_list_dropdown_item, null);
                        }
                        TextView vName = (TextView) v.findViewById(R.id.name);
                        vName.setText(period(periods[i]));
                        return v;
                    }
                });
                if (m.mileage > 0)
                    vMileage.setText(String.format("%,d", m.mileage));
                for (int n = 0; n < periods.length; n++) {
                    if (periods[n] == m.period) {
                        vPeriod.setSelection(n);
                        break;
                    }
                }
                current = m.date;

                dialog.findViewById(R.id.dropdown).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        vName.showDropDown();
                    }
                });
                vName.setAdapter(new NamesAdapter());
                if (m.name != null) {
                    vName.setText(m.name);
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            vName.showDropDown();
                        }
                    });
                }
                vName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Preset p = preset.get(i);
                        if (p.mileage > 0) {
                            vMileage.setText(String.format("%,d", p.mileage));
                        } else {
                            vMileage.setText("");
                        }
                        for (int n = 0; n < periods.length; n++) {
                            if (periods[n] == p.period) {
                                vPeriod.setSelection(n);
                            }
                        }
                    }
                });

                final Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                final Runnable setOk = new Runnable() {
                    @Override
                    public void run() {
                        boolean ok = (vName.getText().length() > 0) &&
                                ((vPeriod.getSelectedItemPosition() > 0) || (vMileage.getText().length() > 0));
                        if ((vMileage.getText().length() == 0) && (current == null))
                            ok = false;
                        okButton.setEnabled(ok);
                    }
                };

                setOk.run();

                final TextView vDate = (TextView) dialog.findViewById(R.id.date);
                final View vDateDrop = dialog.findViewById(R.id.date_dropdown);

                View.OnClickListener clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        caldroidFragment = new CaldroidFragment() {

                            @Override
                            public void onAttach(Activity activity) {
                                super.onAttach(activity);
                                CaldroidListener listener = new CaldroidListener() {

                                    @Override
                                    public void onSelectDate(Date date, View view) {
                                        current = date;
                                        setOk.run();
                                        DateFormat df = android.text.format.DateFormat.getMediumDateFormat(MaintenanceActivity.this);
                                        vDate.setText(df.format(date));
                                        caldroidFragment.dismiss();
                                    }
                                };
                                setCaldroidListener(listener);
                            }

                        };
                        Bundle args = new Bundle();
                        args.putString(CaldroidFragment.DIALOG_TITLE, getString(R.string.day));
                        Calendar calendar = Calendar.getInstance();
                        args.putInt(CaldroidFragment.START_DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                        if (current != null) {
                            args.putInt(CaldroidFragment.MONTH, current.getMonth() + 1);
                            args.putInt(CaldroidFragment.YEAR, current.getYear() + 1900);
                        }
                        caldroidFragment.setArguments(args);
                        caldroidFragment.setMaxDate(new Date());
                        caldroidFragment.show(getSupportFragmentManager(), "Tag");
                        if (current != null)
                            caldroidFragment.setSelectedDates(current, current);
                    }
                };
                vDate.setOnClickListener(clickListener);
                vDateDrop.setOnClickListener(clickListener);
                if (current != null) {
                    DateFormat df = android.text.format.DateFormat.getMediumDateFormat(MaintenanceActivity.this);
                    vDate.setText(df.format(current));
                }

                TextWatcher textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        setOk.run();
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                };
                vName.addTextChangedListener(textWatcher);
                vMileage.addTextChangedListener(textWatcher);
                vPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        setOk.run();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });

                if (m.id > 0) {
                    dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final ProgressDialog progressDialog = new ProgressDialog(MaintenanceActivity.this);
                            progressDialog.setCancelable(true);
                            progressDialog.setMessage(getString(R.string.save_data));
                            progressDialog.setIndeterminate(true);
                            progressDialog.show();
                            HttpTask task = new HttpTask() {
                                @Override
                                void result(JsonObject res) throws ParseException {
                                    loadData(res.get("data").asArray());
                                    BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
                                    adapter.notifyDataSetChanged();
                                    progressDialog.dismiss();
                                    dialog.dismiss();
                                }

                                @Override
                                void error() {
                                    progressDialog.dismiss();
                                    Toast toast = Toast.makeText(MaintenanceActivity.this, R.string.save_data_error, Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            };
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.remove(Names.Car.MAINTENANCE_TIME + car_id);
                            ed.commit();
                            task.execute(URL_MAINTENANCE, preferences.getString(Names.Car.CAR_KEY + car_id, ""), Locale.getDefault().getLanguage(),
                                    "set", id);
                        }
                    });
                }

                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final ProgressDialog progressDialog = new ProgressDialog(MaintenanceActivity.this);
                        progressDialog.setCancelable(true);
                        progressDialog.setMessage(getString(R.string.save_data));
                        progressDialog.setIndeterminate(true);
                        progressDialog.show();
                        HttpTask task = new HttpTask() {
                            @Override
                            void result(JsonObject res) throws ParseException {
                                loadData(res.get("data").asArray());
                                BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
                                adapter.notifyDataSetChanged();
                                progressDialog.dismiss();
                                dialog.dismiss();
                            }

                            @Override
                            void error() {
                                progressDialog.dismiss();
                                Toast toast = Toast.makeText(MaintenanceActivity.this, R.string.save_data_error, Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        };
                        SharedPreferences.Editor ed = preferences.edit();
                        ed.remove(Names.Car.MAINTENANCE_TIME + car_id);
                        ed.commit();
                        task.execute(URL_MAINTENANCE, preferences.getString(Names.Car.CAR_KEY + car_id, ""), Locale.getDefault().getLanguage(),
                                "name", vName.getText(),
                                "mileage", vMileage.getText(),
                                "period", periods[vPeriod.getSelectedItemPosition()],
                                "last", (current != null) ? current.getTime() / 1000 : null,
                                "set", (id > 0) ? id : null);
                    }
                });
            }
        });
    }

    void load() {
        vList.setVisibility(View.GONE);
        vControl.setVisibility(View.VISIBLE);
        vRefresh.setVisibility(View.GONE);
        vText.setText(R.string.loading);
        vProgress.setVisibility(View.VISIBLE);
        HttpTask task = new HttpTask() {
            @Override
            void result(JsonObject res) throws ParseException {
                preset = new Vector<Preset>();
                items = new Vector<Maintenance>();
                JsonArray preset_data = res.get("preset").asArray();
                for (int i = 0; i < preset_data.size(); i++) {
                    JsonObject data = preset_data.get(i).asObject();
                    Preset p = new Preset();
                    p.name = data.get("name").asString();
                    JsonValue v = data.get("period");
                    if (v != null)
                        p.period = v.asInt();
                    v = data.get("distance");
                    if (v != null)
                        p.mileage = v.asInt();
                    preset.add(p);
                }
                loadData(res.get("data").asArray());
                vList.setVisibility(View.VISIBLE);
                vControl.setVisibility(View.GONE);
                vList.setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return items.size() + 1;
                    }

                    @Override
                    public Object getItem(int i) {
                        if (i < items.size())
                            return items.get(i);
                        return null;
                    }

                    @Override
                    public long getItemId(int i) {
                        return i;
                    }

                    @Override
                    public View getView(int i, View convertView, ViewGroup viewGroup) {
                        View v = convertView;
                        if (v == null) {
                            LayoutInflater inflater = (LayoutInflater) MaintenanceActivity.this
                                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            v = inflater.inflate(R.layout.maintenance_item, null);
                        }
                        TextView vName = (TextView) v.findViewById(R.id.name);
                        TextView vInfo = (TextView) v.findViewById(R.id.info);
                        TextView vMileageLeft = (TextView) v.findViewById(R.id.mileage_left);
                        TextView vTimeLeft = (TextView) v.findViewById(R.id.time_left);
                        View vAdd = v.findViewById(R.id.block_add);
                        if (i >= items.size()) {
                            vName.setVisibility(View.GONE);
                            vInfo.setVisibility(View.GONE);
                            vMileageLeft.setVisibility(View.GONE);
                            vTimeLeft.setVisibility(View.GONE);
                            vAdd.setVisibility(View.VISIBLE);
                        } else {
                            Maintenance item = items.get(i);
                            vName.setVisibility(View.VISIBLE);
                            vName.setText(item.name);
                            String info = null;
                            if (item.mileage > 0)
                                info = String.format("%,d", item.mileage) + " " + getString(R.string.km);
                            if (item.period > 0) {
                                if (info != null) {
                                    info += " " + getString(R.string.or) + " " + period(item.period);
                                } else {
                                    info = period(item.period);
                                }
                            }
                            vInfo.setVisibility(View.VISIBLE);
                            vInfo.setText(info);
                            if (item.mileage > 0) {
                                double delta = item.mileage - item.current;
                                String s = getString(R.string.left);
                                if (delta < 50) {
                                    vMileageLeft.setTextColor(getResources().getColor(R.color.error));
                                } else if (delta < 500) {
                                    vMileageLeft.setTextColor(getResources().getColor(R.color.changed));
                                } else {
                                    vMileageLeft.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
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
                            if ((item.period > 0) && (item.date != null)) {
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(item.date);
                                cal.add(Calendar.MONTH, item.period);
                                Date end = cal.getTime();
                                double delta = (end.getTime() - new Date().getTime()) / 86400000;
                                String s = getString(R.string.left);
                                if (delta < 2) {
                                    vTimeLeft.setTextColor(getResources().getColor(R.color.error));
                                } else if (delta < 15) {
                                    vTimeLeft.setTextColor(getResources().getColor(R.color.changed));
                                } else {
                                    vTimeLeft.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
                                }
                                if (delta < 0) {
                                    s = getString(R.string.delay);
                                    delta = -delta;
                                }
                                s += " ";
                                double month = delta / 30;
                                if (month >= 12) {
                                    int years = (int) Math.round(delta / 365.25);
                                    s += getResources().getQuantityString(R.plurals.years, years, years);
                                } else if (month < 1) {
                                    int days = (int) Math.round(delta);
                                    s += getResources().getQuantityString(R.plurals.days, days, days);
                                } else {
                                    int months = (int) Math.round(delta / 30);
                                    s += getResources().getQuantityString(R.plurals.months, months, months);
                                }
                                vTimeLeft.setText(s);
                                vTimeLeft.setVisibility(View.VISIBLE);
                            } else {
                                vTimeLeft.setVisibility(View.GONE);
                            }
                            vAdd.setVisibility(View.GONE);
                        }
                        return v;
                    }
                });
            }

            @Override
            void error() {
                vList.setVisibility(View.GONE);
                vControl.setVisibility(View.VISIBLE);
                vRefresh.setVisibility(View.VISIBLE);
                vText.setText(R.string.error_load);
                vProgress.setVisibility(View.GONE);
            }
        };
        task.execute(URL_MAINTENANCE, preferences.getString(Names.Car.CAR_KEY + car_id, ""), Locale.getDefault().getLanguage());
    }

    String period(int n) {
        if (n == 0)
            return "";
        if ((n % 12) == 0)
            return getResources().getQuantityString(R.plurals.years, n / 12, n / 12);
        return getResources().getQuantityString(R.plurals.months, n, n);
    }

    void loadData(JsonArray array) {
        Vector<Maintenance> res = new Vector<Maintenance>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject d = array.get(i).asObject();
            Maintenance m = new Maintenance();
            m.name = d.get("name").asString();
            m.id = d.get("id").asInt();
            JsonValue v = d.get("period");
            if (v != null)
                m.period = v.asInt();
            v = d.get("mileage");
            if (v != null)
                m.mileage = v.asInt();
            v = d.get("current");
            if (v != null)
                m.current = v.asInt();
            v = d.get("last");
            if (v != null)
                m.date = new Date(v.asLong() * 1000);
            res.add(m);
        }
        items = res;
    }

    static class Preset {
        String name;
        int period;
        int mileage;
    }

    static class Maintenance {
        int id;
        String name;
        int period;
        int mileage;
        int current;
        Date date;
    }

    class NamesAdapter extends BaseAdapter implements Filterable {

        @Override
        public int getCount() {
            return preset.size();
        }

        @Override
        public Object getItem(int i) {
            return preset.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) MaintenanceActivity.this
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.maintenance_dropdown_item, null);
            }
            TextView vText = (TextView) v.findViewById(R.id.name);
            Preset p = preset.get(i);
            vText.setText(p.name);
            String info = null;
            if (p.mileage > 0)
                info = String.format("%,d", p.mileage) + " " + getString(R.string.km);
            if (p.period > 0) {
                if (info != null) {
                    info += " " + getString(R.string.or) + " " + period(p.period);
                } else {
                    info = period(p.period);
                }
            }
            TextView vInfo = (TextView) v.findViewById(R.id.info);
            vInfo.setText(info);
            return v;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    return null;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

                }

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    Preset p = (Preset) resultValue;
                    return p.name;
                }
            };
        }

        ;
    }
}
