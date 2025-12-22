package com.nicholasTropea.game.net;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;

/**
 * Richiesta di login di un giocatore.
 * 
 * JSON atteso:
 * {
 *    "operation" : "login",
 *    "username" : "STRING",
 *    "psw" : "STRING"
 * } 
 */
public class LoginRequest {
    @SerializedName("operation")
    private final String operation = "login";

    @SerializedName("username")
    private final String username;

    @SerializedName("psw")
    private final String password;

    // Costruttore
    public LoginRequest(String username, String password) {
        this.username = Objects.requireNonNull(username, "Required username").trim();
        this.password = Objects.requireNonNull(password, "Required password");

        // Altre validazioni
        if (this.username.isEmpty()) throw new IllegalArgumentException("Username cannot be empty");
        if (this.password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
    }

    // Getters
    public String getOperation() { return this.operation; }
    public String getUsername() { return this.username; }
    public String getPassword() { return this.password; }
}