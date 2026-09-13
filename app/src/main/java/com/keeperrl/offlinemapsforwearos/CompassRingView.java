package com.keeperrl.offlinemapsforwearos;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class CompassRingView extends View {

    private float heading = 0f;
    private boolean trackUp = true;
    private final Paint ringHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint northHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint northTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path pointerPath = new Path();

    public CompassRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);

        // Dark halos drawn behind every stroke/glyph keep the ring legible over
        // light map tiles, roads and labels of any color.
        ringHaloPaint.setStyle(Paint.Style.STROKE);
        ringHaloPaint.setColor(Color.argb(160, 0, 0, 0));
        ringHaloPaint.setStrokeWidth(dp(4f));

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.argb(235, 255, 255, 255));
        ringPaint.setStrokeWidth(dp(2f));

        tickHaloPaint.setStyle(Paint.Style.STROKE);
        tickHaloPaint.setColor(Color.argb(160, 0, 0, 0));
        tickHaloPaint.setStrokeWidth(dp(3.5f));

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setColor(Color.argb(235, 255, 255, 255));
        tickPaint.setStrokeWidth(dp(2f));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(dp(12));

        textHaloPaint.set(textPaint);
        textHaloPaint.setStyle(Paint.Style.STROKE);
        textHaloPaint.setStrokeWidth(dp(3f));
        textHaloPaint.setColor(Color.argb(200, 0, 0, 0));

        northTextPaint.set(textPaint);
        northTextPaint.setColor(Color.parseColor("#FF3B30"));
        northTextPaint.setTextSize(dp(13));

        northHaloPaint.set(textHaloPaint);
        northHaloPaint.setTextSize(dp(13));

        pointerHaloPaint.setStyle(Paint.Style.STROKE);
        pointerHaloPaint.setStrokeJoin(Paint.Join.ROUND);
        pointerHaloPaint.setColor(Color.argb(180, 0, 0, 0));
        pointerHaloPaint.setStrokeWidth(dp(2.5f));

        pointerPaint.setStyle(Paint.Style.FILL);
        pointerPaint.setColor(Color.parseColor("#FF3B30"));
    }

    public void setHeading(float degrees) {
        this.heading = degrees;
        invalidate();
    }

    public void setTrackUp(boolean trackUp) {
        this.trackUp = trackUp;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - dp(6);
        if (radius <= 0)
            return;

        // In track-up mode the ring rotates opposite the heading so "up" always shows
        // the direction of travel; in north-up mode the ring stays fixed with N at top.
        float ringRotation = trackUp ? -heading : 0f;

        canvas.drawCircle(cx, cy, radius, ringHaloPaint);
        canvas.drawCircle(cx, cy, radius, ringPaint);

        float majorTickLen = dp(8);
        float minorTickLen = dp(4);
        for (int angle = 0; angle < 360; angle += 30) {
            boolean major = angle % 90 == 0;
            float tickLen = major ? majorTickLen : minorTickLen;
            double rad = Math.toRadians(angle + ringRotation - 90);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            float x1 = cx + cos * radius;
            float y1 = cy + sin * radius;
            float x2 = cx + cos * (radius - tickLen);
            float y2 = cy + sin * (radius - tickLen);
            canvas.drawLine(x1, y1, x2, y2, tickHaloPaint);
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }

        drawLabel(canvas, "N", 0, cx, cy, radius, ringRotation, northHaloPaint, northTextPaint);
        drawLabel(canvas, "E", 90, cx, cy, radius, ringRotation, textHaloPaint, textPaint);
        drawLabel(canvas, "S", 180, cx, cy, radius, ringRotation, textHaloPaint, textPaint);
        drawLabel(canvas, "W", 270, cx, cy, radius, ringRotation, textHaloPaint, textPaint);

        // Heading pointer only makes sense in north-up mode, where the ring is fixed
        // and the pointer rotates to show which way you're currently facing.
        if (!trackUp) {
            float pointerAngle = heading - 90f;
            drawPointer(canvas, pointerAngle, cx, cy, radius);
        }
    }

    private void drawLabel(Canvas canvas, String label, float bearing, float cx, float cy, float radius, float ringRotation, Paint haloPaint, Paint paint) {
        double rad = Math.toRadians(bearing + ringRotation - 90);
        float labelRadius = radius - dp(15);
        float x = cx + (float) Math.cos(rad) * labelRadius;
        float y = cy + (float) Math.sin(rad) * labelRadius;
        Paint.FontMetrics fm = paint.getFontMetrics();
        float textY = y - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(label, x, textY, haloPaint);
        canvas.drawText(label, x, textY, paint);
    }

    private void drawPointer(Canvas canvas, float angleDegrees, float cx, float cy, float radius) {
        double rad = Math.toRadians(angleDegrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        float tipR = radius + dp(4);
        float baseR = radius - dp(13);
        float halfWidth = dp(8f);
        // perpendicular direction to offset the two base corners of the triangle
        float perpCos = -sin;
        float perpSin = cos;

        float tipX = cx + cos * tipR;
        float tipY = cy + sin * tipR;
        float baseCx = cx + cos * baseR;
        float baseCy = cy + sin * baseR;

        pointerPath.reset();
        pointerPath.moveTo(tipX, tipY);
        pointerPath.lineTo(baseCx + perpCos * halfWidth, baseCy + perpSin * halfWidth);
        pointerPath.lineTo(baseCx - perpCos * halfWidth, baseCy - perpSin * halfWidth);
        pointerPath.close();
        canvas.drawPath(pointerPath, pointerHaloPaint);
        canvas.drawPath(pointerPath, pointerPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
