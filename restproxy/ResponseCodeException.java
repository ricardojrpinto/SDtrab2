package restproxy;

public class ResponseCodeException extends Exception {

	
	private static final long serialVersionUID = -836186694246754993L;

	public ResponseCodeException(){
		super();
	}
	
	public ResponseCodeException(String msg){
		super(msg);
	}
}
