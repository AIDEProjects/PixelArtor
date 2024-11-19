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
    private int xCount = 15;
    private int yCount = 15;
    private int minSize = 200; // wrap_content 的最小宽高 200dp

	public float getCanvasWidth(){ 
		return getWidth() * sclFactor;
	}
	public float getCanvasHeight(){ 
		return getHeight() * sclFactor;
	}

	//画布数据
	private CanvasData canvasData;

	//实时绘制相关
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

	//画布移动相关
	private float startMovingCenterX, startMovingCenterY;//用于记录按下开始时的基准坐标
	private float canvasOffsetX, canvasOffsetY;
	public void setCanvasOffsetXY(float x, float y){
		canvasOffsetX = x;
		canvasOffsetY = y;
		invalidate();
	}
	private int justTouchGridX, justTouchGridY;
	private boolean isDoubleFingerMovingCanvas = true;
	public void setDoubleFingerMovingCanvas(boolean boo){
		AppLog.toast("双指移动画布现在为: " + (isDoubleFingerMovingCanvas = boo));
	}

	//缩放相关
	private float startSclDistance;
	private boolean isDoubleFingerSclCanvas=true;
	public void setDoubleFingerSclCanvas(boolean boo){
		AppLog.toast("双指缩放画布现在为: " + (isDoubleFingerSclCanvas = boo));
	}
	private float sclFactor=1, startSclFactor;
	private float[] sclFixOffset = {0, 0};
	//设置缩放因子
	public void setSclFactor(float sclFactor){
		this.sclFactor = sclFactor;
		invalidate();
	}
	public float[] getSclFixOffset(){
		//sclFixOffset[0] = (1 - sclFactor) * getWidth() / 2f;
		//sclFixOffset[1] = (1 - sclFactor) * getHeight() / 2f;
		sclFixOffset[0] = (1 - sclFactor) * (-canvasOffsetX +  getWidth() / 2f);
		sclFixOffset[1] = (1 - sclFactor) * (-canvasOffsetY +  getHeight() / 2f);
		//AppLog.toastf("sclFixOffset: %.1f, %.1f", sclFixOffset[0], sclFixOffset[1]);
		return sclFixOffset;
	}

	//调试相关
	private String actionStr="";
	private String debugTxt2="";
	private int pointerCount = 0;
	private long flushDebugTxtInterval = 300;//ms
	private Runnable flushDebugTxtRunnable = new Runnable(){
		public void run(){
			MainActivity.setDebugTxt(
				1, 
				""
			);

			postDelayed(this, flushDebugTxtInterval);
		}
	};

	//撤销相关
	private enum DrawType{
		Draw, Eraser
		}
	private Stack<DrawOperators> drawHistories = new Stack<>();
	private int drawHistoryIndex = 0;


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
		bitmap.eraseColor(Color.TRANSPARENT); //清屏透明色以初始化(否则是-65536)

		// 初始化 Paint
		paint = new Paint();
		paint.setFilterBitmap(false);  // 禁用双线性过滤，启用邻近像素模式
		paint.setAntiAlias(false);     // 禁用抗锯齿

		//打印Hi
		{
			Object[] hiList = {
				//H
				2, 2, 1, 11, "#FF0000",
				2, 7, 5, 1, "#FF0000", 
				7, 2, 1, 11, "#FF0000", 
				//i
				12, 2, 1, 11, "#FF0000", 
				12, 3, 1, 1, "#00000000"
			};
			int paramCount = 5;
			for (int i=0;i < hiList.length;i += paramCount){
				setPixelsColor(
					(int)hiList[i + 0], 
					(int)hiList[i + 1], 
					(int)hiList[i + 2], 
					(int)hiList[i + 3], 
					(String)hiList[i + 4]
				);
			}
		}

		//创建一个线程刷新DebugTxt
		post(flushDebugTxtRunnable);
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		/*
		 setScaleX(sclFactor);
		 setScaleY(sclFactor);*/
		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
	}

	private boolean scling, translating;
	private float lastDoubleFingerDistance;
	private float lastMovingDoubleCenterX, lastMovingDoubleCenterY;
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
			float touchX = ev.getX(0);
			float touchY = ev.getY(0);
			//touchX = Math.max(0, Math.min(getCanvasWidth()-1, touchX));
			//touchY = Math.max(0, Math.min(getCanvasHeight()-1, touchY));
			float touchX2 = pointerCount > 1 ?ev.getX(1) : 0;
			float touchY2 = pointerCount > 1 ?ev.getY(1) : 0;
			//格子缩放修正
			float gridSizeX = getCanvasWidth() / xCount;
			float gridSizeY = getCanvasHeight() / yCount;
			//画布偏移修正
			float fixTouchX = touchX;
			float fixTouchY = touchY;
			//缩放偏移
			fixTouchX -= getSclFixOffset()[0];
			fixTouchY -= getSclFixOffset()[1];
			//位移偏移
			fixTouchX -= canvasOffsetX;
			fixTouchY -= canvasOffsetY;
			//转为格子坐标
			int touchGridX = (int)(fixTouchX / gridSizeX);
			//因为y轴反转所以这里入
			int touchGridY = (int)(fixTouchY / gridSizeY);

			//单指绘制
			{
				//判定条件：单指按下或滑动且触摸绘制开启
				if (
					(touchAction == MotionEvent.ACTION_MOVE
					|| touchAction == MotionEvent.ACTION_DOWN) 
					&& pointerCount == 1 && isTouchDraw){
					//设置延迟进入操作防止误触
					if (!drawing && !isEnterDrawingTaskRunning){
						//AppLog.toast("开始计时: " + drawingEnterDelay);
						isEnterDrawingTaskRunning = true;
						postDelayed(
							new Runnable(){
								public void run(){
									//等待一定延迟后如果依旧为单指按下即进入绘制
									if (pointerCount == 1){
										drawing = true;
										//AppLog.toast("计时完成，进入成功.");
									}else{
										//AppLog.toast("计时完成，操作取消.");
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
					drawHistoryIndex++;//封档此条历史线
				}
			}

			float movingDoubleCenterX=0, movingDoubleCenterY=0;
			float doubleFingerDistance=0;
			//如果正在绘制则不响应移动缩放
			if (!drawing){
				//双指移动
				{
					translating = false;
					//计算二指中心：如果触摸指数为2则计算二指中心点
					if (pointerCount == 2){
						movingDoubleCenterX = touchX + (touchX2 - touchX) / 2;
						movingDoubleCenterY = touchY + (touchY2 - touchY) / 2;
						//movingDoubleCenterX /= sclFactor;
						//movingDoubleCenterY /= sclFactor;
					}
					//记录基准点：第二指按下时记录中心点为基准点
					if (touchAction == MotionEvent.ACTION_POINTER_2_DOWN){
						startMovingCenterX = movingDoubleCenterX;
						startMovingCenterY = movingDoubleCenterY;
						//减去开始时画布已有偏移
						startMovingCenterX -= canvasOffsetX;
						startMovingCenterY -= canvasOffsetY;
						//startMovingCenterX -= canvasOffsetX * sclFactor;
						//startMovingCenterY -= canvasOffsetY * sclFactor;
					}
					//更新移动: 双指滑动且移动画布开启
					if (
						touchAction == MotionEvent.ACTION_MOVE && pointerCount == 2
						&& isDoubleFingerMovingCanvas){
						if (lastMovingDoubleCenterX != movingDoubleCenterX
							|| lastMovingDoubleCenterY != movingDoubleCenterY){
							lastMovingDoubleCenterX = movingDoubleCenterX;
							lastMovingDoubleCenterY = movingDoubleCenterY;
							translating = true;
							canvasOffsetX = (movingDoubleCenterX - startMovingCenterX);
							canvasOffsetY = (movingDoubleCenterY - startMovingCenterY);
							//canvasOffsetX = (movingDoubleCenterX - startMovingCenterX)/sclFactor;
							//canvasOffsetY = (movingDoubleCenterY - startMovingCenterY)/sclFactor;
							invalidate();
						}
					}
				}

				//双指缩放
				{
					scling = false;
					//记录实时双指距离
					if (pointerCount == 2){
						float disX = touchX2 - touchX;
						float disY = touchY2 - touchY;
						doubleFingerDistance = (float)Math.sqrt(disX * disX + disY * disY);
					}
					//计算开始时双指距离与缩放因子
					if (touchAction == MotionEvent.ACTION_POINTER_2_DOWN){
						startSclDistance = doubleFingerDistance;
						startSclFactor = sclFactor;
					}
					//更新缩放: 双指滑动且缩放画布开启
					if (
						touchAction == MotionEvent.ACTION_MOVE && pointerCount == 2
						&& isDoubleFingerSclCanvas){
						if (doubleFingerDistance != lastDoubleFingerDistance){
							scling = true;
							lastDoubleFingerDistance = doubleFingerDistance;
							setSclFactor(startSclFactor + (doubleFingerDistance - startSclDistance) / startSclDistance);
							invalidate();
						}
					}
				}
			}
			MainActivity.setDebugTxt(
				4, 
				""
				+ "\n操作模式："
				+ "绘制中: " + drawing
				+ ", 移动中: " + translating
				+ ", 缩放中: " + scling
			);
			debugTxt2 = ""
				+ "viewWidth: " + getWidth()
				+ ", viewHeight: " + getHeight()
				+ "\ncanvasWidth: " + getCanvasWidth()
				+ ", canvasHeight: " + getCanvasHeight()
				+ "\nsclFactor: " + sclFactor
				+ "\ngridSizeX: " + gridSizeX
				+ ", gridSizeY: " + gridSizeY
				+ "\ntouchX1: " + touchX
				+ ", touchY1: " + touchY
				+ "\nsclFixOffsetXY: " + getSclFixOffset()[0] + ", " + getSclFixOffset()[1]
				+ "\nfixTouchX: " + fixTouchX
				+ ", fixTouchY: " + fixTouchY
				+ "\ntouchGridX: " + touchGridX
				+ ", touchGridY(0): " + touchGridY
				+ "\ncanvasOffsetX: " + canvasOffsetX
				+ ", canvasOffsetY: " + canvasOffsetY
				;
			MainActivity.setDebugTxt(3, debugTxt2);
		}catch (Exception e){
			AppLog.dialog("onTouchEvent异常", Log.getStackTraceStr(e));
		}
		return true;
	}


	public float[] screenToWorldCoord(float screenX, float screenY){
		float[] worldCoord = new float[2];
		//位移到世界坐标
		worldCoord[0] = screenX - canvasOffsetX;
		worldCoord[1] = screenY - canvasOffsetY;

		return worldCoord;
	}

	//设置像素点颜色, LU坐标系
    public void setPixelColor(int x, int y, String colorStr){
		setPixelColor(x, y, colorStr, false);
	}
    public void setPixelColor(int x, int y, String colorStr, boolean isUndo) {
		setPixelsColor(x, y, 1, 1, colorStr, isUndo);
	}
	public void setPixelsColor(int startIndexX, int startIndexY, int width, int height, String colorStr) {
		setPixelsColor(startIndexX, startIndexY, width, height, colorStr, false);
	}
	public void setPixelsColor(int startIndexX, int startIndexY, int width, int height, String colorStr, boolean isUndo) {
		try{
			//限制越界
			if (width <= 0 || height <= 0){
				//AppLog.toast("错误：绘制区域越界");
				return;
			}
			if (startIndexX < 0 || startIndexY < 0){
				//AppLog.toast("错误：绘制区域越界");
				return;
			}
			if (startIndexX + width - 1 > xCount - 1 || startIndexY + height - 1 > yCount - 1){
				//AppLog.toast("错误：绘制区域越界");
				return;
			}

			int color = Color.parseColor(colorStr);

			//如果不是撤销(即正常绘制)则记录历史数据
			if (!isUndo){
				int oldColor = bitmap.getColor(startIndexX, startIndexY).toArgb();
				//当颜色改变时
				if (color != oldColor){
					DrawType drawType = Color.alpha(color) == 0 ?DrawType.Eraser : DrawType.Draw;
					String oldColorStr = String.format("#%08X", oldColor);
					DrawOperator drawHistoryData = new DrawOperator(
						startIndexX, startIndexY, 
						width, height, 
						oldColorStr
					);
					boolean create = false;
					//如果越界或绘制类型改变则创建新的
					if (drawHistoryIndex > drawHistories.size() - 1){
						create = true;
					}
					else if (!drawType.equals(drawHistories.get(drawHistoryIndex).drawType)){
						create = true;
					}
					if (create){
						DrawOperators newDrawHistory = new DrawOperators();
						newDrawHistory.drawType = drawType;
						drawHistories.push(newDrawHistory);
						drawHistoryIndex = drawHistories.size() - 1;
						AppLog.toast("创建新历史线");
					}
					//获取当前线历史并添加此次点
					DrawOperators drawHistory = drawHistories.get(drawHistoryIndex);
					drawHistory.push(drawType, drawHistoryData);
					AppLog.toast("记录1次历史操作");
				}
			}

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


	public void undo(){
		if (drawHistories.size() <= 0){
			AppLog.toast("无可撤销操作");
			return;
		}
		DrawOperators drawHistory = drawHistories.pop();

		while (drawHistory.pixelData.size() > 0){
			DrawOperator iobj = drawHistory.pixelData.pop();
			setPixelsColor(iobj.x, iobj.y, iobj.width, iobj.height, iobj.colorStr, true);
		}
		AppLog.toast("撤销1条历史线");
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
		//画布变换, 必须先位移在变换(否则位移量将被拉伸)
		canvas.translate(canvasOffsetX, canvasOffsetY);
		canvas.translate(getSclFixOffset()[0], getSclFixOffset()[1]);
		canvas.scale(sclFactor, sclFactor);

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


	public class DrawOperators {
		public DrawType drawType;
		public Stack<DrawOperator> pixelData = new Stack<>();

		public void push(DrawType drawType, DrawOperator pixelData){
			this.drawType = drawType;
			this.pixelData.push(pixelData);
		}
	}
	public class DrawOperator{
		public int x, y, width, height;
		public String colorStr;

		public DrawOperator(){}
		public DrawOperator(int x, int y, int width, int height, String coloStr){
			this.x = x;this.y = y;
			this.width = width;this.height = height;
			this.colorStr = coloStr;
		}
	}


}

