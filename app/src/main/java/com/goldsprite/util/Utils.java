package com.goldsprite.util;

import android.view.*;
import java.lang.reflect.*;
import com.goldsprite.customanimator.*;

public class Utils
{
	
	public static String getMotionEventActionFieldName(int action) {
        // 遍历 MotionEvent 类的所有字段
        Field[] fields = MotionEvent.class.getFields();

        for (Field field : fields) {
            try {
                // 检查字段是否为静态字段且是 int 类型
                if (field.getType() == int.class) {

                    // 获取字段的值
                    int fieldValue = field.getInt(null); // null 是因为是静态字段

                    // 比较字段值与传入的 action 值
                    if (fieldValue == action) {
                        return field.getName(); // 返回字段名
                    }
                }
            } catch (Exception e) {
				AppLog.dialog("getActionFieldName异常", Log.getStackTraceStr(e));
            }
        }
        return null; // 没有找到匹配的字段
    }
	
}
