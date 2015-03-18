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
        final CarConfig.Command[] cmd = config.getCmd();
        vList.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return cmd.length;
            }

            @Override
            public Object getItem(int position) {
                return cmd[position];
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
                TextView tv = (TextView) v.findViewById(R.id.title);
                tv.setText(cmd[position].name);
                return v;
            }
        });
        return v;
    }
}
