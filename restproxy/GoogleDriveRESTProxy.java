package restproxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import netutils.NetUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
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

public class GoogleDriveRESTProxy extends AbstractRESTProxy implements IFileServer {

	private static final String CLIENT_KEY = "220807002031-enjrp5tf4mjunj838plracj5fcqosr4b.apps.googleusercontent.com";
	private static final String CLIENT_SECRET = "jw7FUMTVGPiYJbTId7tyvNwU";
	private static final String SCOPE = "https://www.googleapis.com/auth/drive";
	private static final String TOKEN_FILENAME = "gdrive_token";
	
	private OAuthService oauthService;
	private Token accessToken;
	private IFileServerInfo fsinfo;
	
	public GoogleDriveRESTProxy(String servername, String username, String url) throws RemoteException {
		fsinfo = new RMIFileServerInfo(servername, username, url);
		gettingToken = false;
		this.getAccessToken();
	}
	
	protected void getAccessToken(){
		oauthService = new ServiceBuilder().provider(Google2Api.class).apiKey(CLIENT_KEY).
				apiSecret(CLIENT_SECRET).scope(SCOPE).build();
		Scanner in = new Scanner(System.in);
		
		
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
		String authorizationUrl = oauthService.getAuthorizationUrl(null);
		
		System.out.println("In order for the app to proceed, you must access the following link:");
		System.out.println(authorizationUrl);
		System.out.println("Grant permission to the app and paste the code given here:");
		System.out.print(">>");
		Verifier verifier = new Verifier(in.nextLine());
		accessToken = oauthService.getAccessToken(null, verifier);
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
		try {
			Response response = traversePath(path);
			
			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(response.getBody());
			
				
			JSONArray items = (JSONArray) res.get("items");
			List<String> files = new ArrayList<String>();
			Iterator it = items.iterator();
			
			while(it.hasNext()){
				JSONObject next = (JSONObject) it.next();
				JSONObject labels = (JSONObject) next.get("labels");
				if(!(Boolean)labels.get("trashed") && !(Boolean)labels.get("restricted")){
					String nextFile = ((String)next.get("title"));
					files.add(nextFile);
				}
			}
			return files.toArray(new String[0]);
			
		} catch (ResponseCodeException e) {
			handleResponseCodeException(e);
			
		} catch (FileNotFoundException | ParseException e) {} 
		return null;
	}

	@Override
	public FileInfo getFileInfo(String path, String username)
			throws RemoteException, InfoNotFoundException,
			UserPermissionException {
		verifyPermission(username);
		try {
			Response response = traversePath(path);
			
			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(response.getBody());
			
			String filepath = (String) res.get("title");
			String size = res.get("fileSize") != null ? (String) res.get("fileSize") : "0";
			String modified = (String) res.get("modifiedDate");
			String mimeType = (String) res.get("mimeType");
			boolean isDir = mimeType.contains("folder");
			
			return new FileInfo(filepath,size,modified, isDir);
		} catch (ResponseCodeException e) {
			handleResponseCodeException(e);
			
		} catch (FileNotFoundException | ParseException e) {} 
		return null;
	}

	@Override
	public byte[] downloadFile(String path, String username)
			throws RemoteException, UserPermissionException {
		verifyPermission(username);
		try {
			Response response = traversePath(path);
			
			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(response.getBody());
			
				
			JSONArray items = (JSONArray) res.get("items");
			Iterator it = items.iterator();
			String downloadURL = (String) ((JSONObject)it.next()).get("downloadUrl");
			
			OAuthRequest request = new OAuthRequest(Verb.GET, downloadURL);
			oauthService.signRequest(accessToken, request);
			response = request.send();
		
			if (response.getCode() != 200){		
				throw new ResponseCodeException(""+response.getCode());
			}
			int size = Integer.parseInt(response.getHeader("Content-Length"));
			byte[] data = new byte[size];
			response.getStream().read(data);
			
			return data;
		} catch (ResponseCodeException e) {
			handleResponseCodeException(e);
			
		} catch (ParseException | IOException e) {} 
		return null;
	}

	@Override
	public boolean uploadFile(String path, byte[] content, String username)
			throws RemoteException, UserPermissionException {
		verifyPermission(username);
		try {
			if(path.endsWith("/")){
				path = path.substring(0, path.lastIndexOf('/'));
			}
			String parent = path.substring(0,path.lastIndexOf('/'));
			String toUpload = path.substring(path.lastIndexOf('/')+1);
			Response response = traversePath(parent);
			
			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(response.getBody());
			String parentId = (String)res.get("id");
			
			OAuthRequest request = new OAuthRequest(Verb.POST, 
					"https://www.googleapis.com/upload/drive/v2/files?uploadType=multipart");
			
			JSONObject fileMetadata = new JSONObject();
			fileMetadata.put("title", toUpload);
			JSONArray parents = new JSONArray();
			JSONObject parentFolder = new JSONObject();
			parentFolder.put("kind", "drive#fileLink");
			parentFolder.put("id", parentId);
			parents.add(parentFolder);
			fileMetadata.put("parents", parents);
			request.addHeader("Content-Type", "multipart/related; boundary=\"buhzinga\"");
			StringBuilder body = new StringBuilder();
			body.append("--buhzinga\n");
			body.append("Content-Type: application/json; charset=Unicode\n\n");
			body.append(fileMetadata.toString()+"\n\n");
			body.append("--buhzinga\n");
			body.append("Content-Type: "+getMimeType(toUpload)+"\n\n");
			byte[] bodyBytes = body.toString().getBytes();
			
			body = new StringBuilder();
			body.append("\n\n--buhzinga--");
			byte[] bodyBytes2 = body.toString().getBytes();
			
			byte[] payload = new byte[bodyBytes.length+bodyBytes2.length+content.length];
			System.arraycopy(bodyBytes, 0, payload, 0, bodyBytes.length);
			System.arraycopy(content, 0, payload, bodyBytes.length, content.length);
			System.arraycopy(bodyBytes2, 0, payload, bodyBytes.length+content.length, bodyBytes2.length);
			
			request.addPayload(payload);
			oauthService.signRequest(accessToken, request);
			
			response = request.send();
			if (response.getCode() != 200){		
				throw new ResponseCodeException(""+response.getCode());
			}
			return true;
			
		} catch (ResponseCodeException e) {
			handleResponseCodeException(e);
			
		} catch (ParseException | FileNotFoundException e) {} 
		return false;
	}

	@Override
	public boolean mkdir(String path, String username) throws RemoteException,
			UserPermissionException {
		verifyPermission(username);
		try {
			if(path.endsWith("/")){
				path = path.substring(0, path.lastIndexOf('/'));
			}
			String parent = path.substring(0,path.lastIndexOf('/'));
			String toCreate = path.substring(path.lastIndexOf('/')+1);
			
			Response response = traversePath(parent);
			
			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(response.getBody());

			String id = (String)res.get("id");
			if(!((String)res.get("mimeType")).contains("folder")) return false;	//not a folder
			
			OAuthRequest request = new OAuthRequest(Verb.POST, 
					"https://www.googleapis.com/drive/v2/files/");
			
			//build the request
			request.addHeader("Content-Type", "application/json");
			StringBuilder bodyBuilder = new StringBuilder();
			bodyBuilder.append("{ \"title\": \""+toCreate+"\", ");
			bodyBuilder.append(" \"parents\": [{\"id\":\""+id+"\"}], ");
			bodyBuilder.append(" \"mimeType\": \"application/vnd.google-apps.folder\" }");
			request.addPayload(bodyBuilder.toString());
			
			//sign and send
			oauthService.signRequest(accessToken, request);
			response = request.send();
			
			if (response.getCode() != 200){		
				throw new ResponseCodeException(""+response.getCode());
			}
			return true;
		} catch (ResponseCodeException e) {
			handleResponseCodeException(e);
			
		} catch (FileNotFoundException | ParseException e) {} 
		return false;
	}

	@Override
	public boolean rmdir(String path, String username) throws RemoteException,
			UserPermissionException {
		verifyPermission(username);
		try {
			Response response = traversePath(path);
			
			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(response.getBody());
			
			String id = (String)res.get("id");
			if(!((String)res.get("mimeType")).contains("folder")) return false;	//not a folder
			
			//first: verify that it's empty
			OAuthRequest request = new OAuthRequest(Verb.GET, 
					"https://www.googleapis.com/drive/v2/files/"+id+"/children");
			oauthService.signRequest(accessToken, request);
			response = request.send();
			
			if (response.getCode() != 200){		
				throw new ResponseCodeException(""+response.getCode());
			}
			
			parser = new JSONParser();
			res = (JSONObject) parser.parse(response.getBody());
			
			JSONArray items = (JSONArray) res.get("items");
			if(!items.isEmpty()) return false;	//dir is not empty
			
			//second: remove dir
			request = new OAuthRequest(Verb.DELETE, 
					"https://www.googleapis.com/drive/v2/files/"+id);
			oauthService.signRequest(accessToken, request);
			response = request.send();
			if (response.getCode() != 204){		//it must return "204: No Content"
				throw new ResponseCodeException(""+response.getCode());
			}
			return true;
		} catch (ResponseCodeException e) {
			handleResponseCodeException(e);
			
		} catch (FileNotFoundException | ParseException e) {} 
		return false;
	}

	@Override
	public boolean rm(String path, String username) throws RemoteException,
			UserPermissionException {
		verifyPermission(username);
		try {
			Response response = traversePath(path);
			
			JSONParser parser = new JSONParser();
			JSONObject res = (JSONObject) parser.parse(response.getBody());
			
			String id = (String)res.get("id");
			if(((String)res.get("mimeType")).contains("folder")) return false;	//it's a folder
			
			OAuthRequest request = new OAuthRequest(Verb.DELETE, 
					"https://www.googleapis.com/drive/v2/files/"+id);
			oauthService.signRequest(accessToken, request);
			response = request.send();
			if (response.getCode() != 204){		//it must return "204: No Content"
				throw new ResponseCodeException(""+response.getCode());
			}
			return true;
		} catch (ResponseCodeException e) {
			handleResponseCodeException(e);
			
		} catch (FileNotFoundException | ParseException e) {} 
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if( args.length != 2){
				System.err.println("Usage: java GoogleDriveRESTProxy <server_name> " +
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
			
			
			IFileServer server = new GoogleDriveRESTProxy(servername, username, url);
			
			Naming.rebind( servername+"@"+username, server);
			
			IContactServer contactServer = (IContactServer) Naming.lookup
					("//"+contactserverURL+":1099/"+IContactServer.SERVER_NAME);
			contactServer.registerServer(server.getServerInfo());
			
			System.out.println( "DirServer "+servername+" bound in registry");
			
		} catch( Throwable th) {
			th.printStackTrace();
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
	
	protected void handleResponseCodeException(ResponseCodeException e) {
		if(e.getMessage() != null 
				&& e.getMessage().contains("401")
					&& !gettingToken){
			new File(TOKEN_FILENAME).delete();
			gettingToken = true;
			new TokenFetcher().start();
		}
	}


	/**
	 * Given a filepath, it gets the folder or file metadata by traversing the whole path,
	 * making a GET request per path token
	 * @throws FileNotFoundException 
	 * @throws ResponseCodeException 
	 */
	protected Response traversePath(String filepath) 
			throws FileNotFoundException, ResponseCodeException{
		OAuthRequest request = new OAuthRequest(Verb.GET, 
				"https://www.googleapis.com/drive/v2/files?q=%27root%27+in+parents");
		request.setConnectionKeepAlive(true);
		oauthService.signRequest(accessToken, request);
		Response response = request.send();
		
		if (response.getCode() != 200){
			throw new ResponseCodeException(""+response.getCode());
		}
		Iterator fpit = new FilePathIterator(filepath);
		String id;
		while(fpit.hasNext()){
			JSONParser parser = new JSONParser();
			JSONObject res;
			try {
				res = (JSONObject) parser.parse(response.getBody());
			} catch (ParseException e) {
				return null;
			}
			
			JSONArray items = (JSONArray) res.get("items");
			Iterator it = items.iterator();
			boolean foundFile = false;
			id = null;
			JSONObject nextFile = null;
			String nextPathToken = (String) fpit.next();
			
			while(it.hasNext() && !foundFile){
				nextFile = (JSONObject) it.next();
				String title = (String) nextFile.get("title");
				JSONObject labels = (JSONObject) nextFile.get("labels");
				if(title.equals(nextPathToken) && 
						!(Boolean)labels.get("trashed") && !(Boolean)labels.get("restricted")){
					foundFile = true;
					id = (String) nextFile.get("id");
				}
			}
			
			if(nextFile == null){
				throw new FileNotFoundException("Unable to find file/directory "+nextPathToken);
			}
			if(id == null) break;	
			
			String resource = 
						((String)nextFile.get("mimeType")).contains("folder") && fpit.hasNext()? 
							"?q=%27"+id+"%27+in+parents" :
							"/"+id;
			request = new OAuthRequest(Verb.GET, 
							"https://www.googleapis.com/drive/v2/files"+ resource);
			request.setConnectionKeepAlive(fpit.hasNext());
			oauthService.signRequest(accessToken, request);
			response = request.send();
			
			if (response.getCode() != 200){
				throw new ResponseCodeException(""+response.getCode());
			}
		}
		return response;
	}
	
	protected static class FilePathIterator implements Iterator<String>{

		private int current;
		private String[] pathTokens;
		
		FilePathIterator(String path){
			current = -1;
			pathTokens = path.split("/");
			while(pathTokens[++current].trim().equals(""));
			current--;
		}
		
		@Override
		public boolean hasNext() {
			return current < pathTokens.length - 1;
		}

		@Override
		public String next() {
			try{
				return pathTokens[++current];
			} catch (IndexOutOfBoundsException e){
				return "";
			}
			
		}

		@Override
		public void remove() {}
		
	}

}
