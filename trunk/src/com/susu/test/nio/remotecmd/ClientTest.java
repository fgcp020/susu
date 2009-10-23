package com.susu.test.nio.remotecmd;

import com.susu.common.nio.connector.NioSocketConnector;
import com.susu.test.nio.simple.TestIoHandler;

/**
 * 客户端测试类，连上服务器后定时给服务器发送报文
 * @author zhjb
 */

public class ClientTest {
	public static boolean isRunning=true;
	public static NioSocketConnector connector ;
	public static TestIoHandler ioHandler;
	public static void main(String[] args) {
		try {
			connector= new NioSocketConnector();
			ioHandler=new TestIoHandler();
			connector.addFilter(new ClientInputFilter(2));
			connector.addFilter(new TestOutputFilter(2));
			connector.init();
			connector.connect("127.0.0.1",9000,ioHandler);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
