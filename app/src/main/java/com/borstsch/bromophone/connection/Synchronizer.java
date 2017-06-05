package com.borstsch.bromophone.connection;


import com.borstsch.bromophone.User;

public class Synchronizer {
    private SendMessageThread sendMessageThread;
    private User user;

    public Synchronizer(SendMessageThread sendMessageThread, User user) {
        this.sendMessageThread = sendMessageThread;
        this.user = user;
    }

    public void startPlayer() {
        if (user == User.SERVER) {
            sendMessageThread.start();
            sendMessageThread.addMessage(Message.getStartPlayerMessage());
        }
    }

    public void stop() {
        if (user == User.SERVER) {
            sendMessageThread.setKillRecieved();
        }
    }
}
