package netutils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetUtils {

	public static String fetchIPAddress() throws SocketException{
		Enumeration<NetworkInterface> eni = NetworkInterface.getNetworkInterfaces();
		while(eni.hasMoreElements()){
			NetworkInterface ni = eni.nextElement();
			if(!ni.isUp() || ni.isVirtual() || ni.isLoopback()) continue;
			Enumeration<InetAddress> eia = ni.getInetAddresses();
			while(eia.hasMoreElements()){
				InetAddress ia = eia.nextElement();
				if(ia instanceof Inet4Address && ia.isSiteLocalAddress())
					return ia.getHostAddress();
			}
		} 
		return null;
	}
	
	public static DatagramSocket assignUDPSocket(int port) throws IOException{
		System.out.println("Attempting to bind socket...");
		int tries = 100;
		DatagramSocket ds = null;
		boolean assigned = true;
		while(!assigned){
			if(tries == 0)
				throw new IOException("Unable to bind datagram socket.");
			try{
				ds = new DatagramSocket(port);
			} catch(SocketException e){
				port++;
				tries--;
			}
		}
		System.out.println("Binding successful.");
		return ds;
	}
}
