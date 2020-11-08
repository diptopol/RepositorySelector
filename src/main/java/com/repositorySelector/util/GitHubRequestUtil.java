package com.repositorySelector.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.repositorySelector.entity.RepositoryInfo;
import com.repositorySelector.entity.RepositoryResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Diptopol
 * @since 11/6/2020 11:48 PM
 */
public class GitHubRequestUtil {

    private static Logger logger = LoggerFactory.getLogger(GitHubRequestUtil.class);

    private static final int MAX_ITEM_LIMIT = 100;

    public static void processRepositoryRequest(Properties properties) {
        List<RepositoryInfo> repositoryInfoList = new ArrayList<>();

        LocalDate searchFrom = LocalDate.parse(properties.getProperty("searchStartsFrom"));

        while (searchFrom.isBefore(LocalDate.parse(properties.getProperty("searchEnds")))) {
            int totalCount = -1;
            int currentPage = 0;
            do {
                try {
                    String repositoryResponseJson = processGithubRequest(getGithubRepositoryURI(currentPage,
                            getSearchParams(searchFrom, searchFrom.plusYears(1), properties)), properties);

                    RepositoryResponse repositoryResponse = getObjectMapper().readValue(repositoryResponseJson,
                            RepositoryResponse.class);

                    //get totalCount for the first time request
                    if (totalCount == -1) {
                        totalCount = repositoryResponse.getTotalCount();

                        logger.info("Total Repository Count (By year): {}; searchFrom: {}; searchTo: {}",
                                totalCount, searchFrom, searchFrom.plusYears(1));
                    }

                    repositoryInfoList.addAll(repositoryResponse.getItems());
                    currentPage++;

                } catch (URISyntaxException | IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } while (repositoryInfoList.size() < totalCount);

            searchFrom = searchFrom.plusYears(1);
        }

        logger.info("Total Repository Count (Before distinct): {}", repositoryInfoList.size());

        repositoryInfoList = repositoryInfoList.stream()
                .distinct()
                .collect(Collectors.toList());

        logger.info("Total Repository Count (before Filter): {}", repositoryInfoList.size());

        Predicate<RepositoryInfo> commitCountPredicate = repositoryInfo -> {
            int count = getItemCount(repositoryInfo.getCommitsUrl(), properties);
            repositoryInfo.setCommitCount(count);

            return count > Integer.valueOf(properties.getProperty("minimumCommits"));
        };

        repositoryInfoList = getFilteredRepositoryList(repositoryInfoList, commitCountPredicate);

        Predicate<RepositoryInfo> contributorPredicate = repositoryInfo -> {
            int count = getItemCount(repositoryInfo.getContributorsUrl(), properties);
            repositoryInfo.setContributorCount(count);

            return count > Integer.valueOf(properties.getProperty("minimumContributor"));
        };

        repositoryInfoList = getFilteredRepositoryList(repositoryInfoList, contributorPredicate);

        Predicate<RepositoryInfo> releaseCountPredicate = repositoryInfo -> {
            int count = getItemCount(repositoryInfo.getTagsUrl(), properties);
            repositoryInfo.setReleaseCount(count);

            return count > Integer.valueOf(properties.getProperty("minimumRelease"));
        };

        repositoryInfoList = getFilteredRepositoryList(repositoryInfoList, releaseCountPredicate);

        logger.info("Total Repository List Size (After all filter): {}", repositoryInfoList.size());

        serializeRepositoryList(repositoryInfoList);
    }

    public static void serializeRepositoryList(List<RepositoryInfo> repositoryInfoList) {
        try {
            getObjectMapper().writeValue(new File("output/repositoryList.json"), repositoryInfoList);

        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private static List<RepositoryInfo> getFilteredRepositoryList(List<RepositoryInfo> repositoryInfoList,
                                                                  Predicate<RepositoryInfo> repositoryInfoPredicate) {

        Date start = new Date();
        repositoryInfoList = repositoryInfoList.stream()
                .filter(repositoryInfoPredicate)
                .collect(Collectors.toList());

        Date end = new Date();
        logger.info("Elapsed Time (In Seconds): {}",
                TimeUnit.MILLISECONDS.toSeconds(end.getTime() - start.getTime()));

        return repositoryInfoList;
    }

    private static int getItemCount(String itemUriStr, Properties properties) {
        int itemCount = 0;

        itemUriStr = itemUriStr.replace("{/sha}", "");

        try {
            URI requestUri = getItemURI(itemUriStr);
            HttpGet request = new HttpGet(requestUri);
            request.addHeader(HttpHeaders.AUTHORIZATION, "token " + properties.getProperty("token"));

            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(request);

            if (Objects.nonNull(response.getFirstHeader("link"))) {
                String[] linkValues = response.getFirstHeader("link").getValue().split(",");
                String lastLinkValue = linkValues[1];

                Pattern pattern = Pattern.compile("(?<=&page=).*?(?=>.*rel=\"last\")");
                Matcher matcher = pattern.matcher(lastLinkValue);

                if (matcher.find()) {
                    itemCount = Integer.valueOf(matcher.group(0));
                }
            } else {
                logger.info(EntityUtils.toString(response.getEntity()));
            }

        } catch (IOException | URISyntaxException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return itemCount;
    }

    private static String processGithubRequest(URI requestUri, Properties properties) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String responseJson = "";

        try {
            HttpGet request = new HttpGet(requestUri);
            request.addHeader(HttpHeaders.AUTHORIZATION, "token " + properties.getProperty("token"));

            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();

            responseJson = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
    }

        return responseJson;
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        return objectMapper;
    }

    private static URI getGithubRepositoryURI(int pageNumber, String searchParam) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder("https://api.github.com/search/repositories");

        uriBuilder.setParameter("q", searchParam);
        uriBuilder.setParameter("sort", "created");
        uriBuilder.setParameter("order", "asc");
        uriBuilder.setParameter("per_page", String.valueOf(MAX_ITEM_LIMIT));
        uriBuilder.setParameter("page", String.valueOf(pageNumber));

        return uriBuilder.build();
    }

    private static URI getItemURI(String itemUriStr)throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(itemUriStr);
        uriBuilder.setParameter("per_page", String.valueOf(1));
        uriBuilder.setParameter("page", String.valueOf(0));

        return uriBuilder.build();
    }

    private static String getSearchParams(LocalDate searchFrom, LocalDate searchTo, Properties properties) {
        String language = properties.getProperty("language");
        int minStars = Integer.valueOf(properties.getProperty("minimumStars"));
        String createdFrom = searchFrom.toString();
        String createdTo = searchTo.toString();
        String lastUpdated = properties.getProperty("lastUpdated");
        int minForks = Integer.valueOf(properties.getProperty("minimumForks"));

        return " language:" + language
                + " stars:>" + minStars
                + " created:" + createdFrom + ".." + createdTo
                + " pushed:>=" + lastUpdated
                + " forks:>=" + minForks
                + " fork:false";
    }

}
