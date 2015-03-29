package net.ugona.plus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class MessageDialog
        extends DialogFragment
        implements DialogInterface.OnClickListener {

    String url;
    String message;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        AppConfig config = AppConfig.get(getActivity());
        message = config.getInfo_message();
        if (savedInstanceState != null)
            message = savedInstanceState.getString(Names.MESSAGE);
        config.setInfo_message("");
        if (!config.getInfo_title().equals(""))
            builder.setTitle(config.getInfo_title());
        builder.setMessage(message);
        builder.setNegativeButton(R.string.cancel, null);
        url = config.getInfo_url();
        if (!url.equals(""))
            builder.setPositiveButton(R.string.more, this);
        AppConfig.save(getActivity());
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.MESSAGE, message);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

}
