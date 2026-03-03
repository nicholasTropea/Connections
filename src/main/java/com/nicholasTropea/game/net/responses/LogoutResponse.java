package com.nicholasTropea.game.net.responses;

import com.nicholasTropea.game.net.Response;

/**
 * Response to a {@link LogoutRequest}.
 * 
 * Expected JSON format:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING
 * }
 * }</pre>
 * 
 * Possible errors: "user not logged in"
 */
public class LogoutResponse extends Response {


    /**
     * Private constructor for creating logout responses.
     *
     * @param success Whether the logout was successful
     * @param error Error message if unsuccessful
     */
    private LogoutResponse(boolean success, String error) {
        super("logout", success, error);
    }


    /**
     * Creates a successful logout response.
     * 
     * @return Instance with success=true and error=null
     */
    public static LogoutResponse success() {
        return new LogoutResponse(true, null);
    }


    /**
     * Creates an error logout response.
     * 
     * @param errorMsg Descriptive error message
     * @return Instance with success=false and error=errorMsg
     * @throws IllegalArgumentException if errorMsg is null or empty
     */
    public static LogoutResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new LogoutResponse(false, errorMsg);
    }
}