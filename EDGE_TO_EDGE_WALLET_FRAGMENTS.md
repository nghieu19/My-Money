# Edge-to-Edge Support for Wallet Creation Fragments

## Issue
The colored headers in NewWalletFragment and AddWalletFragment were not extending into the status bar area, even though the app uses Edge-to-Edge mode.

## Solution
Added window insets handling to both fragments so their headers extend into the status bar with proper padding.

## Changes Made

### 1. activity_new_wallet.xml
- Added `android:id="@+id/wallet_header_layout"` to the header LinearLayout
- This allows the fragment to reference and adjust the header padding

### 2. activity_add_wallet.xml
- Added `android:id="@+id/wallet_header_layout"` to the header LinearLayout
- Same purpose as above

### 3. NewWalletFragment.java
- Added `ViewCompat.setOnApplyWindowInsetsListener()` in `onViewCreated()`
- Gets the system bars insets (status bar height)
- Applies top padding to the header layout equal to the status bar height
- This makes the colored header extend behind the status bar while keeping content visible

### 4. AddWalletFragment.java
- Added the same `ViewCompat.setOnApplyWindowInsetsListener()` in `onViewCreated()`
- Ensures consistent edge-to-edge behavior across both wallet creation fragments

## How It Works

```java
// Apply system window insets to make header extend into status bar
androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
    androidx.core.graphics.Insets systemBars = insets.getInsets(
        androidx.core.view.WindowInsetsCompat.Type.systemBars()
    );
    
    // Find the header layout and add top padding for status bar
    LinearLayout headerLayout = view.findViewById(R.id.wallet_header_layout);
    if (headerLayout != null) {
        headerLayout.setPadding(
            headerLayout.getPaddingLeft(),
            systemBars.top,  // Add status bar height as top padding
            headerLayout.getPaddingRight(),
            headerLayout.getPaddingBottom()
        );
    }
    
    return insets;
});
```

### What This Does:

1. **Gets System Insets**: Retrieves the height of the status bar and navigation bar
2. **Finds Header Layout**: Locates the mint green header in the fragment
3. **Applies Top Padding**: Adds padding equal to the status bar height
4. **Result**: The colored header extends behind the status bar, with content (back button, title) properly positioned below it

## Visual Result

**Before:**
```
[White gap - status bar]
[Mint green header]
```

**After:**
```
[Mint green extends into status bar]
[Content positioned below status bar]
```

The mint green color now fills the entire top area including the status bar, creating a seamless edge-to-edge experience that matches modern Android design guidelines.

## Consistency with MainActivity

This approach is consistent with how MainActivity handles edge-to-edge:
- MainActivity uses `ViewCompat.setOnApplyWindowInsetsListener()` on the main layout
- Wallet creation fragments use the same approach on their root views
- Both properly handle system insets for a unified edge-to-edge experience
