package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Risposta ad una richiesta di {@link LoginRequest}.
 * 
 * JSON atteso:
 * <pre>{@code
 * {
 *    "success" : BOOLEAN,
 *    "error" : STRING,
 *    "gameId" : INT,
 *    "words" : LIST<STRING>,
 *    "guessedGroups" : LIST<LIST<STRING>>,
 *    "timeLeft" : LONG,
 *    "errors" : INT,
 *    "score" : INT
 * }
 * }</pre>
 * 
 * Errori possibili: "nome inesistente", "psw incorretta", "connessione già loggata"
 * 
 * @see RegisterResponse per il formato di registrazione
 */
public class LoginResponse {
    /** true se login avvenuto con successo, false altrimenti */
    @SerializedName("success")
    private final boolean success;

    /** Messaggio d'errore (null se success=true) */
    @SerializedName("error")
    private final String error;

    /** Id della partita corrente (null se success=false) */
    @SerializedName("gameId")
    private final Integer gameId;

    /** Lista di parole della partita corrente (null se success=false) */
    @SerializedName("words")
    private final List<String> words;

    /** Lista di gruppi di parole già indovinate della partita corrente */
    @SerializedName("guessedGroups")
    private final List<List<String>> guessedGroups;

    /** Tempo rimanente della partita corrente in millisecondi */
    @SerializedName("timeLeft")
    private final Long timeLeft;

    /** Numero di errori già commessi nella partita corrente*/
    @SerializedName("errors")
    private final Integer errors;

    /** Punteggio ottenuto nella partita corrente */
    @SerializedName("score")
    private final Integer score;

    /** Costruttore privato */
    private LoginResponse(
        boolean success,
        String error,
        Integer gameId,
        List<String> words,
        List<List<String>> guessedGroups,
        Long timeLeft,
        Integer errors,
        Integer  score
    ) {
        this.success = success;
        this.error = error;
        this.gameId = gameId;
        this.words = words != null ? List.copyOf(words) : null;
        this.guessedGroups = guessedGroups != null ? List.copyOf(guessedGroups) : null;
        this.timeLeft = timeLeft;
        this.errors = errors;
        this.score = score;
    }

    /**
     * Crea una risposta di successo per login completato.
     * 
     * @param gameId id della partita corrente
     * @param words lista di parole della partita corrente
     * @param guessedGroups lista di gruppi di parole già indovinate
     *                      della partita corrente
     * @param timeLeft tempo rimanente della partita corrente in millisecondi
     * @param errors numero di errori già commessi nella partita corrente
     * @param score punteggio ottenuto nella partita corrente
     * 
     * @return istanza con success=true e error=null
     */
    public static LoginResponse success(
        Integer gameId,
        List<String> words,
        List<List<String>> guessedGroups,
        Long timeLeft,
        Integer errors,
        Integer score
    ) {
        // Check leggero
        validateSuccess(gameId, words);

        return new LoginResponse(
            true,
            null,
            gameId,
            words,
            guessedGroups,
            timeLeft,
            errors,
            score
        );
    }

    /**
     * Crea una risposta di errore per login fallito.
     * 
     * @param errorMsg messaggio d'errore descrittivo
     * @return istanza con success=false, error=errorMsg e restante=null
     * @throws IllegalArgumentException se errorMsg=null o vuoto
     */
    public static LoginResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new LoginResponse(false, errorMsg, null, null, null, null, null, null);
    }

    /**
     * Helper function per validazione veloce degli argomenti passati a success()
     * 
     * @param gameId id della partita corrente
     * @param words lista di parole della partita corrente
     * @throws IllegalArgumentException se gameId o words sono malformati
     */
    private static void validateSuccess(Integer gameId, List<String> words) {
        final int MIN_ID = 0;
        final int MAX_ID = 911;
        if (gameId == null || gameId < MIN_ID || gameId > MAX_ID) {
            throw new IllegalArgumentException("gameId must be between 0 and 911");
        }

        if (words == null || words.size() != 16) {
            throw new IllegalArgumentException("words must contain 16 words");
        }
    }

    // Getters
    public boolean isSuccess() { return this.success; }
    public String getError() { return this.error; }
    public Integer getGameId() { return this.gameId; }
    public List<String> getWords() { return this.words; }
    public List<List<String>> getGuessedGroups() { return this.guessedGroups; }
    public Long getTimeLeft() { return this.timeLeft; }
    public Integer getErrors() { return this.errors; }
    public Integer getScore() { return this.score; }
}