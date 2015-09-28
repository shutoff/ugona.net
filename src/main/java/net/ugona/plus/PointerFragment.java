package net.ugona.plus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

public class PointerFragment
        extends MainFragment
        implements ViewPager.OnPageChangeListener {

    ViewPager vPager;
    PagerSlidingTabStrip tabs;

    String car_id;

    @Override
    int layout() {
        return R.layout.primary;
    }

    @Override
    String id() {
        return car_id;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vPager = (ViewPager) v.findViewById(R.id.pager);
        tabs = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);
        setTabs();
        MainActivity activity = (MainActivity) getActivity();
        activity.checkCaps(car_id);
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        car_id = args.getString(Names.ID);
    }

    void setTabs() {
        if (vPager.getAdapter() != null)
            return;
        vPager.setAdapter(new PagesAdapter(getChildFragmentManager()));
        if (tabs != null) {
            tabs.setViewPager(vPager);
            tabs.delegatePageListener = this;
        } else {
            vPager.setOnPageChangeListener(this);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    class PagesAdapter extends FragmentStatePagerAdapter {

        public PagesAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            MainFragment fragment = null;
            switch (i) {
                case 0:
                    fragment = new PointerStateFragment();
                    break;
                case 1:
                    fragment = new EventsFragment();
                    break;
                case 2:
                    fragment = new AuthFragment();
                    break;
            }
            return fragment;
        }

        @Override
        public int getItemPosition(Object object) {
            if (object instanceof PointerStateFragment)
                return 0;
            if (object instanceof EventsFragment)
                return 1;
            if (object instanceof AuthFragment)
                return 2;
            return 0;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.state);
                case 1:
                    return getString(R.string.events);
                case 2:
                    return getString(R.string.preferences);
            }
            return super.getPageTitle(position);
        }

    }
}
