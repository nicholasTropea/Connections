package com.nicholasTropea.game.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nicholasTropea.game.model.PlayerGameState;

/**
 * Manages active player game sessions in a thread-safe manner.
 *
 * <p>Each active session maps a userId to their current game state.
 * Sessions are created on login and removed on logout or disconnect.
 */
public class SessionManager {
    /** Active sessions indexed by userId. */
    private final Map<Integer, PlayerGameState> activeSessions;


    /**
     * Creates a new session manager.
     */
    public SessionManager() {
        this.activeSessions = new ConcurrentHashMap<>();
    }


    /**
     * Creates a new session for a player.
     *
     * @param userId player user identifier
     * @param gameState initial game state for this session
     * @return error message if user already logged in, null on success
     */
    public String createSession(int userId, PlayerGameState gameState) {
        if (this.activeSessions.containsKey(userId)) {
            return "connection already logged in";
        }

        this.activeSessions.put(userId, gameState);
        return null;
    }


    /**
     * Gets the game state for an active session.
     *
     * @param userId player user identifier
     * @return game state or null if no active session
     */
    public PlayerGameState getSession(int userId) {
        return this.activeSessions.get(userId);
    }


    /**
     * Removes an active session.
     *
     * @param userId player user identifier
     */
    public void removeSession(int userId) {
        this.activeSessions.remove(userId);
    }


    /**
     * Checks if a user has an active session.
     *
     * @param userId player user identifier
     * @return true if session exists
     */
    public boolean hasSession(int userId) {
        return this.activeSessions.containsKey(userId);
    }


    /**
     * Gets the total number of active sessions.
     *
     * @return active session count
     */
    public int getActiveSessionCount() {
        return this.activeSessions.size();
    }
}
