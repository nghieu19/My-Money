# OCR Implementation for ImportFragment

## Overview
Successfully integrated ML Kit Text Recognition OCR functionality into the main ImportFragment.java to allow users to capture transaction data from images using camera or gallery.

## Changes Made

### 1. ImportFragment.java

#### Added Imports:
- `android.app.AlertDialog` - For camera/gallery selection dialog
- `android.content.Intent` & `android.content.pm.PackageManager` - For camera/gallery intents and permissions
- `android.net.Uri` & `android.os.Environment` - For image URI handling
- `android.provider.MediaStore` - For camera and gallery access
- `androidx.activity.result.ActivityResultLauncher` - For modern result handling
- `androidx.core.content.ContextCompat` & `FileProvider` - For permissions and file provider
- `com.google.mlkit.vision.*` - For ML Kit text recognition
- `java.util.regex.*` - For pattern matching

#### Added Fields:
```java
private LinearLayout btnCamera;
private ActivityResultLauncher<Intent> cameraLauncher;
private ActivityResultLauncher<Intent> galleryLauncher;
private Uri imageUri;
private static final int CAMERA_PERMISSION_CODE = 100;
```

#### New Methods:

1. **setupOCRLaunchers()** - Initializes activity result launchers for camera and gallery
2. **setupOCRButton()** - Sets up the camera button click listener to show dialog
3. **checkCameraPermissionAndOpen()** - Checks and requests camera permission
4. **onRequestPermissionsResult()** - Handles permission results
5. **openCamera()** - Opens camera to capture image
6. **openGallery()** - Opens gallery to select image
7. **processImage(Uri)** - Processes image using ML Kit OCR
8. **handleExtractedText(String)** - Extracts transaction data from OCR text
9. **parseAndSetDate(String)** - Parses and sets the date from OCR text
10. **fillFormFromOCR()** - Fills the form with extracted data
11. **findAndSetCategory(String)** - Finds and sets category by name

#### Data Extraction Logic:

**Transaction Type:**
- Detects "chi tiêu"/"expense" → expense
- Detects "thu nhập"/"income" → income

**Amount:**
- Regex pattern: `(\\d+[,.]?\\d*)`
- Removes commas and extracts numeric values

**Date:**
- Regex pattern: `(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})`
- Supports both DD/MM/YYYY and DD-MM-YYYY formats

**Category:**
- Keyword matching for categories:
  - "food"/"ăn" → Food
  - "transport"/"xe" → Transport
  - "home"/"nhà" → Home
  - "entertainment"/"giải trí" → Entertainment
  - "salary"/"lương" → Salary
  - "business"/"kinh doanh" → Business
  - Default → Others

### 2. AndroidManifest.xml

#### Added Permissions:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

#### Added Features:
```xml
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
```

#### Added FileProvider:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### 3. file_paths.xml (New File)
Created `app/src/main/res/xml/file_paths.xml`:
```xml
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-path name="external_files" path="." />
    <external-files-path name="my_images" path="Pictures/" />
    <cache-path name="cache" path="." />
</paths>
```

### 4. build.gradle.kts

#### Added Dependency:
```kotlin
implementation("com.google.mlkit:text-recognition:16.0.0")
```

## How to Use

1. **Open Import Screen**: Navigate to the Import tab
2. **Click Camera Button**: Click the camera/OCR button (btnCamera)
3. **Choose Option**:
   - "Chụp ảnh" - Take a photo with camera
   - "Chọn từ thư viện" - Select from gallery
4. **Capture/Select Image**: Take photo or select image
5. **OCR Processing**: ML Kit automatically processes the image
6. **Auto-Fill**: Form fields are automatically filled with extracted data:
   - Transaction type (expense/income)
   - Amount
   - Date
   - Category
7. **Review & Save**: Review the auto-filled data and click Save

## Features

- ✅ Camera permission handling
- ✅ Camera capture functionality
- ✅ Gallery image selection
- ✅ ML Kit text recognition
- ✅ Smart data extraction (type, amount, date, category)
- ✅ Automatic form filling
- ✅ User feedback with Toast messages
- ✅ Error handling

## Requirements

- **Minimum SDK**: 27 (Android 8.1)
- **Camera Permission**: Required for camera capture
- **Storage Permission**: Required for gallery access (Android 12 and below)
- **Google Play Services**: ML Kit Text Recognition

## Testing Checklist

- [ ] Click camera button - dialog appears
- [ ] Select "Chụp ảnh" - camera opens
- [ ] Capture image - OCR processes successfully
- [ ] Select "Chọn từ thư viện" - gallery opens
- [ ] Select image - OCR processes successfully
- [ ] Verify extracted data fills form correctly
- [ ] Test permission denial - shows appropriate message
- [ ] Test with various receipt/transaction images
- [ ] Test date parsing for different formats
- [ ] Test amount extraction with commas/periods
- [ ] Test category matching

## Notes

- The OCR accuracy depends on image quality and text clarity
- Currently supports Vietnamese and English keywords
- Date format supports DD/MM/YYYY and DD-MM-YYYY
- Category matching uses simple keyword detection (can be enhanced)
- Images are temporarily stored in app's external files directory
