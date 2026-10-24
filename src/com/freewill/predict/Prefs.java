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

    // Tags used in the prophecy text
    public static final String T_SPECTATOR = "{spectator}";
    public static final String T_MAGICIAN = "{magician}";
    public static final String T_S_OBJ = "{spectator's object}";
    public static final String T_M_OBJ = "{magician's object}";
    public static final String T_REST = "{remaining object}";

    public static final String[] TAGS = {T_SPECTATOR, T_MAGICIAN, T_S_OBJ, T_M_OBJ, T_REST};
    public static final String[] TAG_HELP = {
            "the name you type on the keyboard",
            "your name, from The Players",
            "the object the spectator ends up holding",
            "the object you end up holding",
            "the object left over (in the normal reveal, always Object 3)",
    };

    public static final String DEF_TEMPLATE = T_SPECTATOR + " is holding the " + T_S_OBJ + ", "
            + T_MAGICIAN + " is holding the " + T_M_OBJ + ", and the " + T_REST + " is in the glass.";

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
        if (TEMPLATE.equals(key) && v != null && v.contains("{obj")) {
            v = migrate(v);
            p.edit().putString(TEMPLATE, v).apply();
        }
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
     * Expert reveal for outcome 0..5, using the shared prophecy text.
     */
    public static String buildSix(SharedPreferences p, int outcome, String typedName) {
        String[] objs = {str(p, OBJ1), str(p, OBJ2), str(p, OBJ3)};
        int[] m = SIX[outcome];
        return fill(p, spectatorName(p, typedName), str(p, MAGICIAN), objs[m[0]], objs[m[1]], objs[m[2]]);
    }

    /** The prediction for a routine (1 or 2) and a pickup. */
    public static String build(SharedPreferences p, int routine, PickupDetector.Pickup pk, String name) {
        if (routine == 2) return buildSix(p, sixOutcome(pk), name);
        return buildPrediction(p, pk.facing, name);
    }

    /** Converts prophecy text written with the old {first}/{obj1} tags. */
    static String migrate(String v) {
        return v.replace("{first}", T_SPECTATOR)
                .replace("{second}", T_MAGICIAN)
                .replace("{obj1}", T_S_OBJ)
                .replace("{obj2}", T_M_OBJ)
                .replace("{obj3}", T_REST);
    }

    /** Fills the prophecy text. Accepts straight or curly apostrophes in the tags. */
    static String fill(SharedPreferences p, String spectator, String magician,
                       String sObj, String mObj, String rest) {
        return str(p, TEMPLATE).replace('\u2019', '\'')
                .replace(T_SPECTATOR, spectator)
                .replace(T_MAGICIAN, magician)
                .replace(T_S_OBJ, sObj)
                .replace(T_M_OBJ, mObj)
                .replace(T_REST, rest);
    }

    private static String spectatorName(SharedPreferences p, String typed) {
        return (typed == null || typed.trim().isEmpty()) ? str(p, SPECTATOR_FALLBACK) : typed.trim();
    }

    /**
     * Normal reveal: whoever picks the phone up holds Object 1, the other person Object 2,
     * and Object 3 (the forced one) is left over.
     */
    public static String buildPrediction(SharedPreferences p, int state, String typedName) {
        boolean specPicked = spectatorFirst(state);
        String o1 = str(p, OBJ1), o2 = str(p, OBJ2);
        return fill(p, spectatorName(p, typedName), str(p, MAGICIAN),
                specPicked ? o1 : o2, specPicked ? o2 : o1, str(p, OBJ3));
    }
}
