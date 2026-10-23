package com.freewill.predict;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Thrill settings + rehearsal screen. */
public class MainActivity extends Activity implements PickupDetector.Listener {

    // Palette (matches the mockup)
    private static final int BG = 0xFF0B0809;
    private static final int CARD = 0xFF160D10;
    private static final int BORDER = 0xFF3A1A20;
    private static final int BORDER_STRONG = 0xFF5A2630;
    private static final int TEXT = 0xFFEDE6DC;
    private static final int MUTED = 0xFFB9A9A6;
    private static final int ACCENT = 0xFFE0606D;
    private static final int CRIMSON = 0xFFC42A3A;
    private static final int CHIP = 0xFF2A1217;
    private static final int GREEN = 0xFF5BBF7A;

    private SharedPreferences prefs;
    private PickupDetector detector;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Typeface cinzel, cinzelBold;

    private View statusDiamond;
    private TextView statusTitle, statusSub;
    private TextView statusButton;
    private EditText templateField;
    private TextView testPhase, testDesc, testResult, testValues;
    private final List<TextView> previews = new ArrayList<>();
    private int lastState = -1;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshTester();
            main.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = Prefs.get(this);
        detector = new PickupDetector(this, this);
        detector.setRearmAfterFire(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        try {
            cinzel = Typeface.createFromAsset(getAssets(), "fonts/cinzel-medium.ttf");
            cinzelBold = Typeface.createFromAsset(getAssets(), "fonts/cinzel-bold.ttf");
        } catch (Exception e) {
            cinzel = Typeface.SERIF;
            cinzelBold = Typeface.create(Typeface.SERIF, Typeface.BOLD);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);

        page.addView(hero());

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(8), dp(16), dp(40));
        page.addView(body);

        body.addView(statusCard());
        body.addView(playersCard());
        body.addView(prophecyCard());
        body.addView(revealCard());
        body.addView(vanishCard());
        body.addView(rehearsalCard());

        setContentView(scroll);
        refreshPreviews();
    }

    // ---------------------------------------------------------------- sections

    private View hero() {
        FrameLayout f = new FrameLayout(this);
        int h = dp(330);
        f.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));

        // Velvet backdrop, already darkened towards the page colour at the bottom.
        ImageView bg = new ImageView(this);
        bg.setImageResource(R.drawable.hero_bg);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        f.addView(bg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));

        // The metal emblem, softly faded at its edges.
        ImageView emblem = new ImageView(this);
        emblem.setImageResource(R.drawable.hero_emblem);
        emblem.setContentDescription("Thrill emblem");
        FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(dp(220), dp(220),
                Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        elp.topMargin = dp(12);
        f.addView(emblem, elp);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(0, 0, 0, dp(22));
        TextView name = text("THRILL", 34, TEXT);
        name.setTypeface(cinzelBold);
        name.setShadowLayer(dp(10), 0, 0, 0xCC000000);
        centered(name, 0.35f);
        titles.addView(name);
        TextView sub = text("PREDICTION KEYBOARD · V0.8", 11, MUTED);
        centered(sub, 0.25f);
        sub.setPadding(sub.getPaddingLeft(), dp(4), 0, 0);
        titles.addView(sub);
        f.addView(titles, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        return f;
    }

    /** Full-width, centred text; letter spacing balanced so it sits truly in the middle. */
    private void centered(TextView t, float spacingEm) {
        t.setLetterSpacing(spacingEm);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Math.round(spacingEm * t.getTextSize()), t.getPaddingTop(), 0, t.getPaddingBottom());
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private View statusCard() {
        LinearLayout c = card(false);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(16), dp(12), dp(12), dp(12));

        statusDiamond = diamond(GREEN, 10);
        c.addView(statusDiamond);

        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(12), 0, dp(8), 0);
        statusTitle = text("", 14, TEXT);
        statusTitle.setTypeface(Typeface.DEFAULT_BOLD);
        statusSub = text("", 12, MUTED);
        t.addView(statusTitle);
        t.addView(statusSub);
        c.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        statusButton = outlineButton("Switch");
        statusButton.setOnClickListener(v -> {
            if (isKeyboardEnabled()) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            } else {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        c.addView(statusButton);
        return c;
    }

    private View playersCard() {
        LinearLayout c = card(true);
        c.addView(heading("THE PLAYERS"));
        LinearLayout r1 = row();
        r1.addView(field("Magician", Prefs.MAGICIAN, false, false), weight(1));
        r1.addView(gap());
        r1.addView(field("Spectator (fallback)", Prefs.SPECTATOR_FALLBACK, false, false), weight(1));
        c.addView(r1);
        LinearLayout r2 = row();
        r2.setPadding(0, dp(10), 0, 0);
        r2.addView(field("Object 1", Prefs.OBJ1, false, false), weight(1));
        r2.addView(gap());
        r2.addView(field("Object 2", Prefs.OBJ2, false, false), weight(1));
        r2.addView(gap());
        r2.addView(field("Forced", Prefs.OBJ3, false, true), weight(1));
        c.addView(r2);
        return c;
    }

    private View prophecyCard() {
        LinearLayout c = card(true);
        c.addView(heading("THE PROPHECY"));
        LinearLayout f = field("Prediction text", Prefs.TEMPLATE, true, false);
        templateField = (EditText) f.getChildAt(1);
        c.addView(f);

        // Placeholder chips: tap to insert at the cursor.
        FlowRow chips = new FlowRow(this, dp(8));
        chips.setPadding(0, dp(12), 0, 0);
        for (String ph : new String[]{"{first}", "{second}", "{obj1}", "{obj2}", "{obj3}"}) {
            TextView chip = text(ph, 12, ACCENT);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setBackground(rounded(CHIP, 0, 0, dp(999)));
            chip.setOnClickListener(v -> {
                int s = Math.max(templateField.getSelectionStart(), 0);
                int e = Math.max(templateField.getSelectionEnd(), 0);
                templateField.getText().replace(Math.min(s, e), Math.max(s, e), ph);
            });
            chips.addView(chip);
        }
        c.addView(chips);
        TextView hint = text("{first} is whoever picks the phone up.", 12, MUTED);
        hint.setPadding(0, dp(10), 0, 0);
        c.addView(hint);
        return c;
    }

    private View revealCard() {
        LinearLayout c = card(true);
        c.addView(heading("THE REVEAL"));
        String[] who = {"SPECTATOR PICKS IT UP", "MAGICIAN PICKS IT UP"};
        for (int i = 0; i < 2; i++) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(14), dp(12), dp(14), dp(12));
            box.setBackground(rounded(BG, BORDER, dp(1), dp(10)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = dp(10);
            box.setLayoutParams(lp);
            TextView label = text(who[i], 11, MUTED);
            label.setLetterSpacing(0.12f);
            box.addView(label);
            TextView pv = text("", 15, TEXT);
            pv.setTypeface(cinzel);
            pv.setLineSpacing(0, 1.3f);
            pv.setPadding(0, dp(6), 0, 0);
            previews.add(pv);
            box.addView(pv);
            c.addView(box);
        }
        TextView rule = text("Rule: lay it face down, bottom pointing at you.", 12, MUTED);
        rule.setPadding(0, dp(10), 0, 0);
        c.addView(rule);
        return c;
    }

    private View vanishCard() {
        LinearLayout c = card(true);
        c.addView(heading("THE VANISH"));

        c.addView(label("After the prediction appears"));
        Spinner after = new Spinner(this, Spinner.MODE_DROPDOWN);
        // Shown order -> stored value (0 switch back, 1 hide, 2 nothing, 3 switch back and hide)
        String[] opts = {"Switch back to my keyboard and hide it", "Switch back to my keyboard",
                "Hide the keyboard", "Do nothing"};
        final int[] codes = {3, 0, 1, 2};
        after.setAdapter(new DarkAdapter(opts));
        android.graphics.drawable.LayerDrawable bgWithArrow = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{inputBackground(false), getDrawable(R.drawable.ic_chevron)});
        bgWithArrow.setLayerGravity(1, Gravity.END | Gravity.CENTER_VERTICAL);
        bgWithArrow.setLayerInsetEnd(1, dp(14));
        after.setBackground(bgWithArrow);
        after.setPopupBackgroundDrawable(rounded(CARD, BORDER_STRONG, dp(1), dp(10)));
        after.setDropDownVerticalOffset(dp(52));
        after.post(() -> after.setDropDownWidth(after.getWidth()));
        int stored = prefs.getInt(Prefs.AFTER, 0);
        for (int i = 0; i < codes.length; i++) if (codes[i] == stored) after.setSelection(i);
        after.setOnItemSelectedListener(new SimpleSelect(pos -> prefs.edit().putInt(Prefs.AFTER, codes[pos]).apply()));
        after.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        c.addView(after);

        c.addView(toggle("Silent tap when armed", Prefs.VIBRATE, true));

        LinearLayout tr = row();
        tr.setPadding(0, dp(14), 0, 0);
        TextView tl = text("Trigger tilt", 14, TEXT);
        tr.addView(tl, weight(1));
        final TextView tv = text("", 14, ACCENT);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tr.addView(tv);
        c.addView(tr);

        SeekBar trig = new SeekBar(this);
        trig.setMax(40); // 5.0 .. 9.0 m/s²
        trig.setProgress(Math.round((Prefs.trigger(prefs) - 5f) * 10));
        trig.setProgressTintList(ColorStateList.valueOf(CRIMSON));
        trig.setThumbTintList(ColorStateList.valueOf(ACCENT));
        trig.setProgressBackgroundTintList(ColorStateList.valueOf(BORDER_STRONG));
        trig.setPadding(dp(4), dp(14), dp(4), dp(14));
        tv.setText(degrees(Prefs.trigger(prefs)));
        trig.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                float t = 5f + p / 10f;
                prefs.edit().putFloat(Prefs.TRIGGER, t).apply();
                detector.setTrigger(t);
                tv.setText(degrees(t));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        c.addView(trig);
        c.addView(text("Lower fires earlier in the pickup, with the screen still angled away.", 12, MUTED));
        return c;
    }

    private View rehearsalCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(dp(16), dp(24), dp(16), dp(24));
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientCenter(0.5f, 0.3f);
        g.setGradientRadius(dp(260));
        g.setColors(new int[]{0xFF3B0B14, CARD});
        g.setCornerRadius(dp(14));
        g.setStroke(dp(1), BORDER_STRONG);
        c.setBackground(g);
        c.setLayoutParams(cardParams());

        TextView h = text("REHEARSAL", 15, TEXT);
        h.setTypeface(cinzelBold);
        centered(h, 0.17f);
        c.addView(h);

        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.t_mark);
        mark.setAlpha(0.9f);
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(dp(64), dp(73));
        mlp.setMargins(0, dp(14), 0, dp(10));
        c.addView(mark, mlp);

        testPhase = text("", 26, ACCENT);
        testPhase.setTypeface(cinzelBold);
        centered(testPhase, 0.22f);
        c.addView(testPhase);

        testDesc = text("", 13, MUTED);
        testDesc.setGravity(Gravity.CENTER);
        testDesc.setPadding(dp(16), dp(6), dp(16), 0);
        testDesc.setGravity(Gravity.CENTER);
        testDesc.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        c.addView(testDesc);

        testResult = text("", 15, TEXT);
        testResult.setTypeface(cinzel);
        testResult.setGravity(Gravity.CENTER);
        testResult.setLineSpacing(0, 1.3f);
        testResult.setPadding(dp(8), dp(12), dp(8), 0);
        testResult.setGravity(Gravity.CENTER);
        testResult.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        c.addView(testResult);

        testValues = text("", 11, 0xFF8E7F7C);
        testValues.setTypeface(Typeface.MONOSPACE);
        testValues.setPadding(0, dp(12), 0, 0);
        testValues.setGravity(Gravity.CENTER);
        testValues.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        c.addView(testValues);

        if (!detector.hasRotationSensor()) {
            TextView w = text("No gyroscope found: who picked it up will be guessed.", 12, ACCENT);
            w.setGravity(Gravity.CENTER);
            w.setPadding(0, dp(10), 0, 0);
            c.addView(w);
        }
        return c;
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = isKeyboardEnabled();
        ((GradientDrawable) statusDiamond.getBackground()).setColor(on ? GREEN : CRIMSON);
        statusTitle.setText(on ? "Keyboard enabled" : "Keyboard not enabled");
        statusSub.setText(on ? "Listed as “Thrill” in your keyboard picker"
                : "Turn on “Thrill” in your keyboard settings. Android warns about every keyboard; this one sends nothing.");
        statusButton.setText(on ? "Switch" : "Enable");
        detector.setTrigger(Prefs.trigger(prefs));
        detector.start();
        main.post(ticker);
    }

    @Override
    protected void onPause() {
        detector.stop();
        main.removeCallbacks(ticker);
        super.onPause();
    }

    @Override public void onArmed() {
        main.post(() -> {
            lastState = -1;
            if (prefs.getBoolean(Prefs.VIBRATE, true)) {
                Haptics.armedTap(this);
            }
        });
    }

    @Override public void onDisarmed() { main.post(() -> lastState = -1); }

    @Override public void onPickup(int state) { main.post(() -> lastState = state); }

    private void refreshTester() {
        switch (detector.getPhase()) {
            case WAITING:
                testPhase.setText("WAITING");
                testDesc.setText("Lay it face down, bottom pointing at you.");
                break;
            case ARMED:
                testPhase.setText("ARMED");
                testDesc.setText("Face down and waiting. Pick it up to see which reveal fires.");
                break;
            case LIFTING:
                testPhase.setText("RISING");
                testDesc.setText("");
                break;
            default:
                testPhase.setText("REVEALED");
                testDesc.setText(lastState >= 0 ? PickupDetector.stateName(lastState) : "");
                break;
        }
        testResult.setText(lastState >= 0 ? Prefs.buildPrediction(prefs, lastState, "") : "");
        float[] g = detector.getGravity();
        testValues.setText(String.format(Locale.US, "x %.1f · y %.1f · z %.1f", g[0], g[1], g[2]));
    }

    private void refreshPreviews() {
        int[] states = {PickupDetector.FACE_SPECTATOR_TOP_UP, PickupDetector.FACE_MAGICIAN_TOP_UP};
        for (int i = 0; i < previews.size(); i++) {
            previews.get(i).setText(Prefs.buildPrediction(prefs, states[i], ""));
        }
    }

    private boolean isKeyboardEnabled() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return false;
        for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
            if (i.getPackageName().equals(getPackageName())) return true;
        }
        return false;
    }

    private String degrees(float t) {
        double deg = Math.toDegrees(Math.asin(Math.min(1.0, t / 9.81)));
        return String.format(Locale.US, "%.0f°", deg);
    }

    // ---------------------------------------------------------------- building blocks

    private LinearLayout card(boolean vertical) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        c.setPadding(dp(16), dp(18), dp(16), dp(18));
        c.setBackground(rounded(CARD, BORDER, dp(1), dp(14)));
        c.setLayoutParams(cardParams());
        return c;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(18);
        return lp;
    }

    private View heading(String s) {
        LinearLayout r = row();
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, 0, 0, dp(14));
        r.addView(diamond(CRIMSON, 8));
        TextView t = text(s, 15, TEXT);
        t.setTypeface(cinzelBold);
        t.setLetterSpacing(0.17f);
        t.setPadding(dp(10), 0, dp(10), 0);
        r.addView(t);
        View line = new View(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{BORDER_STRONG, 0x005A2630});
        line.setBackground(g);
        r.addView(line, new LinearLayout.LayoutParams(0, dp(1), 1f));
        return r;
    }

    private View diamond(int color, int sizeDp) {
        View d = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        d.setBackground(g);
        d.setRotation(45f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        lp.setMargins(dp(2), 0, dp(2), 0);
        d.setLayoutParams(lp);
        return d;
    }

    private TextView label(String s) {
        TextView l = text(s, 12, MUTED);
        l.setPadding(0, 0, 0, dp(6));
        return l;
    }

    private LinearLayout field(String labelText, String key, boolean multi, boolean strong) {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.addView(label(labelText));
        EditText e = new EditText(this);
        e.setText(Prefs.str(prefs, key));
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setHighlightColor(0x66C42A3A);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setBackground(inputBackground(strong));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | (multi ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        if (multi) {
            e.setMinLines(3);
            e.setGravity(Gravity.TOP);
            e.setLineSpacing(0, 1.25f);
        } else {
            e.setSingleLine(true);
            e.setMinHeight(dp(48));
        }
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {
                prefs.edit().putString(key, s.toString()).apply();
                refreshPreviews();
            }
        });
        f.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return f;
    }

    private StateListDrawable inputBackground(boolean strong) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, rounded(BG, CRIMSON, dp(2), dp(10)));
        s.addState(new int[]{}, rounded(BG, strong ? BORDER_STRONG : BORDER, dp(1), dp(10)));
        return s;
    }

    private View toggle(String labelText, String key, boolean def) {
        Switch sw = new Switch(this);
        sw.setText(labelText);
        sw.setTextColor(TEXT);
        sw.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setMinHeight(dp(48));
        int[][] st = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(st, new int[]{ACCENT, MUTED}));
        sw.setTrackTintList(new ColorStateList(st, new int[]{CRIMSON, BORDER_STRONG}));
        sw.setOnCheckedChangeListener((b, v) -> prefs.edit().putBoolean(key, v).apply());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        sw.setLayoutParams(lp);
        return sw;
    }

    private TextView outlineButton(String s) {
        TextView b = text(s, 13, TEXT);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(44));
        b.setMinWidth(dp(76));
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setBackground(rounded(0x00000000, BORDER_STRONG, dp(1), dp(10)));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private GradientDrawable rounded(int fill, int stroke, int strokeW, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        if (strokeW > 0) g.setStroke(strokeW, stroke);
        return g;
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        return r;
    }

    private View gap() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1));
        return v;
    }

    private LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    /** Spinner adapter in the dark palette. */
    private class DarkAdapter extends ArrayAdapter<String> {
        DarkAdapter(String[] items) { super(MainActivity.this, 0, items); }

        @Override public View getView(int pos, View convert, ViewGroup parent) {
            TextView t = text(getItem(pos), 15, TEXT);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(dp(12), 0, dp(40), 0);
            t.setMinHeight(dp(48));
            t.setSingleLine(true);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            return t;
        }

        @Override public View getDropDownView(int pos, View convert, ViewGroup parent) {
            TextView t = text(getItem(pos), 15, TEXT);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(dp(16), 0, dp(16), 0);
            t.setMinHeight(dp(48));
            return t;
        }
    }

    /** Simple wrapping row for the placeholder chips. */
    private static class FlowRow extends ViewGroup {
        private final int spacing;
        FlowRow(Context c, int spacing) { super(c); this.spacing = spacing; }

        @Override protected void onMeasure(int wSpec, int hSpec) {
            int maxW = MeasureSpec.getSize(wSpec) - getPaddingLeft() - getPaddingRight();
            int x = 0, y = 0, lineH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View c = getChildAt(i);
                c.measure(MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                if (x > 0 && x + c.getMeasuredWidth() > maxW) { x = 0; y += lineH + spacing; lineH = 0; }
                x += c.getMeasuredWidth() + spacing;
                lineH = Math.max(lineH, c.getMeasuredHeight());
            }
            setMeasuredDimension(MeasureSpec.getSize(wSpec),
                    y + lineH + getPaddingTop() + getPaddingBottom());
        }

        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int maxW = r - l - getPaddingLeft() - getPaddingRight();
            int x = 0, y = 0, lineH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View c = getChildAt(i);
                int w = c.getMeasuredWidth(), h = c.getMeasuredHeight();
                if (x > 0 && x + w > maxW) { x = 0; y += lineH + spacing; lineH = 0; }
                c.layout(getPaddingLeft() + x, getPaddingTop() + y,
                        getPaddingLeft() + x + w, getPaddingTop() + y + h);
                x += w + spacing;
                lineH = Math.max(lineH, h);
            }
        }
    }

    /** Lets spinners take a lambda. */
    private static class SimpleSelect implements android.widget.AdapterView.OnItemSelectedListener {
        interface OnPick { void pick(int pos); }
        private final OnPick cb;
        SimpleSelect(OnPick cb) { this.cb = cb; }
        @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { cb.pick(pos); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
    }
}
