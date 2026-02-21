package com.nicholasTropea.game.net;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;
import com.google.gson.JsonObject;
import com.google.gson.Type;

import com.nicholasTropea.game.net.requests.*;


/**
 * Custom JSON deserializer for Request objects.
 * <p>
 * Maps the "operation" field in incoming JSON to the appropriate Request
 * subclass for deserialization. Supports all operation types including
 * login, logout, register, and various game-related requests.
 * </p>
 */
public class RequestDeserializer implements JsonDeserializer<Request> {
    /**
     * Deserializes a JSON element to the appropriate Request subclass.
     * <p>
     * Examines the "operation" field in the JSON object and routes to the
     * correct Request subclass deserializer based on operation type.
     * </p>
     *
     * @param json the JSON element to deserialize
     * @param typeOfT the type of the object to deserialize into
     * @param context the deserialization context
     * @return the deserialized Request object
     * @throws JsonParseException if the operation type is unknown
     */
    @Override
    public Request deserialize(
        JsonElement json,
        Type typeOfT,
        JsonDeserializationContext context
    ) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String type = obj.get("operation").getAsString();

        switch (type) {
            case "requestGameInfo" -> {
                return context.deserialize(json, GameInfoRequest.class);
            }

            case "requestGameStats" -> {
                return context.deserialize(json, GameStatsRequest.class);
            }

            case "requestLeaderboard" -> {
                return context.deserialize(json, LeaderboardRequest.class);
            }

            case "requestGameStats" -> {
                return context.deserialize(json, GameStatsRequest.class);
            }

            case "login" -> {
                return context.deserialize(json, LoginRequest.class);
            }

            case "logout" -> {
                return context.deserialize(json, LogoutRequest.class);
            }

            case "requestPlayerStats" -> {
                return context.deserialize(json, PlayerStatsRequest.class);
            }

            case "register" -> {
                return context.deserialize(json, RegisterRequest.class);
            }

            case "submitProposal" -> {
                return context.deserialize(json, SubmitProposalRequest.class);
            }

            case "updateCredentials" -> {
                return context.deserialize(json, UpdateCredentialsRequest.class);
            }

            default -> {
                throw new JsonParseException("Unknown request type: " + type);
            }
        }
    }
}