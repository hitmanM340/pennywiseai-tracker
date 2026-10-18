# AGENTS.md — Instructions for Autonomous Coding Agents

This file defines the operational rules for AI coding agents (Claude Code, Cursor, GitHub Copilot Workspace, OpenAI Codex, Windsurf, etc.) working in `pennywiseai-tracker`.

Read **`CLAUDE.md`** first for full architectural details. Follow the checklist below on every task.

---

## 1. Golden Rules (Never Violate)

1. **Both Flavors Must Compile**:
   - Every change touching billing, settings, DI, or manifests must compile in both `standard` (Google Play) and `fdroid` (FOSS) flavors:
     `./gradlew assembleStandardDebug assembleFdroidDebug`
2. **Strict Room Migration Discipline**:
   - Current Room schema version is **`67`** (`PennyWiseDatabase.kt`).
   - Upstream owns migrations `1..66`. Local custom extensions (`CustomParserRuleEntity`) start at `67+`.
   - When syncing/rebasing with `upstream/main`: if upstream adds new migrations (e.g., `67..N`), shift local custom migrations to `N+1` and regenerate the schema JSON.
3. **Backup Round-Trip Integrity**:
   - Every Room entity must be represented in `BackupModels.kt`, exported in `BackupExporter.kt`, restored in `BackupImporter.kt`, and verified by `BackupSchemaGuardTest.kt`.
   - New fields in `BackupModels.kt` **must** have default values (e.g., `= emptyList()` or `= null`) so older `.pennywisebackup` files import without crashing.
   - Preserve `BackupEncryptor` (`PWENC1` AES-GCM wrapper) and `BackupReceipts` (ZIP receipt bundling).
4. **Financial Precision**:
   - Use `BigDecimal`, `Money`, and `MoneyTotals` for all currency math.
   - Exclude loan-linked transactions from budget spend calculations and AI spend summaries unless explicitly requested.
   - Never sum unconverted foreign-currency amounts into base-currency totals.
5. **Regex & Parser Safety**:
   - Any regex evaluated against SMS bodies (especially user-defined rules in `CustomParserRuleDao` or `RuleCondition`) must be wrapped in `runCatching` and guarded against empty capture groups and ReDoS on long messages.

---

## 2. Design System & UI Consistency Checklist

- **Cards & Lists**: Use `PennyWiseCardV2`, `ListItemCardV2`, `SectionHeaderV2`, `SummaryCardV2`, and `GroupedList` (`ui/components/cards/`).
- **Empty & Error States**: Use `PennyWiseEmptyState` and `ErrorState` (`ui/components/`).
- **Spacing & Typography**: Use `Spacing.*`, `Dimensions.*`, and `TextStyles.*` (`ui/theme/`).
- **Dark & AMOLED Mode**: Test all custom colors and sticky headers against AMOLED mode (`surfaceContainer`, `surfaceContainerHigh`). Never hardcode `Color.White` or `Color.Black` for text or card backgrounds.
- **Dialogs & Sheets**: Always make multi-input dialogs scrollable (`verticalScroll`) and include live validation for regex/numeric inputs.

---

## 3. Verification Workflow Before Finishing Any Task

Run these commands and fix any failures before reporting completion:

```bash
# 1. Run unit test suite (parsers, rule engine, backup schema guard, encryption, viewmodels)
./gradlew testStandardDebugUnitTest

# 2. Verify F-Droid FOSS flavor unit tests & compilation
./gradlew testFdroidDebugUnitTest
```
