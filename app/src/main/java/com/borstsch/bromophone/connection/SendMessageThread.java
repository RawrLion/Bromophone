package com.borstsch.bromophone.connection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.borstsch.bromophone.connection.Message.END_MESSAGES_COMMAND;

public class SendMessageThread extends Thread implements Serializable {
    private List<String> clientIPs;
    private int mLocalPort;
    private Queue<JSONObject> messageQueue = new ConcurrentLinkedQueue<>();
    private boolean killRecieved = false;

    SendMessageThread(List<String> clientIPs, int localPort) {
        this.clientIPs = clientIPs;
        mLocalPort = localPort;
    }

    public void setKillRecieved() {
        killRecieved = true;
    }

    @Override
    public void run() {
        JSONObject msg;
        while (!killRecieved) {
            for (String address : clientIPs) {
                try (Socket socket = new Socket(InetAddress.getByName(address), mLocalPort);
                     DataInputStream is = new DataInputStream(socket.getInputStream());
                     DataOutputStream os = new DataOutputStream(socket.getOutputStream())) {

                    while ((msg = messageQueue.poll()) != null) {
                        os.writeUTF(msg.toString());
                    }
                    os.writeUTF(new JSONObject().put(END_MESSAGES_COMMAND, "").toString());

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean addMessage(JSONObject message) {
        return messageQueue.add(message);
    }
}
