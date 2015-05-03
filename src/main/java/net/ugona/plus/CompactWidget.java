package net.ugona.plus;

import android.content.Context;

public class CompactWidget extends Widget {

    static final int id_layout[] = {
            R.layout.compact_widget,
            R.layout.compact_widget_light
    };

    int getLayoutId(Context context, int widgetId, int theme) {
        return id_layout[theme];
    }

    String getPrefix(String prefix) {
        if (prefix.equals(""))
            prefix = "c";
        return "w" + prefix;
    }
}
