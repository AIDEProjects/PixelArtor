package com.goldsprite.pixel_artor;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.goldsprite.customanimator.*;
import com.goldsprite.util.*;
import android.graphics.*;
import com.google.gson.*;
import java.lang.reflect.*;

public class CanvasData {

    // 颜色映射表：颜色索引 -> RGB值
    private Map<Integer, String> colorMap = new HashMap<>();
    public Map<Integer, String> getColorMap(){ return colorMap; }
    // 反向查找：RGB值 -> 颜色索引
    private transient Map<String, Integer> colorIndexMap = new HashMap<>();
    public Map<String, Integer> getColorIndexMap(){ return colorIndexMap; }
    // 存储像素数据
    private String pixelData = "";
    public String getPixelData(){ return pixelData; }

	public CanvasData(){}

    // 添加颜色并返回颜色索引
    public int recordColor(int color) {
        String rgb = String.format("%06X", (0xFFFFFF & color)); // 获取 RGB

        // 如果颜色未记录，添加到颜色映射表
        if (!colorIndexMap.containsKey(rgb)) {
			//AppLog.toastf("记录新颜色：%s", rgb);
            int index = colorMap.size();
            colorMap.put(index, rgb);
            colorIndexMap.put(rgb, index);
        }
		//AppLog.dialog("当前画布数据", serialize());
        return colorIndexMap.get(rgb);
    }
	
	public int parsePixelData(String si)
	{
		String[] di = si.split("#");
		int alpha = Integer.parseInt(di[0]);
		int index = Integer.parseInt(di[1]);
		int hexcolor = Integer.parseInt(colorMap.get(index), 16);
		int argb = alpha<<24 | hexcolor;
		return argb;
	}

    // 保存像素数据
    public void savePixelData(int x, int y, int alpha, int colorIndex) {
		savePixelData(x, y, 1, 1, alpha, colorIndex);
	}
    public void savePixelData(int startX, int startY, int width, int height, int alpha, int colorIndex) {
        String pixelInfo = alpha + "#" + colorIndex;
        //pixelData.add(pixelInfo);
    }

	public void saveCanvasData(Bitmap bitmap) {
		pixelData = "";  // 清空现有数据

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		// 遍历每个像素点
		for (int y = 0; y < height; y++) {  // 从底部往上遍历
			for (int x = 0; x < width; x++) {
				int color = bitmap.getPixel(x, y);
				int alpha = (color >> 24) & 0xFF;  // 提取 alpha 值

				if (alpha == 0) {
					pixelData += "0";  // 透明像素
				} else {
					// 记录颜色并获取颜色索引
					int colorIndex = recordColor(color);

					// 生成 "{alpha}#{colorIndex}" 格式的数据
					String pixelInfo = alpha + "#" + colorIndex;
					pixelData += pixelInfo;
				}
				if ((x+1) * (y+1) < width * height - 1){
					pixelData += ", ";
				}
			}
		}
	}
	
	
	public static class CanvasDataDeserializer implements JsonDeserializer<CanvasData> {
		@Override
		public CanvasData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
			try {
				// 直接反序列化到 CanvasData
				CanvasData data = new Gson().fromJson(jsonElement, CanvasData.class);
				/*
				// 手动处理 colorIndexMap 并赋值
				Map<String, Integer> colorIndexMap = new HashMap<>();
				for (Map.Entry<Integer, String> entry : data.getColorMap().entrySet()) {
					colorIndexMap.put(entry.getValue(), entry.getKey());
				}
				data.getColorIndexMap().putAll(colorIndexMap);
				
				// 返回完成反序列化的 CanvasData 对象
				*/
				return data;
			}catch (Exception e){
				AppLog.dialog("反序列化异常: ", Log.getStackTraceStr(e));
			}
			
			return null;
		}
	}
	
}

