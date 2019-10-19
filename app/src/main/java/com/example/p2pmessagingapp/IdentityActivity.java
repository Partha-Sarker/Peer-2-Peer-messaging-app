package com.example.p2pmessagingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ShareActionProvider;
import android.widget.Toast;

public class IdentityActivity extends AppCompatActivity {

    public static String name;
    EditText nameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity);
        nameEditText = findViewById(R.id.nameEditText);
        nameEditText.setOnKeyListener( (v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                onSubmitClicked(null);
                return true;
            }
            return false;
        });
    }

    public void onSubmitClicked(View v){
        name = nameEditText.getText().toString().trim();
        if(name == null || name.equals(""))
            showToast("Name can't be empty");
        else if(name.length() > 10)
            showToast("Name length must be equal or less than 10");
        else if(!name.matches("[a-zA-Z]+"))
            showToast("Name can only contain letters and no space");
        else{
            SharedPreferences pref = getSharedPreferences("MyPref", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("name", name);
            editor.commit();
            Intent mainIntent = new Intent(this, MainActivity.class);
            finish();
            overridePendingTransition(0, 0);
            startActivity(mainIntent);
        }
    }

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
