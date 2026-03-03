package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;


/**
 * Abstract base class for all response types in the game network protocol.
 * 
 * <p>Provides common fields for success/error handling that all responses share.
 * Subclasses should provide specific data fields and factory methods for creating
 * success and error responses appropriate to their type.
 * 
 * @see Request for the corresponding request base class
 */
public abstract class Response {
    /**
     * Operation identifier associated with this response.
     * It matches the originating request operation.
     */
    @SerializedName("operation")
    protected final String operation;

    /**
     * Indicates whether the request was successfully processed.
     * True if successful, false if an error occurred.
     */
    @SerializedName("success")
    protected final boolean success;

    /**
     * Error message describing what went wrong.
     * Null if success is true, contains an error description otherwise.
     */
    @SerializedName("error")
    protected final String error;


    /**
     * Constructs a Response with the specified success status and error message.
     * 
     * <p>This constructor is protected so only subclasses can create instances.
     * Subclasses should provide static factory methods (success/error) to create
     * appropriate response instances.
     *
     * @param operation Operation identifier associated with this response
     * @param success True if the request was successfully processed, false otherwise
     * @param error Error message (should be null if success is true)
     */
    protected Response(String operation, boolean success, String error) {
        this.operation = Objects.requireNonNull(operation, "Operation is required");
        this.success = success;
        this.error = error;
    }


    /**
     * Gets the operation identifier for this response.
     *
     * @return Operation identifier
     */
    public String getOperation() { return this.operation; }


    /**
     * Checks if the request was successfully processed.
     *
     * @return True if successful, false otherwise
     */
    public boolean isSuccess() { return this.success; }


    /**
     * Gets the error message if the request failed.
     *
     * @return Error message or null if successful
     */
    public String getError() { return this.error; }
}