package com.example.mymoney.database;

import android.content.Context;

import com.example.mymoney.database.dao.BudgetDao;
import com.example.mymoney.database.dao.CategoryDao;
import com.example.mymoney.database.dao.SavingGoalDao;
import com.example.mymoney.database.dao.TransactionDao;
import com.example.mymoney.database.dao.UserDao;
import com.example.mymoney.database.dao.WalletDao;

public class DatabaseHelper {
    
    private final AppDatabase database;
    
    public DatabaseHelper(Context context) {
        this.database = AppDatabase.getInstance(context);
    }
    
    public UserDao getUserDao() {
        return database.userDao();
    }
    
    public WalletDao getWalletDao() {
        return database.walletDao();
    }
    
    public CategoryDao getCategoryDao() {
        return database.categoryDao();
    }
    
    public TransactionDao getTransactionDao() {
        return database.transactionDao();
    }
    
    public BudgetDao getBudgetDao() {
        return database.budgetDao();
    }
    
    public SavingGoalDao getSavingGoalDao() {
        return database.savingGoalDao();
    }
    
    public AppDatabase getDatabase() {
        return database;
    }
    
    public void close() {
        if (database != null && database.isOpen()) {
            database.close();
        }
    }
}
