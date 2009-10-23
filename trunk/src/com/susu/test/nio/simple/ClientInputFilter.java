package com.susu.test.nio.simple;

import org.apache.log4j.Logger;

import com.susu.common.CommException;
import com.susu.common.nio.filter.InputFilter;
import com.susu.common.nio.session.Session;

/**
 * 打印收到的报文数据
 */
public class ClientInputFilter implements InputFilter {
	private static Logger logger  =  Logger.getLogger(ClientInputFilter.class );
	private int order;
	
	public ClientInputFilter(int order){
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
	}

	public void onReceiveMessage(Session session) throws CommException {
		String s=new String((byte[])session.getReceiveMessage());
		
		logger.info(this.toString()+"onReceiveMessage:"+s);
	}
	
	public String toString(){
		return order+"-inputFilter:";
	}                         
}
