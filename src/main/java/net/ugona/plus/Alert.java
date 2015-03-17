package net.ugona.plus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class Alert extends DialogFragment implements DialogInterface.OnClickListener {

    String title;
    String message;
    String ok;
    Listener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(ok, this)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        title = args.getString(Names.TITLE);
        message = args.getString(Names.MESSAGE);
        ok = args.getString(Names.OK);
        if (ok == null)
            ok = getString(R.string.ok);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (listener != null)
            listener.ok();
        dismiss();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    static interface Listener {
        void ok();
    }
}
