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
 * 一个简单的数据库连接池，采用单例模式
 * 1.初始化时与数据库建立一定数量的连接
 * 2.当数据库连接数达到最大值时不再建立新的数据库连接
 * 3.当目前的所有数据库连接都处于使用状态，且数量小于最大值，如果有新的请求就建立一个新的数据库连接
 * 4.如果发现有数据库连接与数据断开，那将把这个连接从连接池中移除
 * 5.当前数据库连接数介于最大值与最小值之间，释放比较空闲的数据库连接（4个小时没有被使用的数据库连接）
 * @author zhjb2000
 *
 */
public class DBConnectionPool implements TimerTask{
	
	private static Logger logger  =  Logger.getLogger(DBConnectionPool.class);
	
	/**
	 * 数据库连接集合
	 */
	private List<Connection> connList = new ArrayList<Connection>();
	
	private ReentrantLock  connListLock = new ReentrantLock(true);
	
	/**
	 * 每个数据库连接使用情况
	 */
	private Map<Connection,ConnectionInfo> connInfoMap= new ConcurrentHashMap<Connection,ConnectionInfo>();
	
	/**
	 * 默认最小初始化连接数
	 */
	private int minPoolSize=5;
	
	/**
	 * 默认最多连接数
	 */
	private int maxPoolSize=minPoolSize*5;
	
	/**
	 * 测试数据库是否正常连接的sql
	 */
	private String testConnSql="select 1 from dual";
	
	private boolean isInit=false;
	 
	private Boolean isTimerRunning=false;
	
	/**
	 * 驱动类
	 */
	private String driverClass;
	/**
	 * 数据库连接URL
	 */
	private String DBurl;
	/**
	 * 数据库用户名
	 */
	private String user;
	/**
	 * 数据库密码
	 */
	private String password;
	
	
	private static DBConnectionPool instance;
	
	private DBConnectionPool(){}
	
	/**
	 * 初始化数据库连接池
	 * @throws CommException
	 */
	private void init()throws CommException{
		logger.debug("初始化数据库连接池........");
		if(driverClass==null){
			throw new CommException(ErrorCode.DBO0001,"未设置数据驱动");
		}else if(DBurl==null){
			throw new CommException(ErrorCode.DBO0002,"未设置数据库URL");
		}else if(user==null){
			throw new CommException(ErrorCode.DBO0002,"未设置数据库用户名");
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
					logger.debug("启动重连数据库的定时器");
				}
			}
			
			throw new CommException(ErrorCode.DBO0000,ex);
		}
		TimerManager.getCursoryInstance().addTimer("killer", this, 1000*60*60, 1000*60*10);
		logger.debug("初始化数据库连接池结束........");
	}
	/**
	 * 新建一个数据库连接
	 * @return
	 * @throws CommException
	 */
	private Connection buildNewConnection() throws CommException{
		int count=connList.size();
		if(count>=maxPoolSize){
			throw new CommException(ErrorCode.DBO0004,"已经达到配置的数据库最大的链接数");
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
	 * 检查数据库连接是否处于正常
	 * @param conn
	 * @throws Exception
	 */
	private void checkConnection(Connection conn) throws Exception{
		conn.createStatement().executeQuery(testConnSql);
	}
	/**
	 * 获取数据库连接池实例
	 * @return
	 */
	public static synchronized DBConnectionPool getInstance(){
		if(instance==null){
			instance= new DBConnectionPool();
		}
		return instance;
	}
	/**
	 * 关闭数据库连接池
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
	 * 初始化数据库连接池
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
	 * 初始化数据库连接池
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
	 * 获取一个空闲的数据库连接
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
				//是否需要在每个连接使用之前检查与数据库的连接状态，如果这个有做，可能会影响系统性能
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
						logger.debug("启动重连数据库的定时器");
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
						logger.debug("启动重连数据库的定时器");
					}
				}
			}
		}
		return connection;
	}
	/**
	 * 释放一个数据库连接
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
	 * 数据库连接使用信息类
	 * @author zhjb2000
	 *
	 */
	class ConnectionInfo{
		/**
		 * 使用次数
		 */
		long usedCount;
		/**
		 * 上次使用时间
		 */
		long lastTime;
		
		/**
		 * 是否正在使用
		 */
		boolean isUseing;
		
	}
	/**
	 * 定时对数据库连接状态进行检查
	 * 
	 */
	public void onTimeOut(String key) {

		if(DBurl.equals(key)){//定时重连数据库
			if(connList.size()>=minPoolSize){
				synchronized (isTimerRunning) {
					if(!isTimerRunning){
						isTimerRunning=false;
						TimerManager.getCursoryInstance().removeTimer(DBurl);
						logger.debug("取消连数据库的定时器");
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
		}else if("killer".equals(key)) {  //定时检查Connection的数量，把不繁忙的Connection释放掉
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
