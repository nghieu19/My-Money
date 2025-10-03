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
        version = 5,
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
            .fallbackToDestructiveMigration()
            .addCallback(new Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);
                    // Create default user and categories in background thread
                    Executors.newSingleThreadExecutor().execute(() -> {
                        createDefaultUser(context);
                        createDefaultCategories(context);
                    });
                }
                
                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    super.onOpen(db);
                    // Ensure default user and categories exist even if database already existed
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
    
    /**
     * Creates a default user when the database is first created
     * This user will have ID = 1 and can be used for wallets
     */
    private static void createDefaultUser(Context context) {
        AppDatabase db = getInstance(context);
        
        // Create default user
        User defaultUser = new User();
        defaultUser.setUsername("default_user");
        defaultUser.setFullName("Default User");
        defaultUser.setEmail("default@mymoney.app");
        defaultUser.setPassword(""); // Empty password for default user
        
        long userId = db.userDao().insert(defaultUser);
        android.util.Log.d("AppDatabase", "Default user created with ID: " + userId);
    }
    
    /**
     * Ensures the default user exists (for existing databases)
     */
    private static void ensureDefaultUserExists(Context context) {
        AppDatabase db = getInstance(context);
        
        // Check if any user exists
        List<User> users = db.userDao().getAllUsers();
        if (users.isEmpty()) {
            // Create default user if no users exist
            User defaultUser = new User();
            defaultUser.setUsername("default_user");
            defaultUser.setFullName("Default User");
            defaultUser.setEmail("default@mymoney.app");
            defaultUser.setPassword(""); // Empty password for default user
            
            long userId = db.userDao().insert(defaultUser);
            android.util.Log.d("AppDatabase", "Default user ensured with ID: " + userId);
        }
    }
    
    /**
     * Creates default categories when the database is first created
     * Categories: Food, Home, Transport, Relationship, Entertainment (Expense)
     * Salary, Business, Gifts, Others (Income)
     */
    private static void createDefaultCategories(Context context) {
        AppDatabase db = getInstance(context);
        
        // Create default wallet for transactions (independent of categories)
        Wallet defaultWallet = new Wallet();
        defaultWallet.setName("Default Wallet");
        defaultWallet.setType("cash");
        defaultWallet.setBalance(0.0);
        defaultWallet.setCurrency("VND");
        defaultWallet.setUserId(1); // Default user ID
        defaultWallet.setActive(true);
        
        db.walletDao().insert(defaultWallet);

        // Create expense categories (wallet-independent)
        String[] expenseCategoryNames = {"Food", "Home", "Transport", "Relationship", "Entertainment"};
        for (String categoryName : expenseCategoryNames) {
            Category category = new Category();
            category.setName(categoryName);
            category.setDescription("Default " + categoryName + " category");
            category.setType("expense");

            long categoryId = db.categoryDao().insert(category);
            android.util.Log.d("AppDatabase", "Default expense category created: " + categoryName + " with ID: " + categoryId);
        }
        
        // Create income categories (wallet-independent)
        String[] incomeCategoryNames = {"Salary", "Business", "Gifts", "Others"};
        for (String categoryName : incomeCategoryNames) {
            Category category = new Category();
            category.setName(categoryName);
            category.setDescription("Default " + categoryName + " category");
            category.setType("income");

            long categoryId = db.categoryDao().insert(category);
            android.util.Log.d("AppDatabase", "Default income category created: " + categoryName + " with ID: " + categoryId);
        }
    }
    
    /**
     * Ensures default categories exist (for existing databases)
     */
    private static void ensureDefaultCategoriesExist(Context context) {
        AppDatabase db = getInstance(context);
        
        // Check if any categories exist
        List<Category> categories = db.categoryDao().getAllCategories();
        if (categories.isEmpty()) {
            // Create default wallet if none exists (for transactions, not categories)
            List<Wallet> wallets = db.walletDao().getActiveWalletsByUserId(1);
            if (wallets.isEmpty()) {
                Wallet defaultWallet = new Wallet();
                defaultWallet.setName("Default Wallet");
                defaultWallet.setType("cash");
                defaultWallet.setBalance(0.0);
                defaultWallet.setCurrency("VND");
                defaultWallet.setUserId(1);
                defaultWallet.setActive(true);
                db.walletDao().insert(defaultWallet);
            }
            
            // Create expense categories (wallet-independent)
            String[] expenseCategoryNames = {"Food", "Home", "Transport", "Relationship", "Entertainment"};
            for (String categoryName : expenseCategoryNames) {
                Category category = new Category();
                category.setName(categoryName);
                category.setDescription("Default " + categoryName + " category");
                category.setType("expense");

                long categoryId = db.categoryDao().insert(category);
                android.util.Log.d("AppDatabase", "Default expense category ensured: " + categoryName + " with ID: " + categoryId);
            }
            
            // Create income categories (wallet-independent)
            String[] incomeCategoryNames = {"Salary", "Business", "Gifts", "Others"};
            for (String categoryName : incomeCategoryNames) {
                Category category = new Category();
                category.setName(categoryName);
                category.setDescription("Default " + categoryName + " category");
                category.setType("income");

                long categoryId = db.categoryDao().insert(category);
                android.util.Log.d("AppDatabase", "Default income category ensured: " + categoryName + " with ID: " + categoryId);
            }
        }
    }
}

