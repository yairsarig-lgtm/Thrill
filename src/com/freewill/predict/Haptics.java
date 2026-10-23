package com.freewill.predict;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * The "armed" signal: one very short, low-strength tap.
 * Kept faint so a phone lying face down on a table doesn't rattle audibly, and sent with
 * alarm attributes so it still plays when the phone is on silent or Do Not Disturb.
 */
public final class Haptics {
    private Haptics() {}

    public static void armedTap(Context c) {
        Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;
        VibrationEffect effect = v.hasAmplitudeControl()
                ? VibrationEffect.createOneShot(25, 70)                       // soft and short
                : VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        try {
            v.vibrate(effect, attrs);
        } catch (Exception e) {
            try { v.vibrate(effect); } catch (Exception ignored) {}
        }
    }
}
