package com.example.weighttracker4000;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Analyzes a list of weight records to determine the trend over time.
// Plain Java with no Android code, so it can be unit tested with JUnit.
public class WeightTrendAnalyzer {

    // Time intervals the user can choose on the Trends screen
    public enum Interval { WEEK, MONTH, YEAR }

    // Dates are stored in the database as MM/dd/yyyy text
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    // Parses a date string in the format "MM/dd/yyyy" into a LocalDate object.
    // Package-private so TrendsActivity can label the graph's dates.
    static LocalDate parseDate(String date) {
        return LocalDate.parse(date, DATE_FORMAT);
    }

    // Returns the number of days from the first record to this entry.
    // Used as the x-value by both the regression and the graph, so the trend line lines up with the points.
    public static long daysSinceFirst(WeightEntry first, WeightEntry entry) {
        return ChronoUnit.DAYS.between(parseDate(first.getDate()), parseDate(entry.getDate()));
    }

    // Returns true if the date is in MM/dd/yyyy format and is a real calendar date
    public static boolean isValidDate(String date) {
        try {
            parseDate(date);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // Returns a new list without records whose date is not in MM/dd/yyyy format, such as entries
    // saved before date validation was added. Skipping them keeps one bad record from crashing the analysis.
    public static List<WeightEntry> removeInvalidDates(List<WeightEntry> records) {
        List<WeightEntry> valid = new ArrayList<>();
        for (WeightEntry entry : records) {
            if (isValidDate(entry.getDate())) {
                valid.add(entry);
            }
        }
        return valid;
    }

    // Returns a new list of the records sorted by date from oldest to newest.
    // The original list is not changed. Every date must be valid (see removeInvalidDates).
    public static List<WeightEntry> sortByDate(List<WeightEntry> records) {
        List<WeightEntry> sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparing(entry -> parseDate(entry.getDate())));
        return sorted;
    }

    // Steps a date back by the given number of weeks, months or years
    private static LocalDate minusIntervals(LocalDate date, Interval interval, long count) {
        switch (interval) {
            case WEEK:
                return date.minusWeeks(count);
            case MONTH:
                return date.minusMonths(count);
            case YEAR:
                return date.minusYears(count);
            default:
                throw new IllegalArgumentException("Unknown interval: " + interval);
        }
    }

    // A period is one week, month or year ending on its end date. periodsBack = 0 is the period
    // ending today, 1 is the period before that, and so on. The start date is NOT part of the
    // period (it is the last day of the previous one), so neighboring periods never overlap.
    public static LocalDate getPeriodStart(Interval interval, int periodsBack, LocalDate today) {
        return minusIntervals(today, interval, periodsBack + 1);
    }

    // Last day included in the period
    public static LocalDate getPeriodEnd(Interval interval, int periodsBack, LocalDate today) {
        return minusIntervals(today, interval, periodsBack);
    }

    // Returns only the records in the selected period: after its start date, up to and including its end date.
    // Today is passed in (instead of using LocalDate.now()) so unit tests get repeatable results.
    public static List<WeightEntry> filterByInterval(List<WeightEntry> sorted, Interval interval,
                                                     int periodsBack, LocalDate today) {
        LocalDate start = getPeriodStart(interval, periodsBack, today);
        LocalDate end = getPeriodEnd(interval, periodsBack, today);
        List<WeightEntry> filtered = new ArrayList<>();

        // Keep each record dated inside the period
        for (WeightEntry entry : sorted) {
            LocalDate entryDate = parseDate(entry.getDate());
            if (entryDate.isAfter(start) && !entryDate.isAfter(end)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    // Returns the average weight of the records.
    // Returns 0.0 for an empty list; TrendsActivity shows "No weight data available" before calling this.
    public static double calculateAverageWeight(List<WeightEntry> records) {
        if (records.isEmpty()) {
            return 0.0;
        }
        double totalWeight = 0.0;
        for (WeightEntry entry : records) {
            totalWeight += entry.getWeight();
        }
        return totalWeight / records.size();
    }

    // Returns the change in weight from the first record to the last (negative means weight lost).
    // The list must be sorted oldest to newest, so the first and last records are found in O(1).
    public static double calculateWeightChange(List<WeightEntry> sorted) {
        if (sorted.size() < 2) {
            return 0.0;
        }
        double firstWeight = sorted.get(0).getWeight();
        double lastWeight = sorted.get(sorted.size() - 1).getWeight();
        return lastWeight - firstWeight;
    }

    // Groups the records into 7-day blocks counted from the first record and returns one average per block.
    // Used to keep the Year graph readable (at most 53 points instead of up to 365).
    // Because the list is sorted, each week's records sit next to each other, so this is one O(n) pass
    // with no map needed. The list must be sorted oldest to newest.
    public static List<WeeklyAverage> averageByWeek(List<WeightEntry> sorted) {
        List<WeeklyAverage> weeklyAverages = new ArrayList<>();
        if (sorted.isEmpty()) {
            return weeklyAverages;
        }

        WeightEntry first = sorted.get(0);
        LocalDate firstDate = parseDate(first.getDate());
        long currentWeek = 0;
        double weightSum = 0.0;
        long daySum = 0;
        int count = 0;

        for (WeightEntry entry : sorted) {
            long day = daysSinceFirst(first, entry);
            long week = day / 7;

            // Reached a new week: save the finished week's average and start a new running total
            if (week != currentWeek && count > 0) {
                weeklyAverages.add(new WeeklyAverage(firstDate.plusWeeks(currentWeek),
                        (double) daySum / count, weightSum / count, count));
                weightSum = 0.0;
                daySum = 0;
                count = 0;
            }
            currentWeek = week;
            weightSum += entry.getWeight();
            daySum += day;
            count++;
        }

        // Save the last week, which the loop never reaches a "next week" for
        weeklyAverages.add(new WeeklyAverage(firstDate.plusWeeks(currentWeek),
                (double) daySum / count, weightSum / count, count));
        return weeklyAverages;
    }

    // One week's average weight, as calculated by averageByWeek
    public static class WeeklyAverage {

        private final LocalDate weekStart;     // first day of the 7-day block
        private final double averageDay;       // average days-since-first-record of the entries (graph x-value)
        private final double averageWeight;
        private final int entryCount;          // how many records were averaged

        public WeeklyAverage(LocalDate weekStart, double averageDay, double averageWeight, int entryCount) {
            this.weekStart = weekStart;
            this.averageDay = averageDay;
            this.averageWeight = averageWeight;
            this.entryCount = entryCount;
        }

        public LocalDate getWeekStart() {
            return weekStart;
        }

        public double getAverageDay() {
            return averageDay;
        }

        public double getAverageWeight() {
            return averageWeight;
        }

        public int getEntryCount() {
            return entryCount;
        }
    }

    // Convenience method for TrendsActivity: removes unreadable dates, then sorts oldest to newest.
    // Done once when the screen opens so paging between periods only has to filter.
    public static List<WeightEntry> prepareRecords(List<WeightEntry> records) {
        return sortByDate(removeInvalidDates(records));
    }

    // Calculates the line of best fit through the records using least-squares linear regression.
    // x = days since the first record, y = weight. The list must be sorted oldest to newest.
    // Returns null when no trend can be calculated (fewer than 2 records, or all on the same day).
    public static TrendLine calculateTrend(List<WeightEntry> sorted) {
        if (sorted.size() < 2) {
            return null;
        }

        WeightEntry first = sorted.get(0);
        int n = sorted.size();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;

        // One pass through the records to build the sums the formulas need: O(n)
        for (WeightEntry entry : sorted) {
            double x = daysSinceFirst(first, entry);
            double y = entry.getWeight();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        // If every record is on the same day, the denominator is 0 and there is no trend
        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return null;
        }

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        return new TrendLine(slope, intercept);
    }

    // Line of best fit through the weight records: weight = slope * day + intercept.
    // Day 0 is the date of the first record used to calculate the line.
    public static class TrendLine {

        private final double slope;      // pounds gained (+) or lost (-) per day
        private final double intercept;  // estimated weight on day 0

        public TrendLine(double slope, double intercept) {
            this.slope = slope;
            this.intercept = intercept;
        }

        public double getSlope() {
            return slope;
        }

        public double getIntercept() {
            return intercept;
        }

        // Returns the projected weight on the given day (days since the first record)
        public double predict(double day) {
            return slope * day + intercept;
        }
    }
}
