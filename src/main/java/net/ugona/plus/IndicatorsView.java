package net.ugona.plus;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class IndicatorsView extends HorizontalScrollView {

    int offset;

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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ViewGroup layout = (ViewGroup) getChildAt(0);
        int max_width = 0;
        for (int i = 1; i < layout.getChildCount(); i++) {
            View v1 = layout.getChildAt(i - 1);
            View v = layout.getChildAt(i);
            v1.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            v.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            int w = (v1.getMeasuredWidth() + v.getMeasuredWidth()) / 2;
            if (w > max_width)
                max_width = w;
        }
        for (int i = 1; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            int w = (layout.getChildAt(i - 1).getMeasuredWidth() + v.getMeasuredWidth()) / 2;
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
            lp.leftMargin = max_width - w;
            v.setLayoutParams(lp);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        ViewGroup layout = (ViewGroup) getChildAt(0);
        setupChildren();
        if ((getScrollX() == 0) && (getChildAt(0).getWidth() > getWidth())) {
            int padding_left = layout.getPaddingLeft();
            float last_x = 0;
            for (int i = 0; i < layout.getChildCount(); i++) {
                View v = layout.getChildAt(i);
                float right = v.getLeft() + v.getWidth() - padding_left;
                if (right > getWidth())
                    continue;
                if (right > last_x)
                    last_x = right;
            }
            int d = (int) ((getWidth() - last_x) / 2);
            layout.setPadding(d, 0, 0, 0);
        }
    }

    void setupChildren() {
        ViewGroup layout = (ViewGroup) getChildAt(0);
        int count = layout.getChildCount();
        boolean scrolled = (getScrollX() > 0);
        for (int i = 0; i < count; i++) {
            View v = layout.getChildAt(i);
            if (v.getVisibility() == GONE)
                continue;
            if (scrolled) {
                v.setVisibility(VISIBLE);
                continue;
            }
            int x = v.getLeft() - getScrollX();
            v.setVisibility((x + v.getWidth() < getWidth() + offset) ? VISIBLE : INVISIBLE);
        }
        int w = getWidth();
        boolean bArrows = getChildAt(0).getWidth() > w;
        vRightArrow.setVisibility(bArrows ? VISIBLE : GONE);
        int childWidth = layout.getWidth();
        boolean isScrollable = getWidth() + getScrollX() < childWidth + getPaddingLeft() + getPaddingRight();
        vRightArrow.setEnabled(isScrollable);
    }

    void setArrows(View vRightArrow) {
        this.vRightArrow = vRightArrow;
        vRightArrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup layout = (ViewGroup) getChildAt(0);
                int wChild = layout.getChildAt(0).getWidth();
                int scrollX = getScrollX() + wChild;
                int maxScroll = layout.getWidth() + getPaddingLeft() + getPaddingRight() - getWidth();
                if (scrollX > maxScroll)
                    scrollX = maxScroll;
                scrollTo(scrollX, 0);
            }
        });
    }

}
