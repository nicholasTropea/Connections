package com.nicholasTropea.game.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
 * Manages the state and logic for a single NIO client connection.
 * 
 * Handles non-blocking reads, request processing, and sending responses.
 * 
 * @author Nicholas Riccardo Tropea
 */
public class ClientHandler {
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

    /** NIO SocketChannel for the client. */
    private final SocketChannel channel;

    /** Buffer for accumulating partial reads from the channel. */
    private final ByteBuffer readBuffer;

    /** Builder for reconstructing full JSON lines from the read buffer. */
    private final StringBuilder messageBuilder;

    /** JSON serializer/deserializer. */
    private final Gson gson;

    /** User ID of the currently logged-in player (null if not logged in). */
    private Integer loggedInUserId;

    /** Operation handlers registry keyed by request operation. */
    private final Map<String, Function<Request, Response>> handlers;


    /**
     * Creates a new handler for the NIO channel.
     * 
     * @param channel NIO SocketChannel
     * @param runtime Shared server runtime dependencies
     */
    public ClientHandler(SocketChannel channel, ServerRuntime runtime) {
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(8192);
        this.messageBuilder = new StringBuilder();
        
        this.gson = new GsonBuilder()
                    .registerTypeAdapter(Request.class, new RequestDeserializer())
                    .create();

        this.playerRepository = runtime.getPlayerRepository();
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
     * Reads from the channel and processes any complete requests.
     * 
     * @throws IOException if a network error occurs
     */
    public void handleRead() throws IOException {
        readBuffer.clear();
        int bytesRead = channel.read(readBuffer);
        
        if (bytesRead == -1) {
            close();
            throw new IOException("Client disconnected");
        }

        readBuffer.flip();
        String chunk = StandardCharsets.UTF_8.decode(readBuffer).toString();
        messageBuilder.append(chunk);

        processLines();
    }


    /** Extracts and processes complete lines from the message builder. */
    private void processLines() {
        int newlineIndex;
        while ((newlineIndex = messageBuilder.indexOf("\n")) != -1) {
            String line = messageBuilder.substring(0, newlineIndex).trim();
            messageBuilder.delete(0, newlineIndex + 1);

            if (!line.isEmpty()) {
                try {
                    Request req = gson.fromJson(line, Request.class);
                    if (req != null) {
                        Response resp = handleRequest(req);
                        sendResponse(resp);
                    }
                }
                catch (Exception ex) {
                    System.err.println("Error processing request: " + ex.getMessage());
                }
            }
        }
    }


    /** Sends a response back to the client. */
    private void sendResponse(Response resp) {
        try {
            String json = gson.toJson(resp) + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }
        catch (IOException ex) {
            System.err.println("Error sending response: " + ex.getMessage());
            close();
        }
    }


    /** Performs cleanup when the client disconnects. */
    public void close() {
        if (this.loggedInUserId != null) {
            this.udpNotificationService.unregisterEndpoint(this.loggedInUserId);
            this.sessionManager.removeSession(this.loggedInUserId);
            this.loggedInUserId = null;
        }
        try { channel.close(); }
        catch (IOException ignored) {}
    }


    /** Routes the request to the appropriate handler. */
    private Response handleRequest(Request req) {
        Function<Request, Response> handler = handlers.get(req.getOperation());
        if (handler == null) {
            return new Response(req.getOperation(), false, "Unknown request operation: " + req.getOperation()) {};
        }
        return handler.apply(req);
    }


    // --- HANDLERS (Same logic as before, but updated to use channel context) ---

    private LoginResponse handleLogin(LoginRequest req) {
        if (this.loggedInUserId != null) {
            return LoginResponse.error("user already logged in on this connection");
        }

        String loginError = this.playerRepository.validateLogin(
            req.getUsername(),
            req.getPassword()
        );
        if (loginError != null) { return LoginResponse.error(loginError); }

        Player player = this.playerRepository.getPlayerByUsername(req.getUsername());
        if (player == null) { return LoginResponse.error("username not found"); }

        Game game = this.gameRoundCoordinator.getCurrentGame();
        if (game == null) { return LoginResponse.error("active game not found"); }

        int userId = player.getUserId();
        Integer udpPort = req.getUdpPort();

        if (udpPort == null) { return LoginResponse.error("UDP port required"); }

        String sessionError = this.sessionManager.openSessionForCurrentGame(
            userId,
            game.getId(),
            extractWords(game)
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
                this.channel.socket().getInetAddress(),
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

    private List<String> extractWords(Game game) {
        List<String> words = new ArrayList<>();
        for (Game.Group group : game.getGroups()) {
            words.addAll(group.getWords());
        }
        return words;
    }

    private LogoutResponse handleLogout(LogoutRequest req) {
        if (this.loggedInUserId != null) {
            this.udpNotificationService.unregisterEndpoint(this.loggedInUserId);
            this.sessionManager.removeSession(this.loggedInUserId);
            this.loggedInUserId = null;
        }
        return LogoutResponse.success();
    }

    private RegisterResponse handleRegister(RegisterRequest req) {
        String registerError = this.playerRepository.registerPlayer(
            req.getUsername(),
            req.getPassword()
        );
        return (registerError != null) ? RegisterResponse.error(registerError) : RegisterResponse.success();
    }

    private GameInfoResponse handleGameInfo(GameInfoRequest req) {
        if (this.loggedInUserId == null) { return GameInfoResponse.error("user not logged in"); }

        int requestedGameId = req.isCurrent() 
            ? this.gameRoundCoordinator.getCurrentGameId() 
            : req.getGameId();

        if (this.gameRepository.getGameById(requestedGameId) == null) {
            return GameInfoResponse.error("game not found");
        }

        PlayerGameState gameState = this.sessionManager.getStateForGame(this.loggedInUserId, requestedGameId);
        if (gameState == null) { return GameInfoResponse.error("user hasn't participated in the requested game"); }

        boolean isActive = !gameState.isFinished();
        List<List<String>> solution = null;
        if (!isActive) {
            Game game = this.gameRepository.getGameById(gameState.getGameId());
            solution = game.getGroups().stream().map(Game.Group::getWords).collect(java.util.stream.Collectors.toList());
        }

        return GameInfoResponse.success(
            isActive,
            this.gameRoundCoordinator.isCurrentGame(requestedGameId) ? this.gameRoundCoordinator.getRemainingTimeMillis() : null,
            isActive ? gameState.getRemainingWords() : null,
            solution,
            gameState.getGuessedGroups(),
            gameState.getErrorCount(),
            gameState.getScore()
        );
    }

    private GameStatsResponse handleGameStats(GameStatsRequest req) {
        if (this.loggedInUserId == null) { return GameStatsResponse.error("user not logged in"); }

        int requestedGameId = req.isCurrent() 
            ? this.gameRoundCoordinator.getCurrentGameId() 
            : req.getGameId();

        if (this.gameRepository.getGameById(requestedGameId) == null) {
            return GameStatsResponse.error("game not found");
        }

        SessionManager.GameAggregates aggregates = this.sessionManager.getGameAggregates(requestedGameId);
        boolean isActive = this.gameRoundCoordinator.isCurrentGame(requestedGameId);

        if (isActive) {
            return GameStatsResponse.success(true, this.gameRoundCoordinator.getRemainingTimeMillis(), aggregates.getActivePlayers(), aggregates.getFinishedPlayers(), aggregates.getWonPlayers(), null, null);
        }
        return GameStatsResponse.success(false, null, null, aggregates.getFinishedPlayers(), aggregates.getWonPlayers(), aggregates.getParticipants(), aggregates.getAverageScore());
    }

    private LeaderboardResponse handleLeaderboard(LeaderboardRequest req) {
        if (this.loggedInUserId == null) { return LeaderboardResponse.error("user not logged in"); }
        List<LeaderboardEntry> ranking = buildLeaderboardRanking();
        if (ranking.isEmpty()) return LeaderboardResponse.success(List.of());

        if (req.getPlayerUsername() != null) return leaderboardForSinglePlayer(ranking, req.getPlayerUsername());
        if (req.getKTopPlayers() != null) return leaderboardForTopK(ranking, req.getKTopPlayers());
        return LeaderboardResponse.success(toRecords(ranking));
    }

    private PlayerStatsResponse handlePlayerStats(PlayerStatsRequest req) {
        if (this.loggedInUserId == null) { return PlayerStatsResponse.error("user not logged in"); }
        List<PlayerGameState> states = this.sessionManager.getStatesForUser(this.loggedInUserId);
        if (states.isEmpty()) return PlayerStatsResponse.success(0,0,0,0,0.0f,0.0f,0,0, new MistakeHistogram(0,0,0,0,0,0));

        int solved=0, failed=0, unfinished=0, perfect=0;
        int win0=0, win1=0, win2=0, win3=0;

        for (PlayerGameState state : states) {
            switch (state.getFinalState()) {
                case WON -> {
                    solved++;
                    int e = state.getErrorCount();
                    if (e<=0) { perfect++; win0++; }
                    else if (e==1) win1++;
                    else if (e==2) win2++;
                    else win3++;
                }
                case LOST -> failed++;
                case NOT_FINISHED -> unfinished++;
            }
        }
        int played = solved + failed + unfinished;
        return PlayerStatsResponse.success(solved, failed, unfinished, perfect, 
            played==0 ? 0f : (solved*100f)/played, played==0 ? 0f : (failed*100f)/played,
            computeCurrentWinStreak(states), computeMaxWinStreak(states),
            new MistakeHistogram(win0, win1, win2, win3, failed, unfinished));
    }

    private SubmitProposalResponse handleSubmitProposal(SubmitProposalRequest req) {
        PlayerGameState gameState = getCurrentGameStateForLoggedUser();
        if (gameState == null) return SubmitProposalResponse.error("user not logged in");
        if (gameState.isFinished()) return SubmitProposalResponse.error("game already finished");

        Game game = this.gameRepository.getGameById(gameState.getGameId());
        List<String> proposedWords = req.getWords().stream().map(String::trim).map(String::toUpperCase).collect(java.util.stream.Collectors.toList());

        String malformedError = validateProposal(gameState, game, proposedWords);
        if (malformedError != null) return SubmitProposalResponse.error(malformedError);

        Game.Group matchedGroup = findMatchingGroup(game, proposedWords);
        if (matchedGroup != null) {
            gameState.removeWords(proposedWords);
            gameState.addGuessedGroup(matchedGroup.getWords());
            gameState.incrementCorrectProposals();
            recalculateScore(gameState);
            if (gameState.getCorrectProposals() >= 3) gameState.completeAsWon();
            return SubmitProposalResponse.success(true, matchedGroup.getTheme());
        }

        gameState.incrementErrorCount();
        recalculateScore(gameState);
        if (gameState.getErrorCount() >= 4) gameState.completeAsLost();
        return SubmitProposalResponse.success(false, null);
    }

    private String validateProposal(PlayerGameState gameState, Game game, List<String> proposedWords) {
        if (proposedWords == null || proposedWords.size() != 4) return "malformed proposal: exactly 4 words are required";
        if (new HashSet<>(proposedWords).size() != 4) return "malformed proposal: duplicated words are not allowed";
        if (!new HashSet<>(extractWords(game)).containsAll(proposedWords)) return "malformed proposal: words not in current game";
        if (!new HashSet<>(gameState.getRemainingWords()).containsAll(proposedWords)) return "malformed proposal: one or more words already assigned";
        return null;
    }

    private void recalculateScore(PlayerGameState gameState) {
        int bonus = switch (gameState.getCorrectProposals()) {
            case 1 -> 6; case 2 -> 12; case 3 -> 18; default -> 0;
        };
        gameState.setScore(bonus - (gameState.getErrorCount() * 4));
    }

    private Game.Group findMatchingGroup(Game game, List<String> proposedWords) {
        Set<String> proposedSet = new HashSet<>(proposedWords);
        return game.getGroups().stream().filter(g -> new HashSet<>(g.getWords()).equals(proposedSet)).findFirst().orElse(null);
    }

    private UpdateCredentialsResponse handleUpdateCredentials(UpdateCredentialsRequest req) {
        String err = this.playerRepository.updateCredentials(req.getOldUsername(), req.getOldPassword(), req.getNewUsername(), req.getNewPassword());
        return err != null ? UpdateCredentialsResponse.error(err) : UpdateCredentialsResponse.success();
    }

    private PlayerGameState getCurrentGameStateForLoggedUser() {
        if (this.loggedInUserId == null) return null;
        Game g = this.gameRoundCoordinator.getCurrentGame();
        return this.sessionManager.ensureCurrentGameSession(this.loggedInUserId, g.getId(), extractWords(g));
    }

    private List<LeaderboardEntry> buildLeaderboardRanking() {
        List<LeaderboardEntry> r = new ArrayList<>();
        for (Player p : this.playerRepository.getAllPlayers()) {
            int score = this.sessionManager.getStatesForUser(p.getUserId()).stream().mapToInt(PlayerGameState::getScore).sum();
            r.add(new LeaderboardEntry(p.getNickname(), score));
        }
        r.sort(Comparator.comparingInt((LeaderboardEntry e) -> e.score).reversed().thenComparing(e -> e.username));
        return r;
    }

    private List<LeaderboardRecord> toRecords(List<LeaderboardEntry> ranking) {
        List<LeaderboardRecord> recs = new ArrayList<>();
        for (int i=0; i<ranking.size(); i++) recs.add(new LeaderboardRecord(ranking.get(i).username, i+1, ranking.get(i).score));
        return recs;
    }

    private LeaderboardResponse leaderboardForTopK(List<LeaderboardEntry> ranking, int k) {
        return LeaderboardResponse.success(toRecords(ranking.subList(0, Math.min(k, ranking.size()))));
    }

    private LeaderboardResponse leaderboardForSinglePlayer(List<LeaderboardEntry> ranking, String user) {
        for (int i=0; i<ranking.size(); i++) if (ranking.get(i).username.equals(user)) return LeaderboardResponse.success(List.of(new LeaderboardRecord(user, i+1, ranking.get(i).score)));
        return LeaderboardResponse.error("player not found");
    }

    private int computeCurrentWinStreak(List<PlayerGameState> s) {
        int streak = 0;
        for (int i=s.size()-1; i>=0; i--) if (s.get(i).getFinalState()==PlayerGameState.GameResult.WON) streak++; else break;
        return streak;
    }

    private int computeMaxWinStreak(List<PlayerGameState> s) {
        int max=0, curr=0;
        for (PlayerGameState st : s) { if (st.getFinalState()==PlayerGameState.GameResult.WON) { curr++; max=Math.max(max,curr); } else curr=0; }
        return max;
    }

    private record LeaderboardEntry(String username, int score) {}
}
