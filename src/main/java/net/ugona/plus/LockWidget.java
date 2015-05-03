package net.ugona.plus;

import android.content.Context;

public class LockWidget extends Widget {

    static final int id_lock_layout[] = {
            R.layout.lock_widget,
            R.layout.lock_widget_light
    };

    @Override
    boolean isLockScreen(Context context, int widgetID) {
        return true;
    }

    int getLayoutId(Context context, int widgetId, int theme) {
        return id_lock_layout[theme];
    }
}
