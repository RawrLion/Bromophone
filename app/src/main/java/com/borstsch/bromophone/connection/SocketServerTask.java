package com.borstsch.bromophone.connection;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.borstsch.bromophone.R;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import static android.content.ContentValues.TAG;
import static com.borstsch.bromophone.connection.Dispatcher.CONNECTION_ACCEPTED_MSG;


public class SocketServerTask extends AsyncTask<JSONObject, Void, Void> {
    private boolean success;
    private final InetAddress hostAddress;
    private final int hostPort;
    private final Activity mActivity;

    SocketServerTask(int hostPort, InetAddress hostAddress, Activity activity) {
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
        mActivity = activity;
    }

    @Override
    protected Void doInBackground(JSONObject... params) {
        JSONObject jsonData = params[0];

        try (Socket socket = new Socket(hostAddress, hostPort);
             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        ) {
            // transfer JSONObject as String to the server
            dataOutputStream.writeUTF(jsonData.toString());
            Log.i(TAG, "waiting for response from host");

            // Thread will wait till server replies
            String response = dataInputStream.readUTF();
            success = response.equals(CONNECTION_ACCEPTED_MSG);
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        TextView textView = (TextView) mActivity.findViewById(R.id.ip_text);
        if (success) {
            textView.setText("Connected");
            receiveMessage();
        } else {
            textView.setText("NOT Connected");
        }
    }

    private void receiveMessage() {
        ReceiveMessageThread receiveThread = new ReceiveMessageThread(mActivity, hostPort);
        receiveThread.start();
    }
}