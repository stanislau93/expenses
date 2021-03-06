package com.example.admin.expenses.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.support.annotation.VisibleForTesting;

/**
 * The Room database.
 */
@Database(entities = {Window.class, Item.class}, version = 2, exportSchema = false)
public abstract class ExpensesDatabase extends RoomDatabase {

    /**
     * @return The DAO for the Window table.
     */
    @SuppressWarnings("WeakerAccess")
    public abstract WindowDao window();

    /**
     * @return The DAO for the Item table.
     */
    @SuppressWarnings("WeakerAccess")
    public abstract ItemDao item();

    /** The only instance */
    private static ExpensesDatabase sInstance;

    /**
     * Gets the singleton instance of ExpensesDatabase.
     *
     * @param context The context.
     * @return The singleton instance of ExpensesDatabase.
     */
    public static synchronized ExpensesDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = Room
                    .databaseBuilder(context.getApplicationContext(), ExpensesDatabase.class, "ex")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return sInstance;
    }

    /**
     * Switches the internal implementation with an empty in-memory database.
     *
     * @param context The context.
     */
    @VisibleForTesting
    public static void switchToInMemory(Context context) {
        sInstance = Room.inMemoryDatabaseBuilder(context.getApplicationContext(),
                ExpensesDatabase.class).build();
    }
}