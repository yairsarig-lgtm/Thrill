package com.freewill.predict;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Settings storage. Every routine keeps its own settings under keys "r{n}_{name}".
 * Settings saved by older versions (one global set) are used as fallbacks for routines 1 and 2,
 * and for the per-routine vanish settings, so nothing is lost on update.
 */
public final class Prefs {
    private Prefs() {}

    public static final String FILE = "freewill";

    // Legacy global keys (v0.15 and earlier)
    static final String L_MAGICIAN = "magician";
    static final String L_SPECTATOR = "spectator_fallback";
    static final String L_OBJ1 = "obj1", L_OBJ2 = "obj2", L_OBJ3 = "obj3";
    static final String L_TEMPLATE = "template";
    static final String L_AFTER = "after";
    static final String L_VIBRATE = "vibrate";
    static final String L_TRIGGER = "trigger_deg";
    static final String L_PAUSE = "pause_ms";

    /** After the prediction is written: 0 switch back, 1 hide, 2 nothing, 3 switch back and hide. */
    public static final int AFTER_SWITCH = 0, AFTER_HIDE = 1, AFTER_NOTHING = 2, AFTER_SWITCH_HIDE = 3;

    public static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Key of a per-routine setting. */
    public static String k(int routine, String key) {
        return "r" + routine + "_" + key;
    }

    /** Per-routine text, or the default when it is unset or blank. */
    public static String s(SharedPreferences p, int r, String key, String def) {
        String v = p.getString(k(r, key), null);
        return (v == null || v.trim().isEmpty()) ? def : v;
    }

    /** Per-routine text that may legitimately be empty (e.g. a custom prophecy). */
    public static String raw(SharedPreferences p, int r, String key, String def) {
        return p.getString(k(r, key), def);
    }

    public static boolean b(SharedPreferences p, int r, String key, boolean def) {
        return p.getBoolean(k(r, key), def);
    }

    public static int i(SharedPreferences p, int r, String key, int def) {
        return p.getInt(k(r, key), def);
    }

    static String legacy(SharedPreferences p, String key, String def) {
        String v = p.getString(key, null);
        return (v == null || v.trim().isEmpty()) ? def : v;
    }

    // ---- shared player settings (per routine, with legacy fallback) ----

    public static String magician(SharedPreferences p, int r) {
        return s(p, r, "magician", legacy(p, L_MAGICIAN, "Magician"));
    }

    public static String spectatorFallback(SharedPreferences p, int r) {
        return s(p, r, "spectator", legacy(p, L_SPECTATOR, "Spectator"));
    }

    public static String spectator(SharedPreferences p, int r, String typed) {
        return (typed == null || typed.trim().isEmpty()) ? spectatorFallback(p, r) : typed.trim();
    }

    // ---- vanish settings (per routine, with legacy fallback) ----

    public static int after(SharedPreferences p, int r) {
        return p.getInt(k(r, "after"), p.getInt(L_AFTER, AFTER_SWITCH_HIDE));
    }

    public static boolean vibrate(SharedPreferences p, int r) {
        return p.getBoolean(k(r, "vibrate"), p.getBoolean(L_VIBRATE, true));
    }

    public static float trigger(SharedPreferences p, int r) {
        float t = p.getFloat(k(r, "trigger"), p.getFloat(L_TRIGGER, 15f));
        return Math.max(10f, Math.min(40f, t));
    }

    public static long pauseMs(SharedPreferences p, int r) {
        long v = p.getLong(k(r, "pause"), p.getLong(L_PAUSE, 500L));
        return Math.max(200L, Math.min(1000L, v));
    }
}
