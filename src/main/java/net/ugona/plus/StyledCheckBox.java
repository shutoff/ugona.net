package net.ugona.plus;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.widget.CheckBox;

public class StyledCheckBox extends CheckBox {

    public StyledCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        Drawable unchecked = context.getResources().getDrawable(R.drawable.abc_btn_check_to_on_mtrl_000);
        Drawable checked = context.getResources().getDrawable(R.drawable.abc_btn_check_to_on_mtrl_015);
        unchecked.setAlpha(50);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_checked}, unchecked);
        states.addState(new int[]{}, unchecked);
        setButtonDrawable(states);
    }
}
