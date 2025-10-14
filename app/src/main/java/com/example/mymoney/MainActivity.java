package com.example.mymoney;

import android.content.Intent;
import android.content.SharedPreferences;
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

    private static final String PREFS_NAME = "MyMoneyPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private static int selectedWalletId = -1;
    private static int currentUserId = 1;

    private FragmentManager fragmentManager;
    private TextView headerTitle;
    private CardView walletPanel;
    private CardView settingsPanel;
    private ImageView btnWallet;
    private ImageView btnSettings;
    private LinearLayout walletListContainer;

    private LinearLayout settingsLogin;
    private LinearLayout settingsLogout;

    private LinearLayout headerLayout;
    private View headerDivider;
    private View bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.onAttach(this); // ✅ hỗ trợ ngôn ngữ
        ThemeUtils.applyTheme(this); // ✅ áp dụng theme hiện tại
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
        settingsPanel = findViewById(R.id.settings_panel);
        btnWallet = findViewById(R.id.btn_wallet);
        btnSettings = findViewById(R.id.btn_settings);
        settingsLogin = findViewById(R.id.settings_login);
        settingsLogout = findViewById(R.id.settings_logout);
        headerLayout = findViewById(R.id.header_layout);
        headerDivider = findViewById(R.id.header_divider);
        bottomNavigation = findViewById(R.id.bottom_navigation);



        updateCurrentUserId();
        setupWalletButton();
        setupSettingsButton();
        updateSettingsButtonsVisibility();
        setupNavigationBar();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "Home");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCurrentUserId();
        updateSettingsButtonsVisibility();
        loadWalletsFromDatabase();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::refreshCurrentFragment, 300);
    }

    private void updateCurrentUserId() {
        currentUserId = getLoggedInUserId();
    }

    // ================= Wallet =================
    private void setupWalletButton() {
        btnWallet.setOnClickListener(v -> {
            loadWalletsFromDatabase();
            hideSettingsPanel();
            toggleWalletPanel();
        });

        LinearLayout addWalletItem = findViewById(R.id.add_wallet_item);
        addWalletItem.setOnClickListener(v -> {
            hideWalletPanel();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new NewWalletFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });
    }

    private void setupSettingsButton() {
        btnSettings.setOnClickListener(v -> {
            hideWalletPanel();
            updateSettingsButtonsVisibility();
            toggleSettingsPanel();
        });

        // 🔹 Account settings
        LinearLayout settingsAccount = findViewById(R.id.settings_account);
        settingsAccount.setOnClickListener(v -> {
            hideSettingsPanel();
            Intent intent = new Intent(MainActivity.this, AccountActivity.class);
            startActivity(intent);
        });

        // 🌗 Theme switch
        LinearLayout themesLayout = findViewById(R.id.settings_themes);
        ImageView imgThemeIcon = findViewById(R.id.imgThemeIcon); // icon mặt trăng / mặt trời

        // Hiển thị icon đúng khi mở app
        if (ThemeUtils.isDarkMode(this)) {
            imgThemeIcon.setImageResource(R.drawable.ic_moon);
        } else {
            imgThemeIcon.setImageResource(R.drawable.ic_sun);
        }

        // Khi người dùng bấm "Themes"
        themesLayout.setOnClickListener(v -> {
            boolean isDark = ThemeUtils.isDarkMode(this);
            ThemeUtils.toggleTheme(this);
            imgThemeIcon.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);
            recreate();
        });

        // 🌍 Language settings
        LinearLayout settingsLanguage = findViewById(R.id.settings_language);
        settingsLanguage.setOnClickListener(v -> {
            hideSettingsPanel();
            String[] languages = {"English", "Tiếng Việt"};
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Choose Language")
                    .setItems(languages, (dialog, which) -> {
                        String langCode = (which == 0) ? "en" : "vi";
                        LocaleHelper.setLocale(this, langCode);
                        recreate();
                    })
                    .show();
        });

        // 🔐 Login button
        settingsLogin.setOnClickListener(v -> {
            hideSettingsPanel();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // 🔓 Logout button
        settingsLogout.setOnClickListener(v -> {
            hideSettingsPanel();
            performLogout();
        });
    }

    private void updateSettingsButtonsVisibility() {
        if (isLoggedIn()) {
            settingsLogin.setVisibility(View.GONE);
            settingsLogout.setVisibility(View.VISIBLE);
        } else {
            settingsLogin.setVisibility(View.VISIBLE);
            settingsLogout.setVisibility(View.GONE);
        }
    }

    private boolean isLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private int getLoggedInUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, 1);
    }

    private void performLogout() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USERNAME)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();

        currentUserId = 1;
        selectedWalletId = -1;
        updateSettingsButtonsVisibility();
        if (walletListContainer != null) walletListContainer.removeAllViews();
        loadWalletsFromDatabase();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::refreshCurrentFragment, 200);
        Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
    }

    // ================= Wallet loading =================
    private void loadWalletsFromDatabase() {
        walletListContainer = findViewById(R.id.wallet_list_container);
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            int userId = getLoggedInUserId();
            List<Wallet> wallets = db.walletDao().getActiveWalletsByUserId(userId);
            int newSelectedWalletId = wallets.isEmpty() ? -1 : wallets.get(0).getId();
            runOnUiThread(() -> {
                walletListContainer.removeAllViews();
                for (Wallet wallet : wallets) {
                    addWalletItemToPanel(wallet);
                }
                selectedWalletId = newSelectedWalletId;
                refreshCurrentFragment();
            });
        }).start();
    }

    private void addWalletItemToPanel(Wallet wallet) {
        View walletItemView = getLayoutInflater().inflate(R.layout.wallet_item, walletListContainer, false);
        LinearLayout clickableArea = walletItemView.findViewById(R.id.wallet_clickable_area);
        ImageView icon = walletItemView.findViewById(R.id.wallet_icon);
        TextView name = walletItemView.findViewById(R.id.wallet_name);
        TextView balance = walletItemView.findViewById(R.id.wallet_balance);

        name.setText(wallet.getName());
        balance.setText(String.format("%,.0f %s", wallet.getBalance(), wallet.getCurrency()));

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

        clickableArea.setOnClickListener(v -> {
            hideWalletPanel();
            selectedWalletId = wallet.getId();
            Toast.makeText(this, "Chọn ví: " + wallet.getName(), Toast.LENGTH_SHORT).show();
            refreshCurrentFragment();
        });

        walletListContainer.addView(walletItemView);
    }

    private void refreshCurrentFragment() {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment != null) {
            fragmentManager.beginTransaction()
                    .detach(currentFragment)
                    .attach(currentFragment)
                    .commitAllowingStateLoss();
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
        showHeaderAndFooter();
        headerTitle.setText(title);
        fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void hideHeaderAndFooter() {
        if (headerLayout != null) headerLayout.setVisibility(View.GONE);
        if (headerDivider != null) headerDivider.setVisibility(View.GONE);
        if (bottomNavigation != null) bottomNavigation.setVisibility(View.GONE);
    }

    private void showHeaderAndFooter() {
        if (headerLayout != null) headerLayout.setVisibility(View.VISIBLE);
        if (headerDivider != null) headerDivider.setVisibility(View.VISIBLE);
        if (bottomNavigation != null) bottomNavigation.setVisibility(View.VISIBLE);
    }

    public void hideMainHeaderAndFooter() {
        hideHeaderAndFooter();
    }

    public void showMainHeaderAndFooter() {
        showHeaderAndFooter();
    }

    public static int getSelectedWalletId() {
        return selectedWalletId;
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    private void toggleWalletPanel() {
        walletPanel.setVisibility(walletPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void hideWalletPanel() {
        walletPanel.setVisibility(View.GONE);
    }

    private void toggleSettingsPanel() {
        settingsPanel.setVisibility(settingsPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void hideSettingsPanel() {
        settingsPanel.setVisibility(View.GONE);
    }

    public static void setSelectedWalletId(int walletId) {
        selectedWalletId = walletId;
        android.util.Log.d("MainActivity", "Wallet ID set to: " + walletId);
    }

}
