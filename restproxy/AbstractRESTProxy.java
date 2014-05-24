package restproxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import netutils.NetUtils;
import contactserver.IContactServer;

public abstract class AbstractRESTProxy {
	
	
	//Used to see if a thread is already in charge of asking 
	//for the token when the former token has expired.
	protected boolean gettingToken;

	protected class TokenFetcher extends Thread{
			
		TokenFetcher(){
			super();
		}
		
		public void run(){
			getAccessToken();
			gettingToken = false;
		}
	}
	
	protected abstract void getAccessToken();
	
	protected abstract void handleResponseCodeException(ResponseCodeException e);
	
	/*
	 * Supported MIME content types:
	 * 	-application/octet-stream (default)
	 * 	-application/pdf
	 * 	-application/zip
	 * 	-text/plain
	 * 	-image/gif
	 * 	-image/jpeg
	 * 	-image/png
	 */
	protected String getMimeType(String path) {
		String format = path.substring(path.lastIndexOf('.')+1);
		switch(format){
		default: return "application/octet-stream";
		case "pdf": return "application/pdf";
		case "txt":
		case "java":
			return "text/plain";
		case "zip":
			return "application/zip";
		case "gif":
			return "image/gif";
		case "jpg":
			return "image/jpeg";
		case "png":
			return "image/png";
		}
	}
	
	protected static String discoverContactSrvURL() throws IOException {
		InetAddress group = InetAddress.getByName(IContactServer.MULTICAST_ADDRESS);
		DatagramSocket ds = NetUtils.assignUDPSocket(IContactServer.UDP_PORT);
		ds.setSoTimeout(2000); //2 seconds
		for(int i=0; i<3;i++){
			try{
				byte[] buf = "REQ".getBytes();
				DatagramPacket sendPkt = new DatagramPacket(buf, buf.length,group,IContactServer.MULTICAST_PORT);
				ds.send(sendPkt);
				buf = new byte[3];
				DatagramPacket rcv = new DatagramPacket(buf, buf.length);
				ds.receive(rcv);
				String reply = new String(rcv.getData());
				if(reply.equals("RPL")){
					return rcv.getAddress().getHostAddress();
				}
			} catch(SocketTimeoutException e){
				//keep trying
			}
		}
		return null;
	}
}
