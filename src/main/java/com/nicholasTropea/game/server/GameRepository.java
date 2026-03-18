package com.nicholasTropea.game.server;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nicholasTropea.game.model.Game;

/**
 * Loads and provides access to game definitions from JSON storage.
 */
public class GameRepository {
    /** Parsed games indexed by gameId for O(1) lookup. */
    private final Map<Integer, Game> gamesById;

    /** Sorted game ids to allow deterministic selection by index. */
    private final List<Integer> gameIds;


    /**
     * Creates a game repository loading from the provided file path.
     *
     * @param gamesPath JSON file path containing game definitions
     */
    public GameRepository(String gamesPath) {
        this.gamesById = new HashMap<>();
        this.gameIds = new ArrayList<>();
        loadGames(Path.of(gamesPath));
    }


    /**
     * Returns a game by its identifier.
     *
     * @param gameId game identifier
     * @return game instance, or null if not found
     */
    public Game getGameById(int gameId) {
        return this.gamesById.get(gameId);
    }


    /**
     * Returns the total number of loaded games.
     *
     * @return total loaded games
     */
    public int size() { return this.gamesById.size(); }


    /**
     * Returns an immutable copy of all loaded game ids.
     *
     * @return list of game ids
     */
    public List<Integer> getGameIds() { return List.copyOf(this.gameIds); }


    /**
     * Returns game identifier by index position in the sorted game-id list.
     *
     * @param index zero-based index in available game ids
     * @return game identifier
     * @throws IllegalArgumentException if index is out of bounds
     */
    public int getGameIdByIndex(int index) {
        if (index < 0 || index >= this.gameIds.size()) {
            throw new IllegalArgumentException("index out of range");
        }

        return this.gameIds.get(index);
    }


    /**
     * Loads and validates games from the provided JSON path.
     *
     * @param gamesPath games JSON path
     */
    private void loadGames(Path gamesPath) {
        try {
            String json = Files.readString(gamesPath, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<Game>>() { }.getType();
            List<Game> loadedGames = new Gson().fromJson(json, listType);

            if (loadedGames == null || loadedGames.isEmpty()) {
                throw new IllegalStateException("No games found in games.json");
            }

            for (Game game : loadedGames) {
                validateGame(game);

                int gameId = game.getId();
                if (this.gamesById.containsKey(gameId)) {
                    throw new IllegalStateException("Duplicate gameId found: " + gameId);
                }

                this.gamesById.put(gameId, game);
            }

            this.gameIds.addAll(this.gamesById.keySet());
            Collections.sort(this.gameIds);
        }
        catch (IOException ex) {
            throw new IllegalStateException(
                "Failed to read games.json from path: " + gamesPath,
                ex
            );
        }
        catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to parse games.json", ex);
        }
    }


    /**
     * Validates a single game data structure.
     *
     * @param game game to validate
     */
    private static void validateGame(Game game) {
        Objects.requireNonNull(game, "game cannot be null");
        if (game.getId() < 0) {
            throw new IllegalStateException("gameId must be non-negative");
        }

        List<Game.Group> groups = Objects.requireNonNull(game.getGroups());
        if (groups.size() != 4) {
            throw new IllegalStateException("Each game must contain exactly 4 groups");
        }

        Set<String> allWords = new HashSet<>();
        for (Game.Group group : groups) {
            Objects.requireNonNull(group, "group cannot be null");
            if (group.getTheme() == null || group.getTheme().trim().isEmpty()) {
                throw new IllegalStateException("Group theme cannot be null or blank");
            }

            List<String> words = Objects.requireNonNull(group.getWords());
            if (words.size() != 4) {
                throw new IllegalStateException("Each group must contain exactly 4 words");
            }

            for (String word : words) {
                if (word == null || word.trim().isEmpty()) {
                    throw new IllegalStateException("Word cannot be null or blank");
                }
                allWords.add(word);
            }
        }

        if (allWords.size() != 16) {
            throw new IllegalStateException(
                "Each game must contain 16 unique words"
            );
        }
    }
}
