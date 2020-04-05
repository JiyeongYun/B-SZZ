package hgu.csee.isel.szz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
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

import hgu.csee.isel.szz.data.BICInfo;
import hgu.csee.isel.szz.utils.Utils;

public class BSZZ {
	private String GIT_URL;
	private List<String> BFCList = new ArrayList<>();
	private File localPath;

	public BSZZ(String gIT_URL, List<String> bFCList) {
		GIT_URL = gIT_URL;
		BFCList = bFCList;
	}

	public void run() throws IOException {

		try {
			// Clone
			final String REMOTE_URI = GIT_URL + ".git";

			// prepare a new folder for the cloned repository
			localPath = File.createTempFile("TestGitRepository", "");
			if (!localPath.delete()) {
				throw new IOException("Could not delete temporary file " + localPath);
			}

			System.out.println("\nCloning from " + REMOTE_URI + " to " + localPath);

			Git git = Git.cloneRepository().setURI(REMOTE_URI).setDirectory(localPath).call();

			System.out.println("Having repository: " + git.getRepository().getDirectory());

			Repository repo = git.getRepository();

			List<RevCommit> revs = Utils.getRevs(git);

			List<RevCommit> bfcList = Utils.getBFCList(BFCList, revs);

			// Collect BICs
			final long startCollectingBICTime = System.currentTimeMillis();
			
			List<BICInfo> BICList = collectBIC(repo, bfcList);
			
			final long endCollectingBICTime = System.currentTimeMillis();
			System.out.println("\nCollecting BICs takes " + (endCollectingBICTime - startCollectingBICTime) / 1000.0 + "s\n");
			
			// Sort BICs in the order FixSha1, BISha1, BIContent, biLineIdx
			Collections.sort(BICList);

			// Store output
			Utils.storeOutputFile(GIT_URL, BICList);

		} catch (IOException | GitAPIException e) {
			e.printStackTrace();
		} finally {
			// clean up here to not keep using more and more disk-space for these samples
			FileUtils.deleteDirectory(localPath);
			System.out.println("Clean up " + localPath);
		}

	}

	private List<BICInfo> collectBIC(Repository repo, List<RevCommit> bfcList) throws IOException {

		ArrayList<BICInfo> lstBIChanges = new ArrayList<BICInfo>();

		// find bug-fixing commits and get BI lines
		DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);

		for (RevCommit rev : bfcList) {
			if (rev.getParentCount() > 0) {
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

				String BFCSha1 = rev.name() + "";

				// actual loop to get BI Changes
				for (DiffEntry diff : diffs) {
					ArrayList<Integer> lstIdxOfDeletedLinesInPrevFixFile = new ArrayList<Integer>();
					String oldPath = diff.getOldPath();
					String newPath = diff.getNewPath();

					// ignore when no previous revision of a file, Test files, and non-java files.
					if (oldPath.equals("/dev/null") || newPath.indexOf("Test") >= 0 || !newPath.endsWith(".java"))
						continue;

					// get preFixSource and fixSource without comments
					String prevFileSource = Utils.removeComments(Utils.fetchBlob(repo, BFCSha1 + "~1", oldPath));
					String fileSource = Utils.removeComments(Utils.fetchBlob(repo, BFCSha1, newPath));

					EditList editList = Utils.getEditListFromDiff(prevFileSource, fileSource);

					// get line indices that are related to BI lines.
					for (Edit edit : editList) {
						if (edit.getType() != Edit.Type.INSERT) {

							int beginA = edit.getBeginA();
							int endA = edit.getEndA();

							for (int i = beginA; i < endA; i++)
								lstIdxOfDeletedLinesInPrevFixFile.add(i);
						}
					}
					
					// get BI commit from lines in lstIdxOfOnlyInsteredLines
					lstBIChanges.addAll(getBIChangesFromBILineIndices(repo, BFCSha1, rev, newPath, oldPath, prevFileSource, lstIdxOfDeletedLinesInPrevFixFile));
					
				}
			}
		}

		df.close();

		// TEST
//		System.out.println(String.format("%40s\t%10s\t%10s\t%40s\t%20s\t%20s\t%15s\t%17s\t%50s", "BISha1", "oldPath",
//				"FixSha1", "Path", "BIDate", "FixDate", "LineIdxInBI", "LineIdxInPreFix", "BIContent"));
//
//		for (BICInfo csvInfo : lstBIChanges) {
//
//			BICInfo biChange = (BICInfo) csvInfo;
//			System.out.println(String.format("%40s\t%10s\t%10s\t%40s\t%20s\t%20s\t%15s\t%17s\t%50s",
//					biChange.getBISha1(), biChange.getBiPath(), biChange.getFixSha1(), biChange.getPath(),
//					biChange.getBIDate(), biChange.getFixDate(), biChange.getBiLineIdx(),
//					biChange.getLineIdxInPrevFixRev(), biChange.getBIContent()));
//
//		}

		return lstBIChanges;
	}

	private ArrayList<BICInfo> getBIChangesFromBILineIndices(Repository repo, String fixSha1, RevCommit fixCommit,
			String path, String prevPath, String prevFileSource, ArrayList<Integer> lstIdxOfDeletedLinesInPrevFixFile) {

		ArrayList<BICInfo> biChanges = new ArrayList<BICInfo>();

		// do Blame
		BlameCommand blamer = new BlameCommand(repo);
		ObjectId prevFixSha1;
		try {
			prevFixSha1 = repo.resolve(fixSha1 + "~1");
			blamer.setStartCommit(prevFixSha1);
			blamer.setFilePath(prevPath);
			BlameResult blame = blamer.setDiffAlgorithm(Utils.diffAlgorithm)
									  .setTextComparator(Utils.diffComparator)
									  .setFollowFileRenames(true)
									  .call();

			ArrayList<Integer> arrIndicesInOriginalFileSource = lstIdxOfDeletedLinesInPrevFixFile; // getOriginalLineIndices(origPrvFileSource,prevFileSource,lstIdxOfDeletedLines);
			for (int lineIndex : arrIndicesInOriginalFileSource) {
				RevCommit commit = blame.getSourceCommit(lineIndex);

				String BISha1 = commit.name();
				String biPath = blame.getSourcePath(lineIndex);
				String FixSha1 = fixSha1;
				String BIDate = Utils.getStringDateTimeFromCommit(commit);
				String FixDate = Utils.getStringDateTimeFromCommit(fixCommit);
				int BILineIdx = blame.getSourceLine(lineIndex);
				int lineIdxInPrevFixRev = lineIndex;
				String BIContent = prevFileSource.split("\n")[lineIndex].trim();
				String commiter = blame.getSourceCommitter(lineIndex).getName();
				String author = blame.getSourceAuthor(lineIndex).getName();

				String[] splitLinesSrc = prevFileSource.split("\n");

				// split("\n") ignore last empty lines so lineIndex can be out-of-bound and
				// ignore empty line (this happens as comments are removed)
				if (splitLinesSrc.length <= lineIndex || splitLinesSrc[lineIndex].trim().equals(""))
					continue;

				BICInfo biChange = new BICInfo(BISha1, biPath, FixSha1, path, BIDate, FixDate, BILineIdx, lineIdxInPrevFixRev, BIContent, commiter, author);
				biChanges.add(biChange);
			}

		} catch (RevisionSyntaxException | IOException | GitAPIException e) {
			e.printStackTrace();
		}

		return biChanges;
	}
}