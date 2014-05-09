package netutils;

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
}
