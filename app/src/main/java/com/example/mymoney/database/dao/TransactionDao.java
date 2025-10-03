package com.example.mymoney.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mymoney.database.entity.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {
    
    @Insert
    long insert(Transaction transaction);
    
    @Update
    void update(Transaction transaction);
    
    @Delete
    void delete(Transaction transaction);
    
    @Query("SELECT * FROM `transaction` WHERE id = :transactionId")
    Transaction getTransactionById(int transactionId);
    
    @Query("SELECT * FROM `transaction` WHERE wallet_id = :walletId ORDER BY created_at DESC")
    List<Transaction> getTransactionsByWalletId(int walletId);
    
    @Query("SELECT * FROM `transaction` WHERE user_id = :userId ORDER BY created_at DESC")
    List<Transaction> getTransactionsByUserId(int userId);
    
    @Query("SELECT * FROM `transaction` WHERE category_id = :categoryId ORDER BY created_at DESC")
    List<Transaction> getTransactionsByCategoryId(int categoryId);
    
    @Query("SELECT * FROM `transaction` WHERE wallet_id = :walletId AND type = :type ORDER BY created_at DESC")
    List<Transaction> getTransactionsByWalletAndType(int walletId, String type);
    
    @Query("SELECT * FROM `transaction` WHERE user_id = :userId AND type = :type ORDER BY created_at DESC")
    List<Transaction> getTransactionsByUserAndType(int userId, String type);
    
    @Query("SELECT * FROM `transaction` WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    List<Transaction> getTransactionsByDateRange(long startDate, long endDate);
    
    @Query("SELECT * FROM `transaction` WHERE wallet_id = :walletId AND created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    List<Transaction> getTransactionsByWalletAndDateRange(int walletId, long startDate, long endDate);
    
    @Query("SELECT SUM(amount) FROM `transaction` WHERE wallet_id = :walletId AND type = 'expense'")
    double getTotalExpensesByWallet(int walletId);
    
    @Query("SELECT SUM(amount) FROM `transaction` WHERE wallet_id = :walletId AND type = 'income'")
    double getTotalIncomeByWallet(int walletId);
    
    @Query("SELECT SUM(amount) FROM `transaction` WHERE user_id = :userId AND type = 'expense'")
    double getTotalExpensesByUser(int userId);
    
    @Query("SELECT SUM(amount) FROM `transaction` WHERE user_id = :userId AND type = 'income'")
    double getTotalIncomeByUser(int userId);
    
    @Query("SELECT * FROM `transaction` WHERE is_recurring = 1")
    List<Transaction> getRecurringTransactions();
    
    @Query("SELECT * FROM `transaction` ORDER BY created_at DESC")
    List<Transaction> getAllTransactions();
    
    @Query("DELETE FROM `transaction` WHERE id = :transactionId")
    void deleteById(int transactionId);
    
    @Query("SELECT * FROM `transaction` ORDER BY created_at DESC LIMIT :limit")
    List<Transaction> getRecentTransactions(int limit);
}
