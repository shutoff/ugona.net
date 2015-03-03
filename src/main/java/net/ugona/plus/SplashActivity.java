package net.ugona.plus;

import android.os.Bundle;

public class SplashActivity extends AuthDialog {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
    }
}
