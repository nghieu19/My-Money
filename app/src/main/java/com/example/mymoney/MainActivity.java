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

public class MainActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private TextView headerTitle;
    private CardView walletPanel;
    private ImageView btnWallet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_container);
        
        // Apply window insets to avoid status bar overlap
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

        // Set up wallet button
        setupWalletButton();

        // Set up navigation bar click listeners
        setupNavigationBar();

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "Home");
        }
    }

    private void setupWalletButton() {
        btnWallet.setOnClickListener(v -> toggleWalletPanel());

        LinearLayout wallet1Item = findViewById(R.id.wallet_1_item);
        LinearLayout addWalletItem = findViewById(R.id.add_wallet_item);

        wallet1Item.setOnClickListener(v -> {
            hideWalletPanel();
        });

        addWalletItem.setOnClickListener(v -> {
            hideWalletPanel();
        });
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
}