package com.nicholasTropea.game.server;

import com.nicholasTropea.game.server.ClientHandler;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.IOException;

/**
 * Gestisce l'ascolto delle connessioni in ingresso e la creazione di thread
 * per i client individuali.
 * 
 * Implementa {@link Runnable} per essere eseguito in un thread separato.
 * Utilizza un {@link ExecutorService} per gestire i {@link ClientHandler}
 * in parallelo.
 * 
 * @author Nicholas Riccardo Tropea
 */
public class NetworkManager implements Runnable {
    /** Porta di ascolto del server. */
    private int port;

    /** Pool di thread per i client handler. */
    private ExecutorService pool;

    /**
     * Crea un nuovo NetworkManager con la porta specificata.
     * 
     * @param port Porta TCP su cui ascoltare le connessioni
     */
    public NetworkManager(int port) {
        this.port = port;
        this.pool = Executors.newCachedThreadPool();
    }

    /** Esegue il listener principale del server. */
    @Override
    public void run() { this.start(); }

    /**
     * Avvia il ServerSocket e inizia l'ascolto delle connessioni.
     * 
     * @throws IOException Se la porta è già occupata o errore di rete
     */
    private void start() {
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            System.out.println("Server attivo sulla porta " + this.port);
            this.listenForConnections(serverSocket);
        }
        catch (IOException e) { System.err.println("Errore nell'avvio del server: " + e.getMessage()); }
    }

    /**
     * Ciclo principale di accettazione delle connessioni client.
     * 
     * Per ogni client accettato crea un nuovo {@link ClientHandler}
     * e lo esegue nel pool di thread.
     * 
     * @param serverSocket Socket server già aperto
     */
    private void listenForConnections(ServerSocket serverSocket) {
        while (true) {
            try { // Non uso try-with-resources altrimenti la socket viene chiusa subito
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connessione da: " + clientSocket.getInetAddress());
                
                // Crea un nuovo thread per gestire il client
                ClientHandler handler = new ClientHandler(clientSocket);
                this.pool.execute(handler);
            }
            catch (IOException e) { System.err.println("Errore: " + e.getMessage()); }
        }
    }
}