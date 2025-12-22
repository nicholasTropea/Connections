package com.nicholasTropea.game.net;

import java.util.List;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;

/**
 * Richiesta di invio di una proposta di un giocatore.
 * 
 * JSON atteso:
 * {
 *    "operation" : "submitProposal",
 *    "words" : ["word1", "word2", "word3", "word4"]
 * }
 */
public class SubmitProposalRequest {
    @SerializedName("operation")
    private final String operation = "submitProposal";

    @SerializedName("words")
    private final List<String> words;

    /** Costruttore */
    public SubmitProposalRequest(List<String> words) {
        this.validate(words);

        this.words = List.copyOf(words);
    }

    /**
     * Valida gli argomenti passati al costruttore
     * 
     * @param words Lista di parole proposta dal giocatore
     */
    private void validate(List<String> words) {
        Objects.requireNonNull(words, "Words list is required");

        if (words.size() != 4) throw new IllegalArgumentException("Exactly 4 words are required");

        for (String word : words) {
            if (word == null || word.trim().isEmpty()) throw new IllegalArgumentException("Words cannot be null or empty");
        }
    }

    // Getters
    public String getOperation() { return this.operation; }
    public List<String> getWords() { return this.words; }
}