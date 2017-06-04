package com.borstsch.bromophone.connection;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import com.borstsch.bromophone.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class Dispatcher {
    private static final String REQUEST_CONNECT_CLIENT = "request-connect-client";
    public static final String CONNECTION_ACCEPTED_MSG = "Connection Accepted";

    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private String mServiceName;
    private List<String> clientIPs;

    private int hostPort;
    private Integer mLocalPort;
    private ServerSocket mServerSocket;
    private InetAddress hostAddress;

    private Activity mActivity;

    public Dispatcher(Activity activity) {
        mActivity = activity;
    }

    private class MyResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails.  Use the error code to debug.
            Log.e(TAG, "Resolve failed" + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

            if (serviceInfo.getServiceName().equals(mServiceName)) {
                Log.d(TAG, "Same IP.");
                return;
            }
            hostPort = serviceInfo.getPort();
            hostAddress = serviceInfo.getHost();

            connectToHost();
        }
    }

    private class SocketServerThread extends Thread {
        @Override
        public void run() {
            Log.i(TAG, "Creating server socket");
            String messageFromClient;
            String request;
            while (true) {
                try (Socket socket = mServerSocket.accept();
                     DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                     DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                ) {

                //If no message sent from client, this code will block the program
                messageFromClient = dataInputStream.readUTF();

                JSONObject jsondata = new JSONObject(messageFromClient);

                request = jsondata.getString("request");

                if (request.equals(REQUEST_CONNECT_CLIENT)) {
                    String clientIPAddress = jsondata.getString("ipAddress");
                    // Add client IP to a list
                    clientIPs.add(clientIPAddress);
                    dataOutputStream.writeUTF(CONNECTION_ACCEPTED_MSG);
                } else {
                    // There might be other queries, but as of now nothing.
                    dataOutputStream.flush();
                }

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void runServer() throws IOException {
        clientIPs = new ArrayList<>();
        mServerSocket = new ServerSocket(0);
        mLocalPort =  mServerSocket.getLocalPort();
        SocketServerThread socketServerThread = new SocketServerThread();
        socketServerThread.start();
        initializeRegistrationListener();
        while (mLocalPort == null) {}
        registerService(mLocalPort);
    }

    public void runClient() {
        initializeDiscoveryListener();
        mNsdManager = (NsdManager) mActivity.getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices(
                "_http._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    private void registerService(int port) {
        // Create the NsdServiceInfo object, and populate it.
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName("NsdSound");
        serviceInfo.setServiceType("_http._tcp.");
        serviceInfo.setPort(port);

        mNsdManager = (NsdManager) mActivity.getSystemService(Context.NSD_SERVICE);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    private void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mServiceName = NsdServiceInfo.getServiceName();
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView) mActivity.findViewById(R.id.ip_text);
                        textView.setText("Success, port: " + mLocalPort + ", Service name: " + mServiceName);
                    }
                });
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
            }
        };
    }

    private void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(final NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals("_http._tcp.")) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().contains("NsdSound")){
                    mNsdManager.resolveService(service, new MyResolveListener());

                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };

    }

    private void connectToHost() {

        if (hostAddress == null) {
            Log.e(TAG, "Host Address is null");
            return;
        }

        String ipAddress = getLocalIpAddress();
        JSONObject jsonData = new JSONObject();

        try {
            jsonData.put("request", REQUEST_CONNECT_CLIENT);
            jsonData.put("ipAddress", ipAddress);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "can't put request");
            return;
        }

        new SocketServerTask(hostPort, hostAddress, mActivity).execute(jsonData);
    }

    private String getLocalIpAddress() {
        WifiManager wm = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    public void sendMessage() {
        SendMessageThread sendThread = new SendMessageThread(clientIPs, mLocalPort);
        sendThread.start();
    }
}
