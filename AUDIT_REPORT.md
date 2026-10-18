# Audit & Codebase Health Report for Superb AI

> **Purpose:** This document is generated for **Superb AI** (or any advanced code-review AI) to inspect this codebase (`hitmanM340/pennywiseai-tracker`), verify architectural integrity, identify potential edge cases, and guide the next development tasks.

---

## 1. Codebase Overview & Baseline

- **Base Upstream**: [`sarim2000/pennywiseai-tracker`](https://github.com/sarim2000/pennywiseai-tracker) (latest `main`, Room schema `v66`, 355 commits ahead of legacy v50).
- **Current Database Version**: **Room Schema `v67`** (`SCHEMA_VERSION = 67`).
  - Upstream migrations `1..66` are intact and unaltered.
  - `MIGRATION_66_67` adds the `custom_parser_rules` table with composite index `(isEnabled, priority)`.
- **Target SDK**: Android 16 (API 36) / Compile SDK 37 / Min SDK 26 (Android 8.0+).
- **Kotlin**: 2.0.21 / Compose BOM: 2024.12.01 / AGP: 8.8.0.
- **Build Flavors**:
  - `standard`: Google Play distribution (In-App Review, In-App Updates, Google Play Billing).
  - `fdroid`: 100% FOSS distribution (no Google Play services, web-based license support).

---

## 2. Completed Fixes & Architectural Hardening

### A. Performance & In-Memory SMS Caching
- **Problem**: During inbox scans, evaluating custom rules previously queried SQLite (`customParserRuleDao.getEnabledRulesSync()`) and compiled regexes (`Regex(...)`) for every single unrecognized SMS, causing high battery drain and thread stalls.
- **Solution**:
  - Implemented an in-memory compiled rule cache with 10-second TTL in `SmsTransactionProcessor.kt`.
  - Encapsulated regex evaluation in `CompiledCustomRule` and `BuiltInBankFallbacks.kt`.
  - Added ReDoS protection: raw message truncation to 2,000 characters and `runCatching` around all regex matching.

### B. Security & Cloud-Restore Protection
- **Problem**: Android Auto-Backup (`backup_rules.xml`) previously allowed `database_key_prefs.xml` and SQLite files to be backed up to Google Drive. Restoring on a new device with new AndroidKeyStore keys resulted in `AEADBadTagException` / `GeneralSecurityException` crashes.
- **Solution**:
  - Explicitly excluded `database_key_prefs.xml`, `device_encryption_prefs.xml`, `pennywise_secure_prefs.xml`, and `pennywise_database` in both `backup_rules.xml` and `data_extraction_rules.xml`.
  - Wrapped `DatabaseKeyManager.kt` operations in `try-catch` with safe fallback defaults.
  - Added password `CharArray` zeroing in `finally` blocks for `BackupEncryptor.kt` (`password.fill('\u0000')`).
  - Offloaded PBKDF2 decryption (120,000 iterations) to `Dispatchers.Default` in `BackupImporter.kt`.

### C. Built-in Bank Fallbacks & Issue Triage
1. **Bank Fallbacks (#799, #881)**:
   - Added built-in regex fallbacks in `BuiltInBankFallbacks.kt` for **SVC Bank** and **Commercial Bank of Ceylon (ComBank LKR)**.
   - Added automatic reclassification for **LazyPay statement settlements**: converts `CREDIT` to `TRANSFER` to prevent artificial income inflation.
2. **Offline SMS Scanning (#881)**:
   - Configured `NetworkType.NOT_REQUIRED` in `SmsScanManager.kt` so SMS inbox scans run without internet.
   - Wrapped `CurrencyConversionService.kt` rate fetches in safe `try-catch` blocks to prevent unhandled network exceptions during offline execution.
3. **Loan Account Type & Debt Math (#792)**:
   - Added `AccountType.LOAN` to `ManageAccountsViewModel.kt` and `AccountTypeExtensions.kt`.
   - Updated `AccountBalanceRepository.kt`, `HomeViewModel.kt`, and `BalanceCalculator.kt` using `AccountBalanceEntity.isDebtAccount()` so loan/credit repayments reduce debt liabilities instead of inflating asset totals.
4. **Loan Excess Repayment (#876)**:
   - In `LoanRepository.kt`, when a repayment exceeds the outstanding balance, the excess amount flips the loan direction (`LENT` ↔ `BORROWED`) rather than being dropped.
5. **PennyWise AI Tab Visibility Toggle (#912)**:
   - Added user setting `showAiTab` in `UserPreferencesRepository.kt`.
   - Wired toggle to `SettingsScreen.kt`, `MainViewModel.kt`, and `PennyWiseBottomNavigation.kt` to allow hiding the AI chat interface on low-end devices.
6. **Transfers & Manual Entries (#911, #913)**:
   - Verified two-leg atomic transfer execution and balance updates in `AddTransactionUseCase.kt`.
   - Preserved exact merchant name casing (no forced uppercase/lowercase).
   - Filtered categories in manual entry based on transaction type (`INCOME` vs `EXPENSE`).

---

## 3. Verification & Build Gate Evidence

All of the following tests and builds have been executed and passed with 0 errors:

```bash
# 1. Standard flavor unit tests:
./gradlew testStandardDebugUnitTest -> BUILD SUCCESSFUL (All unit tests passed)

# 2. F-Droid flavor unit tests:
./gradlew testFdroidDebugUnitTest -> BUILD SUCCESSFUL (All unit tests passed)

# 3. Standard debug APK assembly:
./gradlew assembleStandardDebug -> BUILD SUCCESSFUL (APK generated)

# 4. F-Droid debug APK assembly:
./gradlew assembleFdroidDebug -> BUILD SUCCESSFUL (APK generated)
```

---

## 4. Key Areas for Superb AI to Inspect

Please review the following components for any subtle edge cases or optimization opportunities:

1. **`BuiltInBankFallbacks.kt`**:
   - Check if the regex patterns for SVC Bank and ComBank handle variations across old/new SMS templates and different currency notations.
   - Verify that LazyPay statement repayments cannot falsely trigger on standard merchant debit alerts.
2. **`LoanRepository.kt` (`recalculateRemaining`)**:
   - Verify edge cases where loan repayments occur out of chronological order or where partial repayments span multiple currencies.
3. **`SmsTransactionProcessor.kt`**:
   - Verify whether the 10-second TTL cache for custom parser rules correctly invalidates when a user adds, edits, or disables a rule in `CreateCustomRuleDialog.kt`.
4. **`BackupSchemaGuardTest.kt` & `BackupModels.kt`**:
   - Confirm that exporting and re-importing a backup with `custom_parser_rules` populates Room database without any duplicate primary key or foreign key collisions.
5. **Multi-Currency Aggregations**:
   - Check that foreign currency accounts with no known exchange rate in `CurrencyConversionService.kt` are displayed with their native currency tag and not summed 1:1 into home currency totals.
