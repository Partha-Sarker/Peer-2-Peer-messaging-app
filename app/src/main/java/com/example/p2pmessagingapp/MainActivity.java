package com.example.p2pmessagingapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    ConstraintLayout layout;

    EditText receivePortEditText, targetPortEditText, messageEditText, targetIPEditText;
    ScrollView conversations;
    LinearLayout conversationLayout;

    MenuItem saveConversation, changeBackground, sendTextFile;

    Button sendButton;

    ServerClass serverClass;
    ClientClass clientClass;

    SendReceive sendReceive;

    static final int MESSAGE_READ = 1;
    static final String TAG = "ahtrap";

    Handler handler = new Handler(msg -> {
        if (msg.what == MESSAGE_READ) {
            byte[] readBuff = (byte[]) msg.obj;
            String tempMsg = new String(readBuff, 0, msg.arg1);
            addMessage(Color.BLUE, tempMsg);
        }
        return true;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout = findViewById(R.id.layout);
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
        saveConversation = menu.findItem(R.id.saveConversation);
        saveConversation.setEnabled(false);
        changeBackground = menu.findItem(R.id.changeBackground);
        changeBackground.setEnabled(false);
        sendTextFile = menu.findItem(R.id.sendTextFile);
        sendTextFile.setEnabled(false);
        return true;
    }

    public void onStartServerClicked(View v) {
        String port = receivePortEditText.getText().toString();
        serverClass = new ServerClass(Integer.parseInt(port));
        serverClass.start();
    }

    public void onConnectClicked(View v) {
        String port = targetPortEditText.getText().toString();
        clientClass = new ClientClass(targetIPEditText.getText().toString(), Integer.parseInt(port));
        clientClass.start();
    }

    public void onSendClicked(View v) {
        String msg = messageEditText.getText().toString();
        if (msg != null && !msg.equals("")) sendReceive.write("message@%@"+msg);
    }

    public void onSendTextFileClicked(MenuItem item) {
        showToast(item.getTitle().toString());
    }

    public void onSaveConversationClicked(MenuItem item) {
        String allMessage = "";
        int count = conversationLayout.getChildCount();
        TextView children;
        for (int i = 0; i < count; i++) {
            children = (TextView) conversationLayout.getChildAt(i);
            if (children.getCurrentTextColor() == Color.BLACK) {
                allMessage += "ME: " + children.getText().toString() + "\n\n";
            } else {
                allMessage += "CLIENT: " + children.getText().toString() + "\n\n";
            }
        }
        writeToFile(allMessage);
    }

    public void onChangeBackgroundClicked(MenuItem item) {
        showToast("Changing Background...");
        changeBackground(item.getTitle().toString());
        sendReceive.write("background@%@"+item.getTitle());
    }

    private class SendReceive extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt) {
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (socket != null) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(String msg) {
            new Thread(() -> {
                try {
                    outputStream.write(msg.getBytes());
                    addMessage(Color.BLACK, msg);
                    runOnUiThread(() ->
                            messageEditText.setText("")
                    );
                } catch (IOException e) {
                    Log.d(TAG, "Can't send message: " + e);
                } catch (Exception e) {
                    Log.d(TAG, "Error: " + e);
                }
            }).start();

        }
    }

    public class ServerClass extends Thread {
        Socket socket;
        ServerSocket serverSocket;
        int port;

        public ServerClass(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                Looper.prepare();
                showToast("Server Started. Waiting for client...");
                Log.d(TAG, "Waiting for client...");
                socket = serverSocket.accept();
                Log.d(TAG, "Connection established from server");
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "ERROR: " + e);
            } catch (Exception e) {
                Log.d(TAG, "ERROR: " + e);
            }
        }
    }

    public class ClientClass extends Thread {
        Socket socket;
        String hostAdd;
        int port;

        public ClientClass(String hostAddress, int port) {
            this.port = port;
            this.hostAdd = hostAddress;
        }

        @Override
        public void run() {
            try {

                socket = new Socket(hostAdd, port);
                sendReceive = new SendReceive(socket);
                sendReceive.start();
                showToast("Connected to other device. You can now exchange messages.");
                Log.d(TAG, "Client is connected to server");
                enableComponent();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Can't connect to server. Check the IP address and Port number and try again: " + e);
            } catch (Exception e) {
                Log.d(TAG, "ERROR: " + e);
            }
        }
    }

    private void addMessage(int color, String fullMessage) {

        runOnUiThread(() -> {
                    TextView textView = new TextView(this);
                    textView.setTextSize(20);
                    textView.setTextColor(color);
                    if (color == Color.BLACK) {
                        textView.setPadding(200, 20, 10, 10);
                        textView.setGravity(Gravity.RIGHT);
                    } else if(color == Color.BLUE){
                        textView.setPadding(10, 20, 200, 10);
                    }

                    String message = fullMessage;

                    String[] messages = message.split("@%@", 0);

                    if(messages[0].equals("message"))
                        message = messages[1];
                    else if(messages[0].equals("background")){
                        changeBackground(messages[1]);
                        if(color == Color.BLACK)
                            message = "You have changed the background";
                        else
                            message = "Client has changed the background";
                        textView.setTextSize(15);
                        textView.setPadding(0, 0, 0, 0);
                        textView.setGravity(Gravity.CENTER);
                    }

                    textView.setText(message);
                    conversationLayout.addView(textView);
                    conversations.post(() -> conversations.fullScroll(View.FOCUS_DOWN));
                }
        );
    }

    public void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    public void enableComponent() {
        runOnUiThread(() -> {
            messageEditText.setEnabled(true);
            sendButton.setEnabled(true);
            saveConversation.setEnabled(true);
            sendTextFile.setEnabled(true);
            changeBackground.setEnabled(true);
        });
    }

    private void writeToFile(String data) {
        Long time= System.currentTimeMillis();
        File path = this.getExternalFilesDir(null);
        File file = new File(path, "Conversations "+time.toString()+".txt");
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(file, false);
            stream.write(data.getBytes());
            stream.close();
            showToast("file saved in: "+file.getPath());
        } catch (FileNotFoundException e) {
            Log.d(TAG, e.toString());
        } catch (IOException e) {
            Log.d(TAG, e.toString());
        }
    }

    private void changeBackground(String message){

        runOnUiThread( ()-> {
            if(message.equals("Background 1"))
                layout.setBackgroundResource(R.drawable.bg1);
            else if(message.equals("Background 2"))
                layout.setBackgroundResource(R.drawable.bg2);
            else if(message.equals("Background 3"))
                layout.setBackgroundResource(R.drawable.bg3);
            else if(message.equals("Background 4"))
                layout.setBackgroundResource(R.drawable.bg4);
            else
                layout.setBackgroundColor(Color.WHITE);
        });
    }

}
