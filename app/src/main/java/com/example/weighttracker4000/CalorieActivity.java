package com.example.weighttracker4000;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalorieActivity extends AppCompatActivity {

    // Dates in the weekly summary rows, e.g. "Nov 15 – Nov 21, 2026"
    private static final DateTimeFormatter WEEK_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter WEEK_DATE_FORMAT_WITH_YEAR = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    private int userID;
    private WeightDatabase database;

    private EditText dateInput;
    private EditText calorieInput;
    private Button addCalorieButton;
    private TextView todayTotalText;
    private LinearLayout calorieGridContainer;
    private int selectedCalorieID = -1;

    // The calorie log's data, loaded by loadCalories() and drawn by showCalorieLog()
    private List<WeekGrouper.Week<CalorieEntry>> weeks = new ArrayList<>();
    private List<CalorieEntry> unreadableEntries = new ArrayList<>();

    // The one week whose entries are shown (identified by its starting Sunday), or null if all are closed
    private LocalDate expandedWeekStart;

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

        // Open the week the saved entry belongs to, so the user can see it in the log
        expandedWeekStart = WeekGrouper.getWeekStart(dateResult.getValue());

        // Clear the calories field and refresh the list
        calorieInput.setText("");
        loadCalories();
    }

    // Load the user's calorie entries and today's total, group the entries into calendar weeks and show the log
    private void loadCalories() {
        String today = InputValidator.formatDate(LocalDate.now());
        todayTotalText.setText("Today: " + database.getTotalCalories(userID, today) + " calories");

        List<CalorieEntry> records = database.getCalories(userID);

        // Sort the readable entries and group them by week, newest week first
        List<CalorieEntry> sorted = WeekGrouper.sortByDate(records, CalorieEntry::getDate);
        weeks = WeekGrouper.groupByCalendarWeek(sorted, CalorieEntry::getDate);

        // Entries with unreadable dates can't be placed in a week, but are kept so they can be fixed or deleted
        unreadableEntries = WeekGrouper.findUnreadableDates(records, CalorieEntry::getDate);

        // The first time the log is shown, open the newest week
        if (expandedWeekStart == null && !weeks.isEmpty()) {
            expandedWeekStart = weeks.get(0).getWeekStart();
        }

        showCalorieLog();
    }

    // Draw the calorie log: one summary row per week, plus the entry rows of the one open week.
    // Opening or closing a week only redraws; it does not query the database again.
    private void showCalorieLog() {
        calorieGridContainer.removeAllViews();

        for (WeekGrouper.Week<CalorieEntry> week : weeks) {
            boolean expanded = week.getWeekStart().equals(expandedWeekStart);
            calorieGridContainer.addView(createWeekHeader(week, expanded));

            if (expanded) {
                for (CalorieEntry entry : week.getEntries()) {
                    calorieGridContainer.addView(createEntryRow(entry));
                }
            }
        }

        // Unreadable dates are listed last under their own heading
        if (!unreadableEntries.isEmpty()) {
            calorieGridContainer.addView(createHeaderView(
                    "Unreadable dates (" + unreadableEntries.size() + ")\nTap an entry to fix its date", false));
            for (CalorieEntry entry : unreadableEntries) {
                calorieGridContainer.addView(createEntryRow(entry));
            }
        }

        if (weeks.isEmpty() && unreadableEntries.isEmpty()) {
            calorieGridContainer.addView(createHeaderView("No calorie entries yet", false));
        }
    }

    // Summary row for one week, e.g. "▼  Nov 15 – Nov 21, 2026" / "18 entries · 12,345 cal · Avg 2,057/day".
    // Tapping it opens that week (closing the one that was open) or closes it if it is already open.
    private TextView createWeekHeader(WeekGrouper.Week<CalorieEntry> week, boolean expanded) {
        List<CalorieEntry> entries = week.getEntries();

        // Total for the week, and the number of different days that have entries.
        // Entries are sorted by date, so a new day starts wherever the date changes: one O(n) pass.
        int totalCalories = 0;
        int daysLogged = 0;
        String previousDate = null;
        for (CalorieEntry entry : entries) {
            totalCalories += entry.getCalories();
            if (!entry.getDate().equals(previousDate)) {
                daysLogged++;
                previousDate = entry.getDate();
            }
        }

        String text = String.format(Locale.US, "%s  %s – %s%n%d %s · %,d cal · Avg %,d/day",
                expanded ? "▼" : "▶",
                week.getWeekStart().format(WEEK_DATE_FORMAT),
                week.getWeekEnd().format(WEEK_DATE_FORMAT_WITH_YEAR),
                entries.size(), entries.size() == 1 ? "entry" : "entries",
                totalCalories, Math.round((double) totalCalories / daysLogged));

        TextView header = createHeaderView(text, expanded);
        header.setOnClickListener(v -> {
            expandedWeekStart = expanded ? null : week.getWeekStart();
            showCalorieLog();
        });
        return header;
    }

    // Styled heading row used for week summaries and the unreadable-dates section.
    // The open week is highlighted green so it stands out from the closed ones.
    private TextView createHeaderView(String text, boolean highlighted) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(15);
        header.setPadding(24, 18, 24, 18);
        header.setBackgroundColor(Color.parseColor(highlighted ? "#C8E6C9" : "#EEEEEE"));

        // Thin gap between rows so each week reads as its own block
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 2;
        header.setLayoutParams(params);
        return header;
    }

    // One row of the log for a single calorie entry: date, calories and a delete button.
    // Tapping the row loads the entry into the inputs so it can be edited.
    private LinearLayout createEntryRow(CalorieEntry entry) {
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

        return row;
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
