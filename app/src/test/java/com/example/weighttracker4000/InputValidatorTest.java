package com.example.weighttracker4000;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.weighttracker4000.InputValidator.ValidationResult;

import org.junit.Test;

import java.time.LocalDate;

public class InputValidatorTest {

    // Fixed "today" so date tests don't change based on when they run
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    // ---- Weight ----

    @Test
    public void weight_validNumber_isAccepted() {
        ValidationResult<Double> result = InputValidator.validateWeight(" 185.5 ");
        assertTrue(result.isValid());
        assertEquals(185.5, result.getValue(), 0.0001);
    }

    @Test
    public void weight_blankOrNull_isRejected() {
        assertFalse(InputValidator.validateWeight("").isValid());
        assertFalse(InputValidator.validateWeight("   ").isValid());
        assertFalse(InputValidator.validateWeight(null).isValid());
    }

    @Test
    public void weight_notANumber_isRejected() {
        // These used to crash the app at Double.parseDouble
        assertFalse(InputValidator.validateWeight("abc").isValid());
        assertFalse(InputValidator.validateWeight("12.3.4").isValid());
        assertFalse(InputValidator.validateWeight("NaN").isValid());
        assertFalse(InputValidator.validateWeight("Infinity").isValid());
    }

    @Test
    public void weight_outOfRange_isRejected() {
        assertFalse(InputValidator.validateWeight("0").isValid());
        assertFalse(InputValidator.validateWeight("-150").isValid());
        assertFalse(InputValidator.validateWeight("49.9").isValid());
        assertFalse(InputValidator.validateWeight("1000.1").isValid());
    }

    @Test
    public void weight_rangeBoundaries_areAccepted() {
        assertTrue(InputValidator.validateWeight("50").isValid());
        assertTrue(InputValidator.validateWeight("1000").isValid());
    }

    @Test
    public void goalWeight_errorMessage_namesTheField() {
        assertEquals("Please enter a goal weight.",
                InputValidator.validateGoalWeight("").getErrorMessage());
    }

    // ---- Date ----

    @Test
    public void date_validFormats_areAccepted() {
        ValidationResult<LocalDate> result = InputValidator.validateDate("11/05/2026", TODAY);
        assertTrue(result.isValid());
        assertEquals(LocalDate.of(2026, 11, 5), result.getValue());

        assertTrue(InputValidator.validateDate("11/5/2026", TODAY).isValid());
    }

    @Test
    public void date_today_isAccepted() {
        assertTrue(InputValidator.validateDate("11/14/2026", TODAY).isValid());
    }

    @Test
    public void date_future_isRejected() {
        assertFalse(InputValidator.validateDate("11/15/2026", TODAY).isValid());
    }

    @Test
    public void date_impossibleDate_isRejected() {
        assertFalse(InputValidator.validateDate("02/30/2026", TODAY).isValid());
        assertFalse(InputValidator.validateDate("13/01/2026", TODAY).isValid());
        assertFalse(InputValidator.validateDate("02/29/2025", TODAY).isValid());
    }

    @Test
    public void date_leapDay_isAccepted() {
        assertTrue(InputValidator.validateDate("02/29/2024", TODAY).isValid());
    }

    @Test
    public void date_wrongFormat_isRejected() {
        assertFalse(InputValidator.validateDate("2026-11-05", TODAY).isValid());
        assertFalse(InputValidator.validateDate("Nov 5", TODAY).isValid());
        assertFalse(InputValidator.validateDate("", TODAY).isValid());
    }

    @Test
    public void formatDate_padsMonthAndDay() {
        LocalDate date = InputValidator.validateDate("1/5/2026", TODAY).getValue();
        assertEquals("01/05/2026", InputValidator.formatDate(date));
    }

    // ---- Calories ----

    @Test
    public void calories_validWholeNumber_isAccepted() {
        ValidationResult<Integer> result = InputValidator.validateCalories("2200");
        assertTrue(result.isValid());
        assertEquals(Integer.valueOf(2200), result.getValue());
    }

    @Test
    public void calories_invalid_isRejected() {
        assertFalse(InputValidator.validateCalories("").isValid());
        assertFalse(InputValidator.validateCalories("12.5").isValid());
        assertFalse(InputValidator.validateCalories("abc").isValid());
        assertFalse(InputValidator.validateCalories("0").isValid());
        assertFalse(InputValidator.validateCalories("10001").isValid());
    }

    // ---- ValidationResult ----

    @Test(expected = IllegalStateException.class)
    public void invalidResult_getValue_throws() {
        InputValidator.validateWeight("abc").getValue();
    }
}
