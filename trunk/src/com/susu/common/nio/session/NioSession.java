package com.susu.common.nio.session;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.susu.common.nio.processor.AbstractProcessor;
import com.susu.common.nio.processor.Event;
import com.susu.common.nio.processor.EventEnum;
/**
 * 客户端与服务器建立的连接会话，保存一些必要的信息
 * @author zhjb
 *
 */
public class NioSession implements Session {
	private static Logger logger  =  Logger.getLogger(NioSession.class );
	private long id;
	private Object receivedMessage;
	private Date createDate;
	private Map<Object,Object> attributeMap= new ConcurrentHashMap<Object, Object>();
	private String localIp;
	private int localPort;
	private int remotePort;
	private String remoteIp;
	private AbstractProcessor processor;
		
	public NioSession(String localIp,int localPort, String remoteIp,int remotePort,AbstractProcessor processor){
		this.localIp=localIp;
		this.localPort=localPort;
		this.remoteIp= remoteIp;
		this.remotePort=remotePort;
		createDate= new Date();
		this.processor=processor;
		id= UUID.randomUUID().getLeastSignificantBits();
	}
	public Object getAttribute(Object key) {
		return attributeMap.get(key);
	}

	public Date getCreateDate() {
		return createDate;
	}

	public long getId() {
		return id;
	}

	public String getLocalIp() {
		return localIp;
	}

	public int getLocalPort() {
		return localPort;
	}

	public Object getReceiveMessage() {
		return receivedMessage;
	}

	public String getRemoteIp() {
		return remoteIp;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public void setAttribute(Object key, Object value) {
		attributeMap.put(key, value);
	}
	public void wirte(Object buffer) {
		Event event= new Event();
		event.setEventEnum(EventEnum.ON_SEND_MSG);
		Object [] obj = new Object[]{this,buffer};
		event.setData(obj);
		processor.addEventQueue(event);
	}

	public void setReceiveMessage(Object msg) {
		receivedMessage=msg;
	}
	public void close() {
		try {
			processor.closeSession(this);	
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
}
