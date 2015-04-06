package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

    static final int PRIMARY_START_ID = 20;

    ViewPager vPager;
    PagerSlidingTabStrip tabs;

    CarState state;
    BroadcastReceiver br;

    Menu carsMenu;

    boolean show_page[] = new boolean[6];

    @Override
    int layout() {
        return R.layout.primary;
    }

    @Override
    void setupSideMenu(Menu menu) {
        if (tabs != null)
            return;
        MenuItem item = menu.add(0, 0, 10, R.string.home);
        item.setIcon(R.drawable.ic_home);
        PagerAdapter adapter = vPager.getAdapter();
        int order = PRIMARY_START_ID;
        for (int i = 0; i < adapter.getCount(); i++) {
            menu.add(1, order, order, adapter.getPageTitle(i));
            order++;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vPager = (ViewPager) v.findViewById(R.id.pager);
        tabs = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);
        state = CarState.get(getActivity(), id());
        setTabs();
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null)
                    return;
                if (intent.getAction().equals(Names.CAR_CHANGED)) {
                    carsMenu = null;
                    int page_id = getPageId(vPager.getCurrentItem());
                    state = CarState.get(getActivity(), id());
                    setPageState();
                    vPager.setAdapter(new PagesAdapter(getChildFragmentManager()));
                    if (tabs != null)
                        tabs.notifyDataSetChanged();
                    vPager.setCurrentItem(getPagePosition(page_id));
                    return;
                }
                if (!id().equals(intent.getStringExtra(Names.ID)))
                    return;
                if (intent.getAction().equals(Names.CONFIG_CHANGED)) {
                    updatePages();
                    carsMenu = null;
                }
            }
        };
        IntentFilter intFilter = new IntentFilter(Names.CONFIG_CHANGED);
        intFilter.addAction(Names.CAR_CHANGED);
        getActivity().registerReceiver(br, intFilter);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(br);
    }

    @Override
    boolean isShowDate() {
        MainFragment fragment = getFragment(0);
        if (fragment == null)
            return false;
        return fragment.isShowDate();
    }

    @Override
    void changeDate() {
        MainFragment fragment = getFragment(0);
        if (fragment != null)
            fragment.changeDate();
        fragment = getFragment(-1);
        if (fragment != null)
            fragment.changeDate();
        fragment = getFragment(1);
        if (fragment != null)
            fragment.changeDate();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MainFragment fragment = getFragment(0);
        if (fragment != null) {
            fragment.onCreateOptionsMenu(menu, inflater);
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AppConfig appConfig = AppConfig.get(getActivity());
        String[] ids = appConfig.getIds().split(";");
        if ((item.getItemId() > 0) && (item.getItemId() <= ids.length)) {
            MainActivity activity = (MainActivity) getActivity();
            activity.setCarId(ids[item.getItemId() - 1]);
            return true;
        }
        PagerAdapter adapter = vPager.getAdapter();
        if ((item.getItemId() >= PRIMARY_START_ID) && (item.getItemId() < PRIMARY_START_ID + adapter.getCount())) {
            vPager.setCurrentItem(item.getItemId() - PRIMARY_START_ID, true);
            return true;
        }
        MainFragment fragment = getFragment(0);
        if ((fragment != null) && fragment.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    void setTabs() {
        if (vPager.getAdapter() != null)
            return;
        setPageState();
        vPager.setAdapter(new PagesAdapter(getChildFragmentManager()));
        if (tabs != null) {
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
        }
        vPager.setCurrentItem(getPagePosition(PAGE_STATE));
    }

    boolean setPageState() {
        boolean upd = false;
        if (show_page[PAGE_PHOTO] != state.isShow_photo()) {
            show_page[PAGE_PHOTO] = state.isShow_photo();
            upd = true;
        }
        if (!show_page[PAGE_ACTIONS]) {
            show_page[PAGE_ACTIONS] = true;
            upd = true;
        }
        if (!show_page[PAGE_STATE]) {
            show_page[PAGE_STATE] = true;
            upd = true;
        }
        if (!show_page[PAGE_EVENT]) {
            show_page[PAGE_EVENT] = true;
            upd = true;
        }
        if (show_page[PAGE_TRACK] != state.isShow_tracks()) {
            show_page[PAGE_TRACK] = state.isShow_tracks();
            upd = true;
        }
        if (show_page[PAGE_STAT] != state.isShow_tracks()) {
            show_page[PAGE_STAT] = state.isShow_tracks();
            upd = true;
        }
        return upd;
    }

    void updatePages() {
        int id = getPageId(vPager.getCurrentItem());
        if (!setPageState())
            return;
        vPager.setAdapter(new PagesAdapter(getChildFragmentManager()));
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
        return show_page[id];
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

    @Override
    Menu combo() {
        AppConfig appConfig = AppConfig.get(getActivity());
        String[] ids = appConfig.getIds().split(";");
        if (ids.length < 2)
            return null;
        if (carsMenu == null) {
            carsMenu = State.createMenu(getActivity());
            int item_id = 0;
            for (String id : ids) {
                CarConfig config = CarConfig.get(getActivity(), id);
                String name = config.getName();
                if (name.equals(""))
                    name = config.getLogin();
                ++item_id;
                carsMenu.add(1, item_id, item_id, name);
            }
        }
        return carsMenu;
    }

    @Override
    int currentComboItem() {
        AppConfig appConfig = AppConfig.get(getActivity());
        String[] ids = appConfig.getIds().split(";");
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(id()))
                return i + 1;
        }
        return 0;
    }

    class PagesAdapter extends FragmentStatePagerAdapter {

        public PagesAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
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
        public int getItemPosition(Object object) {
            int id = 0;
            if (object instanceof PhotoFragment)
                id = 0;
            if (object instanceof ActionFragment)
                id = 1;
            if (object instanceof StateFragment)
                id = 2;
            if (object instanceof EventsFragment)
                id = 3;
            if (object instanceof TracksFragment)
                id = 4;
            if (object instanceof StatFragment)
                id = 5;
            return getPagePosition(id);
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
