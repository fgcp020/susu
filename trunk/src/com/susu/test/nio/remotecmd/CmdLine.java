package com.susu.test.nio.remotecmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

import com.susu.common.nio.session.Session;

public class CmdLine {
	private boolean isQuit=false;
	/**
	 * 控制台打印信息
	 * @param info
	 */
	public void print(String info){
		System.out.println(info);
		System.out.print("-->");
	}
	/**
	 * 执行cmd命令返回执行结果
	 * @param cmd
	 * @return
	 */
	public String execCmd(String cmd){
		StringBuffer sb= new StringBuffer();
		BufferedReader reader =null;
		try {
			if(System.getProperty("os.name").indexOf("Windows")!=-1){
				cmd="cmd.exe /c "+cmd;
			}
			Process p = Runtime.getRuntime().exec(cmd);  
			p.getOutputStream().close();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));   
			
            String line = null; 
            while ((line = reader.readLine()) != null) {   
                sb.append(line).append("\n");
            }
            reader.close();
            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));   
			line = null; 
            while ((line = reader.readLine()) != null) {   
                sb.append(line).append("\n");
            }
            
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(reader!=null){
				try {
					reader.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				
			}
		}

		return sb.toString();
	}
	
	public void cmdLine(Session session){
		Scanner sc = new Scanner(System.in);

		while(!isQuit){
			System.out.print("-->");
			String cmd=sc.nextLine();
			if(cmd.equals("exit")){
				isQuit=true;
				session.close();
				session=null;
				try {
					ClientTest.connector.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if(session==null){
				break;
			}
			if(cmd!=null){
				session.wirte(cmd);
			}
		}
	}
	
	public static void main(String []s){
		CmdLine c= new CmdLine();
		System.out.println(c.execCmd("wmic process get name,CommandLine"));
//		System.out.println(c.execCmd("dir"));
	}
}
