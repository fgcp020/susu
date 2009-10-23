package com.susu.common.nio.filter;

import com.susu.common.CommException;


/**
 * 过滤器接口
 * @author zhjb
 *
 */
public interface Filter {
	
	/**
	 * 过滤器初始化方法
	 * @throws Exception
	 */
	public void init() throws CommException;
	
	/**
	 * 过滤器销毁方法
	 * @throws Exception
	 */
	public void destroy() throws CommException;
	
	/**
	 * 返回过滤器的排序序号
	 * @return
	 */
	public int getOrder();

}
