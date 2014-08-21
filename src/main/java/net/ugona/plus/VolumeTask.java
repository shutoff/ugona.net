package net.ugona.plus;

import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;

import java.util.Timer;
import java.util.TimerTask;

public class VolumeTask extends TimerTask {

    static final int max_count = 15;
    int start_level;
    int max_level;
    int prev_level;
    int count;
    int count_vibro;
    Timer timer;
    AudioManager audioManager;
    Vibrator vibrator;

    VolumeTask(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        start_level = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        prev_level = start_level;
        max_level = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        count = 0;
        count_vibro = 0;
        timer = new Timer();
        timer.schedule(this, 1000, 1000);
    }

    @Override
    public void run() {
        if (count < max_count)
            count++;
        if (count_vibro == 0) {
            vibrator.vibrate(1000);
            count_vibro = 5;
        }
        count_vibro--;
        if (start_level >= 0) {
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != prev_level) {
                start_level = -1;
                return;
            }
            int level = start_level + (max_level - start_level) * count / max_count;
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, level, 0);
            prev_level = level;
        }

    }

    void stop() {
        timer.cancel();
        vibrator.cancel();
        if (start_level >= 0)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, start_level, 0);
    }
}
