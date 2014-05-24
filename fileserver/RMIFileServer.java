package fileserver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;

import netutils.NetUtils;

import contactserver.IContactServer;
import fileutils.FileInfo;
import fileutils.InfoNotFoundException;

/**
 * 
 * @author Carlos Bate & Ricardo Pinto
 *
 */
public class RMIFileServer
		extends UnicastRemoteObject
		implements IFileServer {
	
	
	private static final long serialVersionUID = -4285899588803243137L;
	
	private File basePath;
	private IFileServerInfo fsinfo;


	protected RMIFileServer(String servername, String username, String url, String pathname) 
			throws RemoteException {
		super();
		basePath = new File( pathname);
		fsinfo = new RMIFileServerInfo(servername, username, url);
	}
	
	
	@Override
	public IFileServerInfo getServerInfo(){
		return fsinfo;
	}
	
	@Override
	public void addPermission(String username){
		((RMIFileServerInfo)fsinfo).addPermission(username);
	}


	@Override
	public void removePermission(String username){
		((RMIFileServerInfo)fsinfo).removePermission(username);
	}

	@Override
	public String[] dir(String path, String username) 
			throws InfoNotFoundException, UserPermissionException {
		verifyPermission(username);
		File f = new File( basePath, path);
		if( f.exists())
			if(f.isDirectory())
				return f.list();
			else
				throw new InfoNotFoundException("Path "+path+" is not a directory.");
		else
			throw new InfoNotFoundException( "Directory not found :" + path);
	}

	@Override
	public FileInfo getFileInfo(String path, String user) 
			throws InfoNotFoundException, UserPermissionException {
		verifyPermission(user);
		File f = new File( basePath, path);
		if( f.exists())
			return new FileInfo( f.getName(), f.length(), new Date(f.lastModified()), f.isFile());
		else
			throw new InfoNotFoundException( "File not found :" + path);
	}
	
	@Override
	public byte[] downloadFile(String path, String user) 
			throws UserPermissionException {
		verifyPermission(user);
		try {
			RandomAccessFile f = new RandomAccessFile(new File(basePath,path), "r");
			long size = f.length();
			byte[] b = new byte[(int)size];
			f.readFully(b);
			f.close();
			return b;
		} catch (IOException e) {
			System.err.println("Error reading file.");
		}
		return null;
	}	
	
	@Override
	public boolean uploadFile(String path, byte[] content, String user)
			throws UserPermissionException {
		verifyPermission(user);
		try {
			RandomAccessFile f = new RandomAccessFile(new File(basePath,path), "rw");
			f.write(content);
			f.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	@Override
	public boolean mkdir(String path, String user) throws UserPermissionException{
		verifyPermission(user);
		return new File(basePath,path).mkdir();
	}

	@Override
	public boolean rmdir(String path, String user) throws UserPermissionException{
		verifyPermission(user);
		File f = new File(basePath, path);
		if(f.exists() && f.isDirectory())
			return f.delete();
		else return false;
	}
	
	@Override
	public boolean rm(String path, String user) throws UserPermissionException{
		verifyPermission(user);
		File f = new File(basePath, path);
		if(f.isFile()){
			return f.delete();
		}
		return false;
	}


	protected void verifyPermission(String user) throws UserPermissionException {

		try {
			if(user == null || !fsinfo.isPermitted(user))
				throw new UserPermissionException(fsinfo.getServerName()+"@"+fsinfo.getUserName());
		} catch (RemoteException e) {
			//This exception is never thrown, because the invocation is local
		}
	}
	
	

	public static void main( String args[]) throws Exception {
		try {
			if( args.length != 3){
				System.err.println("Usage: java RMIFileServer <server_name> " +
						"<user_name> <path>");
				return;
			}
			
			String servername = args[0];
			String contactserverURL = discoverContactSrvURL();
			String username = args[1];
			String path = args[2];
			String url = NetUtils.fetchIPAddress();
			
			if(url == null){
				System.err.println("Unable to fetch IP address.");
				return;
			}
			
			System.setProperty("java.rmi.server.hostname",url);
			
			System.getProperties().put( "java.security.policy", "policy.all");
			
			if( System.getSecurityManager() == null) {
				
				System.setSecurityManager( new RMISecurityManager());
			}
			

			try { // start rmiregistry
				LocateRegistry.createRegistry(1099);
			} catch( RemoteException e) { 
				// do nothing - already started with rmiregistry
			}
			
			IFileServer server = new RMIFileServer( servername, username, url, path);
			Naming.rebind( servername+"@"+username, server);
			
			IContactServer contactServer = (IContactServer) Naming.lookup
					("//"+contactserverURL+":1099/"+IContactServer.SERVER_NAME);
			contactServer.registerServer(server.getServerInfo());
			
			System.out.println( "DirServer "+servername+" bound in registry");
			
		} catch( Throwable th) {
			th.printStackTrace();
		}
	}
	
	private static String discoverContactSrvURL() throws IOException {
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
