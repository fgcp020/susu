package com.susu.common.timer;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.susu.common.util.Utils;



/**
 * 统一Timer管理器
 */
public class TimerManager extends Thread{
	private static Logger logger  =  Logger.getLogger(TimerManager.class);
	/** 高精准的单例 */
	private static TimerManager instanceNicety = null;
	
	/** 粗略的单例 */
	private static TimerManager instanceCursory = null;
	
	/**存放的定时器任务列表*/
	private Map<String ,TimerTaskItem> timerTaskMap= new ConcurrentHashMap<String,TimerTaskItem>();
	
	/** 是否运行 */
	protected boolean isRunning = true;
	
	/**
	 * 构造函数
	 */
	private TimerManager(){}
	
	/**
	 * 获得高精准的Timer管理器的实例
	 */
	public static synchronized TimerManager getNicetyInstance(){
		if ( instanceNicety == null ){
			instanceNicety = new TimerManager();
			instanceNicety.setName("TimerManager.Nicety");
			instanceNicety.setPriority(4);
			instanceNicety.start();
		}
		return instanceNicety;
	}
	
	/**
	 * 获得粗略Timer管理器的实例
	 */
	public static synchronized TimerManager getCursoryInstance(){
		if ( instanceCursory == null ){
			instanceCursory = new TimerManager();
			instanceCursory.setName("GessTimerManager.Cursory");
			instanceCursory.setPriority(4);
			instanceCursory.start();
		}
		return instanceCursory;
	}
	
	/**
	 * 添加定时器
	 * @param key        		任务的类的key
	 * @param timerTask         执行定时任务的类
	 * @param periodTime       执行各后续任务之间的时间间隔（毫秒）
	 * @param startDelayTime   开始执行任务的延迟时间（毫秒）
	 */
	public void addTimer(String key,TimerTask timerTask,long periodTime,long startDelayTime){
		synchronized(timerTaskMap){
			TimerTaskItem timer = new TimerTaskItem();
			timer.timerTask = timerTask;
			timer.periodTime = periodTime;
			timer.nextTriggerTime = System.currentTimeMillis() + startDelayTime + periodTime ;
			timerTaskMap.put(key, timer);
		}		
	}
	
	/**
	 * 更改定时器的时间间隔
	 * @param timerTask      执行定时任务的类
	 * @param newPeriodTime  更改后的周期时间（毫秒）
	 */
	public void changeTimerPeriodTime(String key,long newPeriodTime){
		synchronized(timerTaskMap){
			if(timerTaskMap.get(key)!=null){
				TimerTaskItem item=timerTaskMap.get(key);
				item.nextTriggerTime = item.nextTriggerTime - item.periodTime + newPeriodTime;
				item.periodTime = newPeriodTime;
			}
			
		}
	}
	
	/**
	 * 移除定时器
	 */
	public void removeTimer(String key){
		synchronized(timerTaskMap){
			timerTaskMap.remove(key);
		}
	}
	public void close(){
		isRunning=false;
	}
	/**
	 * 线程方法
	 */
	public void run(){
		while (isRunning){
			try{
				Thread.sleep(5);
				synchronized(timerTaskMap){
					long currTime = System.currentTimeMillis();
					Iterator<String> iter = timerTaskMap.keySet().iterator();
					while(iter.hasNext()){
						String key=iter.next();
						TimerTaskItem task = timerTaskMap.get(key);
						if(currTime>=task.nextTriggerTime){
							try {
								//触发定时任务
								task.timerTask.onTimeOut(key);
								//计算下一次任务的执行时间
								task.nextTriggerTime = System.currentTimeMillis() + task.periodTime ;
							} catch (Exception e) {
								logger.error(Utils.getExceptionStack(e));
							}
						}
					}
				}
			}catch(Exception e){
				logger.error(Utils.getExceptionStack(e));
			}
		}
	}
	
	/**
	 * 单个Timer的相关参数
	 *
	 */
	class TimerTaskItem{
		/** 执行定时任务的类 */
		public TimerTask timerTask = null;
		
		/** 定时的周期时间（毫秒）*/
		public long periodTime = 0;
		
		/** 下次触发时间 */
		public long nextTriggerTime = 0 ;
	}

}