package com.example.p2pmessagingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements WritableGUI{

    EditText receivePortEditText, targetPortEditText, messageEditText, targetIPEditText;
    TextView chatText;
    MessageListener listener;

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

    @Override
    public void write(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        chatText.append("\n"+s);
    }

    public void onListenClicked(View v){
        listener = new MessageListener(Integer.parseInt(receivePortEditText.getText().toString()), this);
        listener.start();
    }

    public void onSendClicked(View v){
        MessageTransmitter transmitter = new MessageTransmitter(messageEditText.getText().toString(), targetIPEditText.getText().toString(), Integer.parseInt(targetPortEditText.getText().toString()));
        transmitter.start();
    }

}
