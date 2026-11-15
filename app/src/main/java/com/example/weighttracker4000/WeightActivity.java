package com.example.weighttracker4000;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SmsManager;
import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;


public class WeightActivity extends AppCompatActivity {

    private int userID;
    private WeightDatabase database;

    private EditText goalWeightInput;
    private EditText dateInput;
    private EditText weightInput;
    private Button setGoalButton;
    private Button addWeightButton;
    private LinearLayout weightGridContainer;
    private int selectedWeightID = -1;

    // Initialize the activity and set up the UI components
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weight_grid_screen);

        // Initialize database and get user ID from intent
        database = new WeightDatabase(this);
        userID = getIntent().getIntExtra(MainActivity.EXTRA_USER_ID, -1);

        // Initialize UI components by finding their IDs
        goalWeightInput = findViewById(R.id.goalWeightInput);
        dateInput = findViewById(R.id.dateInput);
        weightInput = findViewById(R.id.weightInput);
        setGoalButton = findViewById(R.id.setGoalButton);
        addWeightButton = findViewById(R.id.addWeightButton);
        weightGridContainer = findViewById(R.id.weightGridContainer);

        // Tapping the date field opens a calendar. Start with today's date filled in.
        DatePickerHelper.attach(dateInput);
        dateInput.setText(InputValidator.formatDate(LocalDate.now()));

        // Load existing weight entries from the database and display them in the grid
        loadGoalWeight();
        loadWeights();

        // Set click listeners for the "Set Goal" and "Add Weight" buttons
        setGoalButton.setOnClickListener(v -> setGoalWeight());
        addWeightButton.setOnClickListener(v -> addWeightEntry());

        // Close this screen and return to the main menu underneath it
        Button mainMenuButton = findViewById(R.id.mainMenuButton);
        mainMenuButton.setOnClickListener(v -> finish());
    }


    // Load the user's goal weight from the database and display it in the input field
    private void loadGoalWeight() {
        double goalWeight = database.getGoalWeight(userID);

        // If a goal weight exists, set it in the input field
        if (goalWeight > 0) {
            goalWeightInput.setText(String.valueOf(goalWeight));
        }
    }


    // Validate the goal weight and save it to the database
    private void setGoalWeight() {
        InputValidator.ValidationResult<Double> goalResult =
                InputValidator.validateGoalWeight(goalWeightInput.getText().toString());

        // If the goal weight is invalid, show the error on the field and stop
        if (!goalResult.isValid()) {
            showFieldError(goalWeightInput, goalResult.getErrorMessage());
            return;
        }

        // Save the validated goal weight to the database
        double goalWeight = goalResult.getValue();
        database.setGoalWeight(userID, goalWeight);

        // Show a toast message confirming that the goal weight has been saved
        Toast.makeText(this, "Goal weight set.", Toast.LENGTH_SHORT).show();
        loadWeights();
    }

    // Validate the inputs, then add a new weight entry or update the selected one
    private void addWeightEntry() {
        // Validate the date and weight before anything is saved
        InputValidator.ValidationResult<LocalDate> dateResult =
                InputValidator.validateDate(dateInput.getText().toString());
        InputValidator.ValidationResult<Double> weightResult =
                InputValidator.validateWeight(weightInput.getText().toString());

        // If either input is invalid, show the error on that field and stop
        if (!dateResult.isValid()) {
            showFieldError(dateInput, dateResult.getErrorMessage());
            return;
        }
        if (!weightResult.isValid()) {
            showFieldError(weightInput, weightResult.getErrorMessage());
            return;
        }

        // Use the validated values. The date is saved in one consistent format (MM/dd/yyyy).
        String date = InputValidator.formatDate(dateResult.getValue());
        double weight = weightResult.getValue();

        if (selectedWeightID == -1) {
            // Add a new entry, then check if the new weight meets the goal weight
            if (database.addWeight(userID, date, weight) != -1) {
                Toast.makeText(this, "Weight entry added", Toast.LENGTH_SHORT).show();
                checkGoalAlert(weight);
            }
        } else {
            // Update the selected entry. updateWeight returns the number of rows changed,
            // so 0 means the entry no longer exists (for example, it was deleted).
            if (database.updateWeight(selectedWeightID, date, weight) > 0) {
                Toast.makeText(this, "Weight entry updated", Toast.LENGTH_SHORT).show();
                checkGoalAlert(weight);
            } else {
                Toast.makeText(this, "Entry no longer exists", Toast.LENGTH_SHORT).show();
            }
            exitEditMode();
        }

        // Reset the inputs and refresh the displayed weights
        dateInput.setText(InputValidator.formatDate(LocalDate.now()));
        weightInput.setText("");
        loadWeights();
    }

    // Return the button to "add" mode after an edit is saved or the edited entry is deleted
    private void exitEditMode() {
        selectedWeightID = -1;
        addWeightButton.setText("Add Weight Entry");
    }

    // Check if the user's current weight meets their goal weight and send an SMS alert if enabled
    private void checkGoalAlert(double currentWeight) {
        double goalWeight = database.getGoalWeight(userID);

        // Only continue if a goal weight has been saved and the user has reached the goal
        if (goalWeight > 0 && currentWeight <= goalWeight) {

            // Only send an SMS if the user enabled SMS notifications
            if (database.notificationsEnabled(userID)) {
                sendGoalSMS();
            }
        }
    }

    // Send an SMS message when the user reaches their goal weight
    private void sendGoalSMS() {
        String phoneNumber = "5554";
        String message = "Way to go!!! You have reached your goal weight! We knew you could do it!";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (Exception e) {
            Toast.makeText(this, "SMS Failed to Send", Toast.LENGTH_SHORT).show();
        }
    }


    // Load the user's weight entries from the database and display them in a grid layout
    private void loadWeights() {

        // Clear existing views in the weight grid container to avoid duplicates
        weightGridContainer.removeAllViews();

        double goalWeight = database.getGoalWeight(userID);

        // Create a row in the grid for each weight entry
        for (WeightEntry entry : database.getWeights(userID)) {

            // Create horizontal row to display weight entry data
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setMinimumHeight(60);

            // Create cells for the date, weight and goal weight
            TextView dateView = makeCell(entry.getDate());
            TextView weightView = makeCell(String.valueOf(entry.getWeight()));
            TextView goalView = makeCell(goalWeight > 0 ? String.valueOf(goalWeight) : "None");

            // Create a button to delete the weight entry
            Button deleteButton = new Button(this);
            deleteButton.setText("X");
            deleteButton.setOnClickListener(v -> {

                // Remove the weight entry from the database
                database.deleteWeight(entry.getId());

                // If the deleted entry was being edited, leave edit mode so "Update" can't
                // try to save to an entry that no longer exists
                if (selectedWeightID == entry.getId()) {
                    exitEditMode();
                    weightInput.setText("");
                }

                Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
                loadWeights();
            });

            // Add all views to the row
            row.addView(dateView);
            row.addView(weightView);
            row.addView(goalView);
            row.addView(deleteButton);

            // Allow the user to click on a row to edit the weight entry
            row.setOnClickListener(v -> {

                // Load the selected weight entry data into the input fields
                selectedWeightID = entry.getId();
                dateInput.setText(entry.getDate());
                weightInput.setText(String.valueOf(entry.getWeight()));

                // Change the button text to indicate that the user is in edit mode
                addWeightButton.setText("Update Entry");

                // Toast message to user indicating they are in edit mode
                Toast.makeText(this, "You can now update this entry.", Toast.LENGTH_SHORT).show();
            });

            // Add the row to the weight grid
            weightGridContainer.addView(row);
        }
    }

    // Show a validation error on an input field and move the cursor there so the user can fix it
    private void showFieldError(EditText field, String message) {
        field.setError(message);
        field.requestFocus();
    }

    // Create and format TextView cells for the weight grid display
    private TextView makeCell(String text) {
        TextView cell = new TextView(this);
        cell.setText(text);
        cell.setGravity(Gravity.CENTER);
        cell.setTextSize(16);

        // Set layout parameters for the cell to ensure it takes up equal space in the row
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                60,
                1
        );

        cell.setLayoutParams(params);
        return cell;
    }
}
