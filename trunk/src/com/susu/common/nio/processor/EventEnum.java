package com.susu.common.nio.processor;
/**
 * hls2009-7-10
 */
public enum EventEnum {
	ON_RECEIVE_MESSAGE,  //接受数据事件
	ON_CREATE_SESSION,   //新建Session事件
	ON_CLOSE_SESSION,    //关闭Session事件
	ON_SEND_MSG,         //发送数据事件
	ON_QUIT,             //退出程序事件
}
