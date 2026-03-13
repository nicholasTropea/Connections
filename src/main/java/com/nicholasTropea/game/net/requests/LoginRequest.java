package com.nicholasTropea.game.net.requests;

import java.util.Objects;
import java.util.Scanner;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;


/**
 * Concrete request for authenticating a user.
 * Receives a {@link LoginResponse}.
 * 
 * <p>Expected JSON format:
 * <pre>{@code
 * {
 *    "operation": "login",
 *    "username": "STRING",
 *    "psw": "STRING"
 *    "udpPort": INT
 * }
 * }</pre>
 * 
 * <p>Possible errors: "incorrect password", "username not registered"
 */
public class LoginRequest extends Request {
    /**
     * The username of the account to log into.
     */
    @SerializedName("username")
    private final String username;

    /**
     * The password of the account to log into.
     */
    @SerializedName("psw")
    private final String password;

    /**
     * Optional UDP port used by the client to receive async notifications.
     */
    @SerializedName("udpPort")
    private final Integer udpPort;


    /**
     * Constructs a login request with the provided credentials.
     * Validates that username is not empty and password meets minimum length requirements.
     * 
     * @param username The username of the account (must not be empty)
     * @param password The password of the account (must be at least 6 characters)
     * @throws NullPointerException if either username or password is null
     * @throws IllegalArgumentException if username is empty or password is too short
     */
    public LoginRequest(String username, String password) {
        this(username, password, null);
    }


    /**
     * Constructs a login request with credentials and optional UDP port.
     *
     * @param username account username
     * @param password account password
     * @param udpPort UDP port for asynchronous notifications, optional
     */
    public LoginRequest(String username, String password, Integer udpPort) {
        super("login");

        this.username = Objects.requireNonNull(username, "Username is required").trim();
        this.password = Objects.requireNonNull(password, "Password is required");
        if (this.username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (this.password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (udpPort != null && (udpPort < 1 || udpPort > 65535)) {
            throw new IllegalArgumentException("udpPort must be between 1 and 65535");
        }

        this.udpPort = udpPort;
    }


    /**
     * Prompts the user to enter login credentials (username and password).
     * Validates input and re-prompts on blank entries.
     * 
     * @param scan The Scanner to read user input from
     * @return A new LoginRequest with the entered credentials
     * @throws IllegalArgumentException if credentials don't meet validation requirements
     */
    public static LoginRequest createRequest(Scanner scan) {
        String username = getValidInput(scan, "Username");
        String password = getValidInput(scan, "Password");

        return new LoginRequest(username, password);
    }


    /**
     * Prompts the user to enter information of a specified type.
     * Validates that the input is not empty and re-prompts on blank input.
     * 
     * @param scan The Scanner to read user input from
     * @param inputType The type of input being requested (e.g., "Username", "Password")
     * @return The entered input as a non-empty String
     */
    private static String getValidInput(Scanner scan, String inputType) {
        String input = null;

        do {
            if (input != null) {
                System.out.println(inputType + " can't be empty.");
            }

            System.out.print("Enter the player " + inputType + ":");
            input = scan.nextLine().trim();
        } while (input.isEmpty());

        return input;
    }


    // Getters
    /**
     * Gets the username for this login request.
     * 
     * @return The username
     */
    public String getUsername() { return this.username; }


    /**
     * Gets the password for this login request.
     * 
     * @return The password
     */
    public String getPassword() { return this.password; }


    /**
     * Gets the optional UDP port used for async notifications.
     *
     * @return UDP port or null if not provided
     */
    public Integer getUdpPort() { return this.udpPort; }
}