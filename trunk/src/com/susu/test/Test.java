package com.susu.test;

import com.susu.common.nio.connector.NioSocketConnector;
import com.susu.common.nio.session.Session;
import com.susu.test.nio.simple.ClientInputFilter;
import com.susu.test.nio.simple.ClinetOutputFilter;
import com.susu.test.nio.simple.TestIoHandler;



public class Test {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		 NioSocketConnector connector ;
		 TestIoHandler ioHandler=null;
		 connector = new NioSocketConnector();
			
		ioHandler=new TestIoHandler();
		connector.addFilter(new ClientInputFilter(11));
		connector.addFilter(new ClinetOutputFilter(11));
		connector.init();
		Session session = connector.connect("168.33.113.175",17001,ioHandler);
		session.wirte("ConnectTest");
		Thread.sleep(1000*4);
		//session.close();
		connector.close();
	}

}
