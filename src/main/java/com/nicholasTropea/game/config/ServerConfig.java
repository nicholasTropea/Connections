package com.nicholasTropea.game.config;

import java.util.Properties;

/**
 * Server runtime configuration loaded from properties.
 */
public final class ServerConfig {
    private static final String RESOURCE = "config/server.properties";

    private final int tcpPort;
    private final long roundDurationMillis;
    private final long sessionAutosaveSeconds;


    private ServerConfig(
        int tcpPort,
        long roundDurationMillis,
        long sessionAutosaveSeconds
    ) {
        this.tcpPort = tcpPort;
        this.roundDurationMillis = roundDurationMillis;
        this.sessionAutosaveSeconds = sessionAutosaveSeconds;
    }


    /**
     * Loads server configuration from classpath default resource.
     *
     * @return loaded server config
     */
    public static ServerConfig loadDefault() {
        Properties properties = PropertiesLoader.loadFromClasspath(RESOURCE);
        int tcpPort = parseInt(properties, "serverPort", 1, 65535);
        long roundDurationMillis = parseLong(
            properties,
            "roundDurationMilliseconds",
            1,
            Long.MAX_VALUE
        );
        long sessionAutosaveSeconds = parseLong(
            properties,
            "sessionAutosaveSeconds",
            1,
            Long.MAX_VALUE
        );

        return new ServerConfig(
            tcpPort,
            roundDurationMillis,
            sessionAutosaveSeconds
        );
    }


    /** @return TCP listening port */
    public int getTcpPort() { return this.tcpPort; }

    /** @return global round duration in milliseconds */
    public long getRoundDurationMillis() { return this.roundDurationMillis; }

    /** @return session autosave period in seconds */
    public long getSessionAutosaveSeconds() {
        return this.sessionAutosaveSeconds;
    }


    private static int parseInt(
        Properties properties,
        String key,
        int min,
        int max
    ) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing required property: " + key);
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                throw new IllegalStateException("Property out of range: " + key);
            }
            return parsed;
        }
        catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid integer property: " + key, ex);
        }
    }


    private static long parseLong(
        Properties properties,
        String key,
        long min,
        long max
    ) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing required property: " + key);
        }

        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed < min || parsed > max) {
                throw new IllegalStateException("Property out of range: " + key);
            }
            return parsed;
        }
        catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid long property: " + key, ex);
        }
    }
}
