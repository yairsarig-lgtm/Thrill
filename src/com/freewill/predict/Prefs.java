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
    public static final String TRIGGER = "trigger";      // tilt trigger, m/s^2 on the long axis

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
        return p.getFloat(TRIGGER, 7.0f);
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
