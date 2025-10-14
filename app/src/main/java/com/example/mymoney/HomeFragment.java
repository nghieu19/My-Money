package com.example.mymoney;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymoney.adapter.TransactionAdapter;
import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.entity.Transaction;
import com.example.mymoney.database.entity.Wallet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView balanceAmount;
    private TextView balanceDate;
    private TextView expensesAmount;
    private TextView incomesAmount;
    private RecyclerView recentTransactionsRecyclerView;
    private TransactionAdapter transactionAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        balanceAmount = view.findViewById(R.id.balance_amount);
        balanceDate = view.findViewById(R.id.balance_date);
        expensesAmount = view.findViewById(R.id.expenses_amount);
        incomesAmount = view.findViewById(R.id.incomes_amount);
        recentTransactionsRecyclerView = view.findViewById(R.id.recent_transactions_recycler_view);

        // Set up RecyclerView
        setupRecyclerView();

        // Load wallet data
        loadWalletData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload data when fragment becomes visible
        loadWalletData();
    }

    private void setupRecyclerView() {
        recentTransactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        transactionAdapter = new TransactionAdapter(
                AppDatabase.getInstance(requireContext()),
                transaction -> {
                    // Handle transaction click
                    android.util.Log.d("HomeFragment", "Clicked transaction: " + transaction.getId());
                }
        );
        recentTransactionsRecyclerView.setAdapter(transactionAdapter);
    }

    private void loadWalletData() {
        android.util.Log.d("HomeFragment", "loadWalletData() called - Current user: " + MainActivity.getCurrentUserId() + ", Selected wallet: " + MainActivity.getSelectedWalletId());

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                int walletId = MainActivity.getSelectedWalletId();

                // If no wallet selected, use the first available wallet
                if (walletId == -1) {
                    List<Wallet> wallets = db.walletDao().getActiveWalletsByUserId(MainActivity.getCurrentUserId());
                    android.util.Log.d("HomeFragment", "No wallet selected. Found " + wallets.size() + " wallets for user " + MainActivity.getCurrentUserId());
                    if (!wallets.isEmpty()) {
                        walletId = wallets.get(0).getId();
                        MainActivity.setSelectedWalletId(walletId);
                        android.util.Log.d("HomeFragment", "Auto-selected first wallet: ID " + walletId);
                    }
                }

                if (walletId != -1) {
                    Wallet wallet = db.walletDao().getWalletById(walletId);

                    // Calculate total expenses and incomes from transactions FOR THIS WALLET
                    double totalExpenses = db.transactionDao().getTotalExpensesByWallet(walletId);
                    double totalIncomes = db.transactionDao().getTotalIncomeByWallet(walletId);

                    // Get recent transactions FOR THIS WALLET (limit to 5)
                    List<Transaction> allTransactions = db.transactionDao().getTransactionsByWalletId(walletId);
                    List<Transaction> recentTransactions = allTransactions.size() > 5
                            ? allTransactions.subList(0, 5)
                            : allTransactions;

                    android.util.Log.d("HomeFragment", "Loading wallet ID: " + walletId +
                            ", transactions: " + recentTransactions.size());

                    // Update UI on main thread
                    if (getActivity() != null) {
                        final Wallet finalWallet = wallet;
                        final double finalExpenses = totalExpenses;
                        final double finalIncomes = totalIncomes;
                        final List<Transaction> finalTransactions = recentTransactions;

                        getActivity().runOnUiThread(() -> {
                            if (finalWallet != null) {
                                balanceAmount.setText(String.format(Locale.getDefault(),
                                        "%,.0f %s", finalWallet.getBalance(), finalWallet.getCurrency()));
                            } else {
                                balanceAmount.setText("0 VND");
                            }

                            expensesAmount.setText(String.format(Locale.getDefault(),
                                    "-%,.0f VND", finalExpenses));
                            incomesAmount.setText(String.format(Locale.getDefault(),
                                    "+%,.0f VND", finalIncomes));

                            // Set current date
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            balanceDate.setText(sdf.format(new Date()));

                            // Update recent transactions
                            transactionAdapter.setTransactions(finalTransactions);

                            android.util.Log.d("HomeFragment", "UI updated with " + finalTransactions.size() + " transactions");
                        });
                    }
                } else {
                    // No wallet available
                    android.util.Log.d("HomeFragment", "No wallet available for user " + MainActivity.getCurrentUserId());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            balanceAmount.setText("0 VND");
                            expensesAmount.setText("-0 VND");
                            incomesAmount.setText("+0 VND");

                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            balanceDate.setText(sdf.format(new Date()));

                            // Clear recent transactions
                            transactionAdapter.setTransactions(new java.util.ArrayList<>());
                            android.util.Log.d("HomeFragment", "UI cleared - no wallet data");
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error loading wallet data", e);
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Public method to refresh data from outside (e.g., after importing transaction)
     */
    public void refreshData() {
        android.util.Log.d("HomeFragment", "refreshData() called from MainActivity");
        loadWalletData();
    }
    /**
     * Set the selected wallet ID (for fragments like HomeFragment)
     */


}