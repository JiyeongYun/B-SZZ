package hgu.csee.isel.alinew.szz.bszz.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

public class Utils {
	
	static public String fetchBlob(Repository repo, String revSpec, String path) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException{

		// Resolve the revision specification
		final ObjectId id = repo.resolve(revSpec);

		// Makes it simpler to release the allocated resources in one go
		ObjectReader reader = repo.newObjectReader();

		// Get the commit object for that revision
		RevWalk walk = new RevWalk(reader);
		RevCommit commit = walk.parseCommit(id);
		walk.close();

		// Get the revision's file tree
		RevTree tree = commit.getTree();
		// .. and narrow it down to the single file's path
		TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);

		if (treewalk != null) {
			// use the blob id to read the file's data
			byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
			reader.close();
			return new String(data, "utf-8");
		} else {
			return "";
		}

	}
	
	public static String removeComments(String code) {

		JavaASTParser codeAST = new JavaASTParser(code);
		@SuppressWarnings("unchecked")
		List<Comment> lstComments = codeAST.cUnit.getCommentList();

		for(Comment comment:lstComments){
			code = replaceComments(code,comment.getStartPosition(),comment.getLength());
		}

		return code;
	}

	private static String replaceComments(String code, int startPosition, int length) {

		String pre = code.substring(0,startPosition);
		String post = code.substring(startPosition+length,code.length());

		String comments = code.substring(startPosition, startPosition+length);

		comments = comments.replaceAll("\\S"," "); //all non-white space character is replaced into space(one of the white space characters)

		code = pre + comments + post;

		return code;
	}
	
	static public DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS);
	static public RawTextComparator diffComparator = RawTextComparator.WS_IGNORE_ALL;
	static public EditList getEditListFromDiff(String file1, String file2) {
		RawText rt1 = new RawText(file1.getBytes());
		RawText rt2 = new RawText(file2.getBytes());
		EditList diffList = new EditList();

		diffList.addAll(diffAlgorithm
				.diff(diffComparator, rt1, rt2));
		return diffList;
	}


	public static String getStringDateTimeFromCommit(RevCommit commit) {
		
		
		
		SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date commitDate = commit.getAuthorIdent().getWhen();

		TimeZone GMT = commit.getCommitterIdent().getTimeZone();
		ft.setTimeZone(GMT);

		return ft.format(commitDate);
	}


}
