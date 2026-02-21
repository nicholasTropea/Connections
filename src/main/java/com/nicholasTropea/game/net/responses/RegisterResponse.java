package com.nicholasTropea.game.net.responses;

import com.google.gson.annotations.SerializedName;

/**
 * Risposta ad una richiesta di {@link RegisterRequest}.
 * 
 * JSON atteso:
 * <pre>{@code
 * {
 *    "success" : BOOLEAN,
 *    "error" : STRING
 * }
 * }</pre>
 * 
 * Errori possibili: "username già registrato"
 * 
 * @see LoginResponse per il formato di login
 */
public class RegisterResponse {
    /** true se registrazione avvenuta con successo, false altrimenti */
    @SerializedName("success")
    private final boolean success;

    /** Messaggio d'errore (null se success=true) */
    @SerializedName("error")
    private final String error;

    /** Costruttore privato */
    private RegisterResponse( boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    /**
     * Crea una risposta di successo per registrazione completata.
     * 
     * @return istanza con success=true e error=null
     */
    public static RegisterResponse success() {
        return new RegisterResponse(true, null);
    }

    /**
     * Crea una risposta di errore per registrazione fallita.
     * 
     * @param errorMsg messaggio d'errore descrittivo
     * @return istanza con success=false e error=errorMsg
     * @throws IllegalArgumentException se errorMsg=null o vuoto
     */
    public static RegisterResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new RegisterResponse(false, errorMsg);
    }

    // Getters
    public boolean isSuccess() { return this.success; }
    public String getError() { return this.error; }
}