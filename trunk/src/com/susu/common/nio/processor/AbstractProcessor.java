package com.susu.common.nio.processor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.susu.common.CommException;
import com.susu.common.ErrorCode;
import com.susu.common.nio.filter.Filter;
import com.susu.common.nio.filter.FilterComparator;
import com.susu.common.nio.filter.InputFilter;
import com.susu.common.nio.filter.OutputFilter;
import com.susu.common.nio.handler.IoHandler;
import com.susu.common.nio.session.NioSession;
import com.susu.common.nio.session.Session;
import com.susu.common.util.Utils;


/**
 * Socket事件处理类，读、写、关闭等事件的处理
 * @author zhjb
 *
 */
public abstract class AbstractProcessor {
	private static Logger logger  =  Logger.getLogger(AbstractProcessor.class );
	/**读取数据后需要执行的过滤器集合，按照order进行排序*/
	private List<InputFilter> inputFilterList = new ArrayList<InputFilter>();
	
	/**发送数据前需要执行的过滤器集合，按照order进行排序*/
	private List<OutputFilter> outputFilterList = new ArrayList<OutputFilter>();
	/**Socket与Session之间的映射关系*/
	private Map<SocketChannel,Session> socketChannelSessionMap= new ConcurrentHashMap<SocketChannel,Session>();
	
	/**
	 * key与Socket读写类实例映射关系
	 * key的生成规则：
	 * 对于服务器使用,服务器本地IP:服务器本地监听端口
	 * 对于客户端使用，连接服务器IP:连接服务器端口
	 */
	private Map<String, IoHandler> ioHanderMap= new ConcurrentHashMap<String,IoHandler>();
	
	/**运行标志*/
	private boolean isRunning=true;
	
	/**Socket事件队列锁*/
	private ReentrantLock  eventQueueLock = new ReentrantLock(true);
	/**Socket事件队列*/
	private ConcurrentLinkedQueue<Event> eventQueue= new ConcurrentLinkedQueue<Event>();
	/**Socket事件队列锁条件*/
	private Condition eventQueueCondition = eventQueueLock.newCondition();
	/**业务处理线程组 初始化为CPU的个数或者是核数*/
	private Processor[] processors;
	
	/**Socket事件选择器*/
	private Selector selector;
	/**Socket读数据线程*/
	private ReadProcessor readProcessor;
	
	
	public AbstractProcessor(){
		try {
			selector=Selector.open();
		} catch (IOException e) {
			logger.error(Utils.getExceptionStack(e));
			throw new RuntimeException("open Selector failed");
		}
	}
	
	/**
	 * key的生成规则：
	 * 对于服务器使用,服务器本地IP:服务器本地监听端口
	 * 对于客户端使用，连接服务器IP:连接服务器端口
	 * @param sc
	 * @return
	 */
	protected abstract String getKey(SocketChannel sc)throws Exception;
	
	/**
	 * 添加过滤器
	 * @param filter
	 */
	public void addFilter(Filter filter){
		
		if(filter instanceof InputFilter){
			inputFilterList.add((InputFilter)filter);
		}else if(filter instanceof OutputFilter){
			outputFilterList.add((OutputFilter)filter);
		}
			
	}
	/**
	 * 添加socket事件
	 * @param event
	 */
	public void addEventQueue(Event event){
		eventQueueLock.lock();
		try {
			eventQueue.add(event);
			eventQueueCondition.signal();
		} catch (Exception e) {
			logger.error(Utils.getExceptionStack(e));
		}finally{
			eventQueueLock.unlock();
		}
		
		
	}
	/**
	 * 不同的IP和端口绑定不同的IO处理器
	 * @param key
	 * @param ioHandler
	 */
	public void bindIoHandler(String  key, IoHandler ioHandler){
		ioHanderMap.put(key, ioHandler);
	}
	
	/**
	 * 关闭释放资源
	 */
	public void close(){
		isRunning=false;
		Event event= new Event();
		//模拟N个退出事件放入事件队列，唤醒等待处理业务的线程
		event.setEventEnum(EventEnum.ON_QUIT);
		for(int i=0;i<processors.length;i++){
			addEventQueue(event);
		}
		
		//关闭所有的Socket连接
		for(SocketChannel sc: socketChannelSessionMap.keySet()){
			try {
				if(sc.isOpen()){
					sc.close();
					onCloseSession(sc);
				}
			} catch (Exception e) {
				logger.error(Utils.getExceptionStack(e));
			}
		}
		//释放所有IOHander的资源
		Iterator<String> iter = ioHanderMap.keySet().iterator();
		while(iter.hasNext()){
			try {
				ioHanderMap.get(iter.next()).destroy();
			} catch (Exception e) {
				logger.error(Utils.getExceptionStack(e));
			}
		}
		//释放输入过滤器资源
		for(Filter f:inputFilterList){
			try {
				f.destroy();
			} catch (Exception e) {
				logger.error(Utils.getExceptionStack(e));
			}
		}
		//释放输出过滤器资源
		for(Filter f:outputFilterList){
			try {
				f.destroy();
			} catch (Exception e) {
				logger.error(Utils.getExceptionStack(e));
			}
		}
	}
	/**
	 * 连接远程服务器
	 * @param address
	 * @return
	 * @throws CommException
	 */
	public Session connect(SocketAddress address) throws CommException{
		Session session=null;
		try {
			SocketChannel sc = SocketChannel.open(address);
			//创建一个Session会话 绑定一个Socket
			session=onCreateSession(sc);
		} catch (Exception e) {
			String ex=Utils.getExceptionStack(e);
			logger.error(ex);
			throw new CommException(ErrorCode.NIO0001,ex);
		}
		
		return session;
	}
	/**
	 * 关闭一个session
	 * @param session
	 * @throws CommException
	 */
	public void closeSession(Session session) throws CommException{
		try {
			Iterator<SocketChannel> iter = socketChannelSessionMap.keySet().iterator();
			while(iter.hasNext()){
				SocketChannel key=iter.next();
				if(session.equals(socketChannelSessionMap.get(key))){
					SelectionKey skey =key.keyFor(selector);
					if(skey!=null){ 
						skey.cancel(); //取消事件注册
					}
					key.close();
					//构造Session关闭时间
					Event event= new Event();
					event.setEventEnum(EventEnum.ON_CLOSE_SESSION);
					event.setData(key);
					//加入事件队列等待处理
					addEventQueue(event);
					break;
				}
			}
		} catch (Exception e) {
			String ex= Utils.getExceptionStack(e);
			logger.error(ex);
			throw new CommException(ErrorCode.NIO0006,ex);
		}
	}
	/**
	 * 初始化处理器
	 * @throws CommException
	 */
	public void  init()throws CommException{
		//过滤器排序
		FilterComparator filterComparator =new FilterComparator();
		Collections.sort(inputFilterList, filterComparator);
		Collections.sort(outputFilterList, filterComparator);
		
		logger.debug("初始化读数据过滤器........");
		for(Filter f:inputFilterList){
			f.init();
		}
		logger.debug("初始化发送数据过滤器........");
		for(Filter f:outputFilterList){
			f.init();
		}
		logger.debug("启动读数据线程........");
		readProcessor= new ReadProcessor(selector);
		readProcessor.start();
		
		logger.debug("启动业务处理线程........");
		processors= new Processor[Runtime.getRuntime().availableProcessors()];
		for (int i = 0; i < processors.length; i++) {
			processors[i]= new Processor();
			processors[i].start();
		}
		
	}
	
	public Selector getSelector() {
		return selector;
	}
	/**
	 * 业务线程事件分发
	 * @param event
	 */
	private void doEvent(Event event){
		if(event!=null){
			switch(event.getEventEnum()){
				case ON_CREATE_SESSION:
					onCreateSession((SocketChannel)event.getData());
					break;
				case ON_RECEIVE_MESSAGE: 
					Session session=(Session)event.getData();
					onReceiveMessage(session);
					break;
				case ON_SEND_MSG: 
					onWrite(event);
					break;
				case ON_CLOSE_SESSION: 
					onCloseSession((SocketChannel)event.getData());
					break;
				case ON_QUIT: 
					break;
			}
		}
	}
	/**
	 * 读数据线程事件分发
	 * @param s
	 */
	private void doEvent(SelectionKey s){
		//Socket 关闭事件
		if(!s.isValid()){
			logger.info("CloseSession");
			SocketChannel sc = (SocketChannel) s.channel();
			Event event= new Event();
			event.setEventEnum(EventEnum.ON_CLOSE_SESSION);
			event.setData(sc);
			addEventQueue(event);
		}else if (s.isValid() && s.isAcceptable()){//Socket建立连接事件
			logger.info("onAccept");
			ServerSocketChannel ssc = (ServerSocketChannel) s.channel();
			accept(ssc);
		}else if (s.isValid() && s.isReadable() ){// 读取数据事件
			logger.info("onRead");
			SocketChannel sc = (SocketChannel) s.channel();
			if(sc.isOpen()){
				onRead(sc);
			}
			
		}else if (s.isValid() && s.isWritable()){// 写数据事件，Socket发送数据暂时不用selector来处理
			logger.info("onWrite");
		}else{
			logger.info("其他事件");
		}
	}
	/**
	 * 服务器接收新的Socket连接
	 * @param ssc
	 */
	private void accept(ServerSocketChannel ssc){
		try {
			SocketChannel sc = ssc.accept();
			//构造事件放入事件队列等待处理
			Event event= new Event();
			event.setEventEnum(EventEnum.ON_CREATE_SESSION);
			event.setData(sc);
			addEventQueue(event);
		} catch (Exception e) {
			logger.error(Utils.getExceptionStack(e));
		}
		
	}
	/**
	 * 从Socket读取数据，构造接收数据事件放入事件队列，等待业务事件线程处理
	 * @param sc
	 */
	private void onRead(SocketChannel sc){
		try {
			String key=getKey(sc);
			IoHandler handler=ioHanderMap.get(key);
			Object msg=handler.read(sc);
			Session session = socketChannelSessionMap.get(sc);
			this.registerEvent(session, SelectionKey.OP_READ);
			session.setReceiveMessage(msg);
			Event event= new Event();
			event.setEventEnum(EventEnum.ON_RECEIVE_MESSAGE);
			event.setData(session);
			addEventQueue(event);
		}catch(CommException e){
			if(e.getErrorCode().equals(ErrorCode.NIO0002)){
				onCloseSession(sc);
			}
			
		}catch(Exception e){
			onCloseSession(sc);
			logger.error(Utils.getExceptionStack(e));
		}
	}
	/**
	 * 接收到数据，执行过滤器进行数据处理
	 * @param session
	 */
	private void onReceiveMessage(Session session ){
		try {
			logger.debug("onReceiveMessage:开始执行过滤器");
			for(InputFilter f:inputFilterList){
				f.onReceiveMessage(session);
			}
			logger.debug("onReceiveMessage:执行过滤器结束");
		} catch (Exception e) {
			logger.error(Utils.getExceptionStack(e));
		}
	}
	/**
	 *发送数据
	 * @param event
	 */
	private void onWrite(Event event){
		SocketChannel sc=null;
		try {
			Object[] obj=(Object[])event.getData();
			Session session=(Session)obj[0];
			Object data=obj[1];
			logger.debug("开始执行过滤器");
			for(OutputFilter f:outputFilterList){
				data=f.onSendMsg(data);
			}
			logger.debug("执行过滤器结束");
			Iterator<SocketChannel> iter = socketChannelSessionMap.keySet().iterator();
			while(iter.hasNext()){
				SocketChannel key=iter.next();
				if(session.equals(socketChannelSessionMap.get(key))){
					sc=key;
					break;
				}
			}
			if(sc!=null){
				String  key= getKey(sc);
				IoHandler handler=ioHanderMap.get(key);
				if(sc.isOpen()){
					handler.wirte(sc, data);
				}
				//发送完成以后注册读数据事件
				registerEvent(session,  SelectionKey.OP_READ);
			}
		}catch(CommException e) {
			if(e.getErrorCode().equals(ErrorCode.NIO0003)){
				onCloseSession(sc);
			}
			logger.error(e.toString());
		}catch (Exception e) {
			onCloseSession(sc);
			logger.error(Utils.getExceptionStack(e));
		}
		
	}
	/**
	 * 根据一个Socket创建一个Session会话
	 * @param sc
	 * @return
	 */
	private Session onCreateSession(SocketChannel sc){
		Session session=null;
		try {
			//配置非阻塞方式
			sc.configureBlocking(false);
			InetSocketAddress remote=(InetSocketAddress)sc.socket().getRemoteSocketAddress();
			session= new NioSession(
					sc.socket().getLocalAddress().getHostAddress(),
					sc.socket().getLocalPort(),
					remote.getAddress().getHostAddress(),
					remote.getPort(),
					this);
			//建立Socket与Session的映射关系
			socketChannelSessionMap.put(sc,session);
			this.registerEvent(session, SelectionKey.OP_READ);
			//调用过滤器onCreateSession
			for(InputFilter f:inputFilterList){
				f.onCreateSession(session);
			}
			
		} catch (Exception e) {
			logger.error(Utils.getExceptionStack(e));
		}
		return session;
	}
	/**
	 * 关闭Socket
	 * @param sc
	 */
	private void onCloseSession(SocketChannel sc){
		try {
			
			Session session = socketChannelSessionMap.get(sc);
			//移除Socket与Session的映射关系
			socketChannelSessionMap.remove(sc);
			sc.close();
			//调用过滤器onCloseSession
			for(InputFilter f:inputFilterList){
				f.onCloseSession(session);
			}
		} catch (Exception e) {
			logger.error(Utils.getExceptionStack(e));
		}
	}
	/**
	 * 注册Socket事件
	 * @param session
	 * @param event
	 * @throws IOException
	 */
	public void registerEvent(Session session,int event) throws IOException{
	
		Iterator<SocketChannel> iter = socketChannelSessionMap.keySet().iterator();
		while(iter.hasNext()){
			SocketChannel key=iter.next();
			if(key.isOpen()&&session.equals(socketChannelSessionMap.get(key))){
				key.register(selector,event);
				selector.wakeup();
				break;
			}
		}
		
	}
	/**
	 * Socket读数据线程
	 * @author zhjb
	 *
	 */
	class ReadProcessor extends Thread{
		private Selector selector;
		public ReadProcessor(Selector selector){
			this.selector=selector;
		}
		public void run(){
			try {
				boolean isWorking;
				while(isRunning){
					isWorking=false;
					int selected =selector.select(2000);
					if(selected>0){
						Set<SelectionKey> keySet = selector.selectedKeys();
						if(!keySet.isEmpty()){
							isWorking=true;
							for(SelectionKey key:keySet){
								doEvent(key);
							}
							keySet.clear();
						}
					}
					if(!isWorking){
						Thread.sleep(5);
					}
				}
				logger.info("ReadProcessor 退出");
			} catch (Exception e) {
				logger.error(Utils.getExceptionStack(e));
			}
		}
	}
	/**
	 * 业务处理线程
	 * @author zhjb
	 *
	 */
	class Processor  extends Thread{
		public void run(){
			Event event=null;
			while(isRunning){
				eventQueueLock.lock();
				try {
					//等待队列中有数据插入
					while(eventQueue.isEmpty())
						eventQueueCondition.await();
					//判断是否运行，否就退出线程
					if(!isRunning){
						eventQueueLock.unlock();
						break;
					}
					//安全检查,判断队列是否空
					if(eventQueue.isEmpty()){
						eventQueueLock.unlock();
						continue;
					}
					event = eventQueue.poll();
					eventQueueCondition.signalAll();//唤醒其他的等待该条件的条件锁
					
					//处理事件
				} catch (Exception e) {
					eventQueueLock.unlock();
					logger.error(Utils.getExceptionStack(e));
				}finally{
					if(eventQueueLock.isLocked())
						eventQueueLock.unlock();
				}
				doEvent(event);
			}
			logger.info("Processor 退出");
		}
	}
	
}