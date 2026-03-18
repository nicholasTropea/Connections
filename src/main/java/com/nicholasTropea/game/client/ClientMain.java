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
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    /** Queue for pending UDP notifications to avoid interrupting input. */
    private static final Queue<String> PENDING_NOTIFICATIONS = 
        new ConcurrentLinkedQueue<>();


    /** Tracks current console input phase for safe async notification rendering. */
    private enum InputMode {
        IDLE,
        ACTION_SELECTION,
        MULTI_FIELD_INPUT
    }


    /** Current input mode shared with UDP listener thread. */
    private static volatile InputMode currentInputMode = InputMode.IDLE;


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
                displayPendingNotifications();
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
            displayPendingNotifications();
            printSectionTitle("Available Actions");

            List<ClientAction> options = buildMenu(loggedIn);

            for (int i = 0; i < options.size(); i++) {
                System.out.println("  " + (i + 1) + ": " + options.get(i).getLabel());
            }
            
            System.out.print("\nSelect action (enter code): ");

            try {
                currentInputMode = InputMode.ACTION_SELECTION;
                String selected = scan.nextLine().trim();
                currentInputMode = InputMode.IDLE;

                int code = Integer.parseInt(selected);
                if (code < 1 || code > options.size()) {
                    throw new IllegalArgumentException("Invalid selection");
                }

                ClientAction action = options.get(code - 1);
                currentInputMode = InputMode.MULTI_FIELD_INPUT;
                return buildRequestFromAction(action, scan, udpSocket);
            }
            catch (IllegalArgumentException e) {
                System.out.println("Invalid selection: " + e.getMessage());
                System.out.println("Please try again.\n");
            }
            finally {
                currentInputMode = InputMode.IDLE;
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
                printSectionTitle("Login Successful");
                System.out.println("Current Game ID: " + r.getGameId());
                System.out.println("Time Left: " + formatDuration(r.getTimeLeft()));
                System.out.println("Current Errors: " + r.getErrors() + "/4");
                System.out.println("Current Score: " + r.getScore());
                printSeparator();

                if (r.getWords() != null && !r.getWords().isEmpty()) {
                    System.out.println("Words Available: " + r.getWords().size());
                    r.getWords().forEach(word -> System.out.println("  - " + word));
                    printSeparator();
                }

                List<List<String>> guessed = r.getGuessedGroups();
                int guessedCount = guessed == null ? 0 : guessed.size();
                System.out.println("Correct Groups Found: " + guessedCount);
                if (guessed != null && !guessed.isEmpty()) {
                    for (List<String> group : guessed) {
                        System.out.println("  * " + String.join(", ", group));
                    }
                }
            }
            case GameInfoResponse r -> {
                printSectionTitle("Game Information");
                if (r.isActive()) {
                    System.out.println("Status: Active");
                    System.out.println("Time Left: " + formatDuration(r.getTimeLeft()));
                    System.out.println("Current Errors: " + r.getErrors() + "/4");
                    System.out.println("Current Score: " + r.getScore());
                    printSeparator();

                    List<String> wordsLeft = r.getWordsLeft();
                    int wordsLeftCount = wordsLeft == null ? 0 : wordsLeft.size();
                    System.out.println("Words Left to Group: " + wordsLeftCount);
                    if (wordsLeft != null && !wordsLeft.isEmpty()) {
                        wordsLeft.forEach(word -> System.out.println("  - " + word));
                    }
                }
                else {
                    System.out.println("Status: Completed");
                    if (r.getTimeLeft() != null) {
                        System.out.println(
                            "Time Left (Current Round): " +
                            formatDuration(r.getTimeLeft())
                        );
                    }
                    System.out.println("Final Errors: " + r.getErrors() + "/4");
                    System.out.println("Final Score: " + r.getScore());
                    printSeparator();

                    if (r.getSolution() != null && !r.getSolution().isEmpty()) {
                        System.out.println("Final Solution:");
                        for (List<String> group : r.getSolution()) {
                            System.out.println("  * " + String.join(", ", group));
                        }
                        printSeparator();
                    }
                }

                List<List<String>> guessedGroups = r.getGuessedGroups();
                int guessedCount = guessedGroups == null ? 0 : guessedGroups.size();
                System.out.println("Correct Groups Found: " + guessedCount);
                if (guessedGroups != null && !guessedGroups.isEmpty()) {
                    System.out.println("Your Correct Groups:");
                    for (List<String> group : guessedGroups) {
                        System.out.println("  * " + String.join(", ", group));
                    }
                }
            }
            case GameStatsResponse r -> {
                printSectionTitle("Game Statistics");
                if (r.isActive()) {
                    System.out.println("Status: Active");
                    System.out.println("Time Left: " + formatDuration(r.getTimeLeft()));
                    System.out.println(
                        "Players Still Playing: " + r.getActivePlayers()
                    );
                    System.out.println("Players Finished: " + r.getFinishedPlayers());
                    System.out.println("Players Won: " + r.getWonPlayers());
                    printSeparator();
                }
                else {
                    System.out.println("Status: Completed");
                    System.out.println("Participants: " + r.getTotalPlayers());
                    System.out.println("Players Finished: " + r.getFinishedPlayers());
                    System.out.println("Players Won: " + r.getWonPlayers());
                    if (r.getAverageScore() != null) {
                        System.out.println(
                            "Average Score: " + String.format("%.2f", r.getAverageScore())
                        );
                    }
                    printSeparator();
                }
            }
            case LeaderboardResponse r -> {
                printSectionTitle("Leaderboard");
                if (r.getRecords() == null || r.getRecords().isEmpty()) {
                    System.out.println("No ranking data available.");
                    break;
                }

                r.getRecords().forEach(rec -> 
                    System.out.println(
                        "  "
                        + rec.getPosition()
                        + ". "
                        + rec.getUsername()
                        + " ("
                        + rec.getPoints()
                        + " pts)"
                    )
                );
            }
            case PlayerStatsResponse r -> {
                printSectionTitle("Player Statistics");
                int solved = safeInt(r.getSolvedPuzzles());
                int failed = safeInt(r.getFailedPuzzles());
                int unfinished = safeInt(r.getUnfinishedPuzzles());
                int completed = solved + failed + unfinished;

                System.out.println("Puzzles Completed: " + completed);
                System.out.println("Solved Puzzles: " + solved);
                System.out.println("Failed Puzzles: " + failed);
                System.out.println("Unfinished Puzzles: " + unfinished);
                System.out.println("Perfect Puzzles: " + safeInt(r.getPerfectPuzzles()));
                System.out.println("Win Rate: " + formatPercentage(r.getWinRate()));
                System.out.println("Loss Rate: " + formatPercentage(r.getLossRate()));
                System.out.println("Current Streak: " + r.getCurrentStreak());
                System.out.println("Max Streak: " + r.getMaxStreak());
                printSeparator();

                System.out.println("\nMistake Histogram:");
                if (r.getHistogram() != null) {
                    r.getHistogram().print();
                }
                displayPendingNotifications();
            }
            case SubmitProposalResponse r -> {
                printSectionTitle("Proposal Result");
                boolean isCorrect = Boolean.TRUE.equals(r.getResult());
                System.out.println(
                    isCorrect ? "Correct group submitted." : "Incorrect group."
                );
                if (isCorrect) {
                    System.out.println("Group Name: " + r.getGroupName());
                }
            }
            default -> {
                if (resp instanceof LogoutResponse) {
                    printSectionTitle("Logout Successful");
                }
                else if (resp instanceof RegisterResponse) {
                    printSectionTitle("Registration Successful");
                }
                else if (resp instanceof UpdateCredentialsResponse) {
                    printSectionTitle("Credentials Updated");
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
     * Formats percentage values to two decimals and appends percent symbol.
     *
     * @param value numeric percentage value
     * @return formatted value or n/a if null
     */
    private static String formatPercentage(Float value) {
        if (value == null) {
            return "n/a";
        }

        return String.format("%.2f%%", value);
    }


    /**
     * Prints a compact section title for better CLI readability.
     *
     * @param title title text
     */
    private static void printSectionTitle(String title) {
        System.out.println("\n[" + title + "]");
        System.out.println("------------------------------------------------------------");
    }


    /** Prints a visual separator line for content blocks. */
    private static void printSeparator() {
        System.out.println("------------------------------------------------------------");
    }


    /**
     * Returns 0 if boxed integer is null.
     *
     * @param value boxed integer
     * @return integer value or 0
     */
    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
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
     * Displays any pending UDP notifications without interrupting input.
     */
    private static void displayPendingNotifications() {
        while (!PENDING_NOTIFICATIONS.isEmpty()) {
            String notification = PENDING_NOTIFICATIONS.poll();
            System.out.println("\n[ASYNC UDP NOTIFICATION]");
            System.out.println(notification);
        }
    }


    /**
     * Starts a background thread that listens for UDP notifications.
     * Notifications are queued and displayed at safe points to avoid
     * interrupting user input.
     *
     * @param udpSocket socket bound to local UDP port
     */
    private static void startUdpListener(DatagramSocket udpSocket) {
        Thread listenerThread = new Thread(() -> {
            byte[] buffer = new byte[2048];

            while (!udpSocket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(
                        buffer,
                        buffer.length
                    );
                    udpSocket.receive(packet);
                    
                    String json = new String(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getLength(),
                        StandardCharsets.UTF_8
                    );

                    String notification = "Round update received.";
                    if (currentInputMode == InputMode.ACTION_SELECTION) {
                        System.out.println("\n[ASYNC UDP NOTIFICATION]");
                        System.out.println(notification);
                        System.out.print("\nSelect action (enter code): ");
                    }
                    else {
                        PENDING_NOTIFICATIONS.offer(notification);
                    }

                    LOGGER.info("ASYNC_UDP " + json);
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