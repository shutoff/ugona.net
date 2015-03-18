package net.ugona.plus;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

import java.util.HashMap;


public class SettingsFragment extends MainFragment implements Alert.Listener {

    static final int PAGE_AUTH = 0;
    static final int PAGE_NOTIFY = 1;
    static final int PAGE_WIDGETS = 2;
    static final int PAGE_COMMANDS = 3;
    static final int PAGE_DEVICE = 4;
    static final int PAGE_AZ = 5;
    static final int PAGE_HEATER = 6;
    static final int PAGE_ZONES = 7;

    boolean show_page[] = new boolean[8];

    ViewPager vPager;
    PagerSlidingTabStrip tabs;
    Handler handler;

    HashMap<String, Object> changed;

    @Override
    int layout() {
        return R.layout.primary;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vPager = (ViewPager) v.findViewById(R.id.pager);
        tabs = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);
        setPageState();
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
        vPager.setCurrentItem(getPagePosition(PAGE_AUTH));
        handler = new Handler();
        return v;
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
        if (item.getItemId() == android.R.id.home) {
            FragmentStatePagerAdapter adapter = (FragmentStatePagerAdapter) vPager.getAdapter();
            if (adapter == null)
                return super.onOptionsItemSelected(item);
            for (int i = 0; i < adapter.getCount(); i++) {
                adapter.startUpdate(vPager);
                MainFragment fragment = (MainFragment) adapter.instantiateItem(vPager, i);
                adapter.finishUpdate(vPager);
                if (fragment == null)
                    continue;
                if (!fragment.onOptionsItemSelected(item))
                    continue;
                vPager.setCurrentItem(i);
                Alert alert = new Alert();
                Bundle args = new Bundle();
                args.putString(Names.TITLE, adapter.getPageTitle(i).toString());
                args.putString(Names.MESSAGE, getString(R.string.changed_msg));
                args.putString(Names.OK, getString(R.string.exit));
                alert.setArguments(args);
                alert.setListener(this);
                alert.show(getFragmentManager(), "alert");
                adapter.finishUpdate(vPager);
                return true;
            }
        }
        MainFragment fragment = getFragment(0);
        if ((fragment != null) && fragment.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
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

    boolean setPageState() {
        boolean upd = false;
        if (!show_page[PAGE_AUTH]) {
            show_page[PAGE_AUTH] = true;
            upd = true;
        }
        if (!show_page[PAGE_NOTIFY]) {
            show_page[PAGE_NOTIFY] = true;
            upd = true;
        }
        if (!show_page[PAGE_WIDGETS]) {
            show_page[PAGE_WIDGETS] = true;
            upd = true;
        }
        if (!show_page[PAGE_COMMANDS]) {
            show_page[PAGE_COMMANDS] = true;
            upd = true;
        }
        if (!show_page[PAGE_DEVICE]) {
            show_page[PAGE_DEVICE] = true;
            upd = true;
        }
        if (!show_page[PAGE_AZ]) {
            show_page[PAGE_AZ] = true;
            upd = true;
        }
        if (!show_page[PAGE_HEATER]) {
            show_page[PAGE_HEATER] = true;
            upd = true;
        }
        if (!show_page[PAGE_ZONES]) {
            show_page[PAGE_ZONES] = true;
            upd = true;
        }
        return upd;
    }

    boolean isShowPage(int id) {
        return show_page[id];
    }

    int getPageId(int n) {
        int last = 0;
        for (int i = 0; i < show_page.length; i++) {
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
    public void ok() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                getActivity().getSupportFragmentManager().popBackStackImmediate();
            }
        });
    }

    class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            MainFragment fragment = null;
            switch (getPageId(i)) {
                case PAGE_AUTH:
                    fragment = new AuthFragment();
                    break;
                case PAGE_NOTIFY:
                    fragment = new NotifyFragment();
                    break;
                case PAGE_WIDGETS:
                    fragment = new WidgetsFragment();
                    break;
                case PAGE_COMMANDS:
                    fragment = new CommandsFragment();
                    break;
                case PAGE_DEVICE:
                    fragment = new DeviceFragment();
                    break;
                case PAGE_AZ:
                    fragment = new AzFragment();
                    break;
                case PAGE_HEATER:
                    fragment = new HeaterFragment();
                    break;
                case PAGE_ZONES:
                    fragment = new ZonesFragment();
                    break;
            }
            return fragment;
        }

        @Override
        public int getItemPosition(Object object) {
            int id = 0;
            if (object instanceof AuthFragment)
                id = 0;
            if (object instanceof NotifyFragment)
                id = 1;
            if (object instanceof WidgetsFragment)
                id = 2;
            if (object instanceof CommandsFragment)
                id = 3;
            if (object instanceof DeviceBaseFragment)
                id = 4;
            if (object instanceof AzFragment)
                id = 5;
            if (object instanceof HeaterFragment)
                id = 6;
            if (object instanceof ZonesFragment)
                id = 7;
            return getPagePosition(id);
        }

        @Override
        public int getCount() {
            return getPagePosition(show_page.length);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (getPageId(position)) {
                case PAGE_AUTH:
                    return getString(R.string.auth);
                case PAGE_NOTIFY:
                    return getString(R.string.notifications);
                case PAGE_WIDGETS:
                    return getString(R.string.widgets);
                case PAGE_COMMANDS:
                    return getString(R.string.commands);
                case PAGE_DEVICE:
                    return getString(R.string.device_settings);
                case PAGE_AZ:
                    return getString(R.string.autostart);
                case PAGE_HEATER:
                    return getString(R.string.rele);
                case PAGE_ZONES:
                    return getString(R.string.zones);
            }
            return super.getPageTitle(position);
        }
    }

}
