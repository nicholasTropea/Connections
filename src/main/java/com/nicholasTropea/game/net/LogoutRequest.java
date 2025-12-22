package com.nicholasTropea.game.net;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;

/**
 * Richiesta di logout di un giocatore.
 * 
 * JSON atteso:
 * {
 *    "operation" : "logout"
 * }
 */
public class LogoutRequest {
    @SerializedName("operation")
    private final String operation = "logout";

    // Costruttore
    public LogoutRequest() { ; }

    // Getters
    public String getOperation() { return this.operation; }
}