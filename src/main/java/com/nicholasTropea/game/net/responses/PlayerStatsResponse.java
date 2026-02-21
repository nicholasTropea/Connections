package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.model.MistakeHistogram;

/**
 * Risposta ad una richiesta di {@link PlayerStatsRequest}.
 * 
 * JSON atteso:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING,
 *      "solvedPuzzles" : INT,
 *      "failedPuzzles" : INT,
 *      "unfinishedPuzzles" : INT,
 *      "perfectPuzzles" : INT,
 *      "winRate" : FLOAT,
 *      "lossRate" : FLOAT,
 *      "currentStreak" : INT,
 *      "maxStreak" : INT,
 *      "histogram" : MistakeHistogram
 * }
 * }</pre>
 * 
 * Errori possibili: "utente non loggato"
 */
public class PlayerStatsResponse {
    /** true se richiesta avvenuta con successo, false altrimenti */
    @SerializedName("success")
    private final boolean success;
        
    /** Messaggio d'errore (null se success=true) */
    @SerializedName("error")
    private final String error;

    /** Numero di puzzle risolti */
    @SerializedName("solvedPuzzles")
    private final Integer solved;
        
    /** Numero di puzzle persi */
    @SerializedName("failedPuzzles")
    private final Integer failed;

    /** Numero di puzzle non finiti */
    @SerializedName("unfinishedPuzzles")
    private final Integer unfinished;
        
    /** Numero di puzzle risolti senza errori */
    @SerializedName("perfectPuzzles")
    private final Integer perfect;

    /** Winrate */
    @SerializedName("winRate")
    private final Float winRate;

    /** Lossrate */
    @SerializedName("lossRate")
    private final Float lossRate;

    /** Streak di vittorie attuale */
    @SerializedName("currentStreak")
    private final Integer currentStreak;    

    /** Massima streak di vittorie ottenuta */
    @SerializedName("maxStreak")
    private final Integer maxStreak;

    /** Attuale istogramma delle partite dell'utente */
    @SerializedName("histogram")
    private final MistakeHistogram histogram;

    /** Costruttore */
    private PlayerStatsResponse(
        boolean success,
        String error,
        Integer solved,
        Integer failed,
        Integer unfinished,
        Integer perfect,
        Float winRate,
        Float lossRate,
        Integer currentStreak,
        Integer maxStreak,
        MistakeHistogram histogram
    ) {
        this.success = success;
        this.error = error;
        this.solved = solved;
        this.failed = failed;
        this.unfinished = unfinished;
        this.perfect = perfect;
        this.winRate = winRate;
        this.lossRate = lossRate;
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.histogram = histogram;
    }

    /**
     * Crea una risposta di successo.
     * 
     * @return istanza con success=true e error=null
     */
    public static PlayerStatsResponse success(
        Integer solved,
        Integer failed,
        Integer unfinished,
        Integer perfect,
        Float winRate,
        Float lossRate,
        Integer currentStreak,
        Integer maxStreak,
        MistakeHistogram histogram
    ) {
        if (
            solved < 0 || failed < 0 || unfinished < 0 ||
            perfect < 0 || winRate < 0 || lossRate < 0 ||
            currentStreak < 0 || maxStreak < 0 || maxStreak < currentStreak
        ) {
            throw new IllegalArgumentException(
                "puzzle, streak and rate parameters cannot be < 0,
                maxStreak cannot be < currentStreak"
            );
        }

        return new PlayerStatsResponse(
            true, null, solved,
            failed, unfinished, perfect,
            winRate, lossRate, currentStreak,
            maxStreak, histogram
        );
    }

    /**
     * Crea una risposta di errore.
     * 
     * @param errorMsg messaggio d'errore descrittivo
     * @return istanza con success=false, error=errorMsg e restante null
     * @throws IllegalArgumentException se errorMsg=null o vuoto
     */
    public static PlayerStatsResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new PlayerStatsResponse(
            false,
            errorMsg,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    // Getters
    public boolean isSuccess() { return this.success; }
    public String getError() { return this.error; }
    public Integer getSolvedPuzzles() { return this.solved; }
    public Integer getFailedPuzzles() { return this.failed; }
    public Integer getUnfinishedPuzzles() { return this.unfinished; }
    public Integer getPerfectPuzzles() { return this.perfect; }
    public Float getWinRate() { return this.winRate; }
    public Float getLossRate() { return this.lossRate; }
    public Integer getCurrentStreak() { return this.currentStreak; }
    public Integer getMaxStreak() { return this.maxStreak; }
    // Non una copia, tanto è usa e getta
    public MistakeHistogram getHistogram() { return this.histogram; }
}