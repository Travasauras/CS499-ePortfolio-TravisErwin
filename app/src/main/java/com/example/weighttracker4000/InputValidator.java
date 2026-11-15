package com.example.weighttracker4000;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

// Validates user input before it is saved. Each method returns a ValidationResult
// holding either the parsed value or an error message to show the user.
public final class InputValidator {

    private static final int MIN_WEIGHT = 50;
    private static final int MAX_WEIGHT = 1000;
    private static final int MAX_CALORIES = 10000;

    // STRICT rejects impossible dates such as 02/30/2026
    private static final DateTimeFormatter INPUT_DATE =
            DateTimeFormatter.ofPattern("M/d/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter SAVED_DATE = DateTimeFormatter.ofPattern("MM/dd/uuuu");

    private InputValidator() {
    }

    public static ValidationResult<Double> validateWeight(String input) {
        return checkWeight(input, "weight");
    }

    public static ValidationResult<Double> validateGoalWeight(String input) {
        return checkWeight(input, "goal weight");
    }

    private static ValidationResult<Double> checkWeight(String input, String field) {
        if (isBlank(input)) {
            return ValidationResult.invalid("Please enter a " + field + ".");
        }
        try {
            double weight = Double.parseDouble(input.trim());
            // Written this way so NaN and Infinity also fail the range check
            if (!(weight >= MIN_WEIGHT && weight <= MAX_WEIGHT)) {
                return ValidationResult.invalid("The " + field + " must be between "
                        + MIN_WEIGHT + " and " + MAX_WEIGHT + " lbs.");
            }
            return ValidationResult.valid(weight);
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("The " + field + " must be a number.");
        }
    }

    public static ValidationResult<LocalDate> validateDate(String input) {
        return validateDate(input, LocalDate.now());
    }

    // Takes "today" as a parameter so unit tests can use a fixed date
    public static ValidationResult<LocalDate> validateDate(String input, LocalDate today) {
        if (isBlank(input)) {
            return ValidationResult.invalid("Please enter a date.");
        }
        try {
            LocalDate date = LocalDate.parse(input.trim(), INPUT_DATE);
            if (date.isAfter(today)) {
                return ValidationResult.invalid("The date cannot be in the future.");
            }
            return ValidationResult.valid(date);
        } catch (DateTimeParseException e) {
            return ValidationResult.invalid("Enter a valid date as MM/DD/YYYY.");
        }
    }

    // Saves every date in the same format, e.g. 11/5/2026 -> 11/05/2026
    public static String formatDate(LocalDate date) {
        return date.format(SAVED_DATE);
    }

    public static ValidationResult<Integer> validateCalories(String input) {
        if (isBlank(input)) {
            return ValidationResult.invalid("Please enter calories.");
        }
        try {
            int calories = Integer.parseInt(input.trim());
            if (calories < 1 || calories > MAX_CALORIES) {
                return ValidationResult.invalid("Calories must be between 1 and " + MAX_CALORIES + ".");
            }
            return ValidationResult.valid(calories);
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Calories must be a whole number.");
        }
    }

    private static boolean isBlank(String input) {
        return input == null || input.trim().isEmpty();
    }

    public static final class ValidationResult<T> {
        private final T value;
        private final String errorMessage;

        private ValidationResult(T value, String errorMessage) {
            this.value = value;
            this.errorMessage = errorMessage;
        }

        static <T> ValidationResult<T> valid(T value) {
            return new ValidationResult<>(value, null);
        }

        static <T> ValidationResult<T> invalid(String errorMessage) {
            return new ValidationResult<>(null, errorMessage);
        }

        public boolean isValid() {
            return errorMessage == null;
        }

        public T getValue() {
            if (!isValid()) {
                throw new IllegalStateException(errorMessage);
            }
            return value;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
