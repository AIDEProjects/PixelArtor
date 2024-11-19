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

public class CommandLineView extends EditText {

    private PixelCanvas2 pixelCanvas;

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

    public void setPixelCanvas(PixelCanvas2 pixelCanvas) {
        this.pixelCanvas = pixelCanvas;
    }

    private void handleCommand(String command) {
        String[] parts = command.split(" ");
        if (parts.length < 1) return;

        String instruction = parts[0];
        switch (instruction) {
			case "setPixelColor":
                if (parts.length == 4) {
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int color = Color.parseColor(parts[3]); // 解析十六进制颜色
                        if (pixelCanvas != null) {
                            pixelCanvas.setPixelColor(x, y, color);
                        }
                    } catch (Exception e) {
						AppLog.dialog("", Log.getStackTraceStr(e));
                    }
                }
                break;
            default:
                // 处理未知指令
                break;
        }
    }
}

