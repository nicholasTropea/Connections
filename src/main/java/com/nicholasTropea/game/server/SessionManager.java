package com.nicholasTropea.game.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import com.nicholasTropea.game.model.PlayerGameState;

/**
 * Manages active player game sessions in a thread-safe manner.
 *
 * <p>Each active session maps a userId to their current game state.
 * Sessions are created on login and removed on logout or disconnect.
 */
public class SessionManager {
    /** Immutable persisted snapshot for one user-game state entry. */
    public static final class GameStateSnapshot {
        private final int userId;
        private final int gameId;
        private final int correctProposals;
        private final int errorCount;
        private final int score;
        private final List<String> remainingWords;
        private final List<List<String>> guessedGroups;
        private final PlayerGameState.GameResult finalState;


        /**
         * Creates a state snapshot.
         *
         * @param userId player user identifier
         * @param gameId game identifier
         * @param correctProposals number of correct proposals
         * @param errorCount number of wrong proposals
         * @param score game score
         * @param remainingWords words not yet grouped
         * @param finalState final game state
         */
        public GameStateSnapshot(
            int userId,
            int gameId,
            int correctProposals,
            int errorCount,
            int score,
            List<String> remainingWords,
            List<List<String>> guessedGroups,
            PlayerGameState.GameResult finalState
        ) {
            this.userId = userId;
            this.gameId = gameId;
            this.correctProposals = correctProposals;
            this.errorCount = errorCount;
            this.score = score;
            this.remainingWords = remainingWords == null
                ? List.of()
                : List.copyOf(remainingWords);
            this.guessedGroups = new ArrayList<>();
            if (guessedGroups != null) {
                for (List<String> group : guessedGroups) {
                    if (group != null) {
                        this.guessedGroups.add(List.copyOf(group));
                    }
                }
            }
            this.finalState = finalState;
        }


        /** @return player user identifier */
        public int getUserId() { return this.userId; }

        /** @return game identifier */
        public int getGameId() { return this.gameId; }

        /** @return number of correct proposals */
        public int getCorrectProposals() { return this.correctProposals; }

        /** @return number of wrong proposals */
        public int getErrorCount() { return this.errorCount; }

        /** @return stored score */
        public int getScore() { return this.score; }

        /** @return snapshot remaining words */
        public List<String> getRemainingWords() {
            return List.copyOf(this.remainingWords);
        }


        /** @return snapshot guessed groups */
        public List<List<String>> getGuessedGroups() {
            List<List<String>> copy = new ArrayList<>();
            if (this.guessedGroups != null) {
                for (List<String> group : this.guessedGroups) {
                    if (group != null) {
                        copy.add(List.copyOf(group));
                    }
                }
            }
            return List.copyOf(copy);
        }

        /** @return final state */
        public PlayerGameState.GameResult getFinalState() { return this.finalState; }
    }


    /** Immutable aggregate statistics for a specific game. */
    public static final class GameAggregates {
        private final int participants;
        private final int activePlayers;
        private final int finishedPlayers;
        private final int wonPlayers;
        private final float averageScore;


        private GameAggregates(
            int participants,
            int activePlayers,
            int finishedPlayers,
            int wonPlayers,
            float averageScore
        ) {
            this.participants = participants;
            this.activePlayers = activePlayers;
            this.finishedPlayers = finishedPlayers;
            this.wonPlayers = wonPlayers;
            this.averageScore = averageScore;
        }


        /** @return number of players with a state for the game */
        public int getParticipants() { return this.participants; }

        /** @return players that have not finished this game */
        public int getActivePlayers() { return this.activePlayers; }

        /** @return players that finished this game */
        public int getFinishedPlayers() { return this.finishedPlayers; }

        /** @return players that finished this game with a win */
        public int getWonPlayers() { return this.wonPlayers; }

        /** @return average score across all participants */
        public float getAverageScore() { return this.averageScore; }
    }


    /** Active sessions indexed by userId. */
    private final Map<Integer, PlayerGameState> activeSessions;

    /**
     * All known game states per user, keyed by gameId.
     *
     * <p>This allows restoring state when a player logs out and logs in again
     * during the same global round.
     */
    private final Map<Integer, Map<Integer, PlayerGameState>> userGameStates;


    /**
     * Creates a new session manager.
     */
    public SessionManager() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.userGameStates = new ConcurrentHashMap<>();
    }


    /**
     * Opens a session for the current global game.
     *
     * <p>If a state for {@code currentGameId} already exists for this user,
     * that state is restored. Otherwise a new state is created.
     *
     * @param userId player user identifier
     * @param currentGameId current global game id
     * @param initialWords current game words used for first-time state creation
     * @return error message if user already logged in, null on success
     */
    public synchronized String openSessionForCurrentGame(
        int userId,
        int currentGameId,
        List<String> initialWords
    ) {
        if (this.activeSessions.containsKey(userId)) {
            return "connection already logged in";
        }

        PlayerGameState state = getOrCreateState(userId, currentGameId, initialWords);
        this.activeSessions.put(userId, state);
        return null;
    }


    /**
     * Gets the game state for an active session.
     *
     * @param userId player user identifier
     * @return game state or null if no active session
     */
    public synchronized PlayerGameState getSession(int userId) {
        return this.activeSessions.get(userId);
    }


    /**
     * Gets the state for a specific user/game pair.
     *
     * @param userId player user identifier
     * @param gameId game identifier
     * @return state for that game, or null if absent
     */
    public synchronized PlayerGameState getStateForGame(int userId, int gameId) {
        Map<Integer, PlayerGameState> statesByGame = this.userGameStates.get(userId);
        if (statesByGame == null) { return null; }

        return statesByGame.get(gameId);
    }


    /**
     * Returns all known states for a player ordered by ascending gameId.
     *
     * @param userId player user identifier
     * @return ordered list of game states, empty if none exist
     */
    public synchronized List<PlayerGameState> getStatesForUser(int userId) {
        Map<Integer, PlayerGameState> statesByGame = this.userGameStates.get(userId);
        if (statesByGame == null || statesByGame.isEmpty()) { return List.of(); }

        List<PlayerGameState> states = new ArrayList<>(statesByGame.values());
        states.sort(Comparator.comparingInt(PlayerGameState::getGameId));
        return states;
    }


    /**
     * Ensures the user is bound to the current global game state.
     *
     * <p>If the user is logged in and the round changed, the active session is
     * moved to the state of {@code currentGameId}. Existing state is restored
     * if present; otherwise a new one is created.
     *
     * @param userId player user identifier
     * @param currentGameId current global game id
     * @param initialWords words of current game for first-time state creation
     * @return active state for current game, or null if user is not logged in
     */
    public synchronized PlayerGameState ensureCurrentGameSession(
        int userId,
        int currentGameId,
        List<String> initialWords
    ) {
        PlayerGameState active = this.activeSessions.get(userId);
        if (active == null) { return null; }

        if (active.getGameId() == currentGameId) { return active; }

        PlayerGameState updated = getOrCreateState(userId, currentGameId, initialWords);
        this.activeSessions.put(userId, updated);
        
        return updated;
    }


    /**
     * Removes an active session.
     *
     * @param userId player user identifier
     */
    public synchronized void removeSession(int userId) {
        this.activeSessions.remove(userId);
    }


    /**
     * Checks if a user has an active session.
     *
     * @param userId player user identifier
     * @return true if session exists
     */
    public synchronized boolean hasSession(int userId) {
        return this.activeSessions.containsKey(userId);
    }


    /**
     * Gets the total number of active sessions.
     *
     * @return active session count
     */
    public int getActiveSessionCount() {
        return this.activeSessions.size();
    }


    /**
     * Computes aggregate statistics for the provided game.
     *
     * @param gameId game identifier
     * @return immutable aggregate snapshot for that game
     */
    public synchronized GameAggregates getGameAggregates(int gameId) {
        int participants = 0;
        int activePlayers = 0;
        int finishedPlayers = 0;
        int wonPlayers = 0;
        int totalScore = 0;

        // Map of played games of stored players
        for (Map<Integer, PlayerGameState> statesByGame : this.userGameStates.values()) {
            PlayerGameState state = statesByGame.get(gameId);
            if (state == null) { continue; } // Check if he played gameId game

            participants++;
            totalScore += state.getScore();

            if (state.isFinished()) { finishedPlayers++; }
            else { activePlayers++; }

            if (state.getFinalState() == PlayerGameState.GameResult.WON) { wonPlayers++; }
        }

        float averageScore = participants == 0
            ? 0.0f
            : (float) totalScore / participants;

        return new GameAggregates(
            participants,
            activePlayers,
            finishedPlayers,
            wonPlayers,
            averageScore
        );
    }


    /**
     * Exports all known user-game states for persistence.
     *
     * @return immutable list of snapshots
     */
    public synchronized List<GameStateSnapshot> exportSnapshots() {
        List<GameStateSnapshot> snapshots = new ArrayList<>();

        for (Map<Integer, PlayerGameState> statesByGame : this.userGameStates.values()) {
            for (PlayerGameState state : statesByGame.values()) {
                snapshots.add(
                    new GameStateSnapshot(
                        state.getUserId(),
                        state.getGameId(),
                        state.getCorrectProposals(),
                        state.getErrorCount(),
                        state.getScore(),
                        state.getRemainingWords(),
                        state.getGuessedGroups(),
                        state.getFinalState()
                    )
                );
            }
        }

        snapshots.sort(
            Comparator
                .comparingInt(GameStateSnapshot::getUserId)
                .thenComparingInt(GameStateSnapshot::getGameId)
        );

        return List.copyOf(snapshots);
    }


    /**
     * Imports persisted snapshots and rebuilds in-memory state maps.
     *
     * @param snapshots snapshots loaded from persistent storage
     */
    public synchronized void importSnapshots(List<GameStateSnapshot> snapshots) {
        this.activeSessions.clear();
        this.userGameStates.clear();

        if (snapshots == null || snapshots.isEmpty()) { return; }

        for (GameStateSnapshot snapshot : snapshots) {
            List<String> remainingWords = snapshot.getRemainingWords();
            if (remainingWords == null) { remainingWords = List.of(); }

            PlayerGameState restored = new PlayerGameState(
                snapshot.getUserId(),
                snapshot.getGameId(),
                remainingWords
            );

            restored.restoreProgress(
                snapshot.getCorrectProposals(),
                snapshot.getErrorCount(),
                snapshot.getScore(),
                remainingWords,
                snapshot.getGuessedGroups(),
                snapshot.getFinalState()
            );

            Map<Integer, PlayerGameState> statesByGame = this.userGameStates
                .computeIfAbsent(snapshot.getUserId(), key -> new HashMap<>());

            statesByGame.put(snapshot.getGameId(), restored);
        }
    }


    /**
     * Gets an existing state or creates a new one for user/game pair.
     *
     * @param userId player user identifier
     * @param gameId game identifier
     * @param initialWords words to initialize newly created game state
     * @return existing or newly created player game state
     */
    private PlayerGameState getOrCreateState(
        int userId,
        int gameId,
        List<String> initialWords
    ) {
        Map<Integer, PlayerGameState> statesByGame =
            this.userGameStates.computeIfAbsent(userId, key -> new HashMap<>());

        PlayerGameState existing = statesByGame.get(gameId);
        if (existing != null) {return existing; }

        List<String> shuffledWords = new ArrayList<>(initialWords);
        Collections.shuffle(shuffledWords);

        PlayerGameState created = new PlayerGameState(userId, gameId, shuffledWords);
        statesByGame.put(gameId, created);

        return created;
    }
}
