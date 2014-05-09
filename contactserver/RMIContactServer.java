package contactserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.ws.WebServiceException;

import netutils.NetUtils;

import fileserver.IFileServerInfo;
import fileserver.WSFileServerInfo;

public class RMIContactServer extends UnicastRemoteObject implements
		IContactServer {

	private static final long serialVersionUID = 7136843464433653568L;
	
	
	protected Map<String,IFileServerInfo> RMIfileServers;	//keys follow the format "server@user"
	protected Map<String,WSFileServerInfo> WSfileServers;
	private static String contactURL;

	protected RMIContactServer() throws RemoteException{
		super();
		new FileServerTracker().start();
		RMIfileServers = new HashMap<String, IFileServerInfo>();
		WSfileServers = new HashMap<String, WSFileServerInfo>();
	}
	
	private class FileServerTracker extends Thread{
		
		FileServerTracker(){
			super();
		}
		
		public void run(){
			trackFileServers();
		}
		
		
		void trackFileServers(){
			for(;;){
				try {
					Thread.sleep(2000);
					List<String> toRemove = new LinkedList<String>();
					synchronized(RMIfileServers){
						for(String s: RMIfileServers.keySet()){
							IFileServerInfo fs = RMIfileServers.get(s);
							try{
								fs.ping();
							} catch(RemoteException e){
								toRemove.add(s);
							}
						}
						for(String s: toRemove){
							RMIfileServers.remove(s);
						}
					}
					//TODO fazer o mesmo tratamento para os WSFileServers
					toRemove = new LinkedList<String>();
					synchronized(WSfileServers){
						for(String s: WSfileServers.keySet()){
							try{
								 WSfileServers.get(s).ping();
							} catch(WebServiceException e){
								toRemove.add(s);
							}
						}
						for(String s: toRemove){
							RMIfileServers.remove(s);
						}
					}
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}			
			}
		}
	}
	
	
	@Override
	public List<String> getAccessibleServers(String username) throws RemoteException {
		List<String> res = new LinkedList<String>();
		synchronized(RMIfileServers){
			for(String s: RMIfileServers.keySet()){
				if(RMIfileServers.get(s).isPermitted(username)){
					res.add(RMIfileServers.get(s).getServerName());
				}
			}
		}
		synchronized(WSfileServers){
			for(String s: WSfileServers.keySet()){
				if(WSfileServers.get(s).isPermitted(username))
					res.add(WSfileServers.get(s).getServerName());
			}
		}
		return res;
	}
	
	@Override
	public String getURL(String servername, String username){
		synchronized(WSfileServers){
			if(WSfileServers.containsKey(servername+"@"+username)){
				WSFileServerInfo fsinfo = WSfileServers.get(servername+"@"+username);
				if(fsinfo != null){
					return "http://"+fsinfo.getURL();
				}
				return null;
			}
			else{
				synchronized(RMIfileServers){
					IFileServerInfo fsinfo = RMIfileServers.get(servername+"@"+username);
					
					try {
						if(fsinfo != null){
							return fsinfo.getURL();
						}
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				return null;
			}
		}
	}

	@Override
	public void registerServer(IFileServerInfo server) throws RemoteException {
		String key = server.getServerName()+"@"+server.getUserName();
		synchronized(RMIfileServers){
			RMIfileServers.put(key, server);
		}
	}

	//WebServiceMethod
	public void registerServer(WSFileServerInfo server) throws RemoteException {
		String key = server.getServerName()+"@"+server.getUserName();
		synchronized(WSfileServers){
			WSfileServers.put(key, server);
		}
	}
	
	public static void main(String[] args) throws Exception{
		try {
			System.getProperties().put( "java.security.policy", "policy.all");
			if((contactURL = NetUtils.fetchIPAddress())==null){
				System.err.println("Error: unable to fetch IP address");
				return;
			}
			
			System.setProperty("java.rmi.server.hostname",contactURL);
			
			if( System.getSecurityManager() == null) {
				System.setSecurityManager( new RMISecurityManager());
			}

			try { // start rmiregistry
				LocateRegistry.createRegistry(1099);
			} catch( RemoteException e) { 
				// do nothing - already started with rmiregistry
			}

			IContactServer contactServer = new RMIContactServer();
			Naming.rebind( SERVER_NAME, contactServer);	
			
			System.out.println( "Contact server bound in registry with name \""+SERVER_NAME+"\"");
			runDiscoveryService();
			
		} catch( Throwable th) {
			th.printStackTrace();
		}
	}
	
	/*
	 * Request messages are identified by the string "REQ"
	 * Reply messages are identified by the string "RPL"
	 */
	private static void runDiscoveryService() throws IOException{
		InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
		MulticastSocket ms = new MulticastSocket(MULTICAST_PORT);
		ms.joinGroup(group);
		ms.setSoTimeout(0);	//wait until a msg arrives
		for(;;){
			try{
				byte[] buf = new byte[3];
				DatagramPacket rcv = new DatagramPacket(buf, buf.length);
				ms.receive(rcv);
				String rcvStr = new String(rcv.getData());
				if(rcvStr.equals("REQ")){
					buf = "RPL".getBytes();
					DatagramPacket sendPkt = new DatagramPacket(buf, buf.length,rcv.getAddress(),UDP_PORT);
					ms.send(sendPkt);
				} 
			} catch(IOException e){
				//a send or receive failed, but the server continues listening to requests
			}
		}
	}

}
