package net.ugona.plus;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

public class IndicatorsView extends HorizontalScrollView {

    int offset;

    View vLeftArrow;
    View vRightArrow;

    public IndicatorsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndicatorsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        offset = metrics.densityDpi / 16;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        setupChildren();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        setupChildren();
        boolean bArrows = getChildAt(0).getWidth() > getWidth();
        vLeftArrow.setVisibility(bArrows ? VISIBLE : INVISIBLE);
        vRightArrow.setVisibility(bArrows ? VISIBLE : INVISIBLE);
    }

    void setupChildren() {
        ViewGroup layout = (ViewGroup) getChildAt(0);
        int count = layout.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = layout.getChildAt(i);
            if (v.getVisibility() == GONE)
                continue;
            int x = v.getLeft() - getScrollX();
            v.setVisibility((x + v.getWidth() < getWidth() + offset) ? VISIBLE : INVISIBLE);
        }
    }

    void setArrows(View vLeftArrow, View vRightArrow) {
        this.vLeftArrow = vLeftArrow;
        vLeftArrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup layout = (ViewGroup) getChildAt(0);
                int wChild = layout.getChildAt(0).getWidth();
                int scrollX = getScrollX() - wChild;
                if (scrollX < 0)
                    scrollX = 0;
                scrollTo(scrollX, 0);
            }
        });
        this.vRightArrow = vRightArrow;
        vRightArrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup layout = (ViewGroup) getChildAt(0);
                int wChild = layout.getChildAt(0).getWidth();
                int scrollX = getScrollX() + wChild;
                if (scrollX > getMaxScrollAmount())
                    scrollX = getMaxScrollAmount();
                scrollTo(scrollX, 0);
            }
        });
    }

}
