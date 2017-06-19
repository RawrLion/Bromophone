package com.borstsch.bromophone.connection;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public final class Message {
    public static final String START_PLAYER_COMMAND = "START";
    public static final String PLAY_COMMAND = "PLAY";
    public static final String END_MESSAGES_COMMAND = "END";

    private Message() {}

    @Nullable
    public static JSONObject getPlayMessage() {
        try {
            return new JSONObject().put(PLAY_COMMAND, "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static JSONObject getStartPlayerMessage() {
        try {
            return new JSONObject().put(START_PLAYER_COMMAND, "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
