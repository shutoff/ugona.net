package net.ugona.plus;

import android.app.Activity;
import android.app.AlarmManager;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
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
    boolean show_main;

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
        if (show_main) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(Names.ID, car_id);
            startActivity(intent);
        }
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
        createNotification(getBaseContext(), tvAlarm.getText().toString(), car_id);
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
                        try {
                            AssetFileDescriptor fd =
                                    getContentResolver().openAssetFileDescriptor(displayPhotoUri, "r");
                            InputStream inputStream = fd.createInputStream();
                            Bitmap photo = BitmapFactory.decodeStream(inputStream);
                            imgPhoto.setImageBitmap(photo);
                            inputStream.close();
                        } catch (Exception ex) {
                            Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                            Cursor cursor = getContentResolver().query(photoUri,
                                    new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
                            if (cursor != null) {
                                try {
                                    if (cursor.moveToFirst()) {
                                        byte[] data = cursor.getBlob(0);
                                        if (data != null) {
                                            Bitmap photo = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                                            imgPhoto.setImageBitmap(photo);
                                        }
                                    }
                                } finally {
                                    cursor.close();
                                }
                            }
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
            show_main = !alarm.equals(getString(R.string.alarm_test));
            String[] cars = preferences.getString(Names.CARS, "").split(",");
            if (cars.length > 1) {
                String name = preferences.getString(Names.CAR_NAME + car_id, "");
                if (name.length() == 0) {
                    name = getString(R.string.car);
                    if (car_id.length() > 0)
                        name += " " + car_id;
                }
                alarm = name + "\n" + alarm;
            }
            tvAlarm.setText(alarm);
            String sound = Preferences.getAlarm(preferences, car_id);
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

    static void createNotification(Context context, String text, String car_id) {
        createNotification(context, text, R.drawable.warning, car_id, null);
    }

    static int createNotification(Context context, String text, int pictId, String car_id, String sound) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int max_id = 0;
        String[] cars = preferences.getString(Names.CARS, "").split(",");
        for (String id : cars) {
            String[] ids = preferences.getString(Names.N_IDS + id, "").split(",");
            for (String n_ids : ids) {
                try {
                    int n = Integer.parseInt(n_ids);
                    if (n > max_id)
                        max_id = n;
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        max_id++;
        SharedPreferences.Editor ed = preferences.edit();
        String s = preferences.getString(Names.N_IDS + car_id, "");
        if (!s.equals(""))
            s += ",";
        s += max_id;
        ed.putString(Names.N_IDS + car_id, s);
        ed.commit();

        Intent iNotification = new Intent(context, FetchService.class);
        iNotification.setAction(FetchService.ACTION_NOTIFICATION);
        iNotification.putExtra(Names.ID, car_id);
        if (sound != null)
            iNotification.putExtra(Names.NOTIFY, sound);
        iNotification.putExtra(Names.TITLE, text);
        iNotification.putExtra(Names.ALARM, pictId);
        iNotification.putExtra(Names.EVENT_ID, max_id);
        Uri data = Uri.withAppendedPath(Uri.parse("http://service/notification/"), car_id);
        iNotification.setData(data);
        PendingIntent pi = PendingIntent.getService(context, 0, iNotification, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pi);
        return max_id;
    }

    static void removeNotification(Context context, String car_id, int n_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String n_ids = preferences.getString(Names.N_IDS + car_id, "");
        String[] ids = n_ids.split(",");
        String res = null;
        for (String id : ids) {
            if (id.equals(n_id + "")) {
                manager.cancel(n_id);
                continue;
            }
            if (res == null) {
                res = id;
                continue;
            }
            res += ",";
            res += id;
        }
        SharedPreferences.Editor ed = preferences.edit();
        if (res == null) {
            ed.remove(Names.N_IDS + car_id);
        } else {
            ed.putString(Names.N_IDS + car_id, res);
        }
        ed.commit();
    }

}
