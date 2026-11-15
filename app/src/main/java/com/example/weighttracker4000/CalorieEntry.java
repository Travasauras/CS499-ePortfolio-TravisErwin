package com.example.weighttracker4000;

// One calorie entry from the database. Screens use this object instead of reading
// database columns directly, which keeps database code out of the Activities.
public class CalorieEntry {

    private final int id;
    private final String date;
    private final int calories;

    public CalorieEntry(int id, String date, int calories) {
        this.id = id;
        this.date = date;
        this.calories = calories;
    }

    public int getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public int getCalories() {
        return calories;
    }
}
