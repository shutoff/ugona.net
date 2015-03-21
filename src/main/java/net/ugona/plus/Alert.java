package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

public class Alert extends DialogFragment implements DialogInterface.OnClickListener {

    String title;
    String message;
    String ok;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
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
        setArgs(args);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.TITLE, title);
        outState.putString(Names.MESSAGE, message);
        outState.putString(Names.OK, ok);
    }

    void setArgs(Bundle args) {
        title = args.getString(Names.TITLE);
        message = args.getString(Names.MESSAGE);
        ok = args.getString(Names.OK);
        if (ok == null)
            ok = getString(R.string.ok);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Fragment fragment = getTargetFragment();
        if (fragment != null)
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
        dismiss();
    }

}
