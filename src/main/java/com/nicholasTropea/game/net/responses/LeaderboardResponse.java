package com.nicholasTropea.game.net.responses;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import com.nicholasTropea.game.model.LeaderboardRecord;
import com.nicholasTropea.game.net.Response;

/**
 * Response to a {@link LeaderboardRequest}.
 * 
 * Expected JSON format:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING,
 *      "records" : List<LeaderboardRecord>
 * }
 * }</pre>
 * 
 * Possible errors: "username not registered", "user not logged in"
 */
public class LeaderboardResponse extends Response {

    /** Sorted list containing the requested players */
    @SerializedName("records")
    private final List<LeaderboardRecord> records;


    /**
     * Private constructor for creating leaderboard responses.
     *
     * @param success Whether the request was successful
     * @param error Error message if unsuccessful
     * @param records List of leaderboard records
     */
    private LeaderboardResponse(
        boolean success,
        String error,
        List<LeaderboardRecord> records
    ) {
        super("requestLeaderboard", success, error);
        this.records = records;
    }


    /**
     * Creates a successful leaderboard response.
     * 
     * @param records Records of the requested players
     * @return Instance with success=true and error=null
     * @throws IllegalArgumentException if records is null
     */
    public static LeaderboardResponse success(List<LeaderboardRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException(
                "records cannot be null, if no records should be returned, " +
                "return an empty list"
            );
        }

        return new LeaderboardResponse(true, null, records);
    }


    /**
     * Creates an error leaderboard response.
     * 
     * @param errorMsg Descriptive error message
     * @return Instance with success=false, error=errorMsg and remaining fields null
     * @throws IllegalArgumentException if errorMsg is null or empty
     */
    public static LeaderboardResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new LeaderboardResponse(false, errorMsg, null);
    }


    /**
     * Gets the leaderboard records.
     *
     * @return List of leaderboard records or null if request failed
     */
    public List<LeaderboardRecord> getRecords() { return this.records; }
}