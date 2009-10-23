package com.susu.common.nio.handler;

import java.nio.channels.SocketChannel;

import com.susu.common.CommException;

/**
 * Socket��д�ӿ�
 * @author zhjb
 */
public interface IoHandler {
	
	/**
	 * ��ʼ��Socket��д�ӿ�
	 * @throws Exception
	 */
	public void init() throws CommException;
	
	/**
	 * Socketд����
	 * @param sc
	 * @param buffer
	 * @throws Exception
	 */
	public void wirte(SocketChannel sc,Object buffer)throws CommException;
	
	/**
	 * Socket������
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
