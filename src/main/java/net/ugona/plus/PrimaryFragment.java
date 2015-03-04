package net.ugona.plus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

public class PrimaryFragment extends MainFragment {

    static final int PAGE_PHOTO = 0;
    static final int PAGE_ACTIONS = 1;
    static final int PAGE_STATE = 2;
    static final int PAGE_EVENT = 3;
    static final int PAGE_TRACK = 4;
    static final int PAGE_STAT = 5;

    ViewPager vPager;
    PagerSlidingTabStrip tabs;

    CarState state;

    @Override
    int layout() {
        return R.layout.primary;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vPager = (ViewPager) v.findViewById(R.id.pager);
        tabs = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);
        state = CarState.get(getActivity(), id());
        setTabs();
        return v;
    }

    @Override
    boolean isShowDate() {
        MainFragment fragment = getFragment(0);
        if (fragment == null)
            return false;
        return fragment.isShowDate();
    }

    @Override
    Menu menu() {
        MainFragment fragment = getFragment(0);
        if (fragment == null)
            return null;
        return fragment.menu();
    }

    void setTabs() {
        if (vPager.getAdapter() != null)
            return;
        vPager.setAdapter(new PagerAdapter(getChildFragmentManager()));
        tabs.setViewPager(vPager);
        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                MainActivity activity = (MainActivity) getActivity();
                activity.updateMenu();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        vPager.setCurrentItem(getPagePosition(PAGE_STATE));
    }

    void updatePages() {
        int id = getPageId(vPager.getCurrentItem());
        vPager.getAdapter().notifyDataSetChanged();
        if (tabs != null)
            tabs.notifyDataSetChanged();
        vPager.setCurrentItem(getPagePosition(id));
    }

    MainFragment getFragment(int position) {
        FragmentStatePagerAdapter adapter = (FragmentStatePagerAdapter) vPager.getAdapter();
        if (adapter == null)
            return null;
        int pos = vPager.getCurrentItem() + position;
        if ((pos < 0) || (pos >= adapter.getCount()))
            return null;
        return (MainFragment) adapter.instantiateItem(vPager, vPager.getCurrentItem() + position);
    }

    boolean isShowPage(int id) {
        switch (id) {
            case PAGE_PHOTO:
                return state.isShow_photo();
            case PAGE_ACTIONS:
            case PAGE_STATE:
            case PAGE_EVENT:
                return true;
            case PAGE_TRACK:
            case PAGE_STAT:
                return state.isShow_tracks();
        }
        return false;
    }

    int getPageId(int n) {
        int last = 0;
        for (int i = 0; i < 6; i++) {
            if (!isShowPage(i))
                continue;
            last = i;
            if (n == 0)
                return i;
            n--;
        }
        return last;
    }

    int getPagePosition(int id) {
        int pos = 0;
        for (int i = 0; i < id; i++) {
            if (isShowPage(i))
                pos++;
        }
        return pos;
    }

    class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Log.v("v", "getItem " + i + ", " + getPageId(i));
            MainFragment fragment = null;
            switch (getPageId(i)) {
                case PAGE_PHOTO:
                    fragment = new PhotoFragment();
                    break;
                case PAGE_ACTIONS:
                    fragment = new ActionFragment();
                    break;
                case PAGE_STATE:
                    fragment = new StateFragment();
                    break;
                case PAGE_EVENT:
                    fragment = new EventsFragment();
                    break;
                case PAGE_TRACK:
                    fragment = new TracksFragment();
                    break;
                case PAGE_STAT:
                    fragment = new StatFragment();
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return getPagePosition(6);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (getPageId(position)) {
                case PAGE_PHOTO:
                    return getString(R.string.photo);
                case PAGE_ACTIONS:
                    return getString(R.string.control);
                case PAGE_STATE:
                    return getString(R.string.state);
                case PAGE_EVENT:
                    return getString(R.string.events);
                case PAGE_TRACK:
                    return getString(R.string.tracks);
                case PAGE_STAT:
                    return getString(R.string.stat);
            }
            return super.getPageTitle(position);
        }
    }

}
