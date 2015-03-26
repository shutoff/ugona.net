package net.ugona.plus;

public class AzFragment extends DeviceBaseFragment {
    @Override
    boolean filter(String id) {
        return (id.length() > 3) && id.substring(0, 3).equals("az_");
    }
}
