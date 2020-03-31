package hgu.csee.isel.alinew.szz.bszz.data;

public class DeletedLineInCommits {
	String sha1 = "";
	String date = "";
	String oldPath = "";
	String path = "";
	int lineNum = 0;
	String line = "";
	
	public DeletedLineInCommits(String sha1,String date,String oldPath,String path,int lineNum,String line){
		this.sha1 = sha1;
		this.date = date;
		this.oldPath = oldPath;
		this.path = path;
		this.lineNum = lineNum;
		this.line = line;
	}
	
	public String getSha1() {
		return sha1;
	}

	public String getBIDate(){
		return date;
	}

	public String getPath() {
		return path;
	}
	
	public int getLineNum() {
		return lineNum;
	}
}