package com.nicholasTropea.game.net.responses;

import com.google.gson.annotations.SerializedName;

/**
 * Risposta ad una richiesta di {@link UpdateCredentialsRequest}.
 * 
 * JSON atteso:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING
 * }
 * }</pre>
 * 
 * Errori possibili: "oldPsw non valida", "utente inesistente", "newName già registrato"
 */
public class UpdateCredentialsResponse {
    /** true se richiesta avvenuta con successo, false altrimenti */
    @SerializedName("success")
    private final boolean success;
        
    /** Messaggio d'errore (null se success=true) */
    @SerializedName("error")
    private final String error;
    
    /** Costruttore */
    private UpdateCredentialsResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    /**
     * Crea una risposta di successo.
     * 
     * @return istanza con success=true e error=null
     */
    public static UpdateCredentialsResponse success() {
        return new UpdateCredentialsResponse(true, null);
    }

    /**
     * Crea una risposta di errore.
     * 
     * @param errorMsg messaggio d'errore descrittivo
     * @return istanza con success=false, error=errorMsg
     * @throws IllegalArgumentException se errorMsg=null o vuoto
     */
    public static UpdateCredentialsResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new UpdateCredentialsResponse(false, errorMsg);
    }

    // Getters
    public boolean isSuccess() { return this.success; }
    public String getError() { return this.error; }
}