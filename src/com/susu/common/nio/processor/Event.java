package com.susu.common.nio.processor;

public class Event {
	
	private EventEnum eventEnum;
	
	private Object data;
	
	/**
	 * ���eventEnum��:  	ON_RECEIVE_MESSAGE,  //���������¼�
	 *          			ON_CREATE_SESSION,   //�½�Session�¼�
	 *         				ON_CLOSE_SESSION,    //�ر�Session�¼�
	 * ��data����Session����
	 * ���eventEnum��: 	ON_SEND_MSG,         //���������¼�
	 * ��data����Ҫ���͵����ݶ���         
	 * @return
	 */
	public EventEnum getEventEnum() {
		return eventEnum;
	}
	public void setEventEnum(EventEnum eventEnum) {
		this.eventEnum = eventEnum;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	
}
