package net.ugona.plus;

import android.content.Context;

public class CarLockWidget extends CarWidget {

    @Override
    boolean isLockScreen(Context context, int widgetID) {
        return true;
    }
}
