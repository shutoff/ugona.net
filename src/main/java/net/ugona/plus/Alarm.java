package net.ugona.plus;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("ConstantConditions")
public class Alarm extends Activity {

    TextView tvAlarm;
    ImageView imgPhoto;
    MediaPlayer player;
    VolumeTask volumeTask;
    Timer timer;
    String car_id;
    SharedPreferences preferences;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.alarm);
        tvAlarm = (TextView) findViewById(R.id.text_alarm);
        imgPhoto = (ImageView) findViewById(R.id.photo);

        Button btn = (Button) findViewById(R.id.alarm);
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setBackgroundResource(R.drawable.pressed);
                        break;
                    case MotionEvent.ACTION_UP:
                        showMain();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        v.setBackgroundResource(R.drawable.button);
                        break;
                }
                return true;
            }
        });

        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        car_id = Preferences.getCar(preferences, getIntent().getStringExtra(Names.ID));

        process(getIntent());
    }

    void showMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Names.ID, car_id);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        process(intent);
    }

    @Override
    public void onAttachedToWindow() {
        //make the activity show even the screen is locked.
        Window window = getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop();
    }

    void stop() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (volumeTask != null) {
            volumeTask.stop();
            volumeTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    void cancelAlarm() {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getBaseContext())
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Car Aarm")   //$NON-NLS-1$
                        .setContentText(tvAlarm.getText());   //$NON-NLS-1$

        Intent notificationIntent = new Intent(getBaseContext(), MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int id = preferences.getInt(Names.IDS, 0);
        id++;
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(Names.IDS, id);
        ed.commit();

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id, builder.build());

        finish();
    }

    void process(Intent intent) {
        car_id = Preferences.getCar(preferences, intent.getStringExtra(Names.ID));
        String number = preferences.getString(Names.CAR_PHONE + car_id, "");
        if (number.length() > 0) {

            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

            Cursor contactLookup = null;
            try {
                ContentResolver contentResolver = getContentResolver();
                contactLookup = contentResolver.query(uri, new String[]{BaseColumns._ID,
                        ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

                if (contactLookup != null) {
                    if (contactLookup.getCount() > 0) {
                        contactLookup.moveToNext();
                        String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
                        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contactId));
                        Uri displayPhotoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);
                        AssetFileDescriptor fd =
                                getContentResolver().openAssetFileDescriptor(displayPhotoUri, "r");
                        InputStream inputStream = fd.createInputStream();
                        if (inputStream != null) {
                            Bitmap photo = BitmapFactory.decodeStream(inputStream);
                            imgPhoto.setImageBitmap(photo);
                            inputStream.close();
                        }
                    }
                }
            } catch (Exception ex) {
                // ignore
            } finally {
                if (contactLookup != null) {
                    contactLookup.close();
                }
            }
        }

        String alarm = intent.getStringExtra(Names.ALARM);
        if (alarm != null) {
            String[] cars = preferences.getString(Names.CARS, "").split(",");
            if (cars.length > 1) {
                String name = preferences.getString(Names.CAR_NAME, "");
                if (name.length() == 0) {
                    name = getString(R.string.car);
                    if (car_id.length() > 0)
                        name += " " + car_id;
                }
                alarm = name + "\n" + alarm;
            }
            tvAlarm.setText(alarm);
            String sound = preferences.getString(Names.ALARM, "");
            Uri uri = Uri.parse(sound);
            Ringtone ringtone = RingtoneManager.getRingtone(getBaseContext(), uri);
            if (ringtone == null)
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (timer != null)
                timer.cancel();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cancelAlarm();
                        }
                    });
                }
            };
            timer = new Timer();
            timer.schedule(timerTask, 3 * 60 * 1000);

            try {
                if (player == null) {
                    volumeTask = new VolumeTask(this);
                    player = new MediaPlayer();
                    player.setDataSource(this, uri);
                    player.setAudioStreamType(AudioManager.STREAM_RING);
                    player.setLooping(true);
                    player.prepare();
                    player.start();
                }
            } catch (Exception err) {
                // ignore
            }
        }
    }


}
