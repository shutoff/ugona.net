package net.ugona.plus;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;
import java.util.Vector;

public class DeviceFragment extends SettingsFragment {

    static final int TIMER_SETUP = 1000;
    static int period_values[] = null;
    BroadcastReceiver br;

    View progress;
    View error;
    ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        listView = (ListView) v.findViewById(R.id.list);
        progress = v.findViewById(R.id.progress);
        error = v.findViewById(R.id.error);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };
        IntentFilter filter = new IntentFilter(SettingActivity.UPDATE_SETTINGS);
        getActivity().registerReceiver(br, filter);

        update();
        return v;
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(br);
        super.onDestroyView();
    }

    void update() {
        super.update();
        SettingActivity activity = (SettingActivity) getActivity();
        if (activity == null)
            return;
        if (activity.values != null) {
            progress.setVisibility(View.GONE);
            error.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            return;
        }
        if (activity.values_error) {
            progress.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            return;
        }
        progress.setVisibility(View.VISIBLE);
        error.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);
    }

    int getVal(int index) {
        SettingActivity activity = (SettingActivity) getActivity();
        if ((activity != null) && (activity.values != null))
            return activity.values[index];
        return preferences.getInt("V_" + index + "_" + car_id, 0);
    }

    int getOldVal(int index) {
        SettingActivity activity = (SettingActivity) getActivity();
        if ((activity != null) && (activity.old_values != null))
            return activity.old_values[index];
        return preferences.getInt("V_" + index + "_" + car_id, 0);
    }

    void setVal(int index, int v) {
        SettingActivity activity = (SettingActivity) getActivity();
        if ((activity != null) && (activity.values != null) && (activity.values[index] != v))
            activity.values[index] = v;
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

    void addTimers(TimerCommands commands) {
        if (commands.size() == 0)
            return;
        final SettingActivity activity = (SettingActivity) getActivity();
        if ((activity != null) && (activity.timers != null)) {
            items.add(new Item(R.string.schedule, ""));
            for (SettingActivity.Timer timer : activity.timers) {
                if (commands.getPosition(timer.com) < 0)
                    continue;
                items.add(new TimerItem(timer, commands));
            }
            items.add(new AddTimerItem(commands));
        }
    }

    static class TimerCommand implements Serializable {
        int id;
        int picture;
        int name;

        TimerCommand(int i, int p, int n) {
            id = i;
            picture = p;
            name = n;
        }
    }

    static class TimerCommands extends Vector<TimerCommand> {

        int getPosition(int cmd) {
            for (int i = 0; i < size(); i++) {
                if (get(i).id == cmd)
                    return i;
            }
            return -1;
        }

    }

    class CheckBitItem extends CheckItem {
        int word;
        int mask;

        CheckBitItem(int name, int word_, int bit) {
            super(name);
            word = word_;
            mask = 1 << bit;
        }

        @Override
        String getValue() {
            int v = getVal(word);
            return ((v & mask) != 0) ? "1" : "";
        }

        @Override
        void setValue(String value) {
            int v = getVal(word) & ~mask;
            if (!value.equals(""))
                v |= mask;
            setVal(word, v);
            setChanged();
        }

        @Override
        boolean changed() {
            int v = getVal(word);
            int ov = getOldVal(word);
            return (v & mask) != (ov & mask);
        }

    }

    class SeekBarItem extends SeekItem {

        int word;

        SeekBarItem(int name, int word_, int min, int max, int unit) {
            super(name, min, max, " " + getString(unit));
            word = word_;
        }

        SeekBarItem(int name, int word_, int min, int max, int unit, double k) {
            super(name, min, max, " " + getString(unit), k);
            word = word_;
        }

        @Override
        String getValue() {
            return getVal(word) + "";
        }

        @Override
        void setValue(String value) {
            setVal(word, Integer.parseInt(value));
            setChanged();
        }

        @Override
        boolean changed() {
            return getVal(word) != getOldVal(word);
        }

    }

    class ListItem extends SpinnerItem {

        String[] values;
        int word;

        ListItem(int name, int word_, int values_id) {
            super(name, values_id, values_id);
            word = word_;
            values = getResources().getStringArray(values_id);
        }

        @Override
        String getValue() {
            return values[getVal(word)];
        }

        @Override
        void setValue(String value) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(value)) {
                    setVal(word, i);
                    setChanged();
                    break;
                }
            }
        }

        @Override
        boolean changed() {
            return getVal(word) != getOldVal(word);
        }

    }

    class AddTimerItem extends AddItem {

        TimerCommands cmd;

        AddTimerItem(TimerCommands commands) {
            super(R.string.add_timer);
            cmd = commands;
        }

        @Override
        void click() {
            SettingActivity.Timer timer = new SettingActivity.Timer();
            timer.param = "";
            timer.com = cmd.get(0).id;
            timer.period = 7;
            timer.clearChanged();
            final SettingActivity activity = (SettingActivity) getActivity();
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
                bos = new ByteArrayOutputStream();
                out = new ObjectOutputStream(bos);
                out.writeObject(cmd);
                data = bos.toByteArray();
                out.close();
                bos.close();
                i.putExtra(Names.STATE, data);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            startActivityForResult(i, TIMER_SETUP);
        }

    }

    class TimerItem extends Item {

        SettingActivity.Timer timer;
        TextView tvTime;
        TimerCommands cmd;

        TimerItem(SettingActivity.Timer t, TimerCommands commands) {
            super(R.string.timer, "");
            timer = t;
            cmd = commands;
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
            ImageView icon = (ImageView) v.findViewById(R.id.timer_icon);
            icon.setVisibility(View.GONE);
            int p = cmd.getPosition(timer.com);
            if (p >= 0) {
                TimerCommand c = cmd.get(p);
                TextView tvCmd = (TextView) v.findViewById(R.id.command);
                tvCmd.setText(c.name);
                if (c.picture > 0) {
                    icon.setImageResource(c.picture);
                    icon.setVisibility(View.VISIBLE);
                }
            }
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
                bos = new ByteArrayOutputStream();
                out = new ObjectOutputStream(bos);
                out.writeObject(cmd);
                data = bos.toByteArray();
                out.close();
                bos.close();
                i.putExtra(Names.STATE, data);
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
