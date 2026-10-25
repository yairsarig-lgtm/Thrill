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
    private Typeface cinzel, cinzelBold, title; // cinzel* hold the Hebrew serif here

    private View statusDiamond;
    private TextView statusTitle, statusSub;
    private TextView statusButton;
    private EditText templateField;
    private TextView testPhase, testDesc, testResult, testValues;
    private final List<TextView> previews = new ArrayList<>();
    private PickupDetector.Pickup lastPickup;
    private int rehearseRoutine = 1;
    private final List<TextView> sixPreviews = new ArrayList<>();
    private final TextView[] routinePills = new TextView[2];

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
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        try {
            cinzel = Typeface.createFromAsset(getAssets(), "fonts/frl-medium.ttf");
            cinzelBold = Typeface.createFromAsset(getAssets(), "fonts/frl-bold.ttf");
            title = Typeface.createFromAsset(getAssets(), "fonts/cinzel-bold.ttf");
        } catch (Exception e) {
            cinzel = Typeface.SERIF;
            cinzelBold = Typeface.create(Typeface.SERIF, Typeface.BOLD);
            title = cinzelBold;
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
        body.addView(expertCard());
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
        emblem.setContentDescription("סמל Thrill");
        FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(dp(220), dp(220),
                Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        elp.topMargin = dp(12);
        f.addView(emblem, elp);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(0, 0, 0, dp(22));
        TextView name = text("THRILL", 34, TEXT);
        name.setTypeface(title);
        name.setShadowLayer(dp(10), 0, 0, 0xCC000000);
        centered(name, 0.35f);
        titles.addView(name);
        TextView sub = text("מקלדת חיזוי · גרסה עברית 0.14", 12, MUTED);
        centered(sub, 0.04f);
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
        t.setPaddingRelative(dp(12), 0, dp(8), 0);
        statusTitle = text("", 14, TEXT);
        statusTitle.setTypeface(Typeface.DEFAULT_BOLD);
        statusSub = text("", 12, MUTED);
        t.addView(statusTitle);
        t.addView(statusSub);
        c.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        statusButton = outlineButton("החלפה");
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
        c.addView(heading("המשתתפים"));
        c.addView(subtitle("השם שלכם, שם לשימוש אם לא מוקלד שם, ושלושת החפצים. "
                + "חפץ 3 הוא החפץ המאולץ בחשיפה הרגילה."));
        LinearLayout r1 = row();
        r1.addView(field("קוסם", Prefs.MAGICIAN, false, false), weight(1));
        r1.addView(gap());
        r1.addView(field("צופה (אם לא הוקלד שם)", Prefs.SPECTATOR_FALLBACK, false, false), weight(1));
        c.addView(r1);
        LinearLayout r2 = row();
        r2.setPadding(0, dp(10), 0, 0);
        r2.addView(field("חפץ 1", Prefs.OBJ1, false, false), weight(1));
        r2.addView(gap());
        r2.addView(field("חפץ 2", Prefs.OBJ2, false, false), weight(1));
        r2.addView(gap());
        r2.addView(field("חפץ 3", Prefs.OBJ3, false, false), weight(1));
        c.addView(r2);
        return c;
    }

    private View prophecyCard() {
        LinearLayout c = card(true);
        c.addView(heading("הנבואה"));
        c.addView(subtitle("משותפת לשתי החשיפות."));
        LinearLayout f = field("טקסט החיזוי", Prefs.TEMPLATE, true, false);
        templateField = (EditText) f.getChildAt(1);
        c.addView(f);

        c.addView(chips(templateField, Prefs.TAGS));

        // What each tag means
        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.VERTICAL);
        legend.setPadding(dp(14), dp(10), dp(14), dp(12));
        legend.setBackground(rounded(BG, BORDER, dp(1), dp(10)));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(14);
        legend.setLayoutParams(llp);
        TextView lt = text("הקישו על תגית כדי להוסיף אותה · כל תגית הופכת ל:", 11, MUTED);
        lt.setLetterSpacing(0f);
        lt.setPadding(0, 0, 0, dp(4));
        legend.addView(lt);
        for (int i = 0; i < Prefs.TAGS.length; i++) {
            TextView tag = text(Prefs.TAGS[i], 13, ACCENT);
            tag.setTypeface(Typeface.DEFAULT_BOLD);
            tag.setPadding(0, dp(8), 0, 0);
            legend.addView(tag);
            legend.addView(text(Prefs.TAG_HELP[i], 13, TEXT));
        }
        TextView how = text("חשיפה רגילה: מי שמרים את הטלפון מקבל את חפץ 1, והשני את חפץ 2. "
                + "חשיפת המומחה: אופן ההרמה קובע מי מקבל איזה חפץ.", 12, MUTED);
        how.setPadding(0, dp(12), 0, 0);
        how.setLineSpacing(0, 1.2f);
        legend.addView(how);
        c.addView(legend);
        return c;
    }

    private View revealCard() {
        LinearLayout c = card(true);
        c.addView(heading("החשיפה"));
        c.addView(subtitle("שגרה 1 · הקלידו את השם, או את השם + 1. חפץ 3 מאולץ."));
        String[] who = {"הצופה מרים", "הקוסם מרים"};
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
            label.setLetterSpacing(0f);
            box.addView(label);
            TextView pv = text("", 15, TEXT);
            pv.setTypeface(cinzel);
            pv.setLineSpacing(0, 1.3f);
            pv.setPadding(0, dp(6), 0, 0);
            previews.add(pv);
            box.addView(pv);
            c.addView(box);
        }
        TextView rule = text("כלל: מניחים את הטלפון הפוך, כשהתחתית מופנית אליכם.", 12, MUTED);
        rule.setPadding(0, dp(10), 0, 0);
        c.addView(rule);
        return c;
    }

    private View expertCard() {
        LinearLayout c = card(true);
        c.addView(heading("חשיפת המומחה"));
        c.addView(subtitle("שגרה 2 · הקלידו את השם + 2. בלי כפייה: ההרמה מכסה כל סידור אפשרי."));

        // Pause length
        LinearLayout pr = row();
        pr.addView(text("אורך העצירה", 14, TEXT), weight(1));
        final TextView pv = text("", 14, ACCENT);
        pv.setTypeface(Typeface.DEFAULT_BOLD);
        pr.addView(pv);
        c.addView(pr);
        SeekBar pause = new SeekBar(this);
        pause.setMax(8); // 0.2 .. 1.0 s
        pause.setProgress((int) Math.round((Prefs.pauseMs(prefs) - 200) / 100.0));
        tintSlider(pause);
        pv.setText(seconds(Prefs.pauseMs(prefs)));
        pause.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                long ms = 200 + p * 100L;
                prefs.edit().putLong(Prefs.PAUSE_MS, ms).apply();
                detector.setPauseMs(ms);
                pv.setText(seconds(ms));
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        c.addView(pause);
        c.addView(text("הרימו מעט את הקצה והחזיקו בלי לזוז. נקישה שקטה מאשרת את העצירה, ואז מסיימים את ההרמה.", 12, MUTED));

        // The six outcomes
        for (int i = 0; i < 6; i++) {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(14), dp(12), dp(14), dp(12));
            box.setBackground(rounded(BG, BORDER, dp(1), dp(10)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(i == 0 ? 16 : 10);
            box.setLayoutParams(lp);
            TextView label = text(Prefs.SIX_NAMES[i], 11, MUTED);
            label.setLetterSpacing(0f);
            box.addView(label);
            TextView sp = text("", 15, TEXT);
            sp.setTypeface(cinzel);
            sp.setLineSpacing(0, 1.3f);
            sp.setPadding(0, dp(6), 0, 0);
            sixPreviews.add(sp);
            box.addView(sp);
            c.addView(box);
        }
        TextView rule = text("הקצוות הם מנקודת המבט שלכם, כשהטלפון הפוך והתחתית מופנית אליכם.", 12, MUTED);
        rule.setPadding(0, dp(10), 0, 0);
        c.addView(rule);
        return c;
    }

    private TextView subtitle(String s) {
        TextView t = text(s, 12, MUTED);
        t.setPadding(0, 0, 0, dp(14));
        t.setLineSpacing(0, 1.2f);
        return t;
    }

    /** Placeholder chips: tap to insert at the cursor of the given field. */
    private View chips(EditText target, String[] phs) {
        FlowRow row = new FlowRow(this, dp(8));
        row.setPadding(0, dp(12), 0, 0);
        for (String ph : phs) {
            TextView chip = text(ph, 12, ACCENT);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setBackground(rounded(CHIP, 0, 0, dp(999)));
            chip.setOnClickListener(v -> {
                int a = Math.max(target.getSelectionStart(), 0);
                int b = Math.max(target.getSelectionEnd(), 0);
                target.getText().replace(Math.min(a, b), Math.max(a, b), ph);
            });
            row.addView(chip);
        }
        return row;
    }

    private void tintSlider(SeekBar sb) {
        sb.setProgressTintList(ColorStateList.valueOf(CRIMSON));
        sb.setThumbTintList(ColorStateList.valueOf(ACCENT));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(BORDER_STRONG));
        sb.setPadding(dp(4), dp(14), dp(4), dp(14));
    }

    private String seconds(long ms) {
        return String.format(Locale.US, "%.1f s", ms / 1000.0);
    }

    private View vanishCard() {
        LinearLayout c = card(true);
        c.addView(heading("ההיעלמות"));

        c.addView(label("אחרי שהחיזוי מופיע"));
        Spinner after = new Spinner(this, Spinner.MODE_DROPDOWN);
        // Shown order -> stored value (0 switch back, 1 hide, 2 nothing, 3 switch back and hide)
        String[] opts = {"חזרה למקלדת שלי והסתרתה", "חזרה למקלדת שלי",
                "הסתרת המקלדת", "לא לעשות כלום"};
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

        c.addView(toggle("נקישה שקטה כשהטלפון דרוך", Prefs.VIBRATE, true));

        LinearLayout tr = row();
        tr.setPadding(0, dp(14), 0, 0);
        TextView tl = text("זווית ההפעלה", 14, TEXT);
        tr.addView(tl, weight(1));
        final TextView tv = text("", 14, ACCENT);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tr.addView(tv);
        c.addView(tr);

        SeekBar trig = new SeekBar(this);
        trig.setMax(30); // 10° .. 40°
        trig.setProgress(Math.round(Prefs.trigger(prefs) - 10f));
        trig.setProgressTintList(ColorStateList.valueOf(CRIMSON));
        trig.setThumbTintList(ColorStateList.valueOf(ACCENT));
        trig.setProgressBackgroundTintList(ColorStateList.valueOf(BORDER_STRONG));
        trig.setPadding(dp(4), dp(14), dp(4), dp(14));
        tv.setText(degrees(Prefs.trigger(prefs)));
        trig.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                float t = 10f + p;
                prefs.edit().putFloat(Prefs.TRIGGER, t).apply();
                detector.setTrigger(t);
                tv.setText(degrees(t));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        c.addView(trig);
        c.addView(text("ערך נמוך יותר מפעיל מוקדם יותר בהרמה, כשהמסך עדיין מוסתר.", 12, MUTED));
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

        TextView h = text("חזרה", 17, TEXT);
        h.setTypeface(cinzelBold);
        centered(h, 0.03f);
        c.addView(h);

        LinearLayout pills = row();
        pills.setGravity(Gravity.CENTER);
        pills.setPadding(0, dp(12), 0, 0);
        for (int i = 0; i < 2; i++) {
            final int r = i + 1;
            TextView pill = text(r == 1 ? "רגילה" : "מומחה", 12, TEXT);
            pill.setTypeface(Typeface.DEFAULT_BOLD);
            pill.setLetterSpacing(0f);
            pill.setGravity(Gravity.CENTER);
            pill.setMinHeight(dp(44));
            pill.setPadding(dp(18), 0, dp(18), 0);
            pill.setOnClickListener(v -> {
                rehearseRoutine = r;
                lastPickup = null;
                refreshPills();
            });
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plp.setMargins(dp(5), 0, dp(5), 0);
            pills.addView(pill, plp);
            routinePills[i] = pill;
        }
        pills.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        c.addView(pills);
        refreshPills();

        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.t_mark);
        mark.setAlpha(0.9f);
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(dp(64), dp(73));
        mlp.setMargins(0, dp(14), 0, dp(10));
        c.addView(mark, mlp);

        testPhase = text("", 26, ACCENT);
        testPhase.setTypeface(cinzelBold);
        centered(testPhase, 0.04f);
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
            TextView w = text("לא נמצא ג'ירוסקופ: זיהוי המרים יתבסס על הערכה.", 12, ACCENT);
            w.setGravity(Gravity.CENTER);
            w.setPadding(0, dp(10), 0, 0);
            c.addView(w);
        }
        return c;
    }

    private void refreshPills() {
        for (int i = 0; i < 2; i++) {
            boolean on = rehearseRoutine == i + 1;
            routinePills[i].setTextColor(on ? TEXT : MUTED);
            routinePills[i].setBackground(on ? rounded(CRIMSON, 0, 0, dp(999))
                    : rounded(0x00000000, BORDER_STRONG, dp(1), dp(999)));
        }
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = isKeyboardEnabled();
        ((GradientDrawable) statusDiamond.getBackground()).setColor(on ? GREEN : CRIMSON);
        statusTitle.setText(on ? "המקלדת פעילה" : "המקלדת לא פעילה");
        statusSub.setText(on ? "מופיעה כ־„Thrill עברית” בבורר המקלדות"
                : "הפעילו את „Thrill עברית” בהגדרות המקלדת. אנדרואיד מזהיר על כל מקלדת; המקלדת הזו לא שולחת דבר.");
        statusButton.setText(on ? "החלפה" : "הפעלה");
        detector.setTrigger(Prefs.trigger(prefs));
        detector.setPauseMs(Prefs.pauseMs(prefs));
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
            lastPickup = null;
            if (prefs.getBoolean(Prefs.VIBRATE, true)) {
                Haptics.armedTap(this);
            }
        });
    }

    @Override public void onDisarmed() { main.post(() -> lastPickup = null); }

    @Override public void onPaused() {
        main.post(() -> {
            if (rehearseRoutine == 2 && prefs.getBoolean(Prefs.VIBRATE, true)) Haptics.armedTap(this);
        });
    }

    @Override public void onPickup(PickupDetector.Pickup pk) { main.post(() -> lastPickup = pk); }

    private void refreshTester() {
        boolean six = rehearseRoutine == 2;
        PickupDetector.Phase ph = detector.getPhase();
        if (six && detector.isPaused() && (ph == PickupDetector.Phase.ARMED || ph == PickupDetector.Phase.LIFTING)) {
            testPhase.setText("עצירה");
            testDesc.setText("העצירה נקלטה. סיימו את ההרמה.");
        } else {
            switch (ph) {
                case WAITING:
                    testPhase.setText("ממתין");
                    testDesc.setText("הניחו את הטלפון הפוך, כשהתחתית מופנית אליכם.");
                    break;
                case ARMED:
                    testPhase.setText("דרוך");
                    testDesc.setText(six ? "הרימו קצה, או הרימו מעט והחזיקו בלי לזוז כדי לעצור."
                            : "הפוך ומחכה. הרימו אותו כדי לראות איזו חשיפה מופעלת.");
                    break;
                case LIFTING:
                    testPhase.setText("עולה");
                    testDesc.setText("");
                    break;
                default:
                    testPhase.setText("נחשף");
                    testDesc.setText(lastPickup == null ? "" : six
                            ? PickupDetector.edgeName(lastPickup.edge, lastPickup.paused)
                            : PickupDetector.stateName(lastPickup.facing));
                    break;
            }
        }
        testResult.setText(lastPickup == null ? "" : Prefs.build(prefs, rehearseRoutine, lastPickup, ""));
        float[] g = detector.getGravity();
        testValues.setText(String.format(Locale.US, "הטיה %.0f° · x %.1f · y %.1f · z %.1f",
                detector.getTilt(), g[0], g[1], g[2]));
    }

    private void refreshPreviews() {
        int[] states = {PickupDetector.FACE_SPECTATOR_TOP_UP, PickupDetector.FACE_MAGICIAN_TOP_UP};
        for (int i = 0; i < previews.size(); i++) {
            previews.get(i).setText(Prefs.buildPrediction(prefs, states[i], ""));
        }
        for (int i = 0; i < sixPreviews.size(); i++) {
            sixPreviews.get(i).setText(Prefs.buildSix(prefs, i, ""));
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
        return String.format(Locale.US, "%.0f°", t);
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
        t.setLetterSpacing(0.02f);
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
            t.setPaddingRelative(dp(12), 0, dp(40), 0);
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
