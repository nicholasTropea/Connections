package com.nicholasTropea.game.net.requests;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;
import java.util.Scanner;


/**
 * Concrete request for retrieving leaderboard data from the server.
 * Supports three types of queries:
 * <ul>
 *   <li>Entire leaderboard (all players)</li>
 *   <li>Top k players by score</li>
 *   <li>Single player's ranking</li>
 * </ul>
 * 
 * <p>Expected JSON format:
 * <pre>
 * {
 *    "operation": "requestLeaderboard",
 *    "playerName": STRING,     // Only for single player query
 *    "topPlayers": INT,        // Only for top-k query
 *    "all": BOOLEAN            // Only for full leaderboard query
 * }
 * </pre>
 */
public class LeaderboardRequest extends Request {
    @SerializedName("playerName")
    private final String playerUsername;

    @SerializedName("topPlayers")
    private final Integer kTopUsers;

    @SerializedName("all")
    private final Boolean all;


    /**
     * Private constructor enforcing at least one parameter is specified.
     * Exactly one of the three parameters should be non-null and valid.
     * 
     * @param playerUsername The username to query (if querying single player)
     * @param kTopUsers The number of top users to retrieve (if querying top-k)
     * @param all Whether to retrieve all players (if querying entire leaderboard)
     * @throws IllegalArgumentException if all parameters are null or invalid
     */
    private LeaderboardRequest(String playerUsername, Integer kTopUsers, Boolean all) {
        super("requestLeaderboard");

        if (
            (playerUsername == null || playerUsername.trim().isEmpty()) &&
            (kTopUsers == null || kTopUsers <= 0) &&
            (all == null || !all)
        ) {
            throw new IllegalArgumentException(
                "Either playerUsername, kTopUsers > 0 or all = true must be provided"
            );
        }

        this.playerUsername = playerUsername;
        this.kTopUsers = kTopUsers;
        this.all = all;
    }


    /**
     * Factory method that prompts the user to select a leaderboard query type
     * and collects the necessary parameters interactively.
     * 
     * @param scan The Scanner to read user input from
     * @return A new LeaderboardRequest configured based on user selection
     */
    public static LeaderboardRequest createRequest(Scanner scan) {
        int selection = getSelection(scan);

        switch (selection) {
            case 1:
                return new LeaderboardRequest(true);
            case 2:
                int k = getValidK(scan);
                return new LeaderboardRequest(k);
            case 3:
                String username = getValidUsername(scan);
                return new LeaderboardRequest(username);
            default:
                throw new IllegalStateException("Invalid selection: " + selection);
        }
    }


    /**
     * Displays the leaderboard menu and prompts the user to select an option.
     * Validates input and re-prompts on invalid selection (must be 1, 2, or 3).
     * 
     * @param scan The Scanner to read user input from
     * @return The selected option (1, 2, or 3)
     */
    private static int getSelection(Scanner scan) {
        System.out.println(
            "Choose the leaderboard option:\n" +
            "\t- 1: Whole leaderboard\n" +
            "\t- 2: Top k leaderboard\n" +
            "\t- 3: Single player position in the leaderboard\n"
        );

        int selection = -1;

        do {
            if (selection != -1) {
                System.out.println("Invalid option: " + selection);
            }

            try {
                selection = Integer.parseInt(scan.nextLine().trim());
            } catch (NumberFormatException e) {
                selection = -1;
            }
        } while (selection < 1 || selection > 3);

        return selection;
    }


    /**
     * Prompts the user to enter the number of top players to display.
     * Validates that the input is a positive integer and re-prompts on error.
     * 
     * @param scan The Scanner to read user input from
     * @return A positive integer representing the number of top players to retrieve
     */
    private static int getValidK(Scanner scan) {
        int k = -1;

        do {
            if (k != -1) {
                System.out.println(
                    "Invalid k: " +
                    k +
                    ". Please enter a positive integer."
                );
            }

            System.out.print("Enter the number of top players to display: ");

            try {
                k = Integer.parseInt(scan.nextLine().trim());
            } catch (NumberFormatException e) {
                k = -1;
            }
        } while (k <= 0);

        return k;
    }


    /**
     * Prompts the user to enter a player's username.
     * Validates that the username is not empty and re-prompts on blank input.
     * 
     * @param scan The Scanner to read user input from
     * @return The entered username as a non-empty String
     */
    private static String getValidUsername(Scanner scan) {
        String username = null;

        do {
            if (username != null) {
                System.out.println("Username can't be empty.");
            }

            System.out.print("Enter the player username: ");
            username = scan.nextLine().trim();
        } while (username.isEmpty());

        return username;
    }


    /**
     * Creates a request for the entire leaderboard.
     * 
     * @param all Must be true to retrieve all players
     */
    public LeaderboardRequest(boolean all) { this(null, null, all); }


    /**
     * Creates a request for the top k players.
     * 
     * @param kTopUsers The number of top players to retrieve (must be positive)
     */
    public LeaderboardRequest(int kTopUsers) { this(null, kTopUsers, null); }


    /**
     * Creates a request for a single player's leaderboard position.
     * 
     * @param playerUsername The username of the player to query (must not be empty)
     */
    public LeaderboardRequest(String playerUsername) { this(playerUsername, null, null); }


    // Getters
    /**
     * Gets the username for single-player query.
     * 
     * @return The player username, or null if this is not a single-player query
     */
    public String getPlayerUsername() { return this.playerUsername; }


    /**
     * Gets the k value for top-k query.
     * 
     * @return The number of top players, or null if this is not a top-k query
     */
    public Integer getKTopPlayers() { return this.kTopUsers; }


    /**
     * Checks if this is a full leaderboard query.
     * 
     * @return true if requesting the entire leaderboard, false otherwise
     */
    public boolean isAll() { return Boolean.TRUE.equals(this.all); }
}