package net.ugona.plus;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

public class RingTonePicker
        extends DialogFragment
        implements DialogInterface.OnClickListener,
        RadioGroup.OnCheckedChangeListener,
        MediaPlayer.OnCompletionListener,
        AdapterView.OnItemSelectedListener {

    int type;
    int vibro;
    String title;
    String sound;
    View btnOk;
    RadioGroup group;
    MediaPlayer player;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            setArgs(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.ringtone_picker, null);

        final String[] vibro_items = getResources().getStringArray(R.array.vibro);
        Spinner vVibro = (Spinner) v.findViewById(R.id.vibro);
        vVibro.setAdapter(new ArrayAdapter(vVibro) {
            @Override
            public int getCount() {
                return vibro_items.length;
            }

            @Override
            public Object getItem(int position) {
                return vibro_items[position];
            }
        });
        vVibro.setSelection(vibro);
        vVibro.setOnItemSelectedListener(this);

        ScrollView scrollView = (ScrollView) v.findViewById(R.id.scroll);
        group = (RadioGroup) scrollView.getChildAt(0);
        RingtoneManager manager = new RingtoneManager(getActivity());
        manager.setType(type);
        int n = 100;

        Uri defaultUri = RingtoneManager.getDefaultUri(type);
        Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), defaultUri);
        RadioButton rb = new RadioButton(getActivity());
        rb.setText(ringtone.getTitle(getActivity()));
        rb.setTag(defaultUri.toString());
        rb.setId(n++);
        if (defaultUri.toString().equals(sound))
            rb.setChecked(true);
        group.addView(rb);
        group.setOnCheckedChangeListener(this);

        Cursor cursor = manager.getCursor();
        int position = 0;
        while (cursor.moveToNext()) {
            String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            String uri = manager.getRingtoneUri(position++).toString();
            rb = new RadioButton(getActivity());
            rb.setText(title);
            rb.setTag(uri);
            rb.setId(n++);
            if (uri.equals(sound))
                rb.setChecked(true);
            group.addView(rb);
        }
        return new AlertDialogWrapper.Builder(getActivity())
                .setTitle(title)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, this)
                .setView(v)
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.TITLE, title);
        outState.putInt(Names.TYPE, type);
        outState.putString(Names.SOUND, sound);
        outState.putInt(Names.VIBRO, vibro);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setArgs(args);
    }

    @Override
    public void onStart() {
        super.onStart();
        MaterialDialog dialog = (MaterialDialog) getDialog();
        btnOk = dialog.getActionButton(DialogAction.POSITIVE);
        btnOk.setEnabled(group.getCheckedRadioButtonId() > 0);
    }

    void setArgs(Bundle args) {
        title = args.getString(Names.TITLE);
        type = args.getInt(Names.TYPE);
        sound = args.getString(Names.SOUND);
        vibro = args.getInt(Names.VIBRO);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        View v = group.findViewById(group.getCheckedRadioButtonId());
        if (v == null)
            return;
        if (btnOk != null)
            btnOk.setEnabled(true);
        sound = v.getTag().toString();
        Fragment fragment = getTargetFragment();
        if (fragment != null) {
            Intent data = new Intent();
            data.putExtra(Names.SOUND, v.getTag().toString());
            data.putExtra(Names.VIBRO, vibro);
            fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        View v = group.findViewById(checkedId);
        if (v == null)
            return;
        if (btnOk != null)
            btnOk.setEnabled(true);
        sound = v.getTag().toString();
        if (player != null) {
            try {
                if (player.isPlaying())
                    player.stop();
                player.release();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        try {
            player = new MediaPlayer();
            player.setDataSource(getActivity(), Uri.parse(sound));
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.prepare();
            player.setLooping(false);
            player.start();
            player.setOnCompletionListener(this);
        } catch (Exception ex) {
            player = null;
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (player != null) {
            try {
                if (player.isPlaying())
                    player.stop();
                player.release();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        super.onDismiss(dialog);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (player != null) {
            try {
                if (player.isPlaying())
                    player.stop();
                player.release();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            player = null;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (vibro == position)
            return;
        vibro = position;
        Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        switch (vibro) {
            case 0:
                v.vibrate(1000);
                break;
            case 2:
                v.vibrate(Notification.SHORT_PATTERN, -1);
                break;
            case 3:
                v.vibrate(Notification.LONG_PATTERN, -1);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
