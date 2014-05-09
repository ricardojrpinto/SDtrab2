package restproxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.DropBoxApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import netutils.NetUtils;
import contactserver.IContactServer;

import fileserver.IFileServer;
import fileserver.IFileServerInfo;
import fileserver.RMIFileServerInfo;
import fileserver.UserPermissionException;
import fileutils.FileInfo;
import fileutils.InfoNotFoundException;

public class RESTProxy implements IFileServer {

	private static transient final String API_KEY = "cjnly006g3y0kpv";
	private static transient final String API_SECRET = "d0zn4p29roxkh1p";
	private static transient final String SCOPE = "dropbox";
	private static transient final String AUTHORIZE_URL = "https://www.dropbox.com/1/oauth/authorize?oauth_token=";
	private transient OAuthService oauthService;
	private transient Token accessToken;
	
	private IFileServerInfo fsinfo;
	
	protected RESTProxy(String servername,String username, String url) throws RemoteException{
		fsinfo = new RMIFileServerInfo(servername, username, url);
		this.getAPItokens();
	}
	
	private void getAPItokens(){
		oauthService = new ServiceBuilder().provider(DropBoxApi.class).apiKey(API_KEY)
				.apiSecret(API_SECRET).scope(SCOPE).build();

		// Obter Request token
		Token requestToken = oauthService.getRequestToken();
		
		System.out.println("In order for the app to proceed, you must access the following link:");
		System.out.println(AUTHORIZE_URL + requestToken.getToken());
		System.out.println("And press enter after granting authorization.");
		System.out.print(">>");
		new Scanner(System.in).next();
		
		Verifier verifier = new Verifier(requestToken.getSecret());
		accessToken = oauthService.getAccessToken(requestToken, verifier);
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
	public String[] dir(String path, String username) throws RemoteException,
			InfoNotFoundException, UserPermissionException {
		verifyPermission(username);
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.dropbox.com/1/metadata/dropbox/"+path+"?list=true");
		oauthService.signRequest(accessToken, request);
		Response response = request.send();
		
		if (response.getCode() != 200){
			System.err.println(response.getCode());
			return null;
		}
		
		JSONParser parser = new JSONParser();
		JSONObject res;
		try {
			res = (JSONObject) parser.parse(response.getBody());
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
		JSONArray items = (JSONArray) res.get("contents");
		List<String> files = new ArrayList<String>();
		Iterator it = items.iterator();
		
		while(it.hasNext()){
			String next = ((String)((JSONObject)it.next()).get("path"));
			files.add(next.substring(next.lastIndexOf('/')+1));
		}
		return files.toArray(new String[0]);	//the argument only serves to identify the parameterized type
	}

	@Override
	public FileInfo getFileInfo(String path, String username)
			throws RemoteException, InfoNotFoundException,
			UserPermissionException {
		verifyPermission(username);
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] downloadFile(String path, String username)
			throws RemoteException, UserPermissionException {
		verifyPermission(username);
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean uploadFile(String path, byte[] content, String username)
			throws RemoteException, UserPermissionException {
		verifyPermission(username);
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mkdir(String path, String username) throws RemoteException,
			UserPermissionException {
		verifyPermission(username);
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rmdir(String path, String username) throws RemoteException,
			UserPermissionException {
		verifyPermission(username);
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rm(String path, String username) throws RemoteException,
			UserPermissionException {
		verifyPermission(username);
		// TODO Auto-generated method stub
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

	/**
	 * @param args
	 */
	public static void main( String args[]) throws Exception {
		try {
			if( args.length != 2){
				System.err.println("Usage: java RESTProxy <server_name> " +
						"<user_name>");
				return;
			}
			
			String servername = args[0];
			//String contactserverURL = discoverContactSrvURL();
			String contactserverURL = "localhost";
			String username = args[1];
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
			
			
			IFileServer server = new RESTProxy(servername, username, url);
			
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
		DatagramSocket ds = new DatagramSocket(IContactServer.UDP_PORT);
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
