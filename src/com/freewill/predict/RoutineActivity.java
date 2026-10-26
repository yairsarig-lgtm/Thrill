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
        body.setPadding(dp(16), dp(8), dp(16), dp(40));
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
            TextView del = outlineButton("Delete this routine");
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

        TextView back = text("‹", 30, TEXT);
        back.setGravity(Gravity.CENTER);
        back.setPadding(0, 0, dp(2), dp(4));
        back.setBackground(rounded(0x990B0809, 0, 0, dp(22)));
        back.setContentDescription("Back to routines");
        back.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP | Gravity.START);
        blp.setMargins(dp(12), dp(14), 0, 0);
        f.addView(back, blp);

        LinearLayout t = column();
        t.setPadding(0, 0, 0, dp(20));
        TextView num = serifText(Routines.roman(r), 60, ACCENT, true);
        num.setShadowLayer(dp(14), 0, dp(4), 0x80C42A3A);
        centered(num, 0f);
        t.addView(num);
        TextView title = serifText("", 22, TEXT, true);
        centered(title, 0.12f);
        title.setPadding(title.getPaddingLeft(), dp(8), 0, 0);
        t.addView(title);
        Runnable setTitle = () -> title.setText(Routines.name(prefs, r).toUpperCase());
        setTitle.run();
        refreshers.add(setTitle);

        TextView code = text(r == Routines.NORMAL ? "Type the name, or the name + 1"
                : (Routines.isCustom(r) ? "Build It Yourself · type the name + " : "Type the name + ") + r, 12, ACCENT);
        code.setTypeface(Typeface.DEFAULT_BOLD);
        code.setPadding(dp(10), dp(5), dp(10), dp(5));
        code.setBackground(rounded(0xE62A1217, 0, 0, dp(6)));
        LinearLayout codeRow = row();
        codeRow.setGravity(Gravity.CENTER);
        codeRow.setPadding(0, dp(8), 0, 0);
        codeRow.addView(code);
        t.addView(codeRow, matchWrap());

        f.addView(t, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        return f;
    }

    // ---------------------------------------------------------------- players

    private LinearLayout namesRow() {
        LinearLayout r1 = row();
        r1.addView(field("Magician", Prefs.k(r, "magician"), Prefs.magician(prefs, r), null, false), weight(1));
        r1.addView(spacer(10, 1));
        r1.addView(field("Spectator if none typed", Prefs.k(r, "spectator"), Prefs.spectatorFallback(prefs, r), null, false), weight(1));
        return r1;
    }

    private View playersCard() {
        LinearLayout c = card();
        c.addView(heading("THE PLAYERS"));
        c.addView(namesRow(), matchWrap());

        switch (r) {
            case Routines.NORMAL:
            case Routines.EXPERT: {
                LinearLayout r2 = row();
                r2.setPadding(0, dp(10), 0, 0);
                for (int i = 0; i < 3; i++) {
                    if (i > 0) r2.addView(spacer(10, 1));
                    String lab = "Object " + (i + 1) + (r == Routines.NORMAL && i == 2 ? " · forced" : "");
                    r2.addView(field(lab, Prefs.k(r, "obj" + (i + 1)), Routines.object(prefs, r, i), null, false), weight(1));
                }
                c.addView(r2, matchWrap());
                break;
            }
            case Routines.EIGHT: {
                TextView l = label("The eight items");
                l.setPadding(0, dp(12), 0, dp(6));
                c.addView(l);
                for (int row = 0; row < 4; row++) {
                    LinearLayout rr = row();
                    rr.setPadding(0, row == 0 ? 0 : dp(8), 0, 0);
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
                TextView l = label("Write numbers as");
                l.setPadding(0, dp(12), 0, dp(6));
                c.addView(l);
                c.addView(segmented(new String[]{"Digits", "Words"}, new String[]{"4", "four"},
                        Routines.dieWords(prefs) ? 1 : 0, pos -> {
                            prefs.edit().putBoolean(Prefs.k(r, "words"), pos == 1).apply();
                            refreshAll();
                        }), matchWrap());
                break;
            }
            case Routines.ESP: {
                TextView l = label("Symbol names, as they appear in the prediction");
                l.setPadding(0, dp(12), 0, dp(8));
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
                    e.setPadding(dp(2), dp(6), dp(2), dp(6));
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
                TextView l = label("Number of flips");
                l.setPadding(0, dp(12), 0, dp(6));
                c.addView(l);
                c.addView(segmented(new String[]{"1", "2", "3"}, new String[]{"2 outs", "4 outs", "8 outs"},
                        Routines.flips(prefs) - 1, pos -> {
                            prefs.edit().putInt(Prefs.k(r, "flips"), pos + 1).apply();
                            main.post(this::rebuild);
                        }), matchWrap());
                LinearLayout rr = row();
                rr.setPadding(0, dp(12), 0, 0);
                rr.addView(field("Word for heads", Prefs.k(r, "heads"), Routines.heads(prefs), null, false), weight(1));
                rr.addView(spacer(10, 1));
                rr.addView(field("Word for tails", Prefs.k(r, "tails"), Routines.tails(prefs), null, false), weight(1));
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
        box.setPadding(dp(10), 0, dp(4), 0);
        box.setBackground(rounded(BG, BORDER, dp(1), dp(10)));
        TextView num = serifText(String.valueOf(n), 13, ACCENT, true);
        num.setMinWidth(dp(16));
        box.addView(num);
        EditText e = input(value, null, false, s -> prefs.edit().putString(key, s).apply());
        e.setBackground(null);
        e.setPadding(dp(6), dp(8), dp(6), dp(8));
        box.addView(e, weight(1));
        return box;
    }

    // ---------------------------------------------------------------- prophecy

    private View prophecyCard() {
        LinearLayout c = card();
        if (r == Routines.EIGHT) return eightProphecy(c);
        if (r == Routines.COIN) return coinProphecy(c);

        c.addView(heading("THE PROPHECY"));
        LinearLayout f = field("Prediction text", Prefs.k(r, Routines.templateKey(prefs, r)),
                Routines.template(prefs, r),
                Routines.isCustom(r) ? "e.g. Tonight's favourite colour will be {outcome}." : null, true);
        if (Routines.isCustom(r)) editOf(f).setMinLines(3);
        c.addView(f);
        c.addView(tagLegend(editOf(f), Routines.tags(prefs, r), Routines.tagHelp(prefs, r)));
        if (Routines.isCustom(r)) {
            c.addView(hint("Leave it empty to write just the outcome."));
        }
        return c;
    }

    private View eightProphecy(LinearLayout c) {
        boolean force = Routines.eightForce(prefs);
        c.addView(heading(force ? "THE MENTAL FORCE" : "THE PROPHECY"));
        c.addView(segmented(new String[]{"Prophecy", "Mental Force"},
                new String[]{"“I knew you'd choose…”", "“I made you choose…”"}, force ? 1 : 0, pos -> {
                    prefs.edit().putInt(Prefs.k(r, "pres"), pos).apply();
                    main.post(this::rebuild);
                }), matchWrap());

        if (force) {
            TextView l = label("You point at (chosen in advance from the eight items)");
            l.setPadding(0, dp(14), 0, dp(6));
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
            c.addView(hint("The list follows the item names above."));
        }

        LinearLayout f = field(force ? "Force text" : "Prediction text", Prefs.k(r, Routines.templateKey(prefs, r)),
                Routines.template(prefs, r), null, true);
        f.setPadding(0, dp(12), 0, 0);
        editOf(f).setMinLines(3);
        c.addView(f);
        EditText tpl = editOf(f);
        c.addView(tagLegend(tpl, Routines.tags(prefs, r), Routines.tagHelp(prefs, r)));

        if (force) {
            LinearLayout box = column();
            box.setPadding(dp(14), dp(12), dp(14), dp(12));
            box.setBackground(rounded(CHIP, 0, 0, dp(10)));
            box.setLayoutParams(matchWrap(10));
            TextView t = text("FLAVOR IDEAS · TAP TO USE", 11, ACCENT);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setLetterSpacing(0.12f);
            box.addView(t);
            for (String idea : Routines.EIGHT_FLAVOR) {
                TextView it = text("“" + idea + "”", 12, TEXT);
                it.setLineSpacing(0, 1.2f);
                it.setPadding(0, dp(10), 0, dp(4));
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
        c.addView(heading("THE PROPHECY"));
        c.addView(subtitle("One line per flip. With " + n + (n == 1 ? " flip" : " flips") + " you write "
                + (n == 1 ? "one line" : n + " lines") + ", plus an optional opening."));
        LinearLayout open = column();
        open.addView(label("Opening line"));
        EditText openEdit = input(Routines.coinOpening(prefs), "Optional", false,
                s -> prefs.edit().putString(Prefs.k(r, "open"), s).apply());
        open.addView(openEdit);
        c.addView(open);
        EditText first = openEdit;
        String[] names = {"Flip 1", "Flip 2", "Flip 3"};
        for (int k = 1; k <= n; k++) {
            LinearLayout f = field(names[k - 1], Prefs.k(r, "line" + k), Routines.coinLine(prefs, k), null, false);
            f.setPadding(0, dp(10), 0, 0);
            c.addView(f);
        }
        c.addView(tagLegend(first, Routines.tags(prefs, r), Routines.tagHelp(prefs, r)));
        c.addView(hint("Tap a tag to insert it into the opening line, or type it into any line."));
        return c;
    }

    // ---------------------------------------------------------------- reveal

    private View revealCard() {
        LinearLayout c = card();
        c.addView(heading("THE REVEAL"));

        if (r == Routines.NORMAL) {
            c.addView(subtitle("Whoever picks the phone up gets Object 1, the other person Object 2. Object 3 is forced."));
            c.addView(preview("SPECTATOR PICKS IT UP", () -> Routines.sentence(prefs, r, 0, "")));
            c.addView(preview("MAGICIAN PICKS IT UP", () -> Routines.sentence(prefs, r, 1, "")));
            c.addView(hint("Rule: lay it face down, bottom pointing at you."));
            return c;
        }

        String howTo;
        switch (r) {
            case Routines.EXPERT: howTo = "Lift the edge that matches. For a paused version, lift slightly, hold still until the silent tap, then finish."; break;
            case Routines.EIGHT: howTo = "Every edge has two versions. Straight lift = the first item. Lift slightly, wait for the silent tap, then continue (or flip it over from the side) = the second."; break;
            case Routines.DIE: howTo = "Same six pickups as the Expert Reveal. Glance at the die, then lift the matching edge."; break;
            case Routines.ESP: howTo = "Count the lines in the symbol and go round clockwise: 1 front, 2 right, 3 back, 4 left, 5 front again with a pause."; break;
            default: howTo = null;
        }
        if (howTo != null) c.addView(subtitle(howTo));
        if (r == Routines.COIN) c.addView(coinMemoryRule());

        c.addView(diagram(slot -> Routines.cellLabel(prefs, r, slot)));
        if (r != Routines.COIN || Routines.flips(prefs) == 3) {
            View pc = pauseControl(r);
            pc.setPadding(0, dp(8), 0, 0);
            c.addView(pc);
        }

        int ex = Routines.exampleSlot(prefs, r);
        if (ex >= 0) {
            c.addView(preview("EXAMPLE · " + Routines.SLOT_NAMES[ex].toUpperCase(),
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
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(rounded(CHIP, 0, 0, dp(12)));
        box.setLayoutParams(matchWrap(0));
        TextView t = text("EASY TO REMEMBER", 11, ACCENT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.15f);
        box.addView(t);
        String[] lines;
        if (n == 1) lines = new String[]{"Heads = front · Tails = back"};
        else if (n == 2) lines = new String[]{"1st flip · Heads = front or back · Tails = a side",
                "2nd flip · Heads = front / left · Tails = back / right"};
        else lines = new String[]{"1st flip · Heads = front or back · Tails = a side",
                "2nd flip · Heads = front / left · Tails = back / right",
                "3rd flip · Heads = straight lift · Tails = pause"};
        for (String s : lines) {
            TextView l = text(s, 13, TEXT);
            l.setPadding(0, dp(8), 0, 0);
            box.addView(l);
        }
        LinearLayout wrap = column();
        wrap.setPadding(0, 0, 0, dp(10));
        wrap.addView(box);
        return wrap;
    }

    interface Sentence { String get(); }

    private View preview(String title, Sentence s) {
        LinearLayout box = column();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(rounded(BG, BORDER, dp(1), dp(10)));
        box.setLayoutParams(matchWrap(10));
        TextView label = text(title, 11, MUTED);
        label.setLetterSpacing(0.12f);
        box.addView(label);
        TextView pv = serifText("", 14, TEXT, false);
        pv.setLineSpacing(0, 1.3f);
        pv.setPadding(0, dp(6), 0, 0);
        box.addView(pv);
        Runnable fill = () -> pv.setText(s.get());
        fill.run();
        refreshers.add(fill);
        return box;
    }

    // ---------------------------------------------------------------- Build It Yourself

    private View customRoutineCard() {
        LinearLayout c = card();
        c.addView(heading("THE ROUTINE"));
        c.addView(field("Routine name", Prefs.k(r, "name"), Prefs.raw(prefs, r, "name", ""), "Name your routine", false));
        LinearLayout names = namesRow();
        names.setPadding(0, dp(10), 0, 0);
        c.addView(names, matchWrap());
        c.addView(hint("Both names are available as tags in your prophecy."));
        return c;
    }

    private View customOutcomesCard() {
        int count = Routines.customCount(prefs, r);
        LinearLayout c = card();
        TextView counter = text(count + " of " + Routines.MAX_OUTCOMES, 12, ACCENT);
        counter.setTypeface(Typeface.DEFAULT_BOLD);
        c.addView(heading("THE OUTCOMES", CRIMSON, counter));
        c.addView(subtitle("Each outcome fills {outcome} in the prophecy. Pick the pickup that writes it."));

        for (int i = 0; i < count; i++) {
            final int idx = i;
            LinearLayout rr = row();
            rr.setGravity(Gravity.CENTER_VERTICAL);
            rr.setPadding(0, i == 0 ? 0 : dp(8), 0, 0);
            TextView num = serifText(String.valueOf(i + 1), 14, ACCENT, true);
            num.setGravity(Gravity.CENTER);
            rr.addView(num, new LinearLayout.LayoutParams(dp(22), ViewGroup.LayoutParams.WRAP_CONTENT));
            EditText e = input(Routines.customOutcome(prefs, r, i), "Outcome " + (i + 1), false,
                    s -> prefs.edit().putString(Prefs.k(r, "out" + idx), s).apply());
            LinearLayout.LayoutParams elp = weight(1);
            elp.setMargins(dp(6), 0, dp(8), 0);
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
                x.setContentDescription("Remove outcome " + (i + 1));
                x.setOnClickListener(v -> {
                    Routines.removeCustomOutcome(prefs, r, idx);
                    main.post(this::rebuild);
                });
                rr.addView(x, new LinearLayout.LayoutParams(dp(36), dp(48)));
            }
            c.addView(rr, matchWrap());
        }

        if (count < Routines.MAX_OUTCOMES) {
            TextView add = text("+  ADD OUTCOME", 13, ACCENT);
            add.setTypeface(Typeface.DEFAULT_BOLD);
            add.setLetterSpacing(0.08f);
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
        pc.setPadding(0, dp(8), 0, 0);
        c.addView(pc);
        return c;
    }

    private static final String[] SHORT_SLOTS = {
            "Front", "Front ‖", "Back", "Back ‖", "Left", "Left ‖", "Right", "Right ‖"};

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
        c.addView(heading("IDEAS", GOLD, null));
        c.addView(subtitle("Optional. Nothing is filled in until you tap an idea; then it sets up the outcomes and prophecy for you to edit."));
        for (Routines.Idea idea : Routines.IDEAS) {
            LinearLayout box = column();
            box.setPadding(dp(14), dp(12), dp(14), dp(12));
            box.setBackground(rounded(BG, BORDER, dp(1), dp(10)));
            box.setLayoutParams(matchWrap(10));
            box.setClickable(true);
            box.setFocusable(true);
            box.addView(serifText(idea.name, 14, TEXT, true));
            TextView d = text(idea.description, 12, MUTED);
            d.setLineSpacing(0, 1.2f);
            d.setPadding(0, dp(4), 0, 0);
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
                .setTitle("Use “" + idea.name + "”?")
                .setMessage("This replaces your outcomes and prophecy.")
                .setPositiveButton("Replace", (d, w) -> { Routines.applyIdea(prefs, r, idea); rebuild(); })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------------------------------------------------------- delete

    private void confirmDelete() {
        Dialog dlg = new Dialog(this);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout c = column();
        c.setPadding(dp(20), dp(24), dp(20), dp(16));
        c.setBackground(rounded(CARD, BORDER_STRONG, dp(1), dp(18)));

        TextView t = serifText("Delete “" + Routines.name(prefs, r) + "”?", 18, TEXT, true);
        c.addView(t);
        List<int[]> plan = Routines.renumberPlan(prefs, r);
        TextView m = text(plan.isEmpty()
                ? "Routine " + Routines.roman(r) + " will be removed."
                : "Routine " + Routines.roman(r) + " will be removed. What should happen to the numbers after it?", 13, MUTED);
        m.setLineSpacing(0, 1.25f);
        m.setPadding(0, dp(10), 0, dp(6));
        c.addView(m);

        if (plan.isEmpty()) {
            c.addView(dialogOption("Delete", "No other routine changes number.", null, () -> {
                Routines.deleteCustom(prefs, r, false);
                dlg.dismiss();
                finish();
            }));
        } else {
            c.addView(dialogOption("Keep numbers",
                    "Nothing else changes. " + r + " stays empty until you build a new routine.", null, () -> {
                        Routines.deleteCustom(prefs, r, false);
                        dlg.dismiss();
                        finish();
                    }));
            StringBuilder sb = new StringBuilder();
            for (int[] mv : plan) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(mv[0]).append(" → ").append(mv[1]);
            }
            c.addView(dialogOption("Renumber", "Close all gaps:", sb.toString(), () -> {
                Routines.deleteCustom(prefs, r, true);
                dlg.dismiss();
                finish();
            }));
        }
        TextView cancel = text("Cancel", 14, MUTED);
        cancel.setGravity(Gravity.CENTER);
        cancel.setMinHeight(dp(44));
        cancel.setOnClickListener(v -> dlg.dismiss());
        c.addView(cancel, matchWrap(6));

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
        b.setPadding(dp(14), dp(14), dp(14), dp(14));
        b.setBackground(rounded(BG, BORDER_STRONG, dp(1), dp(12)));
        b.setLayoutParams(matchWrap(10));
        b.setClickable(true);
        b.setFocusable(true);
        TextView t = text(title, 14, TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        b.addView(t);
        TextView d = text(desc, 12, MUTED);
        d.setLineSpacing(0, 1.2f);
        d.setPadding(0, dp(4), 0, 0);
        b.addView(d);
        if (extra != null) {
            TextView e = text(extra, 12, ACCENT);
            e.setTypeface(Typeface.DEFAULT_BOLD);
            e.setPadding(0, dp(4), 0, 0);
            b.addView(e);
        }
        b.setOnClickListener(v -> action.run());
        return b;
    }
}
