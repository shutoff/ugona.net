package net.ugona.plus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

public abstract class MainFragment
        extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener {

    SwipeRefreshLayout mSwipeRefreshLayout;
    boolean initRefresh;

    abstract int layout();

    boolean isShowDate() {
        return false;
    }

    String getTitle() {
        return null;
    }

    String id() {
        MainFragment parent = (MainFragment) getParentFragment();
        if (parent != null)
            return parent.id();
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null)
            return null;
        return mainActivity.id;
    }

    long date() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null)
            return 0;
        return mainActivity.current;
    }

    @Override
    public void onRefresh() {
        if (mSwipeRefreshLayout != null) {
            if (!initRefresh) {
                initRefresh = true;
                TypedValue typed_value = new TypedValue();
                getActivity().getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, typed_value, true);
                mSwipeRefreshLayout.setProgressViewOffset(false, 0, getResources().getDimensionPixelSize(typed_value.resourceId));
            }
            mSwipeRefreshLayout.setRefreshing(true);
        }
    }

    void changeDate() {
    }

    void refreshDone() {
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(layout(), container, false);
        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.refresh_layout);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setOnRefreshListener(this);
            mSwipeRefreshLayout.setColorSchemeResources(R.color.main, R.color.refresh1, R.color.refresh2, R.color.refresh3);
        }
        return v;
    }

    String timeFormat(int minutes) {
        if (minutes < 60) {
            String s = getString(R.string.m_format);
            return String.format(s, minutes);
        }
        int hours = minutes / 60;
        minutes -= hours * 60;
        String s = getString(R.string.hm_format);
        return String.format(s, hours, minutes);
    }

    Menu combo() {
        return null;
    }

    int currentComboItem() {
        return 0;
    }

    void setupSideMenu(Menu menu) {
    }

}
