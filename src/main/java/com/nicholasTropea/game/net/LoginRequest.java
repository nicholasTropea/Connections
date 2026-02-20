package com.nicholasTropea.game.net;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;


/**
 * Richiesta di login di un giocatore.
 * Riceve una {@link LoginResponse}.
 * 
 * JSON atteso:
 * <pre<{@code
 * {
 *    "operation" : "login",
 *    "username" : "STRING",
 *    "psw" : "STRING"
 * }
 * }</pre>
 * 
 * Errori possibili: "psw errata", "username non registrato"
 */
public class LoginRequest extends Request {
    /** Nome utente dell'account in cui loggarsi */
    @SerializedName("username")
    private final String username;

    /** Password dell'account in cui loggarsi */
    @SerializedName("psw")
    private final String password;

    /**
     * Costruttore.
     * 
     * @param username nome utente dell'account in cui loggarsi
     * @param password password dell'account in cui loggarsi
     */
    public LoginRequest(String username, String password) {
        super("login");

        this.username = Objects.requireNonNull(username, "Required username").trim();
        this.password = Objects.requireNonNull(password, "Required password");

        // Altre validazioni
        if (this.username.isEmpty()) throw new IllegalArgumentException("Username cannot be empty");
        if (this.password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
    }

    // Getters
    public String getUsername() { return this.username; }
    public String getPassword() { return this.password; }
}