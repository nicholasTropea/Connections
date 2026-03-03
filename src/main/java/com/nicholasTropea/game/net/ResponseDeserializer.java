package com.nicholasTropea.game.net;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;

import com.nicholasTropea.game.net.responses.*;


/**
 * Custom JSON deserializer for Response objects.
 * 
 * <p>Maps the "operation" field in incoming JSON to the appropriate Response
 * subclass for deserialization.
 */
public class ResponseDeserializer implements JsonDeserializer<Response> {
    /**
     * Deserializes a JSON element to the appropriate Response subclass.
     *
     * @param json The JSON element to deserialize
     * @param typeOfT The target type
     * @param context The deserialization context
     * @return The deserialized Response object
     * @throws JsonParseException if the operation type is unknown
     */
    @Override
    public Response deserialize(
        JsonElement json,
        Type typeOfT,
        JsonDeserializationContext context
    ) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String type = obj.get("operation").getAsString();

        switch (type) {
            case "requestGameInfo" -> {
                return context.deserialize(json, GameInfoResponse.class);
            }
            case "requestGameStats" -> {
                return context.deserialize(json, GameStatsResponse.class);
            }
            case "requestLeaderboard" -> {
                return context.deserialize(json, LeaderboardResponse.class);
            }
            case "login" -> {
                return context.deserialize(json, LoginResponse.class);
            }
            case "logout" -> {
                return context.deserialize(json, LogoutResponse.class);
            }
            case "requestPlayerStats" -> {
                return context.deserialize(json, PlayerStatsResponse.class);
            }
            case "register" -> {
                return context.deserialize(json, RegisterResponse.class);
            }
            case "submitProposal" -> {
                return context.deserialize(json, SubmitProposalResponse.class);
            }
            case "updateCredentials" -> {
                return context.deserialize(json, UpdateCredentialsResponse.class);
            }
            default -> {
                throw new JsonParseException("Unknown response type: " + type);
            }
        }
    }
}
