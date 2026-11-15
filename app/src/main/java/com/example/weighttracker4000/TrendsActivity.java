package com.example.weighttracker4000;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;

// Trends screen: the user picks a time interval (week, month or year) to view a graph of their weight.
// The interval buttons and graph data are not connected yet; that logic is added in the
// Algorithms and Data Structures enhancement (retrieve -> sort -> filter -> calculate -> graph).
public class TrendsActivity extends AppCompatActivity {

    private int userID;
    private WeightDatabase database;
    private LineChart trendsChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trends_screen);

        // Initialize database and get user ID from intent (used when the graph is filled in)
        database = new WeightDatabase(this);
        userID = getIntent().getIntExtra(MainActivity.EXTRA_USER_ID, -1);

        // Connect UI elements to the trends screen layout
        trendsChart = findViewById(R.id.trendsChart);
        trendsChart.setNoDataText("Choose Week, Month or Year to see your trend.");

        // Close this screen and return to the main menu underneath it
        Button mainMenuButton = findViewById(R.id.mainMenuButton);
        mainMenuButton.setOnClickListener(v -> finish());
    }
}
