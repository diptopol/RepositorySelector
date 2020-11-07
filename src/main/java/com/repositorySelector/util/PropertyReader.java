package com.repositorySelector.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Diptopol
 * @since 11/7/2020 1:19 PM
 */
public class PropertyReader {

    private Properties properties;

    public PropertyReader() {
        InputStream inputStream;
        properties = new Properties();
        String fileName = "config.properties";

        try {
            inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
            properties.load(inputStream);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public Properties getProperties() {
        return this.properties;
    }
}
