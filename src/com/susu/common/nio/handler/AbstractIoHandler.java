package com.susu.common.nio.handler;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.susu.common.CommException;
import com.susu.common.ErrorCode;
import com.susu.common.util.Utils;
/**
 * ��д���ݵĳ�����
 * @author zhjb
 *
 */
public abstract class AbstractIoHandler implements IoHandler {
	private static Logger logger  =  Logger.getLogger(AbstractIoHandler.class );
	
	
	/**
	 * �����ݣ�
	 */
	public synchronized Object read(SocketChannel sc) throws CommException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Object body=null;
		try {
			ByteBuffer head= ByteBuffer.allocate(getLenthOfLenth());
			
			int rec=sc.read(head);
			if(rec==-1){
				throw new CommException(ErrorCode.NIO0002,ErrorCode.NIO0002+":Զ��Socket�ر�");
			}else if(rec==0){
				return null;
			}else if(rec!=head.limit()){
				throw new CommException(ErrorCode.NIO0004,"���ձ���ͷ��ȫ��"+new String(head.array()));
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
					throw new CommException(ErrorCode.NIO0002,"Զ��Socket�ر�");
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
	 * ѭ����������
	 */
	public void wirte(SocketChannel sc, Object buffer) throws CommException {
		try {
			ByteBuffer bb=(ByteBuffer)buffer;
			bb.position(0);
			while(bb.position()<bb.limit()){
				sc.write(bb);
			}
			logger.info("�������ݣ�"+new String(bb.array()));
		} catch (Exception e) {
			logger.debug(Utils.getExceptionStack(e));
			throw new CommException(ErrorCode.NIO0003,Utils.getExceptionStack(e));
		}
		
	}
	/**
	 * ��ȡ���ĳ��ȵĳ���
	 * @return
	 */
	protected abstract int getLenthOfLenth();
	
	/**
	 * ��ȡ���ĳ���
	 * @param lenth
	 * @return
	 */
	protected abstract int getLenth(byte[] lenth);
	
	/**
	 * ����ͷ�ı������Ƿ�����ڱ���ͷ����
	 * @return
	 */
	protected abstract boolean hasContainHead();
	
}
