package fileserver;

public class UserPermissionException extends Exception {

	private static final long serialVersionUID = 5160936942496138827L;

	public UserPermissionException(){
		super();
	}
	
	public UserPermissionException(String s){
		super(s);
	}
}
