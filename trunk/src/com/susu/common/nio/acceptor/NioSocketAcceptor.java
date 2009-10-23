package com.susu.common.nio.acceptor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.susu.common.CommException;
import com.susu.common.ErrorCode;
import com.susu.common.nio.filter.Filter;
import com.susu.common.nio.handler.IoHandler;
import com.susu.common.nio.processor.ServerNioSocketProcessor;
import com.susu.common.util.Utils;

/**
 * 接受器单例模式，接受socket的接连，然后把socket交给Processor进行处理
 * @author zhjb
 *
 */
public class NioSocketAcceptor {
	private static Logger logger  =  Logger.getLogger(NioSocketAcceptor. class );
	
	/**
	 * 作为服务器监听的端口与SocketServer的映射关系
	 */
	private ConcurrentHashMap<Integer,ServerInfo> serverMap = new ConcurrentHashMap<Integer,ServerInfo>(); 
	private ReentrantLock  lock = new ReentrantLock(true);
	/**
	 * socket处理类
	 */
	private ServerNioSocketProcessor processor=new ServerNioSocketProcessor();
	
	public void close() throws Exception{
		lock.lock();
		try {
			Iterator<Integer> iter = serverMap.keySet().iterator();
			while(iter.hasNext()){
				try {
					ServerInfo temp = serverMap.get(iter.next());
					if(temp!=null&&temp.getServerSocketChannel()!=null){
						temp.getServerSocketChannel().close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			serverMap.clear();
			processor.close();
			Thread.sleep(1000*2);
		} catch (Exception e) {
			String es=Utils.getExceptionStack(e);
			logger.info(es);
		}finally{
			lock.unlock();
		}
	}
	/**
	 * 添加过滤器，输入和输出过滤器
	 * @param filter
	 */
	public void addFilter(Filter filter){
		processor.addFilter(filter);
	}
	/**
	 * 绑定服务器上某个端口读取报文和发送报文处理类
	 * @param ip
	 * @param port
	 * @param ioHandler
	 * @throws CommException
	 */
	public void bind(String ip,int port,IoHandler ioHandler)throws CommException{
		try {
			if(serverMap.get(port)==null){
				ServerInfo serverInfo= new ServerInfo();
				
				ServerSocketChannel ssc  = ServerSocketChannel.open();
				InetSocketAddress address = new InetSocketAddress(ip,port);
//				ssc.socket().setReuseAddress(true);
				//绑定端口
				ssc.socket().bind(address);
				//设置为异步模式
				ssc.configureBlocking(false);
				serverInfo.setIp(ip);
				serverInfo.setPort(port);
				serverInfo.setServerSocketChannel(ssc);
				
				lock.lock();
				try {
					serverMap.put(port, serverInfo);
					//注册socket事件
					ssc.register(processor.getSelector(), SelectionKey.OP_ACCEPT);
				} catch (Exception e) {
					String es=Utils.getExceptionStack(e);
					logger.info(es);
				}finally{
					lock.unlock();
				}
				
				
				
				//绑定SocketIO处理类
				processor.bindIoHandler(ip+":"+port, ioHandler);
			}else{
				logger.info("["+ip+":"+port+"]已经被监听");
				throw new CommException(ErrorCode.NIO0000,"["+ip+":"+port+"]已经被监听");
			}
		} catch (Exception e) {
			String es=Utils.getExceptionStack(e);
			logger.info(es);
			throw new CommException(ErrorCode.NIO0000,es);
		}
		
		
	}
	
	public  void  startupAcceptor()throws CommException {
		processor.init();
	}
}
