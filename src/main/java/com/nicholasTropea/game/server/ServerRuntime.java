package com.nicholasTropea.game.server;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Aggregates shared server-side services and repositories.
 *
 * <p>This class centralizes long-lived dependencies and enables explicit
 * dependency injection into connection handlers.
 */
public class ServerRuntime implements AutoCloseable {
    /** Default global round duration: 5 minutes. */
    public static final long DEFAULT_ROUND_DURATION_MS = 300_000L;

    /** Session-state autosave period in seconds. */
    public static final long DEFAULT_SESSION_AUTOSAVE_SECONDS = 15L;

    /** Repository for player persistence and credential validation. */
    private final PlayerRepository playerRepository;

    /** Repository of game definitions loaded from JSON. */
    private final GameRepository gameRepository;

    /** In-memory manager of currently active client sessions. */
    private final SessionManager sessionManager;

    /** Coordinator for the global active game round lifecycle. */
    private final GameRoundCoordinator gameRoundCoordinator;

    /** Persistent storage for global round lifecycle state. */
    private final GameRoundStateRepository gameRoundStateRepository;

    /** Service used to push asynchronous UDP notifications. */
    private final UdpNotificationService udpNotificationService;

    /** Persistent storage for session/game state history. */
    private final SessionStateRepository sessionStateRepository;

    /** Scheduler for periodic autosave of session state. */
    private final ScheduledExecutorService persistenceScheduler;

    /** Autosave period in seconds loaded from configuration. */
    private final long sessionAutosaveSeconds;


    /** Creates a runtime with default dependencies and round duration. */
    public ServerRuntime() {
        this(
            new PlayerRepository(),
            new GameRepository(),
            new SessionManager(),
            DEFAULT_ROUND_DURATION_MS,
            DEFAULT_SESSION_AUTOSAVE_SECONDS
        );
    }


    /**
     * Creates a runtime with explicit dependencies.
     *
     * @param playerRepository player repository
     * @param gameRepository game repository
     * @param sessionManager session manager
     * @param roundDurationMillis global round duration in milliseconds
     */
    public ServerRuntime(
        PlayerRepository playerRepository,
        GameRepository gameRepository,
        SessionManager sessionManager,
        long roundDurationMillis
    ) {
        this(
            playerRepository,
            gameRepository,
            sessionManager,
            roundDurationMillis,
            DEFAULT_SESSION_AUTOSAVE_SECONDS
        );
    }


    /**
     * Creates a runtime with explicit dependencies and autosave configuration.
     *
     * @param playerRepository player repository
     * @param gameRepository game repository
     * @param sessionManager session manager
     * @param roundDurationMillis global round duration in milliseconds
     * @param sessionAutosaveSeconds autosave period in seconds
     */
    public ServerRuntime(
        PlayerRepository playerRepository,
        GameRepository gameRepository,
        SessionManager sessionManager,
        long roundDurationMillis,
        long sessionAutosaveSeconds
    ) {
        this.playerRepository = Objects.requireNonNull(
            playerRepository,
            "playerRepository is required"
        );
        this.gameRepository = Objects.requireNonNull(
            gameRepository,
            "gameRepository is required"
        );
        this.sessionManager = Objects.requireNonNull(
            sessionManager,
            "sessionManager is required"
        );
        this.gameRoundStateRepository = new GameRoundStateRepository();

        GameRoundCoordinator.RoundStateSnapshot roundStateSnapshot =
            this.gameRoundStateRepository.loadSnapshot();

        this.gameRoundCoordinator = new GameRoundCoordinator(
            this.gameRepository,
            roundDurationMillis,
            roundStateSnapshot
        );
        this.udpNotificationService = new UdpNotificationService();
        this.sessionStateRepository = new SessionStateRepository();
        this.sessionAutosaveSeconds = sessionAutosaveSeconds;
        this.persistenceScheduler = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "Session Persistence");
                thread.setDaemon(true);
                return thread;
            }
        );

        restoreSessionState();
        startSessionStateAutosave();

        this.gameRoundCoordinator.addRoundTransitionListener(
            (previousGameId, nextGameId, roundNumber) ->
                this.udpNotificationService.broadcastRoundEnded(
                    previousGameId,
                    nextGameId,
                    roundNumber
                )
        );
    }


    /** @return player repository */
    public PlayerRepository getPlayerRepository() { return this.playerRepository; }


    /** @return game repository */
    public GameRepository getGameRepository() { return this.gameRepository; }


    /** @return session manager */
    public SessionManager getSessionManager() { return this.sessionManager; }


    /** @return game round coordinator */
    public GameRoundCoordinator getGameRoundCoordinator() {
        return this.gameRoundCoordinator;
    }


    /** @return UDP notification service */
    public UdpNotificationService getUdpNotificationService() {
        return this.udpNotificationService;
    }


    /** Restores persisted session state into SessionManager. */
    private void restoreSessionState() {
        List<SessionManager.GameStateSnapshot> snapshots =
            this.sessionStateRepository.loadSnapshots();

        this.sessionManager.importSnapshots(snapshots);

        if (!snapshots.isEmpty()) {
            System.out.println(
                "Restored " + snapshots.size() + " persisted game-state snapshots"
            );
        }
    }


    /** Starts periodic autosave for session/game state history. */
    private void startSessionStateAutosave() {
        this.persistenceScheduler.scheduleAtFixedRate(
            this::persistSessionStateSafely,
            this.sessionAutosaveSeconds,
            this.sessionAutosaveSeconds,
            TimeUnit.SECONDS
        );
    }


    /** Persists session state while guarding against scheduler termination. */
    private void persistSessionStateSafely() {
        try {
            List<SessionManager.GameStateSnapshot> snapshots =
                this.sessionManager.exportSnapshots();
            this.sessionStateRepository.persistSnapshots(snapshots);

            GameRoundCoordinator.RoundStateSnapshot roundSnapshot =
                this.gameRoundCoordinator.exportSnapshot();
            this.gameRoundStateRepository.persistSnapshot(roundSnapshot);
        }
        catch (RuntimeException ex) {
            System.err.println("Session autosave error: " + ex.getMessage());
        }
    }


    /** Closes runtime resources. */
    @Override
    public void close() {
        persistSessionStateSafely();
        this.persistenceScheduler.shutdownNow();
        this.udpNotificationService.close();
        this.gameRoundCoordinator.close();
    }
}
