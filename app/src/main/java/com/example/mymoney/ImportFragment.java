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
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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
        
        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            android.util.Log.e("ImportFragment", "OpenCV initialization failed");
        } else {
            android.util.Log.d("ImportFragment", "OpenCV initialized successfully");
        }
        
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
     * Process image using ML Kit OCR with preprocessing
     */
    private void processImage(Uri uri) {
        try {
            // Load original image
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            
            // Preprocess image for better OCR accuracy
            Bitmap preprocessedBitmap = preprocessImage(originalBitmap);
            
            // Convert to InputImage
            InputImage image = InputImage.fromBitmap(preprocessedBitmap, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        Toast.makeText(requireContext(), "OCR thành công!", Toast.LENGTH_SHORT).show();
                        handleExtractedTextAdvanced(visionText);
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
     * Preprocess image to improve OCR accuracy
     * - Convert to grayscale
     * - Apply Gaussian blur to reduce noise
     * - Apply adaptive thresholding
     */
    private Bitmap preprocessImage(Bitmap originalBitmap) {
        try {
            Mat mat = new Mat();
            Utils.bitmapToMat(originalBitmap, mat);
            
            // 1. Convert to grayscale
            Mat grayMat = new Mat();
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY);
            
            // 2. Apply Gaussian blur to reduce noise
            Mat blurredMat = new Mat();
            Imgproc.GaussianBlur(grayMat, blurredMat, new Size(5, 5), 0);
            
            // 3. Apply adaptive thresholding (better than Otsu for receipts)
            Mat thresholdMat = new Mat();
            Imgproc.adaptiveThreshold(
                blurredMat, 
                thresholdMat, 
                255, 
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, 
                Imgproc.THRESH_BINARY, 
                11, 
                2
            );
            
            // Convert back to Bitmap
            Bitmap processedBitmap = Bitmap.createBitmap(
                thresholdMat.cols(), 
                thresholdMat.rows(), 
                Bitmap.Config.ARGB_8888
            );
            Utils.matToBitmap(thresholdMat, processedBitmap);
            
            // Release resources
            mat.release();
            grayMat.release();
            blurredMat.release();
            thresholdMat.release();
            
            android.util.Log.d("ImportFragment", "Image preprocessing completed");
            return processedBitmap;
            
        } catch (Exception e) {
            android.util.Log.e("ImportFragment", "Error preprocessing image, using original", e);
            return originalBitmap;
        }
    }
    
    /**
     * Advanced text extraction using ML Kit's structured output
     * Segments receipt into logical zones and extracts data
     */
    private void handleExtractedTextAdvanced(Text visionText) {
        List<TextLine> allLines = new ArrayList<>();
        
        // Extract all text blocks with their positions
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                if (line.getBoundingBox() != null) {
                    TextLine textLine = new TextLine(
                        line.getText(),
                        line.getBoundingBox().top,
                        line.getBoundingBox().bottom
                    );
                    allLines.add(textLine);
                    android.util.Log.d("ImportFragment", "Line: " + line.getText() + " at Y: " + line.getBoundingBox().top);
                }
            }
        }
        
        // Sort lines by Y-coordinate (top to bottom)
        Collections.sort(allLines, Comparator.comparingInt(l -> l.y));
        
        // Extract data from structured lines
        String amount = extractAmountAdvanced(allLines);
        String date = extractDateAdvanced(allLines);
        String category = extractCategoryAdvanced(allLines);
        
        // Fill the form
        fillFormFromOCR("", amount, category, date);
    }
    
    /**
     * Helper class to store text line with position
     */
    private static class TextLine {
        String text;
        int y;
        int yBottom;
        
        TextLine(String text, int y, int yBottom) {
            this.text = text;
            this.y = y;
            this.yBottom = yBottom;
        }
    }
    
    /**
     * Extract amount using receipt structure and keywords
     */
    private String extractAmountAdvanced(List<TextLine> lines) {
        String amount = "";
        double maxAmount = 0;
        
        // Keywords that indicate total amount (in order of priority)
        String[] totalKeywords = {
            "total amount", "tổng tiền", "tổng cộng", "grand total",
            "total", "tổng", "thành tiền", "sum", "amount", "số tiền"
        };
        
        // 1. First priority: Find lines with total keywords
        for (TextLine line : lines) {
            String textLower = line.text.toLowerCase();
            
            for (String keyword : totalKeywords) {
                if (textLower.contains(keyword)) {
                    // Extract number from this line or next line
                    String extractedAmount = extractNumberFromLine(line.text);
                    
                    if (!extractedAmount.isEmpty()) {
                        android.util.Log.d("ImportFragment", "Amount found with keyword '" + keyword + "': " + extractedAmount);
                        return extractedAmount;
                    }
                    
                    // Check next line (total might be on separate line)
                    int currentIndex = lines.indexOf(line);
                    if (currentIndex < lines.size() - 1) {
                        extractedAmount = extractNumberFromLine(lines.get(currentIndex + 1).text);
                        if (!extractedAmount.isEmpty()) {
                            android.util.Log.d("ImportFragment", "Amount found in next line after keyword: " + extractedAmount);
                            return extractedAmount;
                        }
                    }
                }
            }
        }
        
        // 2. Second priority: Find the largest amount in the bottom 30% of receipt
        int bottomThreshold = (int) (lines.size() * 0.7);
        for (int i = bottomThreshold; i < lines.size(); i++) {
            String extractedAmount = extractNumberFromLine(lines.get(i).text);
            if (!extractedAmount.isEmpty()) {
                try {
                    double num = Double.parseDouble(extractedAmount);
                    if (num > maxAmount) {
                        maxAmount = num;
                        amount = extractedAmount;
                    }
                } catch (NumberFormatException e) {
                    // Skip
                }
            }
        }
        
        if (!amount.isEmpty()) {
            android.util.Log.d("ImportFragment", "Amount found in bottom section: " + amount);
            return amount;
        }
        
        // 3. Last resort: Find largest number in entire receipt
        for (TextLine line : lines) {
            String extractedAmount = extractNumberFromLine(line.text);
            if (!extractedAmount.isEmpty()) {
                try {
                    double num = Double.parseDouble(extractedAmount);
                    if (num > maxAmount) {
                        maxAmount = num;
                        amount = extractedAmount;
                    }
                } catch (NumberFormatException e) {
                    // Skip
                }
            }
        }
        
        android.util.Log.d("ImportFragment", "Amount found (largest overall): " + amount);
        return amount;
    }
    
    /**
     * Extract number from a line of text
     */
    private String extractNumberFromLine(String text) {
        // Pattern to match numbers with optional decimal point and thousands separator
        // Supports: 1000, 1,000, 1.000, 1000.50, 1,000.50
        Pattern pattern = Pattern.compile("(\\d{1,3}(?:[,.]\\d{3})*(?:[.,]\\d{2})?)");
        Matcher matcher = pattern.matcher(text);
        
        double maxValue = 0;
        String maxNumber = "";
        
        while (matcher.find()) {
            String numStr = matcher.group(1);
            // Normalize: remove thousands separators, keep only last decimal point
            String normalized = numStr.replaceAll("[,.](?=\\d{3})", "").replace(",", ".");
            
            try {
                double value = Double.parseDouble(normalized);
                if (value > maxValue) {
                    maxValue = value;
                    maxNumber = normalized.replace(".", "");
                }
            } catch (NumberFormatException e) {
                // Skip invalid numbers
            }
        }
        
        return maxNumber;
    }
    
    /**
     * Extract date from receipt lines
     */
    private String extractDateAdvanced(List<TextLine> lines) {
        // Date is usually in top 30% of receipt
        int topThreshold = Math.min(lines.size(), (int) (lines.size() * 0.3));
        
        // Numeric date patterns
        Pattern[] numericDatePatterns = {
            Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})"),
            Pattern.compile("(\\d{1,2}\\s+[/-]\\s+\\d{1,2}\\s+[/-]\\s+\\d{2,4})"),
            Pattern.compile("(\\d{2,4}[/-]\\d{1,2}[/-]\\d{1,2})")
        };
        
        // Vietnamese text date pattern: "Ngày 18 tháng 09 năm 2025"
        Pattern vietnameseDatePattern = Pattern.compile(
            "ngày\\s+(\\d{1,2})\\s+tháng\\s+(\\d{1,2})\\s+n[aă]m\\s+(\\d{4})",
            Pattern.CASE_INSENSITIVE
        );
        
        // English text date patterns: "18 September 2025", "September 18, 2025"
        Pattern[] englishDatePatterns = {
            Pattern.compile("(\\d{1,2})\\s+(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{1,2}),?\\s+(\\d{4})", Pattern.CASE_INSENSITIVE)
        };
        
        // Search in top section first
        for (int i = 0; i < topThreshold; i++) {
            String lineText = lines.get(i).text;
            
            // Try Vietnamese text date
            Matcher vietnameseMatcher = vietnameseDatePattern.matcher(lineText);
            if (vietnameseMatcher.find()) {
                String day = vietnameseMatcher.group(1);
                String month = vietnameseMatcher.group(2);
                String year = vietnameseMatcher.group(3);
                String date = day + "/" + month + "/" + year;
                android.util.Log.d("ImportFragment", "Vietnamese date found in top section: " + date);
                parseAndSetDate(date);
                return date;
            }
            
            // Try English text date patterns
            for (Pattern pattern : englishDatePatterns) {
                Matcher matcher = pattern.matcher(lineText);
                if (matcher.find()) {
                    String date = matcher.group(0);
                    android.util.Log.d("ImportFragment", "English text date found in top section: " + date);
                    parseAndSetDateText(date);
                    return date;
                }
            }
            
            // Try numeric date patterns
            for (Pattern pattern : numericDatePatterns) {
                Matcher matcher = pattern.matcher(lineText);
                if (matcher.find()) {
                    String date = matcher.group(1).replaceAll("\\s+", "");
                    android.util.Log.d("ImportFragment", "Numeric date found in top section: " + date);
                    parseAndSetDate(date);
                    return date;
                }
            }
        }
        
        // Search entire receipt if not found in top
        for (TextLine line : lines) {
            String lineText = line.text;
            
            // Try Vietnamese text date
            Matcher vietnameseMatcher = vietnameseDatePattern.matcher(lineText);
            if (vietnameseMatcher.find()) {
                String day = vietnameseMatcher.group(1);
                String month = vietnameseMatcher.group(2);
                String year = vietnameseMatcher.group(3);
                String date = day + "/" + month + "/" + year;
                android.util.Log.d("ImportFragment", "Vietnamese date found: " + date);
                parseAndSetDate(date);
                return date;
            }
            
            // Try English text date patterns
            for (Pattern pattern : englishDatePatterns) {
                Matcher matcher = pattern.matcher(lineText);
                if (matcher.find()) {
                    String date = matcher.group(0);
                    android.util.Log.d("ImportFragment", "English text date found: " + date);
                    parseAndSetDateText(date);
                    return date;
                }
            }
            
            // Try numeric date patterns
            for (Pattern pattern : numericDatePatterns) {
                Matcher matcher = pattern.matcher(lineText);
                if (matcher.find()) {
                    String date = matcher.group(1).replaceAll("\\s+", "");
                    android.util.Log.d("ImportFragment", "Numeric date found: " + date);
                    parseAndSetDate(date);
                    return date;
                }
            }
        }
        
        return "";
    }
    
    /**
     * Extract category from receipt content
     */
    private String extractCategoryAdvanced(List<TextLine> lines) {
        // Combine all text for category detection
        StringBuilder fullText = new StringBuilder();
        for (TextLine line : lines) {
            fullText.append(line.text.toLowerCase()).append(" ");
        }
        
        String text = fullText.toString();
        return extractCategory(text);
    }
    
    
    /**
     * Extract category from text, defaults to "Others" if not found
     */
    private String extractCategory(String text) {
        String textLower = text.toLowerCase();
        
        // Food category - Vietnamese and English keywords
        if (textLower.contains("food") || textLower.contains("ăn") || 
            textLower.contains("thức ăn") || textLower.contains("đồ ăn") ||
            textLower.contains("nhà hàng") || textLower.contains("restaurant") ||
            textLower.contains("quán ăn") || textLower.contains("cafe") ||
            textLower.contains("cà phê")) {
            return "Food";
        }
        
        // Transport category
        if (textLower.contains("transport") || textLower.contains("xe") || 
            textLower.contains("đi lại") || textLower.contains("taxi") ||
            textLower.contains("grab") || textLower.contains("xe buýt") ||
            textLower.contains("bus") || textLower.contains("xăng") ||
            textLower.contains("gas") || textLower.contains("fuel")) {
            return "Transport";
        }
        
        // Home category
        if (textLower.contains("home") || textLower.contains("nhà") || 
            textLower.contains("thuê nhà") || textLower.contains("rent") ||
            textLower.contains("điện") || textLower.contains("nước") ||
            textLower.contains("electric") || textLower.contains("water")) {
            return "Home";
        }
        
        // Entertainment category
        if (textLower.contains("entertainment") || textLower.contains("giải trí") ||
            textLower.contains("phim") || textLower.contains("movie") ||
            textLower.contains("game") || textLower.contains("du lịch") ||
            textLower.contains("travel")) {
            return "Entertainment";
        }
        
        // Relationship category
        if (textLower.contains("relationship") || textLower.contains("tình cảm") ||
            textLower.contains("gift") || textLower.contains("quà") ||
            textLower.contains("tặng")) {
            return "Relationship";
        }
        
        // Salary category (income)
        if (textLower.contains("salary") || textLower.contains("lương") ||
            textLower.contains("wage") || textLower.contains("pay")) {
            return "Salary";
        }
        
        // Business category (income)
        if (textLower.contains("business") || textLower.contains("kinh doanh") ||
            textLower.contains("bán hàng") || textLower.contains("sales")) {
            return "Business";
        }
        
        // Gifts category (income)
        if (textLower.contains("gifts") || textLower.contains("quà tặng") ||
            textLower.contains("bonus") || textLower.contains("thưởng")) {
            return "Gifts";
        }
        
        // Default to Others if no category found
        android.util.Log.d("ImportFragment", "No specific category found, using Others");
        return "Others";
    }

    /**
     * Parse numeric date string and set selectedDate
     */
    private void parseAndSetDate(String dateString) {
        try {
            SimpleDateFormat sdf;
            if (dateString.contains("/")) {
                sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            } else if (dateString.contains("-")) {
                sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            } else {
                android.util.Log.e("ImportFragment", "Unknown date format: " + dateString);
                return;
            }
            
            selectedDate.setTime(sdf.parse(dateString));
            updateDateDisplay();
        } catch (Exception e) {
            android.util.Log.e("ImportFragment", "Error parsing date: " + dateString, e);
        }
    }
    
    /**
     * Parse English text date string and set selectedDate
     * Supports formats like:
     * - "18 September 2025"
     * - "September 18, 2025"
     */
    private void parseAndSetDateText(String dateString) {
        try {
            SimpleDateFormat sdf;
            
            // Determine which format based on pattern
            if (dateString.matches("^\\d{1,2}\\s+\\w+\\s+\\d{4}$")) {
                // Format: "18 September 2025"
                sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
            } else if (dateString.matches("^\\w+\\s+\\d{1,2},?\\s+\\d{4}$")) {
                // Format: "September 18, 2025" or "September 18 2025"
                sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);
            } else {
                android.util.Log.e("ImportFragment", "Unknown text date format: " + dateString);
                return;
            }
            
            // Remove extra comma if present
            String cleanedDate = dateString.replace(",", " ").replaceAll("\\s+", " ").trim();
            if (sdf.toPattern().contains(",")) {
                cleanedDate = dateString.trim();
            }
            
            selectedDate.setTime(sdf.parse(cleanedDate));
            updateDateDisplay();
            android.util.Log.d("ImportFragment", "Successfully parsed text date: " + dateString);
        } catch (Exception e) {
            android.util.Log.e("ImportFragment", "Error parsing text date: " + dateString, e);
        }
    }

    /**
     * Fill form with OCR extracted data
     */
    private void fillFormFromOCR(String type, String amount, String categoryName, String date) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Don't change transaction type, keep current selection
                
                // Set amount
                if (!amount.isEmpty()) {
                    amountInput.setText(amount);
                }
                
                // Set category by name (always set, defaults to Others)
                findAndSetCategory(categoryName);

                parseAndSetDate(date);
                
                // Show extracted data to user
                android.util.Log.d("data","Dữ liệu đã được trích xuất:\n" +
                        "Số tiền: " + (amount.isEmpty() ? "Chưa xác định" : amount) + "\n" +
                        "Danh mục: " + categoryName);
                        
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
}
