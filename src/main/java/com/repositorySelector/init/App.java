package com.repositorySelector.init;

import com.repositorySelector.util.GitHubRequestUtil;
import com.repositorySelector.util.PropertyReader;

import java.util.Properties;

/**
 * @author Diptopol
 * @since 11/6/2020 8:32 PM
 */
public class App {

    public static void main(String[] args) {
        System.out.println("init");

        PropertyReader propertyReader = new PropertyReader();
        GitHubRequestUtil.processRepositoryRequest(propertyReader.getProperties());
    }


}
