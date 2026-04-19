package com.basya.gramm.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.basya.gramm.App;
import com.basya.gramm.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView emoji = findViewById(R.id.tv_splash_emoji);
        TextView title = findViewById(R.id.tv_splash_title);
        TextView sub   = findViewById(R.id.tv_splash_sub);

        AnimationSet set = new AnimationSet(true);
        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        alpha.setDuration(900);
        ScaleAnimation scale = new ScaleAnimation(
            0.4f, 1f, 0.4f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(900);
        set.addAnimation(alpha);
        set.addAnimation(scale);
        emoji.startAnimation(set);
        title.startAnimation(set);

        AlphaAnimation subAnim = new AlphaAnimation(0f, 1f);
        subAnim.setDuration(700);
        subAnim.setStartOffset(700);
        sub.startAnimation(subAnim);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i;
                if (App.loggedIn() && !App.username().isEmpty()) {
                    i = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    i = new Intent(SplashActivity.this, AuthActivity.class);
                }
                startActivity(i);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        }, 2200);
    }
}
