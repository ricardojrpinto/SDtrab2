package restproxy;

import java.rmi.RemoteException;
import java.util.List;

import fileserver.IFileServer;
import fileserver.IFileServerInfo;
import fileserver.UserPermissionException;
import fileutils.FileInfo;
import fileutils.InfoNotFoundException;

public class RESTProxy implements IFileServer {


	@Override
	public IFileServerInfo getServerInfo() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addPermission(String username) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removePermission(String username) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String[] dir(String path, String username) throws RemoteException,
			InfoNotFoundException, UserPermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileInfo getFileInfo(String path, String username)
			throws RemoteException, InfoNotFoundException,
			UserPermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] downloadFile(String path, String username)
			throws RemoteException, UserPermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean uploadFile(String path, byte[] content, String username)
			throws RemoteException, UserPermissionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mkdir(String path, String username) throws RemoteException,
			UserPermissionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rmdir(String path, String username) throws RemoteException,
			UserPermissionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rm(String path, String username) throws RemoteException,
			UserPermissionException {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
	}

}
