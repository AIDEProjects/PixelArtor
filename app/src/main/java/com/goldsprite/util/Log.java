package com.goldsprite.util;

import java.io.*;
import com.goldsprite.customanimator.*;

public class Log
{
	
	public static void log(String log){
		AppLog.saveLog(log);
	}
	public static void log(String log, Object... objs){
		try{
			log = String.format(log, objs);
			AppLog.saveLog(log);
		}catch(Exception e){
			logErr("格式化log打印异常", e);
		}
	}
	public static void logErr(Exception e){
		StackTraceElement methodStackTrace = Thread.currentThread().getStackTrace()[2];
		String msg = methodStackTrace.getMethodName();
		logErr(msg, e, false);
	}
	public static void logErr(String msg, Exception e){
		logErr(msg, e, false);
	}
	public static void logErr(Exception e, boolean isOrigin){
		StackTraceElement methodStackTrace = Thread.currentThread().getStackTrace()[2];
		String msg = methodStackTrace.getMethodName();
		logErr(msg, e, isOrigin);
	}
	//isOrigin参数用于AppLog可能异常时
	public static void logErr(String msg, Exception e, boolean isOrigin){
		String log = msg+": \n"+getStackTraceStr(e);
		if(!isOrigin) 
			AppLog.toast(String.format("发生异常：%s, log已存储到本地.", msg));
		AppLog.saveLog(log);
	}

	public static String getStackTraceStr(Exception e){
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
}
