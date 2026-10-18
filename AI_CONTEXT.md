# PennyWise AI Tracker — AI Model Handoff Context & Architecture Guide

> **Notice for AI Models:** This document is the primary architectural handoff for `hitmanM340/pennywiseai-tracker`. Read this file alongside `CLAUDE.md` and `AGENTS.md` before analyzing code or making modifications.

---

## 1. Executive Summary & Branch State

- **Upstream Repository**: [`sarim2000/pennywiseai-tracker`](https://github.com/sarim2000/pennywiseai-tracker) (Upstream base: Room schema `v66`, 355 commits ahead of v50).
- **Fork Repository**: [`hitmanM340/pennywiseai-tracker`](https://github.com/hitmanM340/pennywiseai-tracker).
- **Current Database Version**: **Room Schema `v67`** (`SCHEMA_VERSION = 67`).
- **Target SDK**: Android 16 (API 36) / Compile SDK 37 / Min SDK 26 (Android 8.0+).
- **Flavors**: `standard` (Google Play with in-app review/billing) and `fdroid` (fully FOSS, tip jar).

---

## 2. Integrated Custom Features (Preserved from Fork)

The following 3 custom features have been upgraded and integrated into Sarim's modern upstream architecture:

### 1. Dynamic Custom SMS Parser Rules (Room v67)
- **Entity**: `app/src/main/java/com/pennywiseai/tracker/data/database/entity/CustomParserRuleEntity.kt`
- **DAO**: `app/src/main/java/com/pennywiseai/tracker/data/database/dao/CustomParserRuleDao.kt`
- **Migration**: `MIGRATION_66_67` in `PennyWiseDatabase.kt` and `DatabaseModule.kt`. Creates `custom_parser_rules` table with index on `(isEnabled, priority)`.
- **Processing Guard**: `SmsTransactionProcessor.kt` evaluates active custom rules if static bank parsers return `null`. Hardened with 2,000-character ReDoS input cap, regex compilation exception isolation (`runCatching`), and amount sanity checking.
- **UI Dialog**: Material 3 V2 token compliant `CreateCustomRuleDialog.kt` in `ui/screens/unrecognized/`, wired to `UnrecognizedSmsScreen.kt` and `UnrecognizedSmsViewModel.kt`.

### 2. Modern Encrypted Backups (`PWENC1` AES-GCM + PBKDF2)
- **Engine**: `BackupEncryptor.kt` in `data/backup/`.
  - Derives 256-bit AES key via `PBKDF2WithHmacSHA256` with 120,000 iterations.
  - Generates 16-byte cryptographically secure salt + 12-byte initialization vector (IV).
  - Encrypts via `AES/GCM/NoPadding` with authentication tag.
  - Prepends 6-byte magic identifier `PWENC1` to prevent corrupted password attempts.
- **Full Schema Compatibility**: Integrated into `BackupExporter.kt`, `BackupImporter.kt`, and `BackupModels.kt`.
- **Receipts & Multi-table Support**: Properly covers receipt image bundles (`receipts/`), upstream tables (`goals`, `tags`, `recurringTransactions`, `merchantAliases`, `webhooks`), and custom parser rules without data loss.
- **Android Auto-Backup Safety**: `pennywise_secure_prefs.xml` excluded from cloud auto-backup in `backup_rules.xml` and `data_extraction_rules.xml` to avoid keystore invalidation on fresh restores.

### 3. Google Pay SMS Parser (`GPayParser`)
- **Parser**: `parser-core/src/main/kotlin/com/pennywiseai/parser/core/bank/GPayParser.kt`.
- **Registry**: Registered in `BankParserFactory.parsers`.
- **Tests**: Full test coverage in `parser-core/src/test/kotlin/com/pennywiseai/parser/core/bank/GPayParserTest.kt`.

### 4. High-Performance SMS Processing & Built-In Bank Fallbacks
- **Rule Caching**: Pre-compiled `CompiledCustomRule` in-memory cache with 10-second TTL in `SmsTransactionProcessor.kt` eliminating N+1 database queries and repeated regex compilations during bulk scans.
- **BuiltInBankFallbacks**: Added `BuiltInBankFallbacks.kt` supporting:
  - **SVC Bank**: Debit/credit SMS parsing.
  - **Commercial Bank of Ceylon (ComBank)**: LKR debit and credit formats.
  - **LazyPay Repayment Reclassification (#799)**: Automatically reclassifies statement bill settlements (`CREDIT` → `TRANSFER`).
- **Offline SMS Scanning (#881)**: `SmsScanManager.kt` uses `NetworkType.NOT_REQUIRED` so SMS reading never blocks when offline, and `CurrencyConversionService.kt` catches network exceptions cleanly.

### 5. Multi-Account & Debt Accounting Enhancements
- **Loan Account Type (#792)**: Added `LOAN` account type to `ManageAccountsViewModel.kt`, `AccountTypeExtensions.kt`, and UI pickers.
- **Debt & Liability Math**: Integrated `isDebtAccount()` across `HomeViewModel.kt`, `BalanceCalculator.kt`, and `AccountBalanceRepository.kt` so loan/credit repayments correctly reduce debt rather than inflating asset balances.
- **Loan Excess Repayment Direction Flipping (#876)**: In `LoanRepository.kt`, overpayments flip the loan direction (LENT ↔ BORROWED) for the surplus amount instead of discarding the remainder.
- **PennyWise AI Tab Visibility Toggle (#912)**: Added toggle in Settings (`UserPreferencesRepository.showAiTab`) to hide the AI assistant tab on lower-end devices.
- **Secure Password Zeroing & Cloud Restore Safeguards**: Memory hardening in `BackupEncryptor.kt` with `password.fill('\u0000')` and auto-backup exclusions for `database_key_prefs.xml`, `device_encryption_prefs.xml`, and `pennywise_database`.

---

## 3. Strict Development Invariants for AI Models

When implementing subsequent features or fixes, you **must adhere to these invariants**:

1. **Room Schema Invariant**:
   - If you modify any Room entity, you **must**:
     - Increment `PennyWiseDatabase.SCHEMA_VERSION` (next would be `68`).
     - Write a strict `MIGRATION_X_Y` in `PennyWiseDatabase.kt`.
     - Register the migration in `PennyWiseDatabase.ALL_MIGRATIONS` and `DatabaseModule.kt`.
     - Ensure schema JSON files written to `app/schemas/...` **NEVER contain a UTF-8 Byte Order Mark (BOM)** (Room KSP parser fails if BOM exists).
2. **Multi-Flavor Compatibility**:
   - The app has two flavors: `standard` and `fdroid`.
   - Never import Play Store billing or update classes directly in `app/src/main/`. Use the abstractions in `app/src/standard/` and `app/src/fdroid/`.
   - Always verify both flavors build: `./gradlew assembleStandardDebug assembleFdroidDebug`.
3. **UI / Material 3 Tokens**:
   - Upstream removed legacy v50 components (`BalanceHistoryDialog`, `AccountBalancesCard`, `PennyWiseCard`).
   - Use Material 3 V2 design tokens: `PennyWiseCardV2`, `ListItemCardV2`, `SectionHeaderV2`, and `TextStyles.kt`.
4. **SAF / Auto-Backup Safety**:
   - Do **NOT** reintroduce `AutoBackupWorker`. Background automated backups must go through upstream's SAF mechanism (`ScheduledFolderBackupScheduler` and `ScheduledFolderBackupWorker`).

---

## 4. Suggested Next Roadmap & Extension Opportunities

Here are candidate tasks and feature expansions ready for implementation:

1. **Custom Rules Management Screen**:
   - Create a dedicated "Custom Rules" screen under **Settings → Rules → SMS Rules**.
   - List all saved custom rules with toggles (`isEnabled`), drag-to-reorder (`priority`), edit dialog, and swipe-to-delete.
2. **Rule Test Bench / Regex Simulator**:
   - An interactive tester inside `CreateCustomRuleDialog` allowing users to paste a raw SMS message and see real-time extraction results for Merchant, Amount, and Account Last 4 digits.
3. **Rule Export & Import (JSON Sharing)**:
   - Allow users to export custom rules as a `.json` file to share with community members or other devices.
4. **Automated Parser Health Diagnostics**:
   - Telemetry/local log counter for unrecognized SMS frequency to identify banks needing new or updated parser regexes.
