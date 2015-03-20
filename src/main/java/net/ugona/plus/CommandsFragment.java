package net.ugona.plus;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class CommandsFragment extends MainFragment {

    final Vector<Group> grp = new Vector<>();
    ListView vList;

    @Override
    int layout() {
        return R.layout.settings;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        vList = (ListView) v.findViewById(R.id.list);
        CarConfig config = CarConfig.get(getActivity(), id());

        final Set<String> used = new HashSet<>();

        final CarConfig.Command[] cmd = config.getCmd();
        int[] selected = config.getCommands();
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
                return grp.size();
            }

            @Override
            public Object getItem(int position) {
                return grp.get(position);
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
                    v = inflater.inflate(R.layout.command_item, null);
                }
                final Group g = grp.get(position);
                CheckBox cmd = (CheckBox) v.findViewById(R.id.cmd);
                cmd.setText(g.name);
                cmd.setChecked(g.checked);
                cmd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        g.checked = isChecked;
                        saveChecked();
                    }
                });
                View edit = v.findViewById(R.id.edit);
                if (g.editable) {
                    edit.setVisibility(View.VISIBLE);
                } else {
                    edit.setVisibility(View.GONE);
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
        config.setCommands(cmd);
    }

    static class Group {
        String name;
        int id;
        boolean checked;
        boolean editable;
    }

}
