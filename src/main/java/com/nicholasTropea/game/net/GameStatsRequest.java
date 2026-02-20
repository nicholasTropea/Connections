package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;


/**
 * Richiesta delle statistiche di una partita da un giocatore.
 * 
 * JSON atteso:
 * {
 *    "operation" : "requestGameStats",
 *    "gameId" : INT,
 *    "current" : BOOLEAN
 * }
 */
public class GameStatsRequest extends Request {
    @SerializedName("gameId")
    private final Integer gameId; // null se current

    @SerializedName("current")
    private final Boolean current; // null se true

    /** Costruttore privato completo (id + current) */
    private GameStatsRequest(Integer gameId, Boolean current) {
        super("requestGameStats");

        if (gameId == null && (current == null || !current)) {
            throw new IllegalArgumentException(
                "Either gameId or current=true must be provided"
            );
        }
        
        if (gameId != null) {
            final int MIN_ID = 0;
            final int MAX_ID = 911;

            if (gameId < MIN_ID || gameId > MAX_ID) {
                throw new IllegalArgumentException("Game id must be between 0 and 911");
            }
        }

        this.gameId = gameId;
        this.current = current;
    }

    /** Partita specifica (solo game id) */
    public GameStatsRequest(int gameId) { this(gameId, null); }

    /** Partita corrente (solo current) */
    public GameStatsRequest(boolean current) { this(null, current); }

    // Getters
    public Integer getGameId() { return this.gameId; }
    public boolean isCurrent() { return Boolean.TRUE.equals(this.current); }
}