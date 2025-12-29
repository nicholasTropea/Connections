package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;

/**
 * Richiesta dello stato di una partita da un giocatore.
 * Riceve una {@link GameInfoResponse}.
 * 
 * JSON atteso:
 * <pre>{@code
 * {
 *    "operation" : "requestGameInfo",
 *    "gameId" : INT,
 *    "current" : BOOLEAN
 * }
 * }</pre>
 * 
 * Errori possibili: "gameId inesistente"
 */
public class GameInfoRequest {
    /** Operazione effettuata */
    @SerializedName("operation")
    private final String operation = "requestGameInfo";

    /** Id della partita (null se current=true) */
    @SerializedName("gameId")
    private final Integer gameId; // null se current

    /** True se richiede informazioni sulla partita corrente, null altrimenti */
    @SerializedName("current")
    private final Boolean current;

    /**
     * Costruttore privato completo.
     * @param gameId id della partita
     * @param current true se partita corrente, null altrimenti
     */
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

    /**
     * Costruttore per partita specifica (solo game id)
     * 
     * @param gameId id della partita
     */
    public GameInfoRequest(int gameId) { this(gameId, null); }

    /**
     * Costruttore per partita corrente (solo current)
     * 
     * @param current flag per partita corrente 
     */
    public GameInfoRequest(boolean current) { this(null, current); }

    // Getters
    public String getOperation() { return this.operation; }
    public Integer getGameId() { return this.gameId; }
    public boolean getCurrent() { return Boolean.TRUE.equals(this.current); }
}