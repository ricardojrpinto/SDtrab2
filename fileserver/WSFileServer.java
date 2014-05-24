package fileserver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.rmi.Naming;
import java.util.Date;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import netutils.NetUtils;
import contactserver.IContactServer;
import fileutils.FileInfo;
import fileutils.InfoNotFoundException;

@WebService
public class WSFileServer{
		
	private File basePath;
	private WSFileServerInfo fsinfo;
	
	public WSFileServer(){
		basePath = new File(".");
		fsinfo = null;
	}

	protected WSFileServer(String servername, String username, String url, String pathname){
		super();
		basePath = new File( pathname);
		fsinfo = new WSFileServerInfo(servername, username, url);
	}
	
	
	@WebMethod
	public WSFileServerInfo getServerInfo(){
		return fsinfo;
	}
	
	@WebMethod
	public void addPermission(String username){
		fsinfo.addPermission(username);
	}


	@WebMethod
	public void removePermission(String username){
		fsinfo.removePermission(username);
	}

	@WebMethod
	public String[] dir(String path, String user) 
			throws InfoNotFoundException, UserPermissionException {
		verifyPermission(user);
		File f = new File( basePath, path);
		if( f.exists())
			if(f.isDirectory())
				return f.list();
			else
				throw new InfoNotFoundException("Path "+path+" is not a directory.");
		else
			throw new InfoNotFoundException( "Directory not found :" + path);
	}

	@WebMethod
	public FileInfo getFileInfo(String path, String user) 
			throws InfoNotFoundException, UserPermissionException {
		verifyPermission(user);
		File f = new File( basePath, path);
		if( f.exists())
			return new FileInfo( f.getName(), f.length(), new Date(f.lastModified()), f.isFile());
		else
			throw new InfoNotFoundException( "File not found :" + path);
	}
	
	@WebMethod
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
	
	@WebMethod
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
	
	@WebMethod
	public boolean mkdir(String path, String user) throws UserPermissionException{
		verifyPermission(user);
		return new File(basePath,path).mkdir();
	}

	@WebMethod
	public boolean rmdir(String path, String user) throws UserPermissionException{
		verifyPermission(user);
		File f = new File(basePath, path);
		if(f.exists() && f.isDirectory())
			return f.delete();
		else return false;
	}
	
	@WebMethod
	public boolean rm(String path, String user) throws UserPermissionException{
		verifyPermission(user);
		File f = new File(basePath, path);
		if(f.isFile()){
			return f.delete();
		}
		return false;
	}


	protected void verifyPermission(String user) throws UserPermissionException {
		if(user == null || !fsinfo.isPermitted(user)){
			throw new UserPermissionException(fsinfo.getServerName()+"@"+fsinfo.getUserName());
		}
	}
	
	

	public static void main( String args[]) throws Exception {
		try {
			if( args.length != 3){
				System.err.println("Usage: java WSFileServer <server_name> " +
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
			
			WSFileServer server = new WSFileServer( servername, username, url, path);
			Endpoint.publish("http://"+url+":8080/" + servername, server);
			
			IContactServer contactServer = (IContactServer) Naming.lookup("//"+contactserverURL+":1099/"+IContactServer.SERVER_NAME);
			contactServer.registerServer(server.getServerInfo());
		
			
			System.out.println("Server running @ http://" + url + ":8080/" + servername);
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
				System.out.println(reply);
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
