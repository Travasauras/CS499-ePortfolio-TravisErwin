package com.example.weighttracker4000;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;

public class CalorieActivity extends AppCompatActivity {

    private int userID;
    private WeightDatabase database;

    private EditText dateInput;
    private EditText calorieInput;
    private Button addCalorieButton;
    private TextView todayTotalText;
    private LinearLayout calorieGridContainer;
    private int selectedCalorieID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calorie_screen);

        // Initialize database and get user ID from intent
        database = new WeightDatabase(this);
        userID = getIntent().getIntExtra(MainActivity.EXTRA_USER_ID, -1);

        // Connect UI elements to the calorie screen layout
        dateInput = findViewById(R.id.calorieDateInput);
        calorieInput = findViewById(R.id.calorieInput);
        addCalorieButton = findViewById(R.id.addCalorieButton);
        todayTotalText = findViewById(R.id.todayTotalText);
        calorieGridContainer = findViewById(R.id.calorieGridContainer);

        // Tapping the date field opens a calendar. Start with today's date filled in,
        // so the user usually only has to enter the calories.
        DatePickerHelper.attach(dateInput);
        dateInput.setText(InputValidator.formatDate(LocalDate.now()));

        addCalorieButton.setOnClickListener(v -> saveCalorieEntry());

        // Close this screen and return to the main menu underneath it
        Button mainMenuButton = findViewById(R.id.mainMenuButton);
        mainMenuButton.setOnClickListener(v -> finish());

        loadCalories();
    }

    // Validate the inputs, then add a new calorie entry or update the selected one
    private void saveCalorieEntry() {
        InputValidator.ValidationResult<LocalDate> dateResult =
                InputValidator.validateDate(dateInput.getText().toString());
        InputValidator.ValidationResult<Integer> calorieResult =
                InputValidator.validateCalories(calorieInput.getText().toString());

        // If either input is invalid, show the error on that field and stop
        if (!dateResult.isValid()) {
            showFieldError(dateInput, dateResult.getErrorMessage());
            return;
        }
        if (!calorieResult.isValid()) {
            showFieldError(calorieInput, calorieResult.getErrorMessage());
            return;
        }

        String date = InputValidator.formatDate(dateResult.getValue());
        int calories = calorieResult.getValue();

        if (selectedCalorieID == -1) {
            // Add a new entry
            if (database.addCalories(userID, date, calories) != -1) {
                Toast.makeText(this, "Calories added", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Update the selected entry, then return the button to add mode
            if (database.updateCalories(selectedCalorieID, date, calories) > 0) {
                Toast.makeText(this, "Calorie entry updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Entry no longer exists", Toast.LENGTH_SHORT).show();
            }
            selectedCalorieID = -1;
            addCalorieButton.setText("Add Calories");
        }

        // Clear the calories field and refresh the list
        calorieInput.setText("");
        loadCalories();
    }

    // Load the user's calorie entries and today's total, and display them
    private void loadCalories() {
        calorieGridContainer.removeAllViews();

        String today = InputValidator.formatDate(LocalDate.now());
        todayTotalText.setText("Today: " + database.getTotalCalories(userID, today) + " calories");

        // Create a row in the grid for each calorie entry
        for (CalorieEntry entry : database.getCalories(userID)) {
            int calorieID = entry.getId();
            String date = entry.getDate();
            int calories = entry.getCalories();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            // Button to delete the entry
            Button deleteButton = new Button(this);
            deleteButton.setText("X");
            deleteButton.setOnClickListener(v -> {
                database.deleteCalories(calorieID);

                // If the deleted entry was being edited, leave edit mode
                if (selectedCalorieID == calorieID) {
                    selectedCalorieID = -1;
                    addCalorieButton.setText("Add Calories");
                }

                Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
                loadCalories();
            });

            row.addView(makeCell(date));
            row.addView(makeCell(String.valueOf(calories)));
            row.addView(deleteButton);

            // Tap a row to load it into the inputs for editing
            row.setOnClickListener(v -> {
                selectedCalorieID = calorieID;
                dateInput.setText(date);
                calorieInput.setText(String.valueOf(calories));
                addCalorieButton.setText("Update Entry");
                Toast.makeText(this, "You can now update this entry.", Toast.LENGTH_SHORT).show();
            });

            calorieGridContainer.addView(row);
        }
    }

    // Show a validation error on an input field and move the cursor there so the user can fix it
    private void showFieldError(EditText field, String message) {
        field.setError(message);
        field.requestFocus();
    }

    // Create and format a TextView cell for the calorie grid
    private TextView makeCell(String text) {
        TextView cell = new TextView(this);
        cell.setText(text);
        cell.setGravity(Gravity.CENTER);
        cell.setTextSize(16);
        cell.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return cell;
    }
}
