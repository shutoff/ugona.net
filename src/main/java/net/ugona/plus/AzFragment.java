package net.ugona.plus;

public class AzFragment extends DeviceBaseFragment {

    final String PRESENT = "az_present";

    @Override
    int getTimer() {
        return 1;
    }

    @Override
    boolean filter(String id) {
        if ((id.length() > 3) && id.substring(0, 3).equals("az_")) {
            if (id.equals(PRESENT))
                return true;
            return getState();
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
        return false;
    }

    @Override
    void onChanged(String id) {
        if (id.equals(PRESENT))
            fill();
    }

    @Override
    boolean showTimers() {
        return getState();
    }
}
