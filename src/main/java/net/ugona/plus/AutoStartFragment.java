package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

public class AutoStartFragment extends DeviceFragment {

    static final int TIMER_SETUP = 1000;

    static int period_values[] = null;

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

        final SettingActivity activity = (SettingActivity) getActivity();
        if ((activity != null) && (activity.timers != null)) {
            items.add(new Item(R.string.schedule, ""));
            for (SettingActivity.Timer timer : activity.timers) {
                if (timer.com != 1)
                    continue;
                items.add(new TimerItem(timer));
            }
            items.add(new AddItem(R.string.add_timer) {
                @Override
                void click() {
                    SettingActivity.Timer timer = new SettingActivity.Timer();
                    timer.param = "";
                    timer.com = 1;
                    timer.period = 7;
                    timer.clearChanged();
                    for (SettingActivity.Timer t : activity.timers) {
                        if (t.id >= timer.id)
                            timer.id = t.id + 1;
                    }
                    Intent i = new Intent(getActivity(), TimerEdit.class);
                    try {
                        byte[] data = null;
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutput out = new ObjectOutputStream(bos);
                        out.writeObject(timer);
                        data = bos.toByteArray();
                        out.close();
                        bos.close();
                        i.putExtra(Names.TRACK, data);
                    } catch (Exception ex) {
                        // ignore
                    }
                    startActivityForResult(i, TIMER_SETUP);
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == TIMER_SETUP) && (resultCode == Activity.RESULT_OK)) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(data.getByteArrayExtra(Names.TRACK));
                ObjectInput in = new ObjectInputStream(bis);
                SettingActivity.Timer timer = (SettingActivity.Timer) in.readObject();
                SettingActivity activity = (SettingActivity) getActivity();
                boolean found = false;
                for (SettingActivity.Timer t : activity.timers) {
                    if (t.id == timer.id) {
                        if (timer.days == 0) {
                            activity.timers.remove(t);
                            activity.timers_deleted = true;
                            update();
                            break;
                        }
                        t.set(timer);
                        found = true;
                        break;
                    }
                }
                if (!found && (timer.days != 0)) {
                    for (SettingActivity.Timer t : activity.timers) {
                        if (t.id >= timer.id)
                            timer.id = t.id + 1;
                    }
                    activity.timers.add(timer);
                    update();
                }
                listUpdate();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    int[] getPeriodValues() {
        if (period_values == null) {
            String[] values = getResources().getStringArray(R.array.period_values);
            period_values = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                period_values[i] = Integer.parseInt(values[i]);
            }
        }
        return period_values;
    }

    class TimerItem extends Item {

        SettingActivity.Timer timer;
        TextView tvTime;

        TimerItem(SettingActivity.Timer t) {
            super(R.string.timer, "");
            timer = t;
        }

        @Override
        void setView(View v) {
            super.setView(v);
            v.findViewById(R.id.block1).setVisibility(View.GONE);
            v.findViewById(R.id.block2).setVisibility(View.GONE);
            tvTime = (TextView) v.findViewById(R.id.time);
            tvTime.setText(String.format("%02d:%02d", timer.hours, timer.minutes));
            tvTime.setTextColor(getResources().getColor(changed() ? R.color.changed : android.R.color.secondary_text_dark));
            int[] periods = getPeriodValues();
            int pos;
            for (pos = 0; pos < periods.length; pos++) {
                if (period_values[pos] == timer.period)
                    break;
            }
            v.findViewById(R.id.block_timer).setVisibility(View.VISIBLE);
            v.findViewById(R.id.week).setVisibility(View.GONE);
            TextView tvTimes = (TextView) v.findViewById(R.id.times);
            tvTimes.setVisibility(View.GONE);
            if (pos < 2) {
                v.findViewById(R.id.week).setVisibility(View.VISIBLE);
                Calendar calendar = Calendar.getInstance();
                int first = calendar.getFirstDayOfWeek();
                DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
                String[] dayNames = symbols.getShortWeekdays();
                TextView tv1 = (TextView) v.findViewById(R.id.d1);
                tv1.setTag(1);
                setTextColor(tv1);
                tv1.setText(dayNames[(first + 6) % 7 + 1]);
                TextView tv2 = (TextView) v.findViewById(R.id.d2);
                tv2.setTag(2);
                setTextColor(tv2);
                tv2.setText(dayNames[first % 7 + 1]);
                TextView tv3 = (TextView) v.findViewById(R.id.d3);
                tv3.setTag(3);
                setTextColor(tv3);
                tv3.setText(dayNames[(first + 1) % 7 + 1]);
                TextView tv4 = (TextView) v.findViewById(R.id.d4);
                tv4.setTag(4);
                setTextColor(tv4);
                tv4.setText(dayNames[(first + 2) % 7 + 1]);
                TextView tv5 = (TextView) v.findViewById(R.id.d5);
                tv5.setTag(5);
                setTextColor(tv5);
                tv5.setText(dayNames[(first + 3) % 7 + 1]);
                TextView tv6 = (TextView) v.findViewById(R.id.d6);
                tv6.setTag(6);
                setTextColor(tv6);
                tv6.setText(dayNames[(first + 4) % 7 + 1]);
                TextView tv7 = (TextView) v.findViewById(R.id.d7);
                tv7.setTag(7);
                setTextColor(tv7);
                tv7.setText(dayNames[(first + 5) % 7 + 1]);
            } else if (pos > 2) {
                int minutes = timer.minutes;
                int hours = timer.hours;
                int p = period_values[pos];
                if (p > 4)
                    p = 6;
                while (hours > p) {
                    hours -= p;
                }
                String res = "";
                for (int i = 0; i < 3; i++) {
                    if (!res.equals(""))
                        res += ", ";
                    res += String.format("%d:%02d", hours, minutes);
                    hours += p;
                }
                if (p < 6)
                    res += " ... ";
                while (hours < 24) {
                    hours += p;
                }
                hours -= p;
                res += ", ";
                res += String.format("%d:%02d", hours, minutes);
                tvTimes.setText(res);
                tvTimes.setVisibility(View.VISIBLE);
            }
            TextView tvRepeat = (TextView) v.findViewById(R.id.repeat);
            String[] repeats = getResources().getStringArray(R.array.periods);
            tvRepeat.setText(repeats[pos]);
        }

        @Override
        void click() {
            Intent i = new Intent(getActivity(), TimerEdit.class);
            try {
                byte[] data = null;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(timer);
                data = bos.toByteArray();
                out.close();
                bos.close();
                i.putExtra(Names.TRACK, data);
            } catch (Exception ex) {
                // ignore
            }
            startActivityForResult(i, TIMER_SETUP);
        }

        boolean changed() {
            return timer.isChanged();
        }

        void setTextColor(TextView tv) {
            int d = (Integer) tv.getTag();
            boolean selected = (timer.days & (1 << d)) != 0;
            int id = selected ? R.color.caldroid_holo_blue_light : R.color.caldroid_gray;
            tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            tv.setTextColor(getResources().getColor(id));
        }
    }
}
