package net.ugona.plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

public class InputText
        extends DialogFragment
        implements DialogInterface.OnClickListener {

    String title;
    String data;
    EditText editText;
    int flags;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.input, null);
        editText = (EditText) v.findViewById(R.id.text);
        if (flags != 0)
            editText.setInputType(flags);
        editText.setText(data);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, null)
                .setView(v);
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.TITLE, title);
        outState.putInt(Names.FLAGS, flags);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    void setArgs(Bundle args) {
        title = args.getString(Names.TITLE);
        data = args.getString(Names.OK);
        flags = args.getInt(Names.FLAGS);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Fragment fragment = getTargetFragment();
        if (getTargetFragment() != null) {
            Intent data = new Intent();
            data.putExtra(Names.OK, editText.getText().toString());
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

}
