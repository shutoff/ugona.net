package net.ugona.plus;

import android.app.Dialog;
import android.content.Context;
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

import java.util.Set;
import java.util.Vector;

public class FABCommands
        extends Dialog
        implements View.OnClickListener,
        ViewTreeObserver.OnGlobalLayoutListener,
        AdapterView.OnItemClickListener {

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

    public FABCommands(Context context, Set<Integer> cmds, String car_id) {
        super(context, R.style.CustomDialogTheme);
        setContentView(R.layout.commands);

        pkg = context.getPackageName();

        FrameLayout layout = (FrameLayout) findViewById(R.id.layout);
        layout.getViewTreeObserver().addOnGlobalLayoutListener(this);

        vFab = findViewById(R.id.fab);
        vFab.setOnClickListener(this);

        CarConfig config = CarConfig.get(getContext(), car_id);
        final Vector<CarConfig.Command> items = new Vector<>();
        CarConfig.Command[] commands = config.getCmd();
        for (CarConfig.Command c : commands) {
            if (cmds.contains(c.id))
                items.add(c);
        }

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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        dismiss();
    }
}
