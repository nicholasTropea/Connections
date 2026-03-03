package com.nicholasTropea.game.net.responses;

import com.nicholasTropea.game.net.Response;

/**
 * Response to a {@link RegisterRequest}.
 * 
 * Expected JSON format:
 * <pre>{@code
 * {
 *    "success" : BOOLEAN,
 *    "error" : STRING
 * }
 * }</pre>
 * 
 * Possible errors: "username already registered"
 * 
 * @see LoginResponse for the login format
 */
public class RegisterResponse extends Response {


    /**
     * Private constructor for creating register responses.
     *
     * @param success Whether the registration was successful
     * @param error Error message if unsuccessful
     */
    private RegisterResponse( boolean success, String error) {
        super("register", success, error);
    }


    /**
     * Creates a successful registration response.
     * 
     * @return Instance with success=true and error=null
     */
    public static RegisterResponse success() {
        return new RegisterResponse(true, null);
    }


    /**
     * Creates an error registration response.
     * 
     * @param errorMsg Descriptive error message
     * @return Instance with success=false and error=errorMsg
     * @throws IllegalArgumentException if errorMsg is null or empty
     */
    public static RegisterResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new RegisterResponse(false, errorMsg);
    }
}