package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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
import java.util.Vector;

public class TimerFragment
        extends DialogFragment
        implements View.OnClickListener,
        TimePicker.OnTimeChangedListener,
        AdapterView.OnItemSelectedListener,
        DialogInterface.OnClickListener {

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
    Vector<DeviceBaseFragment.TimerType> timerTypes;
    int id;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.timer, null);
        Dialog dialog = new AlertDialogWrapper.Builder(getActivity())
                .setTitle(R.string.timer)
                .setView(v)
                .setPositiveButton(R.string.ok, this)
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
        repeat.setAdapter(new ArrayAdapter(repeat) {
            @Override
            public int getCount() {
                return repeats.length;
            }

            @Override
            public Object getItem(int position) {
                return repeats[position];
            }
        });
        for (int i = 0; i < period_values.length; i++) {
            if (period_values[i] == timer.period) {
                repeat.setSelection(i);
                break;
            }
        }
        repeat.setOnItemSelectedListener(this);
        final Spinner vCommand = (Spinner) v.findViewById(R.id.command);
        if ((timerTypes != null) && (timerTypes.size() > 1)) {
            vCommand.setVisibility(View.VISIBLE);
            vCommand.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return timerTypes.size();
                }

                @Override
                public Object getItem(int position) {
                    return timerTypes.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    if (v == null)
                        v = inflater.inflate(R.layout.icon_list_item, null);
                    TextView tvName = (TextView) v.findViewById(R.id.text);
                    tvName.setText(timerTypes.get(position).name);
                    ImageView iv = (ImageView) v.findViewById(R.id.icon);
                    int id = 0;
                    String icon = timerTypes.get(position).icon;
                    if (icon != null)
                        id = getResources().getIdentifier("w_" + icon, "drawable", getActivity().getPackageName());
                    if (id != 0) {
                        iv.setImageResource(id);
                        iv.setVisibility(View.VISIBLE);
                    } else {
                        iv.setVisibility(View.GONE);
                    }
                    return v;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    if (v == null)
                        v = inflater.inflate(R.layout.icon_list_dropdown_item, null);
                    TextView tvName = (TextView) v.findViewById(R.id.text);
                    tvName.setText(timerTypes.get(position).name);
                    String fontName = "Exo2-";
                    fontName += (position == vCommand.getSelectedItemPosition()) ? "Medium" : "Light";
                    tvName.setTypeface(Font.getFont(inflater.getContext(), fontName));
                    ImageView iv = (ImageView) v.findViewById(R.id.icon);
                    int id = 0;
                    String icon = timerTypes.get(position).icon;
                    if (icon != null)
                        id = getResources().getIdentifier("w_" + icon, "drawable", getActivity().getPackageName());
                    if (id != 0) {
                        iv.setImageResource(id);
                        iv.setVisibility(View.VISIBLE);
                    } else {
                        iv.setVisibility(View.GONE);
                    }
                    return v;
                }
            });
            for (int i = 0; i < timerTypes.size(); i++) {
                if (timerTypes.get(i).id == timer.param) {
                    vCommand.setSelection(i);
                    break;
                }
            }
            vCommand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    timer.param = timerTypes.get(position).id;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        } else {
            vCommand.setVisibility(View.GONE);
        }
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
        data = null;
        if (timerTypes != null) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(timerTypes);
                data = bos.toByteArray();
                out.close();
                bos.close();
            } catch (Exception ex) {
                // ignore
            }
        }
        if (data != null)
            outState.putByteArray(Names.COMMANDS, data);
        outState.putInt(Names.ID, id);
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
        data = args.getByteArray(Names.COMMANDS);
        if (data != null) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(data);
                ObjectInput in = new ObjectInputStream(bis);
                timerTypes = (Vector<DeviceBaseFragment.TimerType>) in.readObject();
                in.close();
                bis.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        id = args.getInt(Names.ID);
    }

    void setDayColor(TextView v) {
        int day = (Integer) v.getTag();
        v.setTextColor(getResources().getColor(((timer.day & (1 << ((day + 6) % 7))) != 0) ? R.color.main : R.color.text_dark));
    }

    @Override
    public void onClick(View v) {
        int day = (Integer) v.getTag();
        timer.day ^= 1 << ((day + 6) % 7);
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

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Fragment fragment = getTargetFragment();
        if (fragment != null) {
            Intent intent = new Intent();
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
                intent.putExtra(Names.MESSAGE, data);
            intent.putExtra(Names.ID, id);
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        }
    }
}
