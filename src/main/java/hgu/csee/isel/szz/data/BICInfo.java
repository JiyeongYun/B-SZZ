package hgu.csee.isel.szz.data;

public class BICInfo implements Comparable<BICInfo> {

	String BISha1;
	String biPath;
	String FixSha1;
	String path;
	String BIDate;
	String FixDate;
	int biLineIdx; // line idx in BI file
	int lineIdxInPrevFixRev;
	String BIContent = "";
	String commiter;
	String author;

	public BICInfo(String bISha1, String biPath, String fixSha1, String path, String bIDate, String fixDate,
			int biLineIdx, int lineIdxInPrevFixRev, String bIContent, String commiter, String author) {
		super();
		BISha1 = bISha1;
		this.biPath = biPath;
		FixSha1 = fixSha1;
		this.path = path;
		BIDate = bIDate;
		FixDate = fixDate;
		this.biLineIdx = biLineIdx;
		this.lineIdxInPrevFixRev = lineIdxInPrevFixRev;
		BIContent = bIContent;
		this.commiter = commiter;
		this.author = author;
	}

	public String getBISha1() {
		return BISha1;
	}

	public String getBiPath() {
		return biPath;
	}

	public String getFixSha1() {
		return FixSha1;
	}

	public String getPath() {
		return path;
	}

	public String getBIDate() {
		return BIDate;
	}

	public String getFixDate() {
		return FixDate;
	}

	public int getBiLineIdx() {
		return biLineIdx;
	}

	public int getLineIdxInPrevFixRev() {
		return lineIdxInPrevFixRev;
	}

	public String getBIContent() {
		return BIContent;
	}

	public String getCommiter() {
		return commiter;
	}

	public String getAuthor() {
		return author;
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
		if (biLineIdx != compareWith.biLineIdx)
			return false;
		if (lineIdxInPrevFixRev != compareWith.lineIdxInPrevFixRev)
			return false;
		if (!BIContent.equals(compareWith.BIContent))
			return false;

		return true;
	}

	@Override
	public int compareTo(BICInfo o) {

		// order by FixSha1, BISha1, BIContent, biLineIdx
		if (FixSha1.compareTo(o.FixSha1) < 0)
			return -1;
		else if (FixSha1.compareTo(o.FixSha1) > 0)
			return 1;
		else {
			if (BISha1.compareTo(o.BISha1) < 0)
				return -1;
			else if (BISha1.compareTo(o.BISha1) > 0)
				return 1;
			else {
				if (BIContent.compareTo(o.BIContent) < 0)
					return -1;
				else if (BIContent.compareTo(o.BIContent) > 0)
					return 1;
				else {
					if (biLineIdx < o.biLineIdx)
						return -1;
					else if (biLineIdx > o.biLineIdx)
						return 1;
				}
			}
		}

		// order by BIDate, path, FixDate, lineNum
//		if (BIDate.compareTo(o.BIDate) < 0)
//			return -1;
//		else if (BIDate.compareTo(o.BIDate) > 0)
//			return 1;
//		else {
//			if (path.compareTo(o.path) < 0)
//				return -1;
//			else if (path.compareTo(o.path) > 0)
//				return 1;
//			else {
//				if (FixDate.compareTo(o.FixDate) < 0)
//					return -1;
//				else if (FixDate.compareTo(o.FixDate) > 0)
//					return 1;
//				else {
//					if (biLineIdx < o.biLineIdx)
//						return -1;
//					else if (biLineIdx > o.biLineIdx)
//						return 1;
//				}
//			}
//		}

		return 0;
	}
}