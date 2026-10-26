package com.freewill.predict;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

/** One routine's own page: its players, prophecy, reveal, vanish and rehearsal. */
public class RoutineActivity extends BaseActivity {

    static final String EXTRA_ROUTINE = "routine";

    private int r;
    private ScrollView scroll;
    private RehearsalCard rehearsal;
    private boolean resumed;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        r = getIntent().getIntExtra(EXTRA_ROUTINE, Routines.NORMAL);
        if (!Routines.exists(prefs, r)) { finish(); return; }
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (rehearsal != null) rehearsal.start();
    }

    @Override
    protected void onPause() {
        resumed = false;
        if (rehearsal != null) rehearsal.stop();
        super.onPause();
    }

    @Override
    void onSettingsChanged() {
        if (rehearsal != null) rehearsal.applySettings();
    }

    /** Rebuilds the page (after a structural change) and keeps the scroll position. */
    private void rebuild() {
        final int y = scroll == null ? 0 : scroll.getScrollY();
        if (rehearsal != null) rehearsal.stop();
        build();
        scroll.post(() -> scroll.scrollTo(0, y));
        if (resumed) rehearsal.start();
    }

    private void build() {
        refreshers.clear();
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        LinearLayout page = column();
        scroll.addView(page);

        page.addView(header());

        LinearLayout body = column();
        body.setPaddingRelative(dp(16), dp(8), dp(16), dp(40));
        page.addView(body, matchWrap());

        TextView ex = text(Routines.explanation(prefs, r), 14, MUTED);
        ex.setGravity(Gravity.CENTER);
        ex.setLineSpacing(0, 1.3f);
        body.addView(ex, matchWrap());

        if (Routines.isCustom(r)) {
            body.addView(customRoutineCard());
            body.addView(customOutcomesCard());
            body.addView(prophecyCard());
            body.addView(vanishCard(r));
            body.addView(ideasCard());
        } else {
            body.addView(playersCard());
            body.addView(prophecyCard());
            body.addView(revealCard());
            body.addView(vanishCard(r));
        }

        rehearsal = new RehearsalCard(this, r, false);
        body.addView(rehearsal.build(Routines.name(prefs, r)));

        if (Routines.isCustom(r)) {
            TextView del = outlineButton("מחיקת הרוטינה");
            del.setTextColor(ACCENT);
            del.setMinHeight(dp(48));
            del.setLayoutParams(matchWrap(18));
            del.setOnClickListener(v -> confirmDelete());
            body.addView(del);
        }
        setContentView(scroll);
    }

    // ---------------------------------------------------------------- header

    private View header() {
        FrameLayout f = new FrameLayout(this);
        int h = dp(250);
        f.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));

        ImageView bg = new ImageView(this);
        bg.setImageResource(R.drawable.hero_bg);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        f.addView(bg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));

        TextView back = text("›", 30, TEXT);
        back.setGravity(Gravity.CENTER);
        back.setPaddingRelative(0, 0, dp(2), dp(4));
        back.setBackground(rounded(0x990B0809, 0, 0, dp(22)));
        back.setContentDescription("חזרה לרוטינות");
        back.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP | Gravity.START);
        blp.topMargin = dp(14);
        blp.setMarginStart(dp(12));
        f.addView(back, blp);

        LinearLayout t = column();
        t.setPaddingRelative(0, 0, 0, dp(20));
        TextView num = logoText(Routines.roman(r), 60, ACCENT);
        num.setShadowLayer(dp(14), 0, dp(4), 0x80C42A3A);
        centered(num, 0f);
        t.addView(num);
        TextView title = serifText("", 22, TEXT, true);
        centered(title, 0.12f);
        title.setPaddingRelative(title.getPaddingLeft(), dp(8), 0, 0);
        t.addView(title);
        Runnable setTitle = () -> title.setText(Routines.name(prefs, r).toUpperCase());
        setTitle.run();
        refreshers.add(setTitle);

        TextView code = text(r == Routines.NORMAL ? "הקלידו את השם, או את השם + 1"
                : (Routines.isCustom(r) ? "בנו בעצמכם · הקלידו את השם + " : "הקלידו את השם + ") + r, 12, ACCENT);
        code.setTypeface(Typeface.DEFAULT_BOLD);
        code.setPaddingRelative(dp(10), dp(5), dp(10), dp(5));
        code.setBackground(rounded(0xE62A1217, 0, 0, dp(6)));
        LinearLayout codeRow = row();
        codeRow.setGravity(Gravity.CENTER);
        codeRow.setPaddingRelative(0, dp(8), 0, 0);
        codeRow.addView(code);
        t.addView(codeRow, matchWrap());

        f.addView(t, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        return f;
    }

    // ---------------------------------------------------------------- players

    private LinearLayout namesRow() {
        LinearLayout r1 = row();
        r1.addView(field("הקוסם", Prefs.k(r, "magician"), Prefs.magician(prefs, r), null, false), weight(1));
        r1.addView(spacer(10, 1));
        r1.addView(field("הצופה, אם לא הוקלד שם", Prefs.k(r, "spectator"), Prefs.spectatorFallback(prefs, r), null, false), weight(1));
        return r1;
    }

    private View playersCard() {
        LinearLayout c = card();
        c.addView(heading("המשתתפים"));
        c.addView(namesRow(), matchWrap());

        switch (r) {
            case Routines.NORMAL:
            case Routines.EXPERT: {
                LinearLayout r2 = row();
                r2.setPaddingRelative(0, dp(10), 0, 0);
                for (int i = 0; i < 3; i++) {
                    if (i > 0) r2.addView(spacer(10, 1));
                    String lab = "חפץ " + (i + 1) + (r == Routines.NORMAL && i == 2 ? " · מאולץ" : "");
                    r2.addView(field(lab, Prefs.k(r, "obj" + (i + 1)), Routines.object(prefs, r, i), null, false), weight(1));
                }
                c.addView(r2, matchWrap());
                break;
            }
            case Routines.EIGHT: {
                TextView l = label("שמונת החפצים");
                l.setPaddingRelative(0, dp(12), 0, dp(6));
                c.addView(l);
                for (int row = 0; row < 4; row++) {
                    LinearLayout rr = row();
                    rr.setPaddingRelative(0, row == 0 ? 0 : dp(8), 0, 0);
                    for (int col = 0; col < 2; col++) {
                        int i = row * 2 + col;
                        if (col == 1) rr.addView(spacer(8, 1));
                        rr.addView(numberedInput(i + 1, Routines.item(prefs, i), Prefs.k(r, "item" + (i + 1))), weight(1));
                    }
                    c.addView(rr, matchWrap());
                }
                break;
            }
            case Routines.DIE: {
                TextView l = label("כתיבת המספרים");
                l.setPaddingRelative(0, dp(12), 0, dp(6));
                c.addView(l);
                c.addView(segmented(new String[]{"ספרות", "מילים"}, new String[]{"4", "ארבע"},
                        Routines.dieWords(prefs) ? 1 : 0, pos -> {
                            prefs.edit().putBoolean(Prefs.k(r, "words"), pos == 1).apply();
                            refreshAll();
                        }), matchWrap());
                break;
            }
            case Routines.ESP: {
                TextView l = label("שמות הסמלים, כפי שיופיעו בתחזית");
                l.setPaddingRelative(0, dp(12), 0, dp(8));
                c.addView(l);
                LinearLayout rr = row();
                for (int i = 0; i < 5; i++) {
                    final int idx = i;
                    LinearLayout col = column();
                    col.setGravity(Gravity.CENTER_HORIZONTAL);
                    SymbolView sv = new SymbolView(this, i);
                    col.addView(sv, new LinearLayout.LayoutParams(dp(54), dp(62)));
                    EditText e = input(Routines.symbol(prefs, i), null, false,
                            s -> prefs.edit().putString(Prefs.k(r, "sym" + (idx + 1)), s).apply());
                    e.setTextSize(12);
                    e.setGravity(Gravity.CENTER);
                    e.setPaddingRelative(dp(2), dp(6), dp(2), dp(6));
                    e.setMinHeight(dp(40));
                    LinearLayout.LayoutParams elp = matchWrap(6);
                    col.addView(e, elp);
                    LinearLayout.LayoutParams clp = weight(1);
                    clp.setMargins(dp(3), 0, dp(3), 0);
                    rr.addView(col, clp);
                }
                c.addView(rr, matchWrap());
                break;
            }
            case Routines.COIN: {
                TextView l = label("מספר ההטלות");
                l.setPaddingRelative(0, dp(12), 0, dp(6));
                c.addView(l);
                c.addView(segmented(new String[]{"1", "2", "3"}, new String[]{"2 אפשרויות", "4 אפשרויות", "8 אפשרויות"},
                        Routines.flips(prefs) - 1, pos -> {
                            prefs.edit().putInt(Prefs.k(r, "flips"), pos + 1).apply();
                            main.post(this::rebuild);
                        }), matchWrap());
                LinearLayout rr = row();
                rr.setPaddingRelative(0, dp(12), 0, 0);
                rr.addView(field("המילה לעץ", Prefs.k(r, "heads"), Routines.heads(prefs), null, false), weight(1));
                rr.addView(spacer(10, 1));
                rr.addView(field("המילה לפלי", Prefs.k(r, "tails"), Routines.tails(prefs), null, false), weight(1));
                c.addView(rr, matchWrap());
                break;
            }
            default:
                break;
        }
        return c;
    }

    /** "1 [ Key ]" — a number badge inside the input box. */
    private View numberedInput(int n, String value, String key) {
        LinearLayout box = row();
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPaddingRelative(dp(10), 0, dp(4), 0);
        box.setBackground(rounded(BG, BORDER, dp(1), dp(10)));
        TextView num = serifText(String.valueOf(n), 13, ACCENT, true);
        num.setMinWidth(dp(16));
        box.addView(num);
        EditText e = input(value, null, false, s -> prefs.edit().putString(key, s).apply());
        e.setBackground(null);
        e.setPaddingRelative(dp(6), dp(8), dp(6), dp(8));
        box.addView(e, weight(1));
        return box;
    }

    // ---------------------------------------------------------------- prophecy

    private View prophecyCard() {
        LinearLayout c = card();
        if (r == Routines.EIGHT) return eightProphecy(c);
        if (r == Routines.COIN) return coinProphecy(c);

        c.addView(heading("הנבואה"));
        LinearLayout f = field("טקסט התחזית", Prefs.k(r, Routines.templateKey(prefs, r)),
                Routines.template(prefs, r),
                Routines.isCustom(r) ? "למשל: הצבע האהוב הערב יהיה {תוצאה}." : null, true);
        if (Routines.isCustom(r)) editOf(f).setMinLines(3);
        c.addView(f);
        c.addView(tagLegend(editOf(f), Routines.tags(prefs, r), Routines.tagHelp(prefs, r)));
        if (Routines.isCustom(r)) {
            c.addView(hint("השאירו ריק כדי לכתוב רק את התוצאה."));
        }
        return c;
    }

    private View eightProphecy(LinearLayout c) {
        boolean force = Routines.eightForce(prefs);
        c.addView(heading(force ? "הפורס המנטלי" : "הנבואה"));
        c.addView(segmented(new String[]{"נבואה", "פורס מנטלי"},
                new String[]{"„ידעתי מה תבחרו…”", "„גרמתי לכם לבחור…”"}, force ? 1 : 0, pos -> {
                    prefs.edit().putInt(Prefs.k(r, "pres"), pos).apply();
                    main.post(this::rebuild);
                }), matchWrap());

        if (force) {
            TextView l = label("אתם מצביעים על (נבחר מראש מתוך שמונת החפצים)");
            l.setPaddingRelative(0, dp(14), 0, dp(6));
            c.addView(l);
            Spinner sp = dropdown(pointedOptions(), Routines.pointed(prefs), pos -> {
                prefs.edit().putInt(Prefs.k(r, "pointed"), pos).apply();
                refreshAll();
            });
            c.addView(sp);
            refreshers.add(() -> {
                int sel = sp.getSelectedItemPosition();
                ((DarkAdapter) sp.getAdapter()).replace(pointedOptions());
                sp.setSelection(sel, false);
            });
            c.addView(hint("הרשימה מתעדכנת לפי שמות החפצים למעלה."));
        }

        LinearLayout f = field(force ? "טקסט הפורס" : "טקסט התחזית", Prefs.k(r, Routines.templateKey(prefs, r)),
                Routines.template(prefs, r), null, true);
        f.setPaddingRelative(0, dp(12), 0, 0);
        editOf(f).setMinLines(3);
        c.addView(f);
        EditText tpl = editOf(f);
        c.addView(tagLegend(tpl, Routines.tags(prefs, r), Routines.tagHelp(prefs, r)));

        if (force) {
            LinearLayout box = column();
            box.setPaddingRelative(dp(14), dp(12), dp(14), dp(12));
            box.setBackground(rounded(CHIP, 0, 0, dp(10)));
            box.setLayoutParams(matchWrap(10));
            TextView t = text("רעיונות לניסוח · הקישו לשימוש", 11, ACCENT);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setLetterSpacing(0.02f);
            box.addView(t);
            for (String idea : Routines.EIGHT_FLAVOR) {
                TextView it = text("“" + idea + "”", 12, TEXT);
                it.setLineSpacing(0, 1.2f);
                it.setPaddingRelative(0, dp(10), 0, dp(4));
                it.setOnClickListener(v -> tpl.setText(idea));
                box.addView(it);
            }
            c.addView(box);
        }
        return c;
    }

    private String[] pointedOptions() {
        String[] o = new String[8];
        for (int i = 0; i < 8; i++) o[i] = (i + 1) + " · " + Routines.item(prefs, i);
        return o;
    }

    private View coinProphecy(LinearLayout c) {
        int n = Routines.flips(prefs);
        c.addView(heading("הנבואה"));
        c.addView(subtitle("שורה אחת לכל הטלה. עם " + (n == 1 ? "הטלה אחת" : n + " הטלות") + " כותבים "
                + (n == 1 ? "שורה אחת" : n + " שורות") + ", ועוד שורת פתיחה לא חובה."));
        LinearLayout open = column();
        open.addView(label("שורת פתיחה"));
        EditText openEdit = input(Routines.coinOpening(prefs), "לא חובה", false,
                s -> prefs.edit().putString(Prefs.k(r, "open"), s).apply());
        open.addView(openEdit);
        c.addView(open);
        EditText first = openEdit;
        String[] names = {"הטלה 1", "הטלה 2", "הטלה 3"};
        for (int k = 1; k <= n; k++) {
            LinearLayout f = field(names[k - 1], Prefs.k(r, "line" + k), Routines.coinLine(prefs, k), null, false);
            f.setPaddingRelative(0, dp(10), 0, 0);
            c.addView(f);
        }
        c.addView(tagLegend(first, Routines.tags(prefs, r), Routines.tagHelp(prefs, r)));
        c.addView(hint("הקישו על תגית כדי להוסיף אותה לשורת הפתיחה, או הקלידו אותה בכל שורה."));
        return c;
    }

    // ---------------------------------------------------------------- reveal

    private View revealCard() {
        LinearLayout c = card();
        c.addView(heading("החשיפה"));

        if (r == Routines.NORMAL) {
            c.addView(subtitle("מי שמרים את הטלפון מקבל את חפץ 1, והשני את חפץ 2. חפץ 3 מאולץ."));
            c.addView(preview("הצופה מרים", () -> Routines.sentence(prefs, r, 0, "")));
            c.addView(preview("הקוסם מרים", () -> Routines.sentence(prefs, r, 1, "")));
            c.addView(hint("הכלל: מניחים הפוך, כשהתחתית מכוונת אליכם."));
            return c;
        }

        String howTo;
        switch (r) {
            case Routines.EXPERT: howTo = "הרימו את הצד המתאים. לגרסה עם עצירה, הרימו מעט, החזיקו בלי לזוז עד הרטט השקט, ואז סיימו."; break;
            case Routines.EIGHT: howTo = "לכל צד שתי גרסאות. הרמה ישרה = החפץ הראשון. הרמה קלה, המתנה לרטט השקט ואז המשך (או היפוך מהצד) = השני."; break;
            case Routines.DIE: howTo = "אותן שש הרמות כמו בחשיפת המומחה. הציצו בקובייה והרימו את הצד המתאים."; break;
            case Routines.ESP: howTo = "ספרו את הקווים בסמל והתקדמו עם כיוון השעון: 1 קדמי, 2 ימין, 3 אחורי, 4 שמאל, 5 שוב קדמי עם עצירה."; break;
            default: howTo = null;
        }
        if (howTo != null) c.addView(subtitle(howTo));
        if (r == Routines.COIN) c.addView(coinMemoryRule());

        c.addView(diagram(slot -> Routines.cellLabel(prefs, r, slot)));
        if (r != Routines.COIN || Routines.flips(prefs) == 3) {
            View pc = pauseControl(r);
            pc.setPaddingRelative(0, dp(8), 0, 0);
            c.addView(pc);
        }

        int ex = Routines.exampleSlot(prefs, r);
        if (ex >= 0) {
            c.addView(preview("דוגמה · " + Routines.SLOT_NAMES[ex].toUpperCase(),
                    () -> Routines.sentence(prefs, r, Routines.outcomeFor(prefs, r, ex), "")));
        } else if (r == Routines.EXPERT) {
            int[] order = {Routines.FRONT, Routines.FRONT_P, Routines.BACK, Routines.BACK_P, Routines.RIGHT, Routines.LEFT};
            for (int s : order) {
                c.addView(preview(Routines.SLOT_NAMES[s].toUpperCase(),
                        () -> Routines.sentence(prefs, r, Routines.outcomeFor(prefs, r, s), "")));
            }
        }
        return c;
    }

    private View coinMemoryRule() {
        int n = Routines.flips(prefs);
        LinearLayout box = column();
        box.setPaddingRelative(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(rounded(CHIP, 0, 0, dp(12)));
        box.setLayoutParams(matchWrap(0));
        TextView t = text("קל לזכור", 11, ACCENT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.02f);
        box.addView(t);
        String[] lines;
        if (n == 1) lines = new String[]{"עץ = קדמי · פלי = אחורי"};
        else if (n == 2) lines = new String[]{"הטלה 1 · עץ = קדמי או אחורי · פלי = צד",
                "הטלה 2 · עץ = קדמי / שמאל · פלי = אחורי / ימין"};
        else lines = new String[]{"הטלה 1 · עץ = קדמי או אחורי · פלי = צד",
                "הטלה 2 · עץ = קדמי / שמאל · פלי = אחורי / ימין",
                "הטלה 3 · עץ = הרמה ישרה · פלי = עצירה"};
        for (String s : lines) {
            TextView l = text(s, 13, TEXT);
            l.setPaddingRelative(0, dp(8), 0, 0);
            box.addView(l);
        }
        LinearLayout wrap = column();
        wrap.setPaddingRelative(0, 0, 0, dp(10));
        wrap.addView(box);
        return wrap;
    }

    interface Sentence { String get(); }

    private View preview(String title, Sentence s) {
        LinearLayout box = column();
        box.setPaddingRelative(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(rounded(BG, BORDER, dp(1), dp(10)));
        box.setLayoutParams(matchWrap(10));
        TextView label = text(title, 11, MUTED);
        label.setLetterSpacing(0.02f);
        box.addView(label);
        TextView pv = serifText("", 14, TEXT, false);
        pv.setLineSpacing(0, 1.3f);
        pv.setPaddingRelative(0, dp(6), 0, 0);
        box.addView(pv);
        Runnable fill = () -> pv.setText(s.get());
        fill.run();
        refreshers.add(fill);
        return box;
    }

    // ---------------------------------------------------------------- Build It Yourself

    private View customRoutineCard() {
        LinearLayout c = card();
        c.addView(heading("הרוטינה"));
        c.addView(field("שם הרוטינה", Prefs.k(r, "name"), Prefs.raw(prefs, r, "name", ""), "תנו שם לרוטינה", false));
        LinearLayout names = namesRow();
        names.setPaddingRelative(0, dp(10), 0, 0);
        c.addView(names, matchWrap());
        c.addView(hint("שני השמות זמינים כתגיות בנבואה."));
        return c;
    }

    private View customOutcomesCard() {
        int count = Routines.customCount(prefs, r);
        LinearLayout c = card();
        TextView counter = text(count + " מתוך " + Routines.MAX_OUTCOMES, 12, ACCENT);
        counter.setTypeface(Typeface.DEFAULT_BOLD);
        c.addView(heading("התוצאות", CRIMSON, counter));
        c.addView(subtitle("כל תוצאה ממלאת את {תוצאה} בנבואה. בחרו את ההרמה שכותבת אותה."));

        for (int i = 0; i < count; i++) {
            final int idx = i;
            LinearLayout rr = row();
            rr.setGravity(Gravity.CENTER_VERTICAL);
            rr.setPaddingRelative(0, i == 0 ? 0 : dp(8), 0, 0);
            TextView num = serifText(String.valueOf(i + 1), 14, ACCENT, true);
            num.setGravity(Gravity.CENTER);
            rr.addView(num, new LinearLayout.LayoutParams(dp(22), ViewGroup.LayoutParams.WRAP_CONTENT));
            EditText e = input(Routines.customOutcome(prefs, r, i), "תוצאה " + (i + 1), false,
                    s -> prefs.edit().putString(Prefs.k(r, "out" + idx), s).apply());
            LinearLayout.LayoutParams elp = weight(1);
            elp.setMarginStart(dp(6));
            elp.setMarginEnd(dp(8));
            rr.addView(e, elp);
            Spinner sp = dropdown(SHORT_SLOTS, Routines.customSlot(prefs, r, i), pos -> {
                Routines.setCustomSlot(prefs, r, idx, pos);
                main.post(this::rebuild);
            });
            sp.setLayoutParams(new LinearLayout.LayoutParams(dp(132), dp(48)));
            rr.addView(sp);
            if (count > 2) {
                TextView x = text("×", 22, MUTED);
                x.setGravity(Gravity.CENTER);
                x.setContentDescription("הסרת תוצאה " + (i + 1));
                x.setOnClickListener(v -> {
                    Routines.removeCustomOutcome(prefs, r, idx);
                    main.post(this::rebuild);
                });
                rr.addView(x, new LinearLayout.LayoutParams(dp(36), dp(48)));
            }
            c.addView(rr, matchWrap());
        }

        if (count < Routines.MAX_OUTCOMES) {
            TextView add = text("+  הוספת תוצאה", 13, ACCENT);
            add.setTypeface(Typeface.DEFAULT_BOLD);
            add.setLetterSpacing(0.02f);
            add.setGravity(Gravity.CENTER);
            add.setMinHeight(dp(48));
            add.setBackground(dashed(BORDER_STRONG, dp(10)));
            add.setLayoutParams(matchWrap(12));
            add.setOnClickListener(v -> {
                Routines.addCustomOutcome(prefs, r);
                main.post(this::rebuild);
            });
            c.addView(add);
        }

        View d = diagram(slot -> Routines.cellLabel(prefs, r, slot));
        ((LinearLayout.LayoutParams) d.getLayoutParams()).topMargin = dp(14);
        c.addView(d);
        View pc = pauseControl(r);
        pc.setPaddingRelative(0, dp(8), 0, 0);
        c.addView(pc);
        return c;
    }

    private static final String[] SHORT_SLOTS = {
            "קדמי", "קדמי ‖", "אחורי", "אחורי ‖", "שמאל", "שמאל ‖", "ימין", "ימין ‖"};

    private View ideasCard() {
        LinearLayout c = card();
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setGradientType(android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT);
        g.setGradientCenter(0.5f, 0f);
        g.setGradientRadius(dp(300));
        g.setColors(new int[]{0xFF3B0B14, CARD});
        g.setCornerRadius(dp(14));
        g.setStroke(dp(1), BORDER_STRONG);
        c.setBackground(g);
        c.addView(heading("רעיונות", GOLD, null));
        c.addView(subtitle("לא חובה. שום דבר לא מתמלא עד שמקישים על רעיון; אז הוא מגדיר תוצאות ונבואה שאפשר לערוך."));
        String inUse = Routines.ideaInUse(prefs, r);
        for (Routines.Idea idea : Routines.IDEAS) {
            boolean on = idea.name.equals(inUse);
            LinearLayout box = column();
            box.setPaddingRelative(dp(14), dp(12), dp(14), dp(12));
            box.setBackground(on ? rounded(0xFF2A1217, CRIMSON, dp(2), dp(10)) : rounded(BG, BORDER, dp(1), dp(10)));
            box.setLayoutParams(matchWrap(10));
            box.setClickable(true);
            box.setFocusable(true);
            LinearLayout titleRow = row();
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            titleRow.addView(serifText(idea.name, 14, TEXT, true), weight(1));
            if (on) {
                TextView badge = text("בשימוש", 10, 0xFFFFFFFF);
                badge.setTypeface(Typeface.DEFAULT_BOLD);
                badge.setLetterSpacing(0.02f);
                badge.setPaddingRelative(dp(8), dp(3), dp(8), dp(3));
                badge.setBackground(rounded(CRIMSON, 0, 0, dp(999)));
                titleRow.addView(badge);
            }
            box.addView(titleRow, matchWrap());
            TextView d = text(idea.description, 12, MUTED);
            d.setLineSpacing(0, 1.2f);
            d.setPaddingRelative(0, dp(4), 0, 0);
            box.addView(d);
            box.setOnClickListener(v -> useIdea(idea));
            c.addView(box);
        }
        return c;
    }

    private void useIdea(Routines.Idea idea) {
        boolean hasWork = !Routines.template(prefs, r).trim().isEmpty();
        for (int i = 0; i < Routines.customCount(prefs, r); i++) {
            if (!Routines.customOutcome(prefs, r, i).trim().isEmpty()) hasWork = true;
        }
        if (!hasWork) {
            Routines.applyIdea(prefs, r, idea);
            rebuild();
            return;
        }
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("להשתמש ב„" + idea.name + "”?")
                .setMessage("פעולה זו מחליפה את התוצאות והנבואה שלכם.")
                .setPositiveButton("החלפה", (d, w) -> { Routines.applyIdea(prefs, r, idea); rebuild(); })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // ---------------------------------------------------------------- delete

    private void confirmDelete() {
        Dialog dlg = new Dialog(this);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout c = column();
        c.setPaddingRelative(dp(20), dp(24), dp(20), dp(16));
        c.setBackground(rounded(CARD, BORDER_STRONG, dp(1), dp(18)));

        TextView t = serifText("למחוק את „" + Routines.name(prefs, r) + "”?", 18, TEXT, true);
        c.addView(t);
        List<int[]> plan = Routines.renumberPlan(prefs, r);
        TextView m = text(plan.isEmpty()
                ? "רוטינה " + Routines.roman(r) + " תימחק. אי אפשר לבטל את זה."
                : "רוטינה " + Routines.roman(r) + " תימחק. מה יקרה למספרים שאחריה?", 13, MUTED);
        m.setLineSpacing(0, 1.25f);
        m.setPaddingRelative(0, dp(10), 0, dp(6));
        c.addView(m);

        if (plan.isEmpty()) {
            // Nothing to renumber: a plain confirm with Cancel and Delete side by side.
            LinearLayout buttons = row();
            buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            buttons.setPaddingRelative(0, dp(14), 0, 0);
            TextView no = text("ביטול", 14, MUTED);
            no.setGravity(Gravity.CENTER);
            no.setMinHeight(dp(44));
            no.setPaddingRelative(dp(16), 0, dp(16), 0);
            no.setOnClickListener(v -> dlg.dismiss());
            buttons.addView(no);
            TextView yes = text("מחיקה", 14, 0xFFFFFFFF);
            yes.setTypeface(Typeface.DEFAULT_BOLD);
            yes.setGravity(Gravity.CENTER);
            yes.setMinHeight(dp(44));
            yes.setPaddingRelative(dp(22), 0, dp(22), 0);
            yes.setBackground(rounded(CRIMSON, 0, 0, dp(10)));
            yes.setOnClickListener(v -> {
                Routines.deleteCustom(prefs, r, false);
                dlg.dismiss();
                finish();
            });
            LinearLayout.LayoutParams ylp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ylp.setMarginStart(dp(8));
            buttons.addView(yes, ylp);
            c.addView(buttons, matchWrap());
        } else {
            c.addView(dialogOption("שמירת המספרים",
                    "שום דבר אחר לא משתנה. המספר " + r + " יישאר פנוי עד שתבנו רוטינה חדשה.", null, () -> {
                        Routines.deleteCustom(prefs, r, false);
                        dlg.dismiss();
                        finish();
                    }));
            StringBuilder sb = new StringBuilder();
            for (int[] mv : plan) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(mv[0]).append(" ← ").append(mv[1]);
            }
            c.addView(dialogOption("מספור מחדש", "סגירת כל הפערים:", sb.toString(), () -> {
                Routines.deleteCustom(prefs, r, true);
                dlg.dismiss();
                finish();
            }));
        }
        if (!plan.isEmpty()) {
            TextView cancel = text("ביטול", 14, MUTED);
            cancel.setGravity(Gravity.CENTER);
            cancel.setMinHeight(dp(44));
            cancel.setOnClickListener(v -> dlg.dismiss());
            c.addView(cancel, matchWrap(6));
        }

        dlg.setContentView(c);
        Window w = dlg.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0x00000000));
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dlg.show();
    }

    private View dialogOption(String title, String desc, String extra, Runnable action) {
        LinearLayout b = column();
        b.setPaddingRelative(dp(14), dp(14), dp(14), dp(14));
        b.setBackground(rounded(BG, BORDER_STRONG, dp(1), dp(12)));
        b.setLayoutParams(matchWrap(10));
        b.setClickable(true);
        b.setFocusable(true);
        TextView t = text(title, 14, TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        b.addView(t);
        TextView d = text(desc, 12, MUTED);
        d.setLineSpacing(0, 1.2f);
        d.setPaddingRelative(0, dp(4), 0, 0);
        b.addView(d);
        if (extra != null) {
            TextView e = text(extra, 12, ACCENT);
            e.setTypeface(Typeface.DEFAULT_BOLD);
            e.setPaddingRelative(0, dp(4), 0, 0);
            b.addView(e);
        }
        b.setOnClickListener(v -> action.run());
        return b;
    }
}
