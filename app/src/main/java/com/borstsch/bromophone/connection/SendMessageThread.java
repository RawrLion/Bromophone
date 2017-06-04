package com.borstsch.bromophone.connection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class SendMessageThread extends Thread {
    static final String PLAY_COMMAND = "play";
    private List<String> clientIPs;
    private int mLocalPort;
    private Queue<JSONObject> messageQueue = new ConcurrentLinkedQueue<>();

    SendMessageThread(List<String> clientIPs, int localPort) {
        this.clientIPs = clientIPs;
        mLocalPort = localPort;
        try {
            messageQueue.add(new JSONObject().put(PLAY_COMMAND, ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        for (String address : clientIPs) {
            try (Socket socket = new Socket(InetAddress.getByName(address), mLocalPort);
                 DataInputStream is = new DataInputStream(socket.getInputStream());
                 DataOutputStream os = new DataOutputStream(socket.getOutputStream())) {

                JSONObject msg = messageQueue.poll();
                while (msg != null) {
                    os.writeUTF(msg.toString());
                    msg = messageQueue.poll();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean addMessage(JSONObject message) {
        return messageQueue.add(message);
    }
}
