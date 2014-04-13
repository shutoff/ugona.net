package net.ugona.plus;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CarView extends FrameLayout {

    ImageView vCar;
    ImageView vEngine;
    TextView vT1;
    TextView vT2;
    TextView vT3;
    View view;

    public CarView(Context context) {
        super(context);
        init(context);
    }

    public CarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    void init(Context context) {
        view = LayoutInflater.from(context).inflate(R.layout.car, null);
        addView(view);
        vCar = (ImageView) view.findViewById(R.id.car);
        vEngine = (ImageView) view.findViewById(R.id.engine);
        vT1 = (TextView) view.findViewById(R.id.t1);
        vT2 = (TextView) view.findViewById(R.id.t2);
        vT3 = (TextView) view.findViewById(R.id.t3);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = 0;
        int y = 0;
        int w = vCar.getMeasuredWidth();
        int h = vCar.getMeasuredHeight();
        int ws = w;
        int hs = w * 386 / 256;
        if (hs > h) {
            ws = h * 256 / 386;
            hs = h;
            x = (w - ws) / 2;
        } else {
            y = (h - hs) / 2;
        }
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) vT1.getLayoutParams();
        int w1 = vT1.getMeasuredWidth();
        int x1 = x + ws * 54 / 256 - w1;
        if (x1 < 0)
            x1 = 0;
        lp.leftMargin = x1;
        lp.topMargin = y;

        lp = (RelativeLayout.LayoutParams) vT2.getLayoutParams();
        int w2 = vT2.getMeasuredWidth();
        int h2 = vT2.getMeasuredHeight();
        lp.leftMargin = x + ws * 120 / 256 - w2 / 2;
        lp.topMargin = y + hs * 152 / 386 - h2 / 2;

        lp = (RelativeLayout.LayoutParams) vT3.getLayoutParams();
        int w3 = vT3.getMeasuredWidth();
        int h3 = vT3.getMeasuredHeight();
        lp.leftMargin = x + ws * 153 / 256 - w3 / 2;
        lp.topMargin = y + hs * 39 / 386 - h3 / 2;
        if (lp.topMargin < 10)
            lp.topMargin = 10;
        super.onLayout(changed, l, t, r, b);
    }

    void setDrawable(Drawable d) {
        if (d != null)
            vCar.setImageDrawable(d);
    }

    void setEngine(int id) {
        vEngine.setImageResource(id);
    }

    void setEngineVisible(boolean visible) {
        vEngine.setVisibility(visible ? VISIBLE : GONE);
        if (!visible) {
            AnimationDrawable animation = (AnimationDrawable) vEngine.getDrawable();
            animation.stop();
        }
    }

    void startAnimation() {
        if (vEngine.getVisibility() == GONE)
            return;
        AnimationDrawable animation = (AnimationDrawable) vEngine.getDrawable();
        animation.start();
    }

    void setT1(String t1) {
        vT1.setText(addPlus(t1));
    }

    void setT2(String t2) {
        vT2.setText(addPlus(t2));
    }

    void setT3(String t3) {
        vT3.setText(addPlus(t3));
    }

    String addPlus(String t) {
        t = t.substring(0, t.length() - 3);
        String first = t.substring(0, 1);
        if (first.equals("-") || first.equals("0"))
            return t;
        return "+" + t;
    }

}
