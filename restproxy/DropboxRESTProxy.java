package restproxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import netutils.NetUtils;

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


import contactserver.IContactServer;
import fileserver.IFileServer;
import fileserver.IFileServerInfo;
import fileserver.RMIFileServerInfo;
import fileserver.UserPermissionException;
import fileutils.FileInfo;
import fileutils.InfoNotFoundException;

public class DropboxRESTProxy extends AbstractRESTProxy implements IFileServer {

	private static final String API_KEY = "cjnly006g3y0kpv";
	private static final String API_SECRET = "d0zn4p29roxkh1p";
	private static final String SCOPE = "dropbox";
	private static final String AUTHORIZE_URL = "https://www.dropbox.com/1/oauth/authorize?oauth_token=";
	private static final String TOKEN_FILENAME = "dropbox_token";
	
	private static final int INT_SIZE = 4;
	private OAuthService oauthService;
	private Token accessToken;
	
	private IFileServerInfo fsinfo;
	
	
	
	protected DropboxRESTProxy(String servername,String username, String url) throws RemoteException{
		fsinfo = new RMIFileServerInfo(servername, username, url);
		gettingToken = false;
		this.getAccessToken();
	}
	
	protected void getAccessToken(){
		oauthService = new ServiceBuilder().provider(DropBoxApi.class).apiKey(API_KEY)
				.apiSecret(API_SECRET).scope(SCOPE).build();

		File tokenFile = new File(TOKEN_FILENAME);
		if(tokenFile.exists()){
			try{
				ObjectInputStream tokenInputStream = new ObjectInputStream(new FileInputStream(tokenFile));
				accessToken = (Token) tokenInputStream.readObject();
				tokenInputStream.close();
				return;
			} catch (IOException | ClassNotFoundException  e){
				System.err.println(e.getMessage());
			}
		}
		// Obter Request token
		Token requestToken = oauthService.getRequestToken();
		
		System.out.println("In order for the app to proceed, you must access the following link:");
		System.out.println(AUTHORIZE_URL + requestToken.getToken());
		System.out.println("And press enter after granting authorization.");
		System.out.print(">>");
		new Scanner(System.in).next();
		
		Verifier verifier = new Verifier(requestToken.getSecret());
		accessToken = oauthService.getAccessToken(requestToken, verifier);
		
		try {
			ObjectOutputStream tokenOutStream = new ObjectOutputStream(new FileOutputStream(tokenFile));
			tokenOutStream.writeObject(accessToken);
			
			//Since the token contains the secret, it would be best 
			//to encrypt it or provide tight access control to the file storing it.
			//For simplicity's sake, we ignore this security measure.
			
			tokenOutStream.close();
		} catch (IOException e) {
			tokenFile.delete(); 
		}
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
		try{
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.dropbox.com/1/metadata/dropbox/"+path+"?list=true");
			oauthService.signRequest(accessToken, request);
			Response response = request.send();
			
			if (response.getCode() != 200){
				throw new ResponseCodeException(""+response.getCode());
			}
			
			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(response.getBody());
			JSONArray items = (JSONArray) res.get("contents");
			List<String> files = new ArrayList<String>();
			Iterator it = items.iterator();
			while(it.hasNext()){
				String next = ((String)((JSONObject)it.next()).get("path"));
				files.add(next.substring(next.lastIndexOf('/')+1));
			}
			
			return files.toArray(new String[0]);	//the argument only serves to identify the parameterized type
			
		} catch(ResponseCodeException e){
			handleResponseCodeException(e);
		} catch (ParseException e) {
			System.err.println("Error: "+e.getMessage());
		}
		return null;
	}

	@Override
	public FileInfo getFileInfo(String path, String username)
			throws InfoNotFoundException, UserPermissionException {
		verifyPermission(username);
		try{
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.dropbox.com/1/metadata/dropbox/"+path);
			oauthService.signRequest(accessToken, request);
			Response response = request.send();
	
			if (response.getCode() != 200){
				throw new ResponseCodeException(""+response.getCode());
			}

			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(response.getBody());
			String filepath = (String) res.get("path");
			String size = (String) res.get("size");
			String modified = (String) res.get("modified");
			boolean isDir =  (Boolean)res.get("is_dir");
			
			return new FileInfo(filepath.substring(filepath.lastIndexOf('/')+1),size,modified,isDir);
			
		} catch(ResponseCodeException e){
			if(e.getMessage().contains("404"))
				throw new InfoNotFoundException("File not found :" + path);
			handleResponseCodeException(e);
		} catch(ParseException e){
			System.err.println("Error: "+e.getMessage());
		}
		return null;
	}

	@Override
	public byte[] downloadFile(String path, String username)
			throws UserPermissionException  {
		verifyPermission(username);
		try{
			OAuthRequest request = new OAuthRequest(Verb.GET, 
					"https://api-content.dropbox.com/1/files/dropbox/"+path);
			oauthService.signRequest(accessToken, request);
			Response response = request.send();
	
			if (response.getCode() != 200){
				throw new ResponseCodeException(""+response.getCode());
			}
			
			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(response.getHeader("x-dropbox-metadata"));
			byte[] data = new byte[((Long)res.get("bytes")).intValue()];
			response.getStream().read(data);
			
			return data;
		} catch(ResponseCodeException e){
			handleResponseCodeException(e);
		} catch (IOException | ParseException e) {
			System.err.println("Error: "+e.getMessage());
		}
		return null;
	}

	@Override
	public boolean uploadFile(String path, byte[] content, String username)
			throws UserPermissionException {
		verifyPermission(username);
		try{
			OAuthRequest request = new OAuthRequest(Verb.PUT, 
					"https://api-content.dropbox.com/1/files_put/dropbox/"+path);
			
			request.addHeader("Content-Length", Integer.toString(content.length*INT_SIZE));
			request.addHeader("Content-Type", getMimeType(path));	
			request.addPayload(content);
	
			oauthService.signRequest(accessToken, request);
			Response response = request.send();
			if (response.getCode() != 200){
				throw new ResponseCodeException(""+response.getCode());
			}
			return true;
		} catch (ResponseCodeException e) {
			handleResponseCodeException(e);
		}
		return false;
	}
	
	
	@Override
	public boolean mkdir(String path, String username) throws UserPermissionException {
		verifyPermission(username);
		try{
			OAuthRequest request = new OAuthRequest(Verb.POST, 
					"https://api.dropbox.com/1/fileops/create_folder?root=dropbox&path="+path);
			
			oauthService.signRequest(accessToken, request);
			Response response = request.send();
			if (response.getCode() != 200){
				throw new ResponseCodeException(""+response.getCode());
			}
			return true;
		} catch (ResponseCodeException e) {
			handleResponseCodeException(e);
		}
		return false;
	}

	@Override
	public boolean rmdir(String path, String username) throws UserPermissionException {
		//the dropbox API method is the same for removing both directories and files
		return rm(path,username);
	}

	@Override
	public boolean rm(String path, String username) throws UserPermissionException {
		verifyPermission(username);
		try{
			OAuthRequest request = new OAuthRequest(Verb.POST, 
					"https://api.dropbox.com/1/fileops/delete?root=dropbox&path="+path);
			
			oauthService.signRequest(accessToken, request);
			Response response = request.send();
	
			if (response.getCode() != 200){
				throw new ResponseCodeException(""+response.getCode());
			}
			return true;
		} catch (ResponseCodeException e) {
			handleResponseCodeException(e);
			
		}
		return false;
	}
	
	
	protected void handleResponseCodeException(ResponseCodeException e) {
		if(e.getMessage() != null 
				&& e.getMessage().contains("401")
					&& !gettingToken){
			new File(TOKEN_FILENAME).delete();
			gettingToken = true;
			new TokenFetcher().start();
		}
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
				System.err.println("Usage: java DropboxRESTProxy <server_name> " +
						"<user_name>");
				return;
			}
			
			String servername = args[0];
			//TODO String contactserverURL = discoverContactSrvURL();
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
	
	

}
