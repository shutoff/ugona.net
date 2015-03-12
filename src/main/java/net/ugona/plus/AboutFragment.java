package net.ugona.plus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutFragment extends MainFragment implements View.OnClickListener {

    @Override
    int layout() {
        return R.layout.about;
    }

    @Override
    String getTitle() {
        return getString(R.string.about);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        TextView tvVersion = (TextView) v.findViewById(R.id.version);
        tvVersion.setText(getString(R.string.version) + " " + State.getVersion(getActivity()));

        v.findViewById(R.id.service).setOnClickListener(this);
        v.findViewById(R.id.forum).setOnClickListener(this);
        v.findViewById(R.id.vk).setOnClickListener(this);
        v.findViewById(R.id.fb).setOnClickListener(this);
        v.findViewById(R.id.tw).setOnClickListener(this);
        v.findViewById(R.id.yt).setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View v) {
        String url = null;
        switch (v.getId()) {
            case R.id.service:
                url = "http://www.ugona.net/";
                break;
            case R.id.forum:
                url = "http://forum.ugona.net/topic47012.html";
                break;
            case R.id.vk:
                url = "http://vkontakte.ru/club10143414";
                break;
            case R.id.fb:
                url = "https://www.facebook.com/pages/%D0%A3%D0%B3%D0%BE%D0%BD%D0%B0%D0%BD%D0%B5%D1%82/582072695173014";
                break;
            case R.id.tw:
                url = "http://twitter.com/ugona_net";
                break;
            case R.id.yt:
                url = "http://www.youtube.com/user/kurchanovalex?feature=results_main";
                break;
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}
