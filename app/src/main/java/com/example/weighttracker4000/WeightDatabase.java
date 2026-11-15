package com.example.weighttracker4000;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class WeightDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weight_tracker.db";
    // Version 3 adds the calories table
    private static final int DATABASE_VERSION = 3;


// set up the database and create the tables
    public WeightDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final class UserTable {
        private static final String TABLE_NAME = "users";
        private static final String COLUMN_ID = "_id";
        private static final String COLUMN_USERNAME = "username";
        private static final String COLUMN_PASSWORD = "password";
    }

    private static final class WeightTable {
        private static final String TABLE_NAME = "weights";
        private static final String COLUMN_ID = "_id";
        private static final String COLUMN_USER_ID = "user_id";
        private static final String COLUMN_DATE = "date";
        private static final String COLUMN_WEIGHT = "weight";
    }

    private static final class GoalTable {
        private static final String TABLE_NAME = "goals";
        private static final String COLUMN_ID = "_id";
        private static final String COLUMN_USER_ID = "user_id";
        private static final String COLUMN_TARGET_WEIGHT = "target_weight";
    }

    private static final class NotificationTable {
        private static final String TABLE_NAME = "notifications";
        private static final String COLUMN_ID = "_id";
        private static final String COLUMN_USER_ID = "user_id";
        private static final String COLUMN_ENABLED = "enabled";
    }

    private static final class CalorieTable {
        private static final String TABLE_NAME = "calories";
        private static final String COLUMN_ID = "_id";
        private static final String COLUMN_USER_ID = "user_id";
        private static final String COLUMN_DATE = "date";
        private static final String COLUMN_CALORIES = "calories";
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create table for user login information
        db.execSQL("create table " + UserTable.TABLE_NAME + " (" +
                UserTable.COLUMN_ID + " integer primary key autoincrement, " +
                UserTable.COLUMN_USERNAME + " text not null unique, " +
                UserTable.COLUMN_PASSWORD + " text not null);");

        // Create table for daily weight entries
        db.execSQL("create table " + WeightTable.TABLE_NAME + " (" +
                WeightTable.COLUMN_ID + " integer primary key autoincrement, " +
                WeightTable.COLUMN_USER_ID + " integer not null, " +
                WeightTable.COLUMN_DATE + " text not null, " +
                WeightTable.COLUMN_WEIGHT + " real not null);");

        // Create table for each user's goal weight
        db.execSQL("create table " + GoalTable.TABLE_NAME + " (" +
                GoalTable.COLUMN_ID + " integer primary key autoincrement, " +
                GoalTable.COLUMN_USER_ID + " integer not null, " +
                GoalTable.COLUMN_TARGET_WEIGHT + " real not null);");

        // Create table for SMS notification settings
        db.execSQL("create table " + NotificationTable.TABLE_NAME + " (" +
                NotificationTable.COLUMN_ID + " integer primary key autoincrement, " +
                NotificationTable.COLUMN_USER_ID + " integer not null, " +
                NotificationTable.COLUMN_ENABLED + " integer not null);");

        createCalorieTable(db);
    }

    // Create table for calorie entries
    private void createCalorieTable(SQLiteDatabase db) {
        db.execSQL("create table " + CalorieTable.TABLE_NAME + " (" +
                CalorieTable.COLUMN_ID + " integer primary key autoincrement, " +
                CalorieTable.COLUMN_USER_ID + " integer not null, " +
                CalorieTable.COLUMN_DATE + " text not null, " +
                CalorieTable.COLUMN_CALORIES + " integer not null);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Only add what is new, so existing users, weights and goals are kept
        if (oldVersion < 3) {
            createCalorieTable(db);
        }
    }

    // Add a new user account
    public long addUser(String username, String password) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(UserTable.COLUMN_USERNAME, username);
        values.put(UserTable.COLUMN_PASSWORD, password);

        return db.insert(UserTable.TABLE_NAME, null, values);
    }

    // Check login credentials and return the matching user ID
    public int checkLogin(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " + UserTable.COLUMN_ID +
                        " FROM " + UserTable.TABLE_NAME +
                        " WHERE " + UserTable.COLUMN_USERNAME + " = ? AND " +
                        UserTable.COLUMN_PASSWORD + " = ?",
                new String[]{username, password}
        );

        int userID = -1;

        if (cursor.moveToFirst()) {
            userID = cursor.getInt(0);
        }

        cursor.close();
        return userID;
    }

    // Add a new weight entry for the current user
    public long addWeight(int userId, String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(WeightTable.COLUMN_USER_ID, userId);
        values.put(WeightTable.COLUMN_DATE, date);
        values.put(WeightTable.COLUMN_WEIGHT, weight);

        return db.insert(WeightTable.TABLE_NAME, null, values);
    }

    // Get all weight entries for the current user as a list of WeightEntry objects
    public List<WeightEntry> getWeights(int userID) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + WeightTable.TABLE_NAME +
                        " WHERE " + WeightTable.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userID)}
        );

        List<WeightEntry> entries = new ArrayList<>();

        while (cursor.moveToNext()) {
            entries.add(new WeightEntry(
                    cursor.getInt(cursor.getColumnIndexOrThrow(WeightTable.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(WeightTable.COLUMN_DATE)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(WeightTable.COLUMN_WEIGHT))
            ));
        }

        cursor.close();
        return entries;
    }

    // Get the user's most recent weight by date, or 0 if there are no entries.
    // Dates are stored as MM/dd/yyyy text, which doesn't sort correctly as text,
    // so each date is rearranged into yyyyMMdd before sorting.
    public double getCurrentWeight(int userId) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " + WeightTable.COLUMN_WEIGHT +
                        " FROM " + WeightTable.TABLE_NAME +
                        " WHERE " + WeightTable.COLUMN_USER_ID + " = ?" +
                        " ORDER BY substr(" + WeightTable.COLUMN_DATE + ", 7, 4) || " +
                        "substr(" + WeightTable.COLUMN_DATE + ", 1, 2) || " +
                        "substr(" + WeightTable.COLUMN_DATE + ", 4, 2) DESC, " +
                        WeightTable.COLUMN_ID + " DESC" +
                        " LIMIT 1",
                new String[]{String.valueOf(userId)}
        );

        double currentWeight = 0;

        if (cursor.moveToFirst()) {
            currentWeight = cursor.getDouble(0);
        }

        cursor.close();
        return currentWeight;
    }

    // Update an existing weight entry
    public int updateWeight(int weightID, String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(WeightTable.COLUMN_DATE, date);
        values.put(WeightTable.COLUMN_WEIGHT, weight);

        return db.update(
                WeightTable.TABLE_NAME,
                values,
                WeightTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(weightID)}
        );
    }

    // Delete a selected weight entry
    public int deleteWeight(int weightId) {
        SQLiteDatabase db = getWritableDatabase();

        return db.delete(
                WeightTable.TABLE_NAME,
                WeightTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(weightId)}
        );
    }

    // Add a new calorie entry for the current user
    public long addCalories(int userId, String date, int calories) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(CalorieTable.COLUMN_USER_ID, userId);
        values.put(CalorieTable.COLUMN_DATE, date);
        values.put(CalorieTable.COLUMN_CALORIES, calories);

        return db.insert(CalorieTable.TABLE_NAME, null, values);
    }

    // Get all calorie entries for the current user as a list of CalorieEntry objects
    public List<CalorieEntry> getCalories(int userId) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + CalorieTable.TABLE_NAME +
                        " WHERE " + CalorieTable.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}
        );

        List<CalorieEntry> entries = new ArrayList<>();

        while (cursor.moveToNext()) {
            entries.add(new CalorieEntry(
                    cursor.getInt(cursor.getColumnIndexOrThrow(CalorieTable.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(CalorieTable.COLUMN_DATE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(CalorieTable.COLUMN_CALORIES))
            ));
        }

        cursor.close();
        return entries;
    }

    // Get the total calories the user logged on one date
    public int getTotalCalories(int userId, String date) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + CalorieTable.COLUMN_CALORIES + ")" +
                        " FROM " + CalorieTable.TABLE_NAME +
                        " WHERE " + CalorieTable.COLUMN_USER_ID + " = ? AND " +
                        CalorieTable.COLUMN_DATE + " = ?",
                new String[]{String.valueOf(userId), date}
        );

        int total = 0;

        if (cursor.moveToFirst()) {
            total = cursor.getInt(0);
        }

        cursor.close();
        return total;
    }

    // Update an existing calorie entry
    public int updateCalories(int calorieId, String date, int calories) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(CalorieTable.COLUMN_DATE, date);
        values.put(CalorieTable.COLUMN_CALORIES, calories);

        return db.update(
                CalorieTable.TABLE_NAME,
                values,
                CalorieTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(calorieId)}
        );
    }

    // Delete a selected calorie entry
    public int deleteCalories(int calorieId) {
        SQLiteDatabase db = getWritableDatabase();

        return db.delete(
                CalorieTable.TABLE_NAME,
                CalorieTable.COLUMN_ID + " = ?",
                new String[]{String.valueOf(calorieId)}
        );
    }

    // Save or update the user's goal weight
    public long setGoalWeight(int userId, double goalWeight) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = db.rawQuery(
                "select * from " + GoalTable.TABLE_NAME +
                        " where " + GoalTable.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}
        );

        ContentValues values = new ContentValues();
        values.put(GoalTable.COLUMN_USER_ID, userId);
        values.put(GoalTable.COLUMN_TARGET_WEIGHT, goalWeight);

        long result;

        // Update the goal if it already exists, otherwise insert a new goal
        if (cursor.moveToFirst()) {
            result = db.update(
                    GoalTable.TABLE_NAME,
                    values,
                    GoalTable.COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(userId)}
            );
        } else {
            result = db.insert(
                    GoalTable.TABLE_NAME,
                    null,
                    values
            );
        }

        cursor.close();
        return result;
    }

    // Get the saved goal weight for the current user
    public double getGoalWeight(int userId) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "select " + GoalTable.COLUMN_TARGET_WEIGHT +
                        " from " + GoalTable.TABLE_NAME +
                        " where " + GoalTable.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}
        );

        double goalWeight = 0;

        if (cursor.moveToFirst()) {
            goalWeight = cursor.getDouble(0);
        }

        cursor.close();
        return goalWeight;
    }

    // Check whether the user has already chosen SMS notification settings
    public boolean hasNotificationSettings(int userId) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "select * from " + NotificationTable.TABLE_NAME +
                        " where " + NotificationTable.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}
        );

        boolean exists = cursor.moveToFirst();

        cursor.close();
        return exists;
    }

    // Save the user's SMS notification settings
    public void saveNotificationSettings(int userId, boolean enabled) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(NotificationTable.COLUMN_USER_ID, userId);
        values.put(NotificationTable.COLUMN_ENABLED, enabled ? 1 : 0);

        Cursor cursor = db.rawQuery(
                "select * from " + NotificationTable.TABLE_NAME +
                        " where " + NotificationTable.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}
        );

        // Update existing notification settings instead of creating duplicates
        if (cursor.moveToFirst()) {
            db.update(
                    NotificationTable.TABLE_NAME,
                    values,
                    NotificationTable.COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(userId)}
            );
        } else {
            db.insert(NotificationTable.TABLE_NAME, null, values);
        }

        cursor.close();
    }

    // Check whether SMS notifications are enabled for the current user
    public boolean notificationsEnabled(int userId) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "select " + NotificationTable.COLUMN_ENABLED +
                        " from " + NotificationTable.TABLE_NAME +
                        " where " + NotificationTable.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}
        );

        boolean enabled = false;

        if (cursor.moveToFirst()) {
            enabled = cursor.getInt(0) == 1;
        }

        cursor.close();
        return enabled;
    }
}