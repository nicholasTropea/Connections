package com.nicholasTropea.game.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/** Persists and restores the global active round state. */
public class GameRoundStateRepository {
    /** Default path used for persistent round-state storage. */
    public static final String DEFAULT_STORAGE_PATH =
        "src/main/resources/data/gameRoundState.json";

    /** JSON serializer/deserializer. */
    private final Gson gson;

    /** Storage file location. */
    private final Path storageFile;


    /** Creates repository with default storage path. */
    public GameRoundStateRepository() { this(DEFAULT_STORAGE_PATH); }


    /**
     * Creates repository with explicit storage path.
     *
     * @param storagePath JSON file path
     */
    public GameRoundStateRepository(String storagePath) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.storageFile = Path.of(storagePath);
    }


    /**
     * Loads persisted global round state.
     *
     * @return snapshot, or null when file is absent/empty/invalid
     */
    public synchronized GameRoundCoordinator.RoundStateSnapshot loadSnapshot() {
        try {
            if (!Files.exists(this.storageFile)) {
                Path parent = this.storageFile.getParent();
                if (parent != null) { Files.createDirectories(parent); }
                return null;
            }

            String content = Files.readString(this.storageFile, StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) { return null; }

            StorageData data = this.gson.fromJson(content, StorageData.class);
            if (data == null) { return null; }

            return new GameRoundCoordinator.RoundStateSnapshot(
                data.currentGameId,
                data.roundNumber,
                data.remainingTimeMillis
            );
        }
        catch (IOException | RuntimeException ex) {
            System.err.println("Failed to load game round state: " + ex.getMessage());
            return null;
        }
    }


    /**
     * Persists provided round-state snapshot atomically.
     *
     * @param snapshot round state snapshot to store
     */
    public synchronized void persistSnapshot(
        GameRoundCoordinator.RoundStateSnapshot snapshot
    ) {
        if (snapshot == null) { return; }

        try {
            Path parent = this.storageFile.getParent();
            if (parent != null) { Files.createDirectories(parent); }

            Path tempFile = this.storageFile.resolveSibling(
                this.storageFile.getFileName() + ".tmp"
            );

            StorageData data = new StorageData(
                snapshot.getCurrentGameId(),
                snapshot.getRoundNumber(),
                snapshot.getRemainingTimeMillis()
            );

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
            throw new IllegalStateException("Could not persist game round state", ex);
        }
    }


    /** Root JSON structure for storage format. */
    private static final class StorageData {
        @SerializedName("currentGameId")
        private final int currentGameId;

        @SerializedName("roundNumber")
        private final long roundNumber;

        @SerializedName("remainingTimeMillis")
        private final long remainingTimeMillis;


        private StorageData(
            int currentGameId,
            long roundNumber,
            long remainingTimeMillis
        ) {
            this.currentGameId = currentGameId;
            this.roundNumber = roundNumber;
            this.remainingTimeMillis = remainingTimeMillis;
        }
    }
}
