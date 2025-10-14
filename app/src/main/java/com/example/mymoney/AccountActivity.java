package com.example.mymoney;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AccountActivity extends AppCompatActivity {

    private TextView tvName, tvEmailTop, tvFullName, tvGender, tvEmail, tvPhone, tvDOB, tvJob, tvAddress;
    private Button btnEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Ánh xạ
        tvName = findViewById(R.id.tvName);
        tvEmailTop = findViewById(R.id.tvEmailTop);
        tvFullName = findViewById(R.id.tvFullName);
        tvGender = findViewById(R.id.tvGender);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvDOB = findViewById(R.id.tvDOB);
        tvJob = findViewById(R.id.tvJob);
        tvAddress = findViewById(R.id.tvAddress);
        btnEdit = findViewById(R.id.btnEdit);

        // Lấy thông tin từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);

        String name = prefs.getString("fullName", "(not set)");
        String gender = prefs.getString("gender", "(not set)");
        String email = prefs.getString("email", "(not set)");
        String phone = prefs.getString("phone", "(not set)");
        String dob = prefs.getString("dob", "(not set)");
        String job = prefs.getString("job", "(not set)");
        String address = prefs.getString("address", "(not set)");

        // Hiển thị
        tvName.setText(name);
        tvEmailTop.setText(email);
        tvFullName.setText("Fullname: " + name);
        tvGender.setText("Gender: " + gender);
        tvEmail.setText("Email: " + email);
        tvPhone.setText("Phone Number: " + phone);
        tvDOB.setText("Date of Birth: " + dob);
        tvJob.setText("Job: " + job);
        tvAddress.setText("Address: " + address);

        // Nút Edit
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cập nhật lại khi quay về
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        tvPhone.setText("Phone Number: " + prefs.getString("phone", "(not set)"));
        tvDOB.setText("Date of Birth: " + prefs.getString("dob", "(not set)"));
        tvJob.setText("Job: " + prefs.getString("job", "(not set)"));
        tvAddress.setText("Address: " + prefs.getString("address", "(not set)"));
    }
}
