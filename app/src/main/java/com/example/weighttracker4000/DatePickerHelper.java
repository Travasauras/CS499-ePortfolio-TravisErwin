package com.example.weighttracker4000;

import android.app.DatePickerDialog;
import android.widget.EditText;

import java.time.LocalDate;

// Turns a date EditText into a calendar picker, so users pick a date instead of typing one.
// Shared by the weight and calorie screens so the behavior is the same on both.
public final class DatePickerHelper {

    private DatePickerHelper() {
    }

    public static void attach(EditText dateField) {
        // Stop the keyboard from opening; tapping the field opens the calendar instead
        dateField.setFocusable(false);
        dateField.setOnClickListener(v -> showPicker(dateField));
    }

    private static void showPicker(EditText dateField) {
        // Start the calendar on the date already in the field, or today if it is empty or invalid
        InputValidator.ValidationResult<LocalDate> current =
                InputValidator.validateDate(dateField.getText().toString());
        LocalDate start = current.isValid() ? current.getValue() : LocalDate.now();

        // DatePickerDialog counts months from 0 (January = 0), LocalDate counts from 1
        DatePickerDialog dialog = new DatePickerDialog(
                dateField.getContext(),
                (picker, year, month, day) -> {
                    dateField.setText(InputValidator.formatDate(LocalDate.of(year, month + 1, day)));
                    dateField.setError(null);
                },
                start.getYear(), start.getMonthValue() - 1, start.getDayOfMonth());

        // Future dates cannot be picked, matching the validator's rule
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }
}
