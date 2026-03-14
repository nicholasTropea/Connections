package com.nicholasTropea.game.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

import java.io.IOException; 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.nicholasTropea.game.config.ClientConfig;
import com.nicholasTropea.game.net.Request;
import com.nicholasTropea.game.net.Response;
import com.nicholasTropea.game.net.ResponseDeserializer;

import com.nicholasTropea.game.net.requests.*;
import com.nicholasTropea.game.net.responses.*;


/**
 * Main client application for the Connections game.
 * 
 * Allows users to select actions, send requests to the server,
 * and receive responses.
 */
public class ClientMain {
    /** Client operation logger persisted to file. */
    private static final Logger LOGGER = Logger.getLogger("connections.client.ops");


    /** CLI menu actions. */
    private enum ClientAction {
        REGISTER("Register"),
        LOGIN("Login"),
        LOGOUT("Logout"),
        REQUEST_GAME_INFO("Request game information"),
        REQUEST_GAME_STATS("Request game statistics"),
        REQUEST_LEADERBOARD("Request leaderboard"),
        REQUEST_PLAYER_STATS("Request player statistics"),
        SUBMIT_PROPOSAL("Submit proposal"),
        UPDATE_CREDENTIALS("Update credentials"),
        EXIT("Exit application");

        private final String label;


        ClientAction(String label) { this.label = label; }


        public String getLabel() { return this.label; }
    }


    /**
     * Main entry point for the client application.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // SETUP
        configureLogging();

        ClientConfig config = ClientConfig.loadDefault();
        String host = config.getServerHost();
        int port = config.getServerPort();
        int udpListenPort = config.getUdpListenPort();
        boolean loggedIn = false;

        // OPEN CONNECTION
        try (
            DatagramSocket udpSocket = new DatagramSocket(udpListenPort);
            Socket socket = new Socket(host, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                                new InputStreamReader(socket.getInputStream())
            );
            Scanner scan = new Scanner(System.in)
        ) {
            printConnectionMessage(host, port, udpSocket);
            startUdpListener(udpSocket);

            Gson gson = new GsonBuilder()
                        .registerTypeAdapter(
                            Response.class,
                            new ResponseDeserializer()
                        ).create();
            
            Request req;

            while ((req = getRequest(scan, loggedIn, udpSocket)) != null) {  
                sendRequest(req, gson, out);

                Response resp = awaitResponse(in, gson);
                if (resp == null) { break; }
                
                loggedIn = handleResponse(resp, loggedIn);
            }

            System.out.println("Goodbye, hope you had fun!");
        }
        catch (IOException e) { 
            System.err.println("Connection error: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Connection error", e);
        }
    }


    /**
     * Prompts the user to select and create a request.
     * 
     * @param scan Scanner for reading user input
     * @param loggedIn logged in flag
     * @param udpSocket client udp listener socket
     * @return The created request
     */
    private static Request getRequest(
        Scanner scan,
        boolean loggedIn,
        DatagramSocket udpSocket
    ) {
        while (true) {
            System.out.println("\nAvailable Actions:");

            List<ClientAction> options = buildMenu(loggedIn);

            for (int i = 0; i < options.size(); i++) {
                System.out.println("  " + (i + 1) + ": " + options.get(i).getLabel());
            }
            
            System.out.print("\nSelect action (enter code): ");

            try {
                int code = Integer.parseInt(scan.nextLine().trim());
                if (code < 1 || code > options.size()) {
                    throw new IllegalArgumentException("Invalid selection");
                }

                ClientAction action = options.get(code - 1);
                return buildRequestFromAction(action, scan, udpSocket);
            }
            catch (IllegalArgumentException e) {
                System.out.println("Invalid selection: " + e.getMessage());
                System.out.println("Please try again.\n");
            }
        }
    }


    /**
     * Builds currently available menu actions.
     *
     * @param loggedIn whether client is currently authenticated
     * @return ordered list of allowed actions
     */
    private static List<ClientAction> buildMenu(boolean loggedIn) {
        List<ClientAction> options = new ArrayList<>();
        options.add(ClientAction.UPDATE_CREDENTIALS);

        if (!loggedIn) {
            options.add(ClientAction.REGISTER);
            options.add(ClientAction.LOGIN);
        }
        else {
            options.add(ClientAction.LOGOUT);
            options.add(ClientAction.REQUEST_GAME_INFO);
            options.add(ClientAction.REQUEST_GAME_STATS);
            options.add(ClientAction.REQUEST_LEADERBOARD);
            options.add(ClientAction.REQUEST_PLAYER_STATS);
            options.add(ClientAction.SUBMIT_PROPOSAL);
        }

        options.add(ClientAction.EXIT);
        return options;
    }


    /**
     * Creates a request from a selected client action.
     *
     * @param action selected action
     * @param scan scanner for user input
     * @param udpSocket client udp listener socket
     * @return built request or null if action is exit
     */
    private static Request buildRequestFromAction(
        ClientAction action,
        Scanner scan,
        DatagramSocket udpSocket
    ) {
        return switch (action) {
            case REGISTER -> RegisterRequest.createRequest(scan);
            case LOGIN -> LoginRequest.createRequest(scan, udpSocket.getLocalPort());
            case LOGOUT -> LogoutRequest.createRequest();
            case REQUEST_GAME_INFO -> GameInfoRequest.createRequest(scan);
            case REQUEST_GAME_STATS -> GameStatsRequest.createRequest(scan);
            case REQUEST_LEADERBOARD -> LeaderboardRequest.createRequest(scan);
            case REQUEST_PLAYER_STATS -> PlayerStatsRequest.createRequest();
            case SUBMIT_PROPOSAL -> SubmitProposalRequest.createRequest(scan);
            case UPDATE_CREDENTIALS -> UpdateCredentialsRequest.createRequest(scan);
            case EXIT -> null;
        };
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
    private static void displayResponseDetails(Response resp) { // SHOULD REFACTOR
        switch (resp) {
            case LoginResponse r -> {
                System.out.println("Login successful.");
                System.out.println("Current Game ID: " + r.getGameId());
                System.out.println("Time Left: " + formatDuration(r.getTimeLeft()));
                System.out.println("Errors: " + r.getErrors() + "/4");
                System.out.println("Score: " + r.getScore());
                System.out.println("Words:");
                r.getWords().forEach(word -> System.out.println("  - " + word));
                System.out.println("Guessed Groups: " + r.getGuessedGroups().size());
                for (List<String> group : r.getGuessedGroups()) {
                    System.out.println("  * " + String.join(", ", group));
                }
            }
            case GameInfoResponse r -> {
                System.out.println("Game active: " + r.isActive());
                if (r.isActive()) {
                    System.out.println("Time Left: " + formatDuration(r.getTimeLeft()));
                    System.out.println("Words Left:");
                    r.getWordsLeft().forEach(word -> System.out.println("  - " + word));
                }
                else if (r.getSolution() != null) {
                    System.out.println("Final Solution:");
                    for (List<String> group : r.getSolution()) {
                        System.out.println("  * " + String.join(", ", group));
                    }
                }

                System.out.println("Guessed Groups: " + r.getGuessedGroups().size());
                System.out.println("Errors: " + r.getErrors() + "/4");
                System.out.println("Score: " + r.getScore());
            }
            case GameStatsResponse r -> {
                System.out.println("Game active: " + r.isActive());
                System.out.println("Active Players: " + r.getActivePlayers());
                System.out.println("Finished Players: " + r.getFinishedPlayers());
                System.out.println("Won Players: " + r.getWonPlayers());
                if (!r.isActive()) {
                    System.out.println("Participants: " + r.getTotalPlayers());
                    System.out.println("Average Score: " + r.getAverageScore());
                }
            }
            case LeaderboardResponse r -> {
                System.out.println("Leaderboard:");
                r.getRecords().forEach(rec -> 
                    System.out.println(
                        "  " + rec.getPosition() + ". " + rec.getUsername()
                    )
                );
            }
            case PlayerStatsResponse r -> {
                System.out.println("Solved: " + r.getSolvedPuzzles());
                System.out.println("Failed: " + r.getFailedPuzzles());
                System.out.println("Unfinished: " + r.getUnfinishedPuzzles());
                System.out.println("Perfect: " + r.getPerfectPuzzles());
                System.out.println("Win Rate: " + r.getWinRate() + "%");
                System.out.println("Loss Rate: " + r.getLossRate() + "%");
                System.out.println("Current Streak: " + r.getCurrentStreak());
                System.out.println("Max Streak: " + r.getMaxStreak());
            }
            case SubmitProposalResponse r -> {
                System.out.println("Proposal Result: " + r.getResult());
                if (r.getResult()) {
                    System.out.println("Group Name: " + r.getGroupName());
                }
            }
            default -> {
                if (resp instanceof LogoutResponse) {
                    System.out.println("Logout successful.");
                }
                else if (resp instanceof RegisterResponse) {
                    System.out.println("Registration successful.");
                }
                else if (resp instanceof UpdateCredentialsResponse) {
                    System.out.println("Credentials updated successfully.");
                }
            }
        }
    }


    /** Configures file logger for client operations. */
    private static void configureLogging() {
        try {
            Path logDir = Path.of("logs");
            Files.createDirectories(logDir);

            FileHandler fileHandler = new FileHandler(
                logDir.resolve("client-operations.log").toString(),
                true
            );
            fileHandler.setFormatter(new SimpleFormatter());

            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.INFO);
            LOGGER.addHandler(fileHandler);
        }
        catch (IOException ex) {
            System.err.println("Warning: cannot initialize client log file.");
        }
    }


    /**
     * Formats remaining time in mm:ss.
     *
     * @param millis duration in milliseconds
     * @return formatted duration
     */
    private static String formatDuration(Long millis) {
        if (millis == null) {
            return "n/a";
        }

        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }


    /**
     * Updates logger context for successful auth transitions.
     *
     * @param response server response
     */
    private static void updateAuthStateOnSuccess(Response response) {
        if (response instanceof LoginResponse) {
            LOGGER.info("Authentication state changed: logged in");
        }
        else if (response instanceof LogoutResponse) {
            LOGGER.info("Authentication state changed: logged out");
        }
    }


    /**
     * Starts a background thread that listens for UDP notifications.
     *
     * @param udpSocket socket bound to local UDP port
     */
    private static void startUdpListener(DatagramSocket udpSocket) {
        Thread listenerThread = new Thread(() -> {
            byte[] buffer = new byte[2048];

            while (!udpSocket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    
                    String json = new String(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getLength(),
                        StandardCharsets.UTF_8
                    );

                    System.out.println("\n[ASYNC UDP NOTIFICATION]");
                    System.out.println("Round update received.");
                    LOGGER.info("ASYNC_UDP " + json);
                    System.out.print("\nSelect action (enter code): ");
                }
                catch (IOException ex) {
                    if (!udpSocket.isClosed()) {
                        System.err.println(
                            "UDP listener error: " + ex.getMessage()
                        );
                    }
                    break;
                }
            }
        }, "Connections client UDP listener");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }


    /**
     * Prints the server connection message
     * 
     * @param host server IP
     * @param port server TCP port
     * @param udpSocket socket bound to UDP local port
     */
    private static void printConnectionMessage(
        String host,
        int port,
        DatagramSocket udpSocket
    ) {
        System.out.println("Connected to server at " + host + ":" + port);
        System.out.println(
            "UDP notifications listening on port " + udpSocket.getLocalPort()
        );
        System.out.println("=".repeat(60));
    }


    /**
     * Serializes the given {@link Request} object to JSON and sends it to the server.
     * Logs the operation and the serialized request for debugging purposes.
     *
     * @param req   the request object to be sent
     * @param gson  the Gson instance used for serialization
     * @param out   the PrintWriter used to send the request to the server
     */
    private static void sendRequest(Request req, Gson gson, PrintWriter out) {
        String jsonReq = gson.toJson(req);
        LOGGER.info("SENDING REQUEST " + req.getOperation() + " -> " + jsonReq);
        out.println(jsonReq);
    }


    /**
     * Waits for a response from the server by reading a line
     * from the provided BufferedReader.
     * If the server closes the connection (i.e., the read line is null),
     * logs a warning and returns null.
     * Otherwise, logs the received response and parses it into a
     * Response object using the provided Gson instance.
     *
     * @param in   the BufferedReader to read the server response from
     * @param gson the Gson instance used to parse the response
     * @return     the parsed Response object, or null if the server closed the connection
     */
    private static Response awaitResponse(BufferedReader in, Gson gson) {
        String respLine = null;

        try { respLine = in.readLine(); }
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read server response.", e);
            return null;
        }

        if (respLine == null) {
            System.out.println("\nServer closed the connection. Exiting client.");
            LOGGER.warning("Server closed TCP connection.");
            return null;
        }

        LOGGER.info("RESPONSE: " + respLine);

        try { return parseResponse(respLine, gson); }
        catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Invalid response payload: " + respLine, e);
            return null;
        }
    }


    /**
     * Handles a server response and computes the next authentication state.
     *
     * @param resp current response from server
     * @param loggedIn current authentication state before handling response
     * @return updated authentication state after handling response
     */
    private static boolean handleResponse(Response resp, boolean loggedIn) {
        if (resp.isSuccess()) {
            if (resp instanceof LoginResponse) { loggedIn = true; }
            else if (resp instanceof LogoutResponse) { loggedIn = false; }
            
            updateAuthStateOnSuccess(resp);
            displayResponseDetails(resp);
        }
        else { System.out.println("Operation failed: " + resp.getError()); }

        return loggedIn;
    }
}