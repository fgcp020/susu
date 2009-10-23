package com.susu.common.nio.filter;

import com.susu.common.CommException;
import com.susu.common.nio.session.Session;

/**
 * 输入过滤器，当收到数据时，要执行的过滤器
 * @author zhjb
 *
 */
public interface InputFilter extends Filter {
	/**
	 * 客户端与服务器创建会话的事件，即Socket连接成功事件
	 * @param session
	 */
	public void onCreateSession(Session session)throws CommException;
	
	/**
	 * 客户端与服务器关闭会话的事件，即Socket关闭事件
	 * @param session
	 */
	public void onCloseSession(Session session)throws CommException;
	
	/**
	 * 收到数据事件
	 * @param session
	 */
	public void onReceiveMessage(Session session)throws CommException;
	
}
