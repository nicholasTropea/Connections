package com.nicholasTropea.game.server;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.nicholasTropea.game.model.Game;

/**
 * Coordinates the globally active game round lifecycle.
 *
 * <p>At any moment there is exactly one active global game. The coordinator
 * rotates to the next game after a fixed duration.
 */
public class GameRoundCoordinator implements AutoCloseable {
    /** Immutable snapshot of global round state for persistence. */
    public static final class RoundStateSnapshot {
        private final int currentGameId;
        private final long roundNumber;
        private final long remainingTimeMillis;


        /**
         * Creates a round-state snapshot.
         *
         * @param currentGameId currently active game id
         * @param roundNumber global round number
         * @param remainingTimeMillis remaining time for current round
         */
        public RoundStateSnapshot(
            int currentGameId,
            long roundNumber,
            long remainingTimeMillis
        ) {
            this.currentGameId = currentGameId;
            this.roundNumber = roundNumber;
            this.remainingTimeMillis = remainingTimeMillis;
        }


        /** @return currently active game id */
        public int getCurrentGameId() { return this.currentGameId; }

        /** @return global round number */
        public long getRoundNumber() { return this.roundNumber; }

        /** @return remaining time for current round in milliseconds */
        public long getRemainingTimeMillis() { return this.remainingTimeMillis; }
    }


    /** Listener for round transitions. */
    @FunctionalInterface
    public interface RoundTransitionListener {
        /**
         * Called when round rotates from one game to another.
         *
         * @param previousGameId game id that just ended
         * @param nextGameId game id that became active
         * @param roundNumber new round number
         */
        void onRoundTransition(int previousGameId, int nextGameId, long roundNumber);
    }


    /** Polling interval used to check whether the round expired. */
    private static final long ROTATION_CHECK_INTERVAL_MS = 1000L;

    /** Repository containing all available game definitions. */
    private final GameRepository gameRepository;

    /** Sorted game IDs used for deterministic round rotation. */
    private final List<Integer> gameIds;

    /** Duration of one global round in milliseconds. */
    private final long roundDurationMillis;

    /** Lock guarding round state transitions. */
    private final Object lock;

    /** Background scheduler that rotates rounds when expired. */
    private final ScheduledExecutorService scheduler;

    /** Index of the active game in {@code gameIds}. */
    private int currentGameIndex;

    /** End timestamp (epoch ms) of current round. */
    private long currentRoundEndMillis;

    /** Round counter. */
    private long roundNumber;

    /** Registered listeners notified on round rotation. */
    private final List<RoundTransitionListener> listeners;


    /**
     * Creates and starts a new coordinator.
     *
     * @param gameRepository repository with preloaded games
     * @param roundDurationMillis global round duration in milliseconds
     */
    public GameRoundCoordinator(
        GameRepository gameRepository,
        long roundDurationMillis
    ) {
        this(gameRepository, roundDurationMillis, null);
    }


    /**
     * Creates and starts a new coordinator with optional restored state.
     *
     * @param gameRepository repository with preloaded games
     * @param roundDurationMillis global round duration in milliseconds
     * @param initialSnapshot optional snapshot to restore from
     */
    public GameRoundCoordinator(
        GameRepository gameRepository,
        long roundDurationMillis,
        RoundStateSnapshot initialSnapshot
    ) {
        this.gameRepository = Objects.requireNonNull(
            gameRepository,
            "gameRepository is required"
        );
        this.gameIds = this.gameRepository.getGameIds();
        if (this.gameIds.isEmpty()) {
            throw new IllegalStateException("At least one game is required");
        }

        if (roundDurationMillis <= 0L) {
            throw new IllegalArgumentException("roundDurationMillis must be > 0");
        }

        this.roundDurationMillis = roundDurationMillis;
        this.lock = new Object();
        this.listeners = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "Game round coordinator");
                thread.setDaemon(true);
                return thread;
            }
        );

        long now = System.currentTimeMillis();
        this.currentGameIndex = 0;
        this.currentRoundEndMillis = now + this.roundDurationMillis;
        this.roundNumber = 1L;

        restoreFromSnapshotIfValid(initialSnapshot, now);

        this.scheduler.scheduleAtFixedRate(
            this::rotateIfExpiredSafely,
            ROTATION_CHECK_INTERVAL_MS,
            ROTATION_CHECK_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }


    /**
     * Exports current global round state for persistence.
     *
     * @return snapshot of active game and remaining round time
     */
    public RoundStateSnapshot exportSnapshot() {
        synchronized (this.lock) {
            long now = System.currentTimeMillis();
            rotateIfExpiredLocked(now);

            return new RoundStateSnapshot(
                this.gameIds.get(this.currentGameIndex),
                this.roundNumber,
                Math.max(0L, this.currentRoundEndMillis - now)
            );
        }
    }


    /**
     * Gets the currently active game identifier.
     *
     * @return active game ID
     */
    public int getCurrentGameId() {
        synchronized (this.lock) {
            rotateIfExpiredLocked(System.currentTimeMillis());
            return this.gameIds.get(this.currentGameIndex);
        }
    }


    /**
     * Gets the currently active game definition.
     *
     * @return active game
     */
    public Game getCurrentGame() {
        int gameId = getCurrentGameId();
        Game game = this.gameRepository.getGameById(gameId);
        if (game == null) {
            throw new IllegalStateException("Active game not found: " + gameId);
        }

        return game;
    }


    /**
     * Gets the remaining time for the current global round.
     *
     * @return remaining time in milliseconds
     */
    public long getRemainingTimeMillis() {
        synchronized (this.lock) {
            long now = System.currentTimeMillis();
            rotateIfExpiredLocked(now);
            return Math.max(0L, this.currentRoundEndMillis - now);
        }
    }


    /**
     * Checks whether the provided gameId is the currently active round game.
     *
     * @param gameId game identifier to verify
     * @return true if gameId belongs to current round
     */
    public boolean isCurrentGame(int gameId) { return getCurrentGameId() == gameId; }


    /**
     * Gets current round number.
     *
     * @return current round number
     */
    public long getRoundNumber() {
        synchronized (this.lock) {
            rotateIfExpiredLocked(System.currentTimeMillis());
            return this.roundNumber;
        }
    }


    /**
     * Registers a listener for round transition events.
     *
     * @param listener listener to register
     */
    public void addRoundTransitionListener(RoundTransitionListener listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener is required"));
    }


    /**
     * Unregisters a listener for round transition events.
     *
     * @param listener listener to remove
     */
    public void removeRoundTransitionListener(RoundTransitionListener listener) {
        this.listeners.remove(listener);
    }


    /** Stops the coordinator scheduler. */
    @Override
    public void close() { this.scheduler.shutdownNow(); }


    /** Wraps rotation checks to avoid scheduler suppression on runtime errors. */
    private void rotateIfExpiredSafely() {
        try {
            synchronized (this.lock) {
                rotateIfExpiredLocked(System.currentTimeMillis());
            }
        }
        catch (RuntimeException ex) {
            System.err.println("Round rotation error: " + ex.getMessage());
        }
    }


    /**
     * Rotates to next game if current round has expired.
     *
     * @param now current epoch milliseconds
     */
    private void rotateIfExpiredLocked(long now) {
        if (now < this.currentRoundEndMillis) { return; }

        int previousGameId = this.gameIds.get(this.currentGameIndex);
        this.currentGameIndex = (this.currentGameIndex + 1) % this.gameIds.size();
        this.currentRoundEndMillis = now + this.roundDurationMillis;
        this.roundNumber++;

        int nextGameId = this.gameIds.get(this.currentGameIndex);
        System.out.println(
            "Global round rotated to gameId="
            + nextGameId
            + " round="
            + this.roundNumber
        );

        notifyRoundTransition(previousGameId, nextGameId, this.roundNumber);
    }


    /**
     * Notifies listeners that a round transition occurred.
     *
     * @param previousGameId game id that just ended
     * @param nextGameId new active game id
     * @param roundNumber current round number
     */
    private void notifyRoundTransition(
        int previousGameId,
        int nextGameId,
        long roundNumber
    ) {
        for (RoundTransitionListener listener : this.listeners) {
            try {
                listener.onRoundTransition(previousGameId, nextGameId, roundNumber);
            }
            catch (RuntimeException ex) {
                System.err.println(
                    "Round transition listener error: " + ex.getMessage()
                );
            }
        }
    }


    /**
     * Restores coordinator state from snapshot when compatible with game list.
     *
     * @param snapshot snapshot loaded from persistent storage
     * @param now current epoch milliseconds
     */
    private void restoreFromSnapshotIfValid(RoundStateSnapshot snapshot, long now) {
        if (snapshot == null) { return; }

        int restoredIndex = this.gameIds.indexOf(snapshot.getCurrentGameId());
        if (restoredIndex < 0) {
            System.err.println(
                "Ignoring persisted round state: gameId not found "
                + snapshot.getCurrentGameId()
            );
            
            return;
        }

        long restoredRound = snapshot.getRoundNumber();
        long restoredRemaining = snapshot.getRemainingTimeMillis();

        if (restoredRound < 1L || restoredRemaining <= 0L) {
            System.err.println("Ignoring persisted round state: invalid values");
            return;
        }

        long boundedRemaining = Math.min(restoredRemaining, this.roundDurationMillis);
        this.currentGameIndex = restoredIndex;
        this.roundNumber = restoredRound;
        this.currentRoundEndMillis = now + boundedRemaining;
    }
}
