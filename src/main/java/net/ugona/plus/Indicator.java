package net.ugona.plus;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class Indicator extends FrameLayout {

    public Indicator(Context context) {
        this(context, null);
    }

    public Indicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.indicator, null, true);
        addView(v);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Indicator);
            int srcId = typedArray.getResourceId(R.styleable.Indicator_src, 0);
            if (srcId != 0) {
                ImageView img = (ImageView) v.findViewById(R.id.img);
                img.setImageResource(srcId);
            }
        }

    }

}
