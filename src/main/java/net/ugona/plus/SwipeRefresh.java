package net.ugona.plus;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class SwipeRefresh extends SwipeRefreshLayout {
    public SwipeRefresh(Context context) {
        super(context);
    }

    public SwipeRefresh(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    boolean canChildScrollUp(View v) {
        if (v.getVisibility() != VISIBLE)
            return false;
        if (v instanceof HoursList) {
            HoursList vList = (HoursList) v;
            return vList.canScrollUp();
        }
        if (v instanceof ListView)
            return true;
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (canChildScrollUp(group.getChildAt(i)))
                    return true;
            }
        }
        if (v instanceof HistoryView)
            return true;
        return false;
    }

    @Override
    public boolean canChildScrollUp() {
        for (int i = 0; i < getChildCount(); i++) {
            if (canChildScrollUp(getChildAt(i)))
                return true;
        }
        return false;
    }

}
