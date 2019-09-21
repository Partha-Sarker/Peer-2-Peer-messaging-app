package com.example.p2pmessagingapp;

import android.os.Debug;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class MessageTransmitter extends Thread{

    String message, hostName;
    int port;

    public MessageTransmitter(String message, String hostName, int port) {
        this.message = message;
        this.hostName = hostName;
        this.port = port;
    }

    @Override
    public void run() {
        OutputStreamWriter osw;
        Socket socket = null;
        try {
            socket = new Socket(hostName, port);
            Log.d("ahtrap", "Connected to: "+hostName);
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

            // Send first message
            dOut.writeUTF(message);
            //dOut.flush(); // Send off the data

            dOut.close();
            socket.close();
        } catch (IOException e) {
            Log.d("ahtrap", "Can't connect to: "+hostName+" on port: "+port);
            e.printStackTrace();
        }

    }
}
