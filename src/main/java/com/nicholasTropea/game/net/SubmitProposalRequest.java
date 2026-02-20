package com.nicholasTropea.game.net;

import java.util.List;
import java.util.Objects;
import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;


/**
 * Richiesta di invio di una proposta di un giocatore.
 * 
 * JSON atteso:
 * {
 *    "operation" : "submitProposal",
 *    "words" : ["word1", "word2", "word3", "word4"]
 * }
 */
public class SubmitProposalRequest extends Request {
    @SerializedName("words")
    private final List<String> words;

    /** Costruttore */
    public SubmitProposalRequest(List<String> words) {
        super("submitProposal");

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
    public List<String> getWords() { return this.words; }
}