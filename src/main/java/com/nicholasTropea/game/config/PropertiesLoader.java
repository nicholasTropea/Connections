package com.nicholasTropea.game.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads properties files from classpath resources.
 */
public final class PropertiesLoader {
    private PropertiesLoader() {
        // Utility class
    }


    /**
     * Loads a properties resource from classpath.
     *
     * @param resourcePath classpath resource path
     * @return loaded properties
     */
    public static Properties loadFromClasspath(String resourcePath) {
        try (
                InputStream stream = PropertiesLoader.class
                                                     .getClassLoader()
                                                     .getResourceAsStream(resourcePath)
        ) {
            if (stream == null) {
                throw new IllegalStateException(
                    "Missing configuration resource: " + resourcePath
                );
            }

            Properties properties = new Properties();
            properties.load(stream);
            return properties;
        }
        catch (IOException ex) {
            throw new IllegalStateException(
                "Failed to load configuration resource: " + resourcePath,
                ex
            );
        }
    }
}
