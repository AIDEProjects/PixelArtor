package com.goldsprite.pixel_artor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.*;

public class PixelCanvas extends View {
    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private Paint paint;
    private int pixelSize = 40; // 每个像素点的大小

    public PixelCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PixelCanvas(Context context) {
        super(context);
        init();
    }

    private void init() {
        paint = new Paint();
        // 初始化 bitmap
        setCanvasSize(400, 400);
        setPixelColor(20, 20, Color.RED);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setCanvasSize(width, height);
    }

    // 设置画布大小
    public void setCanvasSize(int width, int height) {
        if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmapCanvas = new Canvas(bitmap);
            invalidate(); // 刷新视图
        }
    }

    // 设置每个像素点颜色
    public void setPixelColor(int x, int y, int color) {
        paint.setColor(color);
        int left = x * pixelSize;
        int top = y * pixelSize;
        int right = left + pixelSize;
        int bottom = top + pixelSize;
        bitmapCanvas.drawRect(left, top, right, bottom, paint);
        invalidate(); // 刷新视图
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, paint);
        }

        // 绘制网格
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF000000); // 黑色网格

        for (int x = 0; x <= getWidth(); x += pixelSize) {
            canvas.drawLine(x, 0, x, getHeight(), paint);
        }

        for (int y = 0; y <= getHeight(); y += pixelSize) {
            canvas.drawLine(0, y, getWidth(), y, paint);
        }
    }
}

