package com.example.mymoney;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.dao.TransactionDao;
import com.example.mymoney.model.CategoryExpense;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

public class BudgetFragment extends Fragment {

    // ==== Views ====
    private LinearLayout layoutInputSection;
    private LinearLayout layoutSavingSection;   // ✅ THÊM VÀO ĐÂY

    private EditText edtTarget, edtMonths, edtIncome;
    private TextView tvResult;
    private Button btnCalc, btnStartSaving, btnEndSaving;
    private EditText edtSavedMoney;
    private Button btnUpdateSaved;

    // ==== Data / Storage ====
    private SharedPreferences prefs;
    private TransactionDao transactionDao;
    private final DecimalFormat df = new DecimalFormat("#,###");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    // Lưu lại nội dung kết quả đã tính toán (HTML)
    private String lastCalculatedSummary = "";
    private ProgressBar progressSaving;
    private TextView tvSavingPercent;


    // ==== Lifecycle ====
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        // Map views
        layoutInputSection = view.findViewById(R.id.layout_input_section);
        layoutSavingSection = view.findViewById(R.id.layout_saving_section);  // ✅ THÊM Ở ĐÂY
        layoutSavingSection.setVisibility(View.GONE);                         // ✅ ẨN MẶC ĐỊNH

        edtTarget = view.findViewById(R.id.edt_target_amount);
        edtMonths = view.findViewById(R.id.edt_time_range);
        edtIncome = view.findViewById(R.id.edt_income);
        tvResult = view.findViewById(R.id.tv_budget_result);
        btnCalc = view.findViewById(R.id.btn_calculate_budget);
        btnStartSaving = view.findViewById(R.id.btn_start_saving);
        btnEndSaving = view.findViewById(R.id.btn_end_saving);
        edtSavedMoney = view.findViewById(R.id.edt_saved_money);
        btnUpdateSaved = view.findViewById(R.id.btn_update_saved);
        progressSaving = view.findViewById(R.id.progressSaving);
        tvSavingPercent = view.findViewById(R.id.tvSavingPercent);
        progressSaving.setVisibility(View.GONE);


        // Init storage & DAO
        prefs = requireContext().getSharedPreferences("budget_prefs", Context.MODE_PRIVATE);
        transactionDao = AppDatabase.getInstance(requireContext()).transactionDao();

        // Trạng thái mặc định
        btnStartSaving.setVisibility(View.GONE);
        btnEndSaving.setVisibility(View.GONE);
        edtSavedMoney.setVisibility(View.GONE);
        btnUpdateSaved.setVisibility(View.GONE);

        // Nếu đang có kế hoạch tiết kiệm → load lại giao diện
        if (prefs.getBoolean("isSaving", false)) {
            loadSavedPlan();   // ✅ sẽ tự show tiến độ
        } else {
            layoutInputSection.setVisibility(View.VISIBLE);
            btnCalc.setVisibility(View.VISIBLE);
        }


        // Tính toán
        btnCalc.setOnClickListener(v ->
                Executors.newSingleThreadExecutor().execute(this::calculateBudget)
        );

        // Bắt đầu tiết kiệm
        btnStartSaving.setOnClickListener(v -> startSavingAction());

        // Kết thúc tiết kiệm
        btnEndSaving.setOnClickListener(v -> endSavingAction());

        // Cập nhật tiền tiết kiệm
        btnUpdateSaved.setOnClickListener(v -> updateSavedMoney());

        return view;
    }

    // ==== Utils ====
    private long floorToThousand(double value) {
        return (long) (Math.floor(value / 1000) * 1000);
    }

    // ==== Tính toán ngân sách ban đầu ====
    private void calculateBudget() {

        String targetStr = edtTarget.getText().toString().trim();
        String monthsStr = edtMonths.getText().toString().trim();
        String incomeStr = edtIncome.getText().toString().trim();

        if (TextUtils.isEmpty(targetStr) || TextUtils.isEmpty(monthsStr) || TextUtils.isEmpty(incomeStr)) {
            requireActivity().runOnUiThread(() -> {
                tvResult.setText("Vui lòng nhập đủ: mục tiêu, số tháng và thu nhập hàng tháng.");
                tvResult.setGravity(Gravity.START);
            });
            return;
        }

        try {
            double target = Double.parseDouble(targetStr);
            double months = Double.parseDouble(monthsStr);
            double income = Double.parseDouble(incomeStr);

            long targetVal = floorToThousand(target);
            long monthsVal = (long) Math.floor(months);
            long incomeVal = floorToThousand(income);

            if (monthsVal <= 0) {
                requireActivity().runOnUiThread(() -> {
                    tvResult.setText("Số tháng phải lớn hơn 0.");
                    tvResult.setGravity(Gravity.START);
                });
                return;
            }

            long savingPerMonth = floorToThousand((double) targetVal / monthsVal);
            long maxExpensePerMonth = floorToThousand(incomeVal - savingPerMonth);

            if (maxExpensePerMonth < 0) {
                requireActivity().runOnUiThread(() -> {
                    tvResult.setText("Lương thấp hơn số tiền cần tiết kiệm mỗi tháng.");
                    tvResult.setGravity(Gravity.START);
                });
                return;
            }

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -3);
            long fromDate = cal.getTimeInMillis();

            List<CategoryExpense> expenses = transactionDao.getExpensesByCategorySince(fromDate);

            double totalExpense3M = 0;
            for (CategoryExpense e : expenses) totalExpense3M += e.total;

            long totalSpent = floorToThousand(totalExpense3M);
            if (totalSpent <= 0) totalSpent = 1000;

            StringBuilder result = new StringBuilder();
            result.append("<b>Mục tiêu:</b> ").append(df.format(targetVal))
                    .append(" VND trong ").append(monthsVal).append(" tháng<br>");
            result.append("<b>Thu nhập hàng tháng:</b> ").append(df.format(incomeVal)).append(" VND<br>");
            result.append("<b>Tiết kiệm mỗi tháng:</b> ").append(df.format(savingPerMonth)).append(" VND<br>");
            result.append("<b>Chi tiêu tối đa mỗi tháng:</b> ").append(df.format(maxExpensePerMonth)).append(" VND<br><br>");

            // ✅ DÙNG 1 editor DUY NHẤT
            SharedPreferences.Editor editor = prefs.edit();

            result.append("<b>Phân bổ chi tiêu theo thói quen 3 tháng gần nhất:</b><br><br>");
            for (CategoryExpense e : expenses) {

                long spentCategory = floorToThousand(e.total);
                double ratio = (double) spentCategory / totalSpent;
                long suggestedPerMonth = floorToThousand(ratio * maxExpensePerMonth);

                // ✅ LƯU CHÍNH XÁC GIỚI HẠN TỪNG CATEGORY
                editor.putLong("limit_" + e.category, suggestedPerMonth);

                result.append("• <b>")
                        .append(e.category)
                        .append("</b>: tối đa ")
                        .append(df.format(suggestedPerMonth))
                        .append(" VND/tháng<br>");
            }

            // ✅ LƯU CÁC THAM SỐ CƠ BẢN
            editor.putLong("target", targetVal);
            editor.putLong("months", monthsVal);
            editor.putLong("income", incomeVal);
            editor.putLong("savingPerMonth", savingPerMonth);
            editor.putLong("maxExpensePerMonth", maxExpensePerMonth);
            editor.putString("summary", result.toString());

            editor.apply();   // ✅ APPLY 1 LẦN DUY NHẤT – GIẢI QUYẾT LỖI

            lastCalculatedSummary = result.toString();

            requireActivity().runOnUiThread(() -> {
                tvResult.setText(android.text.Html.fromHtml(result.toString()));
                tvResult.setGravity(Gravity.START);
                btnStartSaving.setVisibility(View.VISIBLE);
            });

        } catch (Exception e) {
            requireActivity().runOnUiThread(() -> {
                tvResult.setText("Lỗi tính toán.");
                tvResult.setGravity(Gravity.START);
            });
        }
    }



    // ==== Bắt đầu tiết kiệm ====
    private void startSavingAction() {
        long startTime = System.currentTimeMillis();

        prefs.edit()
                .putLong("savingStart", startTime)
                .putBoolean("isSaving", true)
                .apply();

        long savedManual = prefs.getLong("savedManual", 0);
        String startDate = dateFormat.format(new Date(startTime));
        String summary = prefs.getString("summary", lastCalculatedSummary);

        layoutInputSection.setVisibility(View.GONE);
        btnCalc.setVisibility(View.GONE);
        btnStartSaving.setVisibility(View.GONE);

        btnEndSaving.setVisibility(View.VISIBLE);
        edtSavedMoney.setVisibility(View.VISIBLE);
        btnUpdateSaved.setVisibility(View.VISIBLE);
        layoutSavingSection.setVisibility(View.VISIBLE);   // ✅ HIỆN LÊN ĐÚNG CHỖ

        String startText = "<br><b>Bắt đầu tiết kiệm từ ngày:</b> " + startDate + "<br>";
        String savedText = "<b>Tiền đã tiết kiệm:</b> " + df.format(savedManual) + " VND<br><br>";

        String finalText = summary + startText + savedText;

        requireActivity().runOnUiThread(() -> {
            tvResult.setText(android.text.Html.fromHtml(finalText));
            tvResult.setGravity(Gravity.START);
        });
    }

    // ==== Load lại kế hoạch ====
    private void loadSavedPlan() {
        String summary = prefs.getString("summary", "");
        long startTime = prefs.getLong("savingStart", 0);
        long savedManual = prefs.getLong("savedManual", 0);

        if (startTime == 0 || summary.isEmpty()) {
            layoutInputSection.setVisibility(View.VISIBLE);
            btnCalc.setVisibility(View.VISIBLE);
            layoutSavingSection.setVisibility(View.GONE);
            return;
        }

        layoutInputSection.setVisibility(View.GONE);
        btnCalc.setVisibility(View.GONE);
        layoutSavingSection.setVisibility(View.VISIBLE);

        btnEndSaving.setVisibility(View.VISIBLE);
        edtSavedMoney.setVisibility(View.VISIBLE);
        btnUpdateSaved.setVisibility(View.VISIBLE);

        String startDate = dateFormat.format(new Date(startTime));
        String startText = "<br><b>Bắt đầu tiết kiệm từ ngày:</b> " + startDate + "<br>";
        String savedText = "<b>Tiền đã tiết kiệm:</b> " + df.format(savedManual) + " VND<br><br>";

        String finalText = summary + startText + savedText;

        tvResult.setText(android.text.Html.fromHtml(finalText));
        tvResult.setGravity(Gravity.START);

        long target = prefs.getLong("target", 0);
        long saved = prefs.getLong("savedManual", 0);

        int percent = target == 0 ? 0 : (int)((saved * 100) / target);
        if (percent > 100) percent = 100;

        progressSaving.setVisibility(View.VISIBLE);
        progressSaving.setProgress(percent);
        tvSavingPercent.setText(percent + "%");
        Executors.newSingleThreadExecutor().execute(this::checkSavingProgress);


    }


    // ==== Kết thúc tiết kiệm ====
    private void endSavingAction() {
        prefs.edit().clear().apply();

        layoutInputSection.setVisibility(View.VISIBLE);
        btnCalc.setVisibility(View.VISIBLE);
        layoutSavingSection.setVisibility(View.GONE);   // ✅ ẨN LẠI ĐÚNG

        edtTarget.setText("");
        edtMonths.setText("");
        edtIncome.setText("");

        tvResult.setText("Hãy nhập thông tin để tạo kế hoạch tiết kiệm mới.");
        tvResult.setGravity(Gravity.CENTER_HORIZONTAL);
    }

    // ==== Cập nhật tiến độ ====
    private void checkSavingProgress() {

        long savingStart = prefs.getLong("savingStart", 0);
        if (savingStart == 0) {
            requireActivity().runOnUiThread(() -> {
                tvResult.setText("Bạn chưa bắt đầu tiết kiệm.");
                tvResult.setGravity(Gravity.START);
            });
            return;
        }

        long target = prefs.getLong("target", 0);
        long savingPerMonth = prefs.getLong("savingPerMonth", 0);
        long maxExpensePerMonth = prefs.getLong("maxExpensePerMonth", 0);
        long savedManual = prefs.getLong("savedManual", 0);

        // ==== Tính số ngày đã qua ====
        long now = System.currentTimeMillis();
        final long MS_PER_DAY = 24L * 60 * 60 * 1000;
        long daysPassed = ((now - savingStart) / MS_PER_DAY) + 1;

        // ==== Giới hạn & kế hoạch theo tháng =====
        long plannedSavedToDate = savingPerMonth;       // ✅ chỉ theo tháng
        long allowedExpenseToDate = maxExpensePerMonth; // ✅ chỉ theo tháng

        long remainToPlan = plannedSavedToDate - savedManual;
        if (remainToPlan < 0) remainToPlan = 0;

        // ==== Tổng chi tiêu thực tế ====
        long totalExpense = floorToThousand(
                AppDatabase.getInstance(requireContext())
                        .transactionDao()
                        .getTotalExpenseSince(savingStart)
        );

        StringBuilder result = new StringBuilder();
        result.append("<b>📊 Tiến độ tiết kiệm</b><br><br>");
        result.append("<b>Ngày bắt đầu:</b> ").append(dateFormat.format(new Date(savingStart))).append("<br>");
        result.append("<b>Đã qua:</b> ").append(daysPassed).append(" ngày<br><br>");

        // ==== TIẾT KIỆM ====
        result.append("<b>Tiền đã tiết kiệm:</b> ").append(df.format(savedManual)).append(" VND<br>");
        result.append("<b>Cần đạt theo tháng:</b> ").append(df.format(plannedSavedToDate)).append(" VND<br>");
        result.append("<b>Còn thiếu:</b> ").append(df.format(remainToPlan)).append(" VND<br><br>");

        // ==== CHI TIÊU ====
        long expenseThisMonth = getExpenseThisMonth();
        long monthLeft = maxExpensePerMonth - expenseThisMonth;
        if (monthLeft < 0) monthLeft = 0;

        result.append("<b>Tháng này được tiêu tối đa:</b> ")
                .append(df.format(maxExpensePerMonth)).append(" VND<br>");

        result.append("<b>Đã tiêu tháng này:</b> ")
                .append(df.format(monthLeft)).append(" VND<br>");
        result.append("<b>Còn lại trong tháng:</b> ")
                .append(df.format(expenseThisMonth)).append(" VND<br>");


        if (totalExpense > allowedExpenseToDate) {
            result.append("<font color='red'><b>⚠️ Vượt ngân sách!</b></font><br><br>");
        } else {
            result.append("<font color='green'><b>👍 Đang trong giới hạn!</b></font><br><br>");
        }

        // ==== THEO DANH MỤC ====
        result.append("<b>Chi tiêu theo danh mục (giới hạn theo tháng):</b><br>");

        List<CategoryExpense> expenses = transactionDao.getExpensesByCategorySince(savingStart);

        for (CategoryExpense ce : expenses) {

            long spentCat = floorToThousand(ce.total);

            // ✅ Giới hạn theo tháng (đã lưu khi tính toán)
            long perMonthLimit = prefs.getLong("limit_" + ce.category, 0);

            // ✅ Giới hạn tháng → không nhân theo ngày
            long allowedCatToDate = perMonthLimit;

            result.append("• <b>").append(ce.category).append("</b>: ")
                    .append(df.format(spentCat)).append("/")
                    .append(df.format(allowedCatToDate)).append(" VND ");

            if (spentCat > allowedCatToDate) {
                result.append("<font color='red'>(vượt)</font>");
            } else {
                result.append("<font color='green'>(ổn)</font>");
            }
            result.append("<br>");
        }

        requireActivity().runOnUiThread(() -> {
            tvResult.setText(android.text.Html.fromHtml(result.toString()));
            tvResult.setGravity(Gravity.START);
        });
    }



    // ==== Cập nhật số tiền tiết kiệm thủ công ====
    private void updateSavedMoney() {

        String savedStr = edtSavedMoney.getText().toString().trim();

        if (TextUtils.isEmpty(savedStr)) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Lỗi")
                    .setMessage("Vui lòng nhập số tiền bạn đã tiết kiệm.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        long added;
        try {
            added = floorToThousand(Double.parseDouble(savedStr));
            if (added <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Lỗi")
                    .setMessage("Giá trị không hợp lệ.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        long currentSaved = prefs.getLong("savedManual", 0);
        long newTotal = currentSaved + added;

        prefs.edit().putLong("savedManual", newTotal).apply();

        edtSavedMoney.setText("");

        long target = prefs.getLong("target", 0); // ✅ THÊM DÒNG NÀY

        int percent = target == 0 ? 0 : (int)((newTotal * 100) / target);
        if (percent > 100) percent = 100;

        // ✅ Cập nhật thanh tiến độ ngay lập tức
        progressSaving.setProgress(percent);
        tvSavingPercent.setText(percent + "%");

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Thành công")
                .setMessage("Đã cộng thêm: " + df.format(added)
                        + " VND\nTổng tiền đã tiết kiệm: " + df.format(newTotal) + " VND")
                .setPositiveButton("OK", (dialog, which) -> {

                    // ✅ Cập nhật lại toàn bộ nội dung phần text (summary + ngày)
                    loadSavedPlan();
                })
                .show();
    }

    private long getExpenseThisMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1); // đầu tháng
        long from = c.getTimeInMillis(); // từ đầu tháng
        return floorToThousand(
                transactionDao.getTotalExpenseSince(from)
        );
    }


}
