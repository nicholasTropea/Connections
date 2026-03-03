package com.nicholasTropea.game.net.responses;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import com.nicholasTropea.game.net.Response;

/**
 * Response to a {@link GameInfoRequest}.
 * 
 * Expected JSON format:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING,
 *      "active" : BOOLEAN,
 *      "timeLeft" : LONG,
 *      "wordsLeft" : LIST<STRING>,
 *      "solution" : LIST<LIST<STRING>>,
 *      "guessedGroups" : LIST<LIST<STRING>>,
 *      "errors" : INT,
 *      "score" : INT
 * }
 * }</pre>
 * 
 * Possible errors: "user not logged in"
 */
public class GameInfoResponse extends Response {

    /**
     * Game state (true if game is active and not finished, false otherwise)
     */
    @SerializedName("active")
    private final boolean active;

    /** Time remaining in the game in milliseconds (null if active=false) */
    @SerializedName("timeLeft")
    private final Long timeLeft;

    /** Words remaining to be grouped (null if active=false) */
    @SerializedName("wordsLeft")
    private final List<String> wordsLeft;

    /** Correct solution for word groups (null if active=true) */
    @SerializedName("solution")
    private final List<List<String>> solution;

    /** List of guessed word groups */
    @SerializedName("guessedGroups")
    private final List<List<String>> guessedGroups;

    /** Number of errors made in the game */
    @SerializedName("errors")
    private final Integer errors;

    /** Score obtained in the game */
    @SerializedName("score")
    private final Integer score;


    /**
     * Private constructor for creating game info responses.
     *
     * @param success Whether the request was successful
     * @param error Error message if unsuccessful
     * @param active Whether the game is active
     * @param timeLeft Time remaining in milliseconds
     * @param wordsLeft List of words left to group
     * @param solution Correct solution for word groups
     * @param guessedGroups List of guessed groups
     * @param errors Number of errors made
     * @param score Score obtained
     */
    private GameInfoResponse(
        boolean success,
        String error,
        boolean active,
        Long timeLeft,
        List<String> wordsLeft,
        List<List<String>> solution,
        List<List<String>> guessedGroups,
        Integer errors,
        Integer score
    ) {
        super("requestGameInfo", success, error);
        this.active = active;
        this.timeLeft = timeLeft;
        this.wordsLeft = wordsLeft != null ? List.copyOf(wordsLeft) : null;
        this.solution = solution != null ? List.copyOf(solution) : null;
        this.guessedGroups = guessedGroups != null ? List.copyOf(guessedGroups) : null;
        this.errors = errors;
        this.score = score;
    }


    /**
     * Creates a successful game info response.
     * If active == true: timeLeft and wordsLeft != null, solution == null
     * If active == false: solution != null, timeLeft and wordsLeft == null
     * 
     * @param active True if game is current and not finished by player, false otherwise
     * @param timeLeft Time remaining in the game in milliseconds
     * @param wordsLeft Words remaining to be grouped
     * @param solution Correct solution for word groups
     * @param guessedGroups List of guessed word groups
     * @param errors Number of errors made
     * @param score Score obtained
     * @return Instance with success=true and error=null
     * @throws IllegalArgumentException if errors or score is null
     */
    public static GameInfoResponse success(
        boolean active,
        Long timeLeft,
        List<String> wordsLeft,
        List<List<String>> solution,
        List<List<String>> guessedGroups,
        Integer errors,
        Integer score
    ) {
        if (errors == null || score == null) {
            throw new IllegalArgumentException("errors and score cannot be null");
        }

        if (active) {
            return new GameInfoResponse(
                true,
                null,
                true,
                timeLeft,
                wordsLeft,
                null,
                guessedGroups,
                errors,
                score
            );
        }

        return new GameInfoResponse(
            true,
            null,
            false,
            null,
            null,
            solution,
            guessedGroups,
            errors,
            score
        );
    }


    /**
     * Creates an error game info response.
     * 
     * @param errorMsg Descriptive error message
     * @return Instance with success=false, error=errorMsg and remaining fields null
     * @throws IllegalArgumentException if errorMsg is null or empty
     */
    public static GameInfoResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new GameInfoResponse(
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
     * Gets the words left to be grouped.
     *
     * @return Defensive copy of words left list or null if game is not active
     */
    public List<String> getWordsLeft() { return this.wordsLeft; }


    /**
     * Gets the correct solution for word groups.
     *
     * @return Defensive copy of solution list or null if game is active
     */
    public List<List<String>> getSolution() { return this.solution; }


    /**
     * Gets the list of guessed word groups.
     *
     * @return Defensive copy of guessed groups list or null if request failed
     */
    public List<List<String>> getGuessedGroups() { return this.guessedGroups; }


    /**
     * Gets the number of errors made.
     *
     * @return Number of errors or null if request failed
     */
    public Integer getErrors() { return this.errors; }


    /**
     * Gets the score obtained.
     *
     * @return Score or null if request failed
     */
    public Integer getScore() { return this.score; }
}