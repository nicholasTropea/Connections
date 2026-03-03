package com.nicholasTropea.game.net.responses;

import com.nicholasTropea.game.net.Response;

/**
 * Response to an {@link UpdateCredentialsRequest}.
 * 
 * Expected JSON format:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING
 * }
 * }</pre>
 * 
 * Possible errors: "oldPsw not valid", "user not found", "newName already registered"
 */
public class UpdateCredentialsResponse extends Response {


    /**
     * Private constructor for creating update credentials responses.
     *
     * @param success Whether the update was successful
     * @param error Error message if unsuccessful
     */
    private UpdateCredentialsResponse(boolean success, String error) {
        super("updateCredentials", success, error);
    }


    /**
     * Creates a successful update credentials response.
     * 
     * @return Instance with success=true and error=null
     */
    public static UpdateCredentialsResponse success() {
        return new UpdateCredentialsResponse(true, null);
    }


    /**
     * Creates an error update credentials response.
     * 
     * @param errorMsg Descriptive error message
     * @return Instance with success=false and error=errorMsg
     * @throws IllegalArgumentException if errorMsg is null or empty
     */
    public static UpdateCredentialsResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new UpdateCredentialsResponse(false, errorMsg);
    }
}