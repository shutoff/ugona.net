package net.ugona.plus;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Vector;

public class ActionFragment extends MainFragment {

    HoursList vActions;
    Vector<CarConfig.Command> commands;

    @Override
    int layout() {
        return R.layout.tracks;
    }

    @Override
    boolean canRefresh() {
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        commands = new Vector<>();
        CarConfig config = CarConfig.get(getActivity(), id());
        CarConfig.Command[] cmds = config.getCmd();
        if (cmds != null) {
            for (CarConfig.Command cmd : cmds) {
                if ((cmd.inet != 0) || State.hasTelephony(getActivity()))
                    commands.add(cmd);
            }
        }
        v.findViewById(R.id.summary).setVisibility(View.GONE);
        v.findViewById(R.id.first_progress).setVisibility(View.GONE);
        v.findViewById(R.id.progress).setVisibility(View.GONE);
        v.findViewById(R.id.loading).setVisibility(View.GONE);
        v.findViewById(R.id.space).setVisibility(View.GONE);
        vActions = (HoursList) v.findViewById(R.id.tracks);
        vActions.setVisibility(View.VISIBLE);
        vActions.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return commands.size();
            }

            @Override
            public Object getItem(int position) {
                return commands.get(position);
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
                    v = inflater.inflate(R.layout.action_item, null);
                }
                ImageView vIcon = (ImageView) v.findViewById(R.id.icon);
                TextView tvName = (TextView) v.findViewById(R.id.name);
                CarConfig.Command cmd = commands.get(position);
                tvName.setText(cmd.name);
                int icon = getResources().getIdentifier("icon_" + cmd.icon, "drawable", getActivity().getPackageName());
                vIcon.setImageResource(icon);
                return v;
            }
        });
        return v;
    }
}
