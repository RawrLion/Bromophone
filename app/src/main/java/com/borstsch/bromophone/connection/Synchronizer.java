package com.borstsch.bromophone.connection;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.borstsch.bromophone.UserType;
import com.borstsch.bromophone.musicplayer.Audio;

public class Synchronizer {
    private SendMessageThread sendMessageThread;
    private UserType userType;

    public Synchronizer(@Nullable SendMessageThread sendMessageThread,
                        @NonNull UserType userType) {
        this.sendMessageThread = sendMessageThread;
        this.userType = userType;
    }

    public void startPlayer() {
        if (userType == UserType.SERVER) {
            sendMessageThread.start();
            sendMessageThread.addMessage(Message.getStartPlayerMessage());
        }
    }

    public void playTrack(Audio track) {
        if (userType == UserType.SERVER) {
            sendMessageThread.addMessage(Message.getPlayMessage());
        }
    }

    public void resumeTrack(Audio track) {
        if (userType == UserType.SERVER) {
            sendMessageThread.addMessage(Message.getResumeMessage());
        }
    }

    public void pauseTrack(Audio track) {
        if (userType == UserType.SERVER) {
            sendMessageThread.addMessage(Message.getPauseMessage());
        }
    }

    public void stop() {
        if (userType == UserType.SERVER) {
            sendMessageThread.setKillReceived();
        } else {
            ReceiveMessageThread.setKillReceived();
        }
    }
}
