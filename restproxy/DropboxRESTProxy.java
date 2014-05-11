package restproxy;

import java.io.File;
import java.io.FileOutputStream;
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

public class DropboxRESTProxy implements IFileServer {

	private static transient final String API_KEY = "cjnly006g3y0kpv";
	private static transient final String API_SECRET = "d0zn4p29roxkh1p";
	private static transient final String SCOPE = "dropbox";
	private static transient final String AUTHORIZE_URL = "https://www.dropbox.com/1/oauth/authorize?oauth_token=";
	private static final int INT_SIZE = 4;
	private transient OAuthService oauthService;
	private transient Token accessToken;
	
	private IFileServerInfo fsinfo;
	
	protected DropboxRESTProxy(String servername,String username, String url) throws RemoteException{
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
			throws InfoNotFoundException, UserPermissionException {
		verifyPermission(username);
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.dropbox.com/1/metadata/dropbox/"+path);
		oauthService.signRequest(accessToken, request);
		Response response = request.send();

		if (response.getCode() != 200){
			if(response.getCode() == 404)
				throw new InfoNotFoundException("File not found :" + path);
			System.err.println("Error: "+ response.getCode());
			return null;
		}

		try{
			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(response.getBody());
			String filepath = (String) res.get("path");
			String size = (String) res.get("size");
			String modified = (String) res.get("modified");
			boolean isDir =  (Boolean)res.get("is_dir");
			return new FileInfo(filepath.substring(filepath.lastIndexOf('/')+1),size,modified,isDir);
		} catch(ParseException e){
			System.err.println("Error: "+e.getMessage());
			return null;
		}
	}

	@Override
	public byte[] downloadFile(String path, String username)
			throws UserPermissionException  {
		verifyPermission(username);
		OAuthRequest request = new OAuthRequest(Verb.GET, 
				"https://api-content.dropbox.com/1/files/dropbox/"+path);
		oauthService.signRequest(accessToken, request);
		Response response = request.send();

		if (response.getCode() != 200){
			System.err.println("Error: "+ response.getCode());
			return null;
		}
		
		JSONParser parser = new JSONParser();
		byte[] data;
		try {
			JSONObject res = (JSONObject) parser.parse(response.getHeader("x-dropbox-metadata"));
			data = new byte[((Long)res.get("bytes")).intValue()];
			response.getStream().read(data);
		} catch (IOException | ParseException e) {
			System.err.println("Error: "+e.getMessage());
			return null;
		}
		return data;
	}

	@Override
	public boolean uploadFile(String path, byte[] content, String username)
			throws UserPermissionException {
		verifyPermission(username);
		OAuthRequest request = new OAuthRequest(Verb.PUT, 
				"https://api-content.dropbox.com/1/files_put/dropbox/"+path);
		
		request.addHeader("Content-Length", Integer.toString(content.length*INT_SIZE));
		//request.addHeader("Content-Type", this.getContentType(path));	
		request.addHeader("Content-Type", "application/octet-stream");	
		request.addPayload(content);

		oauthService.signRequest(accessToken, request);
		Response response = request.send();
		if (response.getCode() != 200){
			System.err.println("Error: "+ response.getCode());
			return false;
		}
		return true;
		
	}
	
	
	/*
	 * Supported MIME content types:
	 * 	-application/octet-stream (default)
	 * 	-application/pdf
	 * 	-application/zip
	 * 	-text/plain
	 * 	-image/gif
	 * 	-image/jpeg
	 * 	-image/png
	 */
//	protected String getContentType(String path) {
//		String format = path.substring(path.lastIndexOf('.')+1);
//		switch(format){
//		default: return "application/octet-stream";
//		case "pdf": return "application/pdf";
//		case "txt":
//		case "java":
//			return "text/plain";
//		case "zip":
//			return "application/zip";
//		
//		}
//	}

	@Override
	public boolean mkdir(String path, String username) throws UserPermissionException {
		verifyPermission(username);
		OAuthRequest request = new OAuthRequest(Verb.POST, 
				"https://api.dropbox.com/1/fileops/create_folder?root=dropbox&path="+path);
		
		oauthService.signRequest(accessToken, request);
		Response response = request.send();
		if (response.getCode() != 200){
			System.err.println("Error: "+ response.getCode());
			return false;
		}
		return true;
	}

	@Override
	public boolean rmdir(String path, String username) throws UserPermissionException {
		//the dropbox API method is the same for removing both directories and files
		return rm(path,username);
	}

	@Override
	public boolean rm(String path, String username) throws UserPermissionException {
		verifyPermission(username);
		OAuthRequest request = new OAuthRequest(Verb.POST, 
				"https://api.dropbox.com/1/fileops/delete?root=dropbox&path="+path);
		
		oauthService.signRequest(accessToken, request);
		Response response = request.send();

		if (response.getCode() != 200){
			System.err.println("Error: "+ response.getCode());
			return false;
		}
		return true;
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
			
			
			IFileServer server = new DropboxRESTProxy(servername, username, url);
			
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
