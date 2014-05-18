package net.ugona.plus;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class HistoryActivity extends ActionBarActivity {

    FrameLayout holder;
    History mPlot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        mPlot = (History) getLastCustomNonConfigurationInstance();
        initUI();
        Cars.Car[] cars = Cars.getCars(this);
        String title = "";
        if (cars.length > 1) {
            for (Cars.Car car : cars) {
                if (car.id.equals(mPlot.mHistory.car_id))
                    title = car.name + ": ";
            }
        }
        String type = mPlot.mHistory.type;
        if (type.equals("voltage")) {
            title += getString(R.string.voltage);
        } else if (type.equals("reserved")) {
            title += getString(R.string.reserved);
        } else if (type.equals("balance")) {
            title += getString(R.string.balance);
        } else {
            title += getString(R.string.temperature);
        }
        setTitle(title);
    }


    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        Object res = mPlot;
        if (mPlot != null) {
            holder.removeView(mPlot);
            mPlot = null;
        }
        return res;
    }

    void initUI() {
        holder = (FrameLayout) findViewById(R.id.webview);
        holder.setBackgroundColor(getResources().getColor(R.color.caldroid_gray));
        if (mPlot == null) {
            mPlot = new History(this, getIntent().getStringExtra(Names.ID), getIntent().getStringExtra(Names.STATE));
            mPlot.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        holder.addView(mPlot);
    }

    static class History extends FrameLayout implements HistoryView.HistoryViewListener {

        HistoryView mHistory;
        TextView tvNoData;
        View vProgress;
        View vError;

        public History(Context context, String car_id, String type) {
            super(context);
            addView(inflate(context, R.layout.history, null));
            tvNoData = (TextView) findViewById(R.id.no_data);
            vProgress = findViewById(R.id.progress);
            vError = findViewById(R.id.error);
            mHistory = (HistoryView) findViewById(R.id.history);
            mHistory.mListener = this;
            mHistory.init(car_id, type);
            vError.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    vError.setVisibility(GONE);
                    vProgress.setVisibility(VISIBLE);
                    mHistory.loadData();
                }
            });
        }

        @Override
        public void dataReady() {
            vProgress.setVisibility(GONE);
            mHistory.setVisibility(VISIBLE);
        }

        @Override
        public void noData() {
            vProgress.setVisibility(GONE);
            tvNoData.setVisibility(VISIBLE);
        }

        @Override
        public void errorLoading() {
            vProgress.setVisibility(GONE);
            vError.setVisibility(VISIBLE);
        }
    }

}
