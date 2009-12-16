package com.susu.common.db.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.susu.common.CommException;
import com.susu.common.ErrorCode;
import com.susu.common.timer.TimerManager;
import com.susu.common.timer.TimerTask;
import com.susu.common.util.Utils;

/**
 * һ���򵥵����ݿ����ӳأ����õ���ģʽ
 * 1.��ʼ��ʱ�����ݿ⽨��һ������������
 * 2.�����ݿ��������ﵽ���ֵʱ���ٽ����µ����ݿ�����
 * 3.��Ŀǰ���������ݿ����Ӷ�����ʹ��״̬��������С�����ֵ��������µ�����ͽ���һ���µ����ݿ�����
 * 4.������������ݿ����������ݶϿ����ǽ���������Ӵ����ӳ����Ƴ�
 * 5.��ǰ���ݿ��������������ֵ����Сֵ֮�䣬�ͷűȽϿ��е����ݿ����ӣ�4��Сʱû�б�ʹ�õ����ݿ����ӣ�
 * @author zhjb2000
 *
 */
public class DBConnectionPool implements TimerTask{
	
	private static Logger logger  =  Logger.getLogger(DBConnectionPool.class);
	
	/**
	 * ���ݿ����Ӽ���
	 */
	private List<Connection> connList = new ArrayList<Connection>();
	
	private ReentrantLock  connListLock = new ReentrantLock(true);
	
	/**
	 * ÿ�����ݿ�����ʹ�����
	 */
	private Map<Connection,ConnectionInfo> connInfoMap= new ConcurrentHashMap<Connection,ConnectionInfo>();
	
	/**
	 * Ĭ����С��ʼ��������
	 */
	private int minPoolSize=5;
	
	/**
	 * Ĭ�����������
	 */
	private int maxPoolSize=minPoolSize*5;
	
	/**
	 * �������ݿ��Ƿ��������ӵ�sql
	 */
	private String testConnSql="select 1 from dual";
	
	private boolean isInit=false;
	 
	private Boolean isTimerRunning=false;
	
	/**
	 * ������
	 */
	private String driverClass;
	/**
	 * ���ݿ�����URL
	 */
	private String DBurl;
	/**
	 * ���ݿ��û���
	 */
	private String user;
	/**
	 * ���ݿ�����
	 */
	private String password;
	
	
	private static DBConnectionPool instance;
	
	private DBConnectionPool(){}
	
	/**
	 * ��ʼ�����ݿ����ӳ�
	 * @throws CommException
	 */
	private void init()throws CommException{
		logger.debug("��ʼ�����ݿ����ӳ�........");
		if(driverClass==null){
			throw new CommException(ErrorCode.DBO0001,"δ������������");
		}else if(DBurl==null){
			throw new CommException(ErrorCode.DBO0002,"δ�������ݿ�URL");
		}else if(user==null){
			throw new CommException(ErrorCode.DBO0002,"δ�������ݿ��û���");
		}
		
		try {
			Class.forName(driverClass);
			for(;connList.size()<minPoolSize;){
				buildNewConnection();
			}
			isInit=true;
			
		} catch (Exception e) {
			if(connListLock.isLocked()){
				connListLock.unlock();
			}
			String ex=Utils.getExceptionStack(e);
			logger.error(ex);
			synchronized (isTimerRunning) {
				if(!isTimerRunning){
					isTimerRunning=true;
					TimerManager.getCursoryInstance().addTimer(DBurl, this, 1000*5, 0);
					logger.debug("�����������ݿ�Ķ�ʱ��");
				}
			}
			
			throw new CommException(ErrorCode.DBO0000,ex);
		}
		TimerManager.getCursoryInstance().addTimer("killer", this, 1000*60*60, 1000*60*10);
		logger.debug("��ʼ�����ݿ����ӳؽ���........");
	}
	/**
	 * �½�һ�����ݿ�����
	 * @return
	 * @throws CommException
	 */
	private Connection buildNewConnection() throws CommException{
		int count=connList.size();
		if(count>=maxPoolSize){
			throw new CommException(ErrorCode.DBO0004,"�Ѿ��ﵽ���õ����ݿ�����������");
		}
		Connection conn=null;
		try {
			
			conn = DriverManager.getConnection(DBurl,user,password);
			checkConnection(conn);
			ConnectionInfo connInfo= new ConnectionInfo();
			connInfo.isUseing=false;
			connInfo.lastTime=0;
			connInfo.usedCount=0;
			connListLock.lock();
			connList.add(conn);
			connInfoMap.put(conn, connInfo);
			connListLock.unlock();
		} catch (Exception e) {
			if(connListLock.isLocked()){
				connListLock.unlock();
			}
			String ex=Utils.getExceptionStack(e);
			logger.error(ex);
			throw new CommException(ErrorCode.DBO0000,ex);
		}
		
		return conn;
	}
	/**
	 * ������ݿ������Ƿ�������
	 * @param conn
	 * @throws Exception
	 */
	private void checkConnection(Connection conn) throws Exception{
		conn.createStatement().executeQuery(testConnSql);
	}
	/**
	 * ��ȡ���ݿ����ӳ�ʵ��
	 * @return
	 */
	public static synchronized DBConnectionPool getInstance(){
		if(instance==null){
			instance= new DBConnectionPool();
		}
		return instance;
	}
	/**
	 * �ر����ݿ����ӳ�
	 */
	public void close(){
		for(Connection conn:connList){
			try {
				conn.close();
			} catch (SQLException e) {
				logger.error(Utils.getExceptionStack(e));
			}
		}
		connInfoMap.clear();
		TimerManager.getCursoryInstance().removeTimer("killer");
		TimerManager.getCursoryInstance().close();
	}
	/**
	 * ��ʼ�����ݿ����ӳ�
	 * @param minPoolSize
	 * @param maxPoolSize
	 * @param driverClass
	 * @param DBurl
	 * @param user
	 * @param password
	 * @param testTable
	 * @throws CommException
	 */
	public void init(
			int minPoolSize,
			int maxPoolSize,
			String driverClass,
			String DBurl,
			String user,
			String password,
			String testTable)  throws CommException {
		
		this.minPoolSize=minPoolSize;
		this.maxPoolSize=maxPoolSize;
		this.testConnSql="select 1 from "+testTable+" where 1=2";
		this.driverClass=driverClass;
		this.DBurl=DBurl;
		this.user=user;
		this.password=password;
		init();
	}
	/**
	 * ��ʼ�����ݿ����ӳ�
	 * @param driverClass
	 * @param DBurl
	 * @param user
	 * @param password
	 * @param testTable
	 * @throws CommException
	 */
	public void init(
			String driverClass,
			String DBurl,
			String user,
			String password,
			String testTable)  throws CommException {
		
		this.testConnSql="select 1 from "+testTable+" where 1=2";
		this.driverClass=driverClass;
		this.DBurl=DBurl;
		this.user=user;
		this.password=password;
		init();
	}
	/**
	 * ��ȡһ�����е����ݿ�����
	 * @return
	 * @throws CommException
	 */
	public Connection getConnection() throws CommException{
		if(!isInit){
			init();
		}
		Connection connection=null;
		boolean isException=false;
		connListLock.lock();
		Iterator<Connection> iter = connList.iterator();
		while(iter.hasNext()){
			Connection conn=iter.next();
			if(connInfoMap.get(conn).isUseing){
				continue;
			}
			try {
				//�Ƿ���Ҫ��ÿ������ʹ��֮ǰ��������ݿ������״̬�����������������ܻ�Ӱ��ϵͳ����
				checkConnection(conn);
				connection=conn;
				connInfoMap.get(conn).isUseing=true;
				connInfoMap.get(conn).usedCount++;
				connInfoMap.get(conn).lastTime=System.currentTimeMillis();
				break;
			} catch (Exception e) {
				connInfoMap.remove(conn);
				iter.remove();
				isException=true;
				logger.error(Utils.getExceptionStack(e));
			}
		}
		connListLock.unlock();
		try {
			if(connection==null&&!isException){
				connection=buildNewConnection();
				
			}else if(connection==null&&isException){
				connection=buildNewConnection();
				synchronized (isTimerRunning) {
					if(!isTimerRunning){
						isTimerRunning=true;
						TimerManager.getCursoryInstance().addTimer(DBurl, this, 1000*5, 0);
						logger.debug("�����������ݿ�Ķ�ʱ��");
					}
				}
			}
			connInfoMap.get(connection).isUseing=true;
			connInfoMap.get(connection).usedCount++;
			connInfoMap.get(connection).lastTime=System.currentTimeMillis();
		} catch (CommException e) {
			logger.error(e);
			if(e.getErrorCode().equals(ErrorCode.DBO0004)){
				throw e;
			}else{
				synchronized (isTimerRunning) {
					if(!isTimerRunning){
						isTimerRunning=true;
						TimerManager.getCursoryInstance().addTimer(DBurl, this, 1000*5, 0);
						logger.debug("�����������ݿ�Ķ�ʱ��");
					}
				}
			}
		}
		return connection;
	}
	/**
	 * �ͷ�һ�����ݿ�����
	 * @param conn
	 */
	public void freeConnection(Connection conn){
		try {
			connListLock.lock();
			connInfoMap.get(conn).isUseing=false;
			connListLock.unlock();
		} catch (Exception e) {
			if(connListLock.isLocked()){
				connListLock.unlock();
			}
			logger.error(Utils.getExceptionStack(e));
		}
		
	}
	
	/**
	 * ���ݿ�����ʹ����Ϣ��
	 * @author zhjb2000
	 *
	 */
	class ConnectionInfo{
		/**
		 * ʹ�ô���
		 */
		long usedCount;
		/**
		 * �ϴ�ʹ��ʱ��
		 */
		long lastTime;
		
		/**
		 * �Ƿ�����ʹ��
		 */
		boolean isUseing;
		
	}
	/**
	 * ��ʱ�����ݿ�����״̬���м��
	 * 
	 */
	public void onTimeOut(String key) {

		if(DBurl.equals(key)){//��ʱ�������ݿ�
			if(connList.size()>=minPoolSize){
				synchronized (isTimerRunning) {
					if(!isTimerRunning){
						isTimerRunning=false;
						TimerManager.getCursoryInstance().removeTimer(DBurl);
						logger.debug("ȡ�������ݿ�Ķ�ʱ��");
					}
				}
			}
			try {
				for(;connList.size()<minPoolSize;){
					buildNewConnection();
				}
			} catch (Exception e) {
				logger.error(Utils.getExceptionStack(e));
			}
		}else if("killer".equals(key)) {  //��ʱ���Connection���������Ѳ���æ��Connection�ͷŵ�
			try {
				connListLock.unlock();
				while(connList.size()>minPoolSize){
					Connection conn = connList.get(connList.size()-1);
					if(conn!=null){
						ConnectionInfo temp = connInfoMap.get(conn);
						if(!temp.isUseing&&(System.currentTimeMillis()-temp.lastTime)>1000*60*60*4){
							connInfoMap.remove(conn);
							conn.close();
						}
					}
					
				}
			} catch (Exception e) {
				
			}
		}
	}
}
