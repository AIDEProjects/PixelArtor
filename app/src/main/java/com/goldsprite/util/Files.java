package com.goldsprite.util;
import com.goldsprite.customanimator.*;
import com.goldsprite.util.*;
import java.nio.file.Files;
import java.util.*;
import java.nio.file.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.*;

public class Files {

	public static void deleteFile(String logPath) {
		try {
			File file = new File(logPath);
			if (!file.exists() || !file.isFile()) return;

			file.delete();
		} catch (Exception e) {
			AppLog.dialog("删除文件失败: ", Log.getStackTraceStr(e));
		}
	}
	
	public static void writeString(String filePath, String str, boolean isMkdirs, boolean isAppend) {
		try {
			File file = new File(filePath);
			if (isMkdirs) {
				file.getParentFile().mkdirs();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, isAppend));
			bw.write(str);
			bw.newLine();
			bw.close();
		} catch (Exception e) {
			AppLog.dialog("写入文件失败: ", Log.getStackTraceStr(e));
		}
	}
	
	public static String readString(String filePath) {
		StringBuilder str = new StringBuilder();
		try {
			File file = new File(filePath);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				str.append(line).append("\n");
			}
			br.close();
		} catch (Exception e) {
			AppLog.dialog("读取文件失败: ", Log.getStackTraceStr(e));
		}
		return str.toString();
	}
	
}
