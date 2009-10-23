package com.susu.common;

public class ErrorCode {
	/*
	 * 监听端口异常
	 */
	public final static String NIO0000 = "NIO0000";
	
	/*
	 * 链接服务器异常
	 */
	public final static String NIO0001 = "NIO0001";
	/*
	 * 读数据异常
	 */
	public final static String NIO0002 = "NIO0002";
	
	/*
	 * 写数据异常
	 */
	public final static String NIO0003 = "NIO0003";
	
	/*
	 * 读取报文头异常
	 */
	public final static String NIO0004 = "NIO0004";	
	
	/*
	 * 读取报文体异常
	 */
	public final static String NIO0005 = "NIO0005";	
	
	/*
	 * 关闭Session异常
	 */
	public final static String NIO0006 = "NIO0006";	
	
	/**
	 * 链接数据库失败
	 */
	public final static String DBO0000= "DBO0000";	
	
	/**
	 * 未设置数据驱动Class
	 */
	public final static String DBO0001= "DBO0001";	
	
	/**
	 * 未设置数据库URL
	 */
	public final static String DBO0002= "DBO0002";	
	/**
	 * 未设置数据库用户名
	 */
	public final static String DBO0003= "DBO0003";	
	
	/**
	 * 已经达到配置的数据库最大的链接数
	 */
	public final static String DBO0004= "DBO0004";	
	
	/**
	 * 解析数据库映射类异常
	 */
	public final static String DBO0005= "DBO0005";
	/**
	 *加载内存数据库
	 */
	public final static String DBO0006= "DBO0006";
}
