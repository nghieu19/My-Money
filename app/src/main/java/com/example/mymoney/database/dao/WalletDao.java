package com.example.mymoney.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mymoney.database.entity.Wallet;

import java.util.List;

@Dao
public interface WalletDao {
    
    @Insert
    long insert(Wallet wallet);
    
    @Update
    void update(Wallet wallet);
    
    @Delete
    void delete(Wallet wallet);
    
    @Query("SELECT * FROM wallet WHERE id = :walletId")
    Wallet getWalletById(int walletId);
    
    @Query("SELECT * FROM wallet WHERE user_id = :userId")
    List<Wallet> getWalletsByUserId(int userId);
    
    @Query("SELECT * FROM wallet WHERE user_id = :userId AND is_active = 1")
    List<Wallet> getActiveWalletsByUserId(int userId);
    
    @Query("SELECT * FROM wallet")
    List<Wallet> getAllWallets();
    
    @Query("SELECT SUM(balance) FROM wallet WHERE user_id = :userId AND is_active = 1")
    double getTotalBalanceByUserId(int userId);
    
    @Query("UPDATE wallet SET balance = :newBalance, updated_at = :timestamp WHERE id = :walletId")
    void updateBalance(int walletId, double newBalance, long timestamp);
    
    @Query("DELETE FROM wallet WHERE id = :walletId")
    void deleteById(int walletId);
}
