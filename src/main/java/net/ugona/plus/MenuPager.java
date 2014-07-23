package net.ugona.plus;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MenuPager extends LinearLayout {

    View[] childs;
    View selected;

    public MenuPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void setPager(final ViewPager pager) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout menu = (LinearLayout) findViewById(R.id.menu);
        PagerAdapter adapter = pager.getAdapter();
        childs = new View[adapter.getCount()];

        OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = (Integer) v.getTag();
                pager.setCurrentItem(pos, true);
            }
        };

        for (int i = 0; i < adapter.getCount(); i++) {
            String name = adapter.getPageTitle(i).toString();
            View v = inflater.inflate(R.layout.menu_item, null);
            TextView tv = (TextView) v.findViewById(R.id.name);
            tv.setText(name);
            menu.addView(v);
            v.setTag(i);
            v.setOnClickListener(onClickListener);
            childs[i] = v;
        }

        ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                if (selected == childs[i])
                    return;
                if (selected != null) {
                    TextView tv = (TextView) selected.findViewById(R.id.name);
                    tv.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
                    tv.setTypeface(null, Typeface.NORMAL);
                }
                selected = childs[i];
                TextView tv = (TextView) selected.findViewById(R.id.name);
                tv.setTextColor(getResources().getColor(R.color.caldroid_holo_blue_light));
                tv.setTypeface(null, Typeface.BOLD);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        };

        pager.setOnPageChangeListener(pageChangeListener);
        pageChangeListener.onPageSelected(pager.getCurrentItem());

    }
}
