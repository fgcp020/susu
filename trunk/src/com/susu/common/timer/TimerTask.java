package com.zjb.common.timer;

/**
 * ������ִ������Ľӿ�
 */
public interface TimerTask
{
	/**
	 * ��ʱ������
	 * @param key 
	 */
	public void onTimeOut(String key);
	
}