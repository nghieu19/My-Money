package com.example.mymoney;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymoney.adapter.TransactionAdapter;
import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.entity.Transaction;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView transactionsRecyclerView;
    private TransactionAdapter adapter;
    private EditText searchEditText;
    private ImageView filterIcon;
    
    private List<Transaction> allTransactions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        transactionsRecyclerView = view.findViewById(R.id.transactions_recycler_view);
        searchEditText = view.findViewById(R.id.search_edit_text);
        filterIcon = view.findViewById(R.id.filter_icon);
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Set up search functionality
        setupSearch();
        
        // Load transactions
        loadTransactions();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload transactions when fragment becomes visible
        loadTransactions();
    }
    
    private void setupRecyclerView() {
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter(
            AppDatabase.getInstance(requireContext()),
            transaction -> {
                // Handle transaction click - could open details dialog
                // For now, just log it
                android.util.Log.d("HistoryFragment", "Clicked transaction: " + transaction.getId());
            }
        );
        transactionsRecyclerView.setAdapter(adapter);
    }
    
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTransactions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        // TODO: Implement filter functionality
        filterIcon.setOnClickListener(v -> {
            // Show filter dialog (type, date range, category, etc.)
            android.util.Log.d("HistoryFragment", "Filter clicked");
        });
    }
    
    private void loadTransactions() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                int walletId = MainActivity.getSelectedWalletId();
                
                // If no wallet selected, get all transactions for user
                List<Transaction> transactions;
                if (walletId == -1) {
                    transactions = db.transactionDao().getTransactionsByUserId(MainActivity.DEFAULT_USER_ID);
                } else {
                    transactions = db.transactionDao().getTransactionsByWalletId(walletId);
                }
                
                allTransactions = transactions;
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter.setTransactions(transactions);
                        android.util.Log.d("HistoryFragment", "Loaded " + transactions.size() + " transactions");
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("HistoryFragment", "Error loading transactions", e);
                e.printStackTrace();
            }
        }).start();
    }
    
    private void filterTransactions(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.setTransactions(allTransactions);
            return;
        }
        
        // Filter transactions by description or amount
        new Thread(() -> {
            try {
                List<Transaction> filtered = new ArrayList<>();
                String lowerQuery = query.toLowerCase();
                
                AppDatabase db = AppDatabase.getInstance(requireContext());
                
                for (Transaction transaction : allTransactions) {
                    // Check description
                    if (transaction.getDescription() != null && 
                        transaction.getDescription().toLowerCase().contains(lowerQuery)) {
                        filtered.add(transaction);
                        continue;
                    }
                    
                    // Check amount
                    String amountStr = String.valueOf((int) transaction.getAmount());
                    if (amountStr.contains(query)) {
                        filtered.add(transaction);
                        continue;
                    }
                    
                    // Check category name
                    com.example.mymoney.database.entity.Category category = 
                        db.categoryDao().getCategoryById(transaction.getCategoryId());
                    if (category != null && 
                        category.getName().toLowerCase().contains(lowerQuery)) {
                        filtered.add(transaction);
                    }
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter.setTransactions(filtered);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Public method to refresh data from outside (e.g., after importing transaction)
     */
    public void refreshData() {
        loadTransactions();
    }
}
