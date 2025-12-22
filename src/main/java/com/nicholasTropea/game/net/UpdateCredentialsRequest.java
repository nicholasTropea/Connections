package com.nicholasTropea.game.net;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;

/**
 * Richiesta di modifica delle credenziali di un giocatore.
 * 
 * JSON atteso:
 * {
 *    "operation" : "updateCredentials",
 *    "oldName" : "STRING",
 *    "newName" : "STRING",
 *    "oldPsw" : "STRING",
 *    "newPsw" : "STRING"
 * } 
 */
public class UpdateCredentialsRequest {
    @SerializedName("operation")
    private final String operation = "updateCredentials";

    @SerializedName("oldName")
    private final String oldName;

    @SerializedName("oldPsw")
    private final String oldPassword;

    @SerializedName("newName")
    private final String newName; // Vuoto se non cambiato

    @SerializedName("newPsw")
    private final String newPassword; // Vuoto se non cambiato

    /** Costruttore completo (nome + password). */
    public UpdateCredentialsRequest(String oldName, String oldPassword, String newName, String newPassword) {
        this.validate(oldName, oldPassword, newName, newPassword);

        this.oldName = oldName.trim();
        this.oldPassword = oldPassword;
        this.newName = newName != null ? newName.trim() : "";
        this.newPassword = newPassword != null ? newPassword : "";
    }

    /** Solo cambio nome (newPassword vuoto). */
    public UpdateCredentialsRequest(String oldName, String oldPassword, String newName) {
        this(oldName, oldPassword, newName, "");
    }

    /** Solo cambio password (newName vuoto). */
    public UpdateCredentialsRequest(String oldName, String oldPassword, String newPassword) {
        this(oldName, oldPassword, "", newPassword);
    }

    /**
     * Valida gli argomenti passati al costruttore
     * 
     * @param oldName Nome attuale
     * @param oldPassword Password attuale
     * @param newName Nuovo nome
     * @param newPassword Nuova password
     */
    private void validate(String oldName, String oldPassword, String newName, String newPassword) {
        Objects.requireNonNull(oldName, "Required oldName");
        Objects.requireNonNull(oldPassword, "Required oldPassword");

        // Password o nome devono cambiare
        if (
            (newName == null || newName.trim().isEmpty()) &&
            (newPassword == null || newPassword.isEmpty())
        ) {
            throw new IllegalArgumentException("Either password, name or both must change");
        }

        if (oldName.trim().isEmpty()) throw new IllegalArgumentException("Old name cannot be empty");
        if (oldPassword.isEmpty()) throw new IllegalArgumentException("Old password cannot be empty");
        if (oldPassword.length() < 6) throw new IllegalArgumentException("Old password must be at least 6 characters long");
        if (newPassword != null && newPassword.length() < 6) throw new IllegalArgumentException("New password must be at least 6 characters long");
        if (newName != null && newName.trim().isEmpty()) throw new IllegalArgumentException("New name cannot be empty");
    }

    // Getters
    public String getOperation() { return this.operation; }
    public String getOldName() { return this.oldName; }
    public String getOldPassword() { return this.oldPassword; }
    public String getNewName() { return this.newName; }
    public String getNewPassword() { return this.newPassword; }
}