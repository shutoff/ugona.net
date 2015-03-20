package net.ugona.plus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.LocalDate;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public abstract class MainFragment extends Fragment implements OnRefreshListener {

    PullToRefreshLayout mPullToRefreshLayout;

    abstract int layout();

    boolean isShowDate() {
        return false;
    }

    String getTitle() {
        return null;
    }

    String id() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null)
            return null;
        return mainActivity.id;
    }

    LocalDate date() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null)
            return null;
        return mainActivity.current;
    }

    void refresh() {
        refreshDone();
    }

    void changeDate() {
    }

    void refreshDone() {
        try {
            mPullToRefreshLayout.setRefreshComplete();
        } catch (Exception ex) {
            // ignore
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (view instanceof uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout) {
            mPullToRefreshLayout = (PullToRefreshLayout) view;
            ActionBarPullToRefresh.from(getActivity())
                    .allChildrenArePullable()
                    .listener(this)
                    .setup(mPullToRefreshLayout);
            mPullToRefreshLayout.setPullEnabled(true);
        }
/*
        ViewGroup viewGroup = (ViewGroup) view;
        mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());
        ActionBarPullToRefresh.from(getActivity())
        //        .insertLayoutInto(viewGroup)
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
        mPullToRefreshLayout.setPullEnabled(true);
*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(layout(), container, false);
    }

    @Override
    public void onRefreshStarted(View view) {
        refresh();
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

}
