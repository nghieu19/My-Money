package com.example.mymoney;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.entity.User;

public class RegisterActivity extends AppCompatActivity {

    EditText edtFullName, edtEmail, edtUsername, edtPassword;
    Button btnRegister;
    TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Handle Register button
        btnRegister.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            // Register user in background thread
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(this);
                
                // Check if username already exists
                User existingUser = db.userDao().getUserByUsername(username);
                
                runOnUiThread(() -> {
                    if (existingUser != null) {
                        Toast.makeText(this, "Tên đăng nhập đã tồn tại!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Create new user
                        new Thread(() -> {
                            User newUser = new User();
                            newUser.setUsername(username);
                            newUser.setPassword(password);
                            newUser.setEmail(email);
                            newUser.setFullName(fullName);
                            newUser.setCreatedAt(System.currentTimeMillis());
                            
                            long userId = db.userDao().insert(newUser);
                            
                            runOnUiThread(() -> {
                                if (userId > 0) {
                                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, LoginActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(this, "Đăng ký thất bại. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }).start();
                    }
                });
            }).start();
        });

        // Handle "Already have an account? Login" click
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}