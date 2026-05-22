package com.nicholasTropea.game.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/** Sends asynchronous UDP notifications to subscribed clients. */
public class UdpNotificationService implements AutoCloseable {
    private final DatagramSocket socket;
    private final Gson gson;
    private final Map<Integer, UdpEndpoint> endpoints;

    public UdpNotificationService() {
        try { this.socket = new DatagramSocket(); }
        catch (IOException ex) { throw new IllegalStateException("Cannot create UDP socket", ex); }
        this.gson = new Gson();
        this.endpoints = new ConcurrentHashMap<>();
    }

    public void registerEndpoint(int userId, InetAddress address, int port) {
        this.endpoints.put(userId, new UdpEndpoint(address, port));
    }

    public void unregisterEndpoint(int userId) { this.endpoints.remove(userId); }

    /**
     * Broadcasts a rich round-ended notification.
     * 
     * @param prevGameId game that ended
     * @param nextGameId game that started
     * @param roundNumber new round number
     * @param solution words grouped by theme
     * @param participants total players
     * @param won total winners
     * @param avgScore average score
     * @param nextWords words for the new game
     */
    public void broadcastRoundEnded(
        int prevGameId,
        int nextGameId,
        long roundNumber,
        List<List<String>> solution,
        int participants,
        int won,
        float avgScore,
        List<String> nextWords
    ) {
        RoundEndedNotification payload = new RoundEndedNotification(
            prevGameId, nextGameId, roundNumber, solution, participants, won, avgScore, nextWords
        );
        
        String json = this.gson.toJson(payload);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        for (UdpEndpoint endpoint : this.endpoints.values()) {
            sendDatagram(bytes, endpoint);
        }
    }

    @Override
    public void close() { this.socket.close(); }

    private void sendDatagram(byte[] bytes, UdpEndpoint endpoint) {
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, endpoint.address, endpoint.port);
        try { this.socket.send(packet); }
        catch (IOException ex) {
            System.err.println("UDP Error: " + ex.getMessage());
        }
    }

    private static record UdpEndpoint(InetAddress address, int port) {}

    private static class RoundEndedNotification {
        @SerializedName("type") String type = "roundEnded";
        @SerializedName("prevGameId") int prevGameId;
        @SerializedName("nextGameId") int nextGameId;
        @SerializedName("roundNumber") long roundNumber;
        @SerializedName("solution") List<List<String>> solution;
        @SerializedName("participants") int participants;
        @SerializedName("won") int won;
        @SerializedName("avgScore") float avgScore;
        @SerializedName("nextWords") List<String> nextWords;

        RoundEndedNotification(int p, int n, long r, List<List<String>> s, int pt, int w, float a, List<String> nw) {
            this.prevGameId = p; this.nextGameId = n; this.roundNumber = r;
            this.solution = s; this.participants = pt; this.won = w; this.avgScore = a;
            this.nextWords = nw;
        }
    }
}
