package com.example.mymoney;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mymoney.database.AppDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StatisticsFragment extends Fragment {

    private LinearLayout expensesContainer; // Top expenses
    private LinearLayout barChartContainer; // Monthly bar chart
    private TextView tvYear;
    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private int lastUserId = -1;
    private int lastWalletId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        expensesContainer = view.findViewById(R.id.expenses_container);
        barChartContainer = view.findViewById(R.id.bar_chart_container);
        tvYear = view.findViewById(R.id.tv_year);

        tvYear.setText(String.valueOf(selectedYear));
        tvYear.setOnClickListener(v -> showYearPicker());

        loadStatistics(); // tải dữ liệu năm hiện tại
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Check if user or wallet has changed
        int currentUserId = MainActivity.getCurrentUserId();
        int currentWalletId = MainActivity.getSelectedWalletId();
        
        if (currentUserId != lastUserId || currentWalletId != lastWalletId) {
            lastUserId = currentUserId;
            lastWalletId = currentWalletId;
            refreshData();
        }
    }
    
    /**
     * Public method to refresh statistics data
     * Called when wallet is changed or user logs in/out
     */
    public void refreshData() {
        loadStatistics();
    }

    /** Hiển thị hộp chọn năm **/
    private void showYearPicker() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int y = currentYear; y >= currentYear - 5; y--) {
            years.add(String.valueOf(y));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn năm thống kê")
                .setItems(years.toArray(new String[0]), (dialog, which) -> {
                    selectedYear = Integer.parseInt(years.get(which));
                    tvYear.setText(String.valueOf(selectedYear));
                    loadStatistics(); // reload dữ liệu theo năm
                })
                .show();
    }

    /** Load thống kê theo năm **/
    private void loadStatistics() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            if (db == null || db.transactionDao() == null) return;

            long startOfYear, endOfYear;
            Calendar calendar = Calendar.getInstance();
            calendar.set(selectedYear, Calendar.JANUARY, 1, 0, 0, 0);
            startOfYear = calendar.getTimeInMillis();
            calendar.set(selectedYear, Calendar.DECEMBER, 31, 23, 59, 59);
            endOfYear = calendar.getTimeInMillis();

            int userId = MainActivity.getCurrentUserId();
            int walletId = MainActivity.getSelectedWalletId(); // 🔹 Get current wallet
            
            // 🔹 Pass both userId and walletId for wallet-specific statistics
            List<CategoryTotal> topExpenses =
                    db.transactionDao().getTopExpensesByYear(userId, walletId, startOfYear, endOfYear);
            List<MonthTotal> monthlyTotals =
                    db.transactionDao().getMonthlyExpensesByYear(userId, walletId, startOfYear, endOfYear);

            requireActivity().runOnUiThread(() -> {
                displayTopExpenses(topExpenses);
                displayBarChart(monthlyTotals);
            });
        }).start();
    }

    /** Hiển thị danh sách top expenses **/
    private void displayTopExpenses(List<CategoryTotal> topExpenses) {
        expensesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (topExpenses == null || topExpenses.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Chưa có dữ liệu chi tiêu.");
            empty.setPadding(16, 16, 16, 16);
            expensesContainer.addView(empty);
            return;
        }

        double totalAll = 0;
        for (CategoryTotal item : topExpenses) totalAll += item.total;

        for (CategoryTotal item : topExpenses) {
            View row = inflater.inflate(R.layout.item_top_expense, expensesContainer, false);
            TextView tvCategory = row.findViewById(R.id.tvCategory);
            TextView tvPercent = row.findViewById(R.id.tvPercent);
            TextView tvAmount = row.findViewById(R.id.tvAmount);

            tvCategory.setText(item.category);
            double percent = (item.total / totalAll) * 100;
            tvPercent.setText(String.format("%.1f%%", percent));
            tvAmount.setText(String.format("%,.0f VND", item.total));

            expensesContainer.addView(row);
        }
    }

    /** Hiển thị biểu đồ cột tháng **/
    private void displayBarChart(List<MonthTotal> monthlyTotals) {
        if (barChartContainer == null) return;
        barChartContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        if (monthlyTotals == null || monthlyTotals.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Chưa có dữ liệu theo tháng.");
            empty.setPadding(16, 16, 16, 16);
            barChartContainer.addView(empty);
            return;
        }

        // 🔹 Tìm giá trị lớn nhất để chuẩn hóa chiều cao
        double maxTotal = 0;
        for (MonthTotal item : monthlyTotals) {
            if (item.total > maxTotal) maxTotal = item.total;
        }

        // 🔹 Mảng màu cho 12 tháng
        int[] colors = {
                0xFF4CAF50, // Jan - xanh lá
                0xFF2196F3, // Feb - xanh dương
                0xFFFFC107, // Mar - vàng
                0xFFFF5722, // Apr - cam
                0xFF9C27B0, // May - tím
                0xFFE91E63, // Jun - hồng
                0xFF3F51B5, // Jul - xanh tím
                0xFF009688, // Aug - teal
                0xFFCDDC39, // Sep - vàng xanh
                0xFFFF9800, // Oct - cam nhạt
                0xFF795548, // Nov - nâu
                0xFF607D8B  // Dec - xám xanh
        };

        int colorIndex = 0;

        for (MonthTotal item : monthlyTotals) {
            View monthBar = inflater.inflate(R.layout.item_month_bar, barChartContainer, false);

            TextView tvValue = monthBar.findViewById(R.id.tvValue);
            View bar = monthBar.findViewById(R.id.bar);
            TextView tvMonth = monthBar.findViewById(R.id.tvMonth);

            tvValue.setText(String.format("%.1fM", item.total / 1_000_000.0));
            tvMonth.setText(getMonthName(item.month));

            // 🔹 Chiều cao cột theo tỉ lệ
            int maxHeightPx = 250;
            float ratio = (float) (item.total / maxTotal);
            int height = (int) (ratio * maxHeightPx);
            bar.getLayoutParams().height = Math.max(height, 20);

            // 🔹 Mỗi tháng 1 màu
            int color = colors[colorIndex % colors.length];
            bar.setBackgroundColor(color);
            colorIndex++;

            bar.requestLayout();
            barChartContainer.addView(monthBar);
        }
    }

    private String getMonthName(String month) {
        switch (month) {
            case "01": return "Jan";
            case "02": return "Feb";
            case "03": return "Mar";
            case "04": return "Apr";
            case "05": return "May";
            case "06": return "Jun";
            case "07": return "Jul";
            case "08": return "Aug";
            case "09": return "Sep";
            case "10": return "Oct";
            case "11": return "Nov";
            case "12": return "Dec";
            default: return month;
        }
    }
}
