package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.afollestad.materialdialogs.AlertDialogWrapper;

public class Alert
        extends DialogFragment
        implements DialogInterface.OnClickListener {

    String title;
    String message;
    String ok;
    boolean sent;
    String id;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        if (ok == null)
            ok = getString(R.string.ok);
        return new AlertDialogWrapper.Builder(getActivity())
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
        outState.putString(Names.ID, id);
        if (ok != null)
            outState.putString(Names.OK, ok);
    }

    void setArgs(Bundle args) {
        title = args.getString(Names.TITLE);
        message = args.getString(Names.MESSAGE);
        ok = args.getString(Names.OK);
        id = args.getString(Names.ID);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        Fragment fragment = getTargetFragment();
        if (fragment != null) {
            Intent intent = new Intent();
            intent.putExtra(Names.ID, id);
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
            sent = true;
        }
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Fragment fragment = getTargetFragment();
        if ((fragment != null) && !sent)
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
    }

}
