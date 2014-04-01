package net.ugona.plus;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;
import java.util.Vector;

public class AutoStartFragment extends DeviceFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        fill();
        return v;
    }

    @Override
    void update() {
        fill();
        super.update();
    }

    void fill() {
        items = new Vector<Item>();
        items.add(new ListItem(R.string.start_time, 20, R.array.start_timer));
        items.add(new SeekBarItem(R.string.voltage_limit, 18, 60, 93, R.string.v, 0.13) {
            @Override
            String textValue(int progress) {
                if (progress <= 0)
                    return getString(R.string.no_start);
                return super.textValue(progress);
            }

            @Override
            String getValue() {
                if ((getVal(12) & (1 << 7)) == 0)
                    return "0";
                return super.getValue();
            }

            @Override
            void setValue(String value) {
                int v = Integer.parseInt(value);
                int val = getVal(12);
                int mask = 1 << 7;
                val &= ~mask;
                if (v <= min_value) {
                    setVal(12, val);
                    return;
                }
                val |= mask;
                setVal(12, val);
                super.setValue(value);
            }
        });
        items.add(new CheckBitItem(R.string.soft_start, 19, 1));
        if (State.hasTelephony(getActivity()))
            items.add(new CheckBitItem(R.string.inf_sms, 23, 1) {
                @Override
                void setView(View v) {
                    super.setView(v);
                    TextView tv = (TextView) v.findViewById(R.id.value);
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(R.string.inf_sms_msg);
                }

                @Override
                String getValue() {
                    return super.getValue().equals("") ? "1" : "";
                }

                @Override
                void setValue(String value) {
                    super.setValue(value.equals("") ? "1" : "");
                }
            });

        SettingActivity activity = (SettingActivity) getActivity();
        if ((activity != null) && (activity.timers != null)) {
            items.add(new Item(R.string.schedule, ""));
            for (SettingActivity.Timer timer : activity.timers) {
                if (timer.com != 1)
                    continue;
                items.add(new TimerItem(timer));
            }
            items.add(new AddItem(R.string.add_timer));
        }
    }

    void timerEdit(SettingActivity.Timer timer) {
        final Context context = getActivity();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.timer_set)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .setView(inflater.inflate(R.layout.timer, null))
                .create();
        dialog.show();
        final Days days = new Days();
        days.days = timer.days;
        final TimePicker time = (TimePicker) dialog.findViewById(R.id.time);
        time.setIs24HourView(DateFormat.is24HourFormat(context));
        time.setCurrentHour(timer.hours);
        time.setCurrentMinute(timer.minutes);
        Calendar calendar = Calendar.getInstance();
        int first = calendar.getFirstDayOfWeek();
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
        final String[] dayNames = symbols.getShortWeekdays();
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int d = (Integer) v.getTag();
                int mask = 1 << d;
                if ((days.days & mask) == 0) {
                    days.days |= mask;
                } else {
                    days.days &= ~mask;
                }
                setTextColor((TextView) v, days.days);
            }
        };
        TextView tv1 = (TextView) dialog.findViewById(R.id.d1);
        tv1.setTag(1);
        setTextColor(tv1, days.days);
        tv1.setOnClickListener(clickListener);
        tv1.setText(dayNames[(first + 6) % 7 + 1]);
        TextView tv2 = (TextView) dialog.findViewById(R.id.d2);
        tv2.setTag(2);
        setTextColor(tv2, days.days);
        tv2.setOnClickListener(clickListener);
        tv2.setText(dayNames[first % 7 + 1]);
        TextView tv3 = (TextView) dialog.findViewById(R.id.d3);
        tv3.setTag(3);
        setTextColor(tv3, days.days);
        tv3.setOnClickListener(clickListener);
        tv3.setText(dayNames[(first + 1) % 7 + 1]);
        TextView tv4 = (TextView) dialog.findViewById(R.id.d4);
        tv4.setTag(4);
        setTextColor(tv4, days.days);
        tv4.setOnClickListener(clickListener);
        tv4.setText(dayNames[(first + 2) % 7 + 1]);
        TextView tv5 = (TextView) dialog.findViewById(R.id.d5);
        tv5.setTag(5);
        setTextColor(tv5, days.days);
        tv5.setOnClickListener(clickListener);
        tv5.setText(dayNames[(first + 3) % 7 + 1]);
        TextView tv6 = (TextView) dialog.findViewById(R.id.d6);
        tv6.setTag(6);
        setTextColor(tv6, days.days);
        tv6.setOnClickListener(clickListener);
        tv6.setText(dayNames[(first + 4) % 7 + 1]);
        TextView tv7 = (TextView) dialog.findViewById(R.id.d7);
        tv7.setTag(7);
        setTextColor(tv7, days.days);
        tv7.setOnClickListener(clickListener);
        tv7.setText(dayNames[(first + 5) % 7 + 1]);
        Spinner repeat = (Spinner) dialog.findViewById(R.id.repeat);
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
                    LayoutInflater inflater = (LayoutInflater) getActivity()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.list_item, null);
                }
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(repeats[position]);
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
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(repeats[position]);
                return v;
            }
        });
        repeat.setSelection(timer.period);
    }

    void setTextColor(TextView tv, int days) {
        int d = (Integer) tv.getTag();
        boolean selected = (days & (1 << d)) != 0;
        int id = selected ? R.color.caldroid_holo_blue_light : R.color.caldroid_gray;
        tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        tv.setTextColor(getResources().getColor(id));
    }

    static class Days {
        int days;
    }

    class TimerItem extends Item {

        SettingActivity.Timer timer;

        TimerItem(SettingActivity.Timer t) {
            super(R.string.timer, "");
            timer = t;
        }

        @Override
        void setView(View v) {
            super.setView(v);
            v.findViewById(R.id.block1).setVisibility(View.GONE);
            v.findViewById(R.id.block2).setVisibility(View.GONE);
            v.findViewById(R.id.block_timer).setVisibility(View.VISIBLE);
            TextView time = (TextView) v.findViewById(R.id.time);
            time.setText(String.format("%02d:%02d", timer.hours, timer.minutes));
            Calendar calendar = Calendar.getInstance();
            int first = calendar.getFirstDayOfWeek();
            DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
            String[] dayNames = symbols.getShortWeekdays();
            TextView tv1 = (TextView) v.findViewById(R.id.d1);
            tv1.setTag(1);
            setTextColor(tv1, timer.days);
            tv1.setText(dayNames[(first + 6) % 7 + 1]);
            TextView tv2 = (TextView) v.findViewById(R.id.d2);
            tv2.setTag(2);
            setTextColor(tv2, timer.days);
            tv2.setText(dayNames[first % 7 + 1]);
            TextView tv3 = (TextView) v.findViewById(R.id.d3);
            tv3.setTag(3);
            setTextColor(tv3, timer.days);
            tv3.setText(dayNames[(first + 1) % 7 + 1]);
            TextView tv4 = (TextView) v.findViewById(R.id.d4);
            tv4.setTag(4);
            setTextColor(tv4, timer.days);
            tv4.setText(dayNames[(first + 2) % 7 + 1]);
            TextView tv5 = (TextView) v.findViewById(R.id.d5);
            tv5.setTag(5);
            setTextColor(tv5, timer.days);
            tv5.setText(dayNames[(first + 3) % 7 + 1]);
            TextView tv6 = (TextView) v.findViewById(R.id.d6);
            tv6.setTag(6);
            setTextColor(tv6, timer.days);
            tv6.setText(dayNames[(first + 4) % 7 + 1]);
            TextView tv7 = (TextView) v.findViewById(R.id.d7);
            tv7.setTag(7);
            setTextColor(tv7, timer.days);
            tv7.setText(dayNames[(first + 5) % 7 + 1]);
            TextView tvRepeat = (TextView) v.findViewById(R.id.repeat);
            String[] repeats = getResources().getStringArray(R.array.periods);
            tvRepeat.setText(repeats[timer.period]);
        }

        @Override
        void click() {
            timerEdit(timer);
        }
    }
}
