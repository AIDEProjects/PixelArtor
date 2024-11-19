package com.goldsprite.customanimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.widget.Toast;
import com.goldsprite.util.Files;
import com.goldsprite.util.Log;
import com.goldsprite.util.Project;

public class AppLog {

	private static Activity ctx;

	public static void setCtx(Activity ctx) {
		AppLog.ctx = ctx;
	}

    //在短暂toast后结束应用
	public static void finishWithToast(String str) {
		try{
			toast(str);
			new Handler().postDelayed(
				new Runnable(){
					public void run() {
						ctx.finish();
					}
				}, 
				1000
			);
		}catch (Exception e){
			Log.logErr(e, true);
		}
	}
	public static void toastf(Object strObj, Object... objs) {
		final String str = String.format(""+strObj, objs);
		toast(str);
	}
	public static void toast(Object strObj) {
		final String str =""+strObj;
		try{
			ctx.runOnUiThread(
				new Runnable(){
					public void run() {
						Toast.makeText(ctx, str, Toast.LENGTH_SHORT).show();
					}
				}
			);
		}catch (Exception e){
			Log.logErr(e, true);
		}
	}

	public static void dialog(Object titleObj, Object msgObj) {
		dialog(titleObj, msgObj, null, null);
	}
	public static void dialog(Object titleObj, Object msgObj, final Runnable sureRun, final Runnable cancleRun) {
		try{
			final String title = "" + titleObj;
			final String msg = "" + msgObj;
			ctx.runOnUiThread(
				new Runnable(){
					public void run() {
						try {
							AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
							builder.setTitle(title);
							builder.setMessage(msg);
							builder.setPositiveButton("确定", 
								new AlertDialog.OnClickListener(){
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
										if (sureRun != null) {
											sureRun.run();
										}
									}
								}
							);
							builder.setNegativeButton("取消", 
								new AlertDialog.OnClickListener(){
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
										if (cancleRun != null) {
											cancleRun.run();
										}
									}
								}
							);
							AlertDialog dialog = builder.create();
							dialog.show();
						} catch (Exception e) {
							toast("创建dialog代码异常: " + e.getMessage());
						}
					}
				}
			);
		}catch (Exception e){
			Log.logErr(e, true);
		}
	}

	public static void clearLog() {
		try {
			Files.deleteFile(Project.logPath);
		} catch (Exception e2) {
			dialog("清理Log出错: ", Log.getStackTraceStr(e2));
		}
	}
	public static void saveLog(String log) {
		try {
			boolean isMkdirs = true;
			boolean isAppend = true;
			Files.writeString(Project.logPath, log, isMkdirs, isAppend);
		} catch (Exception e2) {
			dialog("保存Log出错: ", Log.getStackTraceStr(e2));
		}
	}

}
