package com.repositorySelector.init;

import com.repositorySelector.util.GitHubRequestUtil;

/**
 * @author Diptopol
 * @since 11/6/2020 8:32 PM
 */
public class App {

    public static void main(String[] args) {
        System.out.println("init");

        GitHubRequestUtil.processRepositoryRequest();
    }


}
