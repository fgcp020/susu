package com.susu.common.nio.processor;

import java.nio.channels.SocketChannel;

/**
 * hls2009-7-8
 */
public class ServerNioSocketProcessor extends AbstractProcessor {

	protected String getKey(SocketChannel sc)throws Exception {
		String key=sc.socket().getLocalAddress().getHostAddress()+":"+sc.socket().getLocalPort();
		return key;
	}

}
