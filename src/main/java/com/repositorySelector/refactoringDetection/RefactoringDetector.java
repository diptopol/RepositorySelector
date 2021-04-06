package com.repositorySelector.refactoringDetection;

import com.repositorySelector.entity.RepositoryInfo;
import com.repositorySelector.util.GitHubRequestUtil;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Diptopol
 * @since 11/11/2020 10:51 AM
 */
public class RefactoringDetector {

    private static Logger logger = LoggerFactory.getLogger(RefactoringDetector.class);

    public static void detectRefactoringData(List<RepositoryInfo> repositoryInfoList) {
        GitService gitService = new GitServiceImpl();

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        //miner.detectAll();

        for (RepositoryInfo repositoryInfo : repositoryInfoList) {
            logger.info("");
            logger.info("======================================");
            logger.info("");
            logger.info("Full Name: {}", repositoryInfo.getFullName());
            String repositoryLocalDirectory = "projectDirectory/" + repositoryInfo.getFullName().replaceAll("/", "-");

            try {
                Repository repo = gitService.cloneIfNotExists(
                        repositoryLocalDirectory,
                        repositoryInfo.getHtmlUrl());


                List<String> commitIdList = new ArrayList<>();
                List<Pair<String, Refactoring>> refactoringPairList = new ArrayList<>();
                int batchSize = 50;

                RefactoringHandler refactoringHandler = new RefactoringHandler() {

                    public void handle(String commitId, List<Refactoring> refs) {
                        if (refs == null)
                            return;

                        for (Refactoring ref : refs) {
                            if (ref.getRefactoringType() == RefactoringType.EXTRACT_OPERATION) {
                                commitIdList.add(commitId);

                                int currentSize = commitIdList.size();

                                if (currentSize % batchSize == 0) {
                                    List<String> sublist = commitIdList.subList(currentSize - batchSize, commitIdList.size());
                                    int suffix = currentSize / batchSize;
                                    GitHubRequestUtil.outputCSV(repositoryInfo.getFullName() + "-" + suffix, sublist);
                                }

                                refactoringPairList.add(new MutablePair<>(commitId, ref));
                            }
                        }
                    }
                };

                RevWalk walk = gitService.createAllRevsWalk(repo, repositoryInfo.getDefaultBranch());

                for (RevCommit revCommit : walk) {
                    String commitId = revCommit.getId().getName();
                    miner.detectAtCommit(repo, commitId, refactoringHandler, 300);
                }

                logger.info("Print commit id list with size {}", commitIdList.size());

                int begin = commitIdList.size() - commitIdList.size() % batchSize;
                List<String> subList = commitIdList.subList(begin, commitIdList.size());

                GitHubRequestUtil.outputCSV(repositoryInfo.getFullName() + "-Remaining", subList);


                /*for (Pair<String, Refactoring> pair: refactoringPairList) {
                    logger.info(pair.getKey());

                    ExtractOperationRefactoring refactoring = (ExtractOperationRefactoring) pair.getValue();

                    logger.info("SourceCodeBefore: {}", refactoring.getSourceOperationCodeRangeBeforeExtraction());
                    logger.info("SourceCodeAfter: {}", refactoring.getSourceOperationAfterExtraction());
                    logger.info("Ref: {}", refactoring.toString());
                }*/
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
