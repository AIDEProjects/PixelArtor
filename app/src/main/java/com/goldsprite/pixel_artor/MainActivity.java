package com.goldsprite.pixel_artor;

import android.app.*;
import android.os.*;
import android.view.*;
import android.graphics.*;
import com.goldsprite.customanimator.*;

public class MainActivity extends Activity 
{
    private PixelCanvas2 pixelCanvas;
    private CommandLineView commandLineView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.setCtx(this);
		setContentView(R.layout.main); // 确保使用正确的布局文件名

        pixelCanvas = findViewById(R.id.pixel_canvas);
        commandLineView = findViewById(R.id.command_line_view);

        // 将 PixelCanvas 设置给指令行
        commandLineView.setPixelCanvas(pixelCanvas);
    }
	
	public void set(View v){
		int x=6;
		int y=2;
		String color = "#FF0000";
		AppLog.toastf("设置像素颜色: {%d, %d, %d}", x, y, color);
		pixelCanvas.setPixelColor(x, y, color);
	}
}
