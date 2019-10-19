package com.example.p2pmessagingapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    ConstraintLayout layout;

    EditText receivePortEditText, targetPortEditText, messageEditText, targetIPEditText;
    ScrollView conversations;
    LinearLayout conversationLayout;

    MenuItem saveConversation, changeBackground, sendTextFile;

    Button startServerButton, connectButton;
    ImageButton sendButton;

    ServerClass serverClass;
    ClientClass clientClass;

    SendReceive sendReceive;

    WifiManager wifiManager;

    String myIP, currentBackground, allMessage = "", myName, clientName = "CLIENT";

    boolean wasClient = false, firstMessage = true, clientFirstMessage = true, darkModeEnabled;

    static final int MESSAGE_READ = 1;
    static final String TAG = "ahtrap";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    Handler handler = new Handler(msg -> {
        if (msg.what == MESSAGE_READ) {
            byte[] readBuff = (byte[]) msg.obj;
            String tempMsg = new String(readBuff, 0, msg.arg1);
            if(clientFirstMessage) {
                clientName = tempMsg;
                clientFirstMessage = false;
                showToast("Client name is: "+clientName);
                setTitle(clientName);
            }
            else
                addMessage(Color.BLUE, tempMsg);
        }
        return true;
    });

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
                    showToast("Other device is unreachable");
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
                //enableChatComponent();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "ERROR: " + e);
            } catch (Exception e) {
                Log.d(TAG, "ERROR: " + e);
                showToast("Can't create server socket. Check wifi state and port and try again");
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
                enableChatComponent();
                sendReceive.write(myName);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Can't connect to server. Check the IP address and Port number and try again: " + e);
            } catch (Exception e) {
                Log.d(TAG, "ERROR: " + e);
            }
        }
    }

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
        startServerButton = findViewById(R.id.startServerButton);
        connectButton = findViewById(R.id.connectDisconnectButton);

        conversations = findViewById(R.id.conversations);
        conversationLayout = new LinearLayout(this);
        conversationLayout.setPaddingRelative(0, 100, 0, 50);
        conversationLayout.setOrientation(LinearLayout.VERTICAL);
        conversations.addView(conversationLayout);

        currentBackground = "White";

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
            showToast("Turning on wifi...");
        }

        verifyStoragePermissions();
        verifyDataFolder();

        receivePortEditText.requestFocus();

        pref = getSharedPreferences("MyPref", MODE_PRIVATE);
        editor = pref.edit();
        myName = pref.getString("name", "ME");
        darkModeEnabled = pref.getBoolean("darkMode", false);
        if(darkModeEnabled){
            darkModeEnabled = false;
            onDarkModeClicked(null);
        }
        showToast("Welcome "+myName);

        KeyboardVisibilityEvent.setEventListener( this, isOpen -> runOnUiThread( ()-> conversations.post(() -> conversations.fullScroll(View.FOCUS_DOWN))));
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
            sendReceive.write("file@%@"+file.getName()+"@%@"+fileText);
        }
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
        if(port == null || port.equals("")){
            showToast("Type a valid port");
            return;
        }
        serverClass = new ServerClass(Integer.parseInt(port));
        serverClass.start();
    }

    public void onConnectClicked(View v) {
        String port = targetPortEditText.getText().toString();
        String hostIP = targetIPEditText.getText().toString();
        if(port == null || port.equals("") || hostIP == null || hostIP.equals("")){
            showToast("Port or IP address is invalid");
            return;
        }
        clientClass = new ClientClass(hostIP, Integer.parseInt(port));
        clientClass.start();
    }

    public void onSendClicked(View v) {
        String msg = messageEditText.getText().toString();
        msg = msg.trim();
        if (msg != null && !msg.equals(""))
            sendReceive.write("message@%@"+msg);
    }

    public void onWifiClicked(MenuItem item){
        if(!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
            item.setIcon(R.drawable.wifi_on);
            showToast("Turning on wifi...");
        }
        else{
            wifiManager.setWifiEnabled(false);
            item.setIcon(R.drawable.wifi_off);
            showToast("Turning off wifi...");
        }
    }

    public void onMyIPClicked(MenuItem item){
        myIP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        if(myIP.equals("0.0.0.0"))
            showToast("Connect this device to wifi first");
        else
            Toast.makeText(this, myIP, Toast.LENGTH_LONG).show();
    }

    public void onSendTextFileClicked(MenuItem item) {
        Intent intent = new Intent().setType("text/plain").setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a TXT file"), 123);
    }

    public void onSaveConversationClicked(MenuItem item) {
        writeToFile("conversation with "+clientName, allMessage, true);
        saveConversation.setEnabled(false);
        new Thread( ()->{
            try {
                sleep(1100);
            } catch (InterruptedException e) {
                Log.e(TAG, e.toString());
            } finally {
                runOnUiThread( ()-> saveConversation.setEnabled(true));
            }

        } ).start();

    }

    public void onChangeBackgroundClicked(MenuItem item) {
        if(item.getTitle().equals(currentBackground))
            return;
        showToast("Changing Background...");
        changeBackground(item.getTitle().toString());
        sendReceive.write("background@%@"+item.getTitle());
        currentBackground = item.getTitle().toString();
    }

    public void onDarkModeClicked(MenuItem item){
        if(darkModeEnabled){
            darkModeEnabled = false;
            changeBackground(currentBackground);
        }
        else{
            darkModeEnabled = true;
            layout.setBackgroundColor(Color.BLACK);
        }
        editor.putBoolean("darkMode", darkModeEnabled);
        editor.commit();
    }

    private void addMessage(int color, String fullMessage) {

        if(fullMessage.equals(myName))
            return;

        runOnUiThread(() -> {
                    Date date=new Date();
                    SimpleDateFormat sdf=new SimpleDateFormat("hh:mm a");
                    String currentTime = sdf.format(date);

                    TextView textView = new TextView(this);
                    textView.setTextSize(20);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                    textView.setLayoutParams(params);
                    textView.setPadding(20, 10, 20, 10);

                    TextView dateView = new TextView(this);
                    dateView.setTextColor(Color.GRAY);
                    dateView.setText(currentTime);
                    LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    dateView.setTextSize(10);
                    dateView.setLayoutParams(dateParams);

                    if (color == Color.BLACK) {
                        allMessage += "You ("+currentTime+"): ";
                        textView.setTextColor(Color.WHITE);
                        params.setMargins(200,0,10,5);

                        if(wasClient || firstMessage)
                            dateParams.setMargins(0,30,20,5);
                        else
                            dateParams.setMargins(0,10,20,5);

                        dateParams.gravity = Gravity.RIGHT;
                        params.gravity = Gravity.RIGHT;
                        textView.setBackgroundResource(R.drawable.my_message);
                        wasClient = false;
                    } else if(color == Color.BLUE){
                        allMessage += clientName+" ("+currentTime+"): ";
                        textView.setTextColor(Color.BLACK);
                        params.setMargins(10,0,200,5);
                        if(!wasClient || firstMessage)
                            dateParams.setMargins(20,30,0,5);
                        else
                            dateParams.setMargins(20,10,0,5);
                        textView.setBackgroundResource(R.drawable.client_message);
                        wasClient = true;
                    }

                    String message = fullMessage;

                    String[] messages = message.split("@%@", 0);

                    if(messages[0].equals("message"))
                        message = messages[1];
                    else{
                        message = "chicki chicki :(";
                        if(messages[0].equals("background")){
                            changeBackground(messages[1]);
                            if(color == Color.BLACK)
                                message = "Background has been changed to "+messages[1];
                            else
                                message = "Background has been changed to "+messages[1];
                        }
                        else if(messages[0].equals("file")){
                            if(color == Color.BLACK){
                                message = messages[1]+" has been sent";
                                showToast(message);
                            }
                            else{
                                message = messages[1]+" has been received and downloaded";
                                writeToFile(messages[1], messages[2], false);
                            }

                        }
                        dateParams.setMargins(0,15,0,5);
                        dateParams.gravity = Gravity.CENTER;
                        textView.setTextSize(15);
                        textView.setPadding(5, 10, 5, 10);
                        params.setMargins(100,0,100,5);
                        textView.setGravity(Gravity.CENTER);

                    }
                    textView.setText(message);
                    allMessage += message+"\n\n";
                    conversationLayout.addView(dateView);
                    conversationLayout.addView(textView);
                    conversations.post(() -> conversations.fullScroll(View.FOCUS_DOWN));
                    firstMessage = false;
                }
        );
    }

    public void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    public void enableChatComponent() {
        runOnUiThread(() -> {
            messageEditText.setVisibility(View.VISIBLE);
            sendButton.setVisibility(View.VISIBLE);
            saveConversation.setEnabled(true);
            sendTextFile.setEnabled(true);
            changeBackground.setEnabled(true);

            receivePortEditText.setVisibility(View.GONE);
            targetIPEditText.setVisibility(View.GONE);
            targetPortEditText.setVisibility(View.GONE);
            connectButton.setVisibility(View.GONE);
            startServerButton.setVisibility(View.GONE);


            messageEditText.requestFocus();
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(messageEditText, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void writeToFile(String fileName, String data, boolean timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());
        String path = Environment.getExternalStorageDirectory().toString();
        File file = null;
        if(timeStamp)
            file = new File(path+"/Peer 2 Peer/Conversations", fileName+" "+currentDateandTime+".txt");
        else
            file = new File(path+"/Peer 2 Peer/Saved Files", fileName);
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
        currentBackground = message;
        if(darkModeEnabled)
            return;
        runOnUiThread( ()-> {
            if(message.equals("Background 1"))
                layout.setBackgroundResource(R.drawable.bg1);
            else if(message.equals("Background 2"))
                layout.setBackgroundResource(R.drawable.bg2);
            else if(message.equals("Background 3"))
                layout.setBackgroundResource(R.drawable.bg3);
            else if(message.equals("Background 4"))
                layout.setBackgroundResource(R.drawable.bg4);
            else if(message.equals("White"))
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
        File folder2 = new File(folder.getPath() + "/Saved Files");
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

    private String readTextFile(Uri uri){
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String line = "";

            while ((line = reader.readLine()) != null) {
                builder.append("\n"+line);
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


}
