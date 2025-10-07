package com.example.mymoney;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymoney.adapter.CategoryAdapter;
import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.entity.Category;
import com.example.mymoney.database.entity.Transaction;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportFragment extends Fragment {

    private LinearLayout expenseSelector, incomeSelector;
    private EditText amountInput, notesInput;
    private LinearLayout dateSelector, categorySelector;
    private TextView dateText, categoryText;
    private ImageView categoryIcon;
    private RadioGroup repeatRadioGroup;
    private RadioButton repeatYes, repeatNo;
    private LinearLayout recurringSection;
    private Spinner recurringSpinner;
    private Button saveButton;
    
    // OCR related fields
    private LinearLayout btnCamera;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private Uri imageUri;
    private static final int CAMERA_PERMISSION_CODE = 100;
    
    private String selectedType = "expense"; // Default to expense
    private Calendar selectedDate;
    private int selectedCategoryId = -1; // Will be loaded from database
    private Category selectedCategory = null;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_import, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize OCR launchers first
        setupOCRLaunchers();
        
        // Initialize views
        expenseSelector = view.findViewById(R.id.expense_selector);
        incomeSelector = view.findViewById(R.id.income_selector);
        amountInput = view.findViewById(R.id.amount_input);
        notesInput = view.findViewById(R.id.notes_input);
        dateSelector = view.findViewById(R.id.date_selector);
        dateText = view.findViewById(R.id.date_text);
        categorySelector = view.findViewById(R.id.category_selector);
        categoryText = view.findViewById(R.id.category_text);
        categoryIcon = view.findViewById(R.id.category_icon);
        repeatRadioGroup = view.findViewById(R.id.repeat_radio_group);
        repeatYes = view.findViewById(R.id.repeat_yes);
        repeatNo = view.findViewById(R.id.repeat_no);
        recurringSection = view.findViewById(R.id.recurring_section);
        recurringSpinner = view.findViewById(R.id.recurring_spinner);
        saveButton = view.findViewById(R.id.save_button);
        btnCamera = view.findViewById(R.id.btnCamera);
        
        // Initialize selected date to today
        selectedDate = Calendar.getInstance();
        updateDateDisplay();
        
        // Set up recurring spinner
        setupRecurringSpinner();
        
        // Set up click listeners
        setupListeners();
        
        // Set up OCR button
        setupOCRButton();
        
        // Load default category from database
        loadDefaultCategory();
    }
    
    private void setupRecurringSpinner() {
        String[] recurringOptions = {
            getString(R.string.daily),
            getString(R.string.weekly),
            getString(R.string.monthly),
            getString(R.string.yearly)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            recurringOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recurringSpinner.setAdapter(adapter);
        recurringSpinner.setSelection(2); // Default to Monthly
    }
    
    private void setupListeners() {
        // Transaction type selectors
        expenseSelector.setOnClickListener(v -> {
            selectTransactionType("expense");
            loadCategoriesForType("expense");
        });
        incomeSelector.setOnClickListener(v -> {
            selectTransactionType("income");
            loadCategoriesForType("income");
        });
        
        // Date selector
        dateSelector.setOnClickListener(v -> showDatePicker());
        
        // Category selector
        categorySelector.setOnClickListener(v -> showCategoryDialog());
        
        // Repeat radio group
        repeatRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.repeat_yes) {
                recurringSection.setVisibility(View.VISIBLE);
            } else {
                recurringSection.setVisibility(View.GONE);
            }
        });
        
        // Save button
        saveButton.setOnClickListener(v -> saveTransaction());
    }
    
    private void selectTransactionType(String type) {
        selectedType = type;
        
        if (type.equals("expense")) {
            expenseSelector.setBackgroundResource(R.drawable.selector_background);
            incomeSelector.setBackgroundResource(R.drawable.search_background);
            // Update text colors if needed
        } else {
            incomeSelector.setBackgroundResource(R.drawable.selector_background);
            expenseSelector.setBackgroundResource(R.drawable.search_background);
        }
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateDisplay();
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateText.setText(sdf.format(selectedDate.getTime()));
    }
    
    /**
     * Load the first available category from database as default
     */
    private void loadDefaultCategory() {
        loadCategoriesForType(selectedType);
    }
    
    /**
     * Load categories based on transaction type (expense or income)
     */
    private void loadCategoriesForType(String type) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                
                // Get categories by type
                List<Category> categories;
                if (type.equals("expense")) {
                    categories = db.categoryDao().getAllExpenseCategories();
                    android.util.Log.d("ImportFragment", "Loaded " + categories.size() + " expense categories");
                } else {
                    categories = db.categoryDao().getAllIncomeCategories();
                    android.util.Log.d("ImportFragment", "Loaded " + categories.size() + " income categories");
                }
                
                if (!categories.isEmpty() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Auto-select first category if none selected or type changed
                        if (selectedCategory == null || !selectedCategory.getType().equals(type)) {
                            selectedCategory = categories.get(0);
                            selectedCategoryId = selectedCategory.getId();
                            updateCategoryDisplay();
                            android.util.Log.d("ImportFragment", "Auto-selected category: " + selectedCategory.getName());
                        }
                    });
                } else {
                    android.util.Log.w("ImportFragment", "No categories found for type: " + type);
                }
            } catch (Exception e) {
                android.util.Log.e("ImportFragment", "Error loading categories", e);
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Show category selection dialog
     */
    private void showCategoryDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_category_selection);
        
        RecyclerView recyclerView = dialog.findViewById(R.id.categories_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        CategoryAdapter adapter = new CategoryAdapter(category -> {
            selectedCategory = category;
            selectedCategoryId = category.getId();
            updateCategoryDisplay();
            dialog.dismiss();
        });
        
        adapter.setSelectedCategoryId(selectedCategoryId);
        recyclerView.setAdapter(adapter);
        
        // Load categories based on current type
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<Category> categories;
                if (selectedType.equals("expense")) {
                    categories = db.categoryDao().getAllExpenseCategories();
                    android.util.Log.d("ImportFragment", "Dialog: Loaded " + categories.size() + " expense categories");
                } else {
                    categories = db.categoryDao().getAllIncomeCategories();
                    android.util.Log.d("ImportFragment", "Dialog: Loaded " + categories.size() + " income categories");
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter.setCategories(categories);
                        android.util.Log.d("ImportFragment", "Dialog: Set " + categories.size() + " categories to adapter");
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("ImportFragment", "Dialog: Error loading categories", e);
                e.printStackTrace();
            }
        }).start();
        
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
    
    /**
     * Update category display with selected category
     */
    private void updateCategoryDisplay() {
        if (selectedCategory != null) {
            categoryText.setText(selectedCategory.getName());
            
            // Set icon based on category name
            int iconRes;
            switch (selectedCategory.getName().toLowerCase()) {
                case "food":
                    iconRes = R.drawable.ic_food;
                    break;
                case "home":
                    iconRes = R.drawable.ic_home;
                    break;
                case "transport":
                    iconRes = R.drawable.ic_taxi;
                    break;
                case "relationship":
                    iconRes = R.drawable.ic_love;
                    break;
                case "entertainment":
                    iconRes = R.drawable.ic_entertainment;
                    break;
                case "salary":
                    iconRes = R.drawable.ic_salary;
                    break;
                case "business":
                    iconRes = R.drawable.ic_work;
                    break;
                case "gifts":
                    iconRes = R.drawable.ic_gift;
                    break;
                case "others":
                default:
                    iconRes = R.drawable.ic_more_apps;
                    break;
            }
            categoryIcon.setImageResource(iconRes);
            categoryIcon.setVisibility(View.VISIBLE);
        } else {
            categoryText.setText("Select Category");
            categoryIcon.setVisibility(View.GONE);
        }
    }
    
    private void saveTransaction() {
        // Get selected wallet from MainActivity
        int walletId = MainActivity.getSelectedWalletId();
        
        // If no wallet selected, try to get the first available wallet
        if (walletId == -1) {
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                var wallets = db.walletDao().getActiveWalletsByUserId(MainActivity.getCurrentUserId());
                if (!wallets.isEmpty()) {
                    MainActivity.setSelectedWalletId(wallets.get(0).getId());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> saveTransactionWithWallet());
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(requireContext(), "Please create a wallet first", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }).start();
            return;
        }
        
        saveTransactionWithWallet();
    }
    
    private void saveTransactionWithWallet() {
        // Validate input
        String amountStr = amountInput.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if category is selected
        if (selectedCategoryId == -1 || selectedCategory == null) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create transaction object
        Transaction transaction = new Transaction();
        transaction.setWalletId(MainActivity.getSelectedWalletId());
        transaction.setCategoryId(selectedCategoryId);
        transaction.setUserId(MainActivity.getCurrentUserId());
        transaction.setAmount(amount);
        transaction.setDescription(notesInput.getText().toString().trim());
        transaction.setType(selectedType);
        transaction.setCreatedAt(selectedDate.getTimeInMillis());
        transaction.setUpdatedAt(System.currentTimeMillis());
        
        // Handle recurring
        boolean isRecurring = repeatYes.isChecked();
        transaction.setRecurring(isRecurring);
        
        if (isRecurring) {
            String[] intervals = {"daily", "weekly", "monthly", "yearly"};
            int selectedPosition = recurringSpinner.getSelectedItemPosition();
            transaction.setRecurringInterval(intervals[selectedPosition]);
        } else {
            transaction.setRecurringInterval(null);
        }
        
        // Save to database in background thread
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                long transactionId = db.transactionDao().insert(transaction);
                
                // Update wallet balance
                updateWalletBalance(transaction);
                
                // Show success message on UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Transaction saved successfully!", Toast.LENGTH_SHORT).show();
                        clearForm();
                        
                        // Refresh HomeFragment if it exists
                        refreshHomeFragment();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error saving transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }
    
    private void updateWalletBalance(Transaction transaction) {
        try {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            com.example.mymoney.database.entity.Wallet wallet = 
                db.walletDao().getWalletById(transaction.getWalletId());
            
            if (wallet != null) {
                double currentBalance = wallet.getBalance();
                double newBalance;
                
                if (transaction.getType().equals("income")) {
                    newBalance = currentBalance + transaction.getAmount();
                } else {
                    newBalance = currentBalance - transaction.getAmount();
                }
                
                wallet.setBalance(newBalance);
                db.walletDao().update(wallet);
                
                android.util.Log.d("ImportFragment", "Wallet balance updated: " + currentBalance + " -> " + newBalance);
            }
        } catch (Exception e) {
            android.util.Log.e("ImportFragment", "Error updating wallet balance", e);
        }
    }
    
    private void refreshHomeFragment() {
        // Refresh HomeFragment and HistoryFragment after saving transaction
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            androidx.fragment.app.FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
            
            // Find and refresh HomeFragment if it exists
            for (androidx.fragment.app.Fragment fragment : fragmentManager.getFragments()) {
                if (fragment instanceof HomeFragment && fragment.isAdded()) {
                    ((HomeFragment) fragment).refreshData();
                    android.util.Log.d("ImportFragment", "HomeFragment refreshed");
                }
                if (fragment instanceof HistoryFragment && fragment.isAdded()) {
                    ((HistoryFragment) fragment).refreshData();
                    android.util.Log.d("ImportFragment", "HistoryFragment refreshed");
                }
            }
        }
    }
    
    private void clearForm() {
        amountInput.setText("");
        notesInput.setText("");
        repeatNo.setChecked(true);
        recurringSection.setVisibility(View.GONE);
        selectedDate = Calendar.getInstance();
        updateDateDisplay();
        selectedType = "expense";
        selectTransactionType("expense");
    }
    
    // ==================== OCR Methods ====================
    
    /**
     * Setup OCR activity result launchers
     */
    private void setupOCRLaunchers() {
        // Launcher for Camera
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK) {
                        if (imageUri != null) {
                            Toast.makeText(requireContext(), "Ảnh đã chụp xong!", Toast.LENGTH_SHORT).show();
                            processImage(imageUri);
                        }
                    }
                }
        );

        // Launcher for Gallery
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        Toast.makeText(requireContext(), "Đã chọn ảnh từ thư viện!", Toast.LENGTH_SHORT).show();
                        processImage(selectedImage);
                    }
                }
        );
    }
    
    /**
     * Setup OCR button click listener
     */
    private void setupOCRButton() {
        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> {
                String[] options = {"Chụp ảnh", "Chọn từ thư viện"};
                new AlertDialog.Builder(requireContext())
                        .setTitle("Nhập dữ liệu bằng ảnh")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                checkCameraPermissionAndOpen();
                            } else {
                                openGallery();
                            }
                        })
                        .show();
            });
        }
    }
    
    /**
     * Check camera permission and open camera
     */
    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(requireContext(), "Bạn cần cấp quyền Camera để chụp ảnh!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Open camera to capture image
     */
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = new File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp.jpg");
        imageUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraLauncher.launch(intent);
    }

    /**
     * Open gallery to select image
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    /**
     * Process image using ML Kit OCR
     */
    private void processImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String rawText = visionText.getText();
                        Toast.makeText(requireContext(), "OCR thành công!", Toast.LENGTH_SHORT).show();
                        handleExtractedText(rawText);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Lỗi OCR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Extract transaction data from OCR text
     */
    private void handleExtractedText(String text) {
        String type = "";
        String category = "";
        String date = "";
        String amount = "";

        // Làm sạch text OCR
        String cleanedText = text.replaceAll("[^\\dA-Za-zÀ-ỹ\\s/\\-.,]", " ").toLowerCase(Locale.ROOT);
        android.util.Log.d("OCR_TEXT", "Raw: " + text);
        android.util.Log.d("OCR_TEXT", "Cleaned: " + cleanedText);

        // 1️⃣ Nhận diện loại giao dịch
        if (cleanedText.contains("chi") || cleanedText.contains("expense")) {
            type = "expense";
        } else if (cleanedText.contains("thu") || cleanedText.contains("income")) {
            type = "income";
        }

        // 2️⃣ Nhận diện số tiền (ưu tiên số lớn nhất)
        // 2️⃣ Nhận diện số tiền chính xác hơn (tìm số dài nhất, bỏ dấu và VND)
        Pattern amountPattern = Pattern.compile("(\\d{3,}(?:[.,]\\d{3,})*)");
        Matcher matcher = amountPattern.matcher(text.replaceAll("[^0-9.,]", " "));
        String longestNum = "";
        while (matcher.find()) {
            String numStr = matcher.group(1);
            if (numStr.length() > longestNum.length()) {
                longestNum = numStr;
            }
        }

        if (!longestNum.isEmpty()) {
            // Xóa dấu chấm, dấu phẩy và ký tự tiền tệ
            String cleaned = longestNum.replaceAll("[.,]", "").replaceAll("[^0-9]", "");
            try {
                long value = Long.parseLong(cleaned);
                amount = String.valueOf(value);
            } catch (NumberFormatException ignored) {}
        }


        // 3️⃣ Nhận diện ngày
        Pattern datePattern = Pattern.compile("(\\d{1,2}[\\-/]\\d{1,2}[\\-/]\\d{2,4})");
        Matcher dateMatcher = datePattern.matcher(text);
        if (dateMatcher.find()) {
            date = dateMatcher.group(1);
            parseAndSetDate(date);
        }

        // 4️⃣ Nhận diện danh mục
        if (cleanedText.contains("ăn") || cleanedText.contains("food")) {
            category = "Food";
        } else if (cleanedText.contains("xe") || cleanedText.contains("transport")) {
            category = "Transport";
        } else if (cleanedText.contains("nhà") || cleanedText.contains("home")) {
            category = "Home";
        } else if (cleanedText.contains("giải trí") || cleanedText.contains("entertainment")) {
            category = "Entertainment";
        } else if (cleanedText.contains("lương") || cleanedText.contains("salary")) {
            category = "Salary";
        } else if (cleanedText.contains("kinh doanh") || cleanedText.contains("business")) {
            category = "Business";
        } else {
            category = "Others";
        }

        // ✅ Gọi hàm hiển thị confirm dialog
        fillFormFromOCR(type, amount, category, date);
    }


    /**
     * Parse date string and set selectedDate
     */
    private void parseAndSetDate(String dateString) {
        try {
            SimpleDateFormat sdf;
            if (dateString.contains("/")) {
                sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            } else {
                sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            }
            
            selectedDate.setTime(sdf.parse(dateString));
            updateDateDisplay();
        } catch (Exception e) {
            android.util.Log.e("ImportFragment", "Error parsing date: " + dateString, e);
        }
    }

    /**
     * Fill form with OCR extracted data
     */
    private void fillFormFromOCR(String type, String amount, String categoryName, String date) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showOCRConfirmDialog(type, amount, categoryName, date);
            });
        }
    }
    
    /**
     * Find and set category by name
     */
    private void findAndSetCategory(String categoryName) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<Category> categories;
                
                if (selectedType.equals("expense")) {
                    categories = db.categoryDao().getAllExpenseCategories();
                } else {
                    categories = db.categoryDao().getAllIncomeCategories();
                }
                
                // Find matching category
                Category matchedCategory = null;
                for (Category cat : categories) {
                    if (cat.getName().equalsIgnoreCase(categoryName)) {
                        matchedCategory = cat;
                        break;
                    }
                }
                
                if (matchedCategory != null) {
                    Category finalCategory = matchedCategory;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            selectedCategory = finalCategory;
                            selectedCategoryId = finalCategory.getId();
                            updateCategoryDisplay();
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ImportFragment", "Error finding category: " + categoryName, e);
            }
        }).start();
    }
    /**
     * Hiển thị hộp xác nhận trước khi lưu dữ liệu OCR vào CSDL
     */
    private void showOCRConfirmDialog(String type, String amount, String categoryName, String date) {
        if (getActivity() == null) return;

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_ocr_confirm);

        TextView tvType = dialog.findViewById(R.id.tvType);
        TextView tvAmount = dialog.findViewById(R.id.tvAmount);
        TextView tvCategory = dialog.findViewById(R.id.tvCategory);
        TextView tvDate = dialog.findViewById(R.id.tvDate);
        EditText etNote = dialog.findViewById(R.id.etNote);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        // Gán dữ liệu OCR vào giao diện
        tvType.setText(type.equals("expense") ? "Expense" : "Income");
        tvAmount.setText(amount + " VND");
        tvCategory.setText(categoryName);
        tvDate.setText(date);

        // Nút hủy
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Nút xác nhận
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();

            // Gán dữ liệu vào form chính
            amountInput.setText(amount);
            notesInput.setText(etNote.getText().toString());
            selectedType = type;
            selectTransactionType(type);
            loadCategoriesForType(type);
            findAndSetCategory(categoryName);
            parseAndSetDate(date);

            // Lưu vào database
            saveTransactionWithWallet();
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

}
