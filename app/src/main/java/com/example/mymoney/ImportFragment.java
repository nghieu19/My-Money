package com.example.mymoney;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportFragment extends Fragment {

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private Uri imageUri;

    private static final int CAMERA_PERMISSION_CODE = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_import, container, false);

        LinearLayout btnCamera = view.findViewById(R.id.btnCamera);

        // Launcher cho Camera
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

        // Launcher cho Gallery
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

        // Khi bấm nút
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

        return view;
    }

    // --------- Permission ---------
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

    // --------- Camera & Gallery ---------
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = new File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp.jpg");
        imageUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraLauncher.launch(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    // --------- OCR xử lý ảnh ---------
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
        }
    }

    // --------- Tách dữ liệu từ text ---------
    private void handleExtractedText(String text) {
        String type = "";
        String category = "";
        String date = "";
        String amount = "";

        // 1. Loại giao dịch
        if (text.toLowerCase().contains("chi tiêu") || text.toLowerCase().contains("expense")) {
            type = "Chi tiêu";
        } else if (text.toLowerCase().contains("thu nhập") || text.toLowerCase().contains("income")) {
            type = "Thu nhập";
        }

        // 2. Số tiền
        Pattern p = Pattern.compile("(\\d+[,.]?\\d*)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            amount = m.group(1);
        }

        // 3. Ngày giao dịch
        Pattern datePattern = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})");
        Matcher dateMatcher = datePattern.matcher(text);
        if (dateMatcher.find()) {
            date = dateMatcher.group(1);
        }

        // 4. Danh mục
        if (text.toLowerCase().contains("study")) category = "Study";
        else if (text.toLowerCase().contains("work")) category = "Work";
        else if (text.toLowerCase().contains("travel")) category = "Travel";
        else category = "Khác";

        // Hiện dữ liệu ra (TODO: có thể gán vào EditText thay vì Toast)
        fillForm(type, amount, category, date);
    }

    private void fillForm(String type, String amount, String category, String date) {
        Toast.makeText(requireContext(),
                "Loại: " + type +
                        "\nSố tiền: " + amount +
                        "\nDanh mục: " + category +
                        "\nNgày: " + date,
                Toast.LENGTH_LONG).show();
    }
}
