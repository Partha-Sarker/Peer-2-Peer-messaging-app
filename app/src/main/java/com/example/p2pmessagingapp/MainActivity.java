package com.example.p2pmessagingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class MainActivity extends AppCompatActivity{

    EditText receivePortEditText, targetPortEditText, messageEditText, targetIPEditText;
    ScrollView conversations;
    LinearLayout conversationLayout;

    Button sendButton;

    ServerClass serverClass;
    ClientClass clientClass;

    SendReceive sendReceive;

    static final int MESSAGE_READ=1;
    static final String TAG = "ahtrap";


    Handler handler = new Handler(msg -> {
        if(msg.what == MESSAGE_READ){
            byte[] readBuff= (byte[]) msg.obj;
            String tempMsg=new String(readBuff,0,msg.arg1);
            addMessage(Color.BLUE, tempMsg);
        }
        return true;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        receivePortEditText = findViewById(R.id.receiveEditText);
        targetPortEditText = findViewById(R.id.targetPortEditText);
        messageEditText = findViewById(R.id.messageEditText);
        targetIPEditText = findViewById(R.id.targetIPEditText);
        sendButton = findViewById(R.id.sendButton);

        conversations = findViewById(R.id.conversations);
        conversationLayout = new LinearLayout(this);
        conversationLayout.setOrientation(LinearLayout.VERTICAL);
        conversations.addView(conversationLayout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public void onStartServerClicked(View v){
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
        if(msg != null && !msg.equals("")) sendReceive.write(msg);
    }

    public void onSendTextFileClicked(MenuItem item){
        showToast(item.getTitle().toString());
    }

    public void onSaveConversationClicked(MenuItem item){
        showToast(item.getTitle().toString());
    }

    public void onChangeWallpaperClicked(MenuItem item){
        showToast(item.getTitle().toString());
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

        public void write(String msg)
        {
            try {
                outputStream.write(msg.getBytes());
                addMessage(Color.BLACK, msg);
                messageEditText.setText("");
            } catch (IOException e) {
                Log.d(TAG, "Can't send message: "+e);
            }
        }
//        public void write(final byte[] bytes) {
//            new Thread(new Runnable(){
//                @Override
//                public void run() {
//                    try {
//                        outputStream.write(bytes);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
//
//        }
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
                Looper.prepare();
                showToast("Server Started. Waiting for client...");
                Log.d(TAG, "Waiting for client...");
                socket=serverSocket.accept();
                Log.d(TAG, "Connection established from server");
                sendReceive=new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "ERROR: "+e);
            } catch (Exception e){
                Log.d(TAG, "ERROR: "+e);
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
                sendReceive=new SendReceive(socket);
                sendReceive.start();
                showToast("Connected to other device. You can now exchange messages.");
                Log.d(TAG, "Client is connected to server");
                enableComponent();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Can't connect to server. Check the IP address and Port number and try again: "+e);
            } catch (Exception e){
                Log.d(TAG, "ERROR: "+e);
            }
        }
    }

    private void addMessage(int color, String message) {
        TextView textView = new TextView(this);
        textView.setPadding(10,20,20,10);
        textView.setTextColor(color);
        textView.setTextSize(20);
        textView.setText(message);
        conversationLayout.addView(textView);
        conversations.post(new Runnable() {
            @Override
            public void run() {
                conversations.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    public void showToast(String message){
        runOnUiThread( () -> {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    public void enableComponent(){
        runOnUiThread( () -> {
            messageEditText.setEnabled(true);
            sendButton.setEnabled(true);
        });
    }

}
