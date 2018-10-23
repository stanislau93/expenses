package com.example.admin.expenses.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.database.Cursor;

@Dao
public interface DebtDao {

    @Insert
    long insert(Debt debt);

    @Query("SELECT * FROM " + Debt.TABLE_NAME + " WHERE window_id = :windowId")
    Cursor selectByWindowId(long windowId);

    @Query("SELECT SUM(amount) FROM " + Debt.TABLE_NAME + " WHERE owner = :owner AND debtor = :debtor AND window_id = :windowId")
    Cursor selectPersonalDebt(String owner, String debtor, long windowId);

    @Query("DELETE FROM " + Debt.TABLE_NAME + " WHERE owner = :owner AND debtor = :debtor AND window_id = :windowId")
    int delete(String owner, String debtor, long windowId);
}