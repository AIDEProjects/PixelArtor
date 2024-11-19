package com.goldsprite.pixel_artor;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public class CommandLineView extends EditText {

    private PixelCanvas pixelCanvas;

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
    }

    private void handleCommand(String command) {
        String[] parts = command.split(" ");
        if (parts.length < 1) return;

        String instruction = parts[0];
        switch (instruction) {
            case "setCanvasSize":
                if (parts.length == 3) {
                    try {
                        int width = Integer.parseInt(parts[1]);
                        int height = Integer.parseInt(parts[2]);
                        if (pixelCanvas != null) {
                            pixelCanvas.setCanvasSize(width, height);
                        }
                    } catch (NumberFormatException e) {
                        // 处理无效的数字格式
                    }
                }
                break;
            case "setPixelColor":
                if (parts.length == 4) {
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int color = Integer.parseInt(parts[3], 16); // 解析十六进制颜色
                        if (pixelCanvas != null) {
                            pixelCanvas.setPixelColor(x, y, color);
                        }
                    } catch (NumberFormatException e) {
                        // 处理无效的数字格式
                    }
                }
                break;
            default:
                // 处理未知指令
                break;
        }
    }
}

