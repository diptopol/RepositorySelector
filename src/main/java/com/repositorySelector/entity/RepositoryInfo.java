package com.repositorySelector.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.Objects;

/**
 * @author Diptopol
 * @since 11/6/2020 11:04 PM
 */
public class RepositoryInfo {

    private int id;
    private String fullName;

    private String contributorsUrl;
    private String tagsUrl;
    private String commitsUrl;

    private int commitCount;
    private int releaseCount;
    private int contributorCount;
    private int stargazersCount;
    private int forksCount;

    private String defaultBranch;

    private boolean fork;

    private String url;
    private String htmlUrl;

    @JsonProperty(value = "created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Date created;

    @JsonProperty(value = "pushed_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Date latestPushedCommitDate;

    private boolean commitCountSuccess;
    private boolean releaseCountSuccess;
    private boolean contributorCountSuccess;

    public RepositoryInfo() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getContributorsUrl() {
        return contributorsUrl;
    }

    public void setContributorsUrl(String contributorsUrl) {
        this.contributorsUrl = contributorsUrl;
    }

    public String getTagsUrl() {
        return tagsUrl;
    }

    public void setTagsUrl(String tagsUrl) {
        this.tagsUrl = tagsUrl;
    }

    public String getCommitsUrl() {
        return commitsUrl;
    }

    public void setCommitsUrl(String commitsUrl) {
        this.commitsUrl = commitsUrl;
    }

    public int getCommitCount() {
        return commitCount;
    }

    public int getReleaseCount() {
        return releaseCount;
    }

    public int getContributorCount() {
        return contributorCount;
    }

    public void setCommitCount(int count) {
        this.commitCount = count;
    }

    public void setReleaseCount(int count) {
        this.releaseCount = count;
    }

    public void setContributorCount(int count) {
        this.contributorCount = count;
    }

    public int getStargazersCount() {
        return stargazersCount;
    }

    public void setStargazersCount(int stargazersCount) {
        this.stargazersCount = stargazersCount;
    }

    public int getForksCount() {
        return forksCount;
    }

    public void setForksCount(int forksCount) {
        this.forksCount = forksCount;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public boolean isFork() {
        return fork;
    }

    public void setFork(boolean fork) {
        this.fork = fork;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLatestPushedCommitDate() {
        return latestPushedCommitDate;
    }

    public void setLatestPushedCommitDate(Date latestPushedCommitDate) {
        this.latestPushedCommitDate = latestPushedCommitDate;
    }

    public boolean isCommitCountSuccess() {
        return commitCountSuccess;
    }

    public void setCommitCountSuccess(boolean commitCountSuccess) {
        this.commitCountSuccess = commitCountSuccess;
    }

    public boolean isReleaseCountSuccess() {
        return releaseCountSuccess;
    }

    public void setReleaseCountSuccess(boolean releaseCountSuccess) {
        this.releaseCountSuccess = releaseCountSuccess;
    }

    public boolean isContributorCountSuccess() {
        return contributorCountSuccess;
    }

    public void setContributorCountSuccess(boolean contributorCountSuccess) {
        this.contributorCountSuccess = contributorCountSuccess;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepositoryInfo that = (RepositoryInfo) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
