package com.nicholasTropea.game.model;

/**
 * Represents a player's position in the leaderboard.
 * 
 * Returned by a {@link LeaderboardResponse}.
 */
public class LeaderboardRecord {
    /** Player username. */
    private final String username;

    /** Player position in the leaderboard. */
    private final int position;

    /** Player total points in the leaderboard. */
    private final int points;
    
    /**
     * Creates a leaderboard record.
     *
     * @param username player username
     * @param position leaderboard position
     * @param points player total points
     * @throws IllegalArgumentException if username is empty/null or position is less than 1
     */
    public LeaderboardRecord(String username, int position, int points) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username cannot be empty or null");
        }
        if (position < 1) {
            throw new IllegalArgumentException("position cannot be less than 1");
        }

        this.username = username;
        this.position = position;
        this.points = points;
    }

    /** Gets the player username. */
    public String getUsername() { return this.username; }

    /** Gets the player position. */
    public int getPosition() { return this.position; }

    /** Gets the player total points. */
    public int getPoints() { return this.points; }
}