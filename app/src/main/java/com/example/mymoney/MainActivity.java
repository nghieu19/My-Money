package com.example.mymoney;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private TextView headerTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_container);
        
        fragmentManager = getSupportFragmentManager();
        headerTitle = findViewById(R.id.header_title);

        // Set up navigation bar click listeners
        setupNavigationBar();

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "Home");
        }
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
        // Update header title
        headerTitle.setText(title);

        // Replace fragment
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}