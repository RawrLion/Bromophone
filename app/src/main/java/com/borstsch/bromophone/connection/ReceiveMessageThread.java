package com.borstsch.bromophone.connection;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.borstsch.bromophone.UserType;
import com.borstsch.bromophone.musicplayer.PlayerActivity;

import org.greenrobot.eventbus.EventBus;
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
import static com.borstsch.bromophone.connection.Message.START_PLAYER_COMMAND;


public class ReceiveMessageThread extends Thread {
    private final UserType userType = UserType.CLIENT;
    private Activity mActivity;
    private int hostPort;
    private static boolean killReceived = false;

    ReceiveMessageThread(Activity activity, int hostPort) {
        mActivity = activity;
        this.hostPort = hostPort;
    }

    public static void setKillReceived() {
        killReceived = true;
    }


    @Override
    public void run() {
        String messageFromServer;
        try (ServerSocket serverSocket = new ServerSocket(hostPort)) {
            while (!killReceived) {
                try (Socket socket = serverSocket.accept();
                     DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                     DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())
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

    private void processMessage(@NonNull final JSONObject message) {
        for (Iterator<String> msgIterator = message.keys(); msgIterator.hasNext();) {
            String command = msgIterator.next();
            switch (command) {
                case START_PLAYER_COMMAND: {
                    Intent intent = new Intent(mActivity, PlayerActivity.class);
                    intent.putExtra("userType", userType);
                    intent.putExtra("msg thread", (Serializable) null);
                    mActivity.startActivity(intent);
                    break;
                }
                default: {
                    EventBus.getDefault().post(message);
                    break;
                }
            }
        }
    }
}