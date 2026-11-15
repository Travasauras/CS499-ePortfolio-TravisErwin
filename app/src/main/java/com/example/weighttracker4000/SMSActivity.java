package com.example.weighttracker4000;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class SMSActivity extends AppCompatActivity {

    private int userID;
    private WeightDatabase database;

    private Button enableSmsButton;
    private Button denySmsButton;

    private static final int SMS_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smspermission_screen);

        // Initialize database and retrieve the current user ID
        database = new WeightDatabase(this);
        userID = getIntent().getIntExtra(MainActivity.EXTRA_USER_ID, -1);

        // Connect buttons to the layout
        enableSmsButton = findViewById(R.id.enableSmsButton);
        denySmsButton = findViewById(R.id.denySmsButton);

        // Enable SMS alerts using the emulator phone number
        enableSmsButton.setOnClickListener(v -> enableSmsNotifications());

        // Disable SMS alerts and continue to the main menu
        denySmsButton.setOnClickListener(v -> {
            database.saveNotificationSettings(userID, false);
            goToMainMenu();
        });
    }

    // Enable SMS notifications and request permission if needed
    private void enableSmsNotifications() {

        // Save SMS notifications are enabled
        database.saveNotificationSettings(userID, true);

        // If SMS permission is already granted, continue to the main menu
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            goToMainMenu();
        } else {

            // Request SMS permission from the user
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_CODE
            );
        }
    }

    // Handle the result of the SMS permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {

            if (grantResults.length == 0 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                database.saveNotificationSettings(userID, false);
            }

            goToMainMenu();
        }
    }

    // Open the main menu and close this screen so Back doesn't return to the SMS question
    private void goToMainMenu() {
        Intent intent = new Intent(SMSActivity.this, MainMenuActivity.class);
        intent.putExtra(MainActivity.EXTRA_USER_ID, userID);
        startActivity(intent);
        finish();
    }
}