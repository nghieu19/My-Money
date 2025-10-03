package com.example.mymoney;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mymoney.database.AppDatabase;
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
        
        // Load wallet data
        loadWalletData();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload data when fragment becomes visible
        loadWalletData();
    }
    
    private void loadWalletData() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                int walletId = MainActivity.getSelectedWalletId();
                
                // If no wallet selected, use the first available wallet
                if (walletId == -1) {
                    List<Wallet> wallets = db.walletDao().getActiveWalletsByUserId(MainActivity.DEFAULT_USER_ID);
                    if (!wallets.isEmpty()) {
                        walletId = wallets.get(0).getId();
                        MainActivity.setSelectedWalletId(walletId);
                    }
                }
                
                if (walletId != -1) {
                    Wallet wallet = db.walletDao().getWalletById(walletId);
                    
                    // Calculate total expenses and incomes from transactions
                    double totalExpenses = db.transactionDao().getTotalExpensesByWallet(walletId);
                    double totalIncomes = db.transactionDao().getTotalIncomeByWallet(walletId);
                    
                    // Update UI on main thread
                    if (getActivity() != null) {
                        final Wallet finalWallet = wallet;
                        final double finalExpenses = totalExpenses;
                        final double finalIncomes = totalIncomes;
                        
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
                        });
                    }
                } else {
                    // No wallet available
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            balanceAmount.setText("0 VND");
                            expensesAmount.setText("-0 VND");
                            incomesAmount.setText("+0 VND");
                            
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            balanceDate.setText(sdf.format(new Date()));
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
