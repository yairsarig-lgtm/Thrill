package com.freewill.predict;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The routine catalogue and everything that turns a pickup into a prediction.
 *
 * Pickups are reduced to one of eight "slots": the edge that rises first (as seen from the
 * magician's seat, phone face down with its bottom toward the magician) and whether there was
 * a pause. Each routine maps slots to its outcomes. Routine 1 is the exception: it uses who the
 * screen ends up facing.
 */
public final class Routines {
    private Routines() {}

    public static final int NORMAL = 1, EXPERT = 2, EIGHT = 3, DIE = 4, ESP = 5, COIN = 6;
    public static final int FIRST_CUSTOM = 7;
    public static final int MAX_OUTCOMES = 8;

    // ---------------------------------------------------------------- slots

    public static final int FRONT = 0, FRONT_P = 1, BACK = 2, BACK_P = 3,
            LEFT = 4, LEFT_P = 5, RIGHT = 6, RIGHT_P = 7;

    public static final String[] SLOT_NAMES = {
            "קדמי", "קדמי · עצירה", "אחורי", "אחורי · עצירה",
            "שמאל", "שמאל · עצירה", "ימין", "ימין · עצירה"};

    /** The order in which Build It Yourself hands out pickups. */
    public static final int[] SLOT_ORDER = {FRONT, BACK, LEFT, RIGHT, FRONT_P, BACK_P, LEFT_P, RIGHT_P};

    public static int slot(PickupDetector.Pickup pk) {
        int base;
        switch (pk.edge) {
            case PickupDetector.EDGE_FRONT: base = FRONT; break;
            case PickupDetector.EDGE_BACK: base = BACK; break;
            case PickupDetector.EDGE_LEFT: base = LEFT; break;
            default: base = RIGHT; break;
        }
        return base + (pk.paused ? 1 : 0);
    }

    // ---------------------------------------------------------------- catalogue

    private static final String[] ROMAN_N = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
    private static final int[] ROMAN_V = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

    public static String roman(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROMAN_V.length; i++) {
            while (n >= ROMAN_V[i]) { sb.append(ROMAN_N[i]); n -= ROMAN_V[i]; }
        }
        return sb.toString();
    }

    public static boolean isCustom(int r) { return r >= FIRST_CUSTOM; }

    public static List<Integer> customIds(SharedPreferences p) {
        List<Integer> ids = new ArrayList<>();
        String csv = p.getString("custom_ids", "");
        for (String s : csv.split(",")) {
            try { if (!s.trim().isEmpty()) ids.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
        }
        Collections.sort(ids);
        return ids;
    }

    private static void saveCustomIds(SharedPreferences p, List<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (int id : ids) { if (sb.length() > 0) sb.append(','); sb.append(id); }
        p.edit().putString("custom_ids", sb.toString()).apply();
    }

    public static boolean exists(SharedPreferences p, int r) {
        return (r >= NORMAL && r <= COIN) || customIds(p).contains(r);
    }

    public static List<Integer> all(SharedPreferences p) {
        List<Integer> l = new ArrayList<>();
        for (int r = NORMAL; r <= COIN; r++) l.add(r);
        l.addAll(customIds(p));
        return l;
    }

    public static String name(SharedPreferences p, int r) {
        switch (r) {
            case NORMAL: return "החשיפה הרגילה";
            case EXPERT: return "חשיפת המומחה";
            case EIGHT: return "השמונה";
            case DIE: return "הטלת קובייה";
            case ESP: return "סמלי ESP";
            case COIN: return "הטלות מטבע";
            default: return Prefs.s(p, r, "name", "רוטינה חדשה");
        }
    }

    /** Short name for the rehearsal picker. */
    public static String shortName(SharedPreferences p, int r) {
        switch (r) {
            case NORMAL: return "רגילה";
            case EXPERT: return "מומחה";
            case EIGHT: return "שמונה";
            case DIE: return "קובייה";
            case ESP: return "ESP";
            case COIN: return "מטבעות";
            default:
                String n = Prefs.s(p, r, "name", "אישית");
                return n.length() > 9 ? n.substring(0, 8) + "…" : n;
        }
    }

    public static String summary(SharedPreferences p, int r) {
        switch (r) {
            case NORMAL: return "2 תוצאות · חפץ אחד מאולץ";
            case EXPERT: return "6 תוצאות · בלי פורס";
            case EIGHT: return eightForce(p) ? "8 חפצים · פורס מנטלי" : "8 חפצים · נבואה";
            case DIE: return "6 תוצאות · קובייה אמיתית";
            case ESP: return "5 סמלים · לפי מספר הקווים";
            case COIN: { int n = flips(p); return (n == 1 ? "הטלה אחת · " : n + " הטלות · ") + (1 << n) + " תוצאות"; }
            default: return customCount(p, r) + " תוצאות · בנויה על ידיכם";
        }
    }

    public static String explanation(SharedPreferences p, int r) {
        switch (r) {
            case NORMAL: return "שלושה חפצים, אחד מהם מאולץ. מי שמרים את הטלפון קובע מי מחזיק באיזה משני האחרים.";
            case EXPERT: return "שלושה חפצים, שלושה מקומות, בלי פורס. איך שהבחירות לא ייפלו, הדרך שבה מרימים את הטלפון כותבת את התחזית היחידה שמתאימה.";
            case EIGHT: return "שמונה חפצים על השולחן ובחירה חופשית של אחד מהם. כל צד של הטלפון, עם עצירה או בלי, מציין חפץ אחר.";
            case DIE: return "הצופה מטיל קובייה אמיתית. מה שיוצא למעלה כבר כתוב בטלפון שלכם.";
            case ESP: return "חמשת סמלי זנר הקלאסיים. הצופה בוחר או חושב על אחד, והטלפון שלכם כבר מחזיק את התשובה.";
            case COIN: return "הצופה מטיל מטבע פעם, פעמיים או שלוש. הטלפון שלכם חזה כל הטלה, לפי הסדר.";
            default: return "רוטינה משלכם: שתיים עד שמונה תוצאות, כל אחת בהרמה שתבחרו.";
        }
    }

    /** Creates a new Build It Yourself routine with the next free number. */
    public static int createCustom(SharedPreferences p) {
        List<Integer> ids = customIds(p);
        int id = FIRST_CUSTOM;
        for (int x : ids) if (x >= id) id = x + 1;
        ids.add(id);
        saveCustomIds(p, ids);
        SharedPreferences.Editor e = p.edit();
        e.putInt(Prefs.k(id, "count"), 2);
        e.putInt(Prefs.k(id, "slot0"), SLOT_ORDER[0]);
        e.putInt(Prefs.k(id, "slot1"), SLOT_ORDER[1]);
        e.apply();
        return id;
    }

    /** Where each custom routine would move if the numbers were closed up after deleting r. */
    public static List<int[]> renumberPlan(SharedPreferences p, int deleting) {
        List<int[]> plan = new ArrayList<>();
        int next = FIRST_CUSTOM;
        for (int id : customIds(p)) {
            if (id == deleting) continue;
            if (id != next) plan.add(new int[]{id, next});
            next++;
        }
        return plan;
    }

    public static void deleteCustom(SharedPreferences p, int r, boolean renumber) {
        List<int[]> plan = renumber ? renumberPlan(p, r) : new ArrayList<int[]>();
        clearKeys(p, r);
        List<Integer> ids = customIds(p);
        ids.remove(Integer.valueOf(r));
        for (int[] mv : plan) {          // ascending, so every target is already free
            moveKeys(p, mv[0], mv[1]);
            ids.remove(Integer.valueOf(mv[0]));
            ids.add(mv[1]);
        }
        saveCustomIds(p, ids);
    }

    private static void clearKeys(SharedPreferences p, int r) {
        String prefix = "r" + r + "_";
        SharedPreferences.Editor e = p.edit();
        for (String key : p.getAll().keySet()) if (key.startsWith(prefix)) e.remove(key);
        e.commit();
    }

    private static void moveKeys(SharedPreferences p, int from, int to) {
        String fp = "r" + from + "_", tp = "r" + to + "_";
        SharedPreferences.Editor e = p.edit();
        for (Map.Entry<String, ?> en : p.getAll().entrySet()) {
            String key = en.getKey();
            if (!key.startsWith(fp)) continue;
            String nk = tp + key.substring(fp.length());
            Object v = en.getValue();
            if (v instanceof String) e.putString(nk, (String) v);
            else if (v instanceof Integer) e.putInt(nk, (Integer) v);
            else if (v instanceof Boolean) e.putBoolean(nk, (Boolean) v);
            else if (v instanceof Float) e.putFloat(nk, (Float) v);
            else if (v instanceof Long) e.putLong(nk, (Long) v);
            e.remove(key);
        }
        e.commit();
    }

    // ---------------------------------------------------------------- typed name

    private static final Pattern TRAILING_NUMBER = Pattern.compile("^(.*?)\\s*(\\d+)\\s*$", Pattern.DOTALL);

    /** "Noa" or "Noa1" = routine 1; "Noa11" = routine 11. Unknown numbers fall back to 1. */
    public static int routineFor(SharedPreferences p, String typed) {
        Matcher m = TRAILING_NUMBER.matcher(typed);
        if (!m.matches()) return NORMAL;
        try {
            int r = Integer.parseInt(m.group(2));
            return exists(p, r) ? r : NORMAL;
        } catch (NumberFormatException e) {
            return NORMAL;
        }
    }

    /** The typed name without the routine number. */
    public static String nameFor(String typed) {
        Matcher m = TRAILING_NUMBER.matcher(typed);
        return m.matches() ? m.group(1) : typed;
    }

    // ---------------------------------------------------------------- tags

    public static final String T_SPECTATOR = "{צופה}";
    public static final String T_MAGICIAN = "{קוסם}";
    public static final String T_S_OBJ = "{חפץ הצופה}";
    public static final String T_M_OBJ = "{חפץ הקוסם}";
    public static final String T_REST = "{חפץ נותר}";
    public static final String T_ITEM = "{חפץ}";
    public static final String T_POINTED = "{חפץ מוצבע}";
    public static final String T_NUMBER = "{מספר}";
    public static final String T_SYMBOL = "{סמל}";
    public static final String T_OUTCOME = "{תוצאה}";

    public static String flipTag(int k) { return "{הטלה " + k + "}"; }

    public static String[] tags(SharedPreferences p, int r) {
        switch (r) {
            case NORMAL:
            case EXPERT: return new String[]{T_SPECTATOR, T_MAGICIAN, T_S_OBJ, T_M_OBJ, T_REST};
            case EIGHT: return eightForce(p)
                    ? new String[]{T_SPECTATOR, T_MAGICIAN, T_POINTED, T_ITEM}
                    : new String[]{T_SPECTATOR, T_MAGICIAN, T_ITEM};
            case DIE: return new String[]{T_SPECTATOR, T_MAGICIAN, T_NUMBER};
            case ESP: return new String[]{T_SPECTATOR, T_MAGICIAN, T_SYMBOL};
            case COIN: {
                int n = flips(p);
                String[] t = new String[2 + n];
                t[0] = T_SPECTATOR; t[1] = T_MAGICIAN;
                for (int k = 1; k <= n; k++) t[1 + k] = flipTag(k);
                return t;
            }
            default: return new String[]{T_SPECTATOR, T_MAGICIAN, T_OUTCOME};
        }
    }

    public static String[] tagHelp(SharedPreferences p, int r) {
        switch (r) {
            case NORMAL: return new String[]{"השם שמקלידים", "השם שלכם, למעלה",
                    "מה שהצופה מחזיק", "מה שאתם מחזיקים", "זה שנשאר (תמיד חפץ 3)"};
            case EXPERT: return new String[]{"השם שמקלידים", "השם שלכם, למעלה",
                    "מה שהצופה מחזיק", "מה שאתם מחזיקים", "זה שנשאר"};
            case EIGHT: return eightForce(p)
                    ? new String[]{"השם שמקלידים", "השם שלכם, למעלה", "מה שאתם מצביעים עליו, נקבע למעלה", "החפץ שההרמה מצביעה עליו"}
                    : new String[]{"השם שמקלידים", "השם שלכם, למעלה", "החפץ שההרמה מצביעה עליו"};
            case DIE: return new String[]{"השם שמקלידים", "השם שלכם, למעלה", "המספר שיצא, 1 עד 6"};
            case ESP: return new String[]{"השם שמקלידים", "השם שלכם, למעלה", "שם הסמל שנבחר"};
            case COIN: {
                int n = flips(p);
                String[] h = new String[2 + n];
                h[0] = "השם שמקלידים"; h[1] = "השם שלכם, למעלה";
                for (int k = 1; k <= n; k++) h[1 + k] = "הטלה " + k + ": המילה שלכם לעץ או לפלי";
                return h;
            }
            default: return new String[]{"השם שמקלידים", "השם שלכם, למעלה", "התוצאה שההרמה מצביעה עליה"};
        }
    }

    // ---------------------------------------------------------------- templates

    static final String DEF_OBJECTS = "ל" + T_SPECTATOR + " יש את ה" + T_S_OBJ + ", ל"
            + T_MAGICIAN + " יש את ה" + T_M_OBJ + ", וה" + T_REST + " בכוס.";
    static final String DEF_EIGHT_PROPHECY = "מכל מה שעל השולחן, הבחירה של " + T_SPECTATOR + " תהיה ה" + T_ITEM + ".";
    static final String DEF_EIGHT_FORCE = "למרות ההצבעה של " + T_MAGICIAN + " על ה" + T_POINTED + ", "
            + "הבחירה של " + T_SPECTATOR + " נמשכה בדיוק אל ה" + T_ITEM + ".";
    static final String DEF_DIE = "הקובייה של " + T_SPECTATOR + " תיפול על " + T_NUMBER + ".";
    static final String DEF_ESP = "הסמל של " + T_SPECTATOR + " הוא ה" + T_SYMBOL + ".";
    static final String DEF_COIN_OPEN = "זה היה ידוע ל" + T_MAGICIAN + " עוד לפני ההטלות של " + T_SPECTATOR + ":";
    static final String[] DEF_COIN_LINES = {
            "ההטלה הראשונה תהיה " + flipTag(1) + ".",
            "השנייה תהיה " + flipTag(2) + ".",
            "והאחרונה תהיה " + flipTag(3) + "."};

    public static final String[] EIGHT_FLAVOR = {
            "המבט של " + T_MAGICIAN + " היה על ה" + T_POINTED + ", אבל היד של " + T_SPECTATOR + " הלכה אל ה" + T_ITEM + ".",
            "בכל פעם שהיד של " + T_MAGICIAN + " נגעה ב" + T_POINTED + ", הבחירה של " + T_SPECTATOR + " התקרבה אל ה" + T_ITEM + ".",
    };

    /** The key that holds a routine's prophecy text. */
    public static String templateKey(SharedPreferences p, int r) {
        if (r == EIGHT) return eightForce(p) ? "tpl_force" : "tpl_prophecy";
        return "template";
    }

    public static String template(SharedPreferences p, int r) {
        switch (r) {
            case NORMAL:
            case EXPERT: return Prefs.s(p, r, "template", legacyTemplate(p));
            case EIGHT: return eightForce(p)
                    ? Prefs.s(p, r, "tpl_force", DEF_EIGHT_FORCE)
                    : Prefs.s(p, r, "tpl_prophecy", DEF_EIGHT_PROPHECY);
            case DIE: return Prefs.s(p, r, "template", DEF_DIE);
            case ESP: return Prefs.s(p, r, "template", DEF_ESP);
            case COIN: return coinOpening(p);
            default: return Prefs.raw(p, r, "template", "");
        }
    }

    private static String legacyTemplate(SharedPreferences p) {
        String v = Prefs.legacy(p, Prefs.L_TEMPLATE, DEF_OBJECTS);
        return v.replace("{first}", T_SPECTATOR).replace("{second}", T_MAGICIAN)
                .replace("{obj1}", T_S_OBJ).replace("{obj2}", T_M_OBJ).replace("{obj3}", T_REST);
    }

    // ---------------------------------------------------------------- routine data

    private static final String[] OBJ_DEF = {"טבעת", "טוש", "מטבע"};
    private static final String[] OBJ_LEGACY = {Prefs.L_OBJ1, Prefs.L_OBJ2, Prefs.L_OBJ3};

    /** Object i (0..2) of routine 1 or 2. */
    public static String object(SharedPreferences p, int r, int i) {
        return Prefs.s(p, r, "obj" + (i + 1), Prefs.legacy(p, OBJ_LEGACY[i], OBJ_DEF[i]));
    }

    private static final String[] ITEM_DEF = {"מפתח", "שעון", "טבעת", "מטבע", "עט", "מצית", "קלף", "משקפיים"};

    public static String item(SharedPreferences p, int i) {
        return Prefs.s(p, EIGHT, "item" + (i + 1), ITEM_DEF[i]);
    }

    public static boolean eightForce(SharedPreferences p) {
        return Prefs.i(p, EIGHT, "pres", 1) == 1;
    }

    public static int pointed(SharedPreferences p) {
        return Math.max(0, Math.min(7, Prefs.i(p, EIGHT, "pointed", 1)));
    }

    public static boolean dieWords(SharedPreferences p) {
        return Prefs.b(p, DIE, "words", false);
    }

    private static final String[] DIE_WORDS = {"אחת", "שתיים", "שלוש", "ארבע", "חמש", "שש"};

    public static String dieNumber(SharedPreferences p, int i) {
        return dieWords(p) ? DIE_WORDS[i] : String.valueOf(i + 1);
    }

    private static final String[] SYMBOL_DEF = {"עיגול", "צלב", "גלים", "ריבוע", "כוכב"};

    public static String symbol(SharedPreferences p, int i) {
        return Prefs.s(p, ESP, "sym" + (i + 1), SYMBOL_DEF[i]);
    }

    public static int flips(SharedPreferences p) {
        return Math.max(1, Math.min(3, Prefs.i(p, COIN, "flips", 3)));
    }

    public static String heads(SharedPreferences p) { return Prefs.s(p, COIN, "heads", "עץ"); }
    public static String tails(SharedPreferences p) { return Prefs.s(p, COIN, "tails", "פלי"); }

    public static String coinOpening(SharedPreferences p) {
        return Prefs.raw(p, COIN, "open", DEF_COIN_OPEN);
    }

    public static String coinLine(SharedPreferences p, int k) {
        return Prefs.s(p, COIN, "line" + k, DEF_COIN_LINES[k - 1]);
    }

    public static int customCount(SharedPreferences p, int r) {
        return Math.max(2, Math.min(MAX_OUTCOMES, Prefs.i(p, r, "count", 2)));
    }

    public static String customOutcome(SharedPreferences p, int r, int i) {
        return Prefs.raw(p, r, "out" + i, "");
    }

    public static int customSlot(SharedPreferences p, int r, int i) {
        return Prefs.i(p, r, "slot" + i, SLOT_ORDER[i]);
    }

    // ---------------------------------------------------------------- slot maps

    private static final int[][] SIX = {   // {spectator's, magician's, remaining}
            {0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {2, 0, 1}, {2, 1, 0}, {1, 2, 0}};

    /** For each of the eight slots, the outcome it writes, or -1 if unused. Null for routine 1. */
    public static int[] slotMap(SharedPreferences p, int r) {
        int[] m = {-1, -1, -1, -1, -1, -1, -1, -1};
        switch (r) {
            case NORMAL: return null;
            case EXPERT:
                m[FRONT] = 0; m[FRONT_P] = 1; m[BACK] = 2; m[BACK_P] = 3; m[RIGHT] = 4; m[LEFT] = 5;
                return m;
            case EIGHT:
                for (int s = 0; s < 8; s++) m[s] = s;
                return m;
            case DIE:
                m[FRONT] = 0; m[FRONT_P] = 1; m[BACK] = 2; m[BACK_P] = 3; m[LEFT] = 4; m[RIGHT] = 5;
                return m;
            case ESP:   // counted by lines, clockwise from the front
                m[FRONT] = 0; m[RIGHT] = 1; m[BACK] = 2; m[LEFT] = 3; m[FRONT_P] = 4;
                return m;
            case COIN: {
                int n = flips(p);
                for (int seq = 0; seq < (1 << n); seq++) m[coinSlot(n, seq)] = seq;
                return m;
            }
            default: {
                int c = customCount(p, r);
                for (int i = 0; i < c; i++) {
                    int s = customSlot(p, r, i);
                    if (s >= 0 && s < 8) m[s] = i;
                }
                return m;
            }
        }
    }

    /**
     * Coin flips. 1st flip: heads = front or back, tails = a side. 2nd flip: heads = front / left,
     * tails = back / right. 3rd flip: heads = straight lift, tails = pause.
     * With one flip: heads = front, tails = back.
     */
    static int coinSlot(int n, int seq) {
        if (n == 1) return (seq & 1) == 0 ? FRONT : BACK;
        int f1 = (seq >> (n - 1)) & 1, f2 = (seq >> (n - 2)) & 1;
        int base = f1 == 0 ? (f2 == 0 ? FRONT : BACK) : (f2 == 0 ? LEFT : RIGHT);
        return n == 3 ? base + (seq & 1) : base;
    }

    /** True if flip k (1-based) of the sequence is tails. */
    static boolean coinTails(int n, int seq, int k) {
        return ((seq >> (n - k)) & 1) == 1;
    }

    /** The outcome a slot writes, falling back to the edge's plain lift, then to the first outcome. */
    public static int outcomeFor(SharedPreferences p, int r, int slot) {
        int[] m = slotMap(p, r);
        if (m == null) return 0;
        if (m[slot] >= 0) return m[slot];
        if ((slot & 1) == 1 && m[slot - 1] >= 0) return m[slot - 1];
        for (int s : SLOT_ORDER) if (m[s] >= 0) return m[s];
        return 0;
    }

    /** Short text for a slot on the table diagram, or null if the slot is unused. */
    public static String cellLabel(SharedPreferences p, int r, int slot) {
        int[] m = slotMap(p, r);
        if (m == null || m[slot] < 0) return null;
        int o = m[slot];
        switch (r) {
            case EXPERT: return object(p, r, SIX[o][0]) + " · " + object(p, r, SIX[o][1]);
            case EIGHT: return (o + 1) + " · " + item(p, o);
            case DIE: return dieNumber(p, o);
            case ESP: return (o + 1) + " · " + symbol(p, o);
            case COIN: {
                int n = flips(p);
                StringBuilder sb = new StringBuilder();
                for (int k = 1; k <= n; k++) {
                    if (k > 1) sb.append(' ');
                    String w = coinTails(n, o, k) ? tails(p) : heads(p);
                    sb.append(w.isEmpty() ? "?" : w.substring(0, 1).toUpperCase());
                }
                return sb.toString();
            }
            default: {
                String v = customOutcome(p, r, o);
                return (o + 1) + " · " + (v.trim().isEmpty() ? "—" : v);
            }
        }
    }

    /** Number of distinct outcomes in a routine (for previews). */
    public static int outcomeCount(SharedPreferences p, int r) {
        switch (r) {
            case NORMAL: return 2;
            case EXPERT: return 6;
            case EIGHT: return 8;
            case DIE: return 6;
            case ESP: return 5;
            case COIN: return 1 << flips(p);
            default: return customCount(p, r);
        }
    }

    // ---------------------------------------------------------------- building predictions

    private static String fill(String tpl, String spectator, String magician) {
        return tpl.replace('’', '\'')
                .replace(T_SPECTATOR, spectator)
                .replace(T_MAGICIAN, magician);
    }

    /**
     * The prediction for one outcome of a routine. For routine 1, outcome 0 = the spectator
     * picks the phone up, 1 = the magician does.
     */
    public static String sentence(SharedPreferences p, int r, int o, String typed) {
        String spectator = Prefs.spectator(p, r, typed);
        String magician = Prefs.magician(p, r);
        switch (r) {
            case NORMAL: {
                boolean spec = o == 0;
                String o1 = object(p, r, 0), o2 = object(p, r, 1);
                return fill(template(p, r).replace('’', '\'')
                        .replace(T_S_OBJ, spec ? o1 : o2)
                        .replace(T_M_OBJ, spec ? o2 : o1)
                        .replace(T_REST, object(p, r, 2)), spectator, magician);
            }
            case EXPERT: {
                int[] m = SIX[o];
                return fill(template(p, r).replace('’', '\'')
                        .replace(T_S_OBJ, object(p, r, m[0]))
                        .replace(T_M_OBJ, object(p, r, m[1]))
                        .replace(T_REST, object(p, r, m[2])), spectator, magician);
            }
            case EIGHT:
                return fill(template(p, r).replace(T_ITEM, item(p, o))
                        .replace(T_POINTED, item(p, pointed(p))), spectator, magician);
            case DIE:
                return fill(template(p, r).replace(T_NUMBER, dieNumber(p, o)), spectator, magician);
            case ESP:
                return fill(template(p, r).replace(T_SYMBOL, symbol(p, o)), spectator, magician);
            case COIN: {
                int n = flips(p);
                StringBuilder sb = new StringBuilder();
                String open = coinOpening(p);
                if (!open.trim().isEmpty()) sb.append(open);
                for (int k = 1; k <= n; k++) {
                    String line = coinLine(p, k);
                    for (int j = 1; j <= n; j++) {
                        line = line.replace(flipTag(j), coinTails(n, o, j) ? tails(p) : heads(p));
                    }
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(line);
                }
                String out = sb.toString();
                for (int j = 1; j <= n; j++) out = out.replace(flipTag(j), coinTails(n, o, j) ? tails(p) : heads(p));
                return fill(out, spectator, magician);
            }
            default: {
                String value = customOutcome(p, r, o);
                String tpl = template(p, r);
                if (tpl.trim().isEmpty()) return fill(value, spectator, magician);
                return fill(tpl.replace(T_OUTCOME, value), spectator, magician);
            }
        }
    }

    /** The prediction for a pickup. */
    public static String build(SharedPreferences p, int r, PickupDetector.Pickup pk, String typed) {
        if (r == NORMAL) return sentence(p, r, pk.spectatorFacing() ? 0 : 1, typed);
        return sentence(p, r, outcomeFor(p, r, slot(pk)), typed);
    }

    /** What the rehearsal shows under REVEALED. */
    public static String describe(int r, PickupDetector.Pickup pk) {
        if (r == NORMAL) return PickupDetector.stateName(pk.facing);
        return SLOT_NAMES[slot(pk)];
    }

    /** The slot shown as the example on a routine page, or -1 to list every outcome. */
    public static int exampleSlot(SharedPreferences p, int r) {
        switch (r) {
            case EIGHT: return LEFT_P;
            case DIE: return BACK_P;
            case ESP: return FRONT_P;
            case COIN: return flips(p) == 3 ? BACK_P : BACK;
            default: return -1;
        }
    }

    // ---------------------------------------------------------------- Build It Yourself ideas

    public static final class Idea {
        public final String name, description, template;
        public final String[] outcomes;
        Idea(String name, String description, String template, String... outcomes) {
            this.name = name; this.description = description; this.template = template; this.outcomes = outcomes;
        }
    }

    public static final Idea[] IDEAS = {
            new Idea("התאמת צבעים", "עד 8 צבעים. הקהל מצביע בהרמת ידיים; התחזית מציינת את הצבע המנצח.",
                    "הצבע האהוב הערב יהיה " + T_OUTCOME + ".",
                    "אדום", "כחול", "ירוק", "צהוב", "סגול", "כתום"),
            new Idea("אבן, נייר ומספריים", "3 תוצאות. אתם תמיד זורקים אבן; התחזית אומרת מה הצופה זורק ומי מנצח.",
                    "הבחירה של " + T_SPECTATOR + ": " + T_OUTCOME,
                    "אבן, ותיקו.", "נייר, והניצחון של " + T_SPECTATOR + ".", "מספריים, והניצחון של " + T_MAGICIAN + "."),
            new Idea("תעלומת רצח", "8 תוצאות. 2 חשודים × 2 כלי רצח × 2 חדרים, מסופרים כסיפור אחד.",
                    "זה היה " + T_OUTCOME + ".",
                    "המשרת, עם החבל, בספרייה", "המשרת, עם החבל, במטבח",
                    "המשרת, עם הפמוט, בספרייה", "המשרת, עם הפמוט, במטבח",
                    "הטבחית, עם החבל, בספרייה", "הטבחית, עם החבל, במטבח",
                    "הטבחית, עם הפמוט, בספרייה", "הטבחית, עם הפמוט, במטבח"),
            new Idea("סידור ישיבה", "6 תוצאות. שלושה אנשים, שלושה כיסאות, כל סידור אפשרי.",
                    "משמאל לימין: " + T_OUTCOME + ".",
                    "אדם 1, אדם 2, אדם 3", "אדם 1, אדם 3, אדם 2", "אדם 2, אדם 1, אדם 3",
                    "אדם 2, אדם 3, אדם 1", "אדם 3, אדם 1, אדם 2", "אדם 3, אדם 2, אדם 1"),
    };

    /** Adds an outcome on the first free pickup. Returns false at the maximum. */
    public static boolean addCustomOutcome(SharedPreferences p, int r) {
        int c = customCount(p, r);
        if (c >= MAX_OUTCOMES) return false;
        int[] m = slotMap(p, r);
        int free = SLOT_ORDER[c];
        for (int s : SLOT_ORDER) if (m[s] < 0) { free = s; break; }
        p.edit().putInt(Prefs.k(r, "count"), c + 1)
                .putString(Prefs.k(r, "out" + c), "")
                .putInt(Prefs.k(r, "slot" + c), free).commit();
        return true;
    }

    /** Removes outcome i, shifting the later ones up. Keeps at least two. */
    public static boolean removeCustomOutcome(SharedPreferences p, int r, int i) {
        int c = customCount(p, r);
        if (c <= 2) return false;
        SharedPreferences.Editor e = p.edit();
        for (int j = i; j < c - 1; j++) {
            e.putString(Prefs.k(r, "out" + j), customOutcome(p, r, j + 1));
            e.putInt(Prefs.k(r, "slot" + j), customSlot(p, r, j + 1));
        }
        e.remove(Prefs.k(r, "out" + (c - 1)));
        e.remove(Prefs.k(r, "slot" + (c - 1)));
        e.putInt(Prefs.k(r, "count"), c - 1);
        e.commit();
        return true;
    }

    /** Moves outcome i to a slot; if another outcome used that slot, the two swap. */
    public static void setCustomSlot(SharedPreferences p, int r, int i, int slot) {
        int c = customCount(p, r);
        int old = customSlot(p, r, i);
        SharedPreferences.Editor e = p.edit();
        for (int j = 0; j < c; j++) {
            if (j != i && customSlot(p, r, j) == slot) e.putInt(Prefs.k(r, "slot" + j), old);
        }
        e.putInt(Prefs.k(r, "slot" + i), slot);
        e.commit();
    }

    /** The idea a custom routine was started from, or "" if none. */
    public static String ideaInUse(SharedPreferences p, int r) {
        return Prefs.raw(p, r, "idea", "");
    }

    /** Fills a custom routine from an idea, overwriting its outcomes and prophecy. */
    public static void applyIdea(SharedPreferences p, int r, Idea idea) {
        SharedPreferences.Editor e = p.edit();
        if (Prefs.raw(p, r, "name", "").trim().isEmpty()) e.putString(Prefs.k(r, "name"), idea.name);
        int n = Math.min(MAX_OUTCOMES, idea.outcomes.length);
        e.putInt(Prefs.k(r, "count"), n);
        for (int i = 0; i < MAX_OUTCOMES; i++) {
            if (i < n) {
                e.putString(Prefs.k(r, "out" + i), idea.outcomes[i]);
                e.putInt(Prefs.k(r, "slot" + i), SLOT_ORDER[i]);
            } else {
                e.remove(Prefs.k(r, "out" + i));
                e.remove(Prefs.k(r, "slot" + i));
            }
        }
        e.putString(Prefs.k(r, "template"), idea.template);
        e.putString(Prefs.k(r, "idea"), idea.name);
        e.commit();
    }
}
