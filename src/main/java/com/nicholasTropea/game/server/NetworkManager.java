package com.nicholasTropea.game.server;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.IOException;
import java.util.Objects;

/**
 * Listens to incoming connections and creates threads to handle single clients.
 *
 * Implements {@link Runnable} so that it can be executed in a separate thread.
 * Uses an {@link ExecutorService} to handle the {@link ClientHandler} in parallel. 
 * 
 * @author Nicholas Riccardo Tropea
 */
public class NetworkManager implements Runnable {
    /** Listening server port. */
    private final int port;

    /** Thread pool for client handlers. */
    private final ExecutorService pool;

    /** Shared runtime containing repositories and coordinators. */
    private final ServerRuntime runtime;


    /**
     * Creates a new NetworkManager  with the passed port.
     * 
     * @param port TCP port on which to listen for connections
     */
    public NetworkManager(int port, ServerRuntime runtime) {
        this.port = port;
        this.pool = Executors.newCachedThreadPool();
        this.runtime = Objects.requireNonNull(runtime, "runtime is required");
    }


    /** Executes the main server listener. */
    @Override
    public void run() { this.start(); }


    /**
     * Avvia il ServerSocket e inizia l'ascolto delle connessioni.
     * 
     * @throws IOException Se la porta è già occupata o errore di rete
     */
    private void start() {
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            System.out.println("Server active on port: " + this.port);
            this.listenForConnections(serverSocket);
        }
        catch (IOException e) {
            System.err.println("Error when starting the server: " + e.getMessage());
        }
    }


    /**
     * Main incoming client connections acceptance cicle
     * 
     * For each accepted client it creates a new {@link ClientHandler}
     * and executes it in the thread pool.
     * 
     * @param serverSocket Open server socket
     */
    private void listenForConnections(ServerSocket serverSocket) {
        while (true) {
            try { // No try-with-resources otherwise the socket will close
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connessione da: " + clientSocket.getInetAddress());
                
                // Create a new client handler thread and execute it
                ClientHandler handler = new ClientHandler(clientSocket, this.runtime);
                this.pool.execute(handler);
            }
            catch (IOException e) { System.err.println("Errore: " + e.getMessage()); }
        }
    }
}