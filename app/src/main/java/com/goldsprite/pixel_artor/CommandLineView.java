package com.goldsprite.pixel_artor;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.graphics.*;
import com.goldsprite.customanimator.*;
import com.goldsprite.util.*;

import android.widget.EditText;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.lang.reflect.*;

import com.goldsprite.methodhandleexecutor.core.MethodHandleExecutor;


public class CommandLineView extends EditText {

    private PixelCanvas pixelCanvas;
	
	private MethodHandleExecutor mhExec;
	

    public CommandLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CommandLineView(Context context) {
        super(context);
        init();
    }

    private void init() {
		
		this.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
						handleCommand(getText().toString());
						setText(""); // 清空输入框
						return true;
					}
					return false;
				}
			});
    }

    public void setPixelCanvas(PixelCanvas pixelCanvas) {
        this.pixelCanvas = pixelCanvas;
		mhExec = new MethodHandleExecutor(pixelCanvas);
    }
	
	private void handleCommand(String commandLine){
		mhExec.executeCommand(commandLine);
	}

    private void handleCommandOld(String command) {
		String fMethodName = "未找到";
		boolean paramMatch = false;
		
        String[] parts = command.split(" ");
        if (parts.length < 1 || pixelCanvas == null) return;

        String methodName = parts[0];
        String[] args = new String[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            args[i - 1] = parts[i];
        }

		Method[] methods = PixelCanvas.class.getMethods();
		for (Method method : methods) {
			try{
				if (method.getName().equals(methodName)) {
					fMethodName = method.getName();
					if(method.getParameterCount() == args.length){
						paramMatch = true;
						Class<?>[] paramTypes = method.getParameterTypes();
						Object[] parsedArgs = new Object[args.length];

						for (int i = 0; i < args.length; i++) {
							parsedArgs[i] = convertToType(args[i], paramTypes[i]);
						}

						method.invoke(pixelCanvas, parsedArgs);
						return;
					}
				}
			}catch (Exception e){
				AppLog.dialog("handleCommand异常", Log.getStackTraceStr(e));
			}
		}
		AppLog.toast("Error: Method not found or parameters do not match.");
		AppLog.dialog(
			"解析指令失败，详细：", 
			""
			+"方法名："+ fMethodName
			+"参数匹配："+ paramMatch
			);
    }

    private Object convertToType(String arg, Class<?> type) {
		try {
			if (type.isPrimitive()) {
				// 基本数据类型使用包装类进行转换
				if (type == int.class) return Integer.parseInt(arg);
				if (type == long.class) return Long.parseLong(arg);
				if (type == double.class) return Double.parseDouble(arg);
				if (type == float.class) return Float.parseFloat(arg);
				if (type == boolean.class) return Boolean.parseBoolean(arg);
				if (type == char.class && arg.length() == 1) return arg.charAt(0);
				if (type == byte.class) return Byte.parseByte(arg);
				if (type == short.class) return Short.parseShort(arg);
			} else {
				// 非基本数据类型使用构造函数进行转换
				Constructor<?> constructor = type.getConstructor(String.class);
				return constructor.newInstance(arg);
			}
		} catch (Exception e) {
			//AppLog.dialog("Error in type conversion: ", Log.getStackTraceStr(e));
		}
		return null;
	}
}

