# CLAUDE.md — PennyWise AI Tracker Architecture & Agent Guide

PennyWise AI Tracker is a privacy-first, 100% offline Android expense tracker built with Kotlin, Jetpack Compose (Material 3), Room, Hilt, WorkManager, Glance Widgets, and on-device MediaPipe LLM (Qwen 2.5).

---

## 1. Quick Build, Test & Verification Commands

Before running Gradle commands, ensure JDK 17+ is active.

```bash
# Compile & run unit tests (Standard Play flavor)
./gradlew testStandardDebugUnitTest

# Compile & run unit tests (F-Droid flavor — no Play Billing / proprietary deps)
./gradlew testFdroidDebugUnitTest

# Export & verify Room schemas + Backup schema guards
./gradlew testStandardDebugUnitTest --tests "com.pennywiseai.tracker.data.backup.*"
./gradlew testStandardDebugUnitTest --tests "com.pennywiseai.tracker.data.manager.DynamicParserRuleTest"

# Assemble both debug flavors to verify flavor source sets compile cleanly
./gradlew assembleStandardDebug assembleFdroidDebug
```

**Mandatory Pre-Commit Gate for AI Agents:**

1. Both `standardDebug` and `fdroidDebug` must compile without errors.
2. Any change to `@Entity` classes in `app/src/main/java/com/pennywiseai/tracker/data/database/entity/` **must** include:
   - A new numbered `Migration(N, N+1)` in `PennyWiseDatabase.kt`
   - Registration of that migration in `DatabaseModule.kt`
   - Exported Room schema JSON in `app/schemas/com.pennywiseai.tracker.data.database.PennyWiseDatabase/{N+1}.json`
   - Updated snapshot serialization in `BackupModels.kt`, `BackupExporter.kt`, `BackupImporter.kt`, and `BackupSchemaGuardTest.kt`.

---

## 2. Repository & Flavor Structure

- **`app/src/main/`**: Shared application code, Room DB, SMS/PDF/CSV parsers, domain rules, Jetpack Compose UI, Glance widgets, and WorkManager workers.
- **`app/src/standard/`**: Google Play flavor (`PlayBillingGateway.kt`, Play `InAppReviewManager.kt`, Play `InAppUpdateManager.kt`, `BillingModule.kt`).
- **`app/src/fdroid/`**: FOSS F-Droid flavor (`FdroidBillingGateway.kt`, no-op review/update managers, `SupportWebOption.kt` for web license keys).
- **`app/src/debug/`**: Debug tools (`LlmProbeReceiver.kt`, `UiVerificationActivity.kt`).
- **`parser-core/`**: Multi-bank SMS parsing library (160+ banks across 25+ countries).

---

## 3. Core Architecture & Data Flow

### 3.1 SMS & Notification Ingestion Pipeline

1. **Entry Points**:
   - Real-time SMS: `SmsBroadcastReceiver`
   - Historical / periodic scan: `OptimizedSmsReaderWorker` (configured via `SmsScanManager` & `SmsScanParamsCalculator`)
   - Push notifications: `BankNotificationListenerService` + `BankNotificationRetryWorker`

2. **Parsing & Custom Fallback (`SmsTransactionProcessor`)**:
   - First attempts compiled bank parsers from `parser-core` (`BankParserFactory`).
   - If unrecognized, evaluates user-defined regex rules from `CustomParserRuleDao` (`custom_parser_rules` table, guarded against invalid regex & ReDoS).
   - If still unrecognized, logs to `UnrecognizedSmsDao` so users can inspect or create a custom parser rule via `CreateCustomRuleDialog`.

3. **Deduplication & Post-Processing**:
   - `TransactionDeduplication` prevents duplicate SMS/notification/statement entries.
   - `RuleEngine` evaluates `TransactionRule` conditions (`AND`/`OR`, including `BankName||Last4` account keys) and executes actions (`SET`, `APPEND`, `PREPEND`, `ADD_TAG`, `REMOVE_TAG`, terminal `BLOCK`).
   - `AccountBalanceRepository` updates account balances and detects discrepancies (`DetectBalanceDiscrepancyUseCase` / `BalanceDiscrepancy.kt`).
   - `WebhookSyncScheduler` / `WebhookSyncWorker` queues outgoing webhook payloads (`WebhookPayloadBuilder`, `WebhookDeliveryService`).
   - `WidgetRefresher` updates Glance home screen widgets (`BudgetWidget`, `RecentTransactionsWidget`, `CategoryPieWidget`, `GoalWidgets`).

### 3.2 Database & Migration Architecture (`PennyWiseDatabase.kt`)

- **Current Schema Version**: `67` (`1.json` .. `66.json` from upstream + `67.json` for `CustomParserRuleEntity`).
- **Core Entities**:
  - Financial: `TransactionEntity`, `TransactionSplitEntity`, `AccountBalanceEntity`, `CardEntity`, `SubscriptionEntity`, `RecurringTransactionEntity`
  - Organization: `CategoryEntity` (supports parent/sub-categories & hidden flag), `TagEntity`, `TransactionGroupEntity`, `MerchantMappingEntity`, `MerchantAliasEntity`, `ProfileEntity`
  - Planning: `BudgetEntity`, `BudgetCategoryEntity`, `BudgetMonthSnapshotEntity`, `BudgetCategoryMonthSnapshotEntity`, `GoalEntity`, `GoalContributionEntity`, `LoanEntity`
  - Automation & AI: `RuleEntity`, `RuleApplicationEntity`, `CustomParserRuleEntity`, `ChatMessage`, `WebhookConfigEntity`, `WebhookQueueEntity`, `UnrecognizedSmsEntity`, `BankNotificationEntity`, `ExchangeRateEntity`

- **Migration Rules**:
  - Never modify an existing `MIGRATION_X_Y`. Always append a new migration.
  - Use `CREATE TABLE IF NOT EXISTS` and `ALTER TABLE ... ADD COLUMN` with explicit `DEFAULT` values for non-null columns.

### 3.3 Money, Currency & Math Rules

- **Never use `Double` or `Float` for monetary calculations.** Always use `BigDecimal` or the `Money` / `MoneyTotals` value classes (`utils/Money.kt`, `utils/MoneyTotals.kt`).
- Multi-currency aggregation must go through `CurrencyConversionService` and `CurrencyGroupedTotals`. Foreign amounts without a valid exchange rate must be skipped or grouped separately—never summed at 1:1 face value.
- Loans enforce `LoanCurrencyRules`: loans and repayments cannot be merged across mismatched currencies.
- Number formatting respects `NumberFormatStyle` (`AUTO`, `INDIAN`, `INTERNATIONAL`) via `CurrencyFormatter.kt`.

### 3.4 Backup, Encryption & SAF Folder Sync

- **Manual & Scheduled Backups**:
  - `BackupExporter` + `BackupImporter` + `BackupReceipts` serialize the full Room database + receipt images into `.pennywisebackup` archives.
  - `FolderBackupWriter` + `ScheduledFolderBackupScheduler` + `ScheduledFolderBackupWorker` handle periodic SAF folder backups (do **not** introduce duplicate WorkManager backup workers).

- **Encryption**:
  - `BackupEncryptor` provides password-based `AES/GCM/NoPadding` (`PBKDF2WithHmacSHA256`, 120k iterations, `PWENC1` magic header) for encrypted backups.
  - `DeviceEncryption` encrypts local secrets (webhook headers via `WebhookHeaderEncoder`, license tokens).
  - Never allow `AndroidKeyStore`-wrapped keys or encrypted SQLite files to be exported without their key in `backup_rules.xml` / `data_extraction_rules.xml`.

### 3.5 On-Device AI Assistant & Entitlements

- **AI Stack**: `LlmServiceImpl` + `PennyWiseTools` + `PendingChatAction` + `TransactionDraft`. Models are downloaded via `ModelDownloader` and `ResumableFetch`.
- **Entitlements & Billing**: `EntitlementGate`, `FreeTierLimits`, `PurchaseGateway` (implemented by `PlayBillingGateway` in `standard` and `FdroidBillingGateway` in `fdroid`), plus direct web license key validation in `billing/license/` (`LicenseClient`, `LicenseManager`, `LicensePolicy`).

---

## 4. UI & Design System V2 Guidelines

To prevent visual inconsistencies across screens:

1. **Use V2 Card & Section Components**:
   - Prefer `PennyWiseCardV2`, `ListItemCardV2`, `SectionHeaderV2`, `SummaryCardV2`, `GroupedList`, and `UnifiedAccountsCard` over legacy card wrappers.
   - Do **not** reintroduce deprecated v50 components (`PennyWiseCard`, `AccountBalancesCard`, `CreditCardsCard`, `BalanceHistoryDialog`).

2. **Theme, Typography & Spacing Tokens**:
   - Always use `Spacing` (`Spacing.xs`, `Spacing.sm`, `Spacing.md`, `Spacing.lg`), `Dimensions`, and `TextStyles` (`ui/theme/TextStyles.kt`) instead of hardcoded `.dp` or `.sp` values.
   - Respect `ThemeStyle` (including **AMOLED pitch-black mode**), `AccentColor`, `AppFont`, `CoverStyle`, and `NavBarStyle`.
   - Use semantic surface tokens (`MaterialTheme.colorScheme.surfaceContainer`, `surfaceContainerHigh`, `onSurfaceVariant`) so cards and sticky headers remain legible in both dark and AMOLED modes.

3. **Edge-to-Edge & Keyboard Safety**:
   - All bottom sheets, dialogs with text inputs, and full-screen forms must handle window insets (`navigationBarsPadding()`, `imePadding()`) and wrap multi-field dialog bodies in `verticalScroll(rememberScrollState())`.

4. **Localization**:
   - User-visible strings belong in `app/src/main/res/values/strings.xml` (Crowdin-managed) and should be surfaced from ViewModels via `UiText`.
