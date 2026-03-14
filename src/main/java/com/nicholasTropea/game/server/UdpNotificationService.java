package com.nicholasTropea.game.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/** Sends asynchronous UDP notifications to subscribed clients. */
public class UdpNotificationService implements AutoCloseable {
    /** UDP socket used for outbound notification datagrams. */
    private final DatagramSocket socket;

    /** JSON serializer for notification payloads. */
    private final Gson gson;

    /** Subscribed user endpoints by userId. */
    private final Map<Integer, UdpEndpoint> endpoints;


    /** Creates a new UDP notification service. */
    public UdpNotificationService() {
        try { this.socket = new DatagramSocket(); }
        catch (IOException ex) {
            throw new IllegalStateException("Cannot create UDP socket", ex);
        }

        this.gson = new Gson();
        this.endpoints = new ConcurrentHashMap<>();
    }


    /**
     * Registers/updates the UDP endpoint for a user.
     *
     * @param userId user identifier
     * @param address client address
     * @param port client UDP port
     */
    public void registerEndpoint(int userId, InetAddress address, int port) {
        this.endpoints.put(userId, new UdpEndpoint(address, port));
    }


    /**
     * Removes UDP subscription for a user.
     *
     * @param userId user identifier
     */
    public void unregisterEndpoint(int userId) { this.endpoints.remove(userId); }


    /**
     * Broadcasts a round-ended notification to all subscribed users.
     *
     * @param previousGameId game that ended
     * @param nextGameId game that started
     * @param roundNumber newly active round number
     */
    public void broadcastRoundEnded(
        int previousGameId,
        int nextGameId,
        long roundNumber
    ) {
        RoundEndedNotification payload = new RoundEndedNotification(
            previousGameId,
            nextGameId,
            roundNumber
        );
        
        String json = this.gson.toJson(payload);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        for (UdpEndpoint endpoint : this.endpoints.values()) {
            sendDatagram(bytes, endpoint);
        }
    }


    /** Closes underlying UDP socket. */
    @Override
    public void close() { this.socket.close(); }


    /**
     * Sends a UDP datagram to one endpoint.
     *
     * @param bytes payload bytes
     * @param endpoint target endpoint
     */
    private void sendDatagram(byte[] bytes, UdpEndpoint endpoint) {
        DatagramPacket packet = new DatagramPacket(
            bytes,
            bytes.length,
            endpoint.address,
            endpoint.port
        );

        try { this.socket.send(packet); }
        catch (IOException ex) {
            System.err.println(
                "Failed to send UDP notification to "
                + endpoint.address
                + ":"
                + endpoint.port
                + " - "
                + ex.getMessage()
            );
        }
    }


    /** UDP endpoint descriptor. */
    private static final class UdpEndpoint {
        private final InetAddress address;
        private final int port;


        private UdpEndpoint(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
    }


    /** Payload for asynchronous round-ended notifications. */
    private static final class RoundEndedNotification {
        @SerializedName("type")
        private final String type;

        @SerializedName("previousGameId")
        private final int previousGameId;

        @SerializedName("nextGameId")
        private final int nextGameId;

        @SerializedName("roundNumber")
        private final long roundNumber;


        private RoundEndedNotification(
            int previousGameId,
            int nextGameId,
            long roundNumber
        ) {
            this.type = "roundEnded";
            this.previousGameId = previousGameId;
            this.nextGameId = nextGameId;
            this.roundNumber = roundNumber;
        }
    }
}
