package fileutils;

import java.util.Date;

public class FileInfo implements java.io.Serializable
{
	private static final long serialVersionUID = -4498079336259690561L;

	public String name;
	public long length;
	public String lengthStr;
	public Date modified;
	public String modifiedStr;
	public boolean isFile;
	
	public FileInfo( String name, long length, Date modified, boolean isFile) {
		this.name = name;
		this.length = length;
		this.modified = modified;
		this.isFile = isFile;
	}
	
	public FileInfo( String name, String length, String modified, boolean isFile) {
		this.name = name;
		this.lengthStr = length;
		this.modified = null;
		this.modifiedStr = modified;
		this.isFile = isFile;
	}
	
	public String toString() {
		if(modified == null){
			return "Name : " + name + "\nLength: " + length + "\nData modified: " 
							+ modifiedStr + "\nisFile : " + isFile; 
		}
		return "Name : " + name + "\nLength: " + length + "\nData modified: " 
							+ modified + "\nisFile : " + isFile; 
	}
}
