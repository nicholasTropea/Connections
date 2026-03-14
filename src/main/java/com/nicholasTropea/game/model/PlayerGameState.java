package com.nicholasTropea.game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the state of a player for a specific game.
 */
public class PlayerGameState {
    /** Player user ID this state belongs to. */
    private final int userId;

    /** Game identifier this state refers to. */
    private final int gameId;
    
    /** Number of correct proposals made by the player. */
    private int correctProposals;

    /** List of words that still need to be grouped. */
    private List<String> remainingWords;

    /** Correctly guessed groups for this game. */
    private List<List<String>> guessedGroups;

    /** Number of mistakes made by the player. */
    private int errorCount;

    /** Score accumulated in this game. */
    private int score;

    /** Final game state for the player. */
    public enum GameResult {
        WON,
        LOST,
        NOT_FINISHED
    }

    /** Current final state for this game session. */
    private GameResult finalState;


    /**
     * Creates a new state object for a player's game session.
     *
     * @param userId unique player identifier
     * @param gameId game identifier
     * @param initialWords words initially available in the game
     * @throws NullPointerException if initialWords is null
     * @throws IllegalArgumentException if userId is negative or gameId is out of range
     */
    public PlayerGameState(int userId, int gameId, List<String> initialWords) {
        if (userId < 0) {
            throw new IllegalArgumentException("userId must be non-negative");
        }
        if (gameId < 0 || gameId > 911) {
            throw new IllegalArgumentException("gameId must be between 0 and 911");
        }

        this.userId = userId;
        this.gameId = gameId;
        this.correctProposals = 0;
        this.remainingWords = new ArrayList<>(Objects.requireNonNull(initialWords));
        this.guessedGroups = new ArrayList<>();
        this.errorCount = 0;
        this.score = 0;
        this.finalState = GameResult.NOT_FINISHED;
    }


    /** @return player user ID */
    public int getUserId() { return this.userId; }


    /** @return game id */
    public int getGameId() { return this.gameId; }


    /** @return number of correct proposals */
    public int getCorrectProposals() { return this.correctProposals; }


    /** @return immutable copy of remaining words */
    public List<String> getRemainingWords() { return List.copyOf(this.remainingWords); }


    /** @return immutable deep copy of guessed groups */
    public List<List<String>> getGuessedGroups() {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> group : this.guessedGroups) {
            copy.add(List.copyOf(group));
        }
        return List.copyOf(copy);
    }


    /** @return error count */
    public int getErrorCount() { return this.errorCount; }


    /** @return game score */
    public int getScore() { return this.score; }


    /** @return final state */
    public GameResult getFinalState() { return this.finalState; }


    /** Increments the number of correct proposals by one. */
    public void incrementCorrectProposals() { this.correctProposals++; }


    /** Increments the number of errors by one. */
    public void incrementErrorCount() { this.errorCount++; }


    /**
     * Sets the game score.
     *
     * @param score new score value
     */
    public void setScore(int score) { this.score = score; }


    /**
     * Removes a collection of guessed words from remaining words.
     *
     * @param guessedWords guessed words to remove
     * @throws NullPointerException if guessedWords is null
     */
    public void removeWords(List<String> guessedWords) {
        this.remainingWords.removeAll(Objects.requireNonNull(guessedWords));
    }


    /**
     * Adds a correctly guessed group.
     *
     * @param guessedGroup guessed group words
     */
    public void addGuessedGroup(List<String> guessedGroup) {
        this.guessedGroups.add(List.copyOf(Objects.requireNonNull(guessedGroup)));
    }


    /** Marks the game as won for this player. */
    public void completeAsWon() { this.finalState = GameResult.WON; }


    /** Marks the game as lost for this player. */
    public void completeAsLost() { this.finalState = GameResult.LOST; }


    /** @return true if final state is WON or LOST */
    public boolean isFinished() { return this.finalState != GameResult.NOT_FINISHED; }


    /**
     * Replaces remaining words with a new set.
     *
     * @param remainingWords new remaining words list
     * @throws NullPointerException if remainingWords is null
     */
    public void setRemainingWords(List<String> remainingWords) {
        this.remainingWords = new ArrayList<>(Objects.requireNonNull(remainingWords));
    }


    /**
     * Restores all mutable progress fields from a persisted snapshot.
     *
     * @param correctProposals number of correct proposals
     * @param errorCount number of wrong proposals
     * @param score stored game score
     * @param remainingWords words not yet grouped
     * @param finalState final game state
     */
    public void restoreProgress(
        int correctProposals,
        int errorCount,
        int score,
        List<String> remainingWords,
        List<List<String>> guessedGroups,
        GameResult finalState
    ) {
        if (correctProposals < 0) {
            throw new IllegalArgumentException("correctProposals must be >= 0");
        }
        if (errorCount < 0) {
            throw new IllegalArgumentException("errorCount must be >= 0");
        }

        this.correctProposals = correctProposals;
        this.errorCount = errorCount;
        this.score = score;
        this.remainingWords = new ArrayList<>(Objects.requireNonNull(remainingWords));
        this.guessedGroups = new ArrayList<>();
        if (guessedGroups != null) {
            for (List<String> group : guessedGroups) {
                if (group != null) {
                    this.guessedGroups.add(List.copyOf(group));
                }
            }
        }
        this.finalState = Objects.requireNonNull(finalState);
    }
}