package net.ugona.plus;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class CenteredScrollView extends ScrollView {

    public CenteredScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CenteredScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int padding = (getHeight() - getChildAt(0).getHeight()) / 2;
        if (padding < 0)
            padding = 0;
        View v = getChildAt(0);
        v.setPadding(0, padding, 0, 0);
    }
}
