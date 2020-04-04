package hgu.csee.isel.szz.data;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;


public class BICInfo implements Comparable<BICInfo>, CSVInfo {
	public final static String[] headers = { "BISha1", "oldPath", "Path", "FixSha1", "BIDate", "FixDate", "LineNumInBI",
			"LineNumInPreFix", "isAddedLine", "Line" };

	@Override
	public String[] getHeaders() {
		return headers;
	}

	String BISha1;
	String biPath;
	String path;
	String FixSha1;
	String BIDate;
	String FixDate;
	int lineNum; // line num in BI file
	int lineNumInPrevFixRev; // line num in previous commit of the fix commit
	boolean isAddedLine;
	String line = "";
	boolean isNoise;
	Edit edit;
	EditList editList;

	String filteredDueTo;

	public BICInfo() {
	}

	public BICInfo(String BISha1, String biPath, String FixSha1, String path, String BIDate, String FixDate,
			int lineNum, int lineNumInPrevFixRev, String line, boolean isAddedLine) {
		this.BISha1 = BISha1;
		this.biPath = biPath;
		this.FixSha1 = FixSha1;
		this.path = path;
		this.BIDate = BIDate;
		this.FixDate = FixDate;
		this.lineNum = lineNum;
		this.lineNumInPrevFixRev = lineNumInPrevFixRev;
		this.line = line;
		this.isAddedLine = isAddedLine;
	}

	public BICInfo(String changeInfo, boolean forSenitizer) {
		String[] splitString = changeInfo.split("\t");

		if (splitString.length == 2) {

			BISha1 = splitString[0];
			path = splitString[1];

		} else {

			BISha1 = splitString[0];
			biPath = splitString[1];
			path = splitString[2];
			FixSha1 = splitString[3];
			BIDate = splitString[4];
			FixDate = splitString[5];
			lineNum = Integer.parseInt(splitString[6]); // if applying Sanitizer, this will be line num in BI code.
			if (!forSenitizer) {
				lineNumInPrevFixRev = Integer.parseInt(splitString[7]); // lineNum in the prv. of fix revision.
				isAddedLine = splitString[8].equals("t") || splitString[8].toLowerCase().equals("true") ? true : false;

				// if raw line data contains tab, the line data is splitted. In this case,
				// replace tab with 5 white spaces
				for (int i = 9; i < splitString.length; i++)
					line += splitString[i] + "     ";
				line = line.trim();
			} else {
				lineNumInPrevFixRev = Integer.parseInt(splitString[6]); // lineNum in the prv. of fix revision.
				isAddedLine = splitString[7].equals("t") || splitString[7].toLowerCase().equals("true") ? true : false;
				line = splitString[8];
			}
		}

		filteredDueTo = "";
	}

	public void setIsNoise(boolean isNoise) {
		this.isNoise = isNoise;
	}

	public void setBIPath(String biPath) {
		this.biPath = biPath;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setBIDate(String date) {
		this.BIDate = date;
	}

	public void setFixDate(String date) {
		this.FixDate = date;
	}

	public void setFilteredDueTo(String filterName) {
		filteredDueTo = filterName;
	}

	public boolean isNoise() {
		return isNoise;
	}

	public String getBISha1() {
		return BISha1;
	}

	public String getBIPath() {
		return biPath;
	}

	public String getPath() {
		return path;
	}

	public String getFixSha1() {
		return FixSha1;
	}

	public String getBIDate() {
		return BIDate;
	}

	public String getFixDate() {
		return FixDate;
	}

	public int getLineNum() {
		return lineNum;
	}

	public String getLine() {
		return line;
	}

	public boolean getIsAddedLine() {
		return isAddedLine;
	}

	public String getFilteredDueTo() {
		return filteredDueTo;
	}

	public void setLineNum(Integer lineNum) {
		this.lineNum = lineNum;
	}

	public void setEdit(Edit edit) {
		this.edit = edit;
	}

	public void setEditList(EditList editListFromDiff) {
		this.editList = editListFromDiff;
	}

	public Edit getEdit() {
		return edit;
	}

	public EditList getEditListFromDiff() {
		return editList;
	}

	public void setBISha1(String biSha1) {
		BISha1 = biSha1;
	}

	public String toString() {
		return getBISha1() + "\t" + getBIPath() + "\t" + getPath() + "\t" + getFixSha1() + "\t" + getIsAddedLine()
				+ "\t" + getLineNum() + "\t" + getLine();
	}

	public String getRecord() {
		return getBISha1() + "\t" + getBIPath() + "\t" + getPath() + "\t" + getFixSha1() + "\t" + getBIDate() + "\t"
				+ getFixDate() + "\t" + getLineNum() + "\t" + getLineNumInPrevFixRev() + "\t" + getIsAddedLine() + "\t"
				+ getLine();
	}

	public String getRecordWithoutLineNumInPrevFix() {
		return getBISha1() + "\t" + getBIPath() + "\t" + getPath() + "\t" + getFixSha1() + "\t" + getBIDate() + "\t"
				+ getFixDate() + "\t" + getLineNum() + "\t" + getIsAddedLine() + "\t" + getLine();
	}

	public int getLineNumInPrevFixRev() {
		return lineNumInPrevFixRev;
	}

	public void setLineNumInPrevFixRev(int lineNum) {
		lineNumInPrevFixRev = lineNum;
	}

	public boolean equals(BICInfo compareWith) {
		if (!BISha1.equals(compareWith.BISha1))
			return false;
		if (!biPath.equals(compareWith.biPath))
			return false;
		if (!path.equals(compareWith.path))
			return false;
		if (!FixSha1.equals(compareWith.FixSha1))
			return false;
		if (!BIDate.equals(compareWith.BIDate))
			return false;
		if (!FixDate.equals(compareWith.FixDate))
			return false;
		if (lineNum != compareWith.lineNum)
			return false;
		if (lineNumInPrevFixRev != compareWith.lineNumInPrevFixRev)
			return false;
		if (isAddedLine != compareWith.isAddedLine)
			return false;
		if (!line.equals(compareWith.line))
			return false;
		;
		if (isNoise != compareWith.isNoise)
			return false;

		return true;
	}

	@Override
	public int compareTo(BICInfo o) {

		// order by BIDate, path, FixDate, lineNum
		if (BIDate.compareTo(o.BIDate) < 0)
			return -1;
		else if (BIDate.compareTo(o.BIDate) > 0)
			return 1;
		else {
			if (path.compareTo(o.path) < 0)
				return -1;
			else if (path.compareTo(o.path) > 0)
				return 1;
			else {
				if (FixDate.compareTo(o.FixDate) < 0)
					return -1;
				else if (FixDate.compareTo(o.FixDate) > 0)
					return 1;
				else {
					if (lineNum < o.lineNum)
						return -1;
					else if (lineNum > o.lineNum)
						return 1;
				}
			}
		}

		return 0;
	}
}