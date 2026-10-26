package com.freewill.predict;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The live rehearsal: lay the phone down, pick it up, see what would be written.
 * On the home page it has a routine picker; on a routine page it is locked to that routine.
 */
class RehearsalCard implements PickupDetector.Listener {

    private final BaseActivity a;
    private final PickupDetector detector;
    private final boolean withPicker;
    private int routine;
    private PickupDetector.Pickup last;
    private boolean running;

    private LinearLayout pickerHolder;
    private TextView phase, desc, result, values;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refresh();
            a.main.postDelayed(this, 100);
        }
    };

    RehearsalCard(BaseActivity activity, int routine, boolean withPicker) {
        this.a = activity;
        this.routine = routine;
        this.withPicker = withPicker;
        this.detector = new PickupDetector(activity, this);
        detector.setRearmAfterFire(true);
    }

    View build(String lockedName) {
        LinearLayout c = a.column();
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(a.dp(16), a.dp(24), a.dp(16), a.dp(22));
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientCenter(0.5f, 0.3f);
        g.setGradientRadius(a.dp(260));
        g.setColors(new int[]{0xFF3B0B14, BaseActivity.CARD});
        g.setCornerRadius(a.dp(14));
        g.setStroke(a.dp(1), BaseActivity.BORDER_STRONG);
        c.setBackground(g);
        c.setLayoutParams(a.matchWrap(18));

        TextView h = a.serifText("REHEARSAL", 16, BaseActivity.TEXT, true);
        a.centered(h, 0.25f);
        c.addView(h);

        TextView sub = a.text(withPicker ? "Choose a routine, then lay the phone down."
                : lockedName + " only. Lay the phone down and pick it up.", 12, BaseActivity.MUTED);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, a.dp(6), 0, 0);
        c.addView(sub, a.matchWrap());

        if (withPicker) {
            pickerHolder = a.column();
            pickerHolder.setPadding(0, a.dp(12), 0, 0);
            c.addView(pickerHolder, a.matchWrap());
            rebuildPicker();
        }

        ImageView mark = new ImageView(a);
        mark.setImageResource(R.drawable.t_mark);
        mark.setAlpha(0.9f);
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(a.dp(54), a.dp(62));
        mlp.setMargins(0, a.dp(16), 0, a.dp(10));
        c.addView(mark, mlp);

        phase = a.serifText("", 24, BaseActivity.ACCENT, true);
        a.centered(phase, 0.22f);
        c.addView(phase);

        desc = a.text("", 13, BaseActivity.MUTED);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(a.dp(12), a.dp(6), a.dp(12), 0);
        c.addView(desc, a.matchWrap());

        result = a.serifText("", 15, BaseActivity.TEXT, false);
        result.setGravity(Gravity.CENTER);
        result.setLineSpacing(0, 1.3f);
        result.setPadding(a.dp(8), a.dp(12), a.dp(8), 0);
        c.addView(result, a.matchWrap());

        values = a.text("", 11, BaseActivity.FAINT);
        values.setTypeface(Typeface.MONOSPACE);
        values.setGravity(Gravity.CENTER);
        values.setPadding(0, a.dp(12), 0, 0);
        c.addView(values, a.matchWrap());

        if (!detector.hasRotationSensor()) {
            TextView w = a.text("No gyroscope found: who picked it up will be guessed.", 12, BaseActivity.ACCENT);
            w.setGravity(Gravity.CENTER);
            w.setPadding(0, a.dp(10), 0, 0);
            c.addView(w, a.matchWrap());
        }
        return c;
    }

    /** Buttons for every routine, four to a row. */
    void rebuildPicker() {
        if (pickerHolder == null) return;
        pickerHolder.removeAllViews();
        List<Integer> ids = Routines.all(a.prefs);
        if (!ids.contains(routine)) routine = Routines.NORMAL;
        List<TextView> buttons = new ArrayList<>();
        LinearLayout r = null;
        for (int i = 0; i < ids.size(); i++) {
            if (i % 4 == 0) {
                r = a.row();
                pickerHolder.addView(r, a.matchWrap(i == 0 ? 0 : 8));
            }
            final int id = ids.get(i);
            TextView b = a.text(Routines.roman(id) + "\n" + Routines.shortName(a.prefs, id), 11, BaseActivity.MUTED);
            b.setGravity(Gravity.CENTER);
            b.setMinHeight(a.dp(56));
            b.setPadding(a.dp(2), a.dp(6), a.dp(2), a.dp(6));
            b.setTag(id);
            LinearLayout.LayoutParams lp = a.weight(1);
            lp.setMargins(a.dp(4), 0, a.dp(4), 0);
            r.addView(b, lp);
            buttons.add(b);
            b.setOnClickListener(v -> {
                routine = id;
                last = null;
                applySettings();
                stylePicker(buttons);
            });
        }
        while (r != null && r.getChildCount() < 4) {   // keep the last row's buttons the same width
            r.addView(a.spacer(0, 1), lpFill());
        }
        stylePicker(buttons);
    }

    private LinearLayout.LayoutParams lpFill() {
        LinearLayout.LayoutParams lp = a.weight(1);
        lp.setMargins(a.dp(4), 0, a.dp(4), 0);
        return lp;
    }

    private void stylePicker(List<TextView> buttons) {
        for (TextView b : buttons) {
            boolean on = (int) b.getTag() == routine;
            b.setBackground(on ? a.rounded(BaseActivity.CRIMSON, 0, 0, a.dp(12))
                    : a.rounded(0x00000000, BaseActivity.BORDER_STRONG, a.dp(1), a.dp(12)));
            b.setTextColor(on ? 0xFFFFFFFF : BaseActivity.MUTED);
        }
    }

    void applySettings() {
        detector.setTrigger(Prefs.trigger(a.prefs, routine));
        detector.setPauseMs(Prefs.pauseMs(a.prefs, routine));
    }

    void start() {
        if (running) return;
        running = true;
        applySettings();
        detector.start();
        a.main.post(ticker);
    }

    void stop() {
        running = false;
        detector.stop();
        a.main.removeCallbacks(ticker);
    }

    private void refresh() {
        boolean edges = routine != Routines.NORMAL;
        PickupDetector.Phase ph = detector.getPhase();
        if (edges && detector.isPaused()
                && (ph == PickupDetector.Phase.ARMED || ph == PickupDetector.Phase.LIFTING)) {
            phase.setText("PAUSED");
            desc.setText("Pause registered. Finish the pickup.");
        } else {
            switch (ph) {
                case WAITING:
                    phase.setText("WAITING");
                    desc.setText("Lay it face down, bottom pointing at you.");
                    break;
                case ARMED:
                    phase.setText("ARMED");
                    desc.setText(edges ? "Lift an edge, or lift slightly and hold still for a pause."
                            : "Face down and waiting. Pick it up to see which reveal fires.");
                    break;
                case LIFTING:
                    phase.setText("RISING");
                    desc.setText("");
                    break;
                default:
                    phase.setText("REVEALED");
                    desc.setText(last == null ? "" : Routines.describe(routine, last));
                    break;
            }
        }
        result.setText(last == null ? "" : Routines.build(a.prefs, routine, last, ""));
        float[] gv = detector.getGravity();
        values.setText(String.format(Locale.US, "tilt %.0f° · x %.1f · y %.1f · z %.1f",
                detector.getTilt(), gv[0], gv[1], gv[2]));
    }

    // ---- detector callbacks ----

    @Override public void onArmed() {
        a.main.post(() -> {
            last = null;
            if (Prefs.vibrate(a.prefs, routine)) Haptics.armedTap(a);
        });
    }

    @Override public void onDisarmed() { a.main.post(() -> last = null); }

    @Override public void onPaused() {
        a.main.post(() -> {
            if (routine != Routines.NORMAL && Prefs.vibrate(a.prefs, routine)) Haptics.armedTap(a);
        });
    }

    @Override public void onPickup(PickupDetector.Pickup pk) { a.main.post(() -> last = pk); }
}
