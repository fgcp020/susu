package com.susu.common.nio.handler;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.susu.common.CommException;
import com.susu.common.ErrorCode;
import com.susu.common.util.Utils;
/**
 * 读写数据的抽象类
 * @author zhjb
 *
 */
public abstract class AbstractIoHandler implements IoHandler {
	private static Logger logger  =  Logger.getLogger(AbstractIoHandler.class );
	
	
	/**
	 * 读数据，
	 */
	public synchronized Object read(SocketChannel sc) throws CommException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Object body=null;
		try {
			ByteBuffer head= ByteBuffer.allocate(getLenthOfLenth());
			
			int rec=sc.read(head);
			if(rec==-1){
				throw new CommException(ErrorCode.NIO0002,ErrorCode.NIO0002+":远程Socket关闭");
			}else if(rec==0){
				return null;
			}else if(rec!=head.limit()){
				throw new CommException(ErrorCode.NIO0004,"接收报文头不全："+new String(head.array()));
			}
			
			ByteBuffer msg=null;
			if(hasContainHead()){
				msg=ByteBuffer.allocate(getLenth(head.array())-getLenthOfLenth());
			}else{
				msg=ByteBuffer.allocate(getLenth(head.array()));
			}
			while(msg.position()!=msg.limit()){
				rec=sc.read(msg);
				if(rec==-1){
					throw new CommException(ErrorCode.NIO0002,"远程Socket关闭");
				}else if(rec==0){
					continue;
				}
			}
			msg.position(0);
			stream.write(head.array());
			stream.write(msg.array());
			byte[] buffer=stream.toByteArray();
			body=buffer;
			stream.close();
			
		}catch(CommException e) {
			throw e;
		}catch(Exception e) {
			String es=Utils.getExceptionStack(e);
			logger.debug(es);
			throw new CommException(ErrorCode.NIO0002,es);
			
		}
		return body;
	}
	/**
	 * 循环发送数据
	 */
	public void wirte(SocketChannel sc, Object buffer) throws CommException {
		try {
			ByteBuffer bb=(ByteBuffer)buffer;
			bb.position(0);
			while(bb.position()<bb.limit()){
				sc.write(bb);
			}
			logger.info("发送数据："+new String(bb.array()));
		} catch (Exception e) {
			logger.debug(Utils.getExceptionStack(e));
			throw new CommException(ErrorCode.NIO0003,Utils.getExceptionStack(e));
		}
		
	}
	/**
	 * 获取报文长度的长度
	 * @return
	 */
	protected abstract int getLenthOfLenth();
	
	/**
	 * 获取报文长度
	 * @param lenth
	 * @return
	 */
	protected abstract int getLenth(byte[] lenth);
	
	/**
	 * 报文头的本身长度是否包涵在报文头里面
	 * @return
	 */
	protected abstract boolean hasContainHead();
	
}
