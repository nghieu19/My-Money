package com.example.mymoney;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * NewWalletFragment - Shows the wallet type selection screen
 * This is the first step in the wallet creation pipeline
 */
public class NewWalletFragment extends Fragment {

    private ImageView btnBack;
    private LinearLayout optionCash;
    private LinearLayout optionBankAccount;
    private LinearLayout optionVirtualAccount;

    // Wallet type constants
    public static final String WALLET_TYPE_CASH = "cash";
    public static final String WALLET_TYPE_BANK = "bank";
    public static final String WALLET_TYPE_VIRTUAL = "virtual";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_new_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Hide main header and footer when this fragment becomes visible
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideMainHeaderAndFooter();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        // Only show header/footer if we're leaving the wallet creation flow entirely
        // Check if the next fragment is NOT a wallet creation fragment
        if (getActivity() != null && isRemoving()) {
            // Fragment is being removed (back button pressed to leave wallet creation)
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showMainHeaderAndFooter();
            }
        }
    }

    private void initializeViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        optionCash = view.findViewById(R.id.option_cash);
        optionBankAccount = view.findViewById(R.id.option_bank_account);
        optionVirtualAccount = view.findViewById(R.id.option_virtual_account);
    }

    private void setupListeners() {
        // Back button listener - return to previous fragment
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Cash option listener
        optionCash.setOnClickListener(v -> openAddWalletFragment(WALLET_TYPE_CASH));

        // Bank account option listener
        optionBankAccount.setOnClickListener(v -> openAddWalletFragment(WALLET_TYPE_BANK));

        // Virtual account option listener
        optionVirtualAccount.setOnClickListener(v -> openAddWalletFragment(WALLET_TYPE_VIRTUAL));
    }

    /**
     * Opens the AddWalletFragment with the selected wallet type
     * @param walletType The type of wallet selected (cash, bank, or virtual)
     */
    private void openAddWalletFragment(String walletType) {
        if (getActivity() == null) return;

        AddWalletFragment addWalletFragment = AddWalletFragment.newInstance(walletType);
        
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, addWalletFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
