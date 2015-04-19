package net.ugona.plus;

import android.app.Activity;
import android.content.Intent;
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
    boolean bDemo;
    int demo_count;
    int demo_state;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Font font = new Font("Exo2");
        font.install(this);
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            car_id = getIntent().getStringExtra(Names.ID);
            title = getIntent().getStringExtra(Names.TITLE);
            bDemo = getIntent().getBooleanExtra(Names.ERROR, false);
        }
        if (savedInstanceState != null) {
            car_id = savedInstanceState.getString(Names.ID);
            title = savedInstanceState.getString(Names.TITLE);
            bDemo = savedInstanceState.getBoolean(Names.ERROR);
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
                if (bDemo && (--demo_count <= 0)) {
                    demo_count = 10;
                    CarState state = CarState.get(Alarm.this, car_id);
                    switch (++demo_state) {
                        case 1:
                            state.setShock(1);
                            break;
                        case 2:
                            state.setShock(2);
                            break;
                        case 3:
                            state.setShock(0);
                            state.setMove(true);
                            break;
                        case 4:
                            state.setTilt(true);
                            break;
                        case 5:
                            state.setSos(true);
                            break;
                        case 6:
                            state.setMove(false);
                            state.setSos(false);
                            state.setTilt(false);
                            state.setIn_sensor(true);
                            break;
                        case 7:
                            state.setExt_sensor(true);
                            break;
                        case 8:
                            state.setIn_sensor(false);
                            state.setExt_sensor(false);
                            state.setSos(true);
                        default:
                            demo_state = 0;
                    }
                    carView.update(state);
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
        outState.putBoolean(Names.ERROR, bDemo);
    }

    @Override
    public void onClick(View v) {
        finish();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.stop();
            player.release();
        }
        timer.cancel();
        CarState state = CarState.get(this, car_id);
        state.setSos(false);
        state.setShock(0);
        state.setMove(false);
        state.setTilt(true);
        state.setIn_sensor(false);
        state.setExt_sensor(false);
        Intent intent = new Intent(Names.UPDATED);
        intent.putExtra(Names.ID, car_id);
        sendBroadcast(intent);
        super.onDestroy();
    }
}
