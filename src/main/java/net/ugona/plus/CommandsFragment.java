package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class CommandsFragment
        extends MainFragment
        implements View.OnClickListener {

    static final int DO_EDIT = 1;

    final Vector<Group> grp = new Vector<>();
    final Map<Integer, String> c_names = new HashMap<>();
    ListView vList;
    boolean longTap;

    Vector<Item> items;

    @Override
    int layout() {
        return R.layout.settings;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        grp.clear();
        c_names.clear();

        vList = (ListView) v.findViewById(R.id.list);
        final CarConfig config = CarConfig.get(getActivity(), id());
        final CarState state = CarState.get(getActivity(), id());

        items = new Vector<>();
        if (State.hasTelephony(getActivity())) {
            items.add(new SpinnerItem(R.string.control_method, 0, R.array.ctrl_entries) {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    config.setInet_cmd(position > 0);
                }

                @Override
                int current() {
                    return config.isInet_cmd() ? 1 : 0;
                }

            });
            if (state.isDevice_password()) {
                items.add(new Item() {
                    @Override
                    void setupView(View v) {
                        CheckBox checkBox = (CheckBox) v.findViewById(R.id.device_passwd);
                        checkBox.setVisibility(View.VISIBLE);
                        checkBox.setChecked(config.isDevice_password());
                        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                config.setDevice_password(isChecked);
                            }
                        });
                    }
                });
            }
            if (State.isDualSim(getActivity())) {
                items.add(new SpinnerItem(R.string.sim_card, 0, R.array.sim_entries) {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        config.setSim_cmd(position);
                    }

                    @Override
                    int current() {
                        return config.getSim_cmd();
                    }
                });
            }
        }
        items.add(new Item() {
            @Override
            void setupView(View v) {
                v.findViewById(R.id.grp_title).setVisibility(View.VISIBLE);
            }
        });

        final String[] custom_names = config.getCustomNames().split("\\|");
        for (String custom_name : custom_names) {
            String[] parts = custom_name.split(":");
            if (parts.length != 2)
                continue;
            c_names.put(Integer.parseInt(parts[0]), parts[1]);
        }

        final Set<String> used = new HashSet<>();

        final CarConfig.Command[] cmd = config.getCmd();
        int[] selected = config.getFab();
        for (CarConfig.Command c : cmd) {
            if (c.group == null)
                continue;
            String name = c.group;
            if (name.equals(""))
                name = c.name;
            if (used.contains(name))
                continue;
            used.add(name);
            Group g = new Group();
            g.name = name;
            g.id = c.id;
            for (int s : selected) {
                if (s == g.id)
                    g.checked = true;
            }
            g.editable = c.custom_name;
            grp.add(g);
        }

        vList.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return items.size() + grp.size();
            }

            @Override
            public Object getItem(int position) {
                if (position < items.size())
                    return items.get(position);
                return grp.get(position - items.size());
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
                    v = inflater.inflate(R.layout.command_item, null);
                }
                v.findViewById(R.id.grp).setVisibility(View.GONE);
                v.findViewById(R.id.grp_title).setVisibility(View.GONE);
                v.findViewById(R.id.spinner_block).setVisibility(View.GONE);
                v.findViewById(R.id.device_passwd).setVisibility(View.GONE);
                if (position < items.size()) {
                    items.get(position).setupView(v);
                    return v;
                }
                v.findViewById(R.id.grp).setVisibility(View.VISIBLE);
                position -= items.size();
                final Group g = grp.get(position);
                CheckBox cmd = (CheckBox) v.findViewById(R.id.cmd);
                cmd.setChecked(g.checked);
                cmd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        g.checked = isChecked;
                        saveChecked();
                    }
                });
                String name = g.name;
                String info = null;
                View edit = v.findViewById(R.id.edit);
                if (g.editable) {
                    edit.setVisibility(View.VISIBLE);
                    edit.setTag(position);
                    edit.setOnClickListener(CommandsFragment.this);
                    if (c_names.get(g.id) != null) {
                        info = name;
                        name = c_names.get(g.id);
                    }
                } else {
                    edit.setVisibility(View.GONE);
                }
                cmd.setText(name);
                TextView tvInfo = (TextView) v.findViewById(R.id.info);
                if (info != null) {
                    tvInfo.setVisibility(View.VISIBLE);
                    tvInfo.setText(info);
                } else {
                    tvInfo.setVisibility(View.GONE);
                }
                return v;
            }
        });
        return v;
    }

    void saveChecked() {
        int count = 0;
        for (Group g : grp) {
            if (g.checked)
                count++;
        }
        int[] cmd = new int[count];
        count = 0;
        for (Group g : grp) {
            if (g.checked)
                cmd[count++] = g.id;
        }
        CarConfig config = CarConfig.get(getActivity(), id());
        config.setFab(cmd);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == DO_EDIT) && (resultCode == Activity.RESULT_OK)) {
            int pos = Integer.parseInt(data.getStringExtra(Names.ID));
            Group g = grp.get(pos);
            String name = data.getStringExtra(Names.OK);
            if (name.equals("") || name.equals(g.name)) {
                c_names.remove(g.id);
            } else {
                c_names.put(g.id, name);
            }
            BaseAdapter adapter = (BaseAdapter) vList.getAdapter();
            adapter.notifyDataSetChanged();
            String custom_names = "";
            Set<Map.Entry<Integer, String>> enries = c_names.entrySet();
            for (Map.Entry<Integer, String> entry : enries) {
                if (!custom_names.equals(""))
                    custom_names += "|";
                custom_names += entry.getKey() + ":" + entry.getValue();
            }
            CarConfig carConfig = CarConfig.get(getActivity(), id());
            carConfig.setCustomNames(custom_names);
            Intent intent = new Intent(Names.CONFIG_CHANGED);
            intent.putExtra(Names.ID, id());
            getActivity().sendBroadcast(intent);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        int pos = (Integer) v.getTag();
        Group g = grp.get(pos);
        InputText inputText = new InputText();
        Bundle args = new Bundle();
        args.putString(Names.TITLE, g.name);
        String custom_name = c_names.get(g.id);
        if (custom_name == null)
            custom_name = g.name;
        args.putString(Names.OK, custom_name);
        args.putString(Names.ID, pos + "");
        inputText.setArguments(args);
        inputText.setTargetFragment(this, DO_EDIT);
        inputText.show(getFragmentManager(), "edit");
    }

    static class Group {
        String name;
        int id;
        boolean checked;
        boolean editable;
    }

    abstract class Item {
        abstract void setupView(View v);
    }

    abstract class SpinnerItem
            extends Item
            implements AdapterView.OnItemSelectedListener {

        int title;
        int text;
        String[] items;

        SpinnerItem(int title, int text, int items) {
            this.title = title;
            this.text = text;
            this.items = getResources().getStringArray(items);
        }

        @Override
        void setupView(View v) {
            v.findViewById(R.id.spinner_block).setVisibility(View.VISIBLE);
            TextView tvTitle = (TextView) v.findViewById(R.id.spinner_title);
            TextView tvText = (TextView) v.findViewById(R.id.spinner_text);
            tvTitle.setText(title);
            if (text != 0) {
                tvText.setVisibility(View.VISIBLE);
                tvText.setText(text);
            } else {
                tvText.setVisibility(View.GONE);
            }
            Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
            spinner.setAdapter(new ArrayAdapter(spinner) {
                @Override
                public int getCount() {
                    return items.length;
                }

                @Override
                public Object getItem(int position) {
                    return items[position];
                }
            });
            spinner.setSelection(current());
            spinner.setOnItemSelectedListener(this);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

        abstract int current();
    }

}
