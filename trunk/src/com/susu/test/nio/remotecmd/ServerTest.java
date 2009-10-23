package com.susu.test.nio.remotecmd;

import com.susu.common.nio.acceptor.NioSocketAcceptor;
import com.susu.test.nio.simple.TestIoHandler;

/**
 * 
 * @author zhjb
 */

public class ServerTest {
	
	public static void main(String [] s){
		
		try {
			NioSocketAcceptor acceptor= new  NioSocketAcceptor();
			TestIoHandler handler = new TestIoHandler();
			acceptor.bind("127.0.0.1", 9000,handler);
			acceptor.addFilter(new ServerInputFilter(1));
			acceptor.addFilter(new TestOutputFilter(1));
			acceptor.startupAcceptor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
