package com.susu.test.nio.simple;

import org.apache.log4j.Logger;

import com.susu.common.CommException;
import com.susu.common.nio.filter.InputFilter;
import com.susu.common.nio.session.Session;

/**
 * 当客户端连接上以后马上给客户端发送一个报文
 */
public class ServerInputFilter implements InputFilter {
	private static Logger logger  =  Logger.getLogger(ServerInputFilter.class );
	private int order;
	
	public ServerInputFilter(int order){
		this.order=order;
	}
	public void destroy() throws CommException {
		System.out.println(this.toString()+"destroy");
	}

	public int getOrder() {
		
		return order;
	}

	public void init() throws CommException {
		System.out.println(this.toString()+"init");

	}

	public void onCloseSession(Session session) throws CommException {
		System.out.println(this.toString()+"onCloseSession");

	}

	public void onCreateSession(Session session) throws CommException {
		System.out.println(this.toString()+"onCreateSession");
		String s="user_type=1usewd=e10aba59abbe56e057f20f883ebran=B00000lan_ip=10.14.ain_flag=0user0078";
		session.wirte(s);
	}

	public void onReceiveMessage(Session session) throws CommException {
		String s=new String((byte[])session.getReceiveMessage());
		
		logger.info(this.toString()+"onReceiveMessage:"+s);
	}
	
	public String toString(){
		return order+"-inputFilter:";
	}                         
}
