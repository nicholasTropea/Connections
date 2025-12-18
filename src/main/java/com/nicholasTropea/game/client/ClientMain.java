package com.nicholasTropea.game.client;

import java.net.Socket;

import java.io.IOException; 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.google.gson.Gson;

import com.nicholasTropea.game.net.RegisterRequest;
import com.nicholasTropea.game.net.RegisterResponse;

public class ClientMain {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5555;

        try (
                Socket socket = new Socket(host, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            System.out.println("Connesso al server.");

            Gson gson = new Gson();

            RegisterRequest req = new RegisterRequest("Mario", "12345");
            String jsonReq = gson.toJson(req);

            out.println(jsonReq);
            System.out.println("Richiesta inviata al server.");

            String respLine = in.readLine();
            RegisterResponse resp = gson.fromJson(respLine, RegisterResponse.class);
            System.out.println(resp.getStatus() + " - " + resp.getMessage());
        }
        catch (IOException e) { e.printStackTrace(); }
    }
}