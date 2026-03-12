package com.nicholasTropea.game.config;

import java.util.Properties;

/**
 * Client runtime configuration loaded from properties.
 */
public final class ClientConfig {
    private static final String RESOURCE = "config/client.properties";

    private final String serverHost;
    private final int serverPort;
    private final int udpListenPort;


    private ClientConfig(String serverHost, int serverPort, int udpListenPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.udpListenPort = udpListenPort;
    }


    /**
     * Loads client configuration from classpath default resource.
     *
     * @return loaded client config
     */
    public static ClientConfig loadDefault() {
        Properties properties = PropertiesLoader.loadFromClasspath(RESOURCE);

        String host = properties.getProperty("serverIP");
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalStateException(
                "Missing required property: serverIP"
            );
        }

        int port = parseInt(properties, "serverPort", 1, 65535);
        int udpPort = parseInt(properties, "clientUDPPort", 0, 65535);
        return new ClientConfig(host.trim(), port, udpPort);
    }


    /** @return server IP */
    public String getServerHost() { return this.serverHost; }

    /** @return server TCP port */
    public int getServerPort() { return this.serverPort; }

    /** @return client UDP listening port (0 means ephemeral) */
    public int getUdpListenPort() { return this.udpListenPort; }


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
}
