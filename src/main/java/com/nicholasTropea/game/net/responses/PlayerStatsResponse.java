package com.nicholasTropea.game.net.responses;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.model.MistakeHistogram;
import com.nicholasTropea.game.net.Response;

/**
 * Response to a {@link PlayerStatsRequest}.
 * 
 * Expected JSON format:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING,
 *      "solvedPuzzles" : INT,
 *      "failedPuzzles" : INT,
 *      "unfinishedPuzzles" : INT,
 *      "perfectPuzzles" : INT,
 *      "winRate" : FLOAT,
 *      "lossRate" : FLOAT,
 *      "currentStreak" : INT,
 *      "maxStreak" : INT,
 *      "histogram" : MistakeHistogram
 * }
 * }</pre>
 * 
 * Possible errors: "user not logged in"
 */
public class PlayerStatsResponse extends Response {

    /** Number of solved puzzles */
    @SerializedName("solvedPuzzles")
    private final Integer solved;
        
    /** Number of failed puzzles */
    @SerializedName("failedPuzzles")
    private final Integer failed;

    /** Number of unfinished puzzles */
    @SerializedName("unfinishedPuzzles")
    private final Integer unfinished;
        
    /** Number of puzzles solved without errors */
    @SerializedName("perfectPuzzles")
    private final Integer perfect;

    /** Win rate */
    @SerializedName("winRate")
    private final Float winRate;

    /** Loss rate */
    @SerializedName("lossRate")
    private final Float lossRate;

    /** Current win streak */
    @SerializedName("currentStreak")
    private final Integer currentStreak;    

    /** Maximum win streak achieved */
    @SerializedName("maxStreak")
    private final Integer maxStreak;

    /** Current histogram of the user's games */
    @SerializedName("histogram")
    private final MistakeHistogram histogram;


    /**
     * Private constructor for creating player stats responses.
     *
     * @param success Whether the request was successful
     * @param error Error message if unsuccessful
     * @param solved Number of solved puzzles
     * @param failed Number of failed puzzles
     * @param unfinished Number of unfinished puzzles
     * @param perfect Number of perfect puzzles
     * @param winRate Win rate
     * @param lossRate Loss rate
     * @param currentStreak Current win streak
     * @param maxStreak Maximum win streak
     * @param histogram Mistake histogram
     */
    private PlayerStatsResponse(
        boolean success,
        String error,
        Integer solved,
        Integer failed,
        Integer unfinished,
        Integer perfect,
        Float winRate,
        Float lossRate,
        Integer currentStreak,
        Integer maxStreak,
        MistakeHistogram histogram
    ) {
        super("requestPlayerStats", success, error);
        this.solved = solved;
        this.failed = failed;
        this.unfinished = unfinished;
        this.perfect = perfect;
        this.winRate = winRate;
        this.lossRate = lossRate;
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.histogram = histogram;
    }


    /**
     * Creates a successful player stats response.
     * 
     * @param solved Number of solved puzzles
     * @param failed Number of failed puzzles
     * @param unfinished Number of unfinished puzzles
     * @param perfect Number of perfect puzzles
     * @param winRate Win rate
     * @param lossRate Loss rate
     * @param currentStreak Current win streak
     * @param maxStreak Maximum win streak
     * @param histogram Mistake histogram
     * @return Instance with success=true and error=null
     * @throws IllegalArgumentException if puzzle/streak/rate parameters are negative
     *         or if maxStreak is less than currentStreak
     */
    public static PlayerStatsResponse success(
        Integer solved,
        Integer failed,
        Integer unfinished,
        Integer perfect,
        Float winRate,
        Float lossRate,
        Integer currentStreak,
        Integer maxStreak,
        MistakeHistogram histogram
    ) {
        if (
            solved < 0 || failed < 0 || unfinished < 0 ||
            perfect < 0 || winRate < 0 || lossRate < 0 ||
            currentStreak < 0 || maxStreak < 0 || maxStreak < currentStreak
        ) {
            throw new IllegalArgumentException(
                "puzzle, streak and rate parameters cannot be < 0, " +
                "maxStreak cannot be < currentStreak"
            );
        }

        return new PlayerStatsResponse(
            true, null, solved,
            failed, unfinished, perfect,
            winRate, lossRate, currentStreak,
            maxStreak, histogram
        );
    }


    /**
     * Creates an error player stats response.
     * 
     * @param errorMsg Descriptive error message
     * @return Instance with success=false, error=errorMsg and remaining fields null
     * @throws IllegalArgumentException if errorMsg is null or empty
     */
    public static PlayerStatsResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new PlayerStatsResponse(
            false,
            errorMsg,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }


    /**
     * Gets the number of solved puzzles.
     *
     * @return Number of solved puzzles or null if request failed
     */
    public Integer getSolvedPuzzles() { return this.solved; }


    /**
     * Gets the number of failed puzzles.
     *
     * @return Number of failed puzzles or null if request failed
     */
    public Integer getFailedPuzzles() { return this.failed; }


    /**
     * Gets the number of unfinished puzzles.
     *
     * @return Number of unfinished puzzles or null if request failed
     */
    public Integer getUnfinishedPuzzles() { return this.unfinished; }


    /**
     * Gets the number of perfect puzzles (solved without errors).
     *
     * @return Number of perfect puzzles or null if request failed
     */
    public Integer getPerfectPuzzles() { return this.perfect; }


    /**
     * Gets the win rate.
     *
     * @return Win rate or null if request failed
     */
    public Float getWinRate() { return this.winRate; }


    /**
     * Gets the loss rate.
     *
     * @return Loss rate or null if request failed
     */
    public Float getLossRate() { return this.lossRate; }


    /**
     * Gets the current win streak.
     *
     * @return Current win streak or null if request failed
     */
    public Integer getCurrentStreak() { return this.currentStreak; }


    /**
     * Gets the maximum win streak.
     *
     * @return Maximum win streak or null if request failed
     */
    public Integer getMaxStreak() { return this.maxStreak; }


    /**
     * Gets the mistake histogram. Not a copy, as it's disposable.
     *
     * @return Mistake histogram or null if request failed
     */
    public MistakeHistogram getHistogram() { return this.histogram; }
}