package com.example.mymoney;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.mymoney.R;


public class BudgetFragment extends Fragment {

    private EditText edtTarget, edtTime;
    private TextView tvResult;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        edtTarget = view.findViewById(R.id.edt_target_amount);
        edtTime = view.findViewById(R.id.edt_time_range);
        Button btnCalc = view.findViewById(R.id.btn_calculate_budget);
        tvResult = view.findViewById(R.id.tv_budget_result);

        btnCalc.setOnClickListener(v -> calculateBudget());

        return view;
    }

    private void calculateBudget() {
        try {
            double target = Double.parseDouble(edtTarget.getText().toString());
            double months = Double.parseDouble(edtTime.getText().toString());

            double monthlyBudget = target / months;
            tvResult.setText(
                    "👉 Để tiết kiệm " + (int)target + " VND trong " + (int)months + " tháng,\n"
                            + "bạn nên đặt giới hạn chi tiêu mỗi tháng khoảng: "
                            + String.format("%,.0f", monthlyBudget) + " VND.\n\n"
                            + "💡 Gợi ý phân bổ:\n"
                            + " - Ăn uống: 40%\n"
                            + " - Giải trí: 15%\n"
                            + " - Di chuyển: 10%\n"
                            + " - Nhà ở: 25%\n"
                            + " - Khác: 10%"
            );
        } catch (Exception e) {
            tvResult.setText("⚠️ Vui lòng nhập đúng số tiền và thời gian.");
        }
    }
}
