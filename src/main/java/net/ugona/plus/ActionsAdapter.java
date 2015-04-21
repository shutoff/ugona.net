package net.ugona.plus;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Vector;

public class ActionsAdapter extends BaseAdapter {

    CarConfig carConfig;
    Vector<CarConfig.Command> commands;
    Context context;
    String car_id;

    ActionsAdapter(Context context, String car_id) {
        carConfig = CarConfig.get(context, car_id);
        fill(context, car_id);
    }

    void fill(Context context, String car_id) {
        this.context = context;
        this.car_id = car_id;
        CarConfig carConfig = CarConfig.get(context, car_id);
        commands = new Vector<>();
        CarConfig.Command[] cmds = carConfig.getCmd();
        if (cmds != null) {
            for (CarConfig.Command cmd : cmds) {
                if (cmd.icon == null)
                    continue;
                if ((cmd.inet != 0) || State.hasTelephony(context))
                    commands.add(cmd);
            }
        }
    }

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
            LayoutInflater inflater = LayoutInflater.from(context);
            v = inflater.inflate(R.layout.action_item, null);
        }
        ImageView vIcon = (ImageView) v.findViewById(R.id.icon);
        TextView tvName = (TextView) v.findViewById(R.id.name);
        TextView tvText = (TextView) v.findViewById(R.id.text);
        CarConfig.Command cmd = commands.get(position);
        String text = cmd.name;
        if (cmd.custom_name)
            text = carConfig.getCommandName(cmd.id);
        int pos = text.indexOf('\n');
        if (pos < 0) {
            tvName.setText(text);
            tvText.setVisibility(View.GONE);
        } else {
            tvName.setText(text.substring(0, pos));
            tvText.setText(text.substring(pos + 1));
            tvText.setVisibility(View.VISIBLE);
        }
        int icon = context.getResources().getIdentifier("icon_" + cmd.icon, "drawable", context.getPackageName());
        vIcon.setImageResource(icon);
        v.findViewById(R.id.progress).setVisibility(Commands.isProcessed(car_id, cmd) ? View.VISIBLE : View.INVISIBLE);
        return v;
    }

    void itemClick(int position, boolean longTap) {
        SendCommandFragment fragment = new SendCommandFragment();
        CarConfig.Command cmd = commands.get(position);
        if (Commands.cancel(context, car_id, cmd))
            return;
        Bundle args = new Bundle();
        args.putString(Names.ID, car_id);
        args.putInt(Names.COMMAND, cmd.id);
        args.putBoolean(Names.ROUTE, longTap);
        fragment.setArguments(args);
        MainActivity activity = (MainActivity) context;
        fragment.show(activity.getSupportFragmentManager(), "send");
    }
}
