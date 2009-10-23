package com.susu.test.nio.remotecmd;

import com.susu.common.CommException;
import com.susu.common.nio.filter.InputFilter;
import com.susu.common.nio.session.Session;

/**
 * 打印收到的报文数据
 */
public class ServerInputFilter implements InputFilter {
	private int order;
	private CmdLine cmdLine ;
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
		cmdLine = new CmdLine();
	}

	public void onReceiveMessage(Session session) throws CommException {
		String s=new String((byte[])session.getReceiveMessage());
		System.out.println(s);
		String ret=cmdLine.execCmd(s.substring(8));
		if("".equals(ret)){
			ret="操作成功";
		}
		session.wirte(ret);
	}
	
	public String toString(){
		return order+"-inputFilter:";
	}                         
}
