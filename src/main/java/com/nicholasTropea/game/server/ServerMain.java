package com.nicholasTropea.game.server;

import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.google.gson.Gson;

import com.nicholasTropea.game.net.RegisterRequest;
import com.nicholasTropea.game.net.RegisterResponse;

public class ServerMain {
    public static void main(String[] args) {
        int port = 5555;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server attivo sulla porta " + port);

            while (true) {
                try (
                        Socket clientSocket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
                ) {
                    System.out.println("Connessione da: " + clientSocket.getInetAddress());

                    Gson gson = new Gson();
                    
                    String line = in.readLine();
                    RegisterRequest req = gson.fromJson(line, RegisterRequest.class);
                    System.out.println("Operazione: " + req.getOperation() + ", utente: " + req.getName());

                    RegisterResponse resp = new RegisterResponse("ok");
                    String jsonResp = gson.toJson(resp);

                    out.println(jsonResp);
                }
                catch (IOException e) { System.err.println("Errore." + e.getMessage()); }
            }
        }
        catch (IOException e) { System.err.println("Errore nell'avvio del server: " + e.getMessage()); }
    }
}