package com.susu.test.nio.simple;

import com.susu.common.nio.connector.NioSocketConnector;
import com.susu.common.nio.session.Session;

/**
 * 客户端测试类，连上服务器后定时给服务器发送报文
 * @author zhjb
 */

public class ClientTest {
	 boolean isRunning=true;
	 NioSocketConnector connector ;
	 TestIoHandler ioHandler;
	public static void main(String[] args) {
		ClientTest ct= new ClientTest();
		ct.start();
	}
	public void start(){
		try {
			connector = new NioSocketConnector();
			
			ioHandler=new TestIoHandler();
			connector.addFilter(new ClientInputFilter(11));
			connector.addFilter(new ClinetOutputFilter(11));
			connector.init();
			
			
			Client  c= new Client();
			c.setName("Test_Client1");
			c.start();
			
			
			c= new Client();
			c.setName("Test_Client2");
			c.start();
			
			c= new Client();
			c.setName("Test_Client3");
			c.start();
			
			Thread.sleep(1000*5);
			isRunning=false;
			Thread.sleep(1000*1);
			connector.close();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	class Client extends Thread{
		Session session;
		public void run(){
			try {
				int n=0;
				session =connector.connect("127.0.0.1",9000,ioHandler);
				while(isRunning){
					
					String s="MsgType=2CheckFlag0,TellerIDs=csl,acctNo=1000468931clientID=10004689sapplyDate=20081126apnt=1remainAmountremainAmount=1status=otraderID=gxb901000000523MsgType=2Chec00000RspMsg=操作成功amount=1applyDate=200rFlag=LNodeID=9010NodeType=6RootID=RspCode=RSP000ancelTime=clientID=1000468908instID=Au(T+D)localOrderNo=91391margin=0.00000000marginType= matchType=1memberID=9010offSetFlag=0orderNo=02000913price=201.00000000remainAmount=1status=otraderID=gxb901000000523MsgType=2CheckFlag=1AcctNos=1000468908BranchIDs=00000000,B00000,00009010,TellerIDs=csl,acctNo=1000468908ApiName=onRecvt=1applyDate=20081126applyTime=13:11:24buyOrSell=bcancelID=cancelTime=clientID=1000468908instID=Au(T+D)localOrdMsgType=2CheckFlag0,TellerIDs=csl,acctNo=1000468931clientID=10004689sapplyDate=20081126apnt=1remainAmountremainAmount=1status=otraderID=gxb901000000523MsgType=2Chec00000RspMsg=操作成功amount=1applyDate=200rFlag=LNodeID=9010NodeType=6RootID=RspCode=RSP000ancelTime=clientID=1000468908instID=Au(T+D)localOrderNo=91391margin=0.00000000marginType= matchType=1memberID=9010offSetFlag=0orderNo=02000913price=201.00000000remainAmount=1status=otraderID=gxb901000000523MsgType=2CheckFlag=1AcctNos=1000468908BranchIDs=00000000,B00000,00009010,TellerIDs=csl,acctNo=1000468908ApiName=onRecvt=1applyDate=20081126applyTime=13:11:24buyOrSell=bcancelID=cancelTime=clientID=1000468908instID=Au(T+D)localOrd";
					session.wirte(s);
					n++;
					Thread.sleep(100);
				}
				System.out.println(this.getName()+":"+n);
				Thread.sleep(100);
				session.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}
