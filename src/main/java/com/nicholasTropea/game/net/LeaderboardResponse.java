package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import com.nicholasTropea.game.model.LeaderboardRecord;

/**
 * Risposta ad una richiesta di {@link LeaderboardRequest}.
 * 
 * JSON atteso:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING,
 *      "records" : List<LeaderboardRecord>
 * }
 * }</pre>
 * 
 * Errori possibili: "username non registrato", "utente non loggato"
 */
public class LeaderboardResponse {
    /** true se richiesta avvenuta con successo, false altrimenti */
    @SerializedName("success")
    private final boolean success;

    /** Messaggio d'errore (null se success=true) */
    @SerializedName("error")
    private final String error;

    /** Lista ordinata contenente i giocatori richiesti */
    @SerializedName("records")
    private final List<LeaderboardRecord> records;

    /** Costruttore privato */
    private LeaderboardResponse(
        boolean success,
        String error,
        List<LeaderboardRecord> records
    ) {
        this.success = success;
        this.error = error;
        this.records = records;
    }

    /**
     * Crea una risposta di successo.
     * 
     * @param records record dei giocatori richiesti
     * 
     * @return istanza con success=true e error=null
     */
    public static LeaderboardResponse success(List<LeaderboardRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException(
                "records cannot be null, if no records should be returned, 
                return an empty list"
            );
        }

        return new LeaderboardResponse(true, null, records);
    }

    /**
     * Crea una risposta di errore.
     * 
     * @param errorMsg messaggio d'errore descrittivo
     * @return istanza con success=false, error=errorMsg e restante=null
     * @throws IllegalArgumentException se errorMsg=null o vuoto
     */
    public static LeaderboardResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new LeaderboardResponse(false, errorMsg, null);
    }

    // Getters
    public boolean isSuccess() { return this.success; }
    public String getError() { return this.error; }
    public List<LeaderboardRecord> getRecords() { return this.records; }
}