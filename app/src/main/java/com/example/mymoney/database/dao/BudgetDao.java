package com.example.mymoney.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mymoney.database.entity.Budget;

import java.util.List;

@Dao
public interface BudgetDao {
    
    @Insert
    long insert(Budget budget);
    
    @Update
    void update(Budget budget);
    
    @Delete
    void delete(Budget budget);
    
    @Query("SELECT * FROM budget WHERE id = :budgetId")
    Budget getBudgetById(int budgetId);
    
    @Query("SELECT * FROM budget WHERE user_id = :userId")
    List<Budget> getBudgetsByUserId(int userId);
    
    @Query("SELECT * FROM budget WHERE wallet_id = :walletId")
    List<Budget> getBudgetsByWalletId(int walletId);
    
    @Query("SELECT * FROM budget WHERE category_id = :categoryId")
    List<Budget> getBudgetsByCategoryId(int categoryId);
    
    @Query("SELECT * FROM budget")
    List<Budget> getAllBudgets();
    
    @Query("DELETE FROM budget WHERE id = :budgetId")
    void deleteById(int budgetId);
}
