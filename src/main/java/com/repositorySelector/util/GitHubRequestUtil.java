package com.repositorySelector.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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

    public static void initialProcessRepositoryRequest(Properties properties) {
        List<RepositoryInfo> repositoryInfoList = new ArrayList<>();

        LocalDate searchFrom = LocalDate.parse(properties.getProperty("searchStartsFrom"));

        while (searchFrom.isBefore(LocalDate.parse(properties.getProperty("searchEnds")))) {
            List<RepositoryInfo> repositoryInfoListByYear = new ArrayList<>();
            int totalCount = -1;
            int currentPage = 1;
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

                    repositoryInfoListByYear.addAll(repositoryResponse.getItems());
                    currentPage++;

                } catch (URISyntaxException | IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } while (repositoryInfoListByYear.size() < totalCount);

            repositoryInfoList.addAll(repositoryInfoListByYear);

            searchFrom = searchFrom.plusYears(1);
        }

        logger.info("Total Repository Count (Before distinct): {}", repositoryInfoList.size());

        repositoryInfoList = repositoryInfoList.stream()
                .distinct()
                .collect(Collectors.toList());

        logger.info("Total Repository Count (before Filter): {}", repositoryInfoList.size());

        sortAndserializeRepositoryList(repositoryInfoList);
    }

    public static void filterAndSerialize(Properties properties) {
        List<RepositoryInfo> repositoryInfoList = readRepositoryList();

        filterAndSerializeRepositoryList(repositoryInfoList, properties);
    }

    public static List<RepositoryInfo> readRepositoryList() {
        return readRepositoryList("repositoryList.json");
    }

    public static List<RepositoryInfo> readRepositoryList(String fileName) {
        List<RepositoryInfo> repositoryInfoList = new ArrayList<>();

        try {
            repositoryInfoList.addAll(getObjectMapper().readValue(new File("output/" + fileName), new TypeReference<List<RepositoryInfo>>() {
            }));
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return repositoryInfoList;
    }

    public static void populateDefaultBranch(Properties properties) {
        String fileName = "filteredRepositoryList.json";

        List<RepositoryInfo> repositoryInfoList = readRepositoryList(fileName);

        Consumer<RepositoryInfo> defaultBranchConsumer = repositoryInfo -> {
            if (Objects.isNull(repositoryInfo.getDefaultBranch()) || repositoryInfo.getDefaultBranch().equals("")) {
                String repositoryJson = processGithubRequest(URI.create(repositoryInfo.getUrl()), properties);

                try {
                     ObjectNode objectNode = getObjectMapper().readValue(repositoryJson, ObjectNode.class);

                    if (objectNode.has("default_branch")) {
                        repositoryInfo.setDefaultBranch(objectNode.get("default_branch").textValue());
                    } else {
                        logger.info(repositoryJson);
                    }

                } catch (JsonProcessingException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        };

        processItemFetchForRepository(repositoryInfoList, defaultBranchConsumer);
        sortAndSerializeRepositoryList(repositoryInfoList, fileName);
    }

    public static void filterOutRepositoryWithLastYearLessMinCommits(Properties properties) {
        String fileName = "filteredRepositoryList.json";
        List<RepositoryInfo> repositoryInfoList = readRepositoryList(fileName);

        repositoryInfoList = repositoryInfoList.stream()
                .filter(repositoryInfo ->
                        repositoryInfo.getCommitCountInLastOneYear() >= Integer.valueOf(properties.getProperty("minimumCommits")))
                .collect(Collectors.toList());

        sortAndSerializeRepositoryList(repositoryInfoList, fileName);
    }

    public static void addingCommitCountsLastYear(Properties properties) {
        String fileName = "filteredRepositoryList.json";
        List<RepositoryInfo> repositoryInfoList = readRepositoryList(fileName);

        Consumer<RepositoryInfo> commitCountLastYearConsumer = repositoryInfo -> {
            try {
                int count = getItemCount(getItemURI(repositoryInfo.getCommitsUrl(),
                        Collections.singletonMap("since", properties.getProperty("lastUpdated"))), properties);

                repositoryInfo.setCommitCountInLastOneYear(count);
            } catch (URISyntaxException ex) {
                logger.error(ex.getMessage(), ex);
            }
        };

        processItemFetchForRepository(repositoryInfoList, commitCountLastYearConsumer);
        sortAndSerializeRepositoryList(repositoryInfoList, fileName);
    }


    public static void outputCSV(String projectName, List<String> commitIds) {
        projectName = projectName.replaceAll("/", "_");
        File file = new File("output/" + projectName + ".csv");

        try {
            PrintWriter pw = new PrintWriter(file);

            pw.write("CommitId\n");
            for (String commitId : commitIds) {
                pw.print(commitId+"\n");
            }

            pw.flush();
            pw.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private static void sortAndserializeRepositoryList(List<RepositoryInfo> repositoryInfoList) {
        sortAndSerializeRepositoryList(repositoryInfoList, "repositoryList.json");
    }

    private static void sortAndSerializeRepositoryList(List<RepositoryInfo> repositoryInfoList, String fileName) {
        repositoryInfoList.sort(Comparator.comparing(RepositoryInfo::getStargazersCount));

        try {
            getObjectMapper().writeValue(new File("output/" + fileName), repositoryInfoList);

        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private static void filterAndSerializeRepositoryList(List<RepositoryInfo> repositoryInfoList, Properties properties) {
        Consumer<RepositoryInfo> commitCountConsumer = repositoryInfo -> {
            if (!repositoryInfo.isCommitCountSuccess()) {
                int count = getItemCount(repositoryInfo.getCommitsUrl(), properties);
                repositoryInfo.setCommitCount(count);
                repositoryInfo.setCommitCountSuccess(true);
            }
        };

        processItemFetchForRepository(repositoryInfoList, commitCountConsumer);
        sortAndserializeRepositoryList(repositoryInfoList);

        int numberOfRepositoryWithCommitCount = (int) repositoryInfoList.stream()
                .filter(repositoryInfo -> repositoryInfo.getCommitCount() > 0 && repositoryInfo.isCommitCountSuccess())
                .count();

        logger.info("Commit Count fetch Completed: {}", numberOfRepositoryWithCommitCount);

        Consumer<RepositoryInfo> contributorCountConsumer = repositoryInfo -> {
            if (!repositoryInfo.isContributorCountSuccess()) {
                int count = getItemCount(repositoryInfo.getContributorsUrl(), properties);
                repositoryInfo.setContributorCount(count);
                repositoryInfo.setContributorCountSuccess(true);
            }
        };

        processItemFetchForRepository(repositoryInfoList, contributorCountConsumer);
        sortAndserializeRepositoryList(repositoryInfoList);

        int numberOfRepositoryWithContributorsCount = (int) repositoryInfoList.stream()
                .filter(repositoryInfo -> repositoryInfo.getContributorCount() > 0 && repositoryInfo.isContributorCountSuccess())
                .count();

        logger.info("Contributor Count fetch Completed: {}", numberOfRepositoryWithContributorsCount);

        Consumer<RepositoryInfo> repositoryInfoConsumer = repositoryInfo -> {
            if (!repositoryInfo.isReleaseCountSuccess()) {
                int count = getItemCount(repositoryInfo.getTagsUrl(), properties);
                repositoryInfo.setReleaseCount(count);
                repositoryInfo.setReleaseCountSuccess(true);
            }
        };

        processItemFetchForRepository(repositoryInfoList, repositoryInfoConsumer);
        sortAndserializeRepositoryList(repositoryInfoList);

        int numberOfRepositoryWithReleaseCount = (int) repositoryInfoList.stream()
                .filter(repositoryInfo -> repositoryInfo.getReleaseCount() > 0 && repositoryInfo.isReleaseCountSuccess())
                .count();

        logger.info("Release Count fetch Completed: {}", numberOfRepositoryWithReleaseCount);

        logger.info("Total Repository List Size (Before all filter): {}", repositoryInfoList.size());

        Predicate<RepositoryInfo> contributorPredicate =
                repositoryInfo -> repositoryInfo.getContributorCount() >= Integer.valueOf(properties.getProperty("minimumContributor"));

        repositoryInfoList = getFilteredRepositoryList(repositoryInfoList, contributorPredicate);


        Predicate<RepositoryInfo> commitCountPredicate =
                repositoryInfo -> repositoryInfo.getCommitCount() >= Integer.valueOf(properties.getProperty("minimumCommits"));

        repositoryInfoList = getFilteredRepositoryList(repositoryInfoList, commitCountPredicate);

        Predicate<RepositoryInfo> releaseCountPredicate =
                repositoryInfo -> repositoryInfo.getReleaseCount() >= Integer.valueOf(properties.getProperty("minimumRelease"));

        repositoryInfoList = getFilteredRepositoryList(repositoryInfoList, releaseCountPredicate);

        logger.info("Total Repository List Size (After all filter): {}", repositoryInfoList.size());

        sortAndSerializeRepositoryList(repositoryInfoList, "filteredRepositoryList.json");
    }

    private static void processItemFetchForRepository(List<RepositoryInfo> repositoryInfoList,
                                                      Consumer<RepositoryInfo> repositoryInfoConsumer) {

        Date start = new Date();
        repositoryInfoList.forEach(repositoryInfoConsumer);
        Date end = new Date();
        logger.info("Elapsed Time (In Seconds): {}",
                TimeUnit.MILLISECONDS.toSeconds(end.getTime() - start.getTime()));
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

    private static int getItemCount(URI requestUri, Properties properties) {
        int itemCount = 0;
        try {
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
                String json = EntityUtils.toString(response.getEntity());
                JsonNode node = getObjectMapper().readValue(json, JsonNode.class);

                if (node.isObject() && node.has("message") && node.get("message").isTextual()) {
                    logger.info(json);

                } else if (node.isArray()) {
                    itemCount = node.size();
                }
            }

        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return itemCount;
    }

    private static int getItemCount(String itemUriStr, Properties properties) {
        int itemCount = 0;

        try {
            URI requestUri = getItemURI(itemUriStr, null);
            itemCount = getItemCount(requestUri, properties);

        } catch (URISyntaxException ex) {
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

    private static URI getItemURI(String itemUriStr, Map<String, String> additionalParams) throws URISyntaxException {
        itemUriStr = itemUriStr.replace("{/sha}", "");

        URIBuilder uriBuilder = new URIBuilder(itemUriStr);

        if (Objects.nonNull(additionalParams) && !additionalParams.isEmpty()) {
            for (Map.Entry<String, String> entry : additionalParams.entrySet()) {
                uriBuilder.setParameter(entry.getKey(), entry.getValue());
            }
        }

        uriBuilder.setParameter("per_page", String.valueOf(1));
        uriBuilder.setParameter("page", String.valueOf(1));

        return uriBuilder.build();
    }

    private static String getSearchParams(LocalDate searchFrom, LocalDate searchTo, Properties properties) {
        String language = properties.getProperty("language");
        int minStars = Integer.valueOf(properties.getProperty("minimumStars"));
        String createdFrom = searchFrom.toString();
        String createdTo = searchTo.toString();
        String lastUpdated = properties.getProperty("lastUpdated");

        return " language:" + language
                + " stars:>=" + minStars
                + " created:" + createdFrom + ".." + createdTo
                + " pushed:>=" + lastUpdated
                + " fork:false"
                ;
    }

}
