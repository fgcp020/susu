package com.susu.common.nio.session;

import java.util.Date;

public interface Session {
	
	/**
	 * ��ȡ�ỰID
	 * @return
	 */
	public long getId();
	
	/**
	 * ��ȡ�Ự����ʱ��
	 * @return
	 */
	public Date getCreateDate();
	
	/**
	 * д����
	 * @param buffer
	 */
	public void wirte(Object buffer);
	
	/**
	 * ��ȡ�յ�������
	 * @return
	 */
	public Object getReceiveMessage();
	
	/**
	 * ���ý��յ����ݣ������յ������Ժ�ת����ҵ�����
	 * @param msg
	 */
	public void setReceiveMessage(Object msg);
	
	/**
	 * �������ԣ��Ա��Ժ�Ĺ�������Ҫ
	 * @param key
	 * @param value
	 */
	public void setAttribute(Object key, Object value);
	
	/**
	 * ��ȡ���õ�����
	 * @param key
	 * @return
	 */
	public Object getAttribute(Object key);
	
	/**
	 * ��ȡ����IP��ַ
	 * @return
	 */
	public String  getLocalIp();
	
	/**
	 * ��ȡ���ض˿ں�
	 * @return
	 */
	public int  getLocalPort();
	
	/**
	 * ��ȡԶ��IP��ַ
	 * @return
	 */
	public String  getRemoteIp();
	
	/**
	 * ��ȡԶ�̶˿ں�
	 * @return
	 */
	public int  getRemotePort();
	
	/**
	 * �رջỰ
	 */
	public void close();
	
}
