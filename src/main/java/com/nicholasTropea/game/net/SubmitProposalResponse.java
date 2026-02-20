package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;

/**
 * Risposta ad una richiesta di {@link SubmitProposalRequest}.
 * 
 * JSON atteso:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING,
 *      "result" : BOOLEAN,
 *      "groupName" : STRING
 * }
 * }</pre>
 * 
 * Errori possibili: "utente non loggato", "parole non valide"
 */
public class SubmitProposalResponse {
    /** true se richiesta avvenuta con successo, false altrimenti */
    @SerializedName("success")
    private final boolean success;
        
    /** Messaggio d'errore (null se success=true) */
    @SerializedName("error")
    private final String error;

    /** Esito della proposta effettuata */
    @SerializedName("result")
    private final Boolean result;
        
    /** Nome del gruppo in caso di proposta corretta (null se result=false) */
    @SerializedName("groupName")
    private final String groupName;
    
    /** Costruttore */
    private SubmitProposalResponse(
        boolean success,
        String error,
        Boolean result,
        String groupName
    ) {
        this.success = success;
        this.error = error;
        this.result = result;
        this.groupName = groupName;
    }

    /**
     * Crea una risposta di successo.
     * 
     * @return istanza con success=true e error=null
     */
    public static SubmitProposalResponse success(boolean result, String groupName) {
        if (result && (groupName == null || groupName.trim().isEmpty())) {
            throw new IllegalArgumentException(
                "If result=true then groupName cannot be empty or null"
            );
        }

        if (!result && groupName != null) {
            throw new IllegalArgumentException(
                "If result=false then groupName must be null"
            );
        }
        
        return new SubmitProposalResponse(true, null, result, groupName);
    }

    /**
     * Crea una risposta di errore.
     * 
     * @param errorMsg messaggio d'errore descrittivo
     * @return istanza con success=false, error=errorMsg e restante null
     * @throws IllegalArgumentException se errorMsg=null o vuoto
     */
    public static SubmitProposalResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new SubmitProposalResponse(false, errorMsg, null, null);
    }

    // Getters
    public boolean isSuccess() { return this.success; }
    public String getError() { return this.error; }
    public Boolean getResult() { return this.result; }
    public String getGroupName() { return this.groupName; }
}