package net.ugona.plus;

public class TracksFragment extends MainFragment {

    @Override
    int layout() {
        return R.layout.tracks;
    }

    @Override
    boolean isShowDate() {
        return true;
    }
}
