package com.nicholasTropea.game.server;

import java.net.Socket;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
    private static final PlayerRepository PLAYER_REPOSITORY = new PlayerRepository();

    /** Shared repository for game catalog loaded from games.json. */
    private static final GameRepository GAME_REPOSITORY = new GameRepository();

    /** Shared session manager for active player game states. */
    private static final SessionManager SESSION_MANAGER = new SessionManager();

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
     */
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.loggedInUserId = null;
        this.handlers = new HashMap<>();
        registerHandlers();
    }


    /**
     * Registers operation handlers in a command-style registry.
     */
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
            
            String line = in.readLine();
            
            if (line == null) {
                System.out.println("Client disconnected without sending data.");
                return;
            }
            
            System.out.println("\n[RECEIVED REQUEST]");
            System.out.println("Raw JSON: " + line);
            
            Request req = gson.fromJson(line, Request.class);
            System.out.println("Request Type: " + req.getClass().getSimpleName());
            System.out.println("Operation: " + req.getOperation());
            
            Response resp = handleRequest(req);
            
            String jsonResp = gson.toJson(resp);
            
            System.out.println("\n[SENDING RESPONSE]");
            System.out.println("Response Type: " + resp.getClass().getSimpleName());
            System.out.println("Success: " + resp.isSuccess());
            System.out.println("Raw JSON: " + jsonResp);
            
            out.println(jsonResp);
            
            System.out.println("\nRequest handled successfully.");
            System.out.println("-".repeat(60));
        }
        catch (IOException e) { 
            System.err.println("Error: " + e.getMessage()); 
        }
    }


    /**
     * Routes the request to appropriate handler and creates mock response.
     * 
     * @param req The request to handle
     * @return The appropriate response for the request type
     */
    private Response handleRequest(Request req) {
        Function<Request, Response> handler = handlers.get(req.getOperation());
        if (handler == null) {
            throw new IllegalArgumentException("Unknown request operation: " + req.getOperation());
        }

        return handler.apply(req);
    }


    /**
     * Handles login request by validating credentials and loading game data.
     */
    private LoginResponse handleLogin(LoginRequest req) {
        System.out.println("  Username: " + req.getUsername());
        String loginError = PLAYER_REPOSITORY.validateLogin(
            req.getUsername(),
            req.getPassword()
        );
        if (loginError != null) {
            return LoginResponse.error(loginError);
        }

        Player player = PLAYER_REPOSITORY.getPlayerByUsername(req.getUsername());
        if (player == null) {
            return LoginResponse.error("username not found");
        }

        int userId = player.getUserId();
        int gameId = selectGameId(req.getUsername());
        Game game = GAME_REPOSITORY.getGameById(gameId);
        if (game == null) {
            return LoginResponse.error("assigned game not found");
        }

        List<String> gameWords = extractWords(game);
        PlayerGameState gameState = new PlayerGameState(userId, gameId, gameWords);

        String sessionError = SESSION_MANAGER.createSession(userId, gameState);
        if (sessionError != null) {
            return LoginResponse.error(sessionError);
        }

        this.loggedInUserId = userId;

        return LoginResponse.success(
            game.getId(),
            gameWords,
            Arrays.asList(),  // guessedGroups
            300000L,  // timeLeft (5 minutes)
            0,  // errors
            0   // score
        );
    }


    /**
     * Selects a deterministic game id based on username hash.
     *
     * @param username player username
     * @return selected game id
     */
    private int selectGameId(String username) {
        int totalGames = GAME_REPOSITORY.size();
        if (totalGames == 0) {
            throw new IllegalStateException("No games loaded in repository");
        }

        int index = Math.abs(username.hashCode()) % totalGames;
        return GAME_REPOSITORY.getGameIdByIndex(index);
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
     * Handles logout request.
     */
    private LogoutResponse handleLogout(LogoutRequest req) {
        if (this.loggedInUserId != null) {
            SESSION_MANAGER.removeSession(this.loggedInUserId);
            this.loggedInUserId = null;
        }

        return LogoutResponse.success();
    }


    /**
     * Handles register request.
     */
    private RegisterResponse handleRegister(RegisterRequest req) {
        System.out.println("  Username: " + req.getUsername());

        String registerError = PLAYER_REPOSITORY.registerPlayer(
            req.getUsername(),
            req.getPassword()
        );
        if (registerError != null) {
            return RegisterResponse.error(registerError);
        }

        return RegisterResponse.success();
    }


    /**
     * Handles game info request by returning current game state.
     */
    private GameInfoResponse handleGameInfo(GameInfoRequest req) {
        if (this.loggedInUserId == null) {
            return GameInfoResponse.error("user not logged in");
        }

        PlayerGameState gameState = SESSION_MANAGER.getSession(this.loggedInUserId);
        if (gameState == null) {
            return GameInfoResponse.error("user not logged in");
        }

        boolean isActive = !gameState.isFinished();
        List<List<String>> solution = null;

        if (!isActive) {
            Game game = GAME_REPOSITORY.getGameById(gameState.getGameId());
            if (game != null) {
                solution = game.getGroups().stream()
                    .map(Game.Group::getWords)
                    .collect(java.util.stream.Collectors.toList());
            }
        }

        return GameInfoResponse.success(
            isActive,
            isActive ? 300000L : null,  // timeLeft
            isActive ? gameState.getRemainingWords() : null,
            solution,
            Arrays.asList(),  // guessedGroups (tracked separately if needed)
            gameState.getErrorCount(),
            gameState.getScore()
        );
    }


    /**
     * Handles game stats request with mock data.
     */
    private GameStatsResponse handleGameStats(GameStatsRequest req) {
        System.out.println("  Game ID: " + req.getGameId());
        return GameStatsResponse.success(
            true,  // active
            300000L,  // timeLeft
            5,  // activePlayers
            3,  // finishedPlayers
            2,  // wonPlayers
            null,  // totalPlayers
            null   // averageScore
        );
    }


    /**
     * Handles leaderboard request with mock data.
     */
    private LeaderboardResponse handleLeaderboard(LeaderboardRequest req) {
        if (req.getPlayerUsername() != null) {
            System.out.println("  Player Username: " + req.getPlayerUsername());
        } else if (req.getKTopPlayers() != null) {
            System.out.println("  Top K: " + req.getKTopPlayers());
        } else if (req.isAll()) {
            System.out.println("  Request Type: All players");
        }
        return LeaderboardResponse.success(
            Arrays.asList(
                new LeaderboardRecord("player1", 1),
                new LeaderboardRecord("player2", 2),
                new LeaderboardRecord("player3", 3)
            )
        );
    }


    /**
     * Handles player stats request with mock data.
     */
    private PlayerStatsResponse handlePlayerStats(PlayerStatsRequest req) {
        return PlayerStatsResponse.success(
            10,  // solved
            2,   // failed
            1,   // unfinished
            5,   // perfect
            76.9f,  // winRate
            15.4f,  // lossRate
            3,   // currentStreak
            5,   // maxStreak
            null // histogram
        );
    }


    /**
     * Handles submit proposal request by validating word grouping.
     */
    private SubmitProposalResponse handleSubmitProposal(SubmitProposalRequest req) {
        System.out.println("  Words: " + req.getWords());

        if (this.loggedInUserId == null) {
            return SubmitProposalResponse.error("user not logged in");
        }

        PlayerGameState gameState = SESSION_MANAGER.getSession(this.loggedInUserId);
        if (gameState == null) {
            return SubmitProposalResponse.error("user not logged in");
        }

        if (gameState.isFinished()) {
            return SubmitProposalResponse.error("game already finished");
        }

        Game game = GAME_REPOSITORY.getGameById(gameState.getGameId());
        if (game == null) {
            return SubmitProposalResponse.error("game not found");
        }

        List<String> proposedWords = req.getWords();
        Game.Group matchedGroup = findMatchingGroup(game, proposedWords);

        if (matchedGroup != null) {
            gameState.removeWords(proposedWords);
            gameState.incrementCorrectProposals();
            gameState.setScore(gameState.getScore() + 10);

            if (gameState.getRemainingWords().isEmpty()) {
                gameState.completeAsWon();
            }

            return SubmitProposalResponse.success(true, matchedGroup.getTheme());
        }
        else {
            gameState.incrementErrorCount();

            if (gameState.getErrorCount() >= 4) {
                gameState.completeAsLost();
            }

            return SubmitProposalResponse.success(false, null);
        }
    }


    /**
     * Finds a group that matches all proposed words.
     *
     * @param game game definition
     * @param proposedWords words to match
     * @return matching group or null if no match
     */
    private Game.Group findMatchingGroup(Game game, List<String> proposedWords) {
        for (Game.Group group : game.getGroups()) {
            List<String> groupWords = group.getWords();
            if (groupWords.size() == proposedWords.size() 
                && groupWords.containsAll(proposedWords)) {
                return group;
            }
        }

        return null;
    }


    /**
     * Handles update credentials request.
     */
    private UpdateCredentialsResponse handleUpdateCredentials(UpdateCredentialsRequest req) {
        System.out.println("  New Username: " + req.getNewUsername());

        String updateError = PLAYER_REPOSITORY.updateCredentials(
            req.getOldUsername(),
            req.getOldPassword(),
            req.getNewUsername(),
            req.getNewPassword()
        );
        if (updateError != null) {
            return UpdateCredentialsResponse.error(updateError);
        }

        return UpdateCredentialsResponse.success();
    }
}