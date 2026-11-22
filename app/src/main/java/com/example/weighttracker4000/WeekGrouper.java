package com.example.weighttracker4000;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

// Sorts dated records and groups them into calendar weeks (Sunday to Saturday) for the weight and
// calorie logs. It is generic (<T>), so the same code works for WeightEntry and CalorieEntry:
// each screen passes a function that reads the date text from its own entry type.
// Plain Java with no Android code, so it can be unit tested with JUnit.
public final class WeekGrouper {

    private WeekGrouper() {
        // Only static methods; no objects are created
    }

    // Returns a new list of the records with readable dates, sorted oldest to newest.
    // Records with unreadable dates (saved before date validation) are left out.
    public static <T> List<T> sortByDate(List<T> records, Function<T, String> dateOf) {
        List<T> sorted = new ArrayList<>();
        for (T record : records) {
            if (WeightTrendAnalyzer.isValidDate(dateOf.apply(record))) {
                sorted.add(record);
            }
        }
        sorted.sort(Comparator.comparing(record -> WeightTrendAnalyzer.parseDate(dateOf.apply(record))));
        return sorted;
    }

    // Returns the records whose date can't be read, so a screen can still show them to be fixed or deleted
    public static <T> List<T> findUnreadableDates(List<T> records, Function<T, String> dateOf) {
        List<T> unreadable = new ArrayList<>();
        for (T record : records) {
            if (!WeightTrendAnalyzer.isValidDate(dateOf.apply(record))) {
                unreadable.add(record);
            }
        }
        return unreadable;
    }

    // Returns the Sunday that starts the calendar week containing this date
    public static LocalDate getWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    // Groups the records into calendar weeks, newest week first, with each week's records newest first.
    // Walking the sorted list from the end means the groups come out newest first already,
    // and each week's records are next to each other, so this is one O(n) pass with no map needed.
    // The list must be sorted oldest to newest (see sortByDate).
    public static <T> List<Week<T>> groupByCalendarWeek(List<T> sorted, Function<T, String> dateOf) {
        List<Week<T>> weeks = new ArrayList<>();
        Week<T> currentWeek = null;

        for (int i = sorted.size() - 1; i >= 0; i--) {
            T record = sorted.get(i);
            LocalDate weekStart = getWeekStart(WeightTrendAnalyzer.parseDate(dateOf.apply(record)));

            // Moved into an earlier week: start a new group
            if (currentWeek == null || !currentWeek.getWeekStart().equals(weekStart)) {
                currentWeek = new Week<>(weekStart);
                weeks.add(currentWeek);
            }
            currentWeek.entries.add(record);
        }
        return weeks;
    }

    // One calendar week of records, as built by groupByCalendarWeek
    public static class Week<T> {

        private final LocalDate weekStart;               // the Sunday that starts the week
        private final List<T> entries = new ArrayList<>(); // newest first

        public Week(LocalDate weekStart) {
            this.weekStart = weekStart;
        }

        public LocalDate getWeekStart() {
            return weekStart;
        }

        // The Saturday that ends the week
        public LocalDate getWeekEnd() {
            return weekStart.plusDays(6);
        }

        public List<T> getEntries() {
            return entries;
        }
    }
}
