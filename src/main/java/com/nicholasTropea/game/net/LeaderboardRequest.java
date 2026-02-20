package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;


/**
 * Richiesta di tutta o parte della classifica globale da un giocatore.
 * 
 * JSON atteso:
 * {
 *    "operation" : "requestLeaderboard",
 *    "playerName" : STRING,
 *    "topPlayers" : INT,
 *    "all" : BOOLEAN
 * }
 */
public class LeaderboardRequest extends Request {
    @SerializedName("playerName")
    private final String playerUsername;

    @SerializedName("topPlayers")
    private final Integer kTopUsers;

    @SerializedName("all")
    private final Boolean all;

    /** Costruttore privato completo */
    private LeaderboardRequest(String playerUsername, Integer kTopUsers, Boolean all) {
        super("requestLeaderboard");

        if (
            (playerUsername == null || playerUsername.trim().isEmpty()) &&
            (kTopUsers == null || kTopUsers <= 0) &&
            (all == null || !all)
        ) {
            throw new IllegalArgumentException(
                "Either playerUsername, kTopUsers > 0 or all = true must be provided"
            );
        }

        this.playerUsername = playerUsername;
        this.kTopUsers = kTopUsers;
        this.all = all;
    }

    /** Tutti i giocatori (solo all) */
    public LeaderboardRequest(boolean all) { this(null, null, all); }

    /** K top utenti (solo kTopUsers) */
    public LeaderboardRequest(int kTopUsers) { this(null, kTopUsers, null); }

    /** Singolo utente (solo playerUsername) */
    public LeaderboardRequest(String playerUsername) { this(playerUsername, null, null); }

    // Getters
    public String getPlayerUsername() { return this.playerUsername; }
    public Integer getKTopPlayers() { return this.kTopUsers; }
    public boolean isAll() { return Boolean.TRUE.equals(this.all); }
}