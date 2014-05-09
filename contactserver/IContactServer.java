
package contactserver;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import fileserver.IFileServerInfo;
import fileserver.WSFileServerInfo;

/**
 * @author Carlos Bate & Ricardo Pinto
 *
 */
public interface IContactServer extends Remote{
	
	static final String SERVER_NAME = "ContactServer";
	static final String MULTICAST_ADDRESS = "224.0.0.10";
	static final int MULTICAST_PORT = 6789;
	static final int UDP_PORT = 5000;	 
	
	/**
	 * Returns a map of servers accessible to user 'username'.
	 */
	List<String> getAccessibleServers(String username) throws RemoteException;
	
	/**
	 * Registers an RMI File Server with this contact server.
	 */
	void registerServer(IFileServerInfo server) throws RemoteException;
	
	/**
	 * Registers a WS File Server with this contact server.
	 */
	void registerServer(WSFileServerInfo server) throws RemoteException;

	/**
	 * Returns the URL of "server@user"
	 */
	String getURL(String server, String user) throws RemoteException;
}
