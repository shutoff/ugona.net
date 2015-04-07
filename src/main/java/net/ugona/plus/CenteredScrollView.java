package net.ugona.plus;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class CenteredScrollView extends ScrollView {

    boolean layoutChanged;
    int selectedColor;
    int bottom;

    public CenteredScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CenteredScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CenteredScrollView);
            selectedColor = typedArray.getColor(R.styleable.CenteredScrollView_selectedColor, 0);
            bottom = typedArray.getDimensionPixelOffset(R.styleable.CenteredScrollView_bottom, 0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (layoutChanged) {
            layoutChanged = false;
            int h = getHeight() - bottom;
            View vChild = getChildAt(0);
            int ch = vChild.getHeight() - vChild.getPaddingTop();
            int padding = (h - ch) / 2;
            if (padding < 0)
                padding = 0;
            View v = getChildAt(0);
            if (v.getPaddingTop() != padding) {
                v.setPadding(0, padding, 0, 0);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        layoutChanged = true;
    }
}
