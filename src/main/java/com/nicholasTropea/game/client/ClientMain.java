package com.nicholasTropea.game.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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

import static com.nicholasTropea.game.client.ConsoleUtils.*;

/**
 * Main client application for the Connections game using NIO.
 */
public class ClientMain {
    private static final Logger LOGGER = Logger.getLogger("connections.client.ops");
    private static final BlockingQueue<Response> RESPONSE_QUEUE = new LinkedBlockingQueue<>();
    private static final Gson GSON = new GsonBuilder()
                        .registerTypeAdapter(Response.class, new ResponseDeserializer())
                        .create();

    // UI State
    private static volatile boolean loggedIn = false;
    private static String currentUser = "Guest";
    private static Integer currentGameId = null;
    private static String lastKnownTime = "N/A";
    private static int currentScore = 0;
    private static int currentErrors = 0;
    private static List<String> currentWords = new ArrayList<>();
    private static String lastOpMessage = "";
    private static Response lastDetailedResponse = null;

    public static void main(String[] args) {
        configureLogging();
        ClientConfig config = ClientConfig.loadDefault();

        try (
            SocketChannel channel = SocketChannel.open();
            DatagramSocket udpSocket = new DatagramSocket(config.getUdpListenPort());
            Scanner scan = new Scanner(System.in)
        ) {
            channel.connect(new InetSocketAddress(config.getServerHost(), config.getServerPort()));
            channel.configureBlocking(false);

            printConnectionMessage(config.getServerHost(), config.getServerPort(), udpSocket.getLocalPort());
            
            // Start NIO Response Listener
            Thread listener = new Thread(() -> startResponseListener(channel));
            listener.setDaemon(true);
            listener.start();

            // Start UDP Notification Listener
            startUdpListener(udpSocket);

            while (true) {
                renderDashboard();
                List<ClientAction> options = buildMenu();
                displayMenu(options);
                
                System.out.print(BOLD + "\nSelect action (enter code): " + RESET);
                String selected = scan.nextLine().trim();
                if (selected.isEmpty()) continue;

                try {
                    int code = Integer.parseInt(selected);
                    if (code < 1 || code > options.size()) {
                        lastOpMessage = RED + "Invalid selection. Please try again." + RESET;
                        continue;
                    }
                    
                    ClientAction action = options.get(code - 1);
                    if (action == ClientAction.EXIT) break;

                    Request req = buildRequestFromAction(action, scan, udpSocket.getLocalPort());
                    if (req != null) {
                        // Only capture username for login to update UI
                        // Registration should not change the 'currentUser' (Guest)
                        final String attemptedUser = (req instanceof LoginRequest lr) ? lr.getUsername() : null;

                        sendRequest(channel, req);
                        Response resp = RESPONSE_QUEUE.take(); 
                        handleResponse(resp, attemptedUser, action.label);
                    }

                }
                catch (NumberFormatException e) {
                    printError("Invalid selection. Please enter a number.");
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception e) {
                    printError("Error: " + e.getMessage());
                }
            }
            System.out.println(BOLD + "\nGoodbye, hope you had fun!" + RESET);
        }
        catch (IOException e) {
            printError("Connection error: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Client error", e);
        }
    }

    private static void renderDashboard() {
        clearScreen();
        printHeader(currentUser, currentGameId, lastKnownTime, currentScore, currentErrors);
        
        if (loggedIn && !currentWords.isEmpty()) {
            System.out.println(BOLD + "\nCURRENT WORDS TO GROUP:" + RESET);
            printWordGrid(currentWords);
        }

        if (lastDetailedResponse != null) {
            displayResponseDetails(lastDetailedResponse);
            lastDetailedResponse = null; // Show once as detailed, then keep in header
        }

        if (!lastOpMessage.isEmpty()) {
            System.out.println("\n" + lastOpMessage);
            lastOpMessage = ""; // Clear after display
        }
    }

    private static void startResponseListener(SocketChannel channel) {
        try (Selector selector = Selector.open()) {
            channel.register(selector, SelectionKey.OP_READ);
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            StringBuilder messageBuilder = new StringBuilder();

            while (channel.isOpen()) {
                if (selector.select() == 0) continue;
                var keys = selector.selectedKeys();
                var iter = keys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isReadable()) {
                        buffer.clear();
                        int read = channel.read(buffer);
                        if (read == -1) {
                            channel.close();
                            return;
                        }
                        buffer.flip();
                        messageBuilder.append(StandardCharsets.UTF_8.decode(buffer));

                        int nl;
                        while ((nl = messageBuilder.indexOf("\n")) != -1) {
                            String line = messageBuilder.substring(0, nl).trim();
                            messageBuilder.delete(0, nl + 1);
                            if (!line.isEmpty()) {
                                Response resp = GSON.fromJson(line, Response.class);
                                RESPONSE_QUEUE.offer(resp);
                            }
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Response listener error", e);
        }
    }

    private static void startUdpListener(DatagramSocket socket) {
        Thread thread = new Thread(() -> {
            byte[] buf = new byte[8192];
            while (!socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String json = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                    
                    RoundEndedNotification notif = GSON.fromJson(json, RoundEndedNotification.class);
                    if (notif != null && "roundEnded".equals(notif.type)) {
                        // Update state for the new round
                        currentGameId = notif.nextGameId;
                        lastKnownTime = "NEW ROUND";
                        currentWords = notif.nextWords != null ? new ArrayList<>(notif.nextWords) : new ArrayList<>();
                        currentErrors = 0;
                        currentScore = 0;
                        
                        lastOpMessage = YELLOW + BOLD + "⚠ ROUND ROTATED! NEW GAME: " + notif.nextGameId + RESET;
                        
                        // Detailed summary of the ended game
                        System.out.println("\n");
                        printSectionTitle("Round Ended - Game " + notif.prevGameId);
                        System.out.println(BOLD + "Global Statistics:" + RESET);
                        System.out.printf("Participants: %d | Winners: %d\n", notif.participants, notif.won);
                        System.out.printf("Average Score: %.2f\n", notif.avgScore);
                        if (notif.solution != null) {
                            System.out.println(BOLD + "\nSolution for Game " + notif.prevGameId + ":" + RESET);
                            notif.solution.forEach(g -> System.out.println(" • " + String.join(", ", g)));
                        }
                        printSeparator();
                        System.out.print(BOLD + YELLOW + "\n⚠ ROUND CHANGED! Press [ENTER] to refresh dashboard... " + RESET);
                    }
                }
                catch (IOException ignored) {}
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static class RoundEndedNotification {
        String type;
        int prevGameId;
        int nextGameId;
        long roundNumber;
        List<List<String>> solution;
        int participants;
        int won;
        float avgScore;
        List<String> nextWords;
    }

    private static void sendRequest(SocketChannel channel, Request req) throws IOException {
        String json = GSON.toJson(req) + "\n";
        ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        LOGGER.info("SENT: " + json.trim());
    }

    private static void handleResponse(Response resp, String attemptedUser, String opLabel) {
        if (!resp.isSuccess()) {
            lastOpMessage = RED + "✘ [" + opLabel + "] Error: " + resp.getError() + RESET;
            return;
        }

        lastOpMessage = GREEN + "✔ " + opLabel + " successful." + RESET;
        lastDetailedResponse = resp;

        if (resp instanceof LoginResponse r) {
            loggedIn = true;
            currentUser = attemptedUser;
            currentGameId = r.getGameId();
            lastKnownTime = formatDuration(r.getTimeLeft());
            currentScore = r.getScore();
            currentErrors = r.getErrors();
            currentWords = r.getWords() != null ? r.getWords() : new ArrayList<>();
        }
        else if (resp instanceof LogoutResponse) {
            loggedIn = false;
            currentUser = "Guest";
            currentGameId = null;
            lastKnownTime = "N/A";
            currentScore = 0;
            currentErrors = 0;
            currentWords.clear();
        }
        else if (resp instanceof GameInfoResponse r) {
            if (r.isActive()) {
                lastKnownTime = formatDuration(r.getTimeLeft());
                currentScore = r.getScore();
                currentErrors = r.getErrors();
                currentWords = r.getWordsLeft() != null ? r.getWordsLeft() : new ArrayList<>();
            }
        }
        else if (resp instanceof SubmitProposalResponse r) {
            if (r.getResult()) {
                lastOpMessage = GREEN + "✔ [" + opLabel + "] CORRECT! Group: " + BOLD + r.getGroupName() + RESET;
            } else {
                lastOpMessage = RED + "✘ [" + opLabel + "] INCORRECT PROPOSAL." + RESET;
            }
        }
    }

    private static void displayResponseDetails(Response resp) {
        switch (resp) {
            case PlayerStatsResponse r -> {
                printSectionTitle("Your Statistics");
                System.out.printf("Solved: %d | Failed: %d | Unfinished: %d\n", r.getSolvedPuzzles(), r.getFailedPuzzles(), r.getUnfinishedPuzzles());
                System.out.printf("Win Rate: %.1f%% | Perfect Games: %d\n", r.getWinRate(), r.getPerfectPuzzles());
                System.out.printf("Streak: %d (Max: %d)\n", r.getCurrentStreak(), r.getMaxStreak());
                if (r.getHistogram() != null) {
                    System.out.println("\nMistake Histogram:");
                    r.getHistogram().print();
                }
            }
            case LeaderboardResponse r -> {
                printSectionTitle("Leaderboard");
                if (r.getRecords() == null || r.getRecords().isEmpty()) {
                    System.out.println("No data available.");
                } else {
                    r.getRecords().forEach(rec -> 
                        System.out.printf("%2d. %-15s %5d pts\n", rec.getPosition(), rec.getUsername(), rec.getPoints())
                    );
                }
            }
            case GameStatsResponse r -> {
                printSectionTitle("Global Game Stats");
                if (r.isActive()) {
                    System.out.println("Active Players: " + r.getActivePlayers());
                } else {
                    System.out.println("Total Participants: " + r.getTotalPlayers());
                    System.out.println("Average Score: " + String.format("%.2f", r.getAverageScore()));
                }
                System.out.println("Finished: " + r.getFinishedPlayers() + " | Winners: " + r.getWonPlayers());
            }
            case GameInfoResponse r -> {
                if (!r.isActive()) {
                    printSectionTitle("Game Summary");
                    System.out.println("Final Score: " + GREEN + r.getScore() + RESET);
                    if (r.getSolution() != null) {
                        System.out.println(BOLD + "\nSOLUTION:" + RESET);
                        r.getSolution().forEach(g -> System.out.println(" • " + String.join(", ", g)));
                    }
                }
            }
            default -> {}
        }
    }

    private enum ClientAction {
        REGISTER("Register"), LOGIN("Login"), LOGOUT("Logout"),
        GAME_INFO("Game Info"), GAME_STATS("Game Stats"), LEADERBOARD("Leaderboard"),
        PLAYER_STATS("My Stats"), SUBMIT("Submit Proposal"), UPDATE("Update Credentials"),
        EXIT("Exit");
        private final String label;
        ClientAction(String l) { this.label = l; }
    }

    private static List<ClientAction> buildMenu() {
        List<ClientAction> actions = new ArrayList<>();
        actions.add(ClientAction.UPDATE);
        if (!loggedIn) {
            actions.add(ClientAction.REGISTER);
            actions.add(ClientAction.LOGIN);
        } else {
            actions.add(ClientAction.LOGOUT);
            actions.add(ClientAction.GAME_INFO);
            actions.add(ClientAction.GAME_STATS);
            actions.add(ClientAction.LEADERBOARD);
            actions.add(ClientAction.PLAYER_STATS);
            actions.add(ClientAction.SUBMIT);
        }
        actions.add(ClientAction.EXIT);
        return actions;
    }

    private static void displayMenu(List<ClientAction> options) {
        printSectionTitle("Menu");
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("  %d. %s\n", i + 1, options.get(i).label);
        }
    }

    private static Request buildRequestFromAction(ClientAction action, Scanner scan, int udpPort) {
        return switch (action) {
            case REGISTER -> RegisterRequest.createRequest(scan);
            case LOGIN -> LoginRequest.createRequest(scan, udpPort);
            case LOGOUT -> LogoutRequest.createRequest();
            case GAME_INFO -> GameInfoRequest.createRequest(scan);
            case GAME_STATS -> GameStatsRequest.createRequest(scan);
            case LEADERBOARD -> LeaderboardRequest.createRequest(scan);
            case PLAYER_STATS -> PlayerStatsRequest.createRequest();
            case SUBMIT -> SubmitProposalRequest.createRequest(scan);
            case UPDATE -> UpdateCredentialsRequest.createRequest(scan);
            case EXIT -> null;
        };
    }

    private static String formatDuration(Long ms) {
        if (ms == null) return "N/A";
        long s = Math.max(0, ms / 1000);
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private static void printConnectionMessage(String host, int port, int udpPort) {
        System.out.println(BOLD + CYAN + "CONNECTED TO " + host + ":" + port + RESET);
        System.out.println(CYAN + "UDP Notifications on port " + udpPort + RESET);
        System.out.println("=".repeat(40));
    }

    private static void configureLogging() {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("logs"));
            FileHandler fh = new FileHandler("logs/client.log", true);
            fh.setFormatter(new SimpleFormatter());
            LOGGER.setUseParentHandlers(false);
            LOGGER.addHandler(fh);
        } catch (IOException ignored) {}
    }
}
