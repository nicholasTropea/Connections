package com.nicholasTropea.game.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/** Persists and restores historical session/game states. */
public class SessionStateRepository {
    /** Default path used for persistent session-state storage. */
    public static final String DEFAULT_STORAGE_PATH =
        "src/main/resources/data/gameStates.json";

    /** JSON serializer/deserializer. */
    private final Gson gson;

    /** Storage file location. */
    private final Path storageFile;


    /** Creates repository with default storage path. */
    public SessionStateRepository() { this(DEFAULT_STORAGE_PATH); }


    /**
     * Creates repository with explicit storage path.
     *
     * @param storagePath JSON file path
     */
    public SessionStateRepository(String storagePath) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.storageFile = Path.of(storagePath);
    }


    /**
     * Loads persisted snapshots from storage.
     *
     * @return list of snapshots, empty when file is absent/empty
     */
    public synchronized List<SessionManager.GameStateSnapshot> loadSnapshots() {
        try {
            if (!Files.exists(this.storageFile)) {
                Path parent = this.storageFile.getParent();
                if (parent != null) { Files.createDirectories(parent); }
                
                return List.of();
            }

            String content = Files.readString(this.storageFile, StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) { return List.of(); }

            StorageData data = this.gson.fromJson(content, StorageData.class);
            if (data == null || data.snapshots == null) { return List.of(); }

            return new ArrayList<>(data.snapshots);
        }
        catch (IOException | RuntimeException ex) {
            System.err.println("Failed to load game states: " + ex.getMessage());
            return List.of();
        }
    }


    /**
     * Persists provided snapshots atomically.
     *
     * @param snapshots session snapshots to store
     */
    public synchronized void persistSnapshots(
        List<SessionManager.GameStateSnapshot> snapshots
    ) {
        try {
            Path parent = this.storageFile.getParent();
            if (parent != null) { Files.createDirectories(parent); }

            Path tempFile = this.storageFile.resolveSibling(
                this.storageFile.getFileName() + ".tmp"
            );

            StorageData data = new StorageData(snapshots);
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
            throw new IllegalStateException("Could not persist game states", ex);
        }
    }


    /** Root JSON structure for storage format. */ 
    private static final class StorageData {
        @SerializedName("snapshots")
        private final List<SessionManager.GameStateSnapshot> snapshots;


        private StorageData(List<SessionManager.GameStateSnapshot> snapshots) {
            this.snapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
        }
    }
}
