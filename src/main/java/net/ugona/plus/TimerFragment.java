package net.ugona.plus;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class TimerFragment
        extends DialogFragment
        implements View.OnClickListener,
        TimePicker.OnTimeChangedListener,
        AdapterView.OnItemSelectedListener {

    final static int[] days_id = {
            R.id.d1,
            R.id.d2,
            R.id.d3,
            R.id.d4,
            R.id.d5,
            R.id.d6,
            R.id.d7
    };
    final static int[] period_values = {
            7,
            0,
            6,
            1,
            2,
            3,
            4,
            5
    };
    DeviceBaseFragment.Timer timer;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.timer, null);
        Dialog dialog = new AlertDialogWrapper.Builder(getActivity())
                .setTitle(R.string.timer)
                .setView(v)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        TimePicker picker = (TimePicker) v.findViewById(R.id.time);
        picker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
        picker.setCurrentHour(timer.hour);
        picker.setCurrentMinute(timer.min);
        picker.setOnTimeChangedListener(this);

        Calendar calendar = Calendar.getInstance();
        int first = calendar.getFirstDayOfWeek();
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
        final String[] dayNames = symbols.getShortWeekdays();
        for (int i = 0; i < days_id.length; i++) {
            TextView tv = (TextView) v.findViewById(days_id[i]);
            tv.setTag((i + first) % 7);
            setDayColor(tv);
            tv.setOnClickListener(this);
            tv.setText(dayNames[(first + 6 + i) % 7 + 1]);
        }

        final Spinner repeat = (Spinner) v.findViewById(R.id.repeat);
        final String[] repeats = getResources().getStringArray(R.array.periods);
        repeat.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return repeats.length;
            }

            @Override
            public Object getItem(int position) {
                return repeats[position];
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
                    v = inflater.inflate(R.layout.list_item, null);
                }
                TextView tvName = (TextView) v;
                tvName.setText(repeats[position]);
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    v = inflater.inflate(R.layout.list_dropdown_item, null);
                }
                TextView tvName = (TextView) v;
                tvName.setText(repeats[position]);
                String fontName = "Exo2-";
                fontName += (position == repeat.getSelectedItemPosition()) ? "Medium" : "Light";
                tvName.setTypeface(Font.getFont(getActivity(), fontName));
                return v;
            }
        });
        for (int i = 0; i < period_values.length; i++) {
            if (period_values[i] == timer.period) {
                repeat.setSelection(i);
                break;
            }
        }
        repeat.setOnItemSelectedListener(this);
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        byte[] data = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(timer);
            data = bos.toByteArray();
            out.close();
            bos.close();
        } catch (Exception ex) {
            // ignore
        }
        if (data != null)
            outState.putByteArray(Names.MESSAGE, data);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        byte[] data = args.getByteArray(Names.MESSAGE);
        if (data != null) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(data);
                ObjectInput in = new ObjectInputStream(bis);
                timer = (DeviceBaseFragment.Timer) in.readObject();
                in.close();
                bis.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (timer == null) {
            timer = new DeviceBaseFragment.Timer();
        }
    }

    void setDayColor(TextView v) {
        int day = (Integer) v.getTag();
        v.setTextColor(getResources().getColor(((timer.day & (1 << day)) != 0) ? R.color.main : R.color.text_dark));
    }

    @Override
    public void onClick(View v) {
        int day = (Integer) v.getTag();
        timer.day ^= 1 << day;
        setDayColor((TextView) v);
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        timer.hour = hourOfDay;
        timer.min = minute;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        timer.period = period_values[position];
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
