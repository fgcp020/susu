package com.susu.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;


public class Utils {
	
	private static ByteArrayOutputStream bos = new ByteArrayOutputStream();
	
	private static PrintWriter printWriter = new PrintWriter(bos);
	
	public static String getExceptionStack(Exception e){
		e.printStackTrace(printWriter);
		printWriter.flush();
		printWriter.close();
		String s=bos.toString();
		
		try {
			bos.flush();
			bos.reset();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return s;
	}
}
