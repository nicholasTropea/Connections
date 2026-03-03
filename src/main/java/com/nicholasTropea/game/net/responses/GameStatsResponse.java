package com.nicholasTropea.game.net.responses;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Response;

/**
 * Response to a {@link GameStatsRequest}.
 * 
 * Expected JSON format:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING,
 *      "active" : BOOLEAN,
 *      "timeLeft" : LONG,
 *      "activePlayers" : INT,
 *      "finishedPlayers" : INT,
 *      "wonPlayers" : INT,
 *      "totalPlayers" : INT,
 *      "averageScore" : FLOAT
 * }
 * }</pre>
 * 
 * Possible errors: "id not found", "user not logged in"
 */
public class GameStatsResponse extends Response {

    /** Game state (true if game is active, false otherwise) */
    @SerializedName("active")
    private final boolean active;

    /** Time remaining in the game in milliseconds (null if active=false) */
    @SerializedName("timeLeft")
    private final Long timeLeft;

    /** Number of players with the game still in progress (null if active=false) */
    @SerializedName("activePlayers")
    private final Integer activePlayers;

    /** Number of players who finished the game */
    @SerializedName("finishedPlayers")
    private final Integer finishedPlayers;

    /** Number of players who finished the game with a win */
    @SerializedName("wonPlayers")
    private final Integer wonPlayers;

    /** Number of players who participated in the game (null if active=true) */
    @SerializedName("totalPlayers")
    private final Integer totalPlayers;

    /** Average score obtained by players (null if active=true) */
    @SerializedName("averageScore")
    private final Float averageScore;


    /**
     * Private constructor for creating game stats responses.
     *
     * @param success Whether the request was successful
     * @param error Error message if unsuccessful
     * @param active Whether the game is active
     * @param timeLeft Time remaining in milliseconds
     * @param activePlayers Number of active players
     * @param finishedPlayers Number of finished players
     * @param wonPlayers Number of players who won
     * @param totalPlayers Total number of players
     * @param averageScore Average score
     */
    private GameStatsResponse(
        boolean success,
        String error,
        boolean active,
        Long timeLeft,
        Integer activePlayers,
        Integer finishedPlayers,
        Integer wonPlayers,
        Integer totalPlayers,
        Float averageScore
    ) {
        super("requestGameStats", success, error);
        this.active = active;
        this.timeLeft = timeLeft;
        this.activePlayers = activePlayers;
        this.finishedPlayers = finishedPlayers;
        this.wonPlayers = wonPlayers;
        this.totalPlayers = totalPlayers;
        this.averageScore = averageScore;
    }


    /**
     * Creates a successful game stats response.
     * If active == true: timeLeft and activePlayers != null,
     *                    averageScore and totalPlayers == null
     * If active == false: averageScore and totalPlayers != null,
     *                     timeLeft and activePlayers == null
     * 
     * @param active True if the game is not finished, false otherwise
     * @param timeLeft Time remaining in the game in milliseconds
     * @param activePlayers Number of players with the game still in progress
     * @param finishedPlayers Number of players who finished the game
     * @param wonPlayers Number of players who finished the game with a win
     * @param totalPlayers Number of players who participated in the game
     * @param averageScore Average score obtained by players
     * @return Instance with success=true and error=null
     * @throws IllegalArgumentException if finishedPlayers or wonPlayers is null
     */
    public static GameStatsResponse success(
        boolean active,
        Long timeLeft,
        Integer activePlayers,
        Integer finishedPlayers,
        Integer wonPlayers,
        Integer totalPlayers,
        Float averageScore
    ) {
        if (finishedPlayers == null || wonPlayers == null) {
            throw new IllegalArgumentException(
                "finishedPlayers and wonPlayers cannot be null"
            );
        }

        if (active) {
            return new GameStatsResponse(
                true,               // success
                null,               // error
                true,               // active
                timeLeft,           
                activePlayers,
                finishedPlayers,
                wonPlayers,
                null,               // totalPlayers
                null                // averageScore
            );
        }

        return new GameStatsResponse(
            true,                   // success
            null,                   // error
            false,                  // active
            null,                   // timeLeft
            null,                   // activePlayers
            finishedPlayers,
            wonPlayers,
            totalPlayers,
            averageScore
        );
    }


    /**
     * Creates an error game stats response.
     * 
     * @param errorMsg Descriptive error message
     * @return Instance with success=false, error=errorMsg and remaining fields null
     * @throws IllegalArgumentException if errorMsg is null or empty
     */
    public static GameStatsResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new GameStatsResponse(
            false,
            errorMsg,
            false,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }


    /**
     * Checks if the game is active.
     *
     * @return True if game is active, false otherwise
     */
    public boolean isActive() { return this.active; }


    /**
     * Gets the time remaining in the game.
     *
     * @return Time remaining in milliseconds or null if game is not active
     */
    public Long getTimeLeft() { return this.timeLeft; }


    /**
     * Gets the number of active players.
     *
     * @return Number of active players or null if game is not active
     */
    public Integer getActivePlayers() { return this.activePlayers; }


    /**
     * Gets the number of players who finished the game.
     *
     * @return Number of finished players or null if request failed
     */
    public Integer getFinishedPlayers() { return this.finishedPlayers; }


    /**
     * Gets the number of players who won.
     *
     * @return Number of players who won or null if request failed
     */
    public Integer getWonPlayers() { return this.wonPlayers; }


    /**
     * Gets the total number of players who participated.
     *
     * @return Total number of players or null if game is active
     */
    public Integer getTotalPlayers() { return this.totalPlayers; }


    /**
     * Gets the average score.
     *
     * @return Average score or null if game is active
     */
    public Float getAverageScore() { return this.averageScore; }
}