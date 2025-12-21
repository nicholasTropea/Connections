package com.nicholasTropea.game.server;

import com.nicholasTropea.game.server.NetworkManager;

/**
 * Punto di ingresso principale del server del gioco.
 * 
 * Avvia il {@link NetworkManager} in un thread separato per gestire
 * le connessioni dei client in parallelo al thread principale.
 * 
 * @author Nicholas Riccardo Tropea
 */
public class ServerMain {
    /** Porta di ascolto del server per le connessioni TCP. */
    private static final int SERVER_PORT = 5555;

    /**
     * Avvia il server creando e lanciando il NetworkManager.
     * 
     * @param args Argomenti della riga di comando (non utilizzati)
     */
    public static void main(String[] args) {
        NetworkManager netManager = new NetworkManager(ServerMain.SERVER_PORT);
        new Thread(netManager).start();
    }
}