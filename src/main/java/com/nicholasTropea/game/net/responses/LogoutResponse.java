package com.nicholasTropea.game.net.responses;

import com.google.gson.annotations.SerializedName;

/**
 * Risposta ad una richiesta di {@link LogoutRequest}.
 * 
 * JSON atteso:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING
 * }
 * }</pre>
 * 
 * Errori possibili: "utente non loggato"
 */
public class LogoutResponse {
    /** true se richiesta avvenuta con successo, false altrimenti */
    @SerializedName("success")
    private final boolean success;

    /** Messaggio d'errore (null se success=true) */
    @SerializedName("error")
    private final String error;

    /** Costruttore privato */
    private LogoutResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    /**
     * Crea una risposta di successo.
     * 
     * @return istanza con success=true e error=null
     */
    public static LogoutResponse success() {
        return new LogoutResponse(true, null);
    }

    /**
     * Crea una risposta di errore.
     * 
     * @param errorMsg messaggio d'errore descrittivo
     * @return istanza con success=false, error=errorMsg
     * @throws IllegalArgumentException se errorMsg=null o vuoto
     */
    public static LogoutResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new LogoutResponse(false, errorMsg);
    }

    // Getters
    public boolean isSuccess() { return this.success; }
    public String getError() { return this.error; }
}