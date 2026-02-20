package com.nicholasTropea.game.net;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;


/**
 * Classe astratta che definisce una richiesta JSON
 */
public abstract class Request {
    /* Operazione effettuata */
    @SerializedName("operation")
    protected final String operation;

    /** Costruttore */
    public Request(String operation) {
        this.operation = Objects.requireNonNull(operation);
    }

    // Getter
    public String getOperation() { return this.operation; }
}