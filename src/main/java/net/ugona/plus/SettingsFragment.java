package net.ugona.plus;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
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

import java.util.Vector;

public class SettingsFragment extends Fragment {

    String car_id;

    ListView list;

    class Item {
        Item(int n, String v) {
            name = getString(n);
            value_ = v;
        }

        Item(int n, int v) {
            name = getString(n);
            value_ = getString(v);
        }

        void click() {
        }

        void setView(View v) {
            TextView tvTitle = (TextView) v.findViewById(R.id.title);
            tvTitle.setVisibility(View.VISIBLE);
            tvTitle.setText(name);
            tvTitle.setTypeface(null, Typeface.BOLD);
            TextView tvValue = (TextView) v.findViewById(R.id.value);
            tvValue.setVisibility(View.VISIBLE);
            tvValue.setText(getValue());
            v.findViewById(R.id.check).setVisibility(View.GONE);
            v.findViewById(R.id.spinner).setVisibility(View.GONE);
            v.findViewById(R.id.seekbar).setVisibility(View.GONE);
            v.findViewById(R.id.v).setVisibility(View.GONE);
        }

        String getValue() {
            return value_;
        }

        void setValue(String value) {
            value_ = value;
        }

        String name;
        String value_;
    }

    class CheckItem extends Item {
        CheckItem(int n) {
            super(n, "");
        }

        @Override
        void setView(View v) {
            super.setView(v);
            v.findViewById(R.id.title).setVisibility(View.GONE);
            v.findViewById(R.id.value).setVisibility(View.GONE);
            CheckBox checkBox = (CheckBox) v.findViewById(R.id.check);
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setText(name);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    boolean wasChecked = !getValue().equals("");
                    if (isChecked == wasChecked)
                        return;
                    setValue(isChecked ? "1" : "");
                    click();
                }
            });
            checkBox.setChecked(!getValue().equals(""));
        }
    }

    class SpinnerItem extends Item {
        SpinnerItem(int n, int values_id, int entries_id) {
            super(n, "");
            entries = getResources().getStringArray(entries_id);
            values = getResources().getStringArray(values_id);
        }

        @Override
        void setView(View v) {
            super.setView(v);
            TextView tvTitle = (TextView) v.findViewById(R.id.title);
            tvTitle.setTypeface(null, Typeface.NORMAL);
            v.findViewById(R.id.value).setVisibility(View.GONE);
            Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
            spinner.setVisibility(View.VISIBLE);
            spinner.setAdapter(new BaseAdapter() {
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
                        v = inflater.inflate(R.layout.car_list_item, null);
                    }
                    TextView tv = (TextView) v.findViewById(R.id.name);
                    tv.setText(values[position]);
                    return v;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    if (v == null) {
                        LayoutInflater inflater = (LayoutInflater) getActivity()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        v = inflater.inflate(R.layout.car_list_dropdown_item, null);
                    }
                    TextView tv = (TextView) v.findViewById(R.id.name);
                    tv.setText(values[position]);
                    return v;
                }
            });
            String value = getValue();
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].equals(value)) {
                    spinner.setSelection(i);
                    break;
                }
            }
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    setValue(entries[position]);
                    click();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

        }

        String[] entries;
        String[] values;
    }

    class SeekItem extends Item {
        SeekItem(int n, int min, int max, String unit_str) {
            super(n, "");
            min_value = min;
            max_value = max;
            unit = unit_str;
            k = 1.;
        }

        SeekItem(int n, int min, int max, String unit_str, double koef) {
            super(n, "");
            min_value = min;
            max_value = max;
            unit = unit_str;
            k = koef;
        }

        @Override
        void setView(View v) {
            super.setView(v);
            TextView tvTitle = (TextView) v.findViewById(R.id.title);
            tvTitle.setTypeface(null, Typeface.NORMAL);
            v.findViewById(R.id.value).setVisibility(View.GONE);
            final TextView tvVal = (TextView) v.findViewById(R.id.v);
            tvVal.setVisibility(View.VISIBLE);
            SeekBar seekBar = (SeekBar) v.findViewById(R.id.seekbar);
            seekBar.setVisibility(View.VISIBLE);
            seekBar.setOnSeekBarChangeListener(null);
            tvVal.setText(textValue(Integer.parseInt(getValue()) - min_value));
            seekBar.setMax(max_value - min_value);
            seekBar.setProgress(Integer.parseInt(getValue()) - min_value);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    setValue((progress + min_value) + "");
                    tvVal.setText(textValue(progress));
                    click();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        String textValue(int progress) {
            if (k >= 0.99)
                return (int) (Math.round((progress + min_value) * k)) + unit;
            double v = (progress + min_value) * k * 100.;
            return Math.round(v) / 100. + unit;
        }

        int min_value;
        int max_value;

        double k;

        String unit;
    }

    Vector<Item> items;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null)
            car_id = savedInstanceState.getString(Names.ID);

        View v = inflater.inflate(R.layout.list, container, false);
        list = (ListView) v.findViewById(R.id.list);

        items = new Vector<Item>();

        list.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return items.size();
            }

            @Override
            public Object getItem(int position) {
                return items.get(position);
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
                    v = inflater.inflate(R.layout.settings_item, null);
                }
                final Item item = items.get(position);
                item.setView(v);
                return v;
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item item = items.get(position);
                item.click();
            }
        });

        return v;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
    }

    void update() {
        BaseAdapter adapter = (BaseAdapter) list.getAdapter();
        adapter.notifyDataSetChanged();
    }

}
