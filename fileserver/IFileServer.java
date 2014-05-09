package fileserver;

import java.rmi.*;


import fileutils.FileInfo;
import fileutils.InfoNotFoundException;

public interface IFileServer extends Remote
{
	/**
	 * Returns information about the server, like server name, owner's name and list of
	 * authorized users.
	 */
	IFileServerInfo getServerInfo() throws RemoteException;
	
	/**
	 * Grants permission to a user to access the server, if he doesn't have it.
	 * Doesn't do anything, if the user already has permission.
	 */
	void addPermission(String username) throws RemoteException;
	
	/**
	 * Removes permission to user 'username' to access the server.
	 * Doesn't do anything, if the user didn't have permission already.
	 */
	void removePermission(String username) throws RemoteException;
	
	/**
	 * Returns an array of strings naming the files and directories in the directory 'path'.
	 */
	String[] dir( String path, String username) 
			throws RemoteException, InfoNotFoundException, UserPermissionException;
	
	/**
	 * Returns information about the file denoted by "path"
	 */
	FileInfo getFileInfo( String path, String username) 
			throws RemoteException, InfoNotFoundException, UserPermissionException;
	
	/**
	 * Downloads a file.
	 * @return the file in form of a byte array
	 */
	byte[] downloadFile(String path, String username) 
			throws RemoteException, UserPermissionException;
	
	/**
	 * Uploads a file
	 */
	boolean uploadFile(String path, byte[] content, String username) 
			throws RemoteException, UserPermissionException;
	
	/**
	 * Creates a directory
	 */
	boolean mkdir(String path, String username) 
			throws RemoteException, UserPermissionException;
	
	/**
	 * Removes the directory with pathname 'path', but only if it's empty.
	 */ 
	boolean rmdir(String path, String username) 
			throws RemoteException, UserPermissionException;
	
	/**
	 * Removes the file denoted by 'path'
	 */
	boolean rm(String path, String username) 
			throws RemoteException, UserPermissionException;
}
