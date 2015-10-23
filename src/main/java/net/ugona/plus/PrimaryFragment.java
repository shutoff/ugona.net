package net.ugona.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

public class PrimaryFragment
        extends MainFragment
        implements ViewPager.OnPageChangeListener {

    static final int PAGE_PHOTO = 0;
    static final int PAGE_ACTIONS = 1;
    static final int PAGE_STATE = 2;
    static final int PAGE_EVENT = 3;
    static final int PAGE_TRACK = 4;
    static final int PAGE_STAT = 5;
    static final int PAGE_SETTINGS = 6;

    static final int DO_PHONE = 1;
    static final int DO_MESSAGE = 2;
    static final int DO_TZ = 3;
    static final int DO_TIME = 4;

    static final int PRIMARY_START_ID = 20;

    ViewPager vPager;
    PagerSlidingTabStrip tabs;

    CarState state;
    BroadcastReceiver br;

    Handler handler;

    Menu carsMenu;

    boolean show_page[] = new boolean[7];

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
        if (vPager == null)
            return;
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
        vPager.setOffscreenPageLimit(2);
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
                    vPager.setAdapter(new PagesAdapter(getChildFragmentManager(), state.isPointer()));
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

        CarState state = CarState.get(getActivity(), id());
        CarConfig carConfig = CarConfig.get(getActivity(), id());
        if (state.isUse_phone() && carConfig.getPhone().equals("") && State.hasTelephony(getActivity())) {
            handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    InputPhoneDialog dialog = new InputPhoneDialog();
                    Bundle args = new Bundle();
                    args.putString(Names.TITLE, getString(R.string.device_phone_number));
                    dialog.setArguments(args);
                    dialog.setTargetFragment(PrimaryFragment.this, DO_PHONE);
                    dialog.show(getActivity().getSupportFragmentManager(), "phone");
                }
            });
        }
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DO_PHONE) {
            CarConfig carConfig = CarConfig.get(getActivity(), id());
            carConfig.setPhone(data.getStringExtra(Names.PHONE));
        }
        AppConfig appConfig = AppConfig.get(getContext());
        if (requestCode == DO_MESSAGE) {
            if (requestCode == DialogInterface.BUTTON_POSITIVE) {
                appConfig.setInfo_message("");
                appConfig.setInfo_title("");
                appConfig.setInfo_url("");
            } else if (requestCode == DialogInterface.BUTTON_NEUTRAL) {
                String url = appConfig.getInfo_url();
                appConfig.setInfo_message("");
                appConfig.setInfo_title("");
                appConfig.setInfo_url("");
                if (!url.equals("")) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            }
        }
        if (requestCode == DO_TZ) {
            if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                appConfig.setTz_warn(true);
            } else if (resultCode == DialogInterface.BUTTON_NEUTRAL) {
                String appPackageName = "ru.org.amip.ClockSync";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }
        }
        if (requestCode == DO_TIME) {
            if (resultCode == DialogInterface.BUTTON_POSITIVE)
                appConfig.setTime_warn(true);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(br);
        carsMenu = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        String message = null;
        String title = null;
        String more = null;
        int request_id = 0;
        AppConfig appConfig = AppConfig.get(getContext());
        if (!appConfig.getInfo_message().equals("")) {
            message = appConfig.getInfo_message();
            title = appConfig.getInfo_title();
            if (!appConfig.getInfo_url().equals(""))
                more = getString(R.string.more);
            request_id = DO_MESSAGE;
        } else {
            long delta = appConfig.getTime_delta();
            if (delta < 0)
                delta = -delta;
            boolean bad_time = (delta > 150000);
            boolean bad_tz = bad_time && ((delta + 150000) % 3600000 < 300000);
            if (bad_tz) {
                if (!appConfig.isTz_warn()) {
                    message = getString(R.string.bad_tz_warn);
                    title = getString(R.string.bad_tz);
                    more = getString(R.string.clock_sync);
                    request_id = DO_TZ;
                }
            } else if (bad_time) {
                if (!appConfig.isTime_warn()) {
                    message = getString(R.string.bad_time_warn);
                    title = getString(R.string.bad_time);
                    request_id = DO_TIME;
                }
            } else {
                appConfig.setTz_warn(false);
                appConfig.setTime_warn(false);
            }
        }
        if (message != null) {
            MessageDialog dialog = new MessageDialog();
            Bundle args = new Bundle();
            args.putString(Names.MESSAGE, message);
            if (title != null)
                args.putString(Names.TITLE, title);
            if (more != null)
                args.putString(Names.MORE, more);
            dialog.setArguments(args);
            dialog.setTargetFragment(this, request_id);
            dialog.show(getFragmentManager(), "info");
        }
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
        CarState carState = CarState.get(getActivity(), id());
        vPager.setAdapter(new PagesAdapter(getChildFragmentManager(), carState.isPointer()));
        if (tabs != null) {
            tabs.setViewPager(vPager);
            tabs.delegatePageListener = this;
        } else {
            vPager.setOnPageChangeListener(this);
        }
        vPager.setCurrentItem(getPagePosition(PAGE_STATE));
    }

    boolean setPageState() {
        boolean cmd = false;
        CarConfig carConfig = CarConfig.get(getActivity(), id());
        CarState carState = CarState.get(getActivity(), id());
        CarConfig.Command[] commands = carConfig.getCmd();
        if (commands != null) {
            for (CarConfig.Command command : commands) {
                if (command.icon != null) {
                    cmd = true;
                    break;
                }
            }
        }
        boolean upd = false;
        if (carState.isPointer()) {
            if (show_page[PAGE_PHOTO]) {
                show_page[PAGE_PHOTO] = false;
                upd = true;
            }
            if (show_page[PAGE_ACTIONS]) {
                show_page[PAGE_ACTIONS] = false;
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
            if (show_page[PAGE_TRACK]) {
                show_page[PAGE_TRACK] = false;
                upd = true;
            }
            if (show_page[PAGE_STAT]) {
                show_page[PAGE_STAT] = false;
                upd = true;
            }
            if (!show_page[PAGE_SETTINGS]) {
                show_page[PAGE_SETTINGS] = true;
                upd = true;
            }
        } else {
            if (show_page[PAGE_PHOTO] != state.isShow_photo()) {
                show_page[PAGE_PHOTO] = state.isShow_photo();
                upd = true;
            }
            if (show_page[PAGE_ACTIONS] != cmd) {
                show_page[PAGE_ACTIONS] = cmd;
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
            if (show_page[PAGE_SETTINGS]) {
                show_page[PAGE_SETTINGS] = false;
                upd = true;
            }
        }
        return upd;
    }

    void updatePages() {
        int id = getPageId(vPager.getCurrentItem());
        if (!setPageState())
            return;
        CarState carState = CarState.get(getActivity(), id());
        vPager.setAdapter(new PagesAdapter(getChildFragmentManager(), carState.isPointer()));
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
        for (int i = 0; i < 7; i++) {
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

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        MainActivity activity = (MainActivity) getActivity();
        activity.updateMenu();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    class PagesAdapter extends FragmentStatePagerAdapter {

        boolean pointer;

        public PagesAdapter(FragmentManager fm, boolean pointer) {
            super(fm);
            this.pointer = pointer;
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
                    if (pointer) {
                        fragment = new PointerStateFragment();
                    } else {
                        fragment = new StateFragment();
                    }
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
                case PAGE_SETTINGS:
                    fragment = new AuthFragment();
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
            if (object instanceof PointerStateFragment)
                id = 2;
            if (object instanceof EventsFragment)
                id = 3;
            if (object instanceof TracksFragment)
                id = 4;
            if (object instanceof StatFragment)
                id = 5;
            if (object instanceof AuthFragment)
                id = 6;
            return getPagePosition(id);
        }

        @Override
        public int getCount() {
            return getPagePosition(7);
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
                case PAGE_SETTINGS:
                    return getString(R.string.preferences);
            }
            return super.getPageTitle(position);
        }

    }
}
