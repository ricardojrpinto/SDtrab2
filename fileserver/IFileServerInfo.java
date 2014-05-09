package fileserver;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IFileServerInfo extends Remote {

	/**
	 * Returns the server's name.
	 */
	String getServerName() throws RemoteException;
	
	/**
	 * Returns the name of the user to whom the server belongs to.
	 */
	String getUserName() throws RemoteException;
	
	/**
	 * Returns the net address of the server in URL format.
	 */
	String getURL() throws RemoteException;
	
	/**
	 * Indicates if a user has permission to access the server
	 */
	boolean isPermitted(String username) throws RemoteException;
	
	void addPermission(String username) throws RemoteException;
	
	void removePermission(String username) throws RemoteException;

	/**
	 * Allows an invoker to test if the server machine is up and running.
	 * The method itself doesn't do anything
	 */
	void ping() throws RemoteException;
	
}
