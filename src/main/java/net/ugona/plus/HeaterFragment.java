package net.ugona.plus;

public class HeaterFragment extends DeviceBaseFragment {

    final static String PRESENT = "heater_present";
    boolean prev_state;

    @Override
    int getTimer() {
        return 2;
    }

    @Override
    boolean filter(String id) {
        if ((id.length() > 7) && id.substring(0, 7).equals("heater_")) {
            if (id.equals(PRESENT))
                return true;
            prev_state = getState();
            return prev_state;
        }
        return false;
    }

    boolean getState() {
        Object o = changed.get(PRESENT);
        if (o == null)
            o = settings.get(PRESENT);
        if (o == null)
            return true;
        if (o instanceof Boolean)
            return (Boolean) o;
        if (o instanceof Integer)
            return ((Integer) o != 0);
        return false;
    }

    @Override
    void onChanged(String id) {
        if (id.equals(PRESENT) && (prev_state != getState()))
            fill();
    }

    @Override
    boolean showTimers() {
        return getState();
    }
}
