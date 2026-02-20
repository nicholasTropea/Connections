package com.nicholasTropea.game.net;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;


/**
 * Richiesta delle statistiche del giocatore.
 * 
 * JSON atteso:
 * {
 *    "operation" : "requestPlayerStats"
 * }
 */
public class PlayerStatsRequest extends Request {
    /** Costruttore */
    public PlayerStatsRequest() { super("requestPlayerStats"); }
}