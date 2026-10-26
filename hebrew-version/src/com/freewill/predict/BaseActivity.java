package com.freewill.predict;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The Thrill look: colours, fonts and the building blocks every screen uses. */
abstract class BaseActivity extends Activity {

    static final int BG = 0xFF0B0809;
    static final int CARD = 0xFF160D10;
    static final int BORDER = 0xFF3A1A20;
    static final int BORDER_STRONG = 0xFF5A2630;
    static final int TEXT = 0xFFEDE6DC;
    static final int MUTED = 0xFFB9A9A6;
    static final int FAINT = 0xFF8E7F7C;
    static final int ACCENT = 0xFFE0606D;
    static final int CRIMSON = 0xFFC42A3A;
    static final int CHIP = 0xFF2A1217;
    static final int GREEN = 0xFF5BBF7A;
    static final int GOLD = 0xFFE8C547;

    static final String VERSION = "v0.19";

    protected SharedPreferences prefs;
    protected Typeface serif, serifBold;
    /** The logo face, used for THRILL and the routine numerals. */
    protected Typeface logo;
    protected final Handler main = new Handler(Looper.getMainLooper());

    /** Things that show values derived from settings; re-run after any edit. */
    protected final List<Runnable> refreshers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = Prefs.get(this);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        try {
            serif = Typeface.createFromAsset(getAssets(), "fonts/frl-medium.ttf");
            serifBold = Typeface.createFromAsset(getAssets(), "fonts/frl-bold.ttf");
            logo = Typeface.createFromAsset(getAssets(), "fonts/cinzel-bold.ttf");
        } catch (Exception e) {
            serif = Typeface.SERIF;
            serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD);
            logo = serifBold;
        }
    }

    void refreshAll() {
        for (Runnable r : refreshers) r.run();
    }

    // ---------------------------------------------------------------- basics

    int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    TextView serifText(String s, int sp, int color, boolean bold) {
        TextView t = text(s, sp, color);
        t.setTypeface(bold ? serifBold : serif);
        return t;
    }

    TextView logoText(String s, int sp, int color) {
        TextView t = text(s, sp, color);
        t.setTypeface(logo);
        return t;
    }

    LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        return r;
    }

    LinearLayout column() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        return c;
    }

    LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    LinearLayout.LayoutParams matchWrap(int topMarginDp) {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(topMarginDp);
        return lp;
    }

    LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    View spacer(int wDp, int hDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(wDp), dp(hDp)));
        return v;
    }

    GradientDrawable rounded(int fill, int stroke, int strokeW, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        if (strokeW > 0) g.setStroke(strokeW, stroke);
        return g;
    }

    GradientDrawable dashed(int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x00000000);
        g.setCornerRadius(radius);
        g.setStroke(dp(1), stroke, dp(5), dp(4));
        return g;
    }

    /** Full-width, centred text; letter spacing balanced so it sits truly in the middle. */
    void centered(TextView t, float spacingEm) {
        if (t.getTypeface() != logo) spacingEm = 0.02f;
        t.setLetterSpacing(spacingEm);
        t.setGravity(Gravity.CENTER);
        t.setPaddingRelative(Math.round(spacingEm * t.getTextSize()), t.getPaddingTop(), 0, t.getPaddingBottom());
        t.setLayoutParams(matchWrap());
    }

    // ---------------------------------------------------------------- cards and headings

    LinearLayout card() {
        LinearLayout c = column();
        c.setPaddingRelative(dp(16), dp(18), dp(16), dp(18));
        c.setBackground(rounded(CARD, BORDER, dp(1), dp(14)));
        c.setLayoutParams(matchWrap(18));
        return c;
    }

    View diamond(int color, int sizeDp) {
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

    View heading(String s) {
        return heading(s, CRIMSON, null);
    }

    View heading(String s, int diamondColor, View trailing) {
        LinearLayout r = row();
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPaddingRelative(0, 0, 0, dp(14));
        r.addView(diamond(diamondColor, 8));
        TextView t = serifText(s, 15, TEXT, true);
        t.setLetterSpacing(0.02f);
        t.setPaddingRelative(dp(10), 0, dp(10), 0);
        r.addView(t);
        View line = new View(this);
        line.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{BORDER_STRONG, 0x005A2630}));
        r.addView(line, new LinearLayout.LayoutParams(0, dp(1), 1f));
        if (trailing != null) {
            r.addView(spacer(8, 1));
            r.addView(trailing);
        }
        return r;
    }

    TextView subtitle(String s) {
        TextView t = text(s, 12, MUTED);
        t.setPaddingRelative(0, 0, 0, dp(14));
        t.setLineSpacing(0, 1.2f);
        return t;
    }

    TextView hint(String s) {
        TextView t = text(s, 12, MUTED);
        t.setLineSpacing(0, 1.2f);
        t.setPaddingRelative(0, dp(8), 0, 0);
        return t;
    }

    TextView label(String s) {
        TextView l = text(s, 12, MUTED);
        l.setPaddingRelative(0, 0, 0, dp(6));
        return l;
    }

    // ---------------------------------------------------------------- inputs

    StateListDrawable inputBackground() {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, rounded(BG, CRIMSON, dp(2), dp(10)));
        s.addState(new int[]{}, rounded(BG, BORDER, dp(1), dp(10)));
        return s;
    }

    interface OnText { void changed(String s); }

    EditText input(String value, String placeholder, boolean multi, OnText cb) {
        EditText e = new EditText(this);
        e.setText(value);
        if (placeholder != null) e.setHint(placeholder);
        e.setTextColor(TEXT);
        e.setHintTextColor(FAINT);
        e.setHighlightColor(0x66C42A3A);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setBackground(inputBackground());
        e.setPaddingRelative(dp(12), dp(10), dp(12), dp(10));
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | (multi ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        if (multi) {
            e.setMinLines(2);
            e.setGravity(Gravity.TOP | Gravity.START);
            e.setLineSpacing(0, 1.25f);
        } else {
            e.setSingleLine(true);
            e.setMinHeight(dp(48));
        }
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {
                cb.changed(s.toString());
                refreshAll();
            }
        });
        e.setLayoutParams(matchWrap());
        return e;
    }

    /** A labelled text field saved under a preference key. */
    LinearLayout field(String labelText, String key, String value, String placeholder, boolean multi) {
        LinearLayout f = column();
        f.addView(label(labelText));
        f.addView(input(value, placeholder, multi, s -> prefs.edit().putString(key, s).apply()));
        return f;
    }

    /** The EditText inside a field(). */
    static EditText editOf(LinearLayout field) {
        return (EditText) field.getChildAt(1);
    }

    /** Placeholder chips that insert themselves at the cursor of the given field. */
    View chips(EditText target, String[] tags) {
        FlowRow row = new FlowRow(this, dp(8));
        row.setPaddingRelative(0, dp(12), 0, 0);
        for (String tag : tags) row.addView(chip(target, tag));
        return row;
    }

    TextView chip(EditText target, String tag) {
        TextView c = text(tag, 12, ACCENT);
        c.setTypeface(Typeface.DEFAULT_BOLD);
        c.setPaddingRelative(dp(12), dp(8), dp(12), dp(8));
        c.setBackground(rounded(CHIP, 0, 0, dp(999)));
        c.setOnClickListener(v -> {
            if (target == null) return;
            int a = Math.max(target.getSelectionStart(), 0);
            int b = Math.max(target.getSelectionEnd(), 0);
            target.getText().replace(Math.min(a, b), Math.max(a, b), tag);
            target.requestFocus();
        });
        return c;
    }

    /** Each tag as a tappable chip next to what it becomes. */
    View tagLegend(EditText target, String[] tags, String[] help) {
        LinearLayout l = column();
        l.setPaddingRelative(0, dp(10), 0, 0);
        for (int i = 0; i < tags.length; i++) {
            LinearLayout r = row();
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setMinimumHeight(dp(44));
            r.addView(chip(target, tags[i]));
            TextView h = text(help[i], 12, MUTED);
            h.setPaddingRelative(dp(10), 0, 0, 0);
            r.addView(h, weight(1));
            l.addView(r);
        }
        return l;
    }

    interface OnCheck { void checked(boolean on); }

    Switch toggle(String labelText, boolean checked, OnCheck cb) {
        Switch sw = new Switch(this);
        sw.setText(labelText);
        sw.setTextColor(TEXT);
        sw.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        sw.setChecked(checked);
        sw.setMinHeight(dp(48));
        int[][] st = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(st, new int[]{ACCENT, MUTED}));
        sw.setTrackTintList(new ColorStateList(st, new int[]{CRIMSON, BORDER_STRONG}));
        sw.setOnCheckedChangeListener((b, v) -> cb.checked(v));
        sw.setLayoutParams(matchWrap(6));
        return sw;
    }

    interface OnPick { void pick(int pos); }

    /** A dropdown with the red chevron, kept 16dp from the right edge. */
    Spinner dropdown(String[] options, int selected, OnPick cb) {
        Spinner sp = new Spinner(this, Spinner.MODE_DROPDOWN);
        sp.setAdapter(new DarkAdapter(options));
        Drawable chevron = getDrawable(R.drawable.ic_chevron);
        LayerDrawable bg = new LayerDrawable(new Drawable[]{inputBackground(), chevron});
        bg.setLayerGravity(1, Gravity.END | Gravity.CENTER_VERTICAL);
        bg.setLayerInsetEnd(1, dp(16));
        sp.setBackground(bg);
        sp.setPopupBackgroundDrawable(rounded(CARD, BORDER_STRONG, dp(1), dp(10)));
        sp.setDropDownVerticalOffset(dp(52));
        sp.post(() -> sp.setDropDownWidth(sp.getWidth()));
        sp.setSelection(Math.max(0, Math.min(options.length - 1, selected)), false);
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            boolean first = true;
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                if (first) { first = false; return; }
                cb.pick(pos);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        sp.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        return sp;
    }

    interface OnProgress { void progress(int value); }

    SeekBar slider(int max, int progress, OnProgress cb) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgress(progress);
        sb.setProgressTintList(ColorStateList.valueOf(CRIMSON));
        sb.setThumbTintList(ColorStateList.valueOf(ACCENT));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(BORDER_STRONG));
        sb.setPaddingRelative(dp(4), dp(14), dp(4), dp(14));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { if (u) cb.progress(p); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        return sb;
    }

    /** "Label ............ value" above a slider. */
    LinearLayout valueRow(String labelText, TextView value) {
        LinearLayout r = row();
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(dp(44));
        r.addView(text(labelText, 14, TEXT), weight(1));
        value.setTypeface(Typeface.DEFAULT_BOLD);
        r.addView(value);
        return r;
    }

    /** A row of equal buttons where one is selected. */
    LinearLayout segmented(String[] labels, String[] subs, int selected, OnPick cb) {
        LinearLayout r = row();
        List<TextView> buttons = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView b = text(labels[i] + (subs == null ? "" : "\n" + subs[i]), 14, TEXT);
            b.setGravity(Gravity.CENTER);
            b.setMinHeight(dp(52));
            b.setPaddingRelative(dp(6), dp(6), dp(6), dp(6));
            b.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMarginStart(dp(8));
            b.setLayoutParams(lp);
            buttons.add(b);
            b.setOnClickListener(v -> {
                styleSegments(buttons, idx);
                cb.pick(idx);
            });
            r.addView(b);
        }
        styleSegments(buttons, selected);
        return r;
    }

    void styleSegments(List<TextView> buttons, int selected) {
        for (int i = 0; i < buttons.size(); i++) {
            boolean on = i == selected;
            TextView b = buttons.get(i);
            b.setBackground(on ? rounded(CRIMSON, 0, 0, dp(10)) : rounded(0x00000000, BORDER_STRONG, dp(1), dp(10)));
            b.setTextColor(on ? 0xFFFFFFFF : MUTED);
        }
    }

    TextView outlineButton(String s) {
        TextView b = text(s, 13, TEXT);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(44));
        b.setMinWidth(dp(76));
        b.setPaddingRelative(dp(14), 0, dp(14), 0);
        b.setBackground(rounded(0x00000000, BORDER_STRONG, dp(1), dp(10)));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    String degrees(float t) { return String.format(Locale.US, "%.0f°", t); }

    String seconds(long ms) { return String.format(Locale.US, "%.1f שנ׳", ms / 1000.0); }

    // ---------------------------------------------------------------- shared sections

    /** The Vanish: what happens after the reveal, silent tap and trigger tilt, for one routine. */
    View vanishCard(int r) {
        LinearLayout c = card();
        c.addView(heading("ההיעלמות"));
        c.addView(label("אחרי שהתחזית מופיעה"));
        String[] opts = {"חזרה למקלדת שלי והסתרה", "חזרה למקלדת שלי",
                "הסתרת המקלדת", "לא לעשות כלום"};
        final int[] codes = {Prefs.AFTER_SWITCH_HIDE, Prefs.AFTER_SWITCH, Prefs.AFTER_HIDE, Prefs.AFTER_NOTHING};
        int stored = Prefs.after(prefs, r), sel = 0;
        for (int i = 0; i < codes.length; i++) if (codes[i] == stored) sel = i;
        c.addView(dropdown(opts, sel, pos -> prefs.edit().putInt(Prefs.k(r, "after"), codes[pos]).apply()));
        c.addView(toggle("רטט שקט כשהטלפון דרוך", Prefs.vibrate(prefs, r),
                on -> prefs.edit().putBoolean(Prefs.k(r, "vibrate"), on).apply()));

        TextView tv = text(degrees(Prefs.trigger(prefs, r)), 14, ACCENT);
        LinearLayout tr = valueRow("זווית הפעלה", tv);
        tr.setPaddingRelative(0, dp(8), 0, 0);
        c.addView(tr);
        c.addView(slider(30, Math.round(Prefs.trigger(prefs, r) - 10f), p -> {
            float t = 10f + p;
            prefs.edit().putFloat(Prefs.k(r, "trigger"), t).apply();
            tv.setText(degrees(t));
            onSettingsChanged();
        }));
        c.addView(text("זווית נמוכה יותר מפעילה מוקדם יותר בהרמה, כשהמסך עדיין מוטה הרחק מהצופים.", 12, MUTED));
        return c;
    }

    /** Pause length slider (0.2–1.0 s) for one routine. */
    View pauseControl(int r) {
        LinearLayout c = column();
        TextView pv = text(seconds(Prefs.pauseMs(prefs, r)), 14, ACCENT);
        c.addView(valueRow("משך העצירה", pv));
        c.addView(slider(8, (int) Math.round((Prefs.pauseMs(prefs, r) - 200) / 100.0), p -> {
            long ms = 200 + p * 100L;
            prefs.edit().putLong(Prefs.k(r, "pause"), ms).apply();
            pv.setText(seconds(ms));
            onSettingsChanged();
        }));
        return c;
    }

    /** Called when a setting that the live detector uses has changed. */
    void onSettingsChanged() {}

    // ---------------------------------------------------------------- the table diagram

    interface CellText { String at(int slot); }

    /** The table seen from above: spectator, phone face down, magician; what each edge writes. */
    View diagram(CellText labels) {
        LinearLayout d = column();
        d.setGravity(Gravity.CENTER_HORIZONTAL);
        d.setPaddingRelative(dp(8), dp(16), dp(8), dp(14));
        d.setBackground(rounded(BG, BORDER, dp(1), dp(12)));
        d.setLayoutParams(matchWrap(4));
        d.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);   // left and right are the table's, never mirrored

        TextView top = text("כאן יושב הצופה", 11, MUTED);
        centered(top, 0.15f);
        d.addView(top);

        LinearLayout r1 = row(), r2 = row(), r3 = row();
        r2.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout front = cell(), back = cell(), left = cell(), right = cell();
        r1.addView(spacer(0, 1), weight(1)); r1.addView(front, weight(1)); r1.addView(spacer(0, 1), weight(1));
        r2.addView(left, weight(1)); r2.addView(phone()); r2.addView(right, weight(1));
        r3.addView(spacer(0, 1), weight(1)); r3.addView(back, weight(1)); r3.addView(spacer(0, 1), weight(1));
        for (LinearLayout rr : new LinearLayout[]{r1, r2, r3}) {
            rr.setPaddingRelative(0, dp(5), 0, dp(5));
            d.addView(rr, matchWrap());
        }

        TextView bottom = text("כאן אתם יושבים", 11, MUTED);
        centered(bottom, 0.15f);
        d.addView(bottom);
        TextView key = text("‖ = עם עצירה", 11, MUTED);
        centered(key, 0f);
        key.setPaddingRelative(0, dp(6), 0, 0);
        d.addView(key);

        Runnable fill = () -> {
            fillCell(front, "קדמי", labels.at(Routines.FRONT), labels.at(Routines.FRONT_P));
            fillCell(back, "אחורי", labels.at(Routines.BACK), labels.at(Routines.BACK_P));
            fillCell(left, "שמאל", labels.at(Routines.LEFT), labels.at(Routines.LEFT_P));
            fillCell(right, "ימין", labels.at(Routines.RIGHT), labels.at(Routines.RIGHT_P));
        };
        fill.run();
        refreshers.add(fill);
        return d;
    }

    private LinearLayout cell() {
        LinearLayout c = column();
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPaddingRelative(dp(4), dp(8), dp(4), dp(8));
        LinearLayout.LayoutParams lp = weight(1);
        lp.setMargins(dp(4), 0, dp(4), 0);
        c.setLayoutParams(lp);
        return c;
    }

    private void fillCell(LinearLayout c, String title, String plain, String paused) {
        c.removeAllViews();
        boolean used = plain != null || paused != null;
        c.setBackground(used ? rounded(CHIP, 0, 0, dp(8)) : dashed(BORDER, dp(8)));
        TextView t = text(title, 12, used ? ACCENT : FAINT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        c.addView(t);
        if (!used) {
            TextView u = text("לא בשימוש", 11, FAINT);
            u.setGravity(Gravity.CENTER);
            c.addView(u);
            return;
        }
        TextView a = text(plain == null ? "—" : plain, 12, plain == null ? FAINT : TEXT);
        a.setGravity(Gravity.CENTER);
        c.addView(a);
        if (paused != null) {
            TextView b = text("‖ " + paused, 12, MUTED);
            b.setGravity(Gravity.CENTER);
            c.addView(b);
        }
    }

    private View phone() {
        LinearLayout p = column();
        p.setGravity(Gravity.CENTER_HORIZONTAL);
        p.setPaddingRelative(0, dp(8), 0, dp(8));
        p.setBackground(rounded(CARD, BORDER_STRONG, dp(2), dp(14)));
        // A portrait phone: same width as the side cells' column, taller than wide.
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(132), 1f);
        lp.setMargins(dp(12), dp(6), dp(12), dp(6));
        p.setLayoutParams(lp);
        TextView top = text("למעלה", 9, FAINT);
        centered(top, 0.15f);
        p.addView(top);
        View fill = new View(this);
        p.addView(fill, new LinearLayout.LayoutParams(1, 0, 1f));
        TextView fd = text("הפוך", 10, FAINT);
        centered(fd, 0f);
        p.addView(fd);
        View fill2 = new View(this);
        p.addView(fill2, new LinearLayout.LayoutParams(1, 0, 1f));
        View bar = new View(this);
        bar.setBackground(rounded(BORDER_STRONG, 0, 0, dp(2)));
        p.addView(bar, new LinearLayout.LayoutParams(dp(26), dp(4)));
        return p;
    }

    // ---------------------------------------------------------------- helpers classes

    /** Spinner adapter in the dark palette, leaving room for the chevron. */
    class DarkAdapter extends ArrayAdapter<String> {
        DarkAdapter(String[] items) {
            super(BaseActivity.this, 0, new ArrayList<>(java.util.Arrays.asList(items)));
        }

        void replace(String[] items) {
            clear();
            addAll(items);
            notifyDataSetChanged();
        }

        @Override public View getView(int pos, View convert, ViewGroup parent) {
            TextView t = text(getItem(pos), 15, TEXT);
            t.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            t.setPaddingRelative(dp(12), 0, dp(48), 0);
            t.setMinHeight(dp(48));
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            return t;
        }

        @Override public View getDropDownView(int pos, View convert, ViewGroup parent) {
            TextView t = text(getItem(pos), 15, TEXT);
            t.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            t.setPaddingRelative(dp(16), 0, dp(16), 0);
            t.setMinHeight(dp(48));
            return t;
        }
    }

    /** Simple wrapping row. */
    static class FlowRow extends ViewGroup {
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
            setMeasuredDimension(MeasureSpec.getSize(wSpec), y + lineH + getPaddingTop() + getPaddingBottom());
        }

        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int maxW = r - l - getPaddingLeft() - getPaddingRight();
            int x = 0, y = 0, lineH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View c = getChildAt(i);
                int w = c.getMeasuredWidth(), h = c.getMeasuredHeight();
                if (x > 0 && x + w > maxW) { x = 0; y += lineH + spacing; lineH = 0; }
                c.layout(getPaddingLeft() + x, getPaddingTop() + y, getPaddingLeft() + x + w, getPaddingTop() + y + h);
                x += w + spacing;
                lineH = Math.max(lineH, h);
            }
        }
    }
}
