package com.susu.common.nio.filter;

/**
 * ���������������������ʱҪִ�еĹ�����
 * @author zhjb
 *
 */
public interface OutputFilter extends Filter {
	/**
	 * ����ΪҪ���͵����ݣ�����ֵΪ����������
	 * ���һ��OutputFilter�ķ�������������Ҫ���͵����ݣ�������ByteBuffer����
	 * @param msg
	 * @return
	 */
	public Object onSendMsg(Object msg);
}
