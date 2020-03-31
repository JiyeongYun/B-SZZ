package hgu.csee.isel.alinew.szz.bszz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections4.IterableUtils;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import hgu.csee.isel.alinew.szz.bszz.bic.BICCollector;
import hgu.csee.isel.alinew.szz.bszz.data.CSVInfo;
import hgu.csee.isel.alinew.szz.bszz.data.BICInfo;
import hgu.csee.isel.alinew.szz.bszz.data.DeletedLineInCommits;
import hgu.csee.isel.alinew.szz.bszz.utils.Utils;

public class BSZZRunner implements BICCollector {
	private boolean unTrackDeletedBIlines = false;
	private String gitURI;

	/**
	 * TODO it must be changed into Iterable<String> BFCCommitList, but now we gonna
	 * deal with just only one commit.
	 */
	private ArrayList<String> BFCommitList = new ArrayList<String>();
	private ArrayList<HashMap<String, ArrayList<BICInfo>>> BFCBICList = new ArrayList<HashMap<String, ArrayList<BICInfo>>>();
	private List<CSVInfo> BICommitList = new ArrayList<>();
	private Repository repo;

	public static void main(String[] args) {
		BSZZRunner szz = new BSZZRunner();
//		szz.collectFrom();
	}

	public BSZZRunner() {
		this.gitURI = "/Users/yoon/git/DataForSZZ";
		this.BFCommitList.add("768b0df07b2722db926e99a8f917deeb5b55d628");
	}

	public BSZZRunner(String gitURI, Iterable<String> BFCommitList) {
		this.gitURI = gitURI;
		this.BFCommitList.addAll(IterableUtils.toList(BFCommitList));
	}
	
	public BSZZRunner(Iterable<String> BFCommitList) {
		this.BFCommitList.addAll(IterableUtils.toList(BFCommitList));
	}

	/**
	 * TODO Let's just print out BIC information and then change return type as
	 * ArrayList<RevCommit> (i.e. BIC list)
	 */
	public List<CSVInfo> collectFrom(List<RevCommit> commitList) {

		try {

			// get deleted lines from commits. This data are used to identify BI lines that
			// are only deleted and are in INSERT hunks in bug-fixing commits.
			HashMap<String, ArrayList<DeletedLineInCommits>> mapDeletedLines = null;
			if (!unTrackDeletedBIlines)
				mapDeletedLines = getDeletedLinesInCommits(commitList);

			for (String bfc : BFCommitList) {

				// find bug-fixing commits and get BI lines
				ArrayList<BICInfo> lstBIChanges = new ArrayList<BICInfo>();
				DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);

				for (RevCommit rev : commitList) {
					if (rev.getName().equals(bfc)) { // when we found BFC on commits
						RevCommit parent = rev.getParent(0); // Get BFC pre-commit (i.e. BFC~1 commit)
						if (parent == null) {
							System.err.println("WARNING: Parent commit does not exist: " + rev.name());
							break;
						}

						df.setRepository(repo);
						df.setDiffAlgorithm(Utils.diffAlgorithm);
						df.setDiffComparator(Utils.diffComparator);
						df.setDetectRenames(true);
						df.setPathFilter(PathSuffixFilter.create(".java"));

						// do diff
						List<DiffEntry> diffs = df.scan(parent.getTree(), rev.getTree());

						// check the change size in a patch
						int numLinesChanges = 0; // deleted + added
						String id = rev.name() + "";

						for (DiffEntry diff : diffs) {
							String oldPath = diff.getOldPath();
							String newPath = diff.getNewPath();

							// ignore when no previous revision of a file, Test files, and non-java files.
							if (oldPath.equals("/dev/null") || newPath.indexOf("Test") >= 0
									|| !newPath.endsWith(".java"))
								continue;

							// get preFixSource and fixSource without comments
							String prevFileSource = Utils.removeComments(Utils.fetchBlob(repo, id + "~1", oldPath));
							String fileSource = Utils.removeComments(Utils.fetchBlob(repo, id, newPath));

							// TEST
//							System.out.println("");
//							System.out.println("Old path : " + oldPath);
//							System.out.println(prevFileSource);
//							System.out.println("New path : " + newPath);
//							System.out.println(fileSource);

							// get line indices that are related to BI lines.
							EditList editList = Utils.getEditListFromDiff(prevFileSource, fileSource);
							for (Edit edit : editList) {

								int beginA = edit.getBeginA();
								int endA = edit.getEndA();
								int beginB = edit.getBeginB();
								int endB = edit.getEndB();

								// TEST
//								System.out.println("Type : " + edit.getType());
//								System.out.println("beginA : " + beginA);
//								System.out.println("endA : " + endA);
//								System.out.println("beginB : " + beginB);
//								System.out.println("endB : " + endB);
//								System.out.println("");

								numLinesChanges += (endA - beginA) + (endB - beginB);
							}
							// TEST
//							System.out.println("numLinesChanges : " + numLinesChanges);
						}

						// actual loop to get BI Changes
						for (DiffEntry diff : diffs) {
							ArrayList<Integer> lstIdxOfDeletedLinesInPrevFixFile = new ArrayList<Integer>();
							ArrayList<Integer> lstIdxOfOnlyInsertedLinesInFixFile = new ArrayList<Integer>();
							String oldPath = diff.getOldPath();
							String newPath = diff.getNewPath();

							// ignore when no previous revision of a file, Test files, and non-java files.
							if (oldPath.equals("/dev/null") || newPath.indexOf("Test") >= 0
									|| !newPath.endsWith(".java"))
								continue;

							// get preFixSource and fixSource without comments
							String prevFileSource = Utils.removeComments(Utils.fetchBlob(repo, id + "~1", oldPath));
							String fileSource = Utils.removeComments(Utils.fetchBlob(repo, id, newPath));

							EditList editList = Utils.getEditListFromDiff(prevFileSource, fileSource);

							// get line indices that are related to BI lines.
							for (Edit edit : editList) {

								if (edit.getType() != Edit.Type.INSERT) {

									int beginA = edit.getBeginA();
									int endA = edit.getEndA();

									for (int i = beginA; i < endA; i++)
										lstIdxOfDeletedLinesInPrevFixFile.add(i);

								} else {
									int beginB = edit.getBeginB();
									int endB = edit.getEndB();

									for (int i = beginB; i < endB; i++)
										lstIdxOfOnlyInsertedLinesInFixFile.add(i);
								}
							}

							// get BI commit from lines in lstIdxOfOnlyInsteredLines
							lstBIChanges.addAll(getBIChangesFromBILineIndices(id, rev, newPath, oldPath,
									prevFileSource, lstIdxOfDeletedLinesInPrevFixFile));
							if (!unTrackDeletedBIlines)
								lstBIChanges
										.addAll(getBIChangesFromDeletedBILine(id, rev, mapDeletedLines,
												fileSource, lstIdxOfOnlyInsertedLinesInFixFile, oldPath, newPath));

						}

					}
				}

				df.close();
				Collections.sort(lstBIChanges); // commits is ordered in the order BIDate, path, FixDate, lineNum.

				/**
				 * TODO : 해당 RevCommit type인 rev를 key로 넣고, lstBIChanges를 value로 넣는다.
				 */
				BICommitList.addAll(lstBIChanges);

				// print out results
//					System.out.println(String.format("%40s\t%10s\t%10s\t%40s\t%20s\t%20s\t%15s\t%17s\t%15s\t%50s",
//							"BISha1", "oldPath", "Path", "FixSha1", "BIDate", "FixDate", "LineNumInBI",
//							"LineNumInPreFix", "isAddedLine", "Line"));

//				for (CSVInfo csvInfo : BICommitList) {
//					if (csvInfo instanceof BICInfo) {
//						BICInfo biChange = (BICInfo) csvInfo;
//							System.out.println(String.format("%40s\t%10s\t%10s\t%40s\t%20s\t%20s\t%15s\t%17s\t%15s\t%50s",
//									biChange.getBISha1(), biChange.getBIPath(), biChange.getPath(), biChange.getFixSha1(),
//									biChange.getBIDate(), biChange.getFixDate(), biChange.getLineNum(),
//									biChange.getLineNumInPrevFixRev(), biChange.getIsAddedLine(), biChange.getLine()));
//					}
//				}

			}
		}

		catch (IOException e) {
			e.printStackTrace();
		}

		return BICommitList;
	}

	/**
	 * Get all deleted lines in commits. The return object, HashMap, is used to
	 * identify a BI commit that induce bug-fixing by a deleted line in the BI
	 * commit
	 * 
	 * @param commitList
	 * @return HashMap<String, ArrayList<DeletedLineInCommits>>
	 */
	private HashMap<String, ArrayList<DeletedLineInCommits>> getDeletedLinesInCommits(List<RevCommit> commitList) {

		// deletedLines are order by commit date (DESC, i.e., recent commit first)
		HashMap<String, ArrayList<DeletedLineInCommits>> deletedLines = new HashMap<String, ArrayList<DeletedLineInCommits>>();

		DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
		df.setRepository(repo);
		df.setDiffAlgorithm(Utils.diffAlgorithm);
		df.setDiffComparator(Utils.diffComparator);
		df.setDetectRenames(true);
		df.setPathFilter(PathSuffixFilter.create(".java"));

		// Traverse all commits to collect deleted lines.
		for (RevCommit rev : commitList) {

			// Get basic commit info
			String sha1 = rev.name() + "";
			String date = Utils.getStringDateTimeFromCommit(rev); // GMT time string

			if (rev.getParentCount() < 1) // skip if there are no parents (i.e. no commits to trace)
				continue;
			// Get diffs from affected files in the commit
			RevCommit preRev = rev.getParent(0);
			List<DiffEntry> diffs;
			try {
				// Deal with diff and get only deleted lines
				diffs = df.scan(preRev.getTree(), rev.getTree());

				for (DiffEntry diff : diffs) {

					String oldPath = diff.getOldPath();
					String newPath = diff.getNewPath();

					// Skip test case files
					if (newPath.indexOf("Test") >= 0 || !newPath.endsWith(".java"))
						continue;

					// Do diff on files without comments to only consider code lines
					String prevfileSource = Utils.removeComments(Utils.fetchBlob(repo, sha1 + "~1", oldPath));
					String fileSource = Utils.removeComments(Utils.fetchBlob(repo, sha1, newPath));

					EditList editList = Utils.getEditListFromDiff(prevfileSource, fileSource);

					String[] arrPrevfileSource = prevfileSource.split("\n");
					for (Edit edit : editList) {
						// Deleted lines are in DELETE and REPLACE types. So, ignore INSERT type.
						if (!edit.getType().equals(Edit.Type.INSERT)) {

							int beginA = edit.getBeginA();
							int endA = edit.getEndA();

							// Line num is not that important for deleted lines in BI commits
							for (int lineIdx = beginA; lineIdx < endA; lineIdx++) {
								if (arrPrevfileSource.length <= lineIdx)
									continue; // split("\n") ignore last empty lines. So, lineIdx can be greater the
												// array length. Ignore this case
								String line = arrPrevfileSource[lineIdx].trim();
								if (line.length() < 2)
									continue; // heuristic: ignore "}" or "{". only consider the line whose length >= 2
								DeletedLineInCommits deletedLine = new DeletedLineInCommits(sha1, date, oldPath,
										newPath, lineIdx + 1, line);
								if (!deletedLines.containsKey(line)) {
									ArrayList<DeletedLineInCommits> lstDeletedLines = new ArrayList<DeletedLineInCommits>();
									lstDeletedLines.add(deletedLine);
									deletedLines.put(line, lstDeletedLines);
								} else {
									deletedLines.get(line).add(deletedLine);
								}
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		df.close();

		return deletedLines;
	}

	private ArrayList<BICInfo> getBIChangesFromBILineIndices(String fixSha1, RevCommit fixCommit, String path,
			String prevPath, String prevFileSource, ArrayList<Integer> lstIdxOfDeletedLinesInPrevFixFile) {

		ArrayList<BICInfo> biChanges = new ArrayList<BICInfo>();

		// do Blame
		BlameCommand blamer = new BlameCommand(repo);
		ObjectId commitID;
		try {
			commitID = repo.resolve(fixSha1 + "~1");
			blamer.setStartCommit(commitID);
			blamer.setFilePath(prevPath);
			BlameResult blame = blamer.setDiffAlgorithm(Utils.diffAlgorithm).setTextComparator(Utils.diffComparator)
					.setFollowFileRenames(true).call();

			ArrayList<Integer> arrIndicesInOriginalFileSource = lstIdxOfDeletedLinesInPrevFixFile; // getOriginalLineIndices(origPrvFileSource,prevFileSource,lstIdxOfDeletedLines);
			for (int lineIndex : arrIndicesInOriginalFileSource) {
				RevCommit commit = blame.getSourceCommit(lineIndex);

				String BISha1 = commit.name();
				String biPath = blame.getSourcePath(lineIndex);
				String FixSha1 = fixSha1;
				String BIDate = Utils.getStringDateTimeFromCommit(commit);
				String FixDate = Utils.getStringDateTimeFromCommit(fixCommit);
				int lineNum = blame.getSourceLine(lineIndex) + 1;
				int lineNumInPrevFixRev = lineIndex + 1;

				String[] splitLinesSrc = prevFileSource.split("\n");

				// split("\n") ignore last empty lines so lineIndex can be out-of-bound and
				// ignore empty line (this happens as comments are removed)
				if (splitLinesSrc.length <= lineIndex || splitLinesSrc[lineIndex].trim().equals(""))
					continue;

				BICInfo biChange = new BICInfo(BISha1, biPath, FixSha1, path, BIDate, FixDate, lineNum,
						lineNumInPrevFixRev, prevFileSource.split("\n")[lineIndex].trim(), true);
				biChanges.add(biChange);
			}

		} catch (RevisionSyntaxException | IOException | GitAPIException e) {
			e.printStackTrace();
		}

		return biChanges;
	}

	private ArrayList<BICInfo> getBIChangesFromDeletedBILine(String fixSha1, RevCommit fixCommit,
			HashMap<String, ArrayList<DeletedLineInCommits>> mapDeletedLines, String fileSource,
			ArrayList<Integer> lstIdxOfOnlyInsteredLinesInFixFile, String oldPath, String path) {

		ArrayList<BICInfo> biChanges = new ArrayList<BICInfo>();

		ArrayList<Integer> arrIndicesInOriginalFileSource = lstIdxOfOnlyInsteredLinesInFixFile;// getOriginalLineIndices(origFileSource,fileSource,lstIdxOfOnlyInsteredLines);

		String[] arrOrigFileSource = fileSource.split("\n");
		for (int lineIdx : arrIndicesInOriginalFileSource) {
			if (arrOrigFileSource.length <= lineIdx)
				continue; // split("\n") ignore last empty lines. So, lineIdx can be greater the array
							// length. Ignore this case
			String addedlineInFixCommit = arrOrigFileSource[lineIdx].trim();
			ArrayList<DeletedLineInCommits> lstDeletedLines = mapDeletedLines.get(addedlineInFixCommit);

			if (lstDeletedLines == null)
				continue;

			DeletedLineInCommits deletedLineToConsider = null;
			for (DeletedLineInCommits deletedLine : lstDeletedLines) {
				if (deletedLine.getBIDate().compareTo(Utils.getStringDateTimeFromCommit(fixCommit)) < 0
						&& deletedLine.getPath().equals(oldPath)) {
					deletedLineToConsider = deletedLine;
				}
			}
			if (deletedLineToConsider == null)
				continue; // no deleted lines exist in lstDeletedLines. Do process next added line
			else {

				// heuristic: line num difference between BI and Fix commits is <= 10, the
				// consider the line is BILine.
				int lineGap = Math.abs(deletedLineToConsider.getLineNum() - (lineIdx + 1));
				if (lineGap > 10)
					continue;

				// get BIChange from the deleted line
				String BISha1 = deletedLineToConsider.getSha1();
				String biPath = deletedLineToConsider.getPath();
				// String path;
				String FixSha1 = fixSha1;
				String BIDate = deletedLineToConsider.getBIDate();
				String FixDate = Utils.getStringDateTimeFromCommit(fixCommit);
				int lineNumInPrevFixRev = lineIdx + 1; // this info is not important in case of a deleted line.

				BICInfo biChange = new BICInfo(BISha1, biPath, FixSha1, path, BIDate, FixDate, lineIdx + 1,
						lineNumInPrevFixRev, addedlineInFixCommit, false);
				biChanges.add(biChange);
			}
		}

		return biChanges;
	}

	public static List<RevCommit> getCommitListFrom(File gitDir) throws IOException, GitAPIException {
		Git git = Git.open(gitDir);
		Iterable<RevCommit> walk = git.log().call();
		List<RevCommit> commitList = IterableUtils.toList(walk);

		return commitList;
	}

	public BSZZRunner(String gitURI) {
		this.gitURI = gitURI;
	}

	@Override
	public void setBFC(List<String> bfcList) {
		ArrayList<String> list = new ArrayList<>(bfcList);
		this.BFCommitList = list;

	}

}
