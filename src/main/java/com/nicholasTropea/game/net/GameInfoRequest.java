package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;

/**
 * Richiesta dello stato di una partita da un giocatore.
 * 
 * JSON atteso:
 * {
 *    "operation" : "requestGameInfo",
 *    "gameId" : INT,
 *    "current" : BOOLEAN
 * }
 */
public class GameInfoRequest {
    @SerializedName("operation")
    private final String operation = "requestGameInfo";

    @SerializedName("gameId")
    private final Integer gameId; // null se current

    @SerializedName("current")
    private final Boolean current; // null o true

    /** Costruttore privato completo (id + current) */
    private GameInfoRequest(Integer gameId, Boolean current) {
        if (gameId == null && (current == null || !current)) {
            throw new IllegalArgumentException("Either gameId or current=true must be provided");
        }

        if (gameId != null) {
            final int MIN_ID = 0;
            final int MAX_ID = 911;

            if (gameId < MIN_ID || gameId > MAX_ID) throw new IllegalArgumentException("Game id must be between 0 and 911");
        }

        this.gameId = gameId;
        this.current = current;
    }

    /** Partita specifica (solo game id) */
    public GameInfoRequest(int gameId) { this(gameId, null); }

    /** Partita corrente (solo current) */
    public GameInfoRequest(boolean current) { this(null, current); }

    // Getters
    public String getOperation() { return this.operation; }
    public Integer getGameId() { return this.gameId; }
    public boolean getCurrent() { return Boolean.TRUE.equals(this.current); }
}