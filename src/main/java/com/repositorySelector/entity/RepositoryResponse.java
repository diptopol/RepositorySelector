package com.repositorySelector.entity;

import java.util.List;

/**
 * @author Diptopol
 * @since 11/6/2020 11:03 PM
 */
public class RepositoryResponse {

    private int totalCount;

    private boolean incompleteResults;

    private List<RepositoryInfo> items;

    public RepositoryResponse() {
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public boolean isIncompleteResults() {
        return incompleteResults;
    }

    public void setIncompleteResults(boolean incompleteResults) {
        this.incompleteResults = incompleteResults;
    }

    public List<RepositoryInfo> getItems() {
        return items;
    }

    public void setItems(List<RepositoryInfo> items) {
        this.items = items;
    }
}
