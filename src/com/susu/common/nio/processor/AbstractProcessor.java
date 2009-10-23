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
 * Socket�¼������࣬����д���رյ��¼��Ĵ���
 * @author zhjb
 *
 */
public abstract class AbstractProcessor {
	private static Logger logger  =  Logger.getLogger(AbstractProcessor.class );
	/**��ȡ���ݺ���Ҫִ�еĹ��������ϣ�����order��������*/
	private List<InputFilter> inputFilterList = new ArrayList<InputFilter>();
	
	/**��������ǰ��Ҫִ�еĹ��������ϣ�����order��������*/
	private List<OutputFilter> outputFilterList = new ArrayList<OutputFilter>();
	/**Socket��Session֮���ӳ���ϵ*/
	private Map<SocketChannel,Session> socketChannelSessionMap= new ConcurrentHashMap<SocketChannel,Session>();
	
	/**
	 * key��Socket��д��ʵ��ӳ���ϵ
	 * key�����ɹ���
	 * ���ڷ�����ʹ��,����������IP:���������ؼ����˿�
	 * ���ڿͻ���ʹ�ã����ӷ�����IP:���ӷ������˿�
	 */
	private Map<String, IoHandler> ioHanderMap= new ConcurrentHashMap<String,IoHandler>();
	
	/**���б�־*/
	private boolean isRunning=true;
	
	/**Socket�¼�������*/
	private ReentrantLock  eventQueueLock = new ReentrantLock(true);
	/**Socket�¼�����*/
	private ConcurrentLinkedQueue<Event> eventQueue= new ConcurrentLinkedQueue<Event>();
	/**Socket�¼�����������*/
	private Condition eventQueueCondition = eventQueueLock.newCondition();
	/**ҵ�����߳��� ��ʼ��ΪCPU�ĸ��������Ǻ���*/
	private Processor[] processors;
	
	/**Socket�¼�ѡ����*/
	private Selector selector;
	/**Socket�������߳�*/
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
	 * key�����ɹ���
	 * ���ڷ�����ʹ��,����������IP:���������ؼ����˿�
	 * ���ڿͻ���ʹ�ã����ӷ�����IP:���ӷ������˿�
	 * @param sc
	 * @return
	 */
	protected abstract String getKey(SocketChannel sc)throws Exception;
	
	/**
	 * ��ӹ�����
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
	 * ���socket�¼�
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
	 * ��ͬ��IP�Ͷ˿ڰ󶨲�ͬ��IO������
	 * @param key
	 * @param ioHandler
	 */
	public void bindIoHandler(String  key, IoHandler ioHandler){
		ioHanderMap.put(key, ioHandler);
	}
	
	/**
	 * �ر��ͷ���Դ
	 */
	public void close(){
		isRunning=false;
		Event event= new Event();
		//ģ��N���˳��¼������¼����У����ѵȴ�����ҵ����߳�
		event.setEventEnum(EventEnum.ON_QUIT);
		for(int i=0;i<processors.length;i++){
			addEventQueue(event);
		}
		
		//�ر����е�Socket����
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
		//�ͷ�����IOHander����Դ
		Iterator<String> iter = ioHanderMap.keySet().iterator();
		while(iter.hasNext()){
			try {
				ioHanderMap.get(iter.next()).destroy();
			} catch (Exception e) {
				logger.error(Utils.getExceptionStack(e));
			}
		}
		//�ͷ������������Դ
		for(Filter f:inputFilterList){
			try {
				f.destroy();
			} catch (Exception e) {
				logger.error(Utils.getExceptionStack(e));
			}
		}
		//�ͷ������������Դ
		for(Filter f:outputFilterList){
			try {
				f.destroy();
			} catch (Exception e) {
				logger.error(Utils.getExceptionStack(e));
			}
		}
	}
	/**
	 * ����Զ�̷�����
	 * @param address
	 * @return
	 * @throws CommException
	 */
	public Session connect(SocketAddress address) throws CommException{
		Session session=null;
		try {
			SocketChannel sc = SocketChannel.open(address);
			//����һ��Session�Ự ��һ��Socket
			session=onCreateSession(sc);
		} catch (Exception e) {
			String ex=Utils.getExceptionStack(e);
			logger.error(ex);
			throw new CommException(ErrorCode.NIO0001,ex);
		}
		
		return session;
	}
	/**
	 * �ر�һ��session
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
						skey.cancel(); //ȡ���¼�ע��
					}
					key.close();
					//����Session�ر�ʱ��
					Event event= new Event();
					event.setEventEnum(EventEnum.ON_CLOSE_SESSION);
					event.setData(key);
					//�����¼����еȴ�����
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
	 * ��ʼ��������
	 * @throws CommException
	 */
	public void  init()throws CommException{
		//����������
		FilterComparator filterComparator =new FilterComparator();
		Collections.sort(inputFilterList, filterComparator);
		Collections.sort(outputFilterList, filterComparator);
		
		logger.debug("��ʼ�������ݹ�����........");
		for(Filter f:inputFilterList){
			f.init();
		}
		logger.debug("��ʼ���������ݹ�����........");
		for(Filter f:outputFilterList){
			f.init();
		}
		logger.debug("�����������߳�........");
		readProcessor= new ReadProcessor(selector);
		readProcessor.start();
		
		logger.debug("����ҵ�����߳�........");
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
	 * ҵ���߳��¼��ַ�
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
	 * �������߳��¼��ַ�
	 * @param s
	 */
	private void doEvent(SelectionKey s){
		//Socket �ر��¼�
		if(!s.isValid()){
			logger.info("CloseSession");
			SocketChannel sc = (SocketChannel) s.channel();
			Event event= new Event();
			event.setEventEnum(EventEnum.ON_CLOSE_SESSION);
			event.setData(sc);
			addEventQueue(event);
		}else if (s.isValid() && s.isAcceptable()){//Socket���������¼�
			logger.info("onAccept");
			ServerSocketChannel ssc = (ServerSocketChannel) s.channel();
			accept(ssc);
		}else if (s.isValid() && s.isReadable() ){// ��ȡ�����¼�
			logger.info("onRead");
			SocketChannel sc = (SocketChannel) s.channel();
			if(sc.isOpen()){
				onRead(sc);
			}
			
		}else if (s.isValid() && s.isWritable()){// д�����¼���Socket����������ʱ����selector������
			logger.info("onWrite");
		}else{
			logger.info("�����¼�");
		}
	}
	/**
	 * �����������µ�Socket����
	 * @param ssc
	 */
	private void accept(ServerSocketChannel ssc){
		try {
			SocketChannel sc = ssc.accept();
			//�����¼������¼����еȴ�����
			Event event= new Event();
			event.setEventEnum(EventEnum.ON_CREATE_SESSION);
			event.setData(sc);
			addEventQueue(event);
		} catch (Exception e) {
			logger.error(Utils.getExceptionStack(e));
		}
		
	}
	/**
	 * ��Socket��ȡ���ݣ�������������¼������¼����У��ȴ�ҵ���¼��̴߳���
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
	 * ���յ����ݣ�ִ�й������������ݴ���
	 * @param session
	 */
	private void onReceiveMessage(Session session ){
		try {
			logger.debug("onReceiveMessage:��ʼִ�й�����");
			for(InputFilter f:inputFilterList){
				f.onReceiveMessage(session);
			}
			logger.debug("onReceiveMessage:ִ�й���������");
		} catch (Exception e) {
			logger.error(Utils.getExceptionStack(e));
		}
	}
	/**
	 *��������
	 * @param event
	 */
	private void onWrite(Event event){
		SocketChannel sc=null;
		try {
			Object[] obj=(Object[])event.getData();
			Session session=(Session)obj[0];
			Object data=obj[1];
			logger.debug("��ʼִ�й�����");
			for(OutputFilter f:outputFilterList){
				data=f.onSendMsg(data);
			}
			logger.debug("ִ�й���������");
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
				//��������Ժ�ע��������¼�
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
	 * ����һ��Socket����һ��Session�Ự
	 * @param sc
	 * @return
	 */
	private Session onCreateSession(SocketChannel sc){
		Session session=null;
		try {
			//���÷�������ʽ
			sc.configureBlocking(false);
			InetSocketAddress remote=(InetSocketAddress)sc.socket().getRemoteSocketAddress();
			session= new NioSession(
					sc.socket().getLocalAddress().getHostAddress(),
					sc.socket().getLocalPort(),
					remote.getAddress().getHostAddress(),
					remote.getPort(),
					this);
			//����Socket��Session��ӳ���ϵ
			socketChannelSessionMap.put(sc,session);
			this.registerEvent(session, SelectionKey.OP_READ);
			//���ù�����onCreateSession
			for(InputFilter f:inputFilterList){
				f.onCreateSession(session);
			}
			
		} catch (Exception e) {
			logger.error(Utils.getExceptionStack(e));
		}
		return session;
	}
	/**
	 * �ر�Socket
	 * @param sc
	 */
	private void onCloseSession(SocketChannel sc){
		try {
			
			Session session = socketChannelSessionMap.get(sc);
			//�Ƴ�Socket��Session��ӳ���ϵ
			socketChannelSessionMap.remove(sc);
			sc.close();
			//���ù�����onCloseSession
			for(InputFilter f:inputFilterList){
				f.onCloseSession(session);
			}
		} catch (Exception e) {
			logger.error(Utils.getExceptionStack(e));
		}
	}
	/**
	 * ע��Socket�¼�
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
	 * Socket�������߳�
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
				logger.info("ReadProcessor �˳�");
			} catch (Exception e) {
				logger.error(Utils.getExceptionStack(e));
			}
		}
	}
	/**
	 * ҵ�����߳�
	 * @author zhjb
	 *
	 */
	class Processor  extends Thread{
		public void run(){
			Event event=null;
			while(isRunning){
				eventQueueLock.lock();
				try {
					//�ȴ������������ݲ���
					while(eventQueue.isEmpty())
						eventQueueCondition.await();
					//�ж��Ƿ����У�����˳��߳�
					if(!isRunning){
						eventQueueLock.unlock();
						break;
					}
					//��ȫ���,�ж϶����Ƿ��
					if(eventQueue.isEmpty()){
						eventQueueLock.unlock();
						continue;
					}
					event = eventQueue.poll();
					eventQueueCondition.signalAll();//���������ĵȴ���������������
					
					//�����¼�
				} catch (Exception e) {
					eventQueueLock.unlock();
					logger.error(Utils.getExceptionStack(e));
				}finally{
					if(eventQueueLock.isLocked())
						eventQueueLock.unlock();
				}
				doEvent(event);
			}
			logger.info("Processor �˳�");
		}
	}
	
}