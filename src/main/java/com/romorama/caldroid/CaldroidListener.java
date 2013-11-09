package com.romorama.caldroid;

import android.view.View;

import java.util.Date;

/**
 * CaldroidListener inform when user clicks on a valid date (not within disabled
 * dates, and valid between min/max dates)
 * <p/>
 * The method onChangeMonth is optional, user can always override this to listen
 * to month change event
 *
 * @author thomasdao
 */
public abstract class CaldroidListener {
    public abstract void onSelectDate(Date date, View view);

    public void onChangeMonth(int month, int year) {
        // Do nothing
    }

    ;
}
