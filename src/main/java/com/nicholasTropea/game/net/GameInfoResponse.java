package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Risposta ad una richiesta di {@link GameInfoRequest}.
 * 
 * JSON atteso:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING,
 *      "active" : BOOLEAN,
 *      "timeLeft" : LONG,
 *      "wordsLeft" : LIST<STRING>,
 *      "solution" : LIST<LIST<STRING>>,
 *      "guessedGroups" : LIST<LIST<STRING>>,
 *      "errors" : INT,
 *      "score" : INT
 * }
 * }</pre>
 * 
 * Errori possibili: 
 */
public class GameInfoResponse {
    /** true se richiesta avvenuta con successo, false altrimenti */
    @SerializedName("success")
    private final boolean success;

    /** Messaggio d'errore (null se success=true) */
    @SerializedName("error")
    private final String error;

    /** Stato della partita (true se partita in corso e non terminata, false altrimenti) */
    @SerializedName("active")
    private final boolean active;

    /** Tempo rimanente della partita in millisecondi (null se active=false) */
    @SerializedName("timeLeft")
    private final Long timeLeft;

    /** Parole rimaste da raggruppare (null se active=false) */
    @SerializedName("wordsLeft")
    private final List<String> wordsLeft;

    /** Assegnazione corretta delle parole (null se active=true) */
    @SerializedName("solution")
    private final List<List<String>> solution;

    /** Lista di gruppi di parole indovinate */
    @SerializedName("guessedGroups")
    private final List<List<String>> guessedGroups;

    /** Numero di errori commessi nella partita */
    @SerializedName("errors")
    private final Integer errors;

    /** Punteggio ottenuto nella partita */
    @SerializedName("score")
    private final Integer score;

    /** Costruttore privato */
    private GameInfoResponse(
        boolean success,
        String error,
        boolean active,
        Long timeLeft,
        List<String> wordsLeft,
        List<List<String>> solution,
        List<List<String>> guessedGroups,
        Integer errors,
        Integer score
    ) {
        this.success = success;
        this.error = error;
        this.active = active;
        this.timeLeft = timeLeft;
        this.wordsLeft = wordsLeft != null ? List.copyOf(wordsLeft) : null;
        this.solution = solution != null ? List.copyOf(solution) : null;
        this.guessedGroups = guessedGroups != null ? List.copyOf(guessedGroups) : null;
        this.errors = errors;
        this.score = score;
    }

    /**
     * Crea una risposta di successo.
     * Se active == true: timeLeft e wordsLeft != null, solution == null
     * Se active == false: solution != null, timeLeft e wordsLeft == null
     * 
     * @param active true se partita corrente e non terminata dal giocatore, false altrimenti
     * @param timeLeft tempo rimanente della partita in millisecondi
     * @param wordsLeft parole rimaste da raggruppare
     * @param solution assegnazione corretta delle parole
     * @param guessedGroups lista di gruppi di parole indovinate
     * @param errors numero di errori commessi
     * @param score punteggio ottenuto
     * 
     * @return istanza con success=true e error=null
     */
    public static GameInfoResponse success(
        boolean active,
        Long timeLeft,
        List<String> wordsLeft,
        List<List<String>> solution,
        List<List<String>> guessedGroups,
        Integer errors,
        Integer score
    ) {
        if (errors == null || score == null) {
            throw new IllegalArgumentException("errors and score cannot be null");
        }

        if (active) return new GameInfoResponse(true, null, true, timeLeft, wordsLeft, null, guessedGroups, errors, score);
        return new GameInfoResponse(true, null, false, null, null, solution, guessedGroups, errors, score);
    }

    /**
     * Crea una risposta di errore.
     * 
     * @param errorMsg messaggio d'errore descrittivo
     * @return istanza con success=false, error=errorMsg e restante=null
     * @throws IllegalArgumentException se errorMsg=null o vuoto
     */
    public static GameInfoResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new GameInfoResponse(false, errorMsg, false, null, null, null, null, null, null);
    }

    // Getters
    public boolean isSuccess() { return this.success; }
    public String getError() { return this.error; }
    public boolean isActive() { return this.active; }
    public Long getTimeLeft() { return this.timeLeft; }
    public List<String> getWordsLeft() { return this.wordsLeft; }
    public List<List<String>> getSolution() { return this.solution; }
    public List<List<String>> getGuessedGroups() { return this.guessedGroups; }
    public Integer getErrors() { return this.errors; }
    public Integer getScore() { return this.score; }
}