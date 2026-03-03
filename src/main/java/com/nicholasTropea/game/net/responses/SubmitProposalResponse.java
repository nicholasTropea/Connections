package com.nicholasTropea.game.net.responses;

import com.google.gson.annotations.SerializedName;
import com.nicholasTropea.game.net.Response;

/**
 * Response to a {@link SubmitProposalRequest}.
 * 
 * Expected JSON format:
 * <pre>{@code
 * {
 *      "success" : BOOLEAN,
 *      "error" : STRING,
 *      "result" : BOOLEAN,
 *      "groupName" : STRING
 * }
 * }</pre>
 * 
 * Possible errors: "user not logged in", "invalid words"
 */
public class SubmitProposalResponse extends Response {

    /** Result of the submitted proposal */
    @SerializedName("result")
    private final Boolean result;
        
    /** Name of the group if proposal was correct (null if result=false) */
    @SerializedName("groupName")
    private final String groupName;


    /**
     * Private constructor for creating submit proposal responses.
     *
     * @param success Whether the request was successful
     * @param error Error message if unsuccessful
     * @param result Whether the proposal was correct
     * @param groupName Name of the correctly guessed group
     */
    private SubmitProposalResponse(
        boolean success,
        String error,
        Boolean result,
        String groupName
    ) {
        super("submitProposal", success, error);
        this.result = result;
        this.groupName = groupName;
    }


    /**
     * Creates a successful submit proposal response.
     * 
     * @param result Whether the proposal was correct
     * @param groupName Name of the group if correct (null if incorrect)
     * @return Instance with success=true and error=null
     * @throws IllegalArgumentException if result=true but groupName is null or empty,
     *         or if result=false but groupName is not null
     */
    public static SubmitProposalResponse success(boolean result, String groupName) {
        if (result && (groupName == null || groupName.trim().isEmpty())) {
            throw new IllegalArgumentException(
                "If result=true then groupName cannot be empty or null"
            );
        }

        if (!result && groupName != null) {
            throw new IllegalArgumentException(
                "If result=false then groupName must be null"
            );
        }
        
        return new SubmitProposalResponse(true, null, result, groupName);
    }


    /**
     * Creates an error submit proposal response.
     * 
     * @param errorMsg Descriptive error message
     * @return Instance with success=false, error=errorMsg and remaining fields null
     * @throws IllegalArgumentException if errorMsg is null or empty
     */
    public static SubmitProposalResponse error(String errorMsg) {
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message must be provided");
        }

        return new SubmitProposalResponse(false, errorMsg, null, null);
    }


    /**
     * Gets the result of the proposal.
     *
     * @return True if proposal was correct, false if incorrect, null if request failed
     */
    public Boolean getResult() { return this.result; }


    /**
     * Gets the name of the correctly guessed group.
     *
     * @return Group name if proposal was correct, null otherwise
     */
    public String getGroupName() { return this.groupName; }
}