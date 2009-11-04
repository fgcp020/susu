package com.susu.common.timer;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.susu.common.util.Utils;



/**
 * ͳһTimer������
 */
public class TimerManager extends Thread{
	private static Logger logger  =  Logger.getLogger(TimerManager.class);
	/** �߾�׼�ĵ��� */
	private static TimerManager instanceNicety = null;
	
	/** ���Եĵ��� */
	private static TimerManager instanceCursory = null;
	
	/**��ŵĶ�ʱ�������б�*/
	private Map<String ,TimerTaskItem> timerTaskMap= new ConcurrentHashMap<String,TimerTaskItem>();
	
	/** �Ƿ����� */
	protected boolean isRunning = true;
	
	/**
	 * ���캯��
	 */
	private TimerManager(){}
	
	/**
	 * ��ø߾�׼��Timer��������ʵ��
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
	 * ��ô���Timer��������ʵ��
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
	 * ��Ӷ�ʱ��
	 * @param key        		��������key
	 * @param timerTask         ִ�ж�ʱ�������
	 * @param periodTime       ִ�и���������֮���ʱ���������룩
	 * @param startDelayTime   ��ʼִ��������ӳ�ʱ�䣨���룩
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
	 * ���Ķ�ʱ����ʱ����
	 * @param timerTask      ִ�ж�ʱ�������
	 * @param newPeriodTime  ���ĺ������ʱ�䣨���룩
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
	 * �Ƴ���ʱ��
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
	 * �̷߳���
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
								//������ʱ����
								task.timerTask.onTimeOut(key);
								//������һ�������ִ��ʱ��
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
	 * ����Timer����ز���
	 *
	 */
	class TimerTaskItem{
		/** ִ�ж�ʱ������� */
		public TimerTask timerTask = null;
		
		/** ��ʱ������ʱ�䣨���룩*/
		public long periodTime = 0;
		
		/** �´δ���ʱ�� */
		public long nextTriggerTime = 0 ;
	}

}