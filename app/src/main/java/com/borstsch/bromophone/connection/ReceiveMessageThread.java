package com.borstsch.bromophone.connection;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.widget.TextView;

import com.borstsch.bromophone.R;
import com.borstsch.bromophone.User;
import com.borstsch.bromophone.musicplayer.PlayerActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;

import static com.borstsch.bromophone.connection.Message.END_MESSAGES_COMMAND;
import static com.borstsch.bromophone.connection.Message.PLAY_COMMAND;
import static com.borstsch.bromophone.connection.Message.START_PLAYER_COMMAND;


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
                    while (!(messageFromServer = dataInputStream.readUTF()).contains(END_MESSAGES_COMMAND)) {

                        JSONObject jsondata = new JSONObject(messageFromServer);

                        processMessage(jsondata);
                    }

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processMessage(final JSONObject message) {
        for (Iterator<String> msgIterator = message.keys(); msgIterator.hasNext();) {
            String command = msgIterator.next();
            switch (command) {
                case START_PLAYER_COMMAND: {
                    Intent intent = new Intent(mActivity, PlayerActivity.class);
                    intent.putExtra("user", User.CLIENT);
                    intent.putExtra("msg thread", (Serializable) null);
                    mActivity.startActivity(intent);
                }
                case PLAY_COMMAND: {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView textView = (TextView) mActivity.findViewById(R.id.ip_text);
                            try {
                                textView.append(message.getString(PLAY_COMMAND));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            MediaPlayer mediaPlayer = MediaPlayer.create(mActivity, R.raw.file);
                            mediaPlayer.start();
                        }
                    });
                }
            }
        }
    }
}