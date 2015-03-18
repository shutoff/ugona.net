package net.ugona.plus;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class CommandsFragment extends MainFragment {

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

        final Map<String, Integer> groups = new HashMap<>();

        final CarConfig.Command[] cmd = config.getCmd();
        for (CarConfig.Command c : cmd) {
            if (c.group == null)
                continue;
            String name = c.group;
            if (name.equals(""))
                name = c.name;
            if (groups.containsKey(name))
                continue;
            groups.put(name, c.id);
        }
        Set<Map.Entry<String, Integer>> entries = groups.entrySet();
        final Vector<Group> grp = new Vector<>();
        int[] selected = config.getCommands();
        for (Map.Entry<String, Integer> entry : entries) {
            Group g = new Group();
            g.name = entry.getKey();
            g.id = entry.getValue();
            for (int s : selected) {
                if (s == g.id)
                    g.checked = true;
            }
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
                Group g = grp.get(position);
                TextView tv = (TextView) v.findViewById(R.id.title);
                tv.setText(g.name);
                return v;
            }
        });
        return v;
    }

    static class Group {
        String name;
        int id;
        boolean checked;
    }

}
