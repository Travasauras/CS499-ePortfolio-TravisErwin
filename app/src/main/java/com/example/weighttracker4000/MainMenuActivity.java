package com.example.weighttracker4000;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;

public class MainMenuActivity extends AppCompatActivity {

    private int userID;
    private WeightDatabase database;
    private TextView todaySummaryText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_menu_screen);

        database = new WeightDatabase(this);
        userID = getIntent().getIntExtra(MainActivity.EXTRA_USER_ID, -1);

        todaySummaryText = findViewById(R.id.todaySummaryText);
        Button weightTrackerButton = findViewById(R.id.weightTrackerButton);
        Button calorieTrackerButton = findViewById(R.id.calorieTrackerButton);
        Button trendsButton = findViewById(R.id.trendsButton);

        // Open the weight tracker for the logged-in user
        weightTrackerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, WeightActivity.class);
            intent.putExtra(MainActivity.EXTRA_USER_ID, userID);
            startActivity(intent);
        });

        // Open the calorie tracker for the logged-in user
        calorieTrackerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CalorieActivity.class);
            intent.putExtra(MainActivity.EXTRA_USER_ID, userID);
            startActivity(intent);
        });

        // Open the trends page for the logged-in user
        trendsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrendsActivity.class);
            intent.putExtra(MainActivity.EXTRA_USER_ID, userID);
            startActivity(intent);
        });

        // Log out: return to the login screen and clear every screen behind it,
        // so pressing Back can't return to the logged-in user's data
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // onResume runs every time this screen comes back into view, including after the
    // user presses Back from another screen, so the summary stays up to date.
    @Override
    protected void onResume() {
        super.onResume();
        updateSummary();
    }

    // Show the user's current weight, goal weight and today's calories
    private void updateSummary() {
        String today = InputValidator.formatDate(LocalDate.now());
        int calories = database.getTotalCalories(userID, today);
        double currentWeight = database.getCurrentWeight(userID);
        double goalWeight = database.getGoalWeight(userID);

        // Show "None" instead of "0.0 lbs" when nothing has been saved yet
        String currentText = currentWeight > 0 ? currentWeight + " lbs" : "None";
        String goalText = goalWeight > 0 ? goalWeight + " lbs" : "None";

        todaySummaryText.setText("Current weight: " + currentText
                + "\nGoal weight: " + goalText
                + "\nToday: " + calories + " calories");
    }
}
