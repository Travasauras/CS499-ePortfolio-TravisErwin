package com.example.weighttracker4000;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Builds a demo account so the Trends graph and the weekly weight and calorie logs can be reviewed
// with a realistic amount of data: two years of daily weigh-ins and 3-4 meals a day.
// Only offered in debug builds (see MainActivity), so demo data never ships in a release.
//
// Dates are generated counting back from today, not stored in a file, so the Week, Month and Year
// views always have current data no matter when the app is run. Fixed random seeds make the
// numbers the same every time, so they match screenshots and the code review video.
public final class DemoDataSeeder {

    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "demo";

    private static final int DAYS_OF_DATA = 730;          // two years, so the previous Year can be paged to
    private static final double START_WEIGHT = 228.0;     // weight two years ago
    private static final double END_WEIGHT = 186.0;       // weight today
    private static final double DAILY_VARIATION = 1.4;    // +/- pounds of day-to-day water-weight swing
    private static final double GOAL_WEIGHT = 180.0;

    private static final long WEIGHT_RANDOM_SEED = 4000;
    private static final long CALORIE_RANDOM_SEED = 4001;

    private DemoDataSeeder() {
        // Only static methods; no objects are created
    }

    // Creates (or resets) the demo account and fills it with fresh data. Returns the demo user's ID.
    public static int seed(WeightDatabase database) {
        LocalDate today = LocalDate.now();
        return database.replaceDemoAccount(DEMO_USERNAME, DEMO_PASSWORD,
                generateWeights(today), generateCalories(today), GOAL_WEIGHT);
    }

    // One weight per day for the last two years, oldest first, trending down from START_WEIGHT to END_WEIGHT.
    // The curve drops a little faster at first and slows down later, like real weight loss.
    public static List<WeightEntry> generateWeights(LocalDate today) {
        Random random = new Random(WEIGHT_RANDOM_SEED);
        List<WeightEntry> weights = new ArrayList<>();

        for (int daysAgo = DAYS_OF_DATA - 1; daysAgo >= 0; daysAgo--) {
            double progress = (double) (DAYS_OF_DATA - 1 - daysAgo) / (DAYS_OF_DATA - 1);   // 0 to 1
            double trendWeight = START_WEIGHT + (END_WEIGHT - START_WEIGHT) * Math.pow(progress, 0.85);
            double noise = (random.nextDouble() * 2 - 1) * DAILY_VARIATION;
            double weight = Math.round((trendWeight + noise) * 10) / 10.0;                  // one decimal place

            // ID 0 because these are not database rows yet; the database assigns real IDs when they are saved
            weights.add(new WeightEntry(0, InputValidator.formatDate(today.minusDays(daysAgo)), weight));
        }
        return weights;
    }

    // Three or four meals a day for the last two years, oldest first, each 350 to 890 calories
    public static List<CalorieEntry> generateCalories(LocalDate today) {
        Random random = new Random(CALORIE_RANDOM_SEED);
        List<CalorieEntry> calories = new ArrayList<>();

        for (int daysAgo = DAYS_OF_DATA - 1; daysAgo >= 0; daysAgo--) {
            String date = InputValidator.formatDate(today.minusDays(daysAgo));
            int meals = random.nextInt(3) == 0 ? 4 : 3;   // about one day in three has a fourth meal

            for (int meal = 0; meal < meals; meal++) {
                int mealCalories = 350 + random.nextInt(55) * 10;   // 350 to 890, in steps of 10
                calories.add(new CalorieEntry(0, date, mealCalories));
            }
        }
        return calories;
    }
}
