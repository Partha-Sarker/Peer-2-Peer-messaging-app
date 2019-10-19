package com.example.p2pmessagingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;


import static java.lang.Thread.sleep;

public class SplashScreen extends AppCompatActivity {

    ImageView splashImage;
    Intent mainIntent, nameIntent;

    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        splashImage = findViewById(R.id.splashImage);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        splashImage.startAnimation(fadeIn);

        mainIntent = new Intent(this, MainActivity.class);
        nameIntent = new Intent(this, IdentityActivity.class);

        pref = getSharedPreferences("MyPref", MODE_PRIVATE);

        showAnimation();

    }

    private void showAnimation() {
        new Thread(()->{

            try {
                sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                fadeOut();
            }
        }).start();

        new Thread(()->{

            try {
                sleep(3500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                checkNextActivity();
            }
        }).start();
    }

    private void checkNextActivity() {
        finish();
        overridePendingTransition(0, 0);
        boolean firstTime = pref.getBoolean("firstTime", true);
        if(firstTime) {
            Editor editor = pref.edit();
            editor.putBoolean("firstTime", false);
            editor.commit();
            startActivity(nameIntent);
            return;
        }
        startActivity(mainIntent);
        return;
    }

    public void fadeOut(){
        runOnUiThread( ()-> {
            Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            fadeOut.setFillAfter(true);
            splashImage.startAnimation(fadeOut);
        });
    }

}
