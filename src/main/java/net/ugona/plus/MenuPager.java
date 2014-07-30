package net.ugona.plus;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MenuPager extends ListView {

    ViewPager.OnPageChangeListener pageChangeListener;

    public MenuPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDivider(null);
        setDividerHeight(0);
    }

    void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        pageChangeListener = listener;
    }

    void setPager(final ViewPager pager) {
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return pager.getAdapter().getCount();
            }

            @Override
            public Object getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null)
                    v = inflater.inflate(R.layout.menu_item, null);
                String name = pager.getAdapter().getPageTitle(position).toString();
                TextView tv = (TextView) v.findViewById(R.id.name);
                tv.setText(name);
                if (pager.getCurrentItem() == position) {
                    tv.setTextColor(getResources().getColor(R.color.caldroid_holo_blue_light));
                    tv.setTypeface(null, Typeface.BOLD);
                } else {
                    tv.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
                    tv.setTypeface(null, Typeface.NORMAL);
                }
                return v;
            }
        });

        pager.getAdapter().registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onInvalidated() {
                BaseAdapter adapter = (BaseAdapter) getAdapter();
                adapter.notifyDataSetChanged();
            }
        });

        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                BaseAdapter adapter = (BaseAdapter) getAdapter();
                adapter.notifyDataSetChanged();
                if (pageChangeListener != null)
                    pageChangeListener.onPageSelected(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                pager.setCurrentItem(position, true);
            }
        });

    }

/*
        LinearLayout menu = (LinearLayout) findViewById(R.id.menu);
        final PagerAdapter adapter = pager.getAdapter();

        pager.getAdapter().registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });

        childs = new View[adapter.getCount()];

        OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                final int pos = (Integer) v.getTag();
                if (pager.isFakeDragging())
                    pager.endFakeDrag();
                pager.setCurrentItem(pos);
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
    */

}
