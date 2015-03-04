package net.ugona.plus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class AboutFragment extends MainFragment {

    @Override
    int layout() {
        return R.layout.about;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        TextView tvVersion = (TextView) v.findViewById(R.id.version);
        tvVersion.setText(getString(R.string.version) + " " + State.getVersion(getActivity()));

        Button btn = (Button) v.findViewById(R.id.service);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ugona.net/"));
                startActivity(browserIntent);
            }
        });
        btn = (Button) v.findViewById(R.id.forum);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.ugona.net/topic47012.html"));
                startActivity(browserIntent);
            }
        });
        return v;
    }
}
