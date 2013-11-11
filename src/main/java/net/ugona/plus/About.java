package net.ugona.plus;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
    }

}
