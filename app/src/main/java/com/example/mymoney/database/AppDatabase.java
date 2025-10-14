package com.example.mymoney.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.mymoney.database.dao.BudgetDao;
import com.example.mymoney.database.dao.CategoryDao;
import com.example.mymoney.database.dao.SavingGoalDao;
import com.example.mymoney.database.dao.TransactionDao;
import com.example.mymoney.database.dao.UserDao;
import com.example.mymoney.database.dao.WalletDao;
import com.example.mymoney.database.entity.Budget;
import com.example.mymoney.database.entity.Category;
import com.example.mymoney.database.entity.SavingGoal;
import com.example.mymoney.database.entity.Transaction;
import com.example.mymoney.database.entity.User;
import com.example.mymoney.database.entity.Wallet;

import java.util.List;
import java.util.concurrent.Executors;

@Database(
        entities = {
                User.class,
                Wallet.class,
                Category.class,
                Transaction.class,
                Budget.class,
                SavingGoal.class
        },
        version = 6, // 🔺 Tăng version để Room cập nhật schema
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "mymoney_database";
    private static AppDatabase instance;

    public abstract UserDao userDao();
    public abstract WalletDao walletDao();
    public abstract CategoryDao categoryDao();
    public abstract TransactionDao transactionDao();
    public abstract BudgetDao budgetDao();
    public abstract SavingGoalDao savingGoalDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration() // ✅ tránh crash khi thay đổi entity
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                createDefaultUser(context);
                                createDefaultCategories(context);
                            });
                        }

                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                ensureDefaultUserExists(context);
                                ensureDefaultCategoriesExist(context);
                            });
                        }
                    })
                    .build();
        }
        return instance;
    }

    // 🧍‍♂️ Tạo user mặc định
    private static void createDefaultUser(Context context) {
        AppDatabase db = getInstance(context);

        User defaultUser = new User();
        defaultUser.setUsername("default_user");
        defaultUser.setFullName("Default User");
        defaultUser.setEmail("default@mymoney.app");
        defaultUser.setPassword("");
        defaultUser.setGender("Not set");
        defaultUser.setJob("");
        defaultUser.setAddress("");
        defaultUser.setTel("");
        defaultUser.setDateOfBirth("");

        long userId = db.userDao().insert(defaultUser);
        android.util.Log.d("AppDatabase", "✅ Default user created with ID: " + userId);
    }

    // ✅ Đảm bảo user mặc định tồn tại
    private static void ensureDefaultUserExists(Context context) {
        AppDatabase db = getInstance(context);
        List<User> users = db.userDao().getAllUsers();
        if (users.isEmpty()) {
            createDefaultUser(context);
        }
    }

    // 🍱 Tạo category mặc định
    private static void createDefaultCategories(Context context) {
        AppDatabase db = getInstance(context);

        // Default wallet
        Wallet defaultWallet = new Wallet();
        defaultWallet.setName("Default Wallet");
        defaultWallet.setType("cash");
        defaultWallet.setBalance(0.0);
        defaultWallet.setCurrency("VND");
        defaultWallet.setUserId(1);
        defaultWallet.setActive(true);

        db.walletDao().insert(defaultWallet);

        // Expense categories
        String[] expenseCategoryNames = {"Food", "Home", "Transport", "Relationship", "Entertainment"};
        for (String categoryName : expenseCategoryNames) {
            Category category = new Category();
            category.setName(categoryName);
            category.setDescription("Default " + categoryName + " category");
            category.setType("expense");
            db.categoryDao().insert(category);
        }

        // Income categories
        String[] incomeCategoryNames = {"Salary", "Business", "Gifts", "Others"};
        for (String categoryName : incomeCategoryNames) {
            Category category = new Category();
            category.setName(categoryName);
            category.setDescription("Default " + categoryName + " category");
            category.setType("income");
            db.categoryDao().insert(category);
        }
    }

    // ✅ Đảm bảo category mặc định tồn tại
    private static void ensureDefaultCategoriesExist(Context context) {
        AppDatabase db = getInstance(context);
        List<Category> categories = db.categoryDao().getAllCategories();
        if (categories.isEmpty()) {
            createDefaultCategories(context);
        }
    }
}
