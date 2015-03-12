package net.ugona.plus;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class SplashActivity extends FragmentActivity implements DialogInterface.OnDismissListener, AuthDialog.Listener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.splash);
        AuthDialog authDialog = new AuthDialog(this, "", this);
        authDialog.show();
        authDialog.setOnDismissListener(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override
    public void auth_ok() {
        setResult(RESULT_OK);
    }
}
