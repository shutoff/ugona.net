package net.ugona.plus;

import android.content.Context;

public class LockWidget extends Widget {

    @Override
    boolean isLockScreen(Context context, int widgetID) {
        return true;
    }
}
