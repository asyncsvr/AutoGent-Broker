package util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetAddress;
import java.sql.Connection;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerHandler extends ChannelInboundHandlerAdapter {

	private static final Logger LOG = LogManager.getLogger(ServerHandler.class);

	public void channelRead(ChannelHandlerContext ctx, Object msg)throws Exception {
		ByteBuf byteBufMessage = (ByteBuf) msg;
		int size = byteBufMessage.readableBytes();
		byte [] byteMessage = new byte[size];
		for(int i = 0 ; i < size; i++){
			byteMessage[i] = byteBufMessage.getByte(i);
		}
		String str = new String(byteMessage);
		LOG.info("rev="+str);
		ctx.write(msg);
		String[] items =str.split("::");
		try{
			String recvAuthCode=items[0];//
			String recvMode=items[1];//norm/lock
			String recvCmdType=items[2];//run
			String recvOpID=items[3];
			String recvTimeLimit=items[4];
			Conf cf=new Conf();
			cf.setConfFile("AutoGent.broker.conf");
			RDao rDao=new RDao();
			rDao.setRdbPasswd(cf.getSingleString("password"));
			rDao.setRdbUrlNdbType(cf.getDbURL());
			rDao.setRdbUser(cf.getSingleString("user"));
			Connection con=rDao.getConnection();
			String resRcsvr=cf.getSingleString("result_receiver");
			
			ArrayList <String> agents=rDao.getAgents(con,recvOpID);
			String cmd=rDao.getCmd(con,recvOpID);
			String sendMsg = recvAuthCode+"::"+resRcsvr+"::"+recvMode+"::"+recvCmdType+"::"+recvOpID+"::"+recvTimeLimit+"::"+cmd+"::";
			int AgentPort=cf.getSinglefValue("agent_port");
			SockPlain sp= new SockPlain();
			String dbType=cf.getSingleString("main_db_type");
			for (String agent: agents){
				LOG.info("target:"+agent+",msg:"+sendMsg);
				boolean res=sp.sendMsg(agent,AgentPort,sendMsg,cf);
				

				rDao.insertSubJob(con, agent, Integer.parseInt(recvOpID),res,dbType);
			
			}
			rDao.setJobStatusSent(con,Integer.parseInt(recvOpID));
			con.close();
		}catch(ArrayIndexOutOfBoundsException e){
			LOG.info("Array Out");
		}catch (NumberFormatException e){
			LOG.error("NumberFormatException:"+msg);
		}
	}


	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	};

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}