package com.nicholasTropea.game.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Represents a registered player. */
public class Player {
    /** Unique player nickname. */
    private String nickname;

    /** Player password. */
    private String password;

    /** Player global score. */
    private int globalScore;

    /** Player win rate. */
    private float winRate;

    /** Player loss rate. */
    private float lossRate;

    /** Current streak of won games. */
    private int currentStreak;

    /** Highest streak ever reached. */
    private int maxStreak;

    /** Supported game result types. */
    public enum ResultType {
        WIN_0,
        WIN_1,
        WIN_2,
        WIN_3,
        LOST,
        NOT_FINISHED
    }

    /** Number of occurrences for each game result type. */
    private Map<ResultType, Integer> resultCounts;

    
    /**
     * Creates a new player instance.
     *
     * @param nickname player nickname
     * @param password player password
     * @throws NullPointerException if nickname or password is null
     * @throws IllegalArgumentException if nickname or password is blank
     */
    public Player(String nickname, String password) {
        this.nickname = requireNotBlank(nickname, "nickname");
        this.password = requireNotBlank(password, "password");
        this.globalScore = 0;
        this.winRate = 0.0f;
        this.lossRate = 0.0f;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.resultCounts = new HashMap<>();
    }


    /** Return the player's nickname */
    public String getNickname() { return this.nickname; }

    /** Return the player's password */
    public String getPassword() { return this.password; }

    /** Return the player's global score */
    public int getGlobalScore() { return this.globalScore; }

    /** Return the player's win rate */
    public float getWinRate() { return this.winRate; }

    /** Return the player's loss rate */
    public float getLossRate() { return this.lossRate; }

    /** Return the player's current streak */
    public int getCurrentStreak() { return this.currentStreak; }

    /** Return the player's max streak */
    public int getMaxStreak() { return this.maxStreak; }

    /** Return the player's results. */
    public Map<ResultType, Integer> getResultCount() {
        return Map.copyOf(this.resultCounts);
    }


    /** Set the player's nickname. */
    public void setNickname(String nickname) {
        this.nickname = requireNotBlank(nickname, "nickname");
    }

    /** Set the player's password. */
    public void setPassword(String password) {
        this.password = requireNotBlank(password, "password");
    }

    /** Set the player's global score */
    public void setGlobalScore(int newScore) { this.globalScore = newScore; }

    /** Set the player's win rate */
    public void setWinRate(float newWinRate) { this.winRate = newWinRate; }

    /** Set the player's loss rate */
    public void setLossRate(float newLossRate) { this.lossRate = newLossRate; }

    /** Set the player's current streak */
    public void setCurrentStreak(int newStreak) { this.currentStreak = newStreak; }

    /** Set the player's max streak */
    public void setMaxStreak(int newStreak) { this.maxStreak = newStreak; }

    /** Set the player's results. */
    public void setResultCount(Map<ResultType, Integer> newCount) {
        this.resultCounts = new HashMap<>(Objects.requireNonNull(newCount));
    }


    /**
     * Validates that a string is not null and not blank.
     *
     * @param value string value to validate
     * @param fieldName field name used in error messages
     * @return validated value
     */
    private static String requireNotBlank(String value, String fieldName) {
        String notNullValue = Objects.requireNonNull(value, fieldName + " cannot be null");
        if (notNullValue.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return notNullValue;
    }
}