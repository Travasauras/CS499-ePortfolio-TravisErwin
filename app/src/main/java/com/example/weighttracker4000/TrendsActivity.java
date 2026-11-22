package com.example.weighttracker4000;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Trends screen: the user picks a time interval (week, month or year) to view a graph of their weight.
// The analysis itself (sort -> filter -> calculate -> linear regression) is done by WeightTrendAnalyzer;
// this screen only gets the records, shows the summary text and draws the graph.
public class TrendsActivity extends AppCompatActivity {

    private static final String NO_DATA_MESSAGE = "No weight data available";

    // Graph styling: weight line matches the app's green buttons, trend line is a dashed orange
    private static final int WEIGHT_COLOR = Color.parseColor("#4CAF50");
    private static final int TREND_COLOR = Color.parseColor("#FF9800");

    // Dates along the bottom of the graph, e.g. 11/20/26
    private static final DateTimeFormatter AXIS_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d/yy");

    // How far the projected trend line extends past the last record, as a fraction of the time shown
    private static final float PROJECTION_FRACTION = 0.25f;

    // Date range shown between the arrow buttons, e.g. Nov 14, 2026 – Nov 20, 2026
    private static final DateTimeFormatter PERIOD_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    // Date in the pop-up shown when a point is tapped, e.g. Fri, Nov 20, 2026
    private static final DateTimeFormatter MARKER_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US);

    private LineChart trendsChart;
    private TextView summaryText;
    private View periodNavigation;
    private TextView periodLabel;
    private Button previousPeriodButton;
    private Button nextPeriodButton;

    // All of the user's valid records, sorted oldest to newest. Loaded once when the screen opens,
    // so moving between periods only filters this list instead of querying the database again.
    private List<WeightEntry> sortedRecords;

    // What is currently on screen: the chosen interval and how many periods back from today
    private WeightTrendAnalyzer.Interval currentInterval;
    private int periodsBack = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trends_screen);

        // Retrieve the user's weight records, drop unreadable dates and sort oldest to newest
        WeightDatabase database = new WeightDatabase(this);
        int userID = getIntent().getIntExtra(MainActivity.EXTRA_USER_ID, -1);
        sortedRecords = WeightTrendAnalyzer.prepareRecords(database.getWeights(userID));

        // Connect UI elements to the trends screen layout
        trendsChart = findViewById(R.id.trendsChart);
        trendsChart.setNoDataText("Choose Week, Month or Year to see your trend.");
        trendsChart.getDescription().setEnabled(false);
        trendsChart.getAxisRight().setEnabled(false);

        // Pop-up with a point's date and weight, shown when the user taps it
        ChartMarkerView marker = new ChartMarkerView(this);
        marker.setChartView(trendsChart);   // lets the pop-up stay inside the chart's edges
        trendsChart.setMarker(marker);

        summaryText = findViewById(R.id.trendSummaryText);
        periodNavigation = findViewById(R.id.periodNavigation);
        periodLabel = findViewById(R.id.periodLabel);
        previousPeriodButton = findViewById(R.id.previousPeriodButton);
        nextPeriodButton = findViewById(R.id.nextPeriodButton);

        // Each interval button starts at the current period (the one ending today)
        Button weekButton = findViewById(R.id.weekButton);
        Button monthButton = findViewById(R.id.monthButton);
        Button yearButton = findViewById(R.id.yearButton);
        weekButton.setOnClickListener(v -> selectInterval(WeightTrendAnalyzer.Interval.WEEK));
        monthButton.setOnClickListener(v -> selectInterval(WeightTrendAnalyzer.Interval.MONTH));
        yearButton.setOnClickListener(v -> selectInterval(WeightTrendAnalyzer.Interval.YEAR));

        // Arrow buttons step one period back in time or forward toward today
        previousPeriodButton.setOnClickListener(v -> {
            periodsBack++;
            showTrend();
        });
        nextPeriodButton.setOnClickListener(v -> {
            periodsBack--;
            showTrend();
        });

        // Close this screen and return to the main menu underneath it
        Button mainMenuButton = findViewById(R.id.mainMenuButton);
        mainMenuButton.setOnClickListener(v -> finish());
    }

    // Switches to a new interval, starting again from the current period
    private void selectInterval(WeightTrendAnalyzer.Interval interval) {
        currentInterval = interval;
        periodsBack = 0;
        periodNavigation.setVisibility(View.VISIBLE);
        showTrend();
    }

    // Runs the weight trend analysis for the selected period and updates the screen
    private void showTrend() {
        LocalDate today = LocalDate.now();
        updatePeriodNavigation(today);

        // Keep only the records in the selected period
        List<WeightEntry> filtered =
                WeightTrendAnalyzer.filterByInterval(sortedRecords, currentInterval, periodsBack, today);

        // No records in this period: clear the graph and tell the user
        if (filtered.isEmpty()) {
            trendsChart.setNoDataText(NO_DATA_MESSAGE);
            trendsChart.clear();
            summaryText.setText(NO_DATA_MESSAGE);
            return;
        }

        // Calculate the statistics; trend is null when there is not enough data for a regression
        double average = WeightTrendAnalyzer.calculateAverageWeight(filtered);
        double change = WeightTrendAnalyzer.calculateWeightChange(filtered);
        WeightTrendAnalyzer.TrendLine trend = WeightTrendAnalyzer.calculateTrend(filtered);

        summaryText.setText(buildSummary(filtered.size(), average, change, trend));

        // Only the current period projects into the future; past periods show the fitted line only.
        // The Year graph shows weekly averages so a full year of entries doesn't crowd the chart.
        boolean projectTrend = periodsBack == 0;
        boolean showWeeklyAverages = currentInterval == WeightTrendAnalyzer.Interval.YEAR;
        drawChart(filtered, trend, projectTrend, showWeeklyAverages);
    }

    // Shows the period's date range and turns the arrow buttons on or off
    private void updatePeriodNavigation(LocalDate today) {
        LocalDate start = WeightTrendAnalyzer.getPeriodStart(currentInterval, periodsBack, today);
        LocalDate end = WeightTrendAnalyzer.getPeriodEnd(currentInterval, periodsBack, today);

        // The start date belongs to the previous period, so the first day shown is the day after it
        periodLabel.setText(start.plusDays(1).format(PERIOD_DATE_FORMAT) + " – " + end.format(PERIOD_DATE_FORMAT));

        // Can go back while older records exist (records on the start date are in the previous period),
        // and forward until reaching the period that ends today
        boolean hasOlderRecords = !sortedRecords.isEmpty()
                && !WeightTrendAnalyzer.parseDate(sortedRecords.get(0).getDate()).isAfter(start);
        setButtonEnabled(previousPeriodButton, hasOlderRecords);
        setButtonEnabled(nextPeriodButton, periodsBack > 0);
    }

    // Enables or disables a button and fades it when disabled, so the user can see it can't be pressed
    private void setButtonEnabled(Button button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.3f);
    }

    // Builds the text shown under the graph
    private String buildSummary(int recordCount, double average, double change,
                                WeightTrendAnalyzer.TrendLine trend) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format(Locale.US, "Records: %d%n", recordCount));
        summary.append(String.format(Locale.US, "Average weight: %.1f lbs%n", average));
        summary.append(String.format(Locale.US, "Weight change: %+.1f lbs%n", change));

        if (trend != null) {
            // Slope is pounds per day; per week is easier to understand
            summary.append(String.format(Locale.US, "Trend: %+.1f lbs per week", trend.getSlope() * 7));
        } else {
            summary.append("Trend: not enough data (needs records on 2 or more days)");
        }
        return summary.toString();
    }

    // Draws the weights as a line graph, plus the trend line when one is available.
    // projectTrend extends the trend line past the last record (used for the current period only).
    // showWeeklyAverages plots one averaged point per week instead of every record; the trend line
    // is still calculated from every record, since averaging first would throw away data.
    private void drawChart(List<WeightEntry> sorted, WeightTrendAnalyzer.TrendLine trend,
                           boolean projectTrend, boolean showWeeklyAverages) {
        WeightEntry first = sorted.get(0);
        LocalDate firstDate = WeightTrendAnalyzer.parseDate(first.getDate());

        // Graph points: x = days since the first record (same x-values the regression used).
        // Each point also carries the text shown in the pop-up when the user taps it.
        List<Entry> weightPoints = new ArrayList<>();
        if (showWeeklyAverages) {
            for (WeightTrendAnalyzer.WeeklyAverage week : WeightTrendAnalyzer.averageByWeek(sorted)) {
                String details = String.format(Locale.US, "Week of %s%nAverage: %.1f lbs (%d %s)",
                        week.getWeekStart().format(PERIOD_DATE_FORMAT), week.getAverageWeight(),
                        week.getEntryCount(), week.getEntryCount() == 1 ? "entry" : "entries");
                weightPoints.add(new Entry((float) week.getAverageDay(), (float) week.getAverageWeight(), details));
            }
        } else {
            for (WeightEntry entry : sorted) {
                String details = String.format(Locale.US, "%s%n%.1f lbs",
                        WeightTrendAnalyzer.parseDate(entry.getDate()).format(MARKER_DATE_FORMAT), entry.getWeight());
                float day = WeightTrendAnalyzer.daysSinceFirst(first, entry);
                weightPoints.add(new Entry(day, (float) entry.getWeight(), details));
            }
        }

        LineDataSet weightSet = new LineDataSet(weightPoints, showWeeklyAverages ? "Weekly average" : "Weight");
        weightSet.setColor(WEIGHT_COLOR);
        weightSet.setCircleColor(WEIGHT_COLOR);
        weightSet.setLineWidth(2f);
        weightSet.setCircleRadius(3f);
        weightSet.setDrawValues(false);

        LineData lineData = new LineData(weightSet);

        // Trend: a straight dashed line from the first record to the last one,
        // extended a bit further when projecting the current period
        if (trend != null) {
            // Use the last actual record (not the last averaged point) so the line covers the whole period
            float lastDay = WeightTrendAnalyzer.daysSinceFirst(first, sorted.get(sorted.size() - 1));
            float endDay = projectTrend ? lastDay + Math.max(1f, lastDay * PROJECTION_FRACTION) : lastDay;

            List<Entry> trendPoints = new ArrayList<>();
            trendPoints.add(new Entry(0f, (float) trend.predict(0)));
            trendPoints.add(new Entry(endDay, (float) trend.predict(endDay)));

            LineDataSet trendSet = new LineDataSet(trendPoints, projectTrend ? "Projected trend" : "Trend");
            trendSet.setColor(TREND_COLOR);
            trendSet.setLineWidth(2f);
            trendSet.enableDashedLine(15f, 10f, 0f);
            trendSet.setDrawCircles(false);
            trendSet.setDrawValues(false);
            trendSet.setHighlightEnabled(false);   // tapping only shows pop-ups for weight points
            lineData.addDataSet(trendSet);
        }

        // Turn the x-values (days since the first record) back into dates for the axis labels
        XAxis xAxis = trendsChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);   // at most one label per day
        xAxis.setLabelCount(4);     // keep the date labels from overlapping
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return firstDate.plusDays(Math.round(value)).format(AXIS_DATE_FORMAT);
            }
        });

        trendsChart.highlightValues(null);   // close any pop-up left over from the previous period
        trendsChart.setData(lineData);
        trendsChart.invalidate();   // redraw the graph with the new data
    }
}
