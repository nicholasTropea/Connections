package com.nicholasTropea.game.net.requests;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;
import java.util.Scanner;


/**
 * Concrete request for retrieving information about a game.
 * Receives a {@link GameInfoResponse}.
 * 
 * <p>Supports two types of queries:
 * <ul>
 *   <li>Current game being played</li>
 *   <li>Specific game by ID</li>
 * </ul>
 * 
 * <p>Expected JSON format:
 * <pre>{@code
 * {
 *    "operation": "requestGameInfo",
 *    "gameId": INT,
 *    "current": BOOLEAN
 * }
 * }</pre>
 * 
 * <p>Possible errors: "gameId does not exist"
 */
public class GameInfoRequest extends Request {
    /**
     * The game ID. Null if querying the current game.
     */
    @SerializedName("gameId")
    private final Integer gameId;

    /**
     * True if requesting information about the current game. Null otherwise.
     */
    @SerializedName("current")
    private final Boolean current;


    /**
     * Private constructor enforcing at least one parameter is specified.
     * Exactly one of the two parameters should be non-null and valid.
     * 
     * @param gameId The game ID (0-911), or null if querying current game
     * @param current True to retrieve current game, or null if querying specific ID
     * @throws IllegalArgumentException if invalid parameters are provided
     */
    private GameInfoRequest(Integer gameId, Boolean current) {
        super("requestGameInfo");

        if (gameId == null && (current == null || !current)) {
            throw new IllegalArgumentException(
                "Either gameId or current=true must be provided"
            );
        }

        if (gameId != null) {
            final int MIN_ID = 0;
            final int MAX_ID = 911;

            if (gameId < MIN_ID || gameId > MAX_ID) {
                throw new IllegalArgumentException("Game id must be between 0 and 911");
            } 
        }

        this.gameId = gameId;
        this.current = current;
    }


    /**
     * Factory method that prompts the user to select a game info query type
     * and collects the necessary parameters interactively.
     * 
     * @param scan The Scanner to read user input from
     * @return A new GameInfoRequest configured based on user selection
     */
    public static GameInfoRequest createRequest(Scanner scan) {
        int selection = getSelection(scan);

        switch (selection) {
            case 1:
                return new GameInfoRequest(true);
            case 2:
                int id = getValidGameID(scan);
                return new GameInfoRequest(id);
            default:
                throw new IllegalStateException("Invalid selection: " + selection);
        }
    }


    /**
     * Displays the game info menu and prompts the user to select an option.
     * Validates input and re-prompts on invalid selection (must be 1 or 2).
     * 
     * @param scan The Scanner to read user input from
     * @return The selected option (1 or 2)
     */
    private static int getSelection(Scanner scan) {
        System.out.println(
            "Choose the game info option:\n" +
            "\t- 1: Current game\n" +
            "\t- 2: Specific game\n"
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
        } while (selection != 1 && selection != 2);

        return selection;
    }


    /**
     * Prompts the user to enter the game id of the selected game.
     * Validates that the input is a valid integer and re-prompts on error.
     * 
     * @param scan The Scanner to read user input from
     * @return A positive integer representing the game id.
     */
    private static int getValidGameID(Scanner scan) {
        int id = -1;

        do {
            if (id != -1) {
                System.out.println(
                    "Invalid game id: " +
                    id +
                    ". Please enter an integer between 0 and 911."
                );
            }

            System.out.print("Enter the game id: ");

            try {
                id = Integer.parseInt(scan.nextLine().trim());
            } catch (NumberFormatException e) {
                id = -1;
            }
        } while (id < 0 || id > 911);

        return id;
    }


    /**
     * Creates a request for a specific game.
     * 
     * @param gameId The ID of the game to retrieve (0-911)
     */
    public GameInfoRequest(int gameId) { this(gameId, null); }


    /**
     * Creates a request for the current game.
     * 
     * @param current Must be true to retrieve current game
     */
    public GameInfoRequest(boolean current) { this(null, current); }


    // Getters
    /**
     * Gets the game ID for specific game query.
     * 
     * @return The game ID, or null if this is a current game query
     */
    public Integer getGameId() { return this.gameId; }


    /**
     * Checks if this is a current game query.
     * 
     * @return true if requesting the current game, false otherwise
     */
    public boolean isCurrent() { return Boolean.TRUE.equals(this.current); }
}