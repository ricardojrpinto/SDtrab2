package fileclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import contactserver.IContactServer;
import fileclient.ws.InfoNotFoundException_Exception;
import fileclient.ws.UserPermissionException_Exception;
import fileclient.ws.WSFileServerService;
import fileserver.IFileServer;
import fileserver.UserPermissionException;
import fileutils.FileInfo;
import fileutils.InfoNotFoundException;


/**
 * Client's base class
 * @authors Carlos Bate & Ricardo Pinto
 */
public class FileClient
{
	private static final int MAX_TRIES = 5;
	private String contactServerURL;
	private String username;
	private Map<String,String> urls;		//keys follow the format 'server@user'
	
	protected FileClient( String username) throws IOException {
		this.contactServerURL = discoverContactSrvURL();
		this.username = username;
		urls = new HashMap<String,String>();
	}
	
	private String discoverContactSrvURL() throws IOException {
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
	

	/**
	 * Attempts to connect to the contact server
	 * @return the contact server instance, if successful;
	 * 			null, otherwise.
	 */
	protected IContactServer contactServer() {
		IContactServer contact = null;
		boolean contacted = false;
		for(int i=0; i<MAX_TRIES && !contacted; i++){
			try{
				System.out.println("contact url = " + contactServerURL);
				contact = (IContactServer) Naming.lookup("rmi://"+contactServerURL+"/"+
										IContactServer.SERVER_NAME);
				
				contacted = true;
			} catch (MalformedURLException | NotBoundException e) {
				e.printStackTrace();
			} catch (RemoteException e){
				if(i == MAX_TRIES-1){
					System.out.println("Connection failed...");
					return null;
				}
				System.out.println("Unable to connect to server. Retrying...");
				try {
					contactServerURL = discoverContactSrvURL();
					System.out.println("URL discovered: "+contactServerURL);
				} catch (IOException e1) {
					//Try again
				}
			}
		}
		return contact;
	}
	
	protected IFileServer fileServer(String url, String servername) {
		IFileServer server = null;
		boolean contacted = false;
		for(int i=0; i<MAX_TRIES && !contacted; i++){
			try{
				server = (IFileServer) Naming.lookup("//"+url+"/"+
										servername);
				contacted = true;
			} catch (MalformedURLException | NotBoundException e) {
				e.printStackTrace();
			} catch (RemoteException e){
				if(i == MAX_TRIES-1){
					System.out.println("Connection failed...");
					return null;
				}
				System.out.println("Unable to connect to server. Retrying...");
			}
		}
		return server;
	}


	/**
	 * Returns an iterator for the list of servers to whom this client has access
	 */
	protected Iterator<String> servers() {
		IContactServer contact = contactServer();
		if(contact == null) return null;
		for(int i=0; i<MAX_TRIES; i++){
			try{
				return contact.getAccessibleServers(username).iterator();
			} 
			catch(RemoteException e){
				System.out.println("Unable to invoke remote operation. Retrying...");	
			} 
		}
		System.err.println("The server might be down, please try again later.");
		return null;
	}
	
	/**
	 * Adiciona o utilizador user � lista de utilizadores com autoriza��o para aceder ao servidor
	 * server.
	 * Devolve false em caso de erro.
	 * NOTA: n�o deve lan�ar excepcao. 
	 * @throws MalformedURLException 
	 */
	protected boolean addPermission( String server, String user) throws MalformedURLException {
		for(int i=0; i<MAX_TRIES; i++){
			try{
				String url = "";
				
				IContactServer contact = contactServer();
				if(!urls.containsKey(server+"@"+username)){
					if(contact == null) return false;
					url = contact.getURL(server, username);
					urls.put(server+"@"+username, url);
				} else {
					url = urls.get(server+"@"+username);
				}
				if(url.contains("http")){
					System.out.println("Server URL:" + url + " server " + server);
					WSFileServerService service = new WSFileServerService(new URL(url+":8080/"+server+"?wsdl"));	
					fileclient.ws.WSFileServer wsServer = service.getWSFileServerPort();
					wsServer.addPermission(user);
					return true;
				}
				else{
					IFileServer fserver = fileServer(url, server+"@"+username);
					if(fserver == null) return false;
					
					fserver.addPermission(user);
					return true;
				}
			} 
			catch(RemoteException e){
				System.out.println("Unable to invoke remote operation. Retrying...");	
			} 
		}
		System.err.println("The server might be down, please try again later.");
		return false;
	}

	/**
	 * Remove o utilizador user da lista de utilizadores com autoriza��o para aceder ao servidor
	 * server.
	 * Devolve false em caso de erro.
	 * NOTA: n�o deve lan�ar excepcao. 
	 * @throws MalformedURLException 
	 */
	protected boolean remPermission( String server, String user) throws MalformedURLException {
		for(int i=0; i<MAX_TRIES; i++){
			try{
				String url = "";
				IContactServer contact = contactServer();
				if(!urls.containsKey(server+"@"+username)){
					if(contact == null) return false;
					url = contact.getURL(server, username);
					urls.put(server+"@"+username, url);
				} else {
					url = urls.get(server+"@"+username);
				}
				if(url!=null && url.contains("http")){
					WSFileServerService service = new WSFileServerService(new URL(url+":8080/"+server+"?wsdl"));
					fileclient.ws.WSFileServer wsServer = service.getWSFileServerPort();
					wsServer.removePermission(user);
					return true;
				}
				else{
					IFileServer fserver = fileServer(url, server+"@"+username);
					if(fserver == null) return false;
					fserver.removePermission(user);
					return true;
				}
			} 
			catch(RemoteException e){
				System.out.println("Unable to invoke remote operation. Retrying...");	
			} 
		}
		System.err.println("The server might be down, please try again later.");
		return false;
	}

	/**
	 * Devolve um array com os ficheiros/directoria na directoria dir no servidor server@user
	 * (ou no sistema de ficheiros do cliente caso server == null).
	 * Devolve null em caso de erro.
	 * NOTA: n�o deve lan�ar excepcao. 
	 * @throws MalformedURLException 
	 */
	protected String[] dir( String server, String user, String dir) throws MalformedURLException {
		for(int i=0; i<MAX_TRIES; i++){
			try{
				String url = "";
				if(!urls.containsKey(server+"@"+user)){
					IContactServer contact = contactServer();
					if(contact == null) return null;
					url = contact.getURL(server, user);
					if(url==null) return null;
					urls.put(server+"@"+user, url);
				} else {
					url = urls.get(server+"@"+user);
				}
				if(url.contains("http")){
					WSFileServerService service = new WSFileServerService(new URL(url+":8080/"+server+"?wsdl"));
					fileclient.ws.WSFileServer wsServer = service.getWSFileServerPort();
					try {
						List<String> aux = wsServer.dir(dir, username);
						Iterator<String> it = aux.iterator();
						String[] res = new String[aux.size()];
						for(int j = 0; j < res.length; j++)
							res[j] = it.next();
						return res;
					} catch (InfoNotFoundException_Exception e) {
						e.printStackTrace();
					} catch (UserPermissionException_Exception e) {
						e.printStackTrace();
					} 
				}
				else{
					IFileServer fserver = fileServer(url, server+"@"+user);
					if(fserver == null) return null;
					return fserver.dir(dir,username);
				}
			} catch(RemoteException e){
				System.out.println("Unable to invoke remote operation. Retrying...");	
			} catch(InfoNotFoundException e) {
				System.out.println(e.getMessage());
			} catch(UserPermissionException e){
				System.out.println("Request denied: user not permitted.\n");
				urls.remove(server+"@"+user);
				return null;
			}
		}
		System.err.println("The server might be down, please try again later.");
		return null;
		
	}
	
	/**
	 * Cria a directoria dir no servidor server@user
	 * (ou no sistema de ficheiros do cliente caso server == null).
	 * Devolve false em caso de erro.
	 * NOTA: n�o deve lan�ar excepcao. 
	 * @throws MalformedURLException 
	 */
	protected boolean mkdir( String server, String user, String dir) throws MalformedURLException {
		for(int i=0; i<MAX_TRIES; i++){
			try{
				String url = "";
				IContactServer contact = contactServer();
				if(!urls.containsKey(server+"@"+user)){
					if(contact == null) return false;
					url = contact.getURL(server, user);
					if(url==null) return false;
					urls.put(server+"@"+user, url);
				} else {
					url = urls.get(server+"@"+user);
				}
				if(url.contains("http")){
					WSFileServerService service = new WSFileServerService(new URL(url + ":8080/" + server + "?wsdl"));
					fileclient.ws.WSFileServer wsServer = service.getWSFileServerPort(); 
					try {
						return wsServer.mkdir(dir, username);
					} catch (UserPermissionException_Exception e) {
						e.printStackTrace();
					}
				}
				else{
					IFileServer fserver = fileServer(url, server+"@"+user);
					if(fserver == null) return false;
					return fserver.mkdir(dir,username);
				}
			} catch(RemoteException e){
				System.out.println("Unable to invoke remote operation. Retrying...");	
			} catch(UserPermissionException e){
				System.out.println("Request denied: user not permitted.\n");
				urls.remove(server+"@"+user);
				return false;
			}
		}
		System.err.println("The server might be down, please try again later.");
		return false;
	}

	/**
	 * Remove a directoria dir no servidor server@user
	 * (ou no sistema de ficheiros do cliente caso server == null).
	 * Devolve false em caso de erro.
	 * NOTA: n�o deve lan�ar excepcao. 
	 * @throws MalformedURLException 
	 */
	protected boolean rmdir( String server, String user, String dir) throws MalformedURLException {
		for(int i=0; i<MAX_TRIES; i++){
			try{
				String url = "";
				IContactServer contact = contactServer();
				if(!urls.containsKey(server+"@"+user)){
					if(contact == null) return false;
					url = contact.getURL(server, user);
					if(url==null) return false;
					urls.put(server+"@"+user, url);
				} else {
					url = urls.get(server+"@"+user);
				}
				if(url.contains("http")){
					WSFileServerService service = new WSFileServerService(new URL(url + ":8080/" + server + "?wsdl"));
					fileclient.ws.WSFileServer wsServer = service.getWSFileServerPort();
					try {
						return wsServer.rmdir(dir, username);
					} catch (UserPermissionException_Exception e) {
						e.printStackTrace();
					}
				}
				else{
					IFileServer fserver = fileServer(url, server+"@"+user);
					if(fserver == null) return false;
					return fserver.rmdir(dir,username);
				}
			} catch(RemoteException e){
				System.out.println("Unable to invoke remote operation. Retrying...");	
			} catch(UserPermissionException e){
				System.out.println("Request denied: user not permitted.\n");
				urls.remove(server+"@"+user);
				return false;
			}
		}
		System.err.println("The server might be down, please try again later.");
		return false;
	}

	/**
	 * Remove o ficheiro path no servidor server@user.
	 * (ou no sistema de ficheiros do cliente caso server == null).
	 * Devolve false em caso de erro.
	 * NOTA: n�o deve lan�ar excepcao. 
	 * @throws MalformedURLException 
	 */
	protected boolean rm( String server, String user, String path) throws MalformedURLException {
		for(int i=0; i<MAX_TRIES; i++){
			try{
				String url = "";
				IContactServer contact = contactServer();
				if(!urls.containsKey(server+"@"+user)){
					if(contact == null) return false;
					url = contact.getURL(server, user);
					if(url==null) return false;
					urls.put(server+"@"+user, url);
				} else {
					url = urls.get(server+"@"+user);
				}
				if(url.contains("http")){
					WSFileServerService service = new WSFileServerService(new URL(url + ":8080/" + server + "?wsdl"));
					fileclient.ws.WSFileServer wsServer = service.getWSFileServerPort();
					try {
						return wsServer.rm(path, username);
					} catch (UserPermissionException_Exception e) {
						e.printStackTrace();
					}
				}
				else{
					IFileServer fserver = fileServer(url, server+"@"+user);
					if(fserver == null) return false;
					return fserver.rm(path,username);
				}
			} catch(RemoteException e){
				System.out.println("Unable to invoke remote operation. Retrying...");	
			} catch(UserPermissionException e){
				System.out.println("Request denied: user not permitted.\n");
				urls.remove(server+"@"+user);
				return false;
			}
		}
		System.err.println("The server might be down, please try again later.");
		return false;
	}

	/**
	 * Devolve informacao sobre o ficheiro/directoria path no servidor server@user.
	 * (ou no sistema de ficheiros do cliente caso server == null).
	 * Devolve false em caso de erro.
	 * NOTA: n�o deve lan�ar excepcao. 
	 * @throws MalformedURLException 
	 */
	protected FileInfo getAttr( String server, String user, String path) throws MalformedURLException {
		for(int i=0; i<MAX_TRIES; i++){
			try{
				String url = "";
				IContactServer contact = contactServer();
				if(!urls.containsKey(server+"@"+user)){
					if(contact == null) return null;
					url = contact.getURL(server, user);
					if(url==null) return null;
					urls.put(server+"@"+user, url);
				} else {
					url = urls.get(server+"@"+user);
				}
				if(url.contains("http")){
					WSFileServerService service = new WSFileServerService(new URL(url + ":8080/" + server + "?wsdl"));
					fileclient.ws.WSFileServer wsServer = service.getWSFileServerPort();
					fileclient.ws.FileInfo f = null;
					try {
						f = wsServer.getFileInfo(path, username);
					} catch (InfoNotFoundException_Exception
							| UserPermissionException_Exception e) {
						e.printStackTrace();
					}
					return new FileInfo(f.getName(), f.getLength(), f.getModified().toGregorianCalendar().getTime(), f.isIsFile());
				}
				else{
					IFileServer fserver = fileServer(url, server+"@"+user);
					if(fserver == null) return null;
					return fserver.getFileInfo(path, username);
				}
			} catch(RemoteException e){
				System.out.println("Unable to invoke remote operation. Retrying...");	
			} catch (InfoNotFoundException e) {
				System.out.println(e.getMessage());
				return null;
			} 
			catch(UserPermissionException e){
				System.out.println("Request denied: user not permitted.\n");
				urls.remove(server+"@"+user);
				return null;
			}
		}
		System.err.println("The server might be down, please try again later.");
		return null;
	}

	/**
	 * Copia ficheiro de fromPath no servidor fromServer@fromUser para o ficheiro 
	 * toPath no servidor toServer@toUser.
	 * (caso fromServer/toServer == local, corresponde ao sistema de ficheiros do cliente).
	 * Devolve false em caso de erro.
	 * NOTA: n�o deve lan�ar excepcao. 
	 * @throws MalformedURLException 
	 */
	protected boolean cp( String fromServer, String fromUser, String fromPath,
							String toServer, String toUser, String toPath) throws MalformedURLException {
		boolean fromIsLocal = (fromServer == null || fromUser == null);
		boolean toIsLocal = (toServer == null || toUser == null);
		IContactServer contact = null;
		
		for(int i=0; i<MAX_TRIES; i++){
			try{
				byte[] fileBytes = null;
				if(fromIsLocal){
					fileBytes = downloadLocalFile(fromPath);
				} else {
					String urlFrom;
					if(!urls.containsKey(fromServer+"@"+fromUser)){
						if(contact==null){
							contact = contactServer();
							if(contact == null) return false;
						}
						urlFrom  = contact.getURL(fromServer, fromUser);
					} else{
						urlFrom = urls.get(fromServer+"@"+fromUser);
					}
					if(urlFrom.contains("http")){
						WSFileServerService service = new WSFileServerService(new URL(urlFrom + ":8080/" + fromServer + "?wsdl"));
						fileclient.ws.WSFileServer wsServer = service.getWSFileServerPort();
						try {
							fileBytes = wsServer.downloadFile(fromPath, username);
						} catch (UserPermissionException_Exception e) {
							e.printStackTrace();
						}
					}
					else{
						IFileServer fromFileServer = fileServer(urlFrom, fromServer+"@"+fromUser);
						if(fromFileServer == null) return false;
						fileBytes = fromFileServer.downloadFile(fromPath, username);
					}
				}
				if(fileBytes == null) return false;
				if(toIsLocal){
					return uploadLocalFile(toPath, fileBytes);
				} else {
					String urlTo;
					if(!urls.containsKey(toServer+"@"+toUser)){
						if(contact==null){
							contact = contactServer();
							if(contact == null) return false;
						}
						urlTo = contact.getURL(toServer, toUser);
					} else {
						urlTo = urls.get(toServer+"@"+toUser);
					}
					if(urlTo.contains("http")){
						WSFileServerService service = new WSFileServerService(new URL(urlTo + ":8080/" + toServer + "?wsdl"));
						fileclient.ws.WSFileServer wsServer = service.getWSFileServerPort();
						try {
							return wsServer.uploadFile(toPath, fileBytes,username);
						} catch (UserPermissionException_Exception e) {
							e.printStackTrace();
						}
					}
					else{
						IFileServer toFileServer = fileServer(urlTo, toServer+"@"+toUser);
						if(toFileServer == null) return false;
						return toFileServer.uploadFile(toPath, fileBytes,username);
					}
				}
				
			} catch(RemoteException e){
				System.out.println("Unable to invoke remote operation. Retrying...");	
			} catch (UserPermissionException e) {
				System.out.println("Request to server "+e.getMessage()+" denied:" +
						" user not permitted.\n");
				urls.remove(e.getMessage());
				return false;
			}
		}
		System.err.println("The server might be down, please try again later.");
		return false;
	}

	private boolean uploadLocalFile(String toPath, byte[] fileBytes) {
		try {
			RandomAccessFile f = new RandomAccessFile(new File(toPath), "rw");
			f.write(fileBytes);
			f.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return false;
		}
		return true;
	}


	private byte[] downloadLocalFile(String fromPath) {
		try {
			RandomAccessFile f = new RandomAccessFile(new File(fromPath), "r");
			long size = f.length();
			byte[] b = new byte[(int)size];
			f.readFully(b);
			f.close();
			return b;
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		return null;
	}


	protected void doit() throws IOException {
		BufferedReader reader = new BufferedReader( new InputStreamReader( System.in));
		
		for( ; ; ) {
			System.out.print(">");
			String line = reader.readLine();
			if( line == null)
				continue;
			String[] cmd = line.split(" ");
			if( cmd[0].equalsIgnoreCase("servers")) {
				Iterator<String> s = servers();
				if(s == null){
					System.out.println("Unable to obtain list of servers.");
				} else if(!s.hasNext()){
					System.out.println( "You have no permission to access any server.");
				} else {
					while(s.hasNext()){
						System.out.println(s.next());
					}
				}
			} else if( cmd[0].equalsIgnoreCase("addPermission")) {
				String server = cmd[1];
				String user = cmd[2];
				
				boolean b = addPermission( server, user);
				
				if( b)
					System.out.println( "success");
				else
					System.out.println( "error");
			} else if( cmd[0].equalsIgnoreCase("remPermission")) {
				String server = cmd[1];
				String user = cmd[2];
				
				boolean b = remPermission( server, user);
				
				if( b)
					System.out.println( "success");
				else
					System.out.println( "error");
			} else if( cmd[0].equalsIgnoreCase("ls")) {
				String[] dirserver = cmd[1].split(":");
				String[] serveruser = dirserver[0].split("@");
				
				String server = dirserver.length == 1 ? null : serveruser[0];
				String user = dirserver.length == 1 || serveruser.length == 1 ? null : serveruser[1];
				String dir = dirserver.length == 1 ? dirserver[0] : dirserver[1];
				
				String[] res = dir( server, user, dir);
				if( res != null) {
					System.out.println( res.length);
					for( int i = 0; i < res.length; i++)
						System.out.println( res[i]);
				} else
					System.out.println( "error");
			} else if( cmd[0].equalsIgnoreCase("mkdir")) {
				String[] dirserver = cmd[1].split(":");
				String[] serveruser = dirserver[0].split("@");
				
				String server = dirserver.length == 1 ? null : serveruser[0];
				String user = dirserver.length == 1 || serveruser.length == 1 ? null : serveruser[1];
				String dir = dirserver.length == 1 ? dirserver[0] : dirserver[1];

				boolean b = mkdir( server, user, dir);
				if( b)
					System.out.println( "success");
				else
					System.out.println( "error");
			} else if( cmd[0].equalsIgnoreCase("rmdir")) {
				String[] dirserver = cmd[1].split(":");
				String[] serveruser = dirserver[0].split("@");
				
				String server = dirserver.length == 1 ? null : serveruser[0];
				String user = dirserver.length == 1 || serveruser.length == 1 ? null : serveruser[1];
				String dir = dirserver.length == 1 ? dirserver[0] : dirserver[1];

				boolean b = rmdir( server, user, dir);
				if( b)
					System.out.println( "success");
				else
					System.out.println( "error");
			} else if( cmd[0].equalsIgnoreCase("rm")) {
				String[] dirserver = cmd[1].split(":");
				String[] serveruser = dirserver[0].split("@");
				
				String server = dirserver.length == 1 ? null : serveruser[0];
				String user = dirserver.length == 1 || serveruser.length == 1 ? null : serveruser[1];
				String path = dirserver.length == 1 ? dirserver[0] : dirserver[1];

				boolean b = rm( server, user, path);
				if( b)
					System.out.println( "success");
				else
					System.out.println( "error");
			} else if( cmd[0].equalsIgnoreCase("getattr")) {
				String[] dirserver = cmd[1].split(":");
				String[] serveruser = dirserver[0].split("@");
				
				String server = dirserver.length == 1 ? null : serveruser[0];
				String user = dirserver.length == 1 || serveruser.length == 1 ? null : serveruser[1];
				String path = dirserver.length == 1 ? dirserver[0] : dirserver[1];

				FileInfo info = getAttr( server, user, path);
				if( info != null) {
					System.out.println( info);
					System.out.println( "success");
				} else
					System.out.println( "error");
			} else if( cmd[0].equalsIgnoreCase("cp")) {
				String[] dirserver1 = cmd[1].split(":");
				String[] serveruser1 = dirserver1[0].split("@");
				
				String fromServer = dirserver1.length == 1 ? null : serveruser1[0];
				String fromUser = dirserver1.length == 1 || serveruser1.length == 1 ? null : serveruser1[1];
				String fromPath = dirserver1.length == 1 ? dirserver1[0] : dirserver1[1];

				String[] dirserver2 = cmd[2].split(":");
				String[] serveruser2 = dirserver2[0].split("@");
				
				String toServer = dirserver2.length == 1 ? null : serveruser2[0];
				String toUser = dirserver2.length == 1 || serveruser2.length == 1 ? null : serveruser2[1];
				String toPath = dirserver2.length == 1 ? dirserver2[0] : dirserver2[1];

				boolean b = cp( fromServer, fromUser, fromPath, toServer, toUser, toPath);
				if( b)
					System.out.println( "success");
				else
					System.out.println( "error");
			} else if( cmd[0].equalsIgnoreCase("help")) {
				System.out.println("servers - lista URLs dos servidores a que tem acesso");
				System.out.println("addPermission server user - adiciona user a lista de utilizadores com permissoes para aceder a server");
				System.out.println("remPermission server user - remove user da lista de utilizadores com permissoes para aceder a server");
				System.out.println("ls server@user:dir - lista ficheiros/directorias presentes na directoria dir (. e .. tem o significado habitual), caso existam ficheiros com o mesmo nome devem ser apresentados como nome@server;");
				System.out.println("mkdir server@user:dir - cria a directoria dir no servidor server@user");
				System.out.println("rmdir server@user:dir - remove a directoria dir no servidor server@user");
				System.out.println("cp path1 path2 - copia o ficheiro path1 para path2; quando path representa um ficheiro num servidor deve ter a forma server@user:path, quando representa um ficheiro local deve ter a forma path");
				System.out.println("rm server@user:path - remove o ficheiro path");
				System.out.println("getattr server@user:path - apresenta informacao sobre o ficheiro/directoria path, incluindo: nome, boolean indicando se e' ficheiro, data da criacao, data da ultima modificacao");
			} else if( cmd[0].equalsIgnoreCase("exit"))
				break;

		}
	}
	
	public static void main( String[] args) {
		if( args.length != 1) {
			System.err.println("Use: java fileclient.FileClient nome_utilizador");
			return;
		}
		try {
			new FileClient( args[0]).doit();
		} catch (IOException e) {
			System.err.println("Error:" + e.getMessage());
			e.printStackTrace();
		}
	}
	
}
