package com.freewill.predict;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A plain-looking keyboard. The performer types the spectator's name into any text field,
 * lays the phone face down (bottom toward the magician) and leaves it. When the phone is
 * picked up, the typed name is replaced by the full prediction and the keyboard gets out
 * of the way.
 */
public class PredictionKeyboard extends InputMethodService implements PickupDetector.Listener {

    private static final String[] ROWS = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};

    private final Handler main = new Handler(Looper.getMainLooper());
    private final StringBuilder typed = new StringBuilder();
    private PickupDetector detector;
    private PowerManager.WakeLock wakeLock;
    private boolean shift = true;
    private boolean fired;
    private TextView spaceKey;
    private LinearLayout root;
    private final java.util.List<TextView> letterKeys = new java.util.ArrayList<>();

    private int bg, keyBg, keyBgSpecial, keyText;

    @Override
    public void onCreate() {
        super.onCreate();
        detector = new PickupDetector(this, this);
    }

    // ---------- UI ----------

    @Override
    public View onCreateInputView() {
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        bg = night ? 0xFF1E1F22 : 0xFFE8EAED;
        keyBg = night ? 0xFF3C3D40 : 0xFFFFFFFF;
        keyBgSpecial = night ? 0xFF2C2D30 : 0xFFC9CDD3;
        keyText = night ? 0xFFE8EAED : 0xFF202124;

        letterKeys.clear();
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        int p = dp(4);
        root.setPadding(p, dp(8), p, dp(10));

        // Number row
        LinearLayout nums = row();
        nums.getLayoutParams().height = dp(46);
        for (char ch : "1234567890".toCharArray()) {
            String d = String.valueOf(ch);
            nums.addView(key(d, 1f, keyBgSpecial, v -> type(d)));
        }
        root.addView(nums);

        // Row 1-2
        root.addView(letterRow(ROWS[0], 0));
        root.addView(letterRow(ROWS[1], 0.5f));

        // Row 3: shift, letters, backspace
        LinearLayout r3 = row();
        r3.addView(special("⇧", 1.5f, v -> { shift = !shift; refreshCase(); }));
        for (char ch : ROWS[2].toCharArray()) r3.addView(letter(ch));
        TextView del = special("⌫", 1.5f, v -> backspace());
        r3.addView(del);
        root.addView(r3);

        // Row 4: switch, comma, space, period, enter
        LinearLayout r4 = row();
        r4.addView(special("🌐", 1.3f, v -> switchKeyboard()));
        r4.addView(key(",", 1f, keyBgSpecial, v -> type(",")));
        spaceKey = key("", 5f, keyBg, v -> type(" "));
        r4.addView(spaceKey);
        r4.addView(key(".", 1f, keyBgSpecial, v -> type(".")));
        r4.addView(special("⏎", 1.6f, v -> enter()));
        root.addView(r4);

        refreshCase();
        updateDot(false);
        return root;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));
        return r;
    }

    private LinearLayout letterRow(String letters, float sidePad) {
        LinearLayout r = row();
        if (sidePad > 0) r.addView(spacer(sidePad));
        for (char ch : letters.toCharArray()) r.addView(letter(ch));
        if (sidePad > 0) r.addView(spacer(sidePad));
        return r;
    }

    private View spacer(float w) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, w));
        return v;
    }

    private TextView letter(char ch) {
        TextView t = key(String.valueOf(ch), 1f, keyBg, v -> {
            String s = ((TextView) v).getText().toString();
            type(s);
        });
        letterKeys.add(t);
        return t;
    }

    private TextView special(String label, float w, View.OnClickListener l) {
        return key(label, w, keyBgSpecial, l);
    }

    private TextView key(String label, float weight, int color, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(keyText);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, label.length() > 1 ? 16 : 21);
        t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(7));
        t.setBackground(d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, weight);
        int m = dp(3);
        lp.setMargins(m, m, m, m);
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            l.onClick(v);
        });
        return t;
    }

    private void refreshCase() {
        for (TextView t : letterKeys) {
            String s = t.getText().toString();
            t.setText(shift ? s.toUpperCase() : s.toLowerCase());
        }
    }

    private void updateDot(boolean armed) {
        if (spaceKey == null) return;
        boolean showDot = false; // the performer never sees the screen while it is face down
        spaceKey.setText(armed && showDot ? "·" : "");
    }

    // ---------- typing ----------

    private void type(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.commitText(s, 1);
        typed.append(s);
        // Auto-capitalise the start of each word of the name.
        boolean wantShift = s.equals(" ");
        if (shift != wantShift) { shift = wantShift; refreshCase(); }
    }

    private void backspace() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (typed.length() > 0) {
            typed.setLength(typed.length() - 1);
            ic.deleteSurroundingText(1, 0);
        } else {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        }
        boolean wantShift = typed.length() == 0 || typed.charAt(typed.length() - 1) == ' ';
        if (shift != wantShift) { shift = wantShift; refreshCase(); }
    }

    private void enter() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        EditorInfo ei = getCurrentInputEditorInfo();
        int action = ei == null ? EditorInfo.IME_ACTION_NONE
                : ei.imeOptions & EditorInfo.IME_MASK_ACTION;
        boolean multiLine = ei != null
                && (ei.inputType & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
        if (!multiLine && action != EditorInfo.IME_ACTION_NONE
                && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action);
        } else {
            ic.commitText("\n", 1);
        }
        typed.setLength(0); // a new line starts a fresh name
        shift = true;
        refreshCase();
    }

    private void switchKeyboard() {
        if (!goBack()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        }
    }

    // ---------- lifecycle ----------

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (!restarting) {
            typed.setLength(0);
            shift = true;
            refreshCase();
        }
        fired = false;
        updateDot(false);
        keepScreenOn(true);
        detector.setTrigger(Prefs.trigger(Prefs.get(this)));
        detector.setPauseMs(Prefs.pauseMs(Prefs.get(this)));
        detector.start();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        detector.stop();
        keepScreenOn(false);
        super.onFinishInputView(finishingInput);
    }

    @Override
    public void onDestroy() {
        detector.stop();
        keepScreenOn(false);
        super.onDestroy();
    }

    private void keepScreenOn(boolean on) {
        try {
            Window w = getWindow() == null ? null : getWindow().getWindow();
            if (w != null) {
                if (on) w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } catch (Exception ignored) {}
        try {
            if (on) {
                if (wakeLock == null) {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    @SuppressWarnings("deprecation")
                    int flags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
                    wakeLock = pm.newWakeLock(flags, "freewill:keyboard");
                    wakeLock.setReferenceCounted(false);
                }
                wakeLock.acquire(30 * 60 * 1000L);
            } else if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception ignored) {}
    }

    // ---------- pickup ----------

    @Override
    public void onArmed() {
        main.post(() -> {
            updateDot(true);
            if (Prefs.get(this).getBoolean(Prefs.VIBRATE, true)) {
                Haptics.armedTap(this);
            }
        });
    }

    @Override
    public void onDisarmed() {
        main.post(() -> updateDot(false));
    }

    @Override
    public void onPaused() {
        // Lets the performer feel that the pause has registered (routine 2 only).
        main.post(() -> {
            if (Prefs.get(this).getBoolean(Prefs.VIBRATE, true) && parseRoutine(typed.toString()) == 2) {
                Haptics.armedTap(this);
            }
        });
    }

    @Override
    public void onPickup(PickupDetector.Pickup pk) {
        main.post(() -> reveal(pk));
    }

    /** "Noa", "Noa1" or "Noa 1" = routine 1; "Noa2" = routine 2. Unknown numbers fall back to 1. */
    static int parseRoutine(String typed) {
        java.util.regex.Matcher m = TRAILING_NUMBER.matcher(typed);
        if (!m.matches()) return 1;
        try {
            int r = Integer.parseInt(m.group(2));
            return (r == 1 || r == 2) ? r : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** The name without the routine number. */
    static String stripRoutine(String typed) {
        java.util.regex.Matcher m = TRAILING_NUMBER.matcher(typed);
        return m.matches() ? m.group(1) : typed;
    }

    private static final java.util.regex.Pattern TRAILING_NUMBER =
            java.util.regex.Pattern.compile("^(.*?)\\s*(\\d+)\\s*$", java.util.regex.Pattern.DOTALL);

    private void reveal(PickupDetector.Pickup pk) {
        if (fired) return;
        fired = true;
        detector.stop();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        SharedPreferences p = Prefs.get(this);
        String raw = typed.toString();
        int routine = parseRoutine(raw);
        String prediction = Prefs.build(p, routine, pk, stripRoutine(raw));

        ic.beginBatchEdit();
        if (typed.length() > 0) ic.deleteSurroundingText(typed.length(), 0);
        ic.commitText(prediction, 1);
        ic.endBatchEdit();
        typed.setLength(0);
        updateDot(false);

        int after = p.getInt(Prefs.AFTER, 0);
        main.postDelayed(() -> {
            if (after == 0) {
                if (!goBack()) requestHideSelf(0);
            } else if (after == 1) {
                requestHideSelf(0);
            } else if (after == 3) {
                // Hide first, then hand the field back to the normal keyboard (stays hidden).
                requestHideSelf(0);
                main.postDelayed(this::goBack, 150);
            }
        }, 120);
    }

    /** Switch back to the keyboard used before this one. */
    private boolean goBack() {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            try { return switchToPreviousInputMethod(); } catch (Exception e) { return false; }
        }
        return false;
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }
}
