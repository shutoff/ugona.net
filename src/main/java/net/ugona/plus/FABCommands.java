package net.ugona.plus;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Vector;

public class FABCommands
        extends Dialog
        implements View.OnClickListener,
        ViewTreeObserver.OnGlobalLayoutListener,
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    static int[] bg = {
            R.drawable.bg_cmd1,
            R.drawable.bg_cmd2,
            R.drawable.bg_cmd3,
            R.drawable.bg_cmd4,
            R.drawable.bg_cmd5
    };
    View vFab;
    ListView vList;
    String pkg;
    Vector<CarConfig.Command> items;
    boolean longTap;
    String car_id;

    public FABCommands(MainActivity activity, final Vector<CarConfig.Command> items, String car_id) {
        super(activity, R.style.CustomDialogTheme);
        setOwnerActivity(activity);
        this.items = items;
        this.car_id = car_id;

        setContentView(R.layout.fab);
        pkg = activity.getPackageName();
        FrameLayout layout = (FrameLayout) findViewById(R.id.layout);
        layout.getViewTreeObserver().addOnGlobalLayoutListener(this);

        vFab = findViewById(R.id.fab);
        vFab.setOnClickListener(this);

        vList = (ListView) findViewById(R.id.list);
        vList.setDivider(null);
        vList.setDividerHeight(0);
        vList.setAdapter(new BaseAdapter() {
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
                if (v == null)
                    v = getLayoutInflater().inflate(R.layout.fab_item, null);
                CarConfig.Command cmd = items.get(position);
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(cmd.name);
                ImageView iv = (ImageView) v.findViewById(R.id.icon);
                int id = getContext().getResources().getIdentifier("b_" + cmd.icon, "drawable", pkg);
                if (id == 0) {
                    iv.setVisibility(View.GONE);
                } else {
                    iv.setVisibility(View.VISIBLE);
                    iv.setImageResource(id);
                    id = bg[position % bg.length];
                    if (cmd.icon.equals("blocking"))
                        id = R.drawable.bg_blocking;
                    iv.setBackgroundResource(id);
                }
                return v;
            }
        });
        vList.setOnItemClickListener(this);
        vList.setOnItemLongClickListener(this);

        getWindow().getAttributes().windowAnimations = R.style.PauseDialogAnimation;
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }


    @Override
    public void onGlobalLayout() {
        int[] list_pos = new int[2];
        vList.getLocationInWindow(list_pos);
        int[] fab_pos = new int[2];
        vFab.getLocationInWindow(fab_pos);
        ListAdapter mAdapter = vList.getAdapter();
        int totalHeight = 0;
        for (int i = 0; i < mAdapter.getCount(); i++) {
            View mView = mAdapter.getView(i, null, vList);
            mView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

            totalHeight += mView.getMeasuredHeight();
        }
        int new_pos = fab_pos[1] - list_pos[1] - totalHeight - 40;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) vList.getLayoutParams();
        new_pos += lp.topMargin;
        if (new_pos < 0)
            new_pos = 0;
        if (new_pos != lp.topMargin) {
            lp.topMargin = new_pos;
            vList.setLayoutParams(lp);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        dismiss();
        CarConfig.Command cmd = items.get(position);
        SendCommandFragment fragment = new SendCommandFragment();
        Bundle args = new Bundle();
        args.putString(Names.ID, car_id);
        args.putInt(Names.COMMAND, cmd.id);
        args.putBoolean(Names.ROUTE, longTap);
        fragment.setArguments(args);
        MainActivity activity = (MainActivity) getOwnerActivity();
        fragment.show(activity.getSupportFragmentManager(), "send");
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(700);
        } catch (Exception ex) {
            // ignore
        }
        longTap = true;
        return false;
    }

}
