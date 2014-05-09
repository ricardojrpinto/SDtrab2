package fileserver;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import javax.jws.*;


@WebService
public class WSFileServerInfo implements Serializable{
	private static final long serialVersionUID = 1640959813069335227L;
	
	private String servername, username, url;
	private Set<String> permittedUsers;

	public WSFileServerInfo(String servername, String username, String url){
		this.servername = servername;
		this.username = username;
		this.url = url;
		permittedUsers = new HashSet<String>();
		permittedUsers.add(username);		//add owner to permitted users
	}

	@WebMethod
	public String getServerName(){
		return servername;
	}
	
	@WebMethod
	public String getUserName(){
		return username;
	}
	
	@WebMethod
	public String getURL(){
		return url;
	}
	
	
	@WebMethod
	public boolean isPermitted(String username){
		return permittedUsers.contains(username);
	}
	
	public void addPermission(String username){
		permittedUsers.add(username);
	}
	
	public void removePermission(String username){
		permittedUsers.remove(username);
	}
	
	@WebMethod
	public void ping() {
		return;
	}

}
