package com.example.mymoney;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.dao.TransactionDao;
import com.example.mymoney.model.CategoryExpense;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class BudgetFragment extends Fragment {

    private EditText edtTarget, edtTime, edtIncome;
    private TextView tvResult, tvSavingStatus;
    private Button btnCalc, btnStart, btnCheck;
    private TransactionDao transactionDao;

    private double target, months, income;
    private boolean savingStarted = false;

    private static final String PREF_NAME = "budget_prefs";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        edtTarget = view.findViewById(R.id.edt_target_amount);
        edtTime = view.findViewById(R.id.edt_time_range);
        edtIncome = view.findViewById(R.id.edt_income);
        btnCalc = view.findViewById(R.id.btn_calculate_budget);
        btnStart = view.findViewById(R.id.btn_start_saving);
        btnCheck = view.findViewById(R.id.btn_check_saving);
        tvResult = view.findViewById(R.id.tv_budget_result);
        tvSavingStatus = view.findViewById(R.id.tv_saving_status);

        transactionDao = AppDatabase.getInstance(requireContext()).transactionDao();

        loadSavedState(); // ✅ Đọc trạng thái khi mở lại

        btnCalc.setOnClickListener(v ->
                Executors.newSingleThreadExecutor().execute(this::calculateBudget));
        btnStart.setOnClickListener(v -> startSaving());
        btnCheck.setOnClickListener(v ->
                Executors.newSingleThreadExecutor().execute(this::checkSavingProgress));

        return view;
    }

    private void calculateBudget() {
        String targetStr = edtTarget.getText().toString().trim();
        String monthsStr = edtTime.getText().toString().trim();
        String incomeStr = edtIncome.getText().toString().trim();

        if (targetStr.isEmpty() || monthsStr.isEmpty() || incomeStr.isEmpty()) {
            requireActivity().runOnUiThread(() ->
                    tvResult.setText("⚠️ Vui lòng nhập đủ thông tin!"));
            return;
        }

        try {
            target = Double.parseDouble(targetStr);
            months = Double.parseDouble(monthsStr);
            income = Double.parseDouble(incomeStr);

            double totalNeedSavePerMonth = target / months;
            double maxExpensePerMonth = income - totalNeedSavePerMonth;

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -3);
            long startDate = cal.getTimeInMillis();

            List<CategoryExpense> expenses = transactionDao.getExpensesByCategorySince(startDate);
            double totalExpense3M = 0;
            for (CategoryExpense e : expenses) totalExpense3M += e.total;

            DecimalFormat df = new DecimalFormat("#,###");
            StringBuilder result = new StringBuilder();

            if (expenses.isEmpty() || totalExpense3M == 0) {
                result.append("⚠️ Chưa có dữ liệu chi tiêu trong 3 tháng gần nhất để phân tích.");
            } else {
                result.append("🎯 Mục tiêu: tiết kiệm ").append(df.format(target)).append(" VND trong ")
                        .append((int) months).append(" tháng.\n💵 Lương hàng tháng: ")
                        .append(df.format(income)).append(" VND\n📉 Mỗi tháng nên chi tối đa: ")
                        .append(df.format(maxExpensePerMonth)).append(" VND\n\n📊 Gợi ý phân bổ ngân sách:\n");

                for (CategoryExpense e : expenses) {
                    double ratio = e.total / totalExpense3M;
                    double suggested = ratio * maxExpensePerMonth;
                    result.append(" - ").append(e.category)
                            .append(": ≤ ").append(df.format(suggested)).append(" VND\n");
                }
            }

            requireActivity().runOnUiThread(() -> {
                tvResult.setText(result.toString());
                btnStart.setVisibility(View.VISIBLE);
                btnCalc.setVisibility(View.GONE);
            });

        } catch (Exception e) {
            requireActivity().runOnUiThread(() ->
                    tvResult.setText("⚠️ Lỗi khi đọc dữ liệu chi tiêu."));
            e.printStackTrace();
        }
    }

    private void startSaving() {
        if (target <= 0 || months <= 0 || income <= 0) {
            tvSavingStatus.setText("⚠️ Hãy tính toán ngân sách trước khi bắt đầu tiết kiệm!");
            return;
        }

        savingStarted = true;
        tvSavingStatus.setText("✅ Đã bắt đầu tiết kiệm! Hãy cập nhật tiến độ thường xuyên để theo dõi.");

        btnCheck.setVisibility(View.VISIBLE);
        btnStart.setVisibility(View.GONE);
        btnCalc.setVisibility(View.GONE);

        // ✅ Lưu trạng thái
        requireContext().getSharedPreferences(PREF_NAME, 0).edit()
                .putBoolean("savingStarted", true)
                .putFloat("target", (float) target)
                .putFloat("months", (float) months)
                .putFloat("income", (float) income)
                .apply();
    }

    private void checkSavingProgress() {
        if (!savingStarted) {
            requireActivity().runOnUiThread(() ->
                    tvSavingStatus.setText("⚠️ Bạn chưa bắt đầu tiết kiệm!"));
            return;
        }

        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);
            long startDate = cal.getTimeInMillis();

            List<CategoryExpense> expenses = transactionDao.getExpensesByCategorySince(startDate);
            double totalExpense = 0;
            for (CategoryExpense e : expenses) totalExpense += e.total;

            double totalNeedSavePerMonth = target / months;
            double maxExpensePerMonth = income - totalNeedSavePerMonth;
            double savedSoFar = income - totalExpense;
            double progressPercent = (savedSoFar / totalNeedSavePerMonth) * 100;

            DecimalFormat df = new DecimalFormat("#,###");
            StringBuilder status = new StringBuilder();
            status.append("💰 Mục tiêu: ").append(df.format(target)).append(" VND\n")
                    .append("📅 Thời gian: ").append((int) months).append(" tháng\n")
                    .append("💵 Lương: ").append(df.format(income)).append(" VND\n\n")
                    .append("📊 Đã tiết kiệm được: ").append(df.format(savedSoFar)).append(" VND\n")
                    .append("⏱️ Tiến độ: ").append(String.format("%.1f", progressPercent)).append("%\n\n");

            if (totalExpense > maxExpensePerMonth) {
                status.append("⚠️ Bạn đã chi tiêu vượt mức ")
                        .append(df.format(totalExpense - maxExpensePerMonth))
                        .append(" VND so với giới hạn!\n👉 Gợi ý tính toán lại ngân sách hợp lý.");
            } else {
                status.append("✅ Chi tiêu trong giới hạn. Tiếp tục giữ vững nhé!");
            }

            requireActivity().runOnUiThread(() -> {
                tvSavingStatus.setText(status.toString());
                btnCalc.setVisibility(View.VISIBLE);
                btnCalc.setText("Tính toán lại chi tiêu hợp lý");
            });

        } catch (Exception e) {
            requireActivity().runOnUiThread(() ->
                    tvSavingStatus.setText("⚠️ Lỗi khi kiểm tra tiến độ."));
            e.printStackTrace();
        }
    }

    private void loadSavedState() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, 0);
        savingStarted = prefs.getBoolean("savingStarted", false);
        target = prefs.getFloat("target", 0);
        months = prefs.getFloat("months", 0);
        income = prefs.getFloat("income", 0);

        if (savingStarted) {
            btnCheck.setVisibility(View.VISIBLE);
            btnCalc.setVisibility(View.VISIBLE);
            btnCalc.setText("Tính toán lại chi tiêu hợp lý");
            btnStart.setVisibility(View.GONE);
            tvSavingStatus.setText("✅ Tiết kiệm đang được theo dõi!");
        } else {
            btnCheck.setVisibility(View.GONE);
            btnStart.setVisibility(View.GONE);
            tvSavingStatus.setText("Chưa bắt đầu tiết kiệm");
        }
    }
}
