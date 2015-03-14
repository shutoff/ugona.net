package net.ugona.plus;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.widget.Button;

public class StyledButton extends Button {
    public StyledButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        int color = Color.rgb(0, 0, 0);
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StyledButton);
            color = typedArray.getColor(R.styleable.StyledButton_bgColor, 0);
        }
        int gray = Color.rgb(0x80, 0x80, 0x80);

        float[] hsv = new float[3];

        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] / 1.5f;
        int pressed_color = Color.HSVToColor(hsv);

        int corner = (int) getResources().getDimension(R.dimen.button_corner);

        GradientDrawable pressed = new GradientDrawable(GradientDrawable.Orientation.BL_TR, new int[]{pressed_color, pressed_color});
        pressed.setCornerRadius(corner);
        GradientDrawable normal = new GradientDrawable(GradientDrawable.Orientation.BL_TR, new int[]{color, color});
        normal.setCornerRadius(corner);
        GradientDrawable disabled = new GradientDrawable(GradientDrawable.Orientation.BL_TR, new int[]{gray, gray});
        disabled.setCornerRadius(corner);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{-android.R.attr.state_enabled}, disabled);
        states.addState(new int[]{}, normal);

        setBackgroundDrawable(states);
    }

}
