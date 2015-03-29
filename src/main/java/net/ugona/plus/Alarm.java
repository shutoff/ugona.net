package net.ugona.plus;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class Alarm
        extends Activity
        implements View.OnClickListener {

    String car_id;
    String title;
    CarView carView;
    CountDownTimer timer;
    CountDownTimer volumeTimer;
    AudioManager audioManager;
    MediaPlayer player;
    int start_level;
    int max_level;
    int prev_level;
    int count;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Font font = new Font("Exo2");
        font.install(this);
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            car_id = getIntent().getStringExtra(Names.ID);
            title = getIntent().getStringExtra(Names.TITLE);
        }
        if (savedInstanceState != null) {
            car_id = savedInstanceState.getString(Names.ID);
            title = savedInstanceState.getString(Names.TITLE);
        }
        setContentView(R.layout.alarm);
        TextView tvTitle = (TextView) findViewById(R.id.title);
        tvTitle.setText(title);
        findViewById(R.id.close).setOnClickListener(this);
        carView = (CarView) findViewById(R.id.car);
        CarState state = CarState.get(this, car_id);
        carView.update(state);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        start_level = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        max_level = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        prev_level = start_level;
        count = 0;
        timer = new CountDownTimer(180000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                count++;
                if (count > 60)
                    count = 60;
                int new_level = start_level + (int) ((count / 60.) * (max_level - start_level));
                if (new_level != prev_level) {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, new_level, 0);
                    prev_level = new_level;
                }
            }

            @Override
            public void onFinish() {
                Notification.showAlarm(Alarm.this, car_id, title);
                finish();
            }
        };
        timer.start();
        CarConfig carConfig = CarConfig.get(this, car_id);
        String sound = carConfig.getAlarmSound();
        Uri uri = Uri.parse(sound);
        if (sound.equals(""))
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        Ringtone ringtone = RingtoneManager.getRingtone(getBaseContext(), uri);
        if (ringtone == null)
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        try {
            if (player == null) {
                player = new MediaPlayer();
                player.setDataSource(this, uri);
                player.setAudioStreamType(AudioManager.STREAM_ALARM);
                player.setLooping(true);
                player.prepare();
                player.start();
            }
        } catch (Exception err) {
            // ignore
        }
    }

    @Override
    public void onAttachedToWindow() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Names.ID, car_id);
        outState.putString(Names.TITLE, title);
    }

    @Override
    public void onClick(View v) {
        finish();
    }

    @Override
    protected void onDestroy() {
        if (player != null)
            player.stop();
        timer.cancel();
        super.onDestroy();
        CarState state = CarState.get(this, car_id);
        state.setSos(false);
        state.setShock(0);
        state.setMove(false);
    }
}
