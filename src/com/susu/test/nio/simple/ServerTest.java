package com.susu.test.nio.simple;

import com.susu.common.nio.acceptor.NioSocketAcceptor;

/**
 * �������˲����࣬�ͻ���һ���ӵ����������͸��ͻ��˷���һ������
 * @author zhjb
 */

public class ServerTest {
	
	public static void main(String [] s){
		
		try {
			NioSocketAcceptor acceptor= new  NioSocketAcceptor();
			TestIoHandler handler = new TestIoHandler();
			acceptor.bind("127.0.0.1", 9000,handler);
			acceptor.addFilter(new ServerInputFilter(1));
			acceptor.addFilter(new ServerOutputFilter(1));
			acceptor.startupAcceptor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
