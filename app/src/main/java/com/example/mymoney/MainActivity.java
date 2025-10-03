package com.example.mymoney;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.entity.Wallet;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int DEFAULT_USER_ID = 1; // Default user ID for wallets
    private static int selectedWalletId = -1; // Currently selected wallet ID (-1 means no wallet selected)

    private FragmentManager fragmentManager;
    private TextView headerTitle;
    private CardView walletPanel;
    private ImageView btnWallet;
    private LinearLayout walletListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_container);
        
        View mainLayout = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        fragmentManager = getSupportFragmentManager();
        headerTitle = findViewById(R.id.header_title);
        walletPanel = findViewById(R.id.wallet_panel);
        btnWallet = findViewById(R.id.btn_wallet);

        setupWalletButton();

        setupNavigationBar();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "Home");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload wallets when activity resumes (e.g., after adding a new wallet)
        loadWalletsFromDatabase();
    }

    private void setupWalletButton() {
        btnWallet.setOnClickListener(v -> {
            loadWalletsFromDatabase();
            toggleWalletPanel();
        });

        LinearLayout addWalletItem = findViewById(R.id.add_wallet_item);

        addWalletItem.setOnClickListener(v -> {
            hideWalletPanel();
            loadFragmentWithBackStack(new NewWalletFragment(), "New Wallet");
        });
    }

    /**
     * Load wallets from database and populate the wallet panel
     */
    private void loadWalletsFromDatabase() {
        walletListContainer = findViewById(R.id.wallet_list_container);
        
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Wallet> wallets = db.walletDao().getActiveWalletsByUserId(DEFAULT_USER_ID); // Default user ID
            
            android.util.Log.d("MainActivity", "Loaded " + wallets.size() + " wallets from database");
            for (Wallet w : wallets) {
                android.util.Log.d("MainActivity", "  - Wallet: " + w.getName() + " (ID: " + w.getId() + ")");
            }
            
            runOnUiThread(() -> {
                // Clear existing wallet items (except add wallet button)
                walletListContainer.removeAllViews();
                
                // Add wallet items dynamically
                for (Wallet wallet : wallets) {
                    addWalletItemToPanel(wallet);
                }
                
                android.util.Log.d("MainActivity", "Wallet items added to panel");
            });
        }).start();
    }

    /**
     * Add a wallet item to the panel
     */
    private void addWalletItemToPanel(Wallet wallet) {
        View walletItemView = getLayoutInflater()
            .inflate(R.layout.wallet_item, walletListContainer, false);
        
        // Find the clickable inner LinearLayout (the one with background)
        LinearLayout clickableArea = (LinearLayout) walletItemView.findViewById(R.id.wallet_clickable_area);
        if (clickableArea == null) {
            // Fallback: try to find the first child LinearLayout
            if (walletItemView instanceof LinearLayout) {
                LinearLayout parent = (LinearLayout) walletItemView;
                if (parent.getChildCount() > 0 && parent.getChildAt(0) instanceof LinearLayout) {
                    clickableArea = (LinearLayout) parent.getChildAt(0);
                }
            }
        }
        
        ImageView icon = walletItemView.findViewById(R.id.wallet_icon);
        TextView name = walletItemView.findViewById(R.id.wallet_name);
        TextView balance = walletItemView.findViewById(R.id.wallet_balance);
        
        name.setText(wallet.getName());
        balance.setText(String.format("%,.0f %s", wallet.getBalance(), wallet.getCurrency()));
        
        // Set icon based on wallet type
        switch (wallet.getType()) {
            case "cash":
                icon.setImageResource(R.drawable.ic_money_bundle);
                break;
            case "bank":
                icon.setImageResource(R.drawable.ic_bankcard);
                break;
            case "virtual":
                icon.setImageResource(R.drawable.ic_virtual_account);
                break;
            default:
                icon.setImageResource(R.drawable.ic_wallet1);
                break;
        }
        
        // Set click listener on the clickable area, not the root
        View finalClickableArea = clickableArea != null ? clickableArea : walletItemView;
        finalClickableArea.setOnClickListener(v -> {
            android.util.Log.d("MainActivity", "Wallet item clicked!");
            hideWalletPanel();
            int oldWalletId = selectedWalletId;
            selectedWalletId = wallet.getId();
            
            android.util.Log.d("MainActivity", "Wallet switched: " + oldWalletId + " -> " + selectedWalletId);
            android.util.Log.d("MainActivity", "Selected wallet: " + wallet.getName());
            
            Toast.makeText(this, "Selected: " + wallet.getName(), Toast.LENGTH_SHORT).show();
            
            // Refresh current fragment with new wallet data
            refreshCurrentFragment();
        });
        
        walletListContainer.addView(walletItemView);
    }
    
    /**
     * Refresh the current fragment to reflect wallet changes
     */
    private void refreshCurrentFragment() {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        
        android.util.Log.d("MainActivity", "Refreshing fragment: " + 
            (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
        
        if (currentFragment instanceof HomeFragment) {
            android.util.Log.d("MainActivity", "Calling HomeFragment.refreshData()");
            ((HomeFragment) currentFragment).refreshData();
        } else if (currentFragment instanceof HistoryFragment) {
            android.util.Log.d("MainActivity", "Calling HistoryFragment.refreshData()");
            ((HistoryFragment) currentFragment).refreshData();
        } else {
            // For other fragments, use detach/attach
            android.util.Log.d("MainActivity", "Using detach/attach for fragment refresh");
            if (currentFragment != null) {
                fragmentManager.beginTransaction()
                    .detach(currentFragment)
                    .attach(currentFragment)
                    .commit();
            }
        }
    }

    private void toggleWalletPanel() {
        if (walletPanel.getVisibility() == View.VISIBLE) {
            hideWalletPanel();
        } else {
            showWalletPanel();
        }
    }

    private void showWalletPanel() {
        walletPanel.setVisibility(View.VISIBLE);
    }

    private void hideWalletPanel() {
        walletPanel.setVisibility(View.GONE);
    }

    private void setupNavigationBar() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navImport = findViewById(R.id.nav_import);
        LinearLayout navAIChat = findViewById(R.id.nav_ai_chat);
        LinearLayout navStatistics = findViewById(R.id.nav_statistics);

        navHome.setOnClickListener(v -> loadFragment(new HomeFragment(), "Home"));
        navHistory.setOnClickListener(v -> loadFragment(new HistoryFragment(), "History"));
        navImport.setOnClickListener(v -> loadFragment(new ImportFragment(), "Import"));
        navAIChat.setOnClickListener(v -> loadFragment(new AIChatFragment(), "AI Chat"));
        navStatistics.setOnClickListener(v -> loadFragment(new StatisticsFragment(), "Statistics"));
    }

    private void loadFragment(Fragment fragment, String title) {
        headerTitle.setText(title);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    /**
     * Load a fragment with back stack support
     * This allows the user to navigate back using the back button
     */
    private void loadFragmentWithBackStack(Fragment fragment, String title) {
        headerTitle.setText(title);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Get the currently selected wallet ID
     */
    public static int getSelectedWalletId() {
        return selectedWalletId;
    }

    /**
     * Set the selected wallet ID
     */
    public static void setSelectedWalletId(int walletId) {
        selectedWalletId = walletId;
    }
}