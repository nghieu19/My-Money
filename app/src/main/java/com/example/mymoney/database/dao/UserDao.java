package com.example.mymoney.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mymoney.database.entity.User;

import java.util.List;

@Dao
public interface UserDao {
    
    @Insert
    long insert(User user);
    
    @Update
    void update(User user);
    
    @Delete
    void delete(User user);
    
    @Query("SELECT * FROM user WHERE id = :userId")
    User getUserById(int userId);
    
    @Query("SELECT * FROM user WHERE username = :username")
    User getUserByUsername(String username);
    
    @Query("SELECT * FROM user WHERE email = :email")
    User getUserByEmail(String email);
    
    @Query("SELECT * FROM user WHERE username = :username AND password = :password")
    User login(String username, String password);
    
    @Query("SELECT * FROM user")
    List<User> getAllUsers();
    
    @Query("DELETE FROM user WHERE id = :userId")
    void deleteById(int userId);
}
