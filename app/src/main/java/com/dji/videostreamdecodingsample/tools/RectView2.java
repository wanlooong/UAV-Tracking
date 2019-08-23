package com.dji.videostreamdecodingsample.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class RectView2 extends SurfaceView implements SurfaceHolder.Callback {
    private static SurfaceHolder holder = null;
    private Paint mPaint ;
    private int StrokeWidth = 5;

    public RectView2(Context context) {
        super(context);
        setZOrderOnTop(true);
        holder = getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);
        holder.addCallback(this);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(StrokeWidth);
        mPaint.setAlpha(100);
        mPaint.setColor(Color.RED);
    }

    protected void Paints(Canvas canvas, Rect rect) {
//        canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);

        canvas.drawRect(rect, mPaint);
        invalidate();
//        invalidate(rect);
    }

    public void RePaint(Rect rect) {
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
            Paints(canvas, rect);
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }
}
