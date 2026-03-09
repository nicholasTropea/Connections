package com.nicholasTropea.game.server;

/**
 * Main entry point for the game server.
 * 
 * Starts the {@link NetworkManager} in a separate thread to handle
 * client connections in parallel to the main thread.
 * 
 * @author Nicholas Riccardo Tropea
 */
public class ServerMain {
    /** Server listening port for TCP connections. */
    private static final int SERVER_PORT = 5555;


    /**
     * Starts the server by creating and launching the NetworkManager.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("CONNECTIONS GAME SERVER");
        System.out.println("=".repeat(60));
        System.out.println("Starting server on port " + SERVER_PORT + "...");
        
        NetworkManager netManager = new NetworkManager(ServerMain.SERVER_PORT);
        new Thread(netManager).start();
        
        System.out.println("Server started successfully!");
        System.out.println("Waiting for client connections...");
        System.out.println("=".repeat(60));
    }
}