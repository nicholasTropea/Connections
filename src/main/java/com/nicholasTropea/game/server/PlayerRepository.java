package com.nicholasTropea.game.server;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import com.nicholasTropea.game.model.Player;

/**
 * Persists and retrieves registered players from a JSON file.
 *
 * <p>Uses integer userId as primary key for stability across nickname changes.
 * All public methods are synchronized to ensure thread-safe access when
 * multiple client handlers operate concurrently.
 */
public class PlayerRepository {
    /** JSON serializer/deserializer for persistence. */
    private final Gson gson;

    /** File path used for persistent storage. */
    private final Path storageFile;

    /** In-memory player index by userId. */
    private final Map<Integer, Player> playersById;

    /** Secondary index: nickname -> userId for lookups. */
    private final Map<String, Integer> nicknameToUserId;

    /** Next userId to assign on registration. */
    private int nextUserId;




    /**
     * Creates a new repository backed by the provided file path.
     *
     * @param storagePath path to the JSON storage file
     */
    public PlayerRepository(String storagePath) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.storageFile = Path.of(storagePath);
        this.playersById = new HashMap<>();
        this.nicknameToUserId = new HashMap<>();
        this.nextUserId = 1;
        loadPlayers();
    }


    /**
     * Registers a new player with auto-assigned userId.
     *
     * @param username account username
     * @param password account password
     * @return null on success, otherwise an error message
     */
    public synchronized String registerPlayer(String username, String password) {
        if (isBlank(username) || isBlank(password)) { return "invalid credentials"; }

        if (this.nicknameToUserId.containsKey(username)) {
            return "username already registered";
        }

        int userId = this.nextUserId++;
        Player newPlayer = new Player(userId, username, password);
        this.playersById.put(userId, newPlayer);
        this.nicknameToUserId.put(username, userId);
        persistPlayers();

        return null;
    }


    /**
     * Validates credentials for login.
     *
     * @param username account username
     * @param password account password
     * @return null if credentials are valid, otherwise an error message
     */
    public synchronized String validateLogin(String username, String password) {
        Integer userId = this.nicknameToUserId.get(username);
        if (userId == null) { return "username not found"; }

        Player player = this.playersById.get(userId);
        if (player == null) { return "username not found"; }

        if (!player.getPassword().equals(password)) { return "incorrect password"; }

        return null;
    }


    /**
     * Gets player by username for internal use.
     *
     * @param username player nickname
     * @return Player object or null if not found
     */
    public synchronized Player getPlayerByUsername(String username) {
        Integer userId = this.nicknameToUserId.get(username);
        return userId != null ? this.playersById.get(userId) : null;
    }


    /**
     * Gets player by userId.
     *
     * @param userId player identifier
     * @return Player object or null if not found
     */
    public synchronized Player getPlayerById(int userId) {
        return this.playersById.get(userId);
    }


    /**
     * Gets a snapshot list of all registered players.
     *
     * @return list containing all players at call time
     */
    public synchronized List<Player> getAllPlayers() {
        return new ArrayList<>(this.playersById.values());
    }


    /**
     * Updates account credentials after verifying old credentials.
     *
     * @param oldUsername current username
     * @param oldPassword current password
     * @param newUsername new username (blank means unchanged)
     * @param newPassword new password (blank means unchanged)
     * @return null on success, otherwise an error message
     */
    public synchronized String updateCredentials(
        String oldUsername,
        String oldPassword,
        String newUsername,
        String newPassword
    ) {
        Integer userId = this.nicknameToUserId.get(oldUsername);
        if (userId == null) { return "user not found"; }

        Player player = this.playersById.get(userId);
        if (player == null) { return "user not found"; }

        if (!player.getPassword().equals(oldPassword)) { return "oldPsw not valid"; }

        boolean hasNewUsername = !isBlank(newUsername);
        boolean hasNewPassword = !isBlank(newPassword);
        if (!hasNewUsername && !hasNewPassword) {
            return "Either password, name or both must change";
        }

        if (
            hasNewUsername
            && !oldUsername.equals(newUsername)
            && this.nicknameToUserId.containsKey(newUsername)
        ) {
            return "newName already registered, choose a different one";
        }

        if (hasNewUsername && !oldUsername.equals(newUsername)) {
            this.nicknameToUserId.remove(oldUsername);
            player.setNickname(newUsername);
            this.nicknameToUserId.put(newUsername, userId);
        }

        if (hasNewPassword) { player.setPassword(newPassword); }

        persistPlayers();

        return null;
    }


    /** JSON storage structure for persisting repository state. */
    private static class StorageData {
        @SerializedName("nextUserId")
        int nextUserId;

        @SerializedName("players")
        Map<Integer, Player> players;

        StorageData(int nextUserId, Map<Integer, Player> players) {
            this.nextUserId = nextUserId;
            this.players = players;
        }
    }


    /** Loads players from persistent storage into memory. */
    private void loadPlayers() {
        try {
            if (!Files.exists(this.storageFile)) {
                Path parent = this.storageFile.getParent();
                if (parent != null) { Files.createDirectories(parent); }
                return;
            }

            String content = Files.readString(this.storageFile, StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) { return; }

            StorageData data = this.gson.fromJson(content, StorageData.class);
            if (data == null || data.players == null) { return; }

            this.nextUserId = data.nextUserId;
            this.playersById.putAll(data.players);

            // Rebuild nickname index
            for (Player player : data.players.values()) {
                this.nicknameToUserId.put(player.getNickname(), player.getUserId());
            }
        }
        catch (IOException | RuntimeException ex) {
            System.err.println("Failed to load players: " + ex.getMessage());
        }
    }


    /** Persists current players map to disk atomically. */
    private void persistPlayers() {
        try {
            Path parent = this.storageFile.getParent();
            if (parent != null) { Files.createDirectories(parent); }

            Path tempFile = this.storageFile.resolveSibling(
                this.storageFile.getFileName() + ".tmp"
            );

            StorageData data = new StorageData(this.nextUserId, this.playersById);
            String json = this.gson.toJson(data);
            Files.writeString(tempFile, json, StandardCharsets.UTF_8);
            Files.move(
                tempFile,
                this.storageFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            );
        }
        catch (IOException ex) {
            throw new IllegalStateException(
                "Could not persist players to storage",
                ex
            );
        }
    }


    /**
     * Checks whether the provided text is null or blank.
     *
     * @param value value to check
     * @return true if null or blank, false otherwise
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
