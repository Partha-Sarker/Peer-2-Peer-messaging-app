package com.example.p2pmessagingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

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

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

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

        verifyStoragePermissions();
        verifyDataFolder();

//        Intent intent = new Intent().setType("text/plain").setAction(Intent.ACTION_GET_CONTENT);
//        startActivityForResult(Intent.createChooser(intent, "Select a TXT file"), 123);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode==123 && resultCode==RESULT_OK) {
            Uri uri = intent.getData();
            String path = getFilePathFromUri(uri);
            File file = new File(path);
            if(file.exists())
                Log.d(TAG, "Selected file exists");
            else
                Log.e(TAG, "Selected file doesn't exists");

            String fileText = readTextFile(uri);
            Log.d(TAG, "text inside file: "+fileText);
            sendReceive.write("file@%@"+file.getName()+"@%@"+fileText);
        }
    }

    private String readTextFile(Uri uri){
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String line = "";

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return builder.toString();
    }

    private String getFilePathFromUri(Uri uri){
        String path = uri.getPathSegments().get(1);
        path = Environment.getExternalStorageDirectory().getPath()+"/"+path.split(":")[1];
        return path;
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
        Intent intent = new Intent().setType("text/plain").setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a TXT file"), 123);
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
        writeToFile("conversations", allMessage, true);
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
                    runOnUiThread(() -> messageEditText.setText(""));
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
                    else{
                        if(messages[0].equals("background")){
                            changeBackground(messages[1]);
                            if(color == Color.BLACK)
                                message = "You have changed the background";
                            else
                                message = "Client has changed the background";
                        }
                        else{
                            if(color == Color.BLACK)
                                message = messages[1]+" has been sent";
                            else{
                                message = messages[1]+" has been received and downloaded";
                                writeToFile(messages[1], messages[2], false);
                            }
                        }

                        textView.setTextSize(15);
                        textView.setPadding(0, 10, 0, 10);
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
            messageEditText.requestFocus();
        });
    }

    private void writeToFile(String fileName, String data, boolean timeStamp) {
        Long time= System.currentTimeMillis();
        String timeMill = " "+time.toString();
        String path = Environment.getExternalStorageDirectory().toString();
        File file = null;
        if(timeStamp)
            file = new File(path+"/Peer 2 Peer/Conversations", fileName+timeMill+".txt");
        else
            file = new File(path+"/Peer 2 Peer/Saved txt files", fileName);
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

    public void verifyStoragePermissions() {
        // Check if we have write permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
        }
    }

    private void verifyDataFolder() {
        File folder = new File(Environment.getExternalStorageDirectory() + "/Peer 2 Peer");
        File folder1 = new File(folder.getPath() + "/Conversations");
        File folder2 = new File(folder.getPath() + "/Saved txt files");
        if(!folder.exists() || !folder.isDirectory()) {
            folder.mkdir();
            folder1.mkdir();
            folder2.mkdir();
        }
        else if(!folder1.exists())
            folder1.mkdir();
        else if(!folder2.exists())
            folder2.mkdir();
    }

}
