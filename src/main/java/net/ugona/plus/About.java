package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class About extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        TextView tvVersion = (TextView) findViewById(R.id.version);
        try {
            PackageManager pkgManager = getPackageManager();
            PackageInfo info = pkgManager.getPackageInfo("net.ugona.plus", 0);
            tvVersion.setText(getString(R.string.version) + " " + info.versionName);
        } catch (Exception ex) {
            // ignore
        }
        Button btn = (Button) findViewById(R.id.service);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ugona.net/"));
                startActivity(browserIntent);
            }
        });
        btn = (Button) findViewById(R.id.forum);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.ugona.net/topic325615.html"));
                startActivity(browserIntent);
            }
        });

    }

}
