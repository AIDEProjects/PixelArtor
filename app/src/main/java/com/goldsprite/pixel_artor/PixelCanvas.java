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
import android.os.*;

public class PixelCanvas extends View {
    private Bitmap bitmap;
    private Paint paint;
    private int xCount = 20;
    private int yCount = 20;
    private int minSize = 200; // wrap_content 的最小宽高 200dp

	private CanvasData canvasData;

	private boolean drawing, isEnterDrawingTaskRunning;
	private long drawingEnterDelay = 20;//持续按下后delay/ms进入绘制
	public void setDrawingEnterDelay(long delay){ this.drawingEnterDelay = delay; }
	private boolean isTouchDraw = true;
	public void setTouchDraw(boolean boo){ 
		AppLog.toast("触摸绘制现在为: " + (isTouchDraw = boo));
	}
	private String drawColor = "#550000FF";
	public void setDrawColor(String drawColor){ 
		AppLog.toast("绘制颜色现在为: " + (this.drawColor = drawColor));
	}

	private float startMovingCenterX, startMovingCenterY;//用于记录按下开始时的基准坐标
	private float lastMovingDoubleCenterX, lastMovingDoubleCenterY;//用于计算双指移动帧步长
	float diff=0, diffStep=0f;//记录步长及步长阈值
	public void setDiffStep(float diffStep){ this.diffStep = diffStep; }
	private boolean isDoubleFingerMovingCanvas = true;
	public void setDoubleFingerMovingCanvas(boolean boo){
		AppLog.toast("双指移动画布现在为: " + (isDoubleFingerMovingCanvas = boo));
	}

	private float canvasOffsetX, canvasOffsetY;

	private int justTouchGridX, justTouchGridY;

	private String actionStr="";
	private String debugTxt2="";
	private int pointerCount = 0;

	private long flushDebugTxtInterval = 300;//ms
	private Runnable flushDebugTxtRunnable = new Runnable(){
		public void run(){
			MainActivity.setDebugTxt(
				1, 
				"触摸操作Id: " + actionStr
				+ "\npointerCount: " + pointerCount
			);
			MainActivity.setDebugTxt(
				2, 
				debugTxt2
			);
			MainActivity.setDebugTxt(
				3, 
				"isTouchDraw: " + isTouchDraw
				+ ", currentDrawColor: " + drawColor
				+ "\nisEnterDrawingTaskRunning: " + isEnterDrawingTaskRunning
				+ ", drawing: " + drawing
				+ ", drawingEnterDelay: " + drawingEnterDelay
			);
			post(this);
		}
	};


    public PixelCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PixelCanvas(Context context) {
        super(context);
        init();
    }

	//初始化配置
    private void init() {
		canvasData = new CanvasData();
		// 创建 Bitmap，并填充为绿色背景
		bitmap = Bitmap.createBitmap(xCount, yCount, Bitmap.Config.ARGB_8888);
		//bitmap.eraseColor(Color.BLUE); // 设置背景色为绿色

		// 初始化 Paint
		paint = new Paint();
		paint.setFilterBitmap(false);  // 禁用双线性过滤，启用邻近像素模式
		paint.setAntiAlias(false);     // 禁用抗锯齿

		//打印Hi
		{
			setPixelsColor(4, 4, 1, 11, "#FF0000");
			setPixelsColor(4, 9, 5, 1, "#FF0000");
			setPixelsColor(9, 4, 1, 11, "#FF0000");

			setPixelsColor(14, 4, 1, 11, "#FF0000");
			setPixelColor(14, 5, "#00000000");
		}

		//创建一个线程刷新DebugTxt
		postDelayed(flushDebugTxtRunnable, flushDebugTxtInterval);
	}


	//处理触摸事件
	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		try{
			//获取触摸行为id并显示
			actionStr = Utils.getMotionEventActionFieldName(ev.getAction());
			if (actionStr == null) actionStr = "";

			int touchAction = ev.getAction();
			pointerCount = ev.getPointerCount();

			//获取单指触摸的像素格坐标
			float viewWidth = getWidth();
			float viewHeight = getHeight();
			float touchX = ev.getX(0);
			float touchY = viewHeight - ev.getY(0);
			float touchX2 = pointerCount > 1 ?ev.getX(1) : 0;
			float touchY2 = pointerCount > 1 ?viewHeight - ev.getY(1) : 0;
			float gridSizeX = viewWidth / xCount;
			float gridSizeY = viewHeight / yCount;
			int touchGridX = Math.round(touchX / gridSizeX);
			int touchGridY = Math.round(touchY / gridSizeY);
			//画布偏移修正
			touchGridX -= Math.round(canvasOffsetX / gridSizeX);
			touchGridY -= Math.round(canvasOffsetY / gridSizeY);

			//单指绘制
			{
				//判定条件：单指按下或滑动且触摸绘制开启
				if (
					(touchAction == MotionEvent.ACTION_MOVE
					|| touchAction == MotionEvent.ACTION_DOWN) 
					&& pointerCount == 1 && isTouchDraw){
					//设置延迟进入操作防止误触
					if (!drawing && !isEnterDrawingTaskRunning){
						AppLog.toast("开始计时: "+drawingEnterDelay);
						isEnterDrawingTaskRunning = true;
						postDelayed(
							new Runnable(){
								public void run(){
									//等待一定延迟后如果依旧为单指按下即进入绘制
									if (pointerCount == 1){
										drawing = true;
										AppLog.toast("计时完成，进入成功.");
									}else{
										AppLog.toast("计时完成，操作取消.");
									}
									isEnterDrawingTaskRunning = false;
								}
							}, 
							drawingEnterDelay
						);
					}
				}
				//判定进入后开始绘制
				if (drawing){
					//判断是否移动到新的坐标
					if (touchGridX != justTouchGridX || touchGridY != justTouchGridY){
						//绘制像素
						setPixelColor(touchGridX, touchGridY, drawColor);
						justTouchGridX = touchGridX;
						justTouchGridY = touchGridY;
					}
					
				}
				//所有手指抬起时退出drawing
				if (touchAction == MotionEvent.ACTION_UP){
					pointerCount = 0;
					drawing = false;
				}
			}

			//双指移动
			//如果正在绘制则不响应移动
			if(!drawing){
				float movingDoubleCenterX=0, movingDoubleCenterY=0;
				//计算二指中心：如果触摸指数为2则计算二指中心点
				if (pointerCount == 2){
					movingDoubleCenterX = touchX + (touchX2 - touchX) / 2;
					movingDoubleCenterY = touchY + (touchY2 - touchY) / 2;
				}
				//记录基准点：第二指按下时记录中心点为基准点
				if (touchAction == MotionEvent.ACTION_POINTER_2_DOWN){
					startMovingCenterX = movingDoubleCenterX;
					startMovingCenterY = movingDoubleCenterY;
					//减去开始时画布已有偏移
					startMovingCenterX -= canvasOffsetX;
					startMovingCenterY -= canvasOffsetY;
				}
				//更新移动: 双指滑动且移动画布开启
				if (
					touchAction == MotionEvent.ACTION_MOVE && pointerCount == 2
					&& isDoubleFingerMovingCanvas){
					diff = 
						(movingDoubleCenterX - lastMovingDoubleCenterX)
						* (movingDoubleCenterY - lastMovingDoubleCenterY);
					//移动量＞移动步长
					if (Math.abs(diff) > diffStep){
						lastMovingDoubleCenterX = movingDoubleCenterX;
						lastMovingDoubleCenterY = movingDoubleCenterY;
						canvasOffsetX = movingDoubleCenterX - startMovingCenterX;
						canvasOffsetY = movingDoubleCenterY - startMovingCenterY;
						invalidate();
					}
				}
			}

			debugTxt2 = ""
				+ "viewWidth: " + viewWidth
				+ ", viewHeight: " + viewHeight
				+ "\ngridSizeX: " + gridSizeX
				+ ", gridSizeY: " + gridSizeY
				+ "\ntouchX1: " + touchX
				+ ", touchY1: " + touchY
				+ "\ntouchX2: " + touchX2
				+ ", touchY2: " + touchY2
				+ "\ntouchGridX: " + touchGridX
				+ ", touchGridY(0): " + touchGridY
				+ "\ncanvasOffsetX: " + canvasOffsetX
				+ ", canvasOffsetY: " + canvasOffsetY
				+ "\ndoubleMovingVel: " + diff
				+ "\ndiffStep: " + diffStep
				;
		}catch (Exception e){
			AppLog.dialog("onTouchEvent异常", Log.getStackTraceStr(e));
		}
		return true;
	}


	//设置像素点颜色
    public void setPixelColor(int x, int y, String colorStr) {
		y = yCount - y;
		setPixelsColor(x, y, 1, 1, colorStr);
	}
	public void setPixelsColor(int startX, int startY, int width, int height, String colorStr) {
		try{
			//坐标转索引
			int startIndexX = startX - 1, startIndexY = startY - 1;
			//限制越界
			startIndexX = Math.max(0, Math.min(xCount - 1, startIndexX));
			startIndexY = Math.max(0, Math.min(yCount - 1, startIndexY));

			int color = Color.parseColor(colorStr);
			if (width * height == 1){
				bitmap.setPixel(startIndexX, startIndexY, color);
			}else{
				int[] colors = new int[width * height];
				// 填充颜色数组
				Arrays.fill(colors, color);
				// 一次性将颜色数组应用到 bitmap 上的指定区域
				bitmap.setPixels(colors, 0, width, startIndexX, startIndexY, width, height);
			}
			invalidate(); // 刷新视图
		}catch (Throwable e){
			AppLog.dialog("设置像素区域颜色异常, " + e.getMessage(), Log.getStackTraceStr(e));
		}
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


	//刷新画布绘制从CanvasData
    private void applyCanvasData() {
        String trimStr = canvasData.getPixelData().replace(" ", "");
		String[] strs = trimStr.split(",");
		int i=0;
		for (String si: strs){
			//跳过空0
			if ("0".equals(si)){
			}else{
				int color = canvasData.parsePixelData(si);
				int width = bitmap.getWidth();
				int x = i % width;
				int y = i / width;
				bitmap.setPixel(x, y, color);
			}
			i++;
		}
        invalidate();  // 刷新 View
    }


	//画布绘制
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.save();
		//画布移动
		canvas.translate(canvasOffsetX, -canvasOffsetY);

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
		float cellWidth = (float) viewWidth / xCount;
		float cellHeight = (float) viewHeight / yCount;

		// 绘制垂直网格线
		for (int x = 0; x <= xCount; x++) {
			float xPos = x * cellWidth;
			canvas.drawLine(xPos, 0, xPos, viewHeight, gridPaint);
		}

		// 绘制水平网格线
		for (int y = 0; y <= yCount; y++) {
			float yPos = y * cellHeight;
			canvas.drawLine(0, yPos, viewWidth, yPos, gridPaint);
		}

		canvas.restore();
	}


    // 将 dp 转换为 px
    private int dpToPx(int dp) {
		return dp;
        //return (int) (dp * getResources().getDisplayMetrics().density);
    }


}

