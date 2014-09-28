package net.ugona.plus;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class HoursList extends FrameLayout {

    ListView list;
    LinearLayout vHours;
    Listener listener;

    public HoursList(Context context) {
        super(context);
        init(context);
    }

    public HoursList(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HoursList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        list.post(new Runnable() {
            @Override
            public void run() {
                setHoursVisible();
            }
        });
    }

    void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.hours_list, null);
        addView(view);

        list = (ListView) view.findViewById(R.id.list);
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                TextView tv = (TextView) v;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        tv.setTextColor(getResources().getColor(R.color.hour_active));
                        return true;
                    case MotionEvent.ACTION_UP: {
                        int h = 0;
                        try {
                            h = Integer.parseInt(tv.getText().toString());
                        } catch (Exception ex) {
                            // ignore
                        }
                        if (listener != null)
                            list.setSelection(listener.setHour(h));
                    }
                    case MotionEvent.ACTION_CANCEL:
                        tv.setTextColor(getResources().getColor(R.color.hour));
                        return true;
                }
                return false;
            }
        };

        vHours = (LinearLayout) view.findViewById(R.id.hours);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f);
        for (int i = 22; i > 0; i -= 2) {
            TextView tView = (TextView) LayoutInflater.from(context).inflate(R.layout.hours_item, null);
            tView.setText(i + "");
            vHours.addView(tView, params);
            tView.setOnTouchListener(touchListener);
        }
    }

    void setListener(Listener l) {
        listener = l;
    }

    void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        list.setOnItemClickListener(listener);
    }

    void setAdapter(BaseAdapter adapter) {
        list.setAdapter(adapter);
        setHoursVisible();
    }

    void notifyChanges() {
        BaseAdapter adapter = (BaseAdapter) list.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            setHoursVisible();
        }
    }

    void setHoursVisible() {
        if (listener == null) {
            vHours.setVisibility(GONE);
            return;
        }
        list.post(new Runnable() {
            @Override
            public void run() {
                BaseAdapter adapter = (BaseAdapter) list.getAdapter();
                if (adapter == null) {
                    vHours.setVisibility(GONE);
                    return;
                }
                int first = list.getFirstVisiblePosition();
                int last = list.getLastVisiblePosition();
                vHours.setVisibility(((first > 0) || (last < adapter.getCount() - 1)) ? VISIBLE : GONE);
            }
        });
    }

    abstract static interface Listener {

        abstract int setHour(int h);

    }

}
