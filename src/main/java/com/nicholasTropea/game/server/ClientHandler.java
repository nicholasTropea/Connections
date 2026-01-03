package com.nicholasTropea.game.server;

import java.net.Socket;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.google.gson.Gson;

import com.nicholasTropea.game.net.RegisterRequest;
import com.nicholasTropea.game.net.RegisterResponse;

/**
 * Gestisce la comunicazione con un singolo client connesso.
 * 
 * Esegue il parsing dei messaggi JSON in ingresso e invia risposte.
 * Ogni istanza viene eseguita in un thread separato dal pool di
 * {@link NetworkManager}.
 * 
 * @author Nicholas Riccardo Tropea
 */
public class ClientHandler implements Runnable {
    /** Socket TCP del client connesso. */
    private Socket clientSocket;

    /**
     * Crea un handler per il client specificato.
     * 
     * @param clienSocket Socket del client appena accettato
     */
    public ClientHandler(Socket clientSocket) { this.clientSocket = clientSocket; }

    /**
     * Gestisce il ciclo di vita della connessione con il client.
     * 
     * Legge messaggi JSON in loop fino alla disconnessione e
     * invia risposte appropriate.
     * 
     * @throws IOException In caso di errore di rete
     */
    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            Gson gson = new Gson();
            
            String line = in.readLine();
            RegisterRequest req = gson.fromJson(line, RegisterRequest.class);
            System.out.println("Operazione: " + req.getOperation() + ", utente: " + req.getName());

            RegisterResponse resp = RegisterResponse.success();
            String jsonResp = gson.toJson(resp);

            out.println(jsonResp);
        }
        catch (IOException e) { System.err.println("Errore: " + e.getMessage()); }
    }
}