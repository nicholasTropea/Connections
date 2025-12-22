package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;

/**
 * Richiesta delle statistiche del giocatore.
 * 
 * JSON atteso:
 * {
 *    "operation" : "requestPlayerStats"
 * }
 */
public class PlayerStatsRequest {
    @SerializedName("operation")
    private final String operation = "requestPlayerStats";

    /** Costruttore */
    public PlayerStatsRequest() { ; }

    // Getters
    public String getOperation() { return this.operation; }
}