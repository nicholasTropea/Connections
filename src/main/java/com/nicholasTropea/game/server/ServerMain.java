package com.nicholasTropea.game.server;

import com.nicholasTropea.game.config.ServerConfig;

/**
 * Main entry point for the game server.
 * 
 * Starts the {@link NetworkManager} in a separate thread to handle
 * client connections in parallel to the main thread.
 * 
 * @author Nicholas Riccardo Tropea
 */
public class ServerMain {
    /**
     * Starts the server by creating and launching the NetworkManager.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        ServerConfig config = ServerConfig.loadDefault();

        System.out.println("=".repeat(60));
        System.out.println("CONNECTIONS GAME SERVER");
        System.out.println("=".repeat(60));
        System.out.println("Starting server on port " + config.getTcpPort() + "...");

        ServerRuntime runtime = new ServerRuntime(
            new PlayerRepository(),
            new GameRepository(),
            new SessionManager(),
            config.getRoundDurationMillis(),
            config.getSessionAutosaveSeconds()
        );
        
        Runtime.getRuntime().addShutdownHook(new Thread(runtime::close));

        NetworkManager netManager = new NetworkManager(config.getTcpPort(), runtime);
        new Thread(netManager).start();
        
        System.out.println("Server started successfully!");
        System.out.println("Waiting for client connections...");
        System.out.println("=".repeat(60));
    }
}