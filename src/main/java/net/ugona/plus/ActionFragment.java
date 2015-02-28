package net.ugona.plus;

public class ActionFragment extends MainFragment {

    @Override
    int layout() {
        return R.layout.tracks;
    }

    @Override
    boolean canRefresh() {
        return false;
    }
}
