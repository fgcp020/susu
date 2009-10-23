package com.susu.common.nio.acceptor;

import java.nio.channels.ServerSocketChannel;

/**
 * 存储服务器信息，Selector对象，ServerSocketChannel对象，ip地址和端口号
 * @author zhjb
 *
 */
public class ServerInfo {
	
	private ServerSocketChannel serverSocketChannel; 
	
	private String ip;
	
	private int port;

	public ServerSocketChannel getServerSocketChannel() {
		return serverSocketChannel;
	}

	public void setServerSocketChannel(ServerSocketChannel serverSocketChannel) {
		this.serverSocketChannel = serverSocketChannel;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
