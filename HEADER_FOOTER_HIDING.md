# Header and Footer Hiding for Wallet Creation Fragments

## Summary
Implemented functionality to hide the main header and footer when showing NewWalletFragment and AddWalletFragment, and prevent the header title from updating to "New Wallet" or "Add Wallet".

## Changes Made

### MainActivity.java
1. **Added new instance variables:**
   - `private LinearLayout headerLayout;` - Reference to the main header layout
   - `private View bottomNavigation;` - Reference to the bottom navigation bar

2. **Initialized views in onCreate():**
   ```java
   headerLayout = findViewById(R.id.header_layout);
   bottomNavigation = findViewById(R.id.bottom_navigation);
   ```

3. **Added visibility control methods:**
   - `hideHeaderAndFooter()` - Hides both header and footer
   - `showHeaderAndFooter()` - Shows both header and footer
   - `hideMainHeaderAndFooter()` - Public method for fragments to hide header/footer
   - `showMainHeaderAndFooter()` - Public method for fragments to show header/footer

4. **Updated fragment loading:**
   - Modified `addWalletItem.setOnClickListener()` to NOT update the header title when loading NewWalletFragment
   - The fragment itself now controls the header/footer visibility

5. **Updated loadFragment methods:**
   - Both `loadFragment()` and `loadFragmentWithBackStack()` now call `showHeaderAndFooter()` to ensure normal fragments show the header/footer

### NewWalletFragment.java
1. **Added lifecycle methods:**
   - `onViewCreated()` - Calls `hideMainHeaderAndFooter()` when fragment is displayed
   - `onDestroyView()` - Calls `showMainHeaderAndFooter()` when fragment is destroyed/navigated away

### AddWalletFragment.java
1. **Added lifecycle methods:**
   - `onViewCreated()` - Calls `hideMainHeaderAndFooter()` when fragment is displayed
   - `onDestroyView()` - Calls `showMainHeaderAndFooter()` when fragment is destroyed/navigated away

## How It Works

1. **When user clicks "Add Wallet" in wallet panel:**
   - MainActivity loads NewWalletFragment without updating the header title
   - NewWalletFragment's `onViewCreated()` hides the main header and footer
   - User sees only the NewWalletFragment's own header from `activity_new_wallet.xml`

2. **When user selects a wallet type (cash/bank/virtual):**
   - NewWalletFragment loads AddWalletFragment
   - AddWalletFragment's `onViewCreated()` hides the main header and footer
   - User sees only the AddWalletFragment's own header from `activity_add_wallet.xml`

3. **When user navigates back:**
   - Fragment's `onDestroyView()` is called
   - Main header and footer are shown again
   - Normal fragments (Home, History, etc.) continue to show the main header/footer

## Benefits

- **Clean UI:** Wallet creation screens have their own custom headers without interference from the main app header
- **No title confusion:** The main header title doesn't incorrectly show "New Wallet" or "Add Wallet"
- **Automatic restoration:** Header/footer automatically reappear when navigating away from wallet creation screens
- **Consistent behavior:** Works correctly with back button navigation and fragment lifecycle

## Testing

Test the following scenarios:
1. ✓ Click "Add Wallet" - main header/footer should be hidden
2. ✓ Select wallet type - main header/footer should remain hidden
3. ✓ Press back button - main header/footer should reappear
4. ✓ Save wallet - main header/footer should reappear
5. ✓ Navigate to other fragments - main header/footer should be visible
