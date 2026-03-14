package com.nicholasTropea.game.net;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;


/**
 * Abstract factory class for creating and managing different types of requests
 */
public abstract class Request {

    /**
     * The operation type that this request represents
     */
    @SerializedName("operation")
    protected final String operation;


    /**
     * Constructs a Request with the specified operation type
     *
     * @param operation the operation type (must not be null)
     * @throws NullPointerException if operation is null
     */
    public Request(String operation) {
        this.operation = Objects.requireNonNull(operation);
    }


    /**
     * Factory method that subclasses implement to create new instances
     * by prompting for parameters
     *
     * @return a new instance of the request with user-provided parameters
     */
    public static Request createRequest() { return null; }


    /**
     * Gets the operation type of this request
     *
     * @return the operation type
     */
    public String getOperation() { return this.operation; }
}