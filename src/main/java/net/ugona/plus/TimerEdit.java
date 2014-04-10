package net.ugona.plus;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class TimerEdit extends ActionBarActivity {

    TimePicker picker;
    Spinner repeat;
    boolean timer_delete;
    SettingActivity.Timer timer;
    int[] period_values;
    View vWeek;
    TextView tvTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setResult(RESULT_CANCELED);
        byte[] data;
        if (savedInstanceState != null) {
            data = savedInstanceState.getByteArray(Names.TRACK);
        } else {
            data = getIntent().getByteArrayExtra(Names.TRACK);
        }
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInput in = new ObjectInputStream(bis);
            timer = (SettingActivity.Timer) in.readObject();
        } catch (Exception ex) {
            // ignore
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timer);

        picker = (TimePicker) findViewById(R.id.time);

        Calendar calendar = Calendar.getInstance();
        int first = calendar.getFirstDayOfWeek();
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
        final String[] dayNames = symbols.getShortWeekdays();
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int d = (Integer) v.getTag();
                int mask = 1 << d;
                if ((timer.days & mask) == 0) {
                    timer.days |= mask;
                } else {
                    timer.days &= ~mask;
                }
                setTextColor((TextView) v);
            }
        };
        TextView tv1 = (TextView) findViewById(R.id.d1);
        tv1.setTag(1);
        setTextColor(tv1);
        tv1.setOnClickListener(clickListener);
        tv1.setText(dayNames[(first + 6) % 7 + 1]);
        TextView tv2 = (TextView) findViewById(R.id.d2);
        tv2.setTag(2);
        setTextColor(tv2);
        tv2.setOnClickListener(clickListener);
        tv2.setText(dayNames[first % 7 + 1]);
        TextView tv3 = (TextView) findViewById(R.id.d3);
        tv3.setTag(3);
        setTextColor(tv3);
        tv3.setOnClickListener(clickListener);
        tv3.setText(dayNames[(first + 1) % 7 + 1]);
        TextView tv4 = (TextView) findViewById(R.id.d4);
        tv4.setTag(4);
        setTextColor(tv4);
        tv4.setOnClickListener(clickListener);
        tv4.setText(dayNames[(first + 2) % 7 + 1]);
        TextView tv5 = (TextView) findViewById(R.id.d5);
        tv5.setTag(5);
        setTextColor(tv5);
        tv5.setOnClickListener(clickListener);
        tv5.setText(dayNames[(first + 3) % 7 + 1]);
        TextView tv6 = (TextView) findViewById(R.id.d6);
        tv6.setTag(6);
        setTextColor(tv6);
        tv6.setOnClickListener(clickListener);
        tv6.setText(dayNames[(first + 4) % 7 + 1]);
        TextView tv7 = (TextView) findViewById(R.id.d7);
        tv7.setTag(7);
        setTextColor(tv7);
        tv7.setOnClickListener(clickListener);
        tv7.setText(dayNames[(first + 5) % 7 + 1]);
        repeat = (Spinner) findViewById(R.id.repeat);
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
                    LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
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
                    LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    v = inflater.inflate(R.layout.list_dropdown_item, null);
                }
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(repeats[position]);
                return v;
            }
        });
        vWeek = findViewById(R.id.week);
        tvTimes = (TextView) findViewById(R.id.times);
        repeat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setTimes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        String[] values = getResources().getStringArray(R.array.period_values);
        period_values = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            period_values[i] = Integer.parseInt(values[i]);
            if (period_values[i] == timer.period)
                repeat.setSelection(i);
        }

        picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                setTimes();
            }
        });
        picker.setIs24HourView(DateFormat.is24HourFormat(this));
        picker.setCurrentHour(timer.hours);
        picker.setCurrentMinute(timer.minutes);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        timer.hours = picker.getCurrentHour();
        timer.minutes = picker.getCurrentMinute();
        timer.period = period_values[repeat.getSelectedItemPosition()];
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
            outState.putByteArray(Names.TRACK, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.timer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.timer_set)
                    .setMessage(R.string.timer_delete)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            timer.days = 0;
                            timer_delete = true;
                            finish();
                        }
                    })
                    .create();
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        timer.period = period_values[repeat.getSelectedItemPosition()];
        if (repeat.getSelectedItemPosition() > 1)
            timer.days = 1;
        if (!timer_delete && (timer.days == 0) && timer.isChanged()) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setMessage(R.string.timer_empty)
                    .setPositiveButton(R.string.ok, null)
                    .create();
            dialog.show();
            return;
        }
        timer.hours = picker.getCurrentHour();
        timer.minutes = picker.getCurrentMinute();
        Intent i = getIntent();
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
        setResult(RESULT_OK, i);
        super.finish();
    }

    void setTimes() {
        int position = repeat.getSelectedItemPosition();
        vWeek.setVisibility((position > 1) ? View.GONE : View.VISIBLE);
        tvTimes.setVisibility((position > 2) ? View.VISIBLE : View.GONE);
        if (position > 2) {
            int hours = picker.getCurrentHour();
            int minutes = picker.getCurrentMinute();
            int p = period_values[position];
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
        }
    }

    void setTextColor(TextView tv) {
        int d = (Integer) tv.getTag();
        boolean selected = (timer.days & (1 << d)) != 0;
        int id = selected ? R.color.caldroid_holo_blue_light : R.color.caldroid_gray;
        tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        tv.setTextColor(getResources().getColor(id));
    }
}
