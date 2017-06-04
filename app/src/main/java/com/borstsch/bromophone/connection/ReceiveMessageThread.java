package com.borstsch.bromophone.connection;

import android.app.Activity;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.TextView;

import com.borstsch.bromophone.R;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;



class ReceiveMessageThread extends Thread {
    private Activity mActivity;
    private int hostPort;

    ReceiveMessageThread(Activity activity, int hostPort) {
        mActivity = activity;
        this.hostPort = hostPort;
    }

    @Override
    public void run() {
        try (Socket socket = new ServerSocket(hostPort).accept();
             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {

            // transfer JSONObject as String to the server
            //dataOutputStream.writeUTF(jsonData.toString());
            //Log.i(TAG, "waiting for response from host");

            // Thread will wait till server replies
            String signal = dataInputStream.readUTF();
            if (signal.equals(SendMessageThread.PLAY_COMMAND)) {
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}