package com.nicholasTropea.game.net;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;


/**
 * Richiesta di registrazione di un nuovo giocatore. 
 * 
 * JSON atteso:
 * {
 *    "operation" : "register",
 *    "username" : "STRING",
 *    "psw" : "STRING"
 * } 
 */
public class RegisterRequest extends Request {
    @SerializedName("username")
    private final String username;

    @SerializedName("psw")
    private final String password;

    // Costruttore
    public RegisterRequest(String username, String password) {
        super("register");

        this.username = Objects.requireNonNull(username, "Required username").trim();
        this.password = Objects.requireNonNull(password, "Required password");

        // Altre validazioni
        if (this.username.isEmpty()) throw new IllegalArgumentException("Username cannot be empty");
        if (this.password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
    }

    // Getters
    public String getuserName() { return this.username; }
    public String getPassword() { return this.password; }
}