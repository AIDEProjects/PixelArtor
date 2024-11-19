package com.goldsprite.pixel_artor;

import android.content.*;
import android.graphics.*;
import android.util.*;
import android.view.*;
import android.view.ViewGroup.*;
import java.util.*;
import com.goldsprite.customanimator.*;
import com.goldsprite.util.Log;
import com.goldsprite.util.*;
import com.google.gson.*;

public class PixelCanvas2 extends View {
    private Bitmap bitmap;
    private Paint paint;
    private int sizeX = 20;
    private int sizeY = 20;
    private int minSize = 200; // wrap_content 的最小宽高 200dp

	private CanvasData canvasData;


    public PixelCanvas2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PixelCanvas2(Context context) {
        super(context);
        init();
    }

    private void init() {
		canvasData = new CanvasData();
		// 创建 Bitmap，并填充为绿色背景
		bitmap = Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);
		//bitmap.eraseColor(Color.BLUE); // 设置背景色为绿色

		// 在 (10,10) 位置绘制红色像素点
		bitmap.setPixel(5, 5, Color.RED);

		// 初始化 Paint
		paint = new Paint();
		paint.setFilterBitmap(false);  // 禁用双线性过滤，启用邻近像素模式
		paint.setAntiAlias(false);     // 禁用抗锯齿
		
		//kk();
		//load("/storage/emulated/0/goldspriteProjects/PixelArtor/kk.txt");
	}

    public void setPixelColor(int x, int y, String colorStr) {
		setPixelsColor(x, y, 1, 1, colorStr);
	}
	public void setPixelsColor(int startX, int startY, int width, int height, String colorStr) {
		int color = Color.parseColor(colorStr);
		//canvasData.recordColor(color);
		//canvasData.savePixelData();
		if (width * height == 1){
			bitmap.setPixel(startX, startY, color);
		}else{
			int[] colors = new int[width * height];
			// 填充颜色数组
			Arrays.fill(colors, color);
			// 一次性将颜色数组应用到 bitmap 上的指定区域
			bitmap.setPixels(colors, 0, width, startX, startY, width, height);
		}
		invalidate(); // 刷新视图
	}

	public void kk(){
		setPixelsColor(7, 5, 4, 4, "#0000FF");
		canvasData.saveCanvasData(bitmap);
		String json = MyGson.toJson(canvasData);
		Files.writeString(Project.projPath+"kk.txt", json, true, false);
		String json2 = Files.readString(Project.projPath+"kk.txt");
		canvasData = MyGson.fromJson(json2, CanvasData.class);
		AppLog.toast("3");
	}
	
	public void save(String path){
		try{
			canvasData.saveCanvasData(bitmap);
			String json = MyGson.toJson(canvasData);
			Files.writeString(path, json, true, false);
		}catch (Exception e){
			AppLog.dialog("", Log.getStackTraceStr(e));
		}
	}

	public void load(String path){
		try{
			String json = Files.readString(path);
			AppLog.toast(json);
			canvasData = MyGson.fromJson(json, CanvasData.class);
			applyCanvasData();
		}catch (Exception e){
			AppLog.dialog("", Log.getStackTraceStr(e));
		}
	}

    // 根据 CanvasData 更新 Bitmap
    private void applyCanvasData() {
        String trimStr = canvasData.getPixelData().replace(" ", "");
		String[] strs = trimStr.split(",");
		int i=0;
		for(String si: strs){
			//跳过空0
			if("0".equals(si)){
			}else{
				int color = canvasData.parsePixelData(si);
				int width = bitmap.getWidth();
				int x = i%width;
				int y = i/width;
				bitmap.setPixel(x, y, color);
			}
			i++;
		}
        invalidate();  // 刷新 View
    }


	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// 获取 View 的宽高
		int viewWidth = getWidth();
		int viewHeight = getHeight();

		// 检查 wrap_content 并设置最小宽高 200dp
		if (getLayoutParams().width == LayoutParams.WRAP_CONTENT) {
			viewWidth = Math.max(viewWidth, dpToPx(minSize));
		}
		if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
			viewHeight = Math.max(viewHeight, dpToPx(minSize));
		}

		// 拉伸 Bitmap 到 View 的宽高
		Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		Rect destRect = new Rect(0, 0, viewWidth, viewHeight);
		canvas.drawBitmap(bitmap, srcRect, destRect, paint);

		// 设置网格画笔
		Paint gridPaint = new Paint();
		gridPaint.setColor(Color.BLACK); // 设置网格线颜色为黑色
		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setStrokeWidth(1);

		// 计算每个单元格的宽度和高度
		float cellWidth = (float) viewWidth / sizeX;
		float cellHeight = (float) viewHeight / sizeY;

		// 绘制垂直网格线
		for (int x = 0; x <= sizeX; x++) {
			float xPos = x * cellWidth;
			canvas.drawLine(xPos, 0, xPos, viewHeight, gridPaint);
		}

		// 绘制水平网格线
		for (int y = 0; y <= sizeY; y++) {
			float yPos = y * cellHeight;
			canvas.drawLine(0, yPos, viewWidth, yPos, gridPaint);
		}
	}



    // 将 dp 转换为 px
    private int dpToPx(int dp) {
		return dp;
        //return (int) (dp * getResources().getDisplayMetrics().density);
    }


}

