package com.repositorySelector.init;

import com.repositorySelector.entity.RepositoryInfo;
import com.repositorySelector.refactoringDetection.RefactoringDetector;
import com.repositorySelector.util.GitHubRequestUtil;
import com.repositorySelector.util.PropertyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Diptopol
 * @since 11/6/2020 8:32 PM
 */
public class App {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("Init {}", LocalDateTime.now());

        PropertyReader propertyReader = new PropertyReader();

        for (Map.Entry<Object, Object> entry : propertyReader.getProperties().entrySet()) {
            if (!"token".equals(entry.getKey())) {
                logger.info("Config Key : {}, Config Value : {}", entry.getKey(), entry.getValue());
            }
        }


        /*List<RepositoryInfo> repositoryInfoList = GitHubRequestUtil.readRepositoryList();

        if (repositoryInfoList.isEmpty()) {
            logger.info("repository list is empty");
            GitHubRequestUtil.initialProcessRepositoryRequest(propertyReader.getProperties());
        }

        GitHubRequestUtil.filterAndSerialize(propertyReader.getProperties());*/


        List<RepositoryInfo> filteredRepositoryInfoList = GitHubRequestUtil.readRepositoryList("filteredRepositoryList.json");

        filteredRepositoryInfoList = filteredRepositoryInfoList.stream()
                .sorted(Comparator.comparing(RepositoryInfo::getReleaseCount))
                .collect(Collectors.toList());

        Collections.reverse(filteredRepositoryInfoList);

        String projectName = "apache/hbase";

        filteredRepositoryInfoList = filteredRepositoryInfoList.stream()
                .filter(repositoryInfo -> repositoryInfo.getFullName().equals(projectName))
                .collect(Collectors.toList());

        // 0<= i <1
        for (int i = 0; i < 1; i++) {
            RefactoringDetector.detectRefactoringData(Collections.singletonList(filteredRepositoryInfoList.get(i)));
        }



        // total found refactoring 139
        /*RefactoringDetector.detectRefactoringData(filteredRepositoryInfoList);
        for (RepositoryInfo repositoryInfo : filteredRepositoryInfoList) {
            RefactoringDetector.detectRefactoringData(Collections.singletonList(filteredRepositoryInfoList.get(20)));
        }*/


        /*
         * default branch name fetch was missing at initial. So written this method to only fetch default_branch name
         * afterwords to save request to github api.
         */
        //GitHubRequestUtil.populateDefaultBranch(propertyReader.getProperties());

        /*
         * Filtered out repository which has more than 100 commits since 2019-11-1
         */

        //GitHubRequestUtil.addingCommitCountsLastYear(propertyReader.getProperties());
        //GitHubRequestUtil.filterOutRepositoryWithLastYearLessMinCommits(propertyReader.getProperties());
    }


}
