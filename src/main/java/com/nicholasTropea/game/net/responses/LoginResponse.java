package com.nicholasTropea.game.net.responses;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import com.nicholasTropea.game.net.Response;

/**
 * Response to a {@link LoginRequest}.
 * 
 * Expected JSON format:
 * <pre>{@code
 * {
 *    "success" : BOOLEAN,
 *    "error" : STRING,
 *    "gameId" : INT,
 *    "words" : LIST<STRING>,
 *    "guessedGroups" : LIST<LIST<STRING>>,
 *    "timeLeft" : LONG,
 *    "errors" : INT,
 *    "score" : INT
 * }
 * }</pre>
 * 
 * Possible errors: "username not found", "incorrect password", "connection already logged in"
 * 
 * @see RegisterResponse for the registration format
 */
public class LoginResponse extends Response {

    /** ID of the current game (null if success=false) */
    @SerializedName("gameId")
    private final Integer gameId;

    /** List of words in the current game (null if success=false) */
    @SerializedName("words")
    private final List<String> words;

    /** List of already guessed word groups in the current game */
    @SerializedName("guessedGroups")
    private final List<List<String>> guessedGroups;

    /** Time remaining in the current game in milliseconds */
    @SerializedName("timeLeft")
    private final Long timeLeft;

    /** Number of errors already made in the current game */
    @SerializedName("errors")
    private final Integer errors;

    /** Score obtained in the current game */
    @SerializedName("score")
    private final Integer score;


    /**
     * Private constructor for creating login responses.
     *
     * @param success Whether the login was successful
     * @param error Error message if unsuccessful
     * @param gameId ID of the current game
     * @param words List of words in the game
     * @param guessedGroups List of already guessed groups
     * @param timeLeft Time remaining in milliseconds
     * @param errors Number of errors made
     * @param score Score obtained
     */
    private LoginResponse(
        boolean success,
        String error,
        Integer gameId,
        List<String> words,
        List<List<String>> guessedGroups,
        Long timeLeft,
        Integer errors,
        Integer  score
    ) {
        super("login", success, error);
        this.gameId = gameId;
        this.words = words != null ? List.copyOf(words) : null;
        this.guessedGroups = guessedGroups != null ? List.copyOf(guessedGroups) : null;
        this.timeLeft = timeLeft;
        this.errors = errors;
        this.score = score;
    }


    /**
     * Creates a successful login response.
     * 
     * @param gameId ID of the current game
     * @param words List of words in the current game
     * @param guessedGroups List of already guessed word groups in the current game
     * @param timeLeft Time remaining in the current game in milliseconds
     * @param errors Number of errors already made in the current game
     * @param score Score obtained in the current game
     * @return Instance with success=true and error=null
     * @throws IllegalArgumentException if gameId is out of range or words doesn't contain 16 elements
     */
    public static LoginResponse success(
        Integer gameId,
        List<String> words,
        List<List<String>> guessedGroups,
        Long timeLeft,
        Integer errors,
        Integer score
    ) {
        validateSuccess(gameId, words);

        return new LoginResponse(
            true,
            null,
            gameId,
            words,
            guessedGroups,
            timeLeft,
            errors,
            score
        );
    }


    /**
     * Creates an error login response.
     * 
     * @param errorMsg Descriptive error message
     * @return Instance with success=false, error=errorMsg and remaining fields null
     * @throws IllegalArgumentException if errorMsg is null or empty
     */
    public static LoginResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new LoginResponse(false, errorMsg, null, null, null, null, null, null);
    }


    /**
     * Helper function for quick validation of arguments passed to success().
     * 
     * @param gameId ID of the current game
     * @param words List of words in the current game
     * @throws IllegalArgumentException if gameId or words are malformed
     */
    private static void validateSuccess(Integer gameId, List<String> words) {
        final int MIN_ID = 0;
        final int MAX_ID = 911;
        if (gameId == null || gameId < MIN_ID || gameId > MAX_ID) {
            throw new IllegalArgumentException("gameId must be between 0 and 911");
        }

        if (words == null || words.size() != 16) {
            throw new IllegalArgumentException("words must contain 16 words");
        }
    }


    /**
     * Gets the game ID.
     *
     * @return Game ID or null if login failed
     */
    public Integer getGameId() { return this.gameId; }


    /**
     * Gets the list of words in the game.
     *
     * @return Defensive copy of words list or null if login failed
     */
    public List<String> getWords() { return this.words; }


    /**
     * Gets the list of already guessed groups.
     *
     * @return Defensive copy of guessed groups list or null
     */
    public List<List<String>> getGuessedGroups() { return this.guessedGroups; }


    /**
     * Gets the time remaining in the game.
     *
     * @return Time remaining in milliseconds or null if login failed
     */
    public Long getTimeLeft() { return this.timeLeft; }


    /**
     * Gets the number of errors made.
     *
     * @return Number of errors or null if login failed
     */
    public Integer getErrors() { return this.errors; }


    /**
     * Gets the score obtained.
     *
     * @return Score or null if login failed
     */
    public Integer getScore() { return this.score; }
}