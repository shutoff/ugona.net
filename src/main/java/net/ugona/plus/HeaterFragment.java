package net.ugona.plus;

public class HeaterFragment extends DeviceBaseFragment {
    @Override
    boolean filter(String id) {
        return (id.length() > 7) && id.substring(0, 7).equals("heater_");
    }
}
