package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;

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
public class LeaderboardRequest {
    @SerializedName("operation")
    private final String operation = "requestLeaderboard";

    @SerializedName("playerName")
    private final String playerUsername;

    @SerializedName("topPlayers")
    private final Integer kTopUsers;

    @SerializedName("all")
    private final Boolean all;

    /** Costruttore privato completo */
    private LeaderboardRequest(String playerUsername, Integer kTopUsers, Boolean all) {
        if (
            (playerUsername == null || playerUsername.trim().isEmpty()) &&
            (kTopUsers == null || kTopUsers == 0) &&
            (all == null || !all)
        ) {
            throw new IllegalArgumentException("Either playerUsername, kTopUsers > 0 or all = true must be provided");
        }

        this.playerUsername = playerUsername;
        this.kTopUsers = kTopUsers;
        this.all = all;
    }

    /** Tutti i giocatori (solo all) */
    public LeaderboardRequest(boolean all) {
        if (!all) throw new IllegalArgumentException("Parameter all must be true to request full leaderboard");
        this(null, null, all);
    }

    /** K top utenti (solo kTopUsers) */
    public LeaderboardRequest(int kTopUsers) {
        if (kTopUsers <= 0) throw new IllegalArgumentException("kTopUsers must be > 0");
        this(null, kTopUsers, null);
    }

    /** Singolo utente (solo playerUsername) */
    public LeaderboardRequest(String playerUsername) {
        if (playerUsername == null || playerUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("playerUsername is required");
        }

        this(playerUsername, null, null);
    }

    // Getters
    public String getOperation() { return this.operation; }
    public String getPlayerUsername() { return this.playerUsername; }
    public Integer getKTopPlayers() { return this.kTopUsers; }
    public boolean isAll() { return Boolean.TRUE.equals(this.all); }
}