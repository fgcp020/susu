package com.susu.common.nio.connector;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.log4j.Logger;

import com.susu.common.CommException;
import com.susu.common.nio.filter.Filter;
import com.susu.common.nio.handler.IoHandler;
import com.susu.common.nio.processor.AbstractProcessor;
import com.susu.common.nio.processor.ClientNioSocketProcessor;
import com.susu.common.nio.session.Session;

/**
 * ��Ϊ�ͻ������ӷ�����
 * @author zhjb
 *
 */
public class NioSocketConnector {
	private static Logger logger  =  Logger.getLogger(NioSocketConnector. class );
	private ClientNioSocketProcessor processor=new ClientNioSocketProcessor();
	
	public void init() throws Exception{
		processor.init();
	}
	public AbstractProcessor getProcessor() {
		return processor;
	}
	/**
	 * ���ӷ�����
	 * @param hostName
	 * @param port
	 * @param ioHandler
	 * @return
	 * @throws CommException
	 */
	public Session connect(String hostName,int port,IoHandler ioHandler) throws CommException{
		processor.bindIoHandler(hostName+":"+port, ioHandler);
		SocketAddress address = new InetSocketAddress(hostName,port);
		return processor.connect(address);
	}
	/**
	 * ��ӹ���������������������
	 * @param filter
	 */
	public void addFilter(Filter filter){
		processor.addFilter(filter);
	}
	public void close() throws Exception{
		processor.close();
		logger.info("NioSocketConnector closed");
	}
}
