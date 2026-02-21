package com.nicholasTropea.game.net.requests;

import com.nicholasTropea.game.net.Request;


/**
 * Request to log out a player from the game server.
 * 
 * This request terminates the current session for the authenticated player.
 * The server should invalidate any active game state associated with this connection.
 * 
 * Expected JSON format:
 * {@code
 * {
 *    "operation": "logout"
 * }
 * }
 * 
 * @see LogoutResponse for the server response
 */
public class LogoutRequest extends Request {
    /**
     * Constructs a LogoutRequest with the logout operation type.
     */
    public LogoutRequest() { super("logout"); }


    /**
     * Factory method to create a new LogoutRequest instance.
     * 
     * @return a new LogoutRequest
     */
    public static LogoutRequest createRequest() {
        return new LogoutRequest();
    }
}