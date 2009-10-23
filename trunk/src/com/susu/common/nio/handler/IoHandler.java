package com.susu.common.nio.handler;

import java.nio.channels.SocketChannel;

import com.susu.common.CommException;

/**
 * Socket读写接口
 * @author zhjb
 */
public interface IoHandler {
	
	/**
	 * 初始化Socket读写接口
	 * @throws Exception
	 */
	public void init() throws CommException;
	
	/**
	 * Socket写方法
	 * @param sc
	 * @param buffer
	 * @throws Exception
	 */
	public void wirte(SocketChannel sc,Object buffer)throws CommException;
	
	/**
	 * Socket读方法
	 * @param sc
	 * @return
	 * @throws Exception
	 */
	public  Object read(SocketChannel sc) throws CommException;
	/**
	 * 
	 * @throws Exception
	 */
	public void destroy() throws CommException;
}
