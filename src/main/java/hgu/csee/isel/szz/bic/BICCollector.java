package hgu.csee.isel.alinew.szz.bszz.bic;

import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

import hgu.csee.isel.alinew.szz.bszz.data.CSVInfo;;

public interface BICCollector {

	void setBFC(List<String> bfcList);

	List<CSVInfo> collectFrom(List<RevCommit> commitList);

}
