package com.borstsch.bromophone.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

class SendMessageThread extends Thread {
    static final String PLAY_COMMAND = "play";
    private List<String> clientIPs;
    private int mLocalPort;

    SendMessageThread(List<String> clientIPs, int localPort) {
        this.clientIPs = clientIPs;
        mLocalPort = localPort;
    }

    @Override
    public void run() {
        for (String address : clientIPs) {
            try (Socket socket = new Socket(InetAddress.getByName(address), mLocalPort);
                 DataInputStream is = new DataInputStream(socket.getInputStream());
                 DataOutputStream os = new DataOutputStream(socket.getOutputStream())) {

                os.writeUTF(PLAY_COMMAND);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
