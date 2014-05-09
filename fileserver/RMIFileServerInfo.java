package fileserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;

public class RMIFileServerInfo 
		extends UnicastRemoteObject 
		implements IFileServerInfo {

	private static final long serialVersionUID = 1640959813069335227L;
	
	private String servername, username, url;
	private Set<String> permittedUsers;


	public RMIFileServerInfo(String servername, String username, String url) throws RemoteException {
		this.servername = servername;
		this.username = username;
		this.url = url;
		permittedUsers = new HashSet<String>();
	}

	@Override
	public String getServerName(){
		return servername;
	}
	
	@Override
	public String getUserName(){
		return username;
	}
	
	@Override
	public String getURL(){
		return url;
	}
	
	
	@Override
	public boolean isPermitted(String username){
		synchronized(permittedUsers){
			return permittedUsers.contains(username);
		}
	}
	
	public void addPermission(String username){
		synchronized(permittedUsers){
			permittedUsers.add(username);
		}
	}
	
	public void removePermission(String username){
		synchronized(permittedUsers){
			permittedUsers.remove(username);
		}
	}

	@Override
	public void ping() throws RemoteException {
		return;
	}

}
