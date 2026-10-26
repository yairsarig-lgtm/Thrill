package com.freewill.predict;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Home: keyboard status, the routine deck, rehearsal for any routine, credits. */
public class HomeActivity extends BaseActivity {

    private View statusDiamond;
    private TextView statusTitle, statusSub, statusButton;
    private LinearLayout deck;
    private RehearsalCard rehearsal;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        LinearLayout page = column();
        scroll.addView(page);

        page.addView(hero());

        LinearLayout body = column();
        body.setPadding(dp(16), dp(4), dp(16), 0);
        page.addView(body, matchWrap());

        body.addView(statusCard());

        View h = heading("THE ROUTINES");
        ((LinearLayout) h).setPadding(0, dp(26), 0, dp(8));
        body.addView(h);
        TextView sub = text("Type the spectator's name, then the routine's number. No number runs routine I.", 12, MUTED);
        sub.setLineSpacing(0, 1.2f);
        body.addView(sub);

        deck = column();
        body.addView(deck, matchWrap());

        rehearsal = new RehearsalCard(this, Routines.NORMAL, true);
        body.addView(rehearsal.build(null));

        page.addView(credits(), matchWrap());

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = isKeyboardEnabled();
        ((GradientDrawable) statusDiamond.getBackground()).setColor(on ? GREEN : CRIMSON);
        statusTitle.setText(on ? "Keyboard enabled" : "Keyboard not enabled");
        statusSub.setText(on ? "Listed as “Thrill” in your keyboard picker"
                : "Turn on “Thrill” in your keyboard settings. Android warns about every keyboard; this one sends nothing.");
        statusButton.setText(on ? "Switch" : "Enable");
        rebuildDeck();
        rehearsal.rebuildPicker();
        rehearsal.start();
    }

    @Override
    protected void onPause() {
        rehearsal.stop();
        super.onPause();
    }

    // ---------------------------------------------------------------- header

    private View hero() {
        FrameLayout f = new FrameLayout(this);
        int h = dp(330);
        f.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));

        ImageView bg = new ImageView(this);
        bg.setImageResource(R.drawable.hero_bg);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        f.addView(bg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));

        ImageView emblem = new ImageView(this);
        emblem.setImageResource(R.drawable.hero_emblem);
        emblem.setContentDescription("Thrill emblem");
        FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(dp(220), dp(220),
                Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        elp.topMargin = dp(12);
        f.addView(emblem, elp);

        LinearLayout titles = column();
        titles.setPadding(0, 0, 0, dp(22));
        TextView name = serifText("THRILL", 34, TEXT, true);
        name.setShadowLayer(dp(10), 0, 0, 0xCC000000);
        centered(name, 0.35f);
        titles.addView(name);
        TextView sub = text("PREDICTION KEYBOARD · " + VERSION.toUpperCase(), 11, MUTED);
        centered(sub, 0.25f);
        sub.setPadding(sub.getPaddingLeft(), dp(4), 0, 0);
        titles.addView(sub);
        f.addView(titles, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        return f;
    }

    private View statusCard() {
        LinearLayout c = row();
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(16), dp(12), dp(12), dp(12));
        c.setBackground(rounded(CARD, BORDER, dp(1), dp(14)));
        c.setLayoutParams(matchWrap(6));

        statusDiamond = diamond(GREEN, 10);
        c.addView(statusDiamond);

        LinearLayout t = column();
        t.setPadding(dp(12), 0, dp(8), 0);
        statusTitle = text("", 14, TEXT);
        statusTitle.setTypeface(Typeface.DEFAULT_BOLD);
        statusSub = text("", 12, MUTED);
        t.addView(statusTitle);
        t.addView(statusSub);
        c.addView(t, weight(1));

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

    private boolean isKeyboardEnabled() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return false;
        for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
            if (i.getPackageName().equals(getPackageName())) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- the deck

    private void rebuildDeck() {
        deck.removeAllViews();
        for (int r : Routines.all(prefs)) deck.addView(routineCard(r));
        deck.addView(buildCard());
    }

    private View routineCard(int r) {
        LinearLayout c = row();
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(16), dp(14), dp(12), dp(14));
        c.setBackground(rounded(CARD, BORDER, dp(1), dp(14)));
        c.setLayoutParams(matchWrap(10));
        c.setClickable(true);
        c.setFocusable(true);
        c.setOnClickListener(v -> open(r));

        TextView num = serifText(Routines.roman(r), r >= 10 ? 15 : 20, ACCENT, true);
        num.setGravity(Gravity.CENTER);
        num.setBackground(rounded(0x00000000, BORDER_STRONG, dp(1), dp(8)));
        c.addView(num, new LinearLayout.LayoutParams(dp(52), dp(64)));

        LinearLayout t = column();
        t.setPadding(dp(14), 0, dp(8), 0);
        TextView name = serifText(Routines.name(prefs, r).toUpperCase(), 15, TEXT, true);
        name.setLetterSpacing(0.06f);
        t.addView(name);
        TextView sum = text(Routines.summary(prefs, r), 12, MUTED);
        sum.setPadding(0, dp(3), 0, 0);
        t.addView(sum);
        TextView code = text(r == Routines.NORMAL ? "Noa · Noa1" : "Noa" + r, 11, ACCENT);
        code.setTypeface(Typeface.DEFAULT_BOLD);
        code.setPadding(dp(8), dp(3), dp(8), dp(3));
        code.setBackground(rounded(CHIP, 0, 0, dp(6)));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(6);
        t.addView(code, clp);
        c.addView(t, weight(1));

        TextView chev = text("›", 26, BORDER_STRONG);
        c.addView(chev);
        return c;
    }

    private View buildCard() {
        LinearLayout c = row();
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(16), dp(14), dp(12), dp(14));
        c.setBackground(dashed(BORDER_STRONG, dp(14)));
        c.setLayoutParams(matchWrap(10));
        c.setClickable(true);
        c.setFocusable(true);
        c.setOnClickListener(v -> open(Routines.createCustom(prefs)));

        TextView plus = serifText("+", 26, ACCENT, true);
        plus.setGravity(Gravity.CENTER);
        plus.setBackground(dashed(BORDER_STRONG, dp(8)));
        c.addView(plus, new LinearLayout.LayoutParams(dp(52), dp(64)));

        LinearLayout t = column();
        t.setPadding(dp(14), 0, dp(8), 0);
        TextView name = serifText("BUILD IT YOURSELF", 15, TEXT, true);
        name.setLetterSpacing(0.06f);
        t.addView(name);
        int next = Routines.FIRST_CUSTOM;
        for (int id : Routines.customIds(prefs)) if (id >= next) next = id + 1;
        TextView sum = text("2 to 8 outcomes, on the pickups you choose · becomes routine " + Routines.roman(next), 12, MUTED);
        sum.setPadding(0, dp(3), 0, 0);
        t.addView(sum);
        c.addView(t, weight(1));
        return c;
    }

    private void open(int r) {
        startActivity(new Intent(this, RoutineActivity.class).putExtra(RoutineActivity.EXTRA_ROUTINE, r));
    }

    // ---------------------------------------------------------------- credits

    private View credits() {
        LinearLayout c = column();
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(dp(16), dp(44), dp(16), dp(36));

        LinearLayout orn = row();
        orn.setGravity(Gravity.CENTER_VERTICAL);
        View l1 = new View(this);
        l1.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x005A2630, BORDER_STRONG}));
        orn.addView(l1, new LinearLayout.LayoutParams(0, dp(1), 1f));
        orn.addView(diamond(CRIMSON, 7));
        View l2 = new View(this);
        l2.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{BORDER_STRONG, 0x005A2630}));
        orn.addView(l2, new LinearLayout.LayoutParams(0, dp(1), 1f));
        c.addView(orn, matchWrap());

        // Both columns hang from the same top line, so the labels and the first line of each
        // name sit level even when one name wraps.
        LinearLayout pair = row();
        pair.setGravity(Gravity.TOP);
        pair.setPadding(0, dp(24), 0, dp(22));
        pair.addView(creditColumn("CREATED BY", "Yair Sarig"), weight(1));
        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(1), dp(58));
        dlp.setMargins(dp(14), 0, dp(14), 0);
        pair.addView(divider, dlp);
        pair.addView(creditColumn("PRESENTED BY", "The Daily Magician"), weight(1));
        c.addView(pair, matchWrap());

        TextView ver = text("THRILL · " + VERSION, 11, FAINT);
        centered(ver, 0.08f);
        c.addView(ver);
        return c;
    }

    private View creditColumn(String role, String name) {
        LinearLayout l = column();
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView r = text(role, 10, MUTED);
        centered(r, 0.3f);
        l.addView(r);
        TextView n = serifText(name, 16, TEXT, true);
        n.setGravity(Gravity.CENTER);
        n.setLetterSpacing(0.08f);
        n.setPadding(0, dp(8), 0, 0);
        n.setLayoutParams(matchWrap());
        l.addView(n);
        return l;
    }
}
