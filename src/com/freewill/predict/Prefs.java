package com.freewill.predict;

import android.content.Context;
import android.content.SharedPreferences;

/** All performer settings, shared between the setup screen and the keyboard. */
public final class Prefs {
    private Prefs() {}

    public static final String FILE = "freewill";

    public static final String MAGICIAN = "magician";
    public static final String SPECTATOR_FALLBACK = "spectator_fallback";
    public static final String OBJ1 = "obj1";
    public static final String OBJ2 = "obj2";
    public static final String OBJ3 = "obj3";
    public static final String TEMPLATE = "template";
    public static final String AFTER = "after";          // 0 switch back, 1 hide, 2 nothing
    public static final String VIBRATE = "vibrate";
    public static final String DOT = "dot";
    public static final String TRIGGER = "trigger_deg";  // tilt from flat (degrees) that writes the prediction

    // Routine 2: the expert reveal
    public static final String PAUSE_MS = "pause_ms";

    /** Routine 2 outcomes, in the order shown in the app. */
    public static final String[] SIX_NAMES = {
            "FRONT", "FRONT · PAUSE", "BACK", "BACK · PAUSE", "RIGHT", "LEFT"};

    /**
     * Who gets which object in each of the six outcomes, as indexes of obj1..obj3:
     * {spectator's object, magician's object, object in the place}.
     */
    private static final int[][] SIX = {
            {0, 1, 2},   // front:          spectator 1, magician 2, place 3
            {0, 2, 1},   // front + pause:  spectator 1, magician 3, place 2
            {1, 0, 2},   // back:           spectator 2, magician 1, place 3
            {2, 0, 1},   // back + pause:   spectator 3, magician 1, place 2
            {2, 1, 0},   // right:          spectator 3, magician 2, place 1
            {1, 2, 0},   // left:           spectator 2, magician 3, place 1
    };

    public static final String DEF_TEMPLATE =
            "{first} has the {obj1}, {second} is holding the {obj2}, and the {obj3} is in the pocket.";

    public static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String str(SharedPreferences p, String key) {
        String def;
        switch (key) {
            case MAGICIAN: def = "Magician"; break;
            case SPECTATOR_FALLBACK: def = "You"; break;
            case OBJ1: def = "coin"; break;
            case OBJ2: def = "key"; break;
            case OBJ3: def = "ring"; break;
            case TEMPLATE: def = DEF_TEMPLATE; break;
            default: def = "";
        }
        String v = p.getString(key, def);
        return (v == null || v.trim().isEmpty()) ? def : v;
    }

    /** Whoever picks the phone up (the person the screen ends up facing) is named first. */
    public static boolean spectatorFirst(int state) {
        return state == PickupDetector.FACE_SPECTATOR_TOP_UP
                || state == PickupDetector.FACE_SPECTATOR_TOP_DOWN;
    }

    public static float trigger(SharedPreferences p) {
        return p.getFloat(TRIGGER, 15f);
    }

    public static long pauseMs(SharedPreferences p) {
        return Math.max(200L, Math.min(1000L, p.getLong(PAUSE_MS, 500L)));
    }

    /** Which of the six outcomes (0..5) a pickup means in routine 2. */
    public static int sixOutcome(PickupDetector.Pickup pk) {
        switch (pk.edge) {
            case PickupDetector.EDGE_FRONT: return pk.paused ? 1 : 0;
            case PickupDetector.EDGE_BACK: return pk.paused ? 3 : 2;
            case PickupDetector.EDGE_RIGHT: return 4;
            default: return 5;
        }
    }

    /**
     * Expert reveal for outcome 0..5, using the shared prophecy text: {first} is the spectator,
     * {second} the magician, and {obj1}/{obj2}/{obj3} become the spectator's object, the
     * magician's object and the one left in the place.
     */
    public static String buildSix(SharedPreferences p, int outcome, String spectatorName) {
        String spectator = (spectatorName == null || spectatorName.trim().isEmpty())
                ? str(p, SPECTATOR_FALLBACK) : spectatorName.trim();
        String magician = str(p, MAGICIAN);
        String[] objs = {str(p, OBJ1), str(p, OBJ2), str(p, OBJ3)};
        int[] m = SIX[outcome];
        return str(p, TEMPLATE)
                .replace("{first}", spectator)
                .replace("{second}", magician)
                .replace("{obj1}", objs[m[0]])
                .replace("{obj2}", objs[m[1]])
                .replace("{obj3}", objs[m[2]])
                .replace("{spectator}", spectator)
                .replace("{magician}", magician);
    }

    /** The prediction for a routine (1 or 2) and a pickup. */
    public static String build(SharedPreferences p, int routine, PickupDetector.Pickup pk, String name) {
        if (routine == 2) return buildSix(p, sixOutcome(pk), name);
        return buildPrediction(p, pk.facing, name);
    }

    /** Builds the final prediction text for a pickup state. */
    public static String buildPrediction(SharedPreferences p, int state, String spectatorName) {
        String spectator = (spectatorName == null || spectatorName.trim().isEmpty())
                ? str(p, SPECTATOR_FALLBACK) : spectatorName.trim();
        String magician = str(p, MAGICIAN);
        boolean specFirst = spectatorFirst(state);
        String first = specFirst ? spectator : magician;
        String second = specFirst ? magician : spectator;
        return str(p, TEMPLATE)
                .replace("{first}", first)
                .replace("{second}", second)
                .replace("{obj1}", str(p, OBJ1))
                .replace("{obj2}", str(p, OBJ2))
                .replace("{obj3}", str(p, OBJ3))
                .replace("{spectator}", spectator)
                .replace("{magician}", magician);
    }
}
