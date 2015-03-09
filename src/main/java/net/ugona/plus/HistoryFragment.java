package net.ugona.plus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HistoryFragment extends MainFragment implements HistoryView.HistoryViewListener {

    HistoryView vHistory;
    View vNoData;
    View vError;
    View vProgress;
    String type;

    @Override
    int layout() {
        return R.layout.history;
    }

    @Override
    boolean isShowDate() {
        return true;
    }

    @Override
    void changeDate() {
        refresh();
    }

    @Override
    boolean canRefresh() {
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        vHistory = (HistoryView) v.findViewById(R.id.history);
        vNoData = v.findViewById(R.id.no_data);
        vProgress = v.findViewById(R.id.progress);
        vError = v.findViewById(R.id.error);
        vHistory.mListener = this;
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        vError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });
        if (type != null)
            refresh();
        return v;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.TITLE, vHistory.type);
    }

    @Override
    public void dataReady() {
        vHistory.setVisibility(View.VISIBLE);
        vNoData.setVisibility(View.GONE);
        vError.setVisibility(View.GONE);
        vProgress.setVisibility(View.GONE);
    }

    @Override
    public void noData() {
        vHistory.setVisibility(View.GONE);
        vNoData.setVisibility(View.VISIBLE);
        vError.setVisibility(View.GONE);
        vProgress.setVisibility(View.GONE);
    }

    @Override
    public void errorLoading() {
        vHistory.setVisibility(View.GONE);
        vNoData.setVisibility(View.GONE);
        vError.setVisibility(View.VISIBLE);
        vProgress.setVisibility(View.GONE);
    }

    void setArgs(Bundle args) {
        type = args.getString(Names.TITLE);
        if (vHistory != null) {
            vHistory.setVisibility(View.GONE);
            vNoData.setVisibility(View.GONE);
            vError.setVisibility(View.GONE);
            vProgress.setVisibility(View.VISIBLE);
            vHistory.init(getActivity(), id(), type, date());
        }
    }

    void refresh() {
        Bundle args = new Bundle();
        args.putString(Names.TITLE, type);
        setArgs(args);
    }

}
