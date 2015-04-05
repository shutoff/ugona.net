package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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
import android.widget.Spinner;
import android.widget.TextView;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;

import org.joda.time.LocalDate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;

public class MaintenanceDialog
        extends DialogFragment
        implements DialogInterface.OnClickListener,
        View.OnClickListener,
        TextWatcher,
        AdapterView.OnItemSelectedListener {

    static int[] periods = new int[]{
            0, 1, 3, 6, 12, 24, 36, 120
    };

    Vector<MaintenanceFragment.Preset> presets;
    MaintenanceFragment.Maintenance maintenance;
    AutoCompleteTextView vName;
    EditText vMileage;
    EditText vMotoTime;
    Spinner vPeriod;
    TextView vDate;
    Handler handler;
    Button okButton;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.maintenance_edit, null);

        vName = (AutoCompleteTextView) v.findViewById(R.id.name);
        vMileage = (EditText) v.findViewById(R.id.mileage);
        vPeriod = (Spinner) v.findViewById(R.id.period);
        vMotoTime = (EditText) v.findViewById(R.id.mototime);
        vDate = (TextView) v.findViewById(R.id.date);

        vName.addTextChangedListener(this);
        vMileage.addTextChangedListener(this);
        vMotoTime.addTextChangedListener(this);

        vName.setAdapter(new NamesAdapter());
        if (maintenance.name != null)
            vName.setText(maintenance.name);
        vName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                MaintenanceFragment.Preset p = presets.get(i);
                if (p.distance > 0) {
                    vMileage.setText(String.format("%,d", p.distance));
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
        v.findViewById(R.id.dropdown).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vName.showDropDown();
            }
        });

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
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.list_item, null);
                }
                TextView tv = (TextView) v;
                tv.setText(period(periods[i]));
                return v;
            }

            public View getDropDownView(int i, View convertView, ViewGroup viewGroup) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.list_dropdown_item, null);
                }
                TextView tv = (TextView) v;
                tv.setText(period(periods[i]));
                return v;
            }
        });

        final TextView vDate = (TextView) v.findViewById(R.id.date);
        final View vDateDrop = v.findViewById(R.id.date_dropdown);
        vDate.setOnClickListener(this);
        vDateDrop.setOnClickListener(this);

        if (maintenance.mileage > 0)
            vMileage.setText(String.format("%,d", maintenance.mileage));
        if (maintenance.mototime > 0)
            vMotoTime.setText(String.format("%,d", maintenance.mototime));
        if (maintenance.last == 0) {
            Date d = new Date();
            maintenance.last = d.getTime() / 1000;
        }
        DateFormat df = android.text.format.DateFormat.getMediumDateFormat(getActivity());
        vDate.setText(df.format(new Date(maintenance.last * 1000)));

        vPeriod.setOnItemSelectedListener(this);
        for (int n = 0; n < periods.length; n++) {
            if (periods[n] == maintenance.period) {
                vPeriod.setSelection(n);
                break;
            }
        }

        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, this)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        afterTextChanged(null);

        if (vName.getText().toString().equals("")) {
            if (handler == null)
                handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    vName.showDropDown();
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
        outState.putByteArray(MaintenanceFragment.PRESET, data);
        data = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(maintenance);
            data = bos.toByteArray();
            out.close();
            bos.close();
        } catch (Exception ex) {
            // ignore
        }
        outState.putByteArray(MaintenanceFragment.DATA, data);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        byte[] data = args.getByteArray(MaintenanceFragment.PRESET);
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInput in = new ObjectInputStream(bis);
            presets = (Vector<MaintenanceFragment.Preset>) in.readObject();
        } catch (Exception ex) {
            // ignore
        }
        data = args.getByteArray(MaintenanceFragment.DATA);
        if (data != null) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(data);
                ObjectInput in = new ObjectInputStream(bis);
                maintenance = (MaintenanceFragment.Maintenance) in.readObject();
            } catch (Exception ex) {
                // ignore
            }
        } else {
            maintenance = new MaintenanceFragment.Maintenance();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        maintenance.name = vName.getText().toString();
        maintenance.period = periods[vPeriod.getSelectedItemPosition()];
        maintenance.mileage = getInt(vMileage.getText().toString());
        maintenance.mototime = getInt(vMotoTime.getText().toString());
        Fragment fragment = getTargetFragment();
        if (fragment == null)
            return;
        Intent intent = new Intent();
        byte[] data = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(maintenance);
            data = bos.toByteArray();
            out.close();
            bos.close();
        } catch (Exception ex) {
            // ignore
        }
        intent.putExtra(MaintenanceFragment.DATA, data);
        fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
    }

    int getInt(String str) {
        str = str.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(str);
        } catch (Exception ex) {
            //
        }
        return 0;
    }

    @Override
    public void onClick(View v) {
        LocalDate d = new LocalDate(maintenance.last * 1000);
        final CalendarDatePickerDialog dialog = new CalendarDatePickerDialog() {
            @Override
            public void onDayOfMonthSelected(int year, int month, int day) {
                super.onDayOfMonthSelected(year, month, day);
                getView().findViewById(R.id.done).performClick();
            }
        };
        dialog.initialize(new CalendarDatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(CalendarDatePickerDialog calendarDatePickerDialog, int i, int i2, int i3) {
                LocalDate d = new LocalDate(i, i2 + 1, i3);
                maintenance.last = d.toDate().getTime() / 1000;
                DateFormat df = android.text.format.DateFormat.getMediumDateFormat(getActivity());
                vDate.setText(df.format(d.toDate()));
            }
        }, d.getYear(), d.getMonthOfYear() - 1, d.getDayOfMonth());
        dialog.show(getActivity().getSupportFragmentManager(), "DATE_PICKER_TAG");
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        boolean ok = (vName.getText().length() > 0) &&
                ((vPeriod.getSelectedItemPosition() > 0) || (vMileage.getText().length() > 0) || (vMotoTime.getText().length() > 0));
        if (okButton != null)
            okButton.setEnabled(ok);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        afterTextChanged(null);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    String period(int n) {
        if (n == 0)
            return "";
        if ((n % 12) == 0)
            return State.getPlural(getActivity(), R.plurals.years, n / 12);
        return State.getPlural(getActivity(), R.plurals.months, n);
    }

    class NamesAdapter extends BaseAdapter implements Filterable {

        @Override
        public int getCount() {
            return presets.size();
        }

        @Override
        public Object getItem(int i) {
            return presets.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                v = inflater.inflate(R.layout.maintenance_dropdown_item, null);
            }
            TextView vText = (TextView) v.findViewById(R.id.name);
            MaintenanceFragment.Preset p = presets.get(i);
            vText.setText(p.name);
            String info = null;
            if (p.distance > 0)
                info = String.format("%,d", p.distance) + " " + getString(R.string.km);
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
                    MaintenanceFragment.Preset p = (MaintenanceFragment.Preset) resultValue;
                    return p.name;
                }
            };
        }
    }

}
