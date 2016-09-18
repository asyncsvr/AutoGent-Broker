package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SockPlain {
	private static final Logger LOG = LogManager.getLogger(SockPlain.class);
	public boolean sendMsg(String hostname,int port,String message,Conf cf){
		Socket socket=null;
		String sendMsg = message;
		String revMsg = null; // 받는 메시지
		int rprtCnt=cf.getSinglefValue("retry_cnt");

		String ip = "localhost";
		BufferedReader insert = null;  
		BufferedReader br = null;
		InputStream is = null;
		OutputStream os = null;
		PrintWriter pw = null;
		int tryCnt=0;
		while (revMsg==null || !sendMsg.matches(revMsg )){
			try {
				//String sendMsg = revAuthCode+"::"+revMode+"::"+revCmdType+"::"+cmd;
				socket = new Socket(hostname,port);
				os = socket.getOutputStream();
				is = socket.getInputStream();
				pw = new PrintWriter(new OutputStreamWriter(os));
				br = new BufferedReader(new InputStreamReader(is)); 
				pw.println(sendMsg);
				pw.flush();
				revMsg = br.readLine();
				LOG.info("rev=" + revMsg);
				if (revMsg== null){
					LOG.error("Host:"+hostname+" encount Error");
				}
				
				pw.close();
				br.close();
				socket.close();
				
				if(sendMsg.matches(revMsg)){
					LOG.info("Sending complete to "+hostname);
					return true;
				}
			}catch (ConnectException e){
				LOG.error("Connection Error to "+hostname);
				tryCnt++;
				if (rprtCnt < tryCnt){
					LOG.error("connection re-try count:"+tryCnt+" exceed to "+hostname);
					break;
				}
				try{
					Thread.sleep(cf.getSinglefValue("retry_interval_sec")*1000);
				}catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}catch (SocketException e){
				LOG.error("SocketException to "+hostname);
				tryCnt++;
				if (rprtCnt < tryCnt){
					LOG.error("connection re-try count:"+tryCnt+" exceed to "+hostname);
					break;
				}
				try{
					Thread.sleep(cf.getSinglefValue("retry_interval_sec")*1000);
				}catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}catch (IOException e) {
				tryCnt++;
				if (rprtCnt < tryCnt){
					LOG.error("connection re-try count:"+tryCnt+" exceed to "+hostname);
					break;
				}
				try {
					Thread.sleep(cf.getSinglefValue("retry_interval_sec")*1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			if (revMsg==null||!sendMsg.matches(revMsg)){
				tryCnt++;
				if (rprtCnt < tryCnt){
					LOG.error("connection re-try count:"+tryCnt+" exceed to "+hostname);
					break;
				}
				try {
					Thread.sleep(cf.getSinglefValue("retry_interval_sec")*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			//TODO:Test How it works retry
			tryCnt++;
			if (rprtCnt < tryCnt){
				LOG.error("connection re-try count:"+tryCnt+" exceed to "+hostname);
				break;
			}
			try {
				Thread.sleep(cf.getSinglefValue("retry_interval_sec")*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
		return false;
	}
	
}
