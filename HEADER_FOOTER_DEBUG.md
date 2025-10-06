# Header and Footer Hiding - Debugging Guide

## Issue Resolution Steps

### Problem
The header and footer were not being hidden for NewWalletFragment and AddWalletFragment.

### Root Causes Identified and Fixed

1. **Missing `onDestroyView()` in AddWalletFragment**
   - The method was removed during manual edits
   - ✅ **Fixed**: Added back the method

2. **Timing Issue**
   - The visibility change was called before views were fully rendered
   - ✅ **Fixed**: Used `view.post()` to defer execution until after layout

3. **Missing Header Divider**
   - The divider line between header and content was not being hidden
   - ✅ **Fixed**: Added `header_divider` to the list of views to hide/show

4. **Added Comprehensive Logging**
   - Added debug logs to track when methods are called
   - Helps identify if views are null or if methods aren't being executed

## Updated Implementation

### MainActivity.java Changes

```java
// Added header divider reference
private View headerDivider;

// Initialize in onCreate()
headerDivider = findViewById(R.id.header_divider);

// Updated hideHeaderAndFooter()
private void hideHeaderAndFooter() {
    android.util.Log.d("MainActivity", "hideHeaderAndFooter called");
    if (headerLayout != null) {
        headerLayout.setVisibility(View.GONE);
    }
    if (headerDivider != null) {
        headerDivider.setVisibility(View.GONE);  // NEW
    }
    if (bottomNavigation != null) {
        bottomNavigation.setVisibility(View.GONE);
    }
}

// Updated showHeaderAndFooter()
private void showHeaderAndFooter() {
    android.util.Log.d("MainActivity", "showHeaderAndFooter called");
    if (headerLayout != null) {
        headerLayout.setVisibility(View.VISIBLE);
    }
    if (headerDivider != null) {
        headerDivider.setVisibility(View.VISIBLE);  // NEW
    }
    if (bottomNavigation != null) {
        bottomNavigation.setVisibility(View.VISIBLE);
    }
}
```

### NewWalletFragment.java Changes

```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    android.util.Log.d("NewWalletFragment", "onViewCreated called");
    
    initializeViews(view);
    setupListeners();
    
    // Use post() to defer until after layout
    view.post(() -> {
        android.util.Log.d("NewWalletFragment", "Attempting to hide header/footer");
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideMainHeaderAndFooter();
        }
    });
}

@Override
public void onDestroyView() {
    super.onDestroyView();
    
    android.util.Log.d("NewWalletFragment", "onDestroyView called - showing header/footer");
    
    if (getActivity() instanceof MainActivity) {
        ((MainActivity) getActivity()).showMainHeaderAndFooter();
    }
}
```

### AddWalletFragment.java Changes

```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    android.util.Log.d("AddWalletFragment", "onViewCreated called");
    
    initializeViews(view);
    setupListeners();
    
    // Use post() to defer until after layout
    view.post(() -> {
        android.util.Log.d("AddWalletFragment", "Attempting to hide header/footer");
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideMainHeaderAndFooter();
        }
    });
}

@Override
public void onDestroyView() {
    super.onDestroyView();
    
    android.util.Log.d("AddWalletFragment", "onDestroyView called - showing header/footer");
    
    if (getActivity() instanceof MainActivity) {
        ((MainActivity) getActivity()).showMainHeaderAndFooter();
    }
}
```

## Views Being Hidden/Shown

1. **header_layout** - The main header containing logo, title, wallet button, settings button
2. **header_divider** - The 1dp divider line below the header
3. **bottom_navigation** - The bottom navigation bar

## How to Debug

### Check Logcat for these messages:

1. **When opening NewWalletFragment:**
   ```
   NewWalletFragment: onViewCreated called
   NewWalletFragment: Attempting to hide header/footer
   MainActivity: hideHeaderAndFooter called
   MainActivity: Hiding header layout
   MainActivity: Hiding header divider
   MainActivity: Hiding bottom navigation
   ```

2. **When opening AddWalletFragment:**
   ```
   AddWalletFragment: onViewCreated called
   AddWalletFragment: Attempting to hide header/footer
   MainActivity: hideHeaderAndFooter called
   MainActivity: Hiding header layout
   MainActivity: Hiding header divider
   MainActivity: Hiding bottom navigation
   ```

3. **When navigating back:**
   ```
   [Fragment]Fragment: onDestroyView called - showing header/footer
   MainActivity: showHeaderAndFooter called
   MainActivity: Showing header layout
   MainActivity: Showing header divider
   MainActivity: Showing bottom navigation
   ```

### Error Messages to Look For:

- **"headerLayout is null!"** - View binding failed in MainActivity
- **"headerDivider is null!"** - View binding failed in MainActivity
- **"bottomNavigation is null!"** - View binding failed in MainActivity
- **"Activity is not MainActivity!"** - Fragment is in wrong activity

## Testing Checklist

- [ ] Open app and click "Add Wallet" button
- [ ] Verify main header disappears
- [ ] Verify header divider disappears
- [ ] Verify bottom navigation disappears
- [ ] Verify NewWalletFragment's custom header is visible
- [ ] Click on any wallet type (Cash/Bank/Virtual)
- [ ] Verify AddWalletFragment's custom header is visible
- [ ] Verify main header/footer still hidden
- [ ] Press back button from AddWalletFragment
- [ ] Verify back to NewWalletFragment, header/footer still hidden
- [ ] Press back button from NewWalletFragment
- [ ] Verify main header, divider, and footer reappear
- [ ] Check Logcat for all expected log messages

## Key Improvements

1. **Timing Fix**: Using `view.post()` ensures the visibility changes happen after the view hierarchy is fully laid out
2. **Complete Hiding**: Now hiding header, divider, AND footer for clean UI
3. **Debug Logging**: Easy to track what's happening and diagnose issues
4. **Null Safety**: Checks for null before setting visibility
