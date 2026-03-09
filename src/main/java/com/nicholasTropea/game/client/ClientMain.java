package com.nicholasTropea.game.client;

import java.net.Socket;

import java.io.IOException; 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.nicholasTropea.game.net.Request;
import com.nicholasTropea.game.net.Response;
import com.nicholasTropea.game.net.ResponseDeserializer;

import com.nicholasTropea.game.net.responses.*;


/**
 * Main client application for the Connections game.
 * 
 * Allows users to select actions, send requests to the server,
 * and receive responses.
 */
public class ClientMain {
    /**
     * Main entry point for the client application.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5555;

        System.out.println("=".repeat(60));
        System.out.println("CONNECTIONS GAME CLIENT");
        System.out.println("=".repeat(60));

        try (
                Socket socket = new Socket(host, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                                    new InputStreamReader(socket.getInputStream())
                );
                Scanner scan = new Scanner(System.in)
        ) {
            System.out.println("Connected to server at " + host + ":" + port);
            System.out.println("=".repeat(60));

            Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Response.class, new ResponseDeserializer())
                        .create();
            while (true) {
                Request req = askForRequest(scan); 

                String jsonReq = gson.toJson(req);
                
                System.out.println("\n" + "-".repeat(60));
                System.out.println("[SENDING REQUEST]");
                System.out.println("Request Type: " + req.getClass().getSimpleName());
                System.out.println("Operation: " + req.getOperation());
                System.out.println("Raw JSON: " + jsonReq);

                out.println(jsonReq);

                String respLine = in.readLine();
                
                System.out.println("\n[RECEIVED RESPONSE]");
                System.out.println("Raw JSON: " + respLine);
                
                Response resp = parseResponse(respLine, gson);
                
                System.out.println("Response Type: " + resp.getClass().getSimpleName());
                System.out.println("Success: " + resp.isSuccess());
                
                if (resp.isSuccess()) {
                    System.out.println("\nRequest handled successfully by server!");
                    displayResponseDetails(resp);
                } else {
                    System.out.println("\nRequest failed with error: " + resp.getError());
                }
                
                System.out.println("-".repeat(60));
            }
        }
        catch (IOException e) { 
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace(); 
        }
    }


    /**
     * Prompts the user to select and create a request.
     * 
     * @param scan Scanner for reading user input
     * @return The created request
     */
    private static Request askForRequest(Scanner scan) {
        while (true) {
            System.out.println("\nAvailable Actions:");
            printMenu();
            System.out.print("\nSelect action (enter code): ");

            try {
                int code = Integer.parseInt(scan.nextLine().trim());
                return Request.parseOption(Request.RequestTypes.fromCode(code));
            }
            catch (IllegalArgumentException e) {
                System.out.println("Invalid selection: " + e.getMessage());
                System.out.println("Please try again.\n");
            }
        }
    }


    /**
     * Parses the JSON response into the appropriate Response subclass.
     * 
     * @param jsonResp The JSON response string
     * @param gson The Gson instance for deserialization
     * @return The parsed Response object
     */
    private static Response parseResponse(String jsonResp, Gson gson) {
        return gson.fromJson(jsonResp, Response.class);
    }


    /**
     * Displays additional details from the response based on its type.
     * 
     * @param resp The response to display
     */
    private static void displayResponseDetails(Response resp) {
        switch (resp) {
            case LoginResponse r -> {
                System.out.println("Game ID: " + r.getGameId());
                System.out.println("Words: " + r.getWords().size());
                System.out.println("Time Left: " + r.getTimeLeft() + " ms");
                System.out.println("Errors: " + r.getErrors());
                System.out.println("Score: " + r.getScore());
            }
            case GameInfoResponse r -> {
                System.out.println("Active: " + r.isActive());
                System.out.println("Errors: " + r.getErrors());
                System.out.println("Score: " + r.getScore());
            }
            case GameStatsResponse r -> {
                System.out.println("Active: " + r.isActive());
                System.out.println("Active Players: " + r.getActivePlayers());
                System.out.println("Finished Players: " + r.getFinishedPlayers());
                System.out.println("Won Players: " + r.getWonPlayers());
            }
            case LeaderboardResponse r -> {
                System.out.println("Records returned: " + r.getRecords().size());
                r.getRecords().forEach(rec -> 
                    System.out.println("  - " + rec.getUsername() + ": Position " + rec.getPosition())
                );
            }
            case PlayerStatsResponse r -> {
                System.out.println("Solved: " + r.getSolvedPuzzles());
                System.out.println("Failed: " + r.getFailedPuzzles());
                System.out.println("Win Rate: " + r.getWinRate() + "%");
                System.out.println("Current Streak: " + r.getCurrentStreak());
            }
            case SubmitProposalResponse r -> {
                System.out.println("Proposal Result: " + r.getResult());
                if (r.getResult()) {
                    System.out.println("Group Name: " + r.getGroupName());
                }
            }
            default -> {
                // Simple responses (Logout, Register, UpdateCredentials) have no extra details
            }
        }
    }


    /**
     * Prints the menu of available actions.
     */
    private static void printMenu() {
        for (Request.RequestTypes type : Request.RequestTypes.values()) {
            System.out.println("  " + type.getCode() + ": " + type.getAction());
        }
    }
}