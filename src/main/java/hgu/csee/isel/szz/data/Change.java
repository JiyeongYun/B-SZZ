package hgu.csee.isel.szz.data;

public class Change {
	String sha1;
	String path;
	int numDeletedLines;
	int numAddedLines;
	
	public Change(String sha1,String path,int numDeletedLines,int numAddedLines){
		this.sha1 = sha1;
		this.path = path;
		this.numDeletedLines = numDeletedLines;
		this.numAddedLines = numAddedLines;
	}
	
	public int getNumDeletedLines(){
		return numDeletedLines;
	}
}
