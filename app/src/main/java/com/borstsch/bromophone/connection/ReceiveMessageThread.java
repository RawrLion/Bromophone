package com.borstsch.bromophone.connection;

import android.app.Activity;
import android.media.MediaPlayer;
import android.widget.TextView;

import com.borstsch.bromophone.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;


class ReceiveMessageThread extends Thread {
    private Activity mActivity;
    private int hostPort;

    ReceiveMessageThread(Activity activity, int hostPort) {
        mActivity = activity;
        this.hostPort = hostPort;
    }


    @Override
    public void run() {
        String messageFromServer;
        try (ServerSocket serverSocket = new ServerSocket(hostPort)) {
            while (true) {
                try (Socket socket = serverSocket.accept();
                     DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                     DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                ) {
                    messageFromServer = dataInputStream.readUTF();

                    JSONObject jsondata = new JSONObject(messageFromServer);

                    processMessage(jsondata);

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processMessage(JSONObject message) {
        for (Iterator<String> msgIterator = message.keys(); msgIterator.hasNext();) {
            String request = msgIterator.next();
            switch (request) {
                case SendMessageThread.PLAY_COMMAND: {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView textView = (TextView) mActivity.findViewById(R.id.ip_text);
                            textView.setText("PLAYING MUSIC");
                            MediaPlayer mediaPlayer = MediaPlayer.create(mActivity, R.raw.file);
                            mediaPlayer.start();
                        }
                    });
                }
            }
        }
    }
}