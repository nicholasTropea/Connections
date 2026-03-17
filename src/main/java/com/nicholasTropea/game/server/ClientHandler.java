package com.nicholasTropea.game.server;

import java.net.Socket;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.nicholasTropea.game.net.Request;
import com.nicholasTropea.game.net.Response;
import com.nicholasTropea.game.net.RequestDeserializer;

import com.nicholasTropea.game.net.requests.*;
import com.nicholasTropea.game.net.responses.*;

import com.nicholasTropea.game.model.Game;
import com.nicholasTropea.game.model.LeaderboardRecord;
import com.nicholasTropea.game.model.MistakeHistogram;
import com.nicholasTropea.game.model.Player;
import com.nicholasTropea.game.model.PlayerGameState;


/**
 * Handles communication with a single connected client.
 * 
 * Parses incoming JSON messages and sends responses.
 * Each instance runs in a separate thread from the
 * {@link NetworkManager} pool.
 * 
 * @author Nicholas Riccardo Tropea
 */
public class ClientHandler implements Runnable {
    /** Shared repository for registered player persistence. */
    private final PlayerRepository playerRepository;

    /** Shared repository for game catalog loaded from games.json. */
    private final GameRepository gameRepository;

    /** Shared session manager for active player game states. */
    private final SessionManager sessionManager;

    /** Coordinator that tracks the globally active round game. */
    private final GameRoundCoordinator gameRoundCoordinator;

    /** Service used to register async UDP notification endpoints. */
    private final UdpNotificationService udpNotificationService;

    /** TCP socket of the connected client. */
    private Socket clientSocket;

    /** User ID of the currently logged-in player (null if not logged in). */
    private Integer loggedInUserId;

    /** Operation handlers registry keyed by request operation. */
    private final Map<String, Function<Request, Response>> handlers;


    /**
     * Creates a handler for the specified client.
     * 
     * @param clientSocket Socket of the newly accepted client
     * @param runtime Shared server runtime dependencies
     */
    public ClientHandler(Socket clientSocket, ServerRuntime runtime) {
        this.clientSocket = clientSocket;

        this.playerRepository = Objects.requireNonNull(
            runtime,
            "runtime is required"
        ).getPlayerRepository();

        this.gameRepository = runtime.getGameRepository();
        this.sessionManager = runtime.getSessionManager();
        this.gameRoundCoordinator = runtime.getGameRoundCoordinator();
        this.udpNotificationService = runtime.getUdpNotificationService();
        this.loggedInUserId = null;

        this.handlers = new HashMap<>();
        registerHandlers();
    }


    /** Registers operation handlers in a command-style registry. */
    private void registerHandlers() {
        handlers.put("login", req -> handleLogin((LoginRequest) req));
        handlers.put("logout", req -> handleLogout((LogoutRequest) req));
        handlers.put("register", req -> handleRegister((RegisterRequest) req));
        handlers.put("requestGameInfo", req -> handleGameInfo((GameInfoRequest) req));
        handlers.put("requestGameStats", req -> handleGameStats((GameStatsRequest) req));
        handlers.put(
            "requestLeaderboard",
            req -> handleLeaderboard((LeaderboardRequest) req)
        );
        handlers.put(
            "requestPlayerStats",
            req -> handlePlayerStats((PlayerStatsRequest) req)
        );
        handlers.put(
            "submitProposal",
            req -> handleSubmitProposal((SubmitProposalRequest) req)
        );
        handlers.put(
            "updateCredentials",
            req -> handleUpdateCredentials((UpdateCredentialsRequest) req)
        );
    }


    /**
     * Manages the lifecycle of the connection with the client.
     * 
     * Reads JSON messages in a loop until disconnection and
     * sends appropriate responses.
     */
    @Override
    public void run() {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("New client connected: " + clientSocket.getInetAddress());
        
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
            );
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Request.class, new RequestDeserializer())
                        .create();

            String line;
            
            while ((line = in.readLine()) != null) {
                try {
                    Request req = gson.fromJson(line, Request.class);
                    if (req == null) {
                        System.err.println("Received null request after JSON parsing.");
                        break;
                    }

                    Response resp = handleRequest(req);
                    String jsonResp = gson.toJson(resp);
                    out.println(jsonResp);
                }
                catch (RuntimeException ex) {
                    System.err.println(
                        "Protocol/processing error for client "
                        + clientSocket.getInetAddress()
                        + ": "
                        + ex.getMessage()
                    );
                    
                    break;
                }
            }

            System.out.println("Client disconnected: " + clientSocket.getInetAddress());

            if (this.loggedInUserId != null) {
                this.udpNotificationService.unregisterEndpoint(this.loggedInUserId);
                this.sessionManager.removeSession(this.loggedInUserId);
                this.loggedInUserId = null;
            }
        }
        catch (IOException e) { 
            System.err.println("Error: " + e.getMessage()); 
        }
    }


    /**
     * Routes the request to the appropriate handler and returns its response.
     *
     * @param req the request to handle
     * @return the response produced by the matching request handler
     */
    private Response handleRequest(Request req) {
        Function<Request, Response> handler = handlers.get(req.getOperation());
        if (handler == null) {
            throw new IllegalArgumentException(
                "Unknown request operation: " + req.getOperation()
            );
        }

        return handler.apply(req);
    }


    /**
     * Handles login request by authenticating the player and loading game data.
     *
     * <p>Validates the provided credentials, ensures that this connection does
     * not already have an authenticated user, loads the current round, opens
     * the corresponding session state, registers the UDP notification endpoint,
     * and returns the initial game snapshot for the player. If any validation
     * or setup step fails, returns an error response describing the failure.
     *
     * @param req the login request containing credentials and UDP port
     * @return a LoginResponse containing the initial player game state, or an
     *         error message if credentials are invalid, the connection is
     *         already authenticated, the current game is unavailable, the UDP
     *         port is missing, or login session setup fails
     */
    private LoginResponse handleLogin(LoginRequest req) {
        if (this.loggedInUserId != null) {
            return LoginResponse.error(
                "user already logged in on this connection"
            );
        }

        String loginError = this.playerRepository.validateLogin(
            req.getUsername(),
            req.getPassword()
        );
        if (loginError != null) { return LoginResponse.error(loginError); }

        Player player = this.playerRepository.getPlayerByUsername(req.getUsername());
        if (player == null) {
            return LoginResponse.error("username not found");
        }

        Game game = this.gameRoundCoordinator.getCurrentGame();
        if (game == null) {
            return LoginResponse.error("active game not found");
        }

        int userId = player.getUserId();
        int gameId = game.getId();
        List<String> gameWords = extractWords(game);
        Integer udpPort = req.getUdpPort();

        if (udpPort == null) {
            return LoginResponse.error("UDP port required");
        }

        String sessionError = this.sessionManager.openSessionForCurrentGame(
            userId,
            gameId,
            gameWords
        );
        if (sessionError != null) { return LoginResponse.error(sessionError); }

        PlayerGameState gameState = this.sessionManager.getSession(userId);
        if (gameState == null) {
            this.sessionManager.removeSession(userId);
            return LoginResponse.error("session not available");
        }

        try {
            this.udpNotificationService.registerEndpoint(
                userId,
                this.clientSocket.getInetAddress(),
                udpPort
            );
        }
        catch (RuntimeException ex) {
            this.sessionManager.removeSession(userId);
            return LoginResponse.error("unable to register UDP endpoint");
        }

        this.loggedInUserId = userId;

        return LoginResponse.success(
            game.getId(),
            gameState.getRemainingWords(),
            gameState.getGuessedGroups(),
            this.gameRoundCoordinator.getRemainingTimeMillis(),
            gameState.getErrorCount(),
            gameState.getScore()
        );
    }


    /**
     * Extracts all words from a game by flattening its groups.
     *
     * @param game game definition
     * @return list of 16 words
     */
    private List<String> extractWords(Game game) {
        List<String> words = new ArrayList<>();
        for (Game.Group group : game.getGroups()) {
            words.addAll(group.getWords());
        }

        return words;
    }


    /**
     * Handles logout request by clearing the active client session.
     *
     * <p>If a user is currently logged in, unregisters the related UDP
     * notification endpoint, removes the in-memory session state, and resets
     * the local authentication marker for this connection. If no user is
     * logged in, the operation is treated as idempotent and still succeeds.
     *
     * @param req the logout request
     * @return a LogoutResponse indicating that the logout operation completed
     *         successfully
     */
    private LogoutResponse handleLogout(LogoutRequest req) {
        if (this.loggedInUserId != null) {
            this.udpNotificationService.unregisterEndpoint(this.loggedInUserId);
            this.sessionManager.removeSession(this.loggedInUserId);
            this.loggedInUserId = null;
        }

        return LogoutResponse.success();
    }


    /**
     * Handles register request by creating a new player account.
     *
     * <p>Validates the provided credentials through the player repository
     * and attempts to persist a new player record. If validation or
     * registration fails, returns an error response with the repository
     * message; otherwise returns a successful registration response.
     *
     * @param req the register request containing username and password
     * @return a RegisterResponse indicating registration outcome, or an
     *         error message if the provided credentials are invalid or the
     *         username is already in use
     */
    private RegisterResponse handleRegister(RegisterRequest req) {
        String registerError = this.playerRepository.registerPlayer(
            req.getUsername(),
            req.getPassword()
        );
        if (registerError != null) {
            return RegisterResponse.error(registerError);
        }

        return RegisterResponse.success();
    }


    /**
     * Handles game info request by returning player-specific game details.
     *
     * <p>Resolves the requested game (current or specific), validates input,
     * and returns the caller's state for that game. Active games return
     * remaining time and unresolved words, while finished games return the
     * full solution with guessed groups, error count, and score.
     *
     * @param req the game info request containing current flag or game id
     * @return a GameInfoResponse containing player-specific game details, or an
     *         error message if the user is not logged in, gameId is missing,
     *         requested game does not exist, or user did not play that game
     */
    private GameInfoResponse handleGameInfo(GameInfoRequest req) {
        if (this.loggedInUserId == null) {
            return GameInfoResponse.error("user not logged in");
        }

        int requestedGameId;
        if (req.isCurrent()) {
            requestedGameId = this.gameRoundCoordinator.getCurrentGameId();
        }
        else {
            Integer gameId = req.getGameId();
            if (gameId == null) {
                return GameInfoResponse.error("gameId is required");
            }
            if (this.gameRepository.getGameById(gameId) == null) {
                return GameInfoResponse.error("game not found");
            }
            requestedGameId = gameId;
        }

        PlayerGameState gameState;
        if (this.gameRoundCoordinator.isCurrentGame(requestedGameId)) {
            gameState = getCurrentGameStateForLoggedUser();
        }
        else {
            gameState = this.sessionManager.getStateForGame(
                this.loggedInUserId,
                requestedGameId
            );
        }

        if (gameState == null) {
            return GameInfoResponse.error(
                "user hasn't participated in the requested game"
            );
        }

        boolean isActive = !gameState.isFinished();
        List<List<String>> solution = null;

        if (!isActive) {
            Game game = this.gameRepository.getGameById(gameState.getGameId());
            if (game != null) {
                solution = game.getGroups().stream()
                    .map(Game.Group::getWords)
                    .collect(java.util.stream.Collectors.toList());
            }
        }

        return GameInfoResponse.success(
            isActive,
            isActive ? this.gameRoundCoordinator.getRemainingTimeMillis() : null,
            isActive ? gameState.getRemainingWords() : null,
            solution,
            gameState.getGuessedGroups(),
            gameState.getErrorCount(),
            gameState.getScore()
        );
    }


    /**
     * Handles game stats request by returning aggregated game information.
     *
     * <p>Resolves the requested game (current or specific), validates input,
     * and returns statistics based on whether the game is still active or
     * already finished. Active games return live counters and remaining time,
     * while finished games return participant count and average score.
     *
     * @param req the game stats request containing current flag or game id
     * @return a GameStatsResponse containing aggregated statistics, or an
     *         error message if the user is not logged in, gameId is missing,
     *         or the requested game does not exist
     */
    private GameStatsResponse handleGameStats(GameStatsRequest req) {
        if (this.loggedInUserId == null) {
            return GameStatsResponse.error("user not logged in");
        }

        int requestedGameId;
        if (req.isCurrent()) {
            requestedGameId = this.gameRoundCoordinator.getCurrentGameId();
        }
        else {
            Integer gameId = req.getGameId();
            if (gameId == null) {
                return GameStatsResponse.error("gameId is required");
            }
            if (this.gameRepository.getGameById(gameId) == null) {
                return GameStatsResponse.error("game not found");
            }
            requestedGameId = gameId;
        }

        SessionManager.GameAggregates aggregates =
            this.sessionManager.getGameAggregates(requestedGameId);
        boolean isActive = this.gameRoundCoordinator.isCurrentGame(requestedGameId);

        if (isActive) {
            return GameStatsResponse.success(
                true,
                this.gameRoundCoordinator.getRemainingTimeMillis(),
                aggregates.getActivePlayers(),
                aggregates.getFinishedPlayers(),
                aggregates.getWonPlayers(),
                null,
                null
            );
        }

        return GameStatsResponse.success(
            false,
            null,
            null,
            aggregates.getFinishedPlayers(),
            aggregates.getWonPlayers(),
            aggregates.getParticipants(),
            aggregates.getAverageScore()
        );
    }


    /**
     * Handles leaderboard request by returning player rankings.
     *
     * <p>Processes the leaderboard request, returning the full ranking, a single
     * player's rank, or the top-K players depending on the request parameters.
     * If the user is not logged in, returns an error response.
     *
     * @param req the leaderboard request specifying player or top-K query
     * @return a LeaderboardResponse containing the requested ranking information,
     *         or an error message if the user is not logged in
     * @error If the user is not logged in, response contains error message
     */
    private LeaderboardResponse handleLeaderboard(LeaderboardRequest req) {
        if (this.loggedInUserId == null) {
            return LeaderboardResponse.error("user not logged in");
        }

        List<LeaderboardEntry> ranking = buildLeaderboardRanking();
        if (ranking.isEmpty()) {
            return LeaderboardResponse.success(List.of());
        }

        if (req.getPlayerUsername() != null) {
            return leaderboardForSinglePlayer(ranking, req.getPlayerUsername());
        }

        if (req.getKTopPlayers() != null) {
            return leaderboardForTopK(ranking, req.getKTopPlayers());
        }

        return LeaderboardResponse.success(toRecords(ranking));
    }


    /**
     * Handles player stats request by checking player states.
     *
     * @param req the player stats request
     * @return a PlayerStatsResponse indicating success, info if correct,
     *         or error message if invalid
     */
    private PlayerStatsResponse handlePlayerStats(PlayerStatsRequest req) {
        if (this.loggedInUserId == null) {
            return PlayerStatsResponse.error("user not logged in");
        }

        List<PlayerGameState> states = this.sessionManager.getStatesForUser(
            this.loggedInUserId
        );
        if (states.isEmpty()) {
            return PlayerStatsResponse.success(
                0,
                0,
                0,
                0,
                0.0f,
                0.0f,
                0,
                0,
                new MistakeHistogram(0, 0, 0, 0, 0, 0)
            );
        }

        int solved = 0;
        int failed = 0;
        int unfinished = 0;
        int perfect = 0;
        int win0 = 0;
        int win1 = 0;
        int win2 = 0;
        int win3 = 0;

        for (PlayerGameState state : states) {
            switch (state.getFinalState()) {
                case WON -> {
                    solved++;
                    int errors = state.getErrorCount();
                    if (errors <= 0) {
                        perfect++;
                        win0++;
                    }
                    else if (errors == 1) { win1++; }
                    else if (errors == 2) { win2++; }
                    else { win3++; }
                }
                case LOST -> failed++;
                case NOT_FINISHED -> unfinished++;
                default -> {
                    // exhaustive by enum, no-op fallback
                }
            }
        }

        int played = solved + failed + unfinished;
        float winRate = played == 0 ? 0.0f : (solved * 100.0f) / played;
        float lossRate = played == 0 ? 0.0f : (failed * 100.0f) / played;

        int currentStreak = computeCurrentWinStreak(states);
        int maxStreak = computeMaxWinStreak(states);
        MistakeHistogram histogram = new MistakeHistogram(
            win0,
            win1,
            win2,
            win3,
            failed,
            unfinished
        );

        return PlayerStatsResponse.success(
            solved,
            failed,
            unfinished,
            perfect,
            winRate,
            lossRate,
            currentStreak,
            maxStreak,
            histogram
        );
    }


    /**
     * Handles submit proposal request by validating word grouping.
     *
     * <p>Validates the submitted group of words, checks for malformed proposals,
     * and updates the game state accordingly. If the proposal matches a group,
     * updates guessed groups and score, and checks for win condition. If not,
     * increments error count and checks for loss condition.
     *
     * @param req the submit proposal request containing the proposed words
     * @return a SubmitProposalResponse indicating success, theme if correct,
     *         or error message if invalid
     */
    private SubmitProposalResponse handleSubmitProposal(SubmitProposalRequest req) {
        PlayerGameState gameState = getCurrentGameStateForLoggedUser();
        if (gameState == null) {
            return SubmitProposalResponse.error("user not logged in");
        }

        if (gameState.isFinished()) {
            return SubmitProposalResponse.error("game already finished");
        }

        Game game = this.gameRepository.getGameById(gameState.getGameId());
        if (game == null) {
            return SubmitProposalResponse.error("game not found");
        }

        List<String> proposedWords = 
            req.getWords().stream()
                          .map(word -> word.trim().toUpperCase())
                          .collect(java.util.stream.Collectors.toList());

        String malformedError = validateProposal(gameState, game, proposedWords);
        if (malformedError != null) {
            return SubmitProposalResponse.error(malformedError);
        }

        Game.Group matchedGroup = findMatchingGroup(game, proposedWords);

        if (matchedGroup != null) {
            gameState.removeWords(proposedWords);
            gameState.addGuessedGroup(matchedGroup.getWords());
            gameState.incrementCorrectProposals();
            recalculateScore(gameState);

            if (gameState.getCorrectProposals() >= 3) {
                gameState.completeAsWon();
            }

            return SubmitProposalResponse.success(true, matchedGroup.getTheme());
        }

        gameState.incrementErrorCount();
        recalculateScore(gameState);

        if (gameState.getErrorCount() >= 4) {
            gameState.completeAsLost();
        }

        return SubmitProposalResponse.success(false, null);
    }


    /**
     * Validates proposal format and state consistency.
     *
     * <p>Malformed proposals must not alter game state and are reported as
     * errors. A proposal is malformed if it has duplicated words, contains
     * words outside the game, or contains words already assigned to a group.
     *
     * @param gameState player state for current game
     * @param game current game definition
     * @param proposedWords submitted words
     * @return null if valid, otherwise error message
     */
    private String validateProposal(
        PlayerGameState gameState,
        Game game,
        List<String> proposedWords
    ) {
        if (proposedWords == null || proposedWords.size() != 4) {
            return "malformed proposal: exactly 4 words are required";
        }

        Set<String> uniqueWords = new HashSet<>(proposedWords);
        if (uniqueWords.size() != 4) {
            return "malformed proposal: duplicated words are not allowed";
        }

        Set<String> allGameWords = new HashSet<>(extractWords(game));
        if (!allGameWords.containsAll(uniqueWords)) {
            return "malformed proposal: words not in current game";
        }

        Set<String> remainingWords = new HashSet<>(gameState.getRemainingWords());
        if (!remainingWords.containsAll(uniqueWords)) {
            return "malformed proposal: one or more words already assigned";
        }

        return null;
    }


    /**
     * Recomputes score from correct and wrong proposal counts.
     *
     * @param gameState state to update
     */
    private void recalculateScore(PlayerGameState gameState) {
        int score = calculateScore(
            gameState.getCorrectProposals(),
            gameState.getErrorCount()
        );

        gameState.setScore(score);
    }


    /**
     * Calculates score according to project specification.
     *
     * @param correctProposals number of correct proposals
     * @param errors number of wrong proposals
     * @return computed game score
     */
    private int calculateScore(int correctProposals, int errors) {
        int bonus = switch (correctProposals) {
            case 1 -> 6;
            case 2 -> 12;
            case 0 -> 0;
            default -> 18;
        };

        return bonus - (errors * 4);
    }


    /**
     * Finds a group that matches all proposed words.
     *
     * @param game game definition
     * @param proposedWords words to match
     * @return matching group or null if no match
     */
    private Game.Group findMatchingGroup(Game game, List<String> proposedWords) {
        Set<String> proposedSet = new HashSet<>(proposedWords);

        for (Game.Group group : game.getGroups()) {
            Set<String> groupWords = new HashSet<>(group.getWords());

            if (groupWords.equals(proposedSet)) { return group; }
        }

        return null;
    }


    /** Handles update credentials request.
     * 
     * @param req the update credentials request containing the credential information
     * @return a UpdateCredentialsResponse indicating success or error message if invalid.
     */
    private UpdateCredentialsResponse handleUpdateCredentials(
        UpdateCredentialsRequest req
    ) {
        String updateError = this.playerRepository.updateCredentials(
            req.getOldUsername(),
            req.getOldPassword(),
            req.getNewUsername(),
            req.getNewPassword()
        );
        
        if (updateError != null) {
            return UpdateCredentialsResponse.error(updateError);
        }
        
        System.out.println(
            "Information updated correctly: " +
            req.getOldUsername() + 
            " -> " +
            req.getNewUsername()
        );

        return UpdateCredentialsResponse.success();
    }


    /**
     * Returns the session state for the logged-in user, synchronized with the
     * current global round.
     *
     * @return current round state for logged user, or null if not logged in
     */
    private PlayerGameState getCurrentGameStateForLoggedUser() {
        if (this.loggedInUserId == null) { return null; }

        Game currentGame = this.gameRoundCoordinator.getCurrentGame();
        List<String> currentWords = extractWords(currentGame);

        return this.sessionManager.ensureCurrentGameSession(
            this.loggedInUserId,
            currentGame.getId(),
            currentWords
        );
    }


    /** Internal leaderboard entry with precomputed total score. */
    private static final class LeaderboardEntry {
        private final String username;
        private final int score;


        private LeaderboardEntry(String username, int score) {
            this.username = username;
            this.score = score;
        }
    }


    /**
     * Builds global leaderboard ranking ordered by score descending.
     *
     * @return sorted ranking entries
     */
    private List<LeaderboardEntry> buildLeaderboardRanking() {
        List<LeaderboardEntry> ranking = new ArrayList<>();

        for (Player player : this.playerRepository.getAllPlayers()) {
            int totalScore = 0;

            List<PlayerGameState> states = this.sessionManager.getStatesForUser(
                player.getUserId()
            );

            for (PlayerGameState state : states) {
                totalScore += state.getScore();
            }

            ranking.add(new LeaderboardEntry(player.getNickname(), totalScore));
        }

        ranking.sort(
            Comparator
                .comparingInt((LeaderboardEntry entry) -> entry.score)
                .reversed()
                .thenComparing(entry -> entry.username)
        );

        return ranking;
    }


    /**
     * Returns leaderboard response containing all ranking entries.
     *
     * @param ranking sorted ranking entries
     * @return response with all records
     */
    private List<LeaderboardRecord> toRecords(List<LeaderboardEntry> ranking) {
        List<LeaderboardRecord> records = new ArrayList<>();

        for (int i = 0; i < ranking.size(); i++) {
            LeaderboardEntry entry = ranking.get(i);
            records.add(new LeaderboardRecord(entry.username, i + 1, entry.score));
        }

        return records;
    }


    /**
     * Builds leaderboard response for top-k players.
     *
     * @param ranking sorted ranking entries
     * @param topK requested top players count
     * @return response containing top-k records
     */
    private LeaderboardResponse leaderboardForTopK(
        List<LeaderboardEntry> ranking,
        int topK
    ) {
        int count = Math.min(topK, ranking.size());

        List<LeaderboardRecord> records = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            LeaderboardEntry entry = ranking.get(i);
            records.add(new LeaderboardRecord(entry.username, i + 1, entry.score));
        }

        return LeaderboardResponse.success(records);
    }


    /**
     * Builds leaderboard response for one player.
     *
     * @param ranking sorted ranking entries
     * @param username username to search
     * @return response containing single player rank, or error if missing
     */
    private LeaderboardResponse leaderboardForSinglePlayer(
        List<LeaderboardEntry> ranking,
        String username
    ) {
        for (int i = 0; i < ranking.size(); i++) {
            LeaderboardEntry entry = ranking.get(i);

            if (entry.username.equals(username)) {
                return LeaderboardResponse.success(
                    List.of(new LeaderboardRecord(entry.username, i + 1, entry.score))
                );
            }
        }

        return LeaderboardResponse.error("player not found");
    }


    /**
     * Computes the current streak from the end of ordered states.
     *
     * @param orderedStates states sorted by game id
     * @return number of trailing wins
     */
    private int computeCurrentWinStreak(List<PlayerGameState> orderedStates) {
        int streak = 0;

        for (int i = orderedStates.size() - 1; i >= 0; i--) {
            if (orderedStates.get(i).getFinalState() == PlayerGameState.GameResult.WON) {
                streak++;
            }
            else { break; }
        }

        return streak;
    }


    /**
     * Computes the maximum consecutive win streak across states.
     *
     * @param orderedStates states sorted by game id
     * @return maximum consecutive wins
     */
    private int computeMaxWinStreak(List<PlayerGameState> orderedStates) {
        int current = 0;
        int max = 0;

        for (PlayerGameState state : orderedStates) {
            if (state.getFinalState() == PlayerGameState.GameResult.WON) {
                current++;
                if (current > max) { max = current; }
            }
            else { current = 0; }
        }

        return max;
    }
}