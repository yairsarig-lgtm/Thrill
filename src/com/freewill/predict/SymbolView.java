package com.freewill.predict;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** One Zener card: circle, cross, waves, square or star, drawn in ink on a light card. */
class SymbolView extends View {
    private final int symbol;
    private final Paint card = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();

    SymbolView(Context c, int symbol) {
        super(c);
        this.symbol = symbol;
        card.setColor(0xFFF4F1EA);
        ink.setColor(0xFF1B1B2F);
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeCap(Paint.Cap.ROUND);
        ink.setStrokeJoin(Paint.Join.ROUND);
        setContentDescription(new String[]{"Circle", "Cross", "Waves", "Square", "Star"}[symbol]);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float rad = Math.min(w, h) * 0.12f;
        r.set(0, 0, w, h);
        c.drawRoundRect(r, rad, rad, card);
        float s = Math.min(w, h) * 0.56f;          // symbol size
        float cx = w / 2f, cy = h / 2f, half = s / 2f;
        ink.setStrokeWidth(s * 0.09f);
        path.reset();
        switch (symbol) {
            case 0:
                c.drawCircle(cx, cy, half * 0.85f, ink);
                break;
            case 1:
                c.drawLine(cx, cy - half, cx, cy + half, ink);
                c.drawLine(cx - half, cy, cx + half, cy, ink);
                break;
            case 2:
                for (int i = -1; i <= 1; i++) {
                    float y = cy + i * half * 0.6f;
                    path.moveTo(cx - half, y);
                    float seg = s / 3f;
                    for (int k = 0; k < 3; k++) {
                        float x0 = cx - half + k * seg;
                        path.quadTo(x0 + seg / 4f, y - seg / 3f, x0 + seg / 2f, y);
                        path.quadTo(x0 + seg * 3f / 4f, y + seg / 3f, x0 + seg, y);
                    }
                }
                c.drawPath(path, ink);
                break;
            case 3:
                c.drawRect(cx - half * 0.8f, cy - half * 0.8f, cx + half * 0.8f, cy + half * 0.8f, ink);
                break;
            default:
                for (int i = 0; i < 10; i++) {
                    double a = Math.PI / 2 + i * Math.PI / 5;
                    float rr = (i % 2 == 0) ? half : half * 0.42f;
                    float x = cx + (float) (rr * Math.cos(a)), y = cy - (float) (rr * Math.sin(a));
                    if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                }
                path.close();
                c.drawPath(path, ink);
                break;
        }
    }
}
