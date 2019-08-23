package com.dji.videostreamdecodingsample.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.util.AttributeSet;
import android.view.View;

public class DashedLine extends View {
    private int w, h;
    private Path path;
    private PathEffect pathEffect;
    private Paint paint;

    public DashedLine(Context context) {
        super(context);
        init();
    }

    public DashedLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        path = new Path();
        pathEffect = new DashPathEffect(new float[]{50, 50}, 0);
        paint = new Paint();
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
//        paint.setColor(Color.BLUE);
        paint.setARGB(125, 255, 255, 255);
        paint.setPathEffect(pathEffect);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.w = w;
        this.h = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        path.moveTo(200, h / 3);
        path.lineTo(w - 200, h / 3);
        canvas.drawPath(path, paint);

        path.moveTo(200, h * 2 / 3);
        path.lineTo(w - 200, h * 2 / 3);
        canvas.drawPath(path, paint);

        path.moveTo(w / 3, 100);
        path.lineTo(w / 3, h - 100);
        canvas.drawPath(path, paint);

        path.moveTo(w * 2 / 3, 100);
        path.lineTo(w * 2 / 3, h - 100);
        canvas.drawPath(path, paint);
    }
}
