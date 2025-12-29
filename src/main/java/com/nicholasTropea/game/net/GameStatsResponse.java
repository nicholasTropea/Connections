package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;

/**
 * Risposta ad una richiesta di {@link GameStatsRequest}.
 * 
 * JSON atteso:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING,
 *      "active" : BOOLEAN,
 *      "timeLeft" : LONG,
 *      "activePlayers" : INT,
 *      "finishedPlayers" : INT,
 *      "wonPlayers" : INT,
 *      "totalPlayers" : INT,
 *      "averageScore" : LONG
 * }
 * }</pre>
 * 
 * Errori possibili: "id inesistente"
 */
public class GameStatsResponse {
    /** true se richiesta avvenuta con successo, false altrimenti */
    @SerializedName("success")
    private final boolean success;

    /** Messaggio d'errore (null se success=true) */
    @SerializedName("error")
    private final String error;

    /** Stato della partita (true se partita in corso, false altrimenti) */
    @SerializedName("active")
    private final boolean active;

    /** Tempo rimanente della partita in millisecondi (null se active=false) */
    @SerializedName("timeLeft")
    private final Long timeLeft;

    /** Numero di giocatori con la partita ancora in corso (null se active=false) */
    @SerializedName("activePlayers")
    private final Integer activePlayers;

    /** Numero di giocatori che hanno concluso la partita */
    @SerializedName("finishedPlayers")
    private final Integer finishedPlayers;

    /** Numero di giocatori che hanno concluso la partita con una vittoria */
    @SerializedName("wonPlayers")
    private final Integer wonPlayers;

    /** Numero di giocatori che hanno partecipato alla partita (null se active=true) */
    @SerializedName("totalPlayers")
    private final Integer totalPlayers;

    /** Punteggio medio ottenuto dai giocatori (null se active=true) */
    @SerializedName("averageScore")
    private final Long averageScore;

    /** Costruttore privato */
    private GameStatsResponse(
        boolean success,
        String error,
        boolean active,
        Long timeLeft,
        Integer activePlayers,
        Integer finishedPlayers,
        Integer wonPlayers,
        Integer totalPlayers,
        Long averageScore
    ) {
        this.success = success;
        this.error = error;
        this.active = active;
        this.timeLeft = timeLeft;
        this.activePlayers = activePlayers;
        this.finishedPlayers = finishedPlayers;
        this.wonPlayers = wonPlayers;
        this.totalPlayers = totalPlayers;
        this.averageScore = averageScore;
    }

    /**
     * Crea una risposta di successo.
     * Se active == true: timeLeft e activePlayers != null, averageScore e totalPlayers == null
     * Se active == false: averageScore e totalPlayers != null, timeLeft e activePlayers == null
     * 
     * @param active true se la partita non è terminata, false altrimenti
     * @param timeLeft tempo rimanente della partita in millisecondi
     * @param activePlayers numero di giocatori con la partita ancora in corso
     * @param finishedPlayers numero di giocatori che hanno concluso la partita
     * @param wonPlayers numero di giocatori che hanno concluso la partita con una vittoria
     * @param totalPlayers numero di giocatori che hanno partecipato alla partita
     * @param averageScore punteggio medio ottenuto dai giocatori
     * 
     * @return istanza con success=true e error=null
     */
    public static GameStatsResponse success(
        boolean active,
        Long timeLeft,
        Integer activePlayers,
        Integer finishedPlayers,
        Integer wonPlayers,
        Integer totalPlayers,
        Long averageScore
    ) {
        if (finishedPlayers == null || wonPlayers == null) {
            throw new IllegalArgumentException("finishedPlayers and wonPlayers cannot be null");
        }

        if (active) {
            return new GameStatsResponse(
                true,               // success
                null,               // error
                true,               // active
                timeLeft,           
                activePlayers,
                finishedPlayers,
                wonPlayers,
                null,               // totalPlayers
                null                // averageScore
            );
        }

        return new GameStatsResponse(
            true,                   // success
            null,                   // error
            false,                  // active
            null,                   // timeLeft
            null,                   // activePlayers
            finishedPlayers,
            wonPlayers,
            totalPlayers,
            averageScore
        );
    }

    /**
     * Crea una risposta di errore.
     * 
     * @param errorMsg messaggio d'errore descrittivo
     * @return istanza con success=false, error=errorMsg e restante=null
     * @throws IllegalArgumentException se errorMsg=null o vuoto
     */
    public static GameStatsResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new GameStatsResponse(false, errorMsg, false, null, null, null, null, null, null);
    }

    // Getters
    public boolean isSuccess() { return this.success; }
    public String getError() { return this.error; }
    public boolean isActive() { return this.active; }
    public Long getTimeLeft() { return this.timeLeft; }
    public Integer getActivePlayers() { return this.activePlayers; }
    public Integer getFinishedPlayers() { return this.finishedPlayers; }
    public Integer getWonPlayers() { return this.wonPlayers; }
    public Integer getTotalPlayers() { return this.totalPlayers; }
    public Integer getAverageScore() { return this.averageScore; }
}