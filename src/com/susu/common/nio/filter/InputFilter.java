package com.susu.common.nio.filter;

import com.susu.common.CommException;
import com.susu.common.nio.session.Session;

/**
 * ��������������յ�����ʱ��Ҫִ�еĹ�����
 * @author zhjb
 *
 */
public interface InputFilter extends Filter {
	/**
	 * �ͻ���������������Ự���¼�����Socket���ӳɹ��¼�
	 * @param session
	 */
	public void onCreateSession(Session session)throws CommException;
	
	/**
	 * �ͻ�����������رջỰ���¼�����Socket�ر��¼�
	 * @param session
	 */
	public void onCloseSession(Session session)throws CommException;
	
	/**
	 * �յ������¼�
	 * @param session
	 */
	public void onReceiveMessage(Session session)throws CommException;
	
}
