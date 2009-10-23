package com.susu.common.nio.processor;

public class Event {
	
	private EventEnum eventEnum;
	
	private Object data;
	
	/**
	 * 如果eventEnum是:  	ON_RECEIVE_MESSAGE,  //接受数据事件
	 *          			ON_CREATE_SESSION,   //新建Session事件
	 *         				ON_CLOSE_SESSION,    //关闭Session事件
	 * 则data中是Session对象
	 * 如果eventEnum是: 	ON_SEND_MSG,         //发送数据事件
	 * 则data中是要发送的数据对象         
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
