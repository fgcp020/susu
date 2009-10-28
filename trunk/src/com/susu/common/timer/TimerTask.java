package com.zjb.common.timer;

/**
 * 定义器执行任务的接口
 */
public interface TimerTask
{
	/**
	 * 超时处理函数
	 * @param key 
	 */
	public void onTimeOut(String key);
	
}