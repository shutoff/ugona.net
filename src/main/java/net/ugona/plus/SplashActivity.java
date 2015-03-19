package net.ugona.plus;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class SplashActivity extends FragmentActivity implements DialogInterface.OnDismissListener, DialogListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.splash);
        StyledAuthDialog styledAuthDialog = new StyledAuthDialog(this, "", this);
        styledAuthDialog.show();
        styledAuthDialog.setOnDismissListener(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override
    public void ok() {
        setResult(RESULT_OK);
    }
}
