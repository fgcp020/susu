package com.susu.common.nio.filter;

import com.susu.common.CommException;


/**
 * �������ӿ�
 * @author zhjb
 *
 */
public interface Filter {
	
	/**
	 * ��������ʼ������
	 * @throws Exception
	 */
	public void init() throws CommException;
	
	/**
	 * ���������ٷ���
	 * @throws Exception
	 */
	public void destroy() throws CommException;
	
	/**
	 * ���ع��������������
	 * @return
	 */
	public int getOrder();

}
