package com.susu.common.nio.processor;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class ClientNioSocketProcessor extends AbstractProcessor {

	protected String getKey(SocketChannel sc)throws Exception {
		InetSocketAddress remote=(InetSocketAddress)sc.socket().getRemoteSocketAddress();
		String key=remote.getAddress().getHostAddress()+":"+remote.getPort();
		return key;
	}

}
