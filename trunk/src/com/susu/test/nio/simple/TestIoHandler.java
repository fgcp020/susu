package com.susu.test.nio.simple;

import com.susu.common.CommException;
import com.susu.common.nio.handler.AbstractIoHandler;

/**
 * 报文定义，报文头为8为的字符串
 */
public class TestIoHandler extends AbstractIoHandler {

	@Override
	protected int getLenth(byte[] lenth) {
		System.out.println(Integer.parseInt(new String(lenth)));
		return Integer.parseInt(new String(lenth));
	}

	protected int getLenthOfLenth() {
		return 8;
	}

	public void destroy() throws CommException {
		System.out.println("TestIoHandler:destroy");

	}

	public void init() throws CommException {
		System.out.println("TestIoHandler:init");

	}

	protected boolean hasContainHead() {
		return false;
	}

}
