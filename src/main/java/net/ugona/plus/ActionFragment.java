package net.ugona.plus;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Vector;

public class ActionFragment
        extends MainFragment
        implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    HoursList vActions;
    Vector<CarConfig.Command> commands;
    boolean longTap;

    @Override
    int layout() {
        return R.layout.tracks;
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
        v.findViewById(R.id.footer).setVisibility(View.GONE);
        v.findViewById(R.id.first_progress).setVisibility(View.GONE);
        v.findViewById(R.id.progress).setVisibility(View.GONE);
        v.findViewById(R.id.loading).setVisibility(View.GONE);
        v.findViewById(R.id.space).setVisibility(View.GONE);
        vActions = (HoursList) v.findViewById(R.id.tracks);
        vActions.disableDivider();
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MainActivity activity = (MainActivity) getActivity();
        activity.do_command(commands.get(position).id, longTap);
        longTap = false;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(700);
        } catch (Exception ex) {
            // ignore
        }
        longTap = true;
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action, menu);
    }
}
