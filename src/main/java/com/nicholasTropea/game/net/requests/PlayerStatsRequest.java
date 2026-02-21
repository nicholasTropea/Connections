package com.nicholasTropea.game.net.requests;

import com.nicholasTropea.game.net.Request;


/**
 * Request for the stats of the player.
 * 
 * Expected JSON format:
 * {@code
 * {
 *    "operation": "requestPlayerStats"
 * }
 * }
 * 
 * @see PlayerStatsResponse for the server response
 */
public class PlayerStatsRequest extends Request {
    /**
     * Constructs a PlayerStatsRequest with the requestPlayerStats operation type.
     */
    public PlayerStatsRequest() { super("requestPlayerStats"); }


    /**
     * Factory method to create a new PlayerStatsRequest instance.
     * 
     * @return a new PlayerStatsRequest
     */
    public static PlayerStatsRequest createRequest() {
        return new PlayerStatsRequest();
    }
}