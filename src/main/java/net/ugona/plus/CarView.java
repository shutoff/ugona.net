package net.ugona.plus;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class CarView extends FrameLayout {

    ImageView vCar;
    ImageView vEngine;
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
    }

    void setDrawable(Drawable d) {
        if (d == null)
            return;
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

}
