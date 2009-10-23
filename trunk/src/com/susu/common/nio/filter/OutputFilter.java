package com.susu.common.nio.filter;

/**
 * 输出过滤器，当发送数据时要执行的过滤器
 * @author zhjb
 *
 */
public interface OutputFilter extends Filter {
	/**
	 * 参数为要发送的数据，返回值为处理后的数据
	 * 最后一个OutputFilter的返回类型是最终要发送的数据，必须是ByteBuffer类型
	 * @param msg
	 * @return
	 */
	public Object onSendMsg(Object msg);
}
