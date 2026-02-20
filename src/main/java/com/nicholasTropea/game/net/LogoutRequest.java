package com.nicholasTropea.game.net;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Request;


/**
 * Richiesta di logout di un giocatore.
 * 
 * JSON atteso:
 * {
 *    "operation" : "logout"
 * }
 */
public class LogoutRequest extends Request {
    // Costruttore
    public LogoutRequest() { super("logout"); }
}