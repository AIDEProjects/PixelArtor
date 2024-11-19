package com.goldsprite.pixel_artor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.*;

public class PixelCanvas2 extends View {
    private Bitmap bitmap;

    public PixelCanvas2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PixelCanvas2(Context context) {
        super(context);
        init();
    }

    private void init() {
        bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
		bitmap.eraseColor(Color.BLUE); // 设置为蓝色背景
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bitmap, 0, 0, null);
    }
}

