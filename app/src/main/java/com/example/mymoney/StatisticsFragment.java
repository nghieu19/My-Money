package com.example.mymoney;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mymoney.database.AppDatabase;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatisticsFragment extends Fragment {

    private LinearLayout expensesContainer;
    private LinearLayout barChartContainer;
    private TextView tvYear, tvDateRange;
    private PieChart pieChart;

    private long startDate, endDate;
    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        expensesContainer = view.findViewById(R.id.expenses_container);
        barChartContainer = view.findViewById(R.id.bar_chart_container);
        tvYear = view.findViewById(R.id.tv_year);
        tvDateRange = view.findViewById(R.id.tv_date_range);
        pieChart = view.findViewById(R.id.pie_chart);

        // Khởi tạo ngày mặc định: đầu và cuối tháng hiện tại
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        startDate = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate = cal.getTimeInMillis();

        tvDateRange.setText(dateFormat.format(startDate) + " - " + dateFormat.format(endDate));
        tvDateRange.setOnClickListener(v -> showDateRangePicker());

        tvYear.setText(String.valueOf(selectedYear));
        tvYear.setOnClickListener(v -> showYearPicker());

        loadStatistics();
        return view;
    }

    /** Bộ chọn khoảng ngày **/
    private void showDateRangePicker() {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog startPicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar startCal = Calendar.getInstance();
                    startCal.set(year, month, dayOfMonth, 0, 0, 0);
                    startDate = startCal.getTimeInMillis();

                    // Mở DatePicker cho ngày kết thúc
                    DatePickerDialog endPicker = new DatePickerDialog(requireContext(),
                            (view2, year2, month2, day2) -> {
                                Calendar endCal = Calendar.getInstance();
                                endCal.set(year2, month2, day2, 23, 59, 59);
                                endDate = endCal.getTimeInMillis();

                                tvDateRange.setText(dateFormat.format(startDate) + " - " + dateFormat.format(endDate));
                                loadStatistics();

                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                    endPicker.setTitle("Chọn ngày kết thúc");
                    endPicker.show();

                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        startPicker.setTitle("Chọn ngày bắt đầu");
        startPicker.show();
    }

    /** Bộ chọn năm **/
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
                    loadStatistics();
                })
                .show();
    }

    /** Tải thống kê **/
    private void loadStatistics() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            if (db == null || db.transactionDao() == null) return;

            // Lấy dữ liệu theo khoảng ngày (PieChart)
            List<CategoryTotal> categoryTotals = db.transactionDao()
                    .getExpensesByDateRange(startDate, endDate);

            // Lấy dữ liệu theo năm (BarChart)
            Calendar calendar = Calendar.getInstance();
            calendar.set(selectedYear, Calendar.JANUARY, 1, 0, 0, 0);
            long startOfYear = calendar.getTimeInMillis();
            calendar.set(selectedYear, Calendar.DECEMBER, 31, 23, 59, 59);
            long endOfYear = calendar.getTimeInMillis();

            List<MonthTotal> monthlyTotals =
                    db.transactionDao().getMonthlyExpensesByYear(startOfYear, endOfYear);

            requireActivity().runOnUiThread(() -> {
                displayPieChart(categoryTotals);
                displayTopExpenses(categoryTotals);
                displayBarChart(monthlyTotals);
            });
        }).start();
    }

    /** Hiển thị biểu đồ tròn **/
    /** Hiển thị biểu đồ tròn (full hình, không chữ giữa, legend giữa) **/
    /** Hiển thị biểu đồ tròn full hình, legend giữa có khoảng cách **/
    /** Hiển thị biểu đồ tròn bên trái + chú thích bên phải **/
    private void displayPieChart(List<CategoryTotal> data) {
        pieChart.clear();

        if (data == null || data.isEmpty()) {
            pieChart.setNoDataText("Không có dữ liệu trong khoảng này");
            pieChart.invalidate();
            return;
        }

        // 🔹 Chuẩn bị dữ liệu
        List<PieEntry> entries = new ArrayList<>();
        for (CategoryTotal item : data) {
            entries.add(new PieEntry((float) item.total, item.category));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{
                Color.parseColor("#4CAF50"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#E91E63"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#009688"),
                Color.parseColor("#CDDC39")
        });
        dataSet.setValueTextSize(0f);
        dataSet.setValueTextColor(Color.TRANSPARENT); // Ẩn text trên lát

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        // 🔹 Cấu hình biểu đồ
        pieChart.setDrawHoleEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(false);
        pieChart.setCenterText(null);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.getLegend().setEnabled(false); // 🟢 Tắt legend mặc định

        pieChart.animateY(1000);
        pieChart.invalidate();

        // 🔹 Hiển thị legend tùy chỉnh bên phải
        LinearLayout legendLayout = getView().findViewById(R.id.legend_container);
        if (legendLayout != null) {
            legendLayout.removeAllViews();

            for (int i = 0; i < data.size(); i++) {
                CategoryTotal item = data.get(i);

                LinearLayout itemLayout = new LinearLayout(getContext());
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setPadding(8, 8, 8, 8);
                itemLayout.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

                // Chấm tròn màu
                View colorDot = new View(getContext());
                int size = (int) (12 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(size, size);
                dotParams.setMarginEnd(12);
                colorDot.setLayoutParams(dotParams);
                colorDot.setBackgroundResource(R.drawable.legend_dot_shape);
                colorDot.getBackground().setTint(dataSet.getColors().get(i % dataSet.getColors().size()));

                // Tên danh mục
                TextView label = new TextView(getContext());
                label.setText(item.category);
                label.setTextSize(14f);
                label.setTextColor(Color.parseColor("#444444"));

                itemLayout.addView(colorDot);
                itemLayout.addView(label);
                legendLayout.addView(itemLayout);
            }
        }
    }



    /** Hiển thị danh sách top expenses (có icon) **/
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
            ImageView imgIcon = row.findViewById(R.id.imgCategoryIcon);

            tvCategory.setText(item.category);
            double percent = (item.total / totalAll) * 100;
            tvPercent.setText(String.format(Locale.getDefault(), "%.1f%%", percent));
            tvAmount.setText(String.format(Locale.getDefault(), "%,.0f VND", item.total));

            // 🟢 Gán icon phù hợp
            String cat = item.category.toLowerCase(Locale.ROOT);
            if (cat.contains("food") || cat.contains("ăn") || cat.contains("drink"))
                imgIcon.setImageResource(R.drawable.ic_food);
            else if (cat.contains("home") || cat.contains("house") || cat.contains("rent"))
                imgIcon.setImageResource(R.drawable.ic_home);
            else if (cat.contains("travel") || cat.contains("transport") || cat.contains("car"))
                imgIcon.setImageResource(R.drawable.ic_travel);
            else if (cat.contains("medicine") || cat.contains("health") || cat.contains("hospital"))
                imgIcon.setImageResource(R.drawable.ic_medicine);
            else if (cat.contains("entertainment") || cat.contains("movie") || cat.contains("game"))
                imgIcon.setImageResource(R.drawable.ic_entertainment);
            else if (cat.contains("gift") || cat.contains("love") || cat.contains("relationship"))
                imgIcon.setImageResource(R.drawable.ic_love);
            else
                imgIcon.setImageResource(R.drawable.ic_other);

            expensesContainer.addView(row);
        }
    }

    /** Hiển thị biểu đồ cột **/
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

        double maxTotal = 0;
        for (MonthTotal item : monthlyTotals) {
            if (item.total > maxTotal) maxTotal = item.total;
        }

        int[] colors = {
                0xFF4CAF50, 0xFF2196F3, 0xFFFFC107, 0xFFFF5722,
                0xFF9C27B0, 0xFFE91E63, 0xFF3F51B5, 0xFF009688,
                0xFFCDDC39, 0xFFFF9800, 0xFF795548, 0xFF607D8B
        };

        int colorIndex = 0;

        for (MonthTotal item : monthlyTotals) {
            View monthBar = inflater.inflate(R.layout.item_month_bar, barChartContainer, false);
            TextView tvValue = monthBar.findViewById(R.id.tvValue);
            View bar = monthBar.findViewById(R.id.bar);
            TextView tvMonth = monthBar.findViewById(R.id.tvMonth);

            tvValue.setText(String.format(Locale.getDefault(), "%.1fM", item.total / 1_000_000.0));
            tvMonth.setText(getMonthName(item.month));

            int maxHeightPx = 250;
            float ratio = (float) (item.total / maxTotal);
            int height = (int) (ratio * maxHeightPx);
            bar.getLayoutParams().height = Math.max(height, 20);

            bar.setBackgroundColor(colors[colorIndex % colors.length]);
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
