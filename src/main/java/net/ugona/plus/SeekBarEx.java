package net.ugona.plus;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.SeekBar;

public class SeekBarEx extends SeekBar {
    public SeekBarEx(Context context) {
        super(context);
    }

    public SeekBarEx(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SeekBarEx(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    ViewParent getParentScroll() {
        for (ViewParent v = getParent(); v != null; v = v.getParent()) {
            if (v instanceof ViewPager)
                return v;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        ViewParent v = getParentScroll();
        if (v != null) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    v.requestDisallowInterceptTouchEvent(true);
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    v.requestDisallowInterceptTouchEvent(false);
                    break;
            }
        }
        return super.onTouchEvent(event);
    }
}
