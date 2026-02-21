package com.nicholasTropea.game.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;


/**
 * Request to submit a player's proposed group of four words.
 *
 * Expected JSON format:
 * {@code
 * {
 *    "operation": "submitProposal",
 *    "words": ["word1", "word2", "word3", "word4"]
 * }
 * }
 *
 * The server evaluates the four submitted words as a proposed group for the
 * current game. Exactly four non-empty words are required.
 */
public class SubmitProposalRequest extends Request {
    @SerializedName("words")
    private final List<String> words;


    /**
     * Constructs a SubmitProposalRequest with the provided words.
     *
     * @param words list of four words proposed by the player
     * @throws NullPointerException if {@code words} is null
     * @throws IllegalArgumentException if {@code words} does not contain exactly
     * 4 non-empty entries
     */
    public SubmitProposalRequest(List<String> words) {
        super("submitProposal");

        validate(words);

        this.words = List.copyOf(words);
    }


    /**
     * Validates the provided words list.
     *
     * @param words list to validate
     */
    private void validate(List<String> words) {
        Objects.requireNonNull(words, "Words list is required");

        if (words.size() != 4) {
            throw new IllegalArgumentException("Exactly 4 words are required");
        }

        for (String word : words) {
            if (word == null || word.trim().isEmpty()) {
                throw new IllegalArgumentException("Words cannot be null or empty");
            }
        }
    }


    /**
     * Prompts the user to enter four words via the provided Scanner and
     * returns a new {@link SubmitProposalRequest}.
     *
     * @param scan Scanner to read user input from
     * @return a new SubmitProposalRequest containing the entered words
     */
    public static SubmitProposalRequest createRequest(Scanner scan) {
        List<String> words = new ArrayList<>();

        String[] order = {"first", "second", "third", "fourth"};

        for (int i = 0; i < 4; i++) {
            words.add(getValidWord(scan, order[i]));
        }

        return new SubmitProposalRequest(words);
    }


    /**
     * Prompts the user to enter a single non-empty word.
     *
     * @param scan Scanner to read user input from
     * @param label Label describing the word position (e.g. "first")
     * @return the entered word (guaranteed non-empty)
     */
    private static String getValidWord(Scanner scan, String label) {
        String input = null;

        do {
            if (input != null) {
                System.out.println("Word cannot be empty.");
            }

            System.out.print("Enter the " + label + " word: ");
            input = scan.nextLine().trim();
        } while (input.isEmpty());

        return input;
    }


    // Getter
    public List<String> getWords() { return this.words; }
}