package com.nicholasTropea.game.net;

import com.nicholasTropea.game.net.requests.*;

import java.util.Objects;
import java.util.Scanner;

import com.google.gson.annotations.SerializedName;


/**
 * Enumeration for various types of requests
 */
public enum RequestTypes {
    REGISTER(1, "Register"),
    LOGIN(2, "Login"),
    LOGOUT(3, "Logout"),
    GAMEINFOREQUEST(4, "Request game information"),
    GAMESTATSREQUEST(5, "Request game stats"),
    LEADERBOARDREQUEST(6, "Request leaderboard"),
    PLAYERSTATSREQUEST(7, "Request player stats"),
    SUBMITPROPOSALREQUEST(8, "Submit a guess"),
    UPDATECREDENTIALSREQUEST(9, "Update credentials");

    private final int code;
    private final String action;


    RequestTypes(int code, String action) {
        this.code = code;
        this.action = action;
    }


    /**
     * Looks up a RequestTypes enum by its code
     *
     * @param code the code to look up
     * @return the corresponding RequestTypes
     * @throws IllegalArgumentException if no matching code is found
     */
    public static RequestTypes fromCode(int code) {
        for (RequestTypes type : RequestTypes.values()) {
            if (type.code == code) return type;
        }

        throw new IllegalArgumentException("Invalid code: " + code);
    }


    /**
     * Gets the integer code associated with this request type
     *
     * @return the code
     */
    public int getCode() { return this.code; }

    /**
     * Gets the human-readable action description for this request type
     *
     * @return the action description
     */
    public String getAction() { return this.action; }
}


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
     * Creates a new request instance based on the specified request type.
     * Invokes the static factory method of the appropriate request subclass.
     *
     * @param type the type of request to create
     * @return a new instance of the requested type
     */
    public static Request parseOption(RequestTypes type) {
        Scanner scan = new Scanner(System.in);
        
        switch (type) {
            case LOGIN -> return LoginRequest.createRequest(scan);
            case LOGOUT -> return LogoutRequest.createRequest(scan);
            case REGISTER -> return RegisterRequest.createRequest(scan);
            case GAMEINFOREQUEST -> return GameInfoRequest.createRequest(scan);
            case GAMESTATSREQUEST -> return GameStatsRequest.createRequest(scan);
            case PLAYERSTATSREQUEST -> return PlayerStatsRequest.createRequest(scan);
            case LEADERBOARDREQUEST -> return LeaderboardRequest.createRequest(scan);
            case SUBMITPROPOSALREQUEST -> {
                return SubmitProposalRequest.createRequest(scan);
            }
            case UPDATECREDENTIALSREQUEST -> {
                return UpdateCredentialsRequest.createRequest(scan);
            }
            default -> { // Unreachable
                throw new IllegalArgumentException("Unknown request type: " + type);
            }
        }
    }


    /**
     * Abstract factory method that subclasses implement to create new instances
     * by prompting for parameters
     *
     * @return a new instance of the request with user-provided parameters
     */
    public abstract Request createRequest();


    /**
     * Gets the operation type of this request
     *
     * @return the operation type
     */
    public String getOperation() { return this.operation; }
}