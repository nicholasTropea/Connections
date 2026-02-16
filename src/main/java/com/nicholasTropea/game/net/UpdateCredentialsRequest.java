package com.nicholasTropea.game.net;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;

/**
 * Richiesta di modifica delle credenziali.
 * Riceve una {@link UpdateCredentialsResponse}.
 * 
 * JSON atteso:
 * <pre>{@code
 * {
 *    "operation" : "updateCredentials",
 *    "oldUsername" : "STRING",
 *    "newUsername" : "STRING",
 *    "oldPsw" : "STRING",
 *    "newPsw" : "STRING"
 * } 
 * }</pre>
 * 
 * Errori possibili: "oldPsw errata", "newUsername già registrato"...
 */
public class UpdateCredentialsRequest {
    /** Operazione effettuata */
    @SerializedName("operation")
    private final String operation = "updateCredentials";

    /** Attuale nome utente dell'account da cambiare */
    @SerializedName("oldUsername")
    private final String oldUsername;

    /** Attuale password dell'account da cambiare */
    @SerializedName("oldPsw")
    private final String oldPassword;

    /** Nuovo nome da impostare (opzionale) */
    @SerializedName("newUsername")
    private final String newUsername;

    /** Nuova password da impostare (opzionale, minimo 6 caratteri) */
    @SerializedName("newPsw")
    private final String newPassword;

    /**
     * Costruttore completo.
     * 
     * Crea una richiesta per cambio di credenziali.
     * 
     * @param oldUsername attuale nome utente dell'account da cambiare
     * @param oldPsw attuale password dell'account da cambiare
     * @param newUsername eventuale nuovo nome da impostare
     * @param newPassword eventuale nuova password da impostare
     */
    public UpdateCredentialsRequest(String oldUsername, String oldPassword, String newUsername, String newPassword) {
        // Check leggero
        validate(oldUsername, oldPassword, newUsername, newPassword);

        this.oldUsername = oldUsername.trim();
        this.oldPassword = oldPassword;
        this.newUsername = newUsername != null ? newUsername.trim() : "";
        this.newPassword = newPassword != null ? newPassword : "";
    }


    /**
     * Valida gli argomenti passati al costruttore.
     * 
     * @param oldUsername Nome attuale
     * @param oldPassword Password attuale
     * @param newUsername Nuovo nome
     * @param newPassword Nuova password
     * 
     * @throws IllegalArgumentException se uno dei parametri è malformato
     */
    private static void validate(String oldUsername, String oldPassword, String newUsername, String newPassword) {
        Objects.requireNonNull(oldUsername, "oldUsername is required");
        Objects.requireNonNull(oldPassword, "oldPassword is required");

        // Password o nome devono cambiare
        if (
            (newUsername == null || newUsername.trim().isEmpty()) &&
            (newPassword == null || newPassword.isEmpty())
        ) {
            throw new IllegalArgumentException("Either password, name or both must change");
        }

        if (oldUsername.trim().isEmpty()) throw new IllegalArgumentException("oldUsername cannot be empty");
        if (oldPassword.isEmpty()) throw new IllegalArgumentException("oldPassword cannot be empty");
        if (oldPassword.length() < 6) throw new IllegalArgumentException("OldPassword must be at least 6 characters long");
        if (newPassword != null && newPassword.length() < 6) throw new IllegalArgumentException("newPassword must be at least 6 characters long");
        if (newUsername != null && newUsername.trim().isEmpty()) throw new IllegalArgumentException("newUsername cannot be empty");
    }

    // Getters
    public String getOperation() { return this.operation; }
    public String getOldUsername() { return this.oldUsername; }
    public String getOldPassword() { return this.oldPassword; }
    public String getNewUsername() { return this.newUsername; }
    public String getNewPassword() { return this.newPassword; }
}