package com.example.mymoney;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.dao.TransactionDao;
import com.example.mymoney.model.CategoryExpense;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class BudgetFragment extends Fragment {

    private EditText edtTarget, edtTime, edtIncome, edtSavedAmount;
    private TextView tvResult, tvSavingStatus, tvStartDate;
    private Button btnCalc, btnStart, btnCheck, btnEnd, btnAddSaved;
    private TransactionDao transactionDao;

    private double target, months, income;
    private double savingPerMonth, maxExpensePerMonth;
    private double savedManual = 0;
    private boolean savingStarted = false;
    private long savingStart = 0L;

    private static final String PREF_NAME = "budget_prefs";
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final DecimalFormat df = new DecimalFormat("#,###");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        // ====== Ánh xạ view ======
        edtTarget = view.findViewById(R.id.edt_target_amount);
        edtTime = view.findViewById(R.id.edt_time_range);
        edtIncome = view.findViewById(R.id.edt_income);
        edtSavedAmount = view.findViewById(R.id.edt_saved_amount);
        tvResult = view.findViewById(R.id.tv_budget_result);
        tvSavingStatus = view.findViewById(R.id.tv_saving_status);
        tvStartDate = view.findViewById(R.id.tv_start_date);
        btnCalc = view.findViewById(R.id.btn_calculate_budget);
        btnStart = view.findViewById(R.id.btn_start_saving);
        btnCheck = view.findViewById(R.id.btn_check_saving);
        btnEnd = view.findViewById(R.id.btn_end_saving);
        btnAddSaved = view.findViewById(R.id.btn_add_saved);

        transactionDao = AppDatabase.getInstance(requireContext()).transactionDao();

        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

// Gọi load trước để lấy giá trị savingStarted đúng từ prefs
        loadSavedState();

// Nếu từng bắt đầu tiết kiệm → ép giao diện hiển thị đang tiết kiệm
        boolean isSaving = prefs.getBoolean("isSaving", savingStarted);
        updateSavingUI(isSaving);


        // ====== Gán sự kiện nút ======
        btnCalc.setOnClickListener(v -> Executors.newSingleThreadExecutor().execute(() -> {
            if (savingStarted) recalculateBudgetBasedOnProgress();
            else calculateBudget();
        }));

        btnStart.setOnClickListener(v -> {
            startSaving();
            prefs.edit().putBoolean("isSaving", true).apply();
            requireActivity().runOnUiThread(() -> updateSavingUI(true));
        });

        btnCheck.setOnClickListener(v ->
                Executors.newSingleThreadExecutor().execute(this::checkSavingProgress));

        btnEnd.setOnClickListener(v -> Executors.newSingleThreadExecutor().execute(() -> {
            endSaving();
            prefs.edit().putBoolean("isSaving", false).apply();
            requireActivity().runOnUiThread(() -> updateSavingUI(false));
        }));

        btnAddSaved.setOnClickListener(v -> addManualSaving());

        return view;
    }

    // ======== Ẩn/hiện UI theo trạng thái ========
    private void updateSavingUI(boolean isSaving) {
        if (!isSaving) {
            // 💰 Chưa bắt đầu tiết kiệm
            edtTarget.setVisibility(View.VISIBLE);
            edtTime.setVisibility(View.VISIBLE);
            edtIncome.setVisibility(View.VISIBLE);
            btnCalc.setVisibility(View.VISIBLE);
            btnStart.setVisibility(View.VISIBLE);

            btnCheck.setVisibility(View.GONE);
            edtSavedAmount.setVisibility(View.GONE);
            btnAddSaved.setVisibility(View.GONE);
            btnEnd.setVisibility(View.GONE);
        } else {
            // 💗 Đang tiết kiệm
            edtTarget.setVisibility(View.GONE);
            edtTime.setVisibility(View.GONE);
            edtIncome.setVisibility(View.GONE);
            btnCalc.setVisibility(View.GONE);
            btnStart.setVisibility(View.GONE);

            btnCheck.setVisibility(View.VISIBLE);
            edtSavedAmount.setVisibility(View.VISIBLE);
            btnAddSaved.setVisibility(View.VISIBLE);
            btnEnd.setVisibility(View.VISIBLE);
        }
    }

    // ======== TÍNH TOÁN NGÂN SÁCH BAN ĐẦU ========
    private void calculateBudget() {
        String targetStr = edtTarget.getText().toString().trim();
        String monthsStr = edtTime.getText().toString().trim();
        String incomeStr = edtIncome.getText().toString().trim();

        if (TextUtils.isEmpty(targetStr) || TextUtils.isEmpty(monthsStr) || TextUtils.isEmpty(incomeStr)) {
            requireActivity().runOnUiThread(() ->
                    tvResult.setText("⚠️ Vui lòng nhập đủ: mục tiêu, số tháng, lương/tháng!"));
            return;
        }

        try {
            target = Double.parseDouble(targetStr);
            months = Double.parseDouble(monthsStr);
            income = Double.parseDouble(incomeStr);

            savingPerMonth = target / months;
            maxExpensePerMonth = income - savingPerMonth;

            if (maxExpensePerMonth < 0) {
                requireActivity().runOnUiThread(() ->
                        tvResult.setText("⚠️ Lương/tháng nhỏ hơn số tiền cần tiết kiệm/tháng. Hãy tăng thời gian hoặc giảm mục tiêu."));
                return;
            }

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -3);
            long startDate = cal.getTimeInMillis();

            List<CategoryExpense> expenses = transactionDao.getExpensesByCategorySince(startDate);
            double totalExpense3M = 0;
            for (CategoryExpense e : expenses) totalExpense3M += e.total;

            StringBuilder result = new StringBuilder();
            StringBuilder budgetPlan = new StringBuilder();

            result.append("🎯 Mục tiêu: ").append(df.format(target)).append(" VND trong ").append((int) months).append(" tháng\n")
                    .append("💵 Lương/tháng: ").append(df.format(income)).append(" VND\n")
                    .append("🏦 Cần tiết kiệm/tháng: ").append(df.format(savingPerMonth)).append(" VND\n")
                    .append("📉 Chi tối đa/tháng: ").append(df.format(maxExpensePerMonth)).append(" VND\n\n");

            if (expenses.isEmpty() || totalExpense3M == 0) {
                result.append("⚠️ Chưa có dữ liệu chi tiêu để gợi ý phân bổ theo danh mục.");
            } else {
                result.append("📊 Gợi ý phân bổ ngân sách/tháng (theo tỷ lệ 3 tháng gần nhất):\n");
                for (CategoryExpense e : expenses) {
                    double ratio = e.total / totalExpense3M;
                    double suggested = ratio * maxExpensePerMonth;
                    result.append(" - ").append(e.category).append(": ≤ ").append(df.format(suggested)).append(" VND\n");
                    budgetPlan.append(e.category).append("=").append(suggested).append(";");
                }
            }

            SharedPreferences.Editor ed = requireContext().getSharedPreferences(PREF_NAME, 0).edit();
            ed.putString("categoryBudgetPlan", budgetPlan.toString());
            ed.apply();

            requireActivity().runOnUiThread(() -> {
                tvResult.setText(result.toString());
                btnStart.setVisibility(View.VISIBLE);
                btnCalc.setVisibility(View.GONE);
            });

        } catch (Exception e) {
            requireActivity().runOnUiThread(() ->
                    tvResult.setText("⚠️ Lỗi khi tính toán ngân sách."));
            e.printStackTrace();
        }
    }

    // ======== LƯU TIẾT KIỆM THỦ CÔNG ========
    private void addManualSaving() {
        String input = edtSavedAmount.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            Toast.makeText(requireContext(), "Nhập số tiền muốn lưu!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double added = Double.parseDouble(input);
            if (added <= 0) {
                Toast.makeText(requireContext(), "Số tiền phải > 0", Toast.LENGTH_SHORT).show();
                return;
            }
            savedManual += added;
            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, 0);
            prefs.edit().putFloat("savedManual", (float) savedManual).apply();

            edtSavedAmount.setText("");
            tvSavingStatus.setText("✅ Đã cộng thêm " + df.format(added)
                    + " VND. Tổng đã tiết kiệm: " + df.format(savedManual) + " VND");
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Giá trị không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    // ======== KIỂM TRA TIẾN ĐỘ ========
    private void checkSavingProgress() {
        if (!savingStarted || savingStart == 0L) {
            requireActivity().runOnUiThread(() ->
                    tvSavingStatus.setText("⚠️ Bạn chưa bắt đầu tiết kiệm!"));
            return;
        }

        try {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, 0);
            savedManual = prefs.getFloat("savedManual", 0);
            savingPerMonth = prefs.getFloat("savingPerMonth", (float) (target / Math.max(1, months)));
            maxExpensePerMonth = prefs.getFloat("maxExpensePerMonth", (float) (income - savingPerMonth));

            long now = System.currentTimeMillis();
            long elapsedDays = daysBetween(savingStart, now);
            long elapsedMonths = elapsedDays / 30;
            long remainingDays = elapsedDays % 30;

            double totalExpense = transactionDao.getTotalExpenseSince(savingStart);
            double allowedExpenseSoFar = maxExpensePerMonth * (elapsedDays / 30.0);

            long monthStart = Math.max(monthStartNow(), savingStart);
            List<CategoryExpense> expenses = transactionDao.getExpensesByCategorySince(monthStart);

            String plan = prefs.getString("categoryBudgetPlan", "");
            java.util.Map<String, Double> planMap = new java.util.HashMap<>();
            for (String entry : plan.split(";")) {
                if (entry.contains("=")) {
                    String[] parts = entry.split("=");
                    try {
                        planMap.put(parts[0], Double.parseDouble(parts[1]));
                    } catch (Exception ignored) {}
                }
            }

            boolean anyOver = false;
            StringBuilder status = new StringBuilder();

            status.append("💰 Mục tiêu: ").append(df.format(target)).append(" VND | ⏳ ")
                    .append((int) months).append(" tháng\n📆 Bắt đầu: ")
                    .append(sdf.format(new Date(savingStart))).append("\n⌛ ");

            // ✅ Hiển thị thời gian đã qua
            if (elapsedDays == 0) {
                status.append("Hôm nay bắt đầu kế hoạch tiết kiệm!\n\n");
            } else {
                status.append("Đã qua: ");
                if (elapsedMonths > 0)
                    status.append(elapsedMonths).append(" tháng ");
                status.append(remainingDays).append(" ngày\n\n");
            }

            // ✅ Hiển thị chi tiêu theo danh mục
            if (!expenses.isEmpty()) {
                status.append("📂 Chi tiêu theo danh mục:\n");
                for (CategoryExpense e : expenses) {
                    double planned = planMap.getOrDefault(e.category, 0.0);
                    status.append("   • ").append(e.category)
                            .append(": ").append(df.format(e.total)).append(" VND");
                    if (planned > 0 && e.total > planned) {
                        status.append(" ⚠️ (vượt ").append(df.format(e.total - planned)).append(")");
                        anyOver = true;
                    }
                    status.append("\n");
                }
            }

            if (anyOver) {
                status.append("\n⚠️ Một số danh mục đã vượt chỉ tiêu! Hãy điều chỉnh chi tiêu hợp lý.\n");
            } else {
                status.append("\n✅ Chi tiêu trong giới hạn kế hoạch. Tiếp tục giữ vững nhé!");
            }

            requireActivity().runOnUiThread(() -> tvSavingStatus.setText(status.toString()));

        } catch (Exception e) {
            requireActivity().runOnUiThread(() ->
                    tvSavingStatus.setText("⚠️ Lỗi khi kiểm tra tiến độ."));
            e.printStackTrace();
        }
    }

    // ======== HÀM PHỤ ========
    private long daysBetween(long start, long end) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(start);
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(end);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        endCal.set(Calendar.HOUR_OF_DAY, 0);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);
        return (endCal.getTimeInMillis() - startCal.getTimeInMillis()) / (1000 * 60 * 60 * 24);
    }

    private long monthStartNow() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private void loadSavedState() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, 0);
        savingStarted = prefs.getBoolean("savingStarted", false);
        target = prefs.getFloat("target", 0);
        months = prefs.getFloat("months", 0);
        income = prefs.getFloat("income", 0);
        savingStart = prefs.getLong("savingStart", 0L);

        if (savingStarted) {
            btnCalc.setText("Tính toán lại chi tiêu hợp lý");
            tvSavingStatus.setText("✅ Tiết kiệm đang được theo dõi!");

            // 🩷 Hiển thị ngày bắt đầu nếu đã lưu
            if (savingStart > 0) {
                tvStartDate.setText("📅 Ngày bắt đầu: " + sdf.format(new Date(savingStart)));
                tvStartDate.setVisibility(View.VISIBLE);
            } else {
                tvStartDate.setText("📅 Ngày bắt đầu: --/--/----");
                tvStartDate.setVisibility(View.VISIBLE);
            }

            edtTarget.setVisibility(View.GONE);
            edtTime.setVisibility(View.GONE);
            edtIncome.setVisibility(View.GONE);
        } else {
            tvStartDate.setText("📅 Ngày bắt đầu: --/--/----");
            tvStartDate.setVisibility(View.VISIBLE);
            tvSavingStatus.setText("Chưa bắt đầu tiết kiệm");
        }
    }


    // ======== BẮT ĐẦU TIẾT KIỆM ========
    private void startSaving() {
        if (target <= 0 || months <= 0 || income <= 0) {
            tvSavingStatus.setText("⚠️ Hãy tính toán ngân sách trước khi bắt đầu tiết kiệm!");
            return;
        }

        savingStarted = true;
        savingStart = System.currentTimeMillis();
        savedManual = 0;

        // Lưu tất cả dữ liệu trước khi cập nhật UI
        SharedPreferences.Editor ed = requireContext().getSharedPreferences(PREF_NAME, 0).edit();
        ed.putBoolean("savingStarted", true);
        ed.putBoolean("isSaving", true);
        ed.putFloat("target", (float) target);
        ed.putFloat("months", (float) months);
        ed.putFloat("income", (float) income);
        ed.putFloat("savingPerMonth", (float) savingPerMonth);
        ed.putFloat("maxExpensePerMonth", (float) maxExpensePerMonth);
        ed.putFloat("savedManual", 0f);
        ed.putLong("savingStart", savingStart);
        ed.apply();

        // Cập nhật giao diện sau khi đã lưu trạng thái
        requireActivity().runOnUiThread(() -> {
            tvStartDate.setText("Ngày bắt đầu: " + sdf.format(new Date(savingStart)));
            tvStartDate.setVisibility(View.VISIBLE);
            tvSavingStatus.setText("✅ Đã bắt đầu tiết kiệm! Hãy nhập khoản tiết kiệm thực tế và cập nhật tiến độ.");
            updateSavingUI(true);
        });
    }

    // ======== KẾT THÚC TIẾT KIỆM ========
    private void endSaving() {
        if (!savingStarted) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, 0);
        prefs.edit().clear().apply();
        requireActivity().runOnUiThread(() -> {
            tvSavingStatus.setText("🏁 Đã kết thúc kế hoạch tiết kiệm!");
            btnCalc.setVisibility(View.VISIBLE);
            btnStart.setVisibility(View.GONE);
            btnCheck.setVisibility(View.GONE);
            btnEnd.setVisibility(View.GONE);
        });
        savingStarted = false;
        savingStart = 0L;
        savedManual = 0;
    }
    private void recalculateBudgetBasedOnProgress() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, 0);
        double targetSaved = prefs.getFloat("target", 0);
        double monthsSaved = prefs.getFloat("months", 0);
        double incomeSaved = prefs.getFloat("income", 0);
        double savedManualNow = prefs.getFloat("savedManual", 0);
        long startTime = prefs.getLong("savingStart", 0L);

        if (targetSaved <= 0 || monthsSaved <= 0 || incomeSaved <= 0 || startTime == 0L) {
            requireActivity().runOnUiThread(() ->
                    tvResult.setText("⚠️ Chưa có dữ liệu kế hoạch trước đó!"));
            return;
        }

        long now = System.currentTimeMillis();
        long daysPassed = daysBetween(startTime, now);
        double monthsPassed = daysPassed / 30.0;

        // 🧮 Tính số tháng và ngày còn lại thực tế
        double monthsRemaining = Math.max(0, monthsSaved - monthsPassed);
        int remainingMonths = (int) Math.floor(monthsRemaining);
        int remainingDays = (int) Math.round((monthsRemaining - remainingMonths) * 30);

        double remainingTarget = Math.max(0, targetSaved - savedManualNow);
        double newSavingPerMonth = (monthsRemaining > 0) ? remainingTarget / monthsRemaining : remainingTarget;
        double newMaxExpense = incomeSaved - newSavingPerMonth;

        // 🔹 Lấy dữ liệu chi tiêu 3 tháng gần nhất
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -3);
        long ratioSince = cal.getTimeInMillis();
        List<CategoryExpense> ratioData = transactionDao.getExpensesByCategorySince(ratioSince);
        double ratioTotal = 0;
        for (CategoryExpense e : ratioData) ratioTotal += e.total;

        long monthStart = Math.max(monthStartNow(), startTime);
        List<CategoryExpense> spentThisMonth = transactionDao.getExpensesByCategorySince(monthStart);
        java.util.Map<String, Double> spentMap = new java.util.HashMap<>();
        for (CategoryExpense e : spentThisMonth) spentMap.put(e.category, e.total);

        StringBuilder out = new StringBuilder();
        out.append("🔄 Cập nhật kế hoạch dựa trên tiến độ:\n")
                .append("🎯 Mục tiêu tổng: ").append(df.format(targetSaved)).append(" VND\n")
                .append("💰 Đã tiết kiệm: ").append(df.format(savedManualNow)).append(" VND\n")
                .append("📆 Còn lại: ");
        if (remainingMonths > 0) out.append(remainingMonths).append(" tháng ");
        out.append(remainingDays).append(" ngày\n")
                .append("🏦 Cần tiết kiệm/tháng mới: ").append(df.format(newSavingPerMonth)).append(" VND\n")
                .append("📉 Chi tối đa/tháng mới: ").append(df.format(newMaxExpense)).append(" VND\n\n");

        if (ratioData.isEmpty() || ratioTotal == 0) {
            out.append("⚠️ Chưa có dữ liệu để phân bổ danh mục.");
            requireActivity().runOnUiThread(() -> tvResult.setText(out.toString()));
            return;
        }

        // 🔸 Phân bổ ngân sách theo tỷ lệ chi tiêu 3 tháng gần nhất
        java.util.Map<String, Double> plan = new java.util.LinkedHashMap<>();
        for (CategoryExpense e : ratioData) {
            double base = (e.total / ratioTotal) * newMaxExpense;
            plan.put(e.category, base);
        }

        // 🔸 Kiểm tra vượt chi trong tháng
        java.util.Set<String> overCats = new java.util.HashSet<>();
        double totalExceeded = 0;
        double totalAdjustable = 0;
        for (String cat : plan.keySet()) {
            double base = plan.get(cat);
            double spent = spentMap.getOrDefault(cat, 0.0);
            if (spent > base) {
                overCats.add(cat);
                totalExceeded += (spent - base);
            } else {
                totalAdjustable += base;
            }
        }

        // 🔹 Cân đối lại ngân sách cho các danh mục chưa vượt
        if (totalExceeded > 0 && totalAdjustable > 0) {
            for (String cat : plan.keySet()) {
                if (!overCats.contains(cat)) {
                    double base = plan.get(cat);
                    double reduced = base - (base / totalAdjustable) * totalExceeded;
                    plan.put(cat, Math.max(0, reduced));
                }
            }
        }

        // 🔸 Lưu lại kế hoạch mới
        StringBuilder planStr = new StringBuilder();
        for (java.util.Map.Entry<String, Double> en : plan.entrySet()) {
            planStr.append(en.getKey()).append("=").append(en.getValue()).append(";");
        }

        SharedPreferences.Editor ed = prefs.edit();
        ed.putFloat("savingPerMonth", (float) newSavingPerMonth);
        ed.putFloat("maxExpensePerMonth", (float) newMaxExpense);
        ed.putString("categoryBudgetPlan", planStr.toString());
        ed.apply();

        // 🔸 Hiển thị gợi ý chi tiêu mới
        out.append("📊 Gợi ý chi tiêu/tháng mới (đã cân đối tự động):\n");
        double totalAlloc = 0;
        for (String cat : plan.keySet()) {
            double alloc = plan.get(cat);
            double spent = spentMap.getOrDefault(cat, 0.0);
            boolean over = spent > alloc;
            out.append(" - ").append(cat).append(": ≤ ").append(df.format(alloc)).append(" VND");
            if (over) {
                out.append(" ⚠️ (đã chi: ").append(df.format(spent))
                        .append(", vượt ").append(df.format(spent - alloc)).append(")");
            }
            out.append("\n");
            totalAlloc += alloc;
        }

        out.append("\n🧮 Tổng ngân sách phân bổ: ").append(df.format(totalAlloc))
                .append(" / ").append(df.format(newMaxExpense)).append(" VND");

        requireActivity().runOnUiThread(() -> {
            tvResult.setText(out.toString());
            tvSavingStatus.setText("✅ Kế hoạch đã được cập nhật theo tiến độ ngày.");
            btnCheck.setVisibility(View.VISIBLE);
            btnEnd.setVisibility(View.VISIBLE);
            btnStart.setVisibility(View.GONE);
            btnAddSaved.setVisibility(View.VISIBLE);
            edtSavedAmount.setVisibility(View.VISIBLE);
        });
    }

}
