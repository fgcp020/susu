package com.susu.test.nio.simple;

import java.nio.ByteBuffer;

import com.susu.common.CommException;
import com.susu.common.nio.filter.OutputFilter;

public class ServerOutputFilter implements OutputFilter {
	
	private int order;
	
	public ServerOutputFilter(int order){
		this.order=order;
	}
	public void destroy() throws CommException {
		System.out.println(this.toString()+"destroy");
	}

	public int getOrder() {
		
		return order;
	}

	
	public String toString(){
		return order+"-outputFilter:";
	}
	public void init() throws CommException {
		System.out.println(this.toString()+"init");
		
	}
	public Object onSendMsg(Object msg) {
		int n=msg.toString().getBytes().length;
		int len=(""+n).length();
		StringBuffer sb= new StringBuffer(1000);
		for(;len<8;len++){
			sb.append("0");
		}
		sb.append(n);
		sb.append(msg.toString());
		byte [] src= sb.toString().getBytes();
		ByteBuffer bb=ByteBuffer.allocate(src.length);
		bb.put(src,0,src.length);
		return bb;
	}
                    
}
