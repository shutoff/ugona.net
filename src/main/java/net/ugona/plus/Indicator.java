package net.ugona.plus;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class Indicator extends FrameLayout {

    View v;
    TextView tvLabel;

    public Indicator(Context context) {
        this(context, null);
    }

    public Indicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.indicator, null, true);
        addView(v);
        tvLabel = (TextView) v.findViewById(R.id.text);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Indicator);
            int srcId = typedArray.getResourceId(R.styleable.Indicator_src, 0);
            if (srcId != 0) {
                ImageView img = (ImageView) v.findViewById(R.id.img);
                img.setImageResource(srcId);
            }
        }
    }

    void setText(CharSequence text) {
        tvLabel.setText(text);
    }

}