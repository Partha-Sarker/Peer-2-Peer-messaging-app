package com.example.p2pmessagingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity{

    EditText receivePortEditText, targetPortEditText, messageEditText, targetIPEditText;
    TextView chatText;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;

    static final int MESSAGE_READ=1;
    static final String TAG = "ahtrap";

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what)
            {
                case MESSAGE_READ:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    chatText.setText(tempMsg);
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        receivePortEditText = findViewById(R.id.receiveEditText);
        targetPortEditText = findViewById(R.id.targetPortEditText);
        messageEditText = findViewById(R.id.messageEditText);
        targetIPEditText = findViewById(R.id.targetIPEditText);
        chatText = findViewById(R.id.chatText);
    }

    public void onHostClicked(View v){
        String port = receivePortEditText.getText().toString();
        serverClass = new ServerClass(Integer.parseInt(port));
        serverClass.start();
    }

    public void onConnectClicked(View v){
        String port = targetPortEditText.getText().toString();
        clientClass = new ClientClass(targetIPEditText.getText().toString(), Integer.parseInt(port));
        clientClass.start();
    }

    public void onSendClicked(View v){
        String msg=messageEditText.getText().toString();
        sendReceive.write(msg.getBytes());
    }

    public class ServerClass extends Thread{
        Socket socket;
        ServerSocket serverSocket;
        int port;

        public ServerClass(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                serverSocket=new ServerSocket(port);
                Log.d(TAG, "Waiting for client...");
                socket=serverSocket.accept();
                Log.d(TAG, "Connection established from server");
                sendReceive=new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "ERROR/n"+e);
            }
        }
    }

    private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt)
        {
            socket=skt;
            try {
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer=new byte[1024];
            int bytes;

            while (socket!=null)
            {
                try {
                    bytes=inputStream.read(buffer);
                    if(bytes>0)
                    {
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ClientClass extends Thread{
        Socket socket;
        String hostAdd;
        int port;

        public  ClientClass(String hostAddress, int port)
        {
            this.port = port;
            this.hostAdd = hostAddress;
        }

        @Override
        public void run() {
            try {

                socket=new Socket(hostAdd, port);
                Log.d(TAG, "Client is connected to server");
                sendReceive=new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Can't connect from client/n"+e);
            }
        }
    }

}
