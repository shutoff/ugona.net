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

    void updateCarImage(Context context, CarState carState) {
        if (carImage == null)
            carImage = new CarImage(context, "compact");
        carImage.update(carState, true);
    }

}
