package net.ugona.plus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

    SharedPreferences preferences;
    Vector<Item> items;
    Vector<Item> visible_items;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null)
            car_id = savedInstanceState.getString(Names.ID);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        car_id = Preferences.getCar(preferences, car_id);

        View v = inflater.inflate(R.layout.list, container, false);
        list = (ListView) v.findViewById(R.id.list);

        items = new Vector<Item>();

        list.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                if (visible_items == null)
                    return 0;
                return visible_items.size();
            }

            @Override
            public Object getItem(int position) {
                return visible_items.get(position);
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
                final Item item = visible_items.get(position);
                item.setView(v);
                return v;
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item item = visible_items.get(position);
                item.click();
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        update();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
    }

    void update() {
        visible_items = new Vector<Item>();
        for (Item item : items) {
            visible_items.add(item);
        }
        listUpdate();
    }

    void listUpdate() {
        BaseAdapter adapter = (BaseAdapter) list.getAdapter();
        adapter.notifyDataSetChanged();
    }

    class Item {
        TextView vChanged;
        String name;
        String value_;

        Item(int n, String v) {
            name = getString(n);
            value_ = v;
        }

        Item(int n, int v) {
            name = getString(n);
            value_ = getString(v);
        }

        Item(String n) {
            name = n;
            value_ = "";
        }

        void click() {
        }

        void setView(View v) {
            v.findViewById(R.id.block1).setVisibility(View.VISIBLE);
            v.findViewById(R.id.block2).setVisibility(View.VISIBLE);
            v.findViewById(R.id.block_add).setVisibility(View.GONE);
            v.findViewById(R.id.block_temp).setVisibility(View.GONE);
            TextView tvTitle = (TextView) v.findViewById(R.id.title);
            tvTitle.setVisibility(View.VISIBLE);
            tvTitle.setText(name);
            tvTitle.setTypeface(null, Typeface.BOLD);
            vChanged = tvTitle;
            setChanged();
            TextView tvValue = (TextView) v.findViewById(R.id.value);
            String val = getValue();
            tvValue.setVisibility(val.equals("") ? View.GONE : View.VISIBLE);
            tvValue.setText(val);
            v.findViewById(R.id.check).setVisibility(View.GONE);
            v.findViewById(R.id.spinner).setVisibility(View.GONE);
            v.findViewById(R.id.seekbar).setVisibility(View.GONE);
            v.findViewById(R.id.v).setVisibility(View.GONE);
            v.findViewById(R.id.v1).setVisibility(View.GONE);
            v.findViewById(R.id.title1).setVisibility(View.GONE);
            v.findViewById(R.id.check_edit).setVisibility(View.GONE);
            v.findViewById(R.id.progress).setVisibility(View.GONE);
            v.findViewById(R.id.block_timer).setVisibility(View.GONE);
            v.findViewById(R.id.block_widget).setVisibility(View.GONE);
        }

        String getValue() {
            return value_;
        }

        void setValue(String value) {
            value_ = value;
        }

        boolean changed() {
            return false;
        }

        void setChanged() {
            vChanged.setTextColor(getResources().getColor(changed() ? R.color.changed : android.R.color.secondary_text_dark));
        }
    }

    class AddItem extends Item {

        AddItem(int n) {
            super(n, "");
        }

        @Override
        void setView(View v) {
            super.setView(v);
            v.findViewById(R.id.block1).setVisibility(View.GONE);
            v.findViewById(R.id.block2).setVisibility(View.GONE);
            v.findViewById(R.id.value).setVisibility(View.GONE);
            View vAdd = v.findViewById(R.id.block_add);
            vAdd.setVisibility(View.VISIBLE);
            vAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    click();
                }
            });
            TextView tv = (TextView) v.findViewById(R.id.text_add);
            tv.setText(name);
        }
    }

    class CheckItem extends Item {
        CheckItem(int n) {
            super(n, "");
        }

        CheckItem(String n) {
            super(n);
        }

        @Override
        void setView(View v) {
            super.setView(v);
            v.findViewById(R.id.title).setVisibility(View.GONE);
            v.findViewById(R.id.value).setVisibility(View.GONE);
            CheckBox checkBox = (CheckBox) v.findViewById(R.id.check);
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setText(name);
            vChanged = checkBox;
            setChanged();
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

    class CheckBoxItem extends CheckItem {
        String key;
        boolean def_value_;

        CheckBoxItem(int name, String item_key, boolean def_value) {
            super(name);
            key = item_key;
            def_value_ = def_value;
            setValue(preferences.getBoolean(key + car_id, def_value_) ? "1" : "");
        }

        void update() {
            setValue(preferences.getBoolean(key + car_id, def_value_) ? "1" : "");
        }

        @Override
        void click() {
            SharedPreferences.Editor ed = preferences.edit();
            ed.putBoolean(key + car_id, !getValue().equals(""));
            ed.commit();
            Intent intent = new Intent(FetchService.ACTION_UPDATE_FORCE);
            intent.putExtra(Names.ID, car_id);
            getActivity().sendBroadcast(intent);
        }
    }

    class SpinnerItem extends Item {
        int msg_id;
        String[] entries;
        String[] values;

        SpinnerItem(int n, int values_id, int entries_id) {
            super(n, "");
            entries = getResources().getStringArray(entries_id);
            values = getResources().getStringArray(values_id);
        }

        @Override
        void setView(View v) {
            super.setView(v);
            TextView tvTitle = (TextView) v.findViewById(R.id.title1);
            tvTitle.setText(name);
            tvTitle.setVisibility(View.VISIBLE);
            vChanged = tvTitle;
            setChanged();
            TextView tvValue = (TextView) v.findViewById(R.id.value);
            if (msg_id == 0) {
                tvValue.setVisibility(View.GONE);
            } else {
                tvValue.setText(msg_id);
                tvValue.setVisibility(View.VISIBLE);
            }
            v.findViewById(R.id.title).setVisibility(View.GONE);
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
                        v = inflater.inflate(R.layout.list_item, null);
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
                        v = inflater.inflate(R.layout.list_dropdown_item, null);
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
    }

    class ListItem extends SpinnerItem {
        String key;

        ListItem(int name, int values, int entries, String item_key, String def_value) {
            super(name, values, entries);
            key = item_key;
            setValue(preferences.getString(key + car_id, def_value));
        }

        ListItem(int name, int values, int entries, String item_key, String def_value, int msg) {
            super(name, values, entries);
            key = item_key;
            setValue(preferences.getString(key + car_id, def_value));
            msg_id = msg;
        }

        @Override
        void click() {
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(key + car_id, getValue());
            ed.commit();
        }
    }

    class ListIntItem extends SpinnerItem {
        String key;

        ListIntItem(int name, int values, int entries, String item_key, int def_value) {
            super(name, values, entries);
            key = item_key;
            try {
                setValue(preferences.getInt(key + car_id, def_value) + "");
            } catch (Exception ex) {
                setValue(def_value + "");
            }
        }

        @Override
        void click() {
            SharedPreferences.Editor ed = preferences.edit();
            ed.putInt(key + car_id, Integer.parseInt(getValue()));
            ed.commit();
        }
    }

    class SeekItem extends Item {
        int min_value;
        int max_value;
        double k;
        String unit;

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
            TextView tvTitle = (TextView) v.findViewById(R.id.title1);
            tvTitle.setText(name);
            tvTitle.setVisibility(View.VISIBLE);
            vChanged = tvTitle;
            setChanged();
            v.findViewById(R.id.value).setVisibility(View.GONE);
            v.findViewById(R.id.title).setVisibility(View.GONE);
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
    }

    class SeekBarPrefItem extends SeekItem {
        String key;

        SeekBarPrefItem(int name, String id_key, int min, int max, String unit, double k) {
            super(name, min, max, unit, k);
            key = id_key;
            setValue(preferences.getInt(key + car_id, 0) + "");
        }

        @Override
        void click() {
            SharedPreferences.Editor ed = preferences.edit();
            ed.putInt(key + car_id, Integer.parseInt(getValue()));
            ed.commit();
            Intent intent = new Intent(FetchService.ACTION_UPDATE_FORCE);
            intent.putExtra(Names.ID, car_id);
            getActivity().sendBroadcast(intent);
        }
    }
}
