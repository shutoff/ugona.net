package net.ugona.plus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.androidplot.xy.XYPlot;

public class HistoryActivity extends ActionBarActivity {

    SharedPreferences preferences;
    FrameLayout holder;
    XYPlot mPlot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.webview);
        mPlot = (XYPlot) getLastCustomNonConfigurationInstance();
        initUI();
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
            mPlot = new XYPlot(this, "");
            mPlot.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        holder.addView(mPlot);
    }
}
