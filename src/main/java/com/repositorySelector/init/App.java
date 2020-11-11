package com.repositorySelector.init;

import com.repositorySelector.entity.RepositoryInfo;
import com.repositorySelector.util.GitHubRequestUtil;
import com.repositorySelector.util.PropertyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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


        List<RepositoryInfo> repositoryInfoList = GitHubRequestUtil.readRepositoryList();

        if (repositoryInfoList.isEmpty()) {
            logger.info("repository list is empty");
            GitHubRequestUtil.initialProcessRepositoryRequest(propertyReader.getProperties());
        }

        GitHubRequestUtil.filterAndSerialize(propertyReader.getProperties());
    }


}
