package com.example.mymoney;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymoney.adapter.CategoryAdapter;
import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.entity.Category;
import com.example.mymoney.database.entity.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ImportFragment extends Fragment {

    private LinearLayout expenseSelector, incomeSelector;
    private EditText amountInput, notesInput;
    private LinearLayout dateSelector, categorySelector;
    private TextView dateText, categoryText;
    private ImageView categoryIcon;
    private RadioGroup repeatRadioGroup;
    private RadioButton repeatYes, repeatNo;
    private LinearLayout recurringSection;
    private Spinner recurringSpinner;
    private Button saveButton;
    
    private String selectedType = "expense"; // Default to expense
    private Calendar selectedDate;
    private int selectedCategoryId = -1; // Will be loaded from database
    private Category selectedCategory = null;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_import, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        expenseSelector = view.findViewById(R.id.expense_selector);
        incomeSelector = view.findViewById(R.id.income_selector);
        amountInput = view.findViewById(R.id.amount_input);
        notesInput = view.findViewById(R.id.notes_input);
        dateSelector = view.findViewById(R.id.date_selector);
        dateText = view.findViewById(R.id.date_text);
        categorySelector = view.findViewById(R.id.category_selector);
        categoryText = view.findViewById(R.id.category_text);
        categoryIcon = view.findViewById(R.id.category_icon);
        repeatRadioGroup = view.findViewById(R.id.repeat_radio_group);
        repeatYes = view.findViewById(R.id.repeat_yes);
        repeatNo = view.findViewById(R.id.repeat_no);
        recurringSection = view.findViewById(R.id.recurring_section);
        recurringSpinner = view.findViewById(R.id.recurring_spinner);
        saveButton = view.findViewById(R.id.save_button);
        
        // Initialize selected date to today
        selectedDate = Calendar.getInstance();
        updateDateDisplay();
        
        // Set up recurring spinner
        setupRecurringSpinner();
        
        // Set up click listeners
        setupListeners();
        
        // Load default category from database
        loadDefaultCategory();
    }
    
    private void setupRecurringSpinner() {
        String[] recurringOptions = {
            getString(R.string.daily),
            getString(R.string.weekly),
            getString(R.string.monthly),
            getString(R.string.yearly)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            recurringOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recurringSpinner.setAdapter(adapter);
        recurringSpinner.setSelection(2); // Default to Monthly
    }
    
    private void setupListeners() {
        // Transaction type selectors
        expenseSelector.setOnClickListener(v -> {
            selectTransactionType("expense");
            loadCategoriesForType("expense");
        });
        incomeSelector.setOnClickListener(v -> {
            selectTransactionType("income");
            loadCategoriesForType("income");
        });
        
        // Date selector
        dateSelector.setOnClickListener(v -> showDatePicker());
        
        // Category selector
        categorySelector.setOnClickListener(v -> showCategoryDialog());
        
        // Repeat radio group
        repeatRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.repeat_yes) {
                recurringSection.setVisibility(View.VISIBLE);
            } else {
                recurringSection.setVisibility(View.GONE);
            }
        });
        
        // Save button
        saveButton.setOnClickListener(v -> saveTransaction());
    }
    
    private void selectTransactionType(String type) {
        selectedType = type;
        
        if (type.equals("expense")) {
            expenseSelector.setBackgroundResource(R.drawable.selector_background);
            incomeSelector.setBackgroundResource(R.drawable.search_background);
            // Update text colors if needed
        } else {
            incomeSelector.setBackgroundResource(R.drawable.selector_background);
            expenseSelector.setBackgroundResource(R.drawable.search_background);
        }
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateDisplay();
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateText.setText(sdf.format(selectedDate.getTime()));
    }
    
    /**
     * Load the first available category from database as default
     */
    private void loadDefaultCategory() {
        loadCategoriesForType(selectedType);
    }
    
    /**
     * Load categories based on transaction type (expense or income)
     */
    private void loadCategoriesForType(String type) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                
                // Get categories by type
                List<Category> categories;
                if (type.equals("expense")) {
                    categories = db.categoryDao().getAllExpenseCategories();
                    android.util.Log.d("ImportFragment", "Loaded " + categories.size() + " expense categories");
                } else {
                    categories = db.categoryDao().getAllIncomeCategories();
                    android.util.Log.d("ImportFragment", "Loaded " + categories.size() + " income categories");
                }
                
                if (!categories.isEmpty() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Auto-select first category if none selected or type changed
                        if (selectedCategory == null || !selectedCategory.getType().equals(type)) {
                            selectedCategory = categories.get(0);
                            selectedCategoryId = selectedCategory.getId();
                            updateCategoryDisplay();
                            android.util.Log.d("ImportFragment", "Auto-selected category: " + selectedCategory.getName());
                        }
                    });
                } else {
                    android.util.Log.w("ImportFragment", "No categories found for type: " + type);
                }
            } catch (Exception e) {
                android.util.Log.e("ImportFragment", "Error loading categories", e);
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Show category selection dialog
     */
    private void showCategoryDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_category_selection);
        
        RecyclerView recyclerView = dialog.findViewById(R.id.categories_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        CategoryAdapter adapter = new CategoryAdapter(category -> {
            selectedCategory = category;
            selectedCategoryId = category.getId();
            updateCategoryDisplay();
            dialog.dismiss();
        });
        
        adapter.setSelectedCategoryId(selectedCategoryId);
        recyclerView.setAdapter(adapter);
        
        // Load categories based on current type
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<Category> categories;
                if (selectedType.equals("expense")) {
                    categories = db.categoryDao().getAllExpenseCategories();
                    android.util.Log.d("ImportFragment", "Dialog: Loaded " + categories.size() + " expense categories");
                } else {
                    categories = db.categoryDao().getAllIncomeCategories();
                    android.util.Log.d("ImportFragment", "Dialog: Loaded " + categories.size() + " income categories");
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter.setCategories(categories);
                        android.util.Log.d("ImportFragment", "Dialog: Set " + categories.size() + " categories to adapter");
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("ImportFragment", "Dialog: Error loading categories", e);
                e.printStackTrace();
            }
        }).start();
        
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
    
    /**
     * Update category display with selected category
     */
    private void updateCategoryDisplay() {
        if (selectedCategory != null) {
            categoryText.setText(selectedCategory.getName());
            
            // Set icon based on category name
            int iconRes;
            switch (selectedCategory.getName().toLowerCase()) {
                case "food":
                    iconRes = R.drawable.ic_food;
                    break;
                case "home":
                    iconRes = R.drawable.ic_home;
                    break;
                case "transport":
                    iconRes = R.drawable.ic_taxi;
                    break;
                case "relationship":
                    iconRes = R.drawable.ic_love;
                    break;
                case "entertainment":
                    iconRes = R.drawable.ic_entertainment;
                    break;
                case "salary":
                    iconRes = R.drawable.ic_salary;
                    break;
                case "business":
                    iconRes = R.drawable.ic_work;
                    break;
                case "gifts":
                    iconRes = R.drawable.ic_gift;
                    break;
                case "others":
                default:
                    iconRes = R.drawable.ic_more_apps;
                    break;
            }
            categoryIcon.setImageResource(iconRes);
            categoryIcon.setVisibility(View.VISIBLE);
        } else {
            categoryText.setText("Select Category");
            categoryIcon.setVisibility(View.GONE);
        }
    }
    
    private void saveTransaction() {
        // Get selected wallet from MainActivity
        int walletId = MainActivity.getSelectedWalletId();
        
        // If no wallet selected, try to get the first available wallet
        if (walletId == -1) {
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                var wallets = db.walletDao().getActiveWalletsByUserId(MainActivity.DEFAULT_USER_ID);
                if (!wallets.isEmpty()) {
                    MainActivity.setSelectedWalletId(wallets.get(0).getId());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> saveTransactionWithWallet());
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(requireContext(), "Please create a wallet first", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }).start();
            return;
        }
        
        saveTransactionWithWallet();
    }
    
    private void saveTransactionWithWallet() {
        // Validate input
        String amountStr = amountInput.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if category is selected
        if (selectedCategoryId == -1 || selectedCategory == null) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create transaction object
        Transaction transaction = new Transaction();
        transaction.setWalletId(MainActivity.getSelectedWalletId());
        transaction.setCategoryId(selectedCategoryId);
        transaction.setUserId(MainActivity.DEFAULT_USER_ID);
        transaction.setAmount(amount);
        transaction.setDescription(notesInput.getText().toString().trim());
        transaction.setType(selectedType);
        transaction.setCreatedAt(selectedDate.getTimeInMillis());
        transaction.setUpdatedAt(System.currentTimeMillis());
        
        // Handle recurring
        boolean isRecurring = repeatYes.isChecked();
        transaction.setRecurring(isRecurring);
        
        if (isRecurring) {
            String[] intervals = {"daily", "weekly", "monthly", "yearly"};
            int selectedPosition = recurringSpinner.getSelectedItemPosition();
            transaction.setRecurringInterval(intervals[selectedPosition]);
        } else {
            transaction.setRecurringInterval(null);
        }
        
        // Save to database in background thread
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                long transactionId = db.transactionDao().insert(transaction);
                
                // Update wallet balance
                updateWalletBalance(transaction);
                
                // Show success message on UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Transaction saved successfully!", Toast.LENGTH_SHORT).show();
                        clearForm();
                        
                        // Refresh HomeFragment if it exists
                        refreshHomeFragment();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error saving transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }
    
    private void updateWalletBalance(Transaction transaction) {
        try {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            com.example.mymoney.database.entity.Wallet wallet = 
                db.walletDao().getWalletById(transaction.getWalletId());
            
            if (wallet != null) {
                double currentBalance = wallet.getBalance();
                double newBalance;
                
                if (transaction.getType().equals("income")) {
                    newBalance = currentBalance + transaction.getAmount();
                } else {
                    newBalance = currentBalance - transaction.getAmount();
                }
                
                wallet.setBalance(newBalance);
                db.walletDao().update(wallet);
                
                android.util.Log.d("ImportFragment", "Wallet balance updated: " + currentBalance + " -> " + newBalance);
            }
        } catch (Exception e) {
            android.util.Log.e("ImportFragment", "Error updating wallet balance", e);
        }
    }
    
    private void refreshHomeFragment() {
        // Notify MainActivity to refresh fragments
        if (getActivity() instanceof MainActivity) {
            // HomeFragment will reload in onResume when user switches back to it
            android.util.Log.d("ImportFragment", "Transaction saved, HomeFragment will refresh on resume");
        }
    }
    
    private void clearForm() {
        amountInput.setText("");
        notesInput.setText("");
        repeatNo.setChecked(true);
        recurringSection.setVisibility(View.GONE);
        selectedDate = Calendar.getInstance();
        updateDateDisplay();
        selectedType = "expense";
        selectTransactionType("expense");
    }
}
