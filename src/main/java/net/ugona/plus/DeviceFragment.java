package net.ugona.plus;

public class DeviceFragment extends DeviceBaseFragment {
    @Override
    boolean filter(String id) {
        if (id.substring(0, 3).equals("az_"))
            return false;
        if (id.substring(0, 7).equals("heater_"))
            return false;
        return true;
    }
}
