package net.ugona.plus;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;

public class DatePicker extends CalendarDatePickerDialog {

    @Override
    public void onDayOfMonthSelected(int year, int month, int day) {
        super.onDayOfMonthSelected(year, month, day);
        getView().findViewById(R.id.done).performClick();
    }
}
