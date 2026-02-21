package com.nicholasTropea.game.net;

import java.util.Objects;
import java.util.Scanner;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;


/**
 * Request to update an existing account's credentials.
 *
 * Receives an {@link UpdateCredentialsResponse}.
 *
 * Expected JSON format:
 * {@code
 * {
 *    "operation": "updateCredentials",
 *    "oldUsername": "STRING",
 *    "newUsername": "STRING",
 *    "oldPsw": "STRING",
 *    "newPsw": "STRING"
 * }
 * }
 *
 * The request must include the current username and current password.
 * At least one of {@code newUsername} or {@code newPsw} must be provided.
 */
public class UpdateCredentialsRequest extends Request {
    @SerializedName("oldUsername")
    private final String oldUsername;

    @SerializedName("oldPsw")
    private final String oldPassword;

    @SerializedName("newUsername")
    private final String newUsername;

    @SerializedName("newPsw")
    private final String newPassword;


    /**
     * Full constructor.
     *
     * @param oldUsername current username (required)
     * @param oldPassword current password (required)
     * @param newUsername new username (optional)
     * @param newPassword new password (optional)
     */
    public UpdateCredentialsRequest(
        String oldUsername,
        String oldPassword,
        String newUsername,
        String newPassword
    ) {
        super("updateCredentials");

        validate(oldUsername, oldPassword, newUsername, newPassword);

        this.oldUsername = oldUsername.trim();
        this.oldPassword = oldPassword;
        this.newUsername = newUsername != null ? newUsername.trim() : "";
        this.newPassword = newPassword != null ? newPassword : "";
    }


    /**
     * Prompts the user for required inputs and returns a new request.
     * Both old username and old password are required. At least one of
     * new username or new password must be provided.
     *
     * @param scan Scanner to read user input from
     * @return a validated UpdateCredentialsRequest
     */
    public static UpdateCredentialsRequest createRequest(Scanner scan) {
        String oldUsername = getNonEmptyInput(scan, "Current username");
        String oldPassword = getNonEmptyInput(scan, "Current password");

        String newUsername = null;
        String newPassword = null;

        do {
            System.out.println(
                "Enter new credentials (leave blank to keep unchanged).\n" + 
                "At least one must be provided."
            );

            System.out.print("New username: ");
            String nu = scan.nextLine().trim();

            System.out.print("New password: ");
            String np = scan.nextLine();

            newUsername = nu.isEmpty() ? null : nu;
            newPassword = np.isEmpty() ? null : np;

            if (
                (newUsername == null || newUsername.trim().isEmpty()) &&
                (newPassword == null || newPassword.isEmpty())
            ) {
                System.out.println(
                    "You must provide at least a new username or a new password."
                );
            }
            else { break; }
        } while (true);

        return new UpdateCredentialsRequest(
            oldUsername,
            oldPassword,
            newUsername,
            newPassword
        );
    }


    /**
     * Helper that prompts until a non-empty value is entered.
     *
     * @param scan Scanner for input
     * @param prompt prompt label
     * @return non-empty trimmed string
     */
    private static String getNonEmptyInput(Scanner scan, String prompt) {
        String input = null;

        do {
            if (input != null) System.out.println(prompt + " cannot be empty.");

            System.out.print(prompt + ": ");
            input = scan.nextLine().trim();
        } while (input.isEmpty());
        
        return input;
    }


    /**
     * Validates constructor arguments.
     */
    private static void validate(
        String oldUsername,
        String oldPassword,
        String newUsername,
        String newPassword
    ) {
        Objects.requireNonNull(oldUsername, "oldUsername is required");
        Objects.requireNonNull(oldPassword, "oldPassword is required");

        if (
            (newUsername == null || newUsername.trim().isEmpty()) &&
            (newPassword == null || newPassword.isEmpty())
        ) {
            throw new IllegalArgumentException(
                "Either password, name or both must change"
            );
        }

        if (oldUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("oldUsername cannot be empty");
        }

        if (oldPassword.isEmpty()) {
            throw new IllegalArgumentException("oldPassword cannot be empty");
        }

        if (oldPassword.length() < 6) {
            throw new IllegalArgumentException(
                "OldPassword must be at least 6 characters long"
            );
        }

        if (newPassword != null && newPassword.length() < 6) {
            throw new IllegalArgumentException(
                "newPassword must be at least 6 characters long"
            );
        }

        if (newUsername != null && newUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("newUsername cannot be empty");
        }
    }


    // Getters
    public String getOldUsername() { return this.oldUsername; }
    public String getOldPassword() { return this.oldPassword; }
    public String getNewUsername() { return this.newUsername; }
    public String getNewPassword() { return this.newPassword; }
}