package com.borstsch.bromophone;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import com.borstsch.bromophone.connection.Dispatcher;
import com.borstsch.bromophone.connection.SendMessageThread;
import com.borstsch.bromophone.musicplayer.PlayerActivity;

public class MainActivity extends AppCompatActivity {

    private Dispatcher mDispatcher;
    private UserType userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView textView = (TextView) findViewById(R.id.ip_text);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        textView.setText("Your Device IP Address: " + ipAddress);

        mDispatcher = new Dispatcher(this);

    }

    /** Called when the userType taps the Send button */
    public void onOKClick(View view) {
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void onServerClick(View view) throws IOException {
        if (userType == null || userType == UserType.SERVER) {
            mDispatcher.runServer();
            userType = UserType.SERVER;
            //Toast.makeText(this, "СОСИ ПИСОС", Toast.LENGTH_LONG).show();
        }
    }

    public void onClientClick(View view) {
        if (userType == null || userType == UserType.CLIENT) {
            mDispatcher.runClient();
            userType = UserType.CLIENT;
        }
    }

    public void onPlayClick(View view) {
        if (userType == UserType.SERVER) {
            SendMessageThread sendMessageThread = mDispatcher.getSendMessageThread();
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("user", userType);
            intent.putExtra("msg thread", sendMessageThread);
            startActivity(intent);
        } else if (userType == UserType.CLIENT) {
            TextView textView = (TextView) findViewById(R.id.ip_text);
            textView.setText("Wait For Server To Start a Party");
        } else {
            TextView textView = (TextView) findViewById(R.id.ip_text);
            textView.setText("Specify Your UserType Type First");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
