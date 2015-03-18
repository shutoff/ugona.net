package net.ugona.plus;

public class AzFragment extends DeviceBaseFragment {
    @Override
    boolean filter(String id) {
        return id.substring(0, 3).equals("az_");
    }
}
