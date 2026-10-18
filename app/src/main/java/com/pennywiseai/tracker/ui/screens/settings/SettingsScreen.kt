package com.pennywiseai.tracker.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.pennywiseai.tracker.ui.effects.overScrollVertical
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.pennywiseai.tracker.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.ui.UiText
import com.pennywiseai.tracker.ui.components.CustomTitleTopAppBar
import com.pennywiseai.tracker.ui.components.SupportDevelopmentDialog
import com.pennywiseai.tracker.ui.components.cards.GroupedColumn
import com.pennywiseai.tracker.ui.components.cards.GroupedList
import com.pennywiseai.tracker.ui.components.cards.GroupedRow
import com.pennywiseai.tracker.ui.components.cards.IconTile
import com.pennywiseai.tracker.ui.components.cards.ListItemPosition
import com.pennywiseai.tracker.ui.components.cards.RowLabels
import com.pennywiseai.tracker.ui.components.cards.SectionHeaderV2
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.amber_light
import com.pennywiseai.tracker.ui.theme.amber_dark
import com.pennywiseai.tracker.ui.theme.orange_light
import com.pennywiseai.tracker.ui.theme.orange_dark
import com.pennywiseai.tracker.ui.theme.green_light
import com.pennywiseai.tracker.ui.theme.green_dark
import com.pennywiseai.tracker.ui.theme.teal_light
import com.pennywiseai.tracker.ui.theme.teal_dark
import com.pennywiseai.tracker.ui.theme.blue_light
import com.pennywiseai.tracker.ui.theme.blue_dark
import com.pennywiseai.tracker.ui.theme.indigo_light
import com.pennywiseai.tracker.ui.theme.indigo_dark
import com.pennywiseai.tracker.ui.theme.red_light
import com.pennywiseai.tracker.ui.theme.red_dark
import com.pennywiseai.tracker.ui.theme.pink_light
import com.pennywiseai.tracker.ui.theme.pink_dark
import com.pennywiseai.tracker.ui.theme.purple_light
import com.pennywiseai.tracker.ui.theme.purple_dark
import com.pennywiseai.tracker.ui.theme.cyan_light
import com.pennywiseai.tracker.ui.theme.cyan_dark
import com.pennywiseai.tracker.ui.theme.yellow_light
import com.pennywiseai.tracker.ui.theme.yellow_dark
import com.pennywiseai.tracker.ui.theme.grey_light
import com.pennywiseai.tracker.ui.theme.grey_dark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel
import com.pennywiseai.tracker.data.preferences.NumberFormatStyle
import com.pennywiseai.tracker.utils.CurrencyFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToUnrecognizedSms: () -> Unit = {},
    onNavigateToManageAccounts: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToLoans: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToTransactionGroups: () -> Unit = {},
    onNavigateToExchangeRates: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToImportStatement: () -> Unit = {},
    onNavigateToWebhooks: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    appLockViewModel: com.pennywiseai.tracker.ui.viewmodel.AppLockViewModel = hiltViewModel(),
    permissionViewModel: com.pennywiseai.tracker.ui.viewmodel.PermissionViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by settingsViewModel.downloadState.collectAsStateWithLifecycle()
    val downloadProgress by settingsViewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadedMB by settingsViewModel.downloadedMB.collectAsStateWithLifecycle()
    val totalMB by settingsViewModel.totalMB.collectAsStateWithLifecycle()
    val isDeveloperModeEnabled by settingsViewModel.isDeveloperModeEnabled.collectAsStateWithLifecycle(initialValue = false)
    val smsScanMonths by settingsViewModel.smsScanMonths.collectAsStateWithLifecycle(initialValue = 3)
    val smsScanAllTime by settingsViewModel.smsScanAllTime.collectAsStateWithLifecycle(initialValue = false)
    val smsScanUseCustomDate by settingsViewModel.smsScanUseCustomDate.collectAsStateWithLifecycle(initialValue = false)
    val smsScanCustomDate by settingsViewModel.smsScanCustomDate.collectAsStateWithLifecycle(initialValue = null)
    val baseCurrency by settingsViewModel.baseCurrency.collectAsStateWithLifecycle(initialValue = "")
    val numberFormatStyle by settingsViewModel.numberFormatStyle.collectAsStateWithLifecycle(initialValue = NumberFormatStyle.AUTO)
    val budgetCycleStartDay by settingsViewModel.budgetCycleStartDay.collectAsStateWithLifecycle(initialValue = 1)
    val importExportMessage by settingsViewModel.importExportMessage.collectAsStateWithLifecycle()
    val exportedBackupFile by settingsViewModel.exportedBackupFile.collectAsStateWithLifecycle()
<<<<<<< HEAD
    val deleteAllTransactionsCount by settingsViewModel.deleteAllTransactionsCount.collectAsStateWithLifecycle()
    val isDeletingAllTransactions by settingsViewModel.isDeletingAllTransactions.collectAsStateWithLifecycle()
    val deleteAllTransactionsResult by settingsViewModel.deleteAllTransactionsResult.collectAsStateWithLifecycle()
=======
    val backupDirectoryUri by settingsViewModel.backupDirectoryUri.collectAsStateWithLifecycle(initialValue = null)
    val monitoredBankPackages by settingsViewModel.monitoredBankPackages.collectAsStateWithLifecycle(initialValue = emptySet())
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
    val unifiedCurrencyMode by settingsViewModel.unifiedCurrencyMode.collectAsStateWithLifecycle(initialValue = false)
    val countCreditCardAsExpense by settingsViewModel.countCreditCardAsExpense.collectAsStateWithLifecycle(initialValue = false)
    val displayCurrency by settingsViewModel.displayCurrency.collectAsStateWithLifecycle(initialValue = "")
    val availableCurrencies by settingsViewModel.availableCurrencies.collectAsStateWithLifecycle()
    val accounts by settingsViewModel.accounts.collectAsStateWithLifecycle()
    val mainAccountKey by settingsViewModel.mainAccountKey.collectAsStateWithLifecycle()
    val showAiTab by settingsViewModel.showAiTab.collectAsStateWithLifecycle(initialValue = true)
    val useContactsForVpa by settingsViewModel.useContactsForVpa.collectAsStateWithLifecycle(initialValue = false)
    val isProEntitled by settingsViewModel.isProEntitled.collectAsStateWithLifecycle()
    val scheduledFolderBackupEnabled by settingsViewModel.scheduledFolderBackupEnabled.collectAsStateWithLifecycle(initialValue = false)
    val scheduledFolderBackupLastTimestamp by settingsViewModel.scheduledFolderBackupLastTimestamp.collectAsStateWithLifecycle(initialValue = null)
    val exportIncludeReceipts by settingsViewModel.exportIncludeReceipts.collectAsStateWithLifecycle(initialValue = true)
    val folderBackupIncludeReceipts by settingsViewModel.folderBackupIncludeReceipts.collectAsStateWithLifecycle(initialValue = false)
    val requestFolderPicker by settingsViewModel.requestFolderPicker.collectAsStateWithLifecycle()
    var showUpgradeSheet by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    // F-Droid builds have no Play billing, so they show a "Support development"
    // tip jar instead of the (un-buyable) Pro upsell. Play builds keep Pro.
    val isFdroidBuild = com.pennywiseai.tracker.BuildConfig.IS_FDROID_BUILD
    // Launches the runtime permission request. If granted, we flip the
    // preference on; if denied, leave the switch off so the user can try
    // again without us silently turning the feature on later.
    val readContactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) settingsViewModel.setUseContactsForVpa(true)
    }
    var showSmsScanDialog by remember { mutableStateOf(false) }
    var showSmsScanDatePicker by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showDisplayCurrencyDialog by remember { mutableStateOf(false) }
    var showNumberFormatDialog by remember { mutableStateOf(false) }
    var showBudgetCycleDialog by remember { mutableStateOf(false) }
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var showMainAccountDropdown by remember { mutableStateOf(false) }
    val permissionUiState by permissionViewModel.uiState.collectAsStateWithLifecycle()
    val isAutoBackupEnabled by settingsViewModel.isAutoBackupEnabled.collectAsStateWithLifecycle(initialValue = true)
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var exportPasswordText by remember { mutableStateOf("") }
    
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var importPasswordText by remember { mutableStateOf("") }
    var importPendingUri by remember { mutableStateOf<Uri?>(null) }
    
    var showSetBackupPasswordDialog by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    val hasNotificationAccess = permissionUiState.hasNotificationAccess
    val context = LocalContext.current
    val notificationAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        permissionViewModel.refreshNotificationAccess()
    }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                importPendingUri = it
                showImportPasswordDialog = true
            }
        }
    )

    // File picker for CSV transaction import
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.importCsv(it)
            }
        }
    )

    // File saver for export
    val exportSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.saveBackupToFile(it)
            }
        }
    )

<<<<<<< HEAD
    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let { settingsViewModel.onBackupFolderSelected(it) }
        }
    )

    LaunchedEffect(requestFolderPicker) {
        if (requestFolderPicker) {
            backupFolderLauncher.launch(null)
            settingsViewModel.onFolderPickerLaunched()
=======
    val backupDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.e("SettingsScreen", "Failed to take persistable URI permission", e)
            }
            settingsViewModel.updateBackupDirectoryUri(it.toString())
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
        }
    }

    // Scroll behaviors for collapsible TopAppBar
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = scrollBehaviorSmall
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = stringResource(R.string.settings_title),
                hasBackButton = true,
                navigationContent = { SettingsNavigationContent(onNavigateBack) },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // ── PennyWise Pro / Support development ──
            // Top of Settings on purpose: highest-discoverability slot.
            // F-Droid builds have no Play billing (everything is already
            // unlocked), so instead of an un-buyable Pro upsell they get a
            // "Support development" tip jar. Play builds keep the Pro upgrade.
            if (isFdroidBuild) {
                SectionHeaderV2(title = stringResource(R.string.support_title))
                SettingsGroup {
                    SettingsNavItem(
                        icon = Icons.Default.Favorite,
                        iconBgColor = yellow_light,
                        iconTint = yellow_dark,
                        title = stringResource(R.string.support_title),
                        subtitle = stringResource(R.string.support_subtitle),
                        onClick = { showSupportDialog = true },
                        position = ListItemPosition.Single,
                    )
                }
            } else {
                // Row content adapts to entitlement state — paid users see
                // "Active" so the row reads as status, free users see "Upgrade"
                // so it reads as a call-to-action.
                SectionHeaderV2(title = stringResource(R.string.settings_pro_section))
                SettingsGroup {
                    SettingsNavItem(
                        icon = Icons.Default.AutoAwesome,
                        iconBgColor = yellow_light,
                        iconTint = yellow_dark,
                        title = if (isProEntitled) stringResource(R.string.settings_pro_title_active) else stringResource(R.string.settings_pro_title_upgrade),
                        subtitle = if (isProEntitled) {
                            stringResource(R.string.settings_pro_subtitle_active)
                        } else {
                            stringResource(R.string.settings_pro_subtitle_upgrade)
                        },
                        onClick = { showUpgradeSheet = true },
                        position = ListItemPosition.Single,
                    )
                }
            }

            // ── Personalization ──
            SectionHeaderV2(title = stringResource(R.string.settings_personalization_section))
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.Palette,
                    iconBgColor = orange_light,
                    iconTint = orange_dark,
                    title = stringResource(R.string.settings_appearance_title),
                    subtitle = stringResource(R.string.settings_appearance_subtitle),
                    onClick = onNavigateToAppearance,
                    position = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ListItemPosition.Top else ListItemPosition.Single
                )
                // Per-app language is a system screen on Android 13+; older
                // versions follow the device language.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    SettingsNavItem(
                        icon = Icons.Default.Language,
                        iconBgColor = orange_light,
                        iconTint = orange_dark,
                        title = stringResource(R.string.settings_language_title),
                        subtitle = stringResource(R.string.settings_language_subtitle),
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                                    .setData(Uri.fromParts("package", context.packageName, null))
                            )
                        },
                        position = ListItemPosition.Bottom
                    )
                }
            }

            // ── Currency ──
            SectionHeaderV2(title = stringResource(R.string.settings_currency_section))
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.CurrencyExchange,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = stringResource(R.string.settings_unified_currency_title),
                    subtitle = stringResource(R.string.settings_unified_currency_subtitle),
                    checked = unifiedCurrencyMode,
                    onCheckedChange = { settingsViewModel.setUnifiedCurrencyMode(it) },
                    position = ListItemPosition.Top
                )
                AnimatedVisibility(visible = unifiedCurrencyMode) {
                    SettingsNavItem(
                        icon = Icons.Default.AttachMoney,
                        iconBgColor = teal_light,
                        iconTint = teal_dark,
                        title = stringResource(R.string.settings_display_currency_title),
                        subtitle = stringResource(R.string.settings_display_currency_subtitle),
                        onClick = { showDisplayCurrencyDialog = true },
                        position = ListItemPosition.Middle,
                        trailingText = "${CurrencyFormatter.getCurrencySymbol(displayCurrency)} $displayCurrency"
                    )
                }
                SettingsNavItem(
                    icon = Icons.Default.SwapHoriz,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = stringResource(R.string.settings_exchange_rates_title),
                    subtitle = stringResource(R.string.settings_exchange_rates_subtitle),
                    onClick = onNavigateToExchangeRates,
                    position = ListItemPosition.Middle
                )
                SettingsSwitchRow(
                    icon = Icons.Default.CreditCard,
                    iconBgColor = indigo_light,
                    iconTint = indigo_dark,
                    title = stringResource(R.string.settings_card_spend_as_expense_title),
                    subtitle = stringResource(R.string.settings_card_spend_as_expense_subtitle),
                    checked = countCreditCardAsExpense,
                    onCheckedChange = { settingsViewModel.setCountCreditCardAsExpense(it) },
                    position = ListItemPosition.Middle
                )
                SettingsDropdownItem(
                    icon = Icons.Default.Flag,
                    iconBgColor = indigo_light,
                    iconTint = indigo_dark,
                    title = stringResource(R.string.settings_default_currency_title),
                    subtitle = stringResource(R.string.settings_default_currency_subtitle),
                    currentValue = "${CurrencyFormatter.getCurrencySymbol(baseCurrency)} $baseCurrency",
                    expanded = showCurrencyDropdown,
                    onExpandedChange = { showCurrencyDropdown = it },
                    position = ListItemPosition.Middle
                ) {
                    availableCurrencies.forEach { currency ->
                        DropdownMenuItem(
                            text = {
                                Text("${CurrencyFormatter.getCurrencySymbol(currency)} $currency")
                            },
                            onClick = {
                                settingsViewModel.updateBaseCurrency(currency)
                                showCurrencyDropdown = false
                            },
                            leadingIcon = if (currency == baseCurrency) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null
                        )
                    }
                }

                // Main account → sets the default currency (unless explicitly chosen above).
                if (accounts.isNotEmpty()) {
                    val mainAccount = accounts.firstOrNull {
                        "${it.bankName}_${it.accountLast4}" == mainAccountKey
                    }
                    SettingsDropdownItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconBgColor = purple_light,
                        iconTint = purple_dark,
                        title = stringResource(R.string.settings_main_account_title),
                        subtitle = stringResource(R.string.settings_main_account_subtitle),
                        currentValue = mainAccount?.let { acc ->
                            val name = acc.alias?.takeIf { it.isNotBlank() } ?: acc.bankName
                            AccountBalanceEntity.accountLabel(name, acc.accountLast4)
                        } ?: stringResource(R.string.settings_main_account_not_set),
                        expanded = showMainAccountDropdown,
                        onExpandedChange = { showMainAccountDropdown = it },
                        position = ListItemPosition.Middle
                    ) {
                        accounts.forEach { account ->
                            val name = account.alias?.takeIf { it.isNotBlank() } ?: account.bankName
                            val label = AccountBalanceEntity.accountLabel(name, account.accountLast4)
                            val key = "${account.bankName}_${account.accountLast4}"
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    settingsViewModel.setMainAccount(account)
                                    showMainAccountDropdown = false
                                },
                                leadingIcon = if (key == mainAccountKey) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }

                SettingsNavItem(
                    icon = Icons.Default.Numbers,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = stringResource(R.string.settings_number_format_title),
                    subtitle = stringResource(R.string.settings_number_format_subtitle),
                    onClick = { showNumberFormatDialog = true },
                    position = ListItemPosition.Bottom,
                    trailingText = numberFormatStyleLabel(numberFormatStyle)
                )
            }

            // ── Budget ──
            // The cycle start day is a budgeting concept, but it changes how
            // Home / Analytics bucket transactions, so it lives up here next
            // to the other "display" knobs rather than buried in Data
            // Management with the budgets list.
            SectionHeaderV2(title = stringResource(R.string.settings_budget_section))
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.DateRange,
                    iconBgColor = teal_light,
                    iconTint = teal_dark,
                    title = stringResource(R.string.settings_budget_cycle_title),
                    subtitle = stringResource(R.string.settings_budget_cycle_subtitle),
                    onClick = { showBudgetCycleDialog = true },
                    position = ListItemPosition.Single,
                    trailingText = ordinalSuffix(budgetCycleStartDay)
                )
            }

            // ── Contacts ──
            SectionHeaderV2(title = stringResource(R.string.settings_contacts_section))
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.Contacts,
                    iconBgColor = teal_light,
                    iconTint = teal_dark,
                    title = stringResource(R.string.settings_contacts_vpa_title),
                    subtitle = stringResource(R.string.settings_contacts_vpa_subtitle),
                    checked = useContactsForVpa,
                    onCheckedChange = { wantsOn ->
                        if (wantsOn) {
                            val alreadyGranted = androidx.core.content.ContextCompat
                                .checkSelfPermission(
                                    context,
                                    android.Manifest.permission.READ_CONTACTS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (alreadyGranted) {
                                settingsViewModel.setUseContactsForVpa(true)
                            } else {
                                readContactsLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                            }
                        } else {
                            settingsViewModel.setUseContactsForVpa(false)
                        }
                    },
                    position = ListItemPosition.Single
                )
            }

            // ── Security ──
            SectionHeaderV2(title = stringResource(R.string.settings_security_section))
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.Lock,
                    iconBgColor = red_light,
                    iconTint = red_dark,
                    title = stringResource(R.string.settings_app_lock_title),
                    subtitle = if (appLockUiState.canUseBiometric) {
                        stringResource(R.string.settings_app_lock_subtitle)
                    } else {
                        appLockUiState.biometricCapability.errorMessageRes?.let { stringResource(it) }.orEmpty()
                    },
                    checked = appLockUiState.isLockEnabled,
                    onCheckedChange = { appLockViewModel.setAppLockEnabled(it) },
                    enabled = appLockUiState.canUseBiometric,
<<<<<<< HEAD
                    position = if (appLockUiState.isLockEnabled) ListItemPosition.Top else ListItemPosition.Single
=======
                    position = ItemPosition.TOP
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
                )
                if (appLockUiState.isLockEnabled) {
                    SettingsNavItem(
                        icon = Icons.Default.Timer,
                        iconBgColor = pink_light,
                        iconTint = pink_dark,
                        title = stringResource(R.string.settings_lock_timeout_title),
                        subtitle = when (appLockUiState.timeoutMinutes) {
                            0 -> stringResource(R.string.settings_lock_timeout_immediately_subtitle)
                            else -> pluralStringResource(
                                R.plurals.settings_lock_timeout_after_minutes,
                                appLockUiState.timeoutMinutes,
                                appLockUiState.timeoutMinutes
                            )
                        },
                        onClick = { showTimeoutDialog = true },
<<<<<<< HEAD
                        position = ListItemPosition.Bottom
=======
                        position = ItemPosition.MIDDLE
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
                    )
                }
                SettingsSwitchRow(
                    icon = Icons.Default.Backup,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = "Automated Backups",
                    subtitle = "Automatically backup your data every 7 days",
                    checked = isAutoBackupEnabled,
                    onCheckedChange = { settingsViewModel.setAutoBackupEnabled(it) },
                    position = ItemPosition.MIDDLE
                )
                if (isAutoBackupEnabled) {
                    SettingsNavItem(
                        icon = Icons.Default.Password,
                        iconBgColor = purple_light,
                        iconTint = purple_dark,
                        title = "Auto-Backup Password",
                        subtitle = "Configure password for scheduled backups",
                        onClick = { showSetBackupPasswordDialog = true },
                        position = ItemPosition.MIDDLE
                    )
                }
                SettingsNavItem(
                    icon = Icons.Default.Folder,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = "Backup Location",
                    subtitle = backupDirectoryUri?.let {
                        try {
                            val parsedUri = Uri.parse(it)
                            parsedUri.lastPathSegment ?: parsedUri.path ?: it
                        } catch (e: Exception) {
                            it
                        }
                    } ?: "Default app storage",
                    onClick = { backupDirLauncher.launch(null) },
                    position = ItemPosition.BOTTOM
                )
            }

            // ── Data Management ──
            SectionHeaderV2(title = stringResource(R.string.settings_data_section))
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.AccountBalance,
                    iconBgColor = red_light,
                    iconTint = red_dark,
                    title = stringResource(R.string.settings_manage_accounts_title),
                    subtitle = stringResource(R.string.settings_manage_accounts_subtitle),
                    onClick = onNavigateToManageAccounts,
                    position = ListItemPosition.Top
                )
                SettingsNavItem(
                    icon = Icons.Default.Category,
                    iconBgColor = purple_light,
                    iconTint = purple_dark,
                    title = stringResource(R.string.settings_categories_title),
                    subtitle = stringResource(R.string.settings_categories_subtitle),
                    onClick = onNavigateToCategories,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.AutoAwesome,
                    iconBgColor = orange_light,
                    iconTint = orange_dark,
                    title = stringResource(R.string.settings_smart_rules_title),
                    subtitle = stringResource(R.string.settings_smart_rules_subtitle),
                    onClick = onNavigateToRules,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = stringResource(R.string.settings_budgets_title),
                    subtitle = stringResource(R.string.settings_budgets_subtitle),
                    onClick = onNavigateToBudgets,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.SwapHoriz,
                    iconBgColor = amber_light,
                    iconTint = amber_dark,
                    title = stringResource(R.string.settings_loans_title),
                    subtitle = stringResource(R.string.settings_loans_subtitle),
                    onClick = onNavigateToLoans,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.EventRepeat,
                    iconBgColor = green_light,
                    iconTint = green_dark,
                    title = stringResource(R.string.settings_recurring_title),
                    subtitle = stringResource(R.string.settings_recurring_subtitle),
                    onClick = onNavigateToRecurring,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.Folder,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = stringResource(R.string.settings_transaction_groups_title),
                    subtitle = stringResource(R.string.settings_transaction_groups_subtitle),
                    onClick = onNavigateToTransactionGroups,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.Upload,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
<<<<<<< HEAD
                    title = stringResource(R.string.settings_export_data_title),
                    subtitle = stringResource(R.string.settings_export_data_subtitle),
                    onClick = { settingsViewModel.exportBackup() },
                    position = ListItemPosition.Middle
                )
                SettingsSwitchRow(
                    icon = Icons.Default.Receipt,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = stringResource(R.string.settings_export_include_receipts_title),
                    subtitle = stringResource(R.string.settings_export_include_receipts_subtitle),
                    checked = exportIncludeReceipts,
                    onCheckedChange = { settingsViewModel.setExportIncludeReceipts(it) },
                    position = ListItemPosition.Middle
                )
                SettingsSwitchRow(
                    icon = Icons.Default.Backup,
                    iconBgColor = purple_light,
                    iconTint = purple_dark,
                    title = stringResource(R.string.settings_folder_backup_title),
                    subtitle = if (scheduledFolderBackupEnabled) {
                        stringResource(R.string.settings_folder_backup_subtitle_enabled)
                    } else if (!isProEntitled) {
                        stringResource(R.string.settings_folder_backup_subtitle_pro)
                    } else {
                        stringResource(R.string.settings_folder_backup_subtitle_disabled)
                    },
                    checked = scheduledFolderBackupEnabled,
                    // Scheduling daily backups is a Pro feature. Turning it ON while
                    // free routes to the paywall; turning it OFF is always allowed so
                    // a lapsed/downgraded user can still stop scheduled backups.
                    onCheckedChange = { enabled ->
                        if (enabled && !isProEntitled) {
                            showUpgradeSheet = true
                        } else {
                            settingsViewModel.setScheduledFolderBackupEnabled(enabled)
                        }
                    },
                    position = ListItemPosition.Middle
                )
                if (scheduledFolderBackupEnabled) {
                    SettingsNavItem(
                        icon = Icons.Default.SaveAlt,
                        iconBgColor = green_light,
                        iconTint = green_dark,
                        title = stringResource(R.string.settings_backup_now_title),
                        subtitle = scheduledFolderBackupLastTimestamp?.let { timestamp ->
                            val formatted = java.time.Instant.ofEpochMilli(timestamp)
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
                            stringResource(R.string.settings_backup_now_last_backup, formatted)
                        } ?: stringResource(R.string.settings_backup_now_subtitle),
                        onClick = { settingsViewModel.backupToFolderNow() },
                        position = ListItemPosition.Middle
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.Receipt,
                        iconBgColor = purple_light,
                        iconTint = purple_dark,
                        title = stringResource(R.string.settings_folder_backup_include_receipts_title),
                        subtitle = stringResource(R.string.settings_folder_backup_include_receipts_subtitle),
                        checked = folderBackupIncludeReceipts,
                        onCheckedChange = { settingsViewModel.setFolderBackupIncludeReceipts(it) },
                        position = ListItemPosition.Middle
                    )
                    SettingsNavItem(
                        icon = Icons.Default.FolderOpen,
                        iconBgColor = amber_light,
                        iconTint = amber_dark,
                        title = stringResource(R.string.settings_change_backup_folder_title),
                        subtitle = stringResource(R.string.settings_change_backup_folder_subtitle),
                        onClick = { settingsViewModel.requestChangeBackupFolder() },
                        position = ListItemPosition.Middle
                    )
                }
                SettingsNavItem(
                    icon = Icons.Default.Download,
                    iconBgColor = cyan_light,
                    iconTint = cyan_dark,
                    title = stringResource(R.string.settings_import_data_title),
                    subtitle = stringResource(R.string.settings_import_data_subtitle),
                    onClick = { importLauncher.launch("*/*") },
                    position = ListItemPosition.Middle
=======
                    title = "Export Data",
                    subtitle = "Backup all data to a file",
                    onClick = { showExportPasswordDialog = true },
                    position = ItemPosition.MIDDLE
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
                )
                SettingsNavItem(
                    icon = Icons.Default.Download,
                    iconBgColor = cyan_light,
                    iconTint = cyan_dark,
                    title = stringResource(R.string.settings_import_csv_title),
                    subtitle = stringResource(R.string.settings_import_csv_subtitle),
                    onClick = { csvImportLauncher.launch("*/*") },
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.Description,
                    iconBgColor = indigo_light,
                    iconTint = indigo_dark,
                    title = stringResource(R.string.settings_import_statement_title),
                    subtitle = stringResource(R.string.settings_import_statement_subtitle),
                    onClick = onNavigateToImportStatement,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.Sms,
                    iconBgColor = orange_light,
                    iconTint = orange_dark,
                    title = stringResource(R.string.settings_unrecognized_sms_title),
                    subtitle = stringResource(R.string.settings_unrecognized_sms_subtitle),
                    onClick = onNavigateToUnrecognizedSms,
                    position = ListItemPosition.Middle
                )
                SettingsNavItem(
                    icon = Icons.Default.CalendarMonth,
                    iconBgColor = teal_light,
                    iconTint = teal_dark,
                    title = stringResource(R.string.settings_sms_scan_title),
                    subtitle = when {
                        smsScanAllTime -> stringResource(R.string.settings_sms_scan_subtitle_all_time)
                        smsScanUseCustomDate -> {
                            val formattedDate = smsScanCustomDate?.let { formatSmsScanCustomDate(it) }
                            if (formattedDate != null) {
                                stringResource(R.string.settings_sms_scan_subtitle_from_date, formattedDate)
                            } else {
                                stringResource(R.string.settings_sms_scan_subtitle_custom)
                            }
                        }
                        else -> pluralStringResource(R.plurals.settings_sms_scan_subtitle_months, smsScanMonths, smsScanMonths)
                    },
                    onClick = { showSmsScanDialog = true },
                    position = ListItemPosition.Middle,
                    trailingText = when {
                        smsScanAllTime -> stringResource(R.string.settings_sms_scan_all_time)
                        smsScanUseCustomDate -> smsScanCustomDate?.let { formatSmsScanCustomDateShort(it) }
                            ?: stringResource(R.string.settings_sms_scan_custom_short)
                        else -> pluralStringResource(R.plurals.settings_sms_scan_months_short, smsScanMonths, smsScanMonths)
                    }
                )
                SettingsNavItem(
                    icon = Icons.Default.DeleteForever,
                    iconBgColor = red_light,
                    iconTint = red_dark,
                    title = stringResource(R.string.settings_delete_all_title),
                    subtitle = stringResource(R.string.settings_delete_all_subtitle),
                    onClick = { settingsViewModel.requestDeleteAllTransactions() },
                    position = ListItemPosition.Bottom
                )
            }

            // ── Notifications ──
            SectionHeaderV2(title = stringResource(R.string.settings_notifications_section))
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.Notifications,
                    iconBgColor = indigo_light,
                    iconTint = indigo_dark,
                    title = stringResource(R.string.settings_notification_access_title),
                    subtitle = if (hasNotificationAccess) stringResource(R.string.settings_notification_access_enabled) else stringResource(R.string.settings_notification_access_disabled),
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        notificationAccessLauncher.launch(intent)
                    },
<<<<<<< HEAD
                    position = ListItemPosition.Single,
                    trailingText = if (hasNotificationAccess) stringResource(R.string.settings_notification_access_on) else stringResource(R.string.settings_notification_access_off)
=======
                    position = ItemPosition.TOP,
                    trailingText = if (hasNotificationAccess) "On" else "Off"
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
                )
                SettingsNavItem(
                    icon = Icons.Default.List,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = "Monitored Bank Apps",
                    subtitle = if (monitoredBankPackages.isEmpty()) "Tap to select bank apps" else "${monitoredBankPackages.size} apps selected",
                    onClick = { showWhitelistDialog = true },
                    position = ItemPosition.BOTTOM
                )
            }

            // ── AI Features ──
            SectionHeaderV2(title = stringResource(R.string.settings_ai_section))
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.AutoAwesome,
                    iconBgColor = purple_light,
                    iconTint = purple_dark,
                    title = "Show PennyWise AI Tab",
                    subtitle = "Hide the AI assistant tab on devices that don't use local LLM chat",
                    checked = showAiTab,
                    onCheckedChange = { settingsViewModel.setShowAiTab(it) },
                    position = ListItemPosition.Single
                )
                AiChatSettingsItem(
                    downloadState = downloadState,
                    downloadProgress = downloadProgress,
                    downloadedMB = downloadedMB,
                    totalMB = totalMB,
                    onDownload = { settingsViewModel.startModelDownload() },
                    onCancel = { settingsViewModel.cancelDownload() },
                    onDelete = { settingsViewModel.deleteModel() }
                )
            }

            // ── Developer ──
            SectionHeaderV2(title = stringResource(R.string.settings_developer_section))
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.Default.CloudUpload,
                    iconBgColor = teal_light,
                    iconTint = teal_dark,
                    title = "Webhooks",
                    subtitle = if (isProEntitled) "Send selected data to your endpoint" else "Pro · Send selected data to your endpoint",
                    onClick = { if (isProEntitled) onNavigateToWebhooks() else showUpgradeSheet = true },
                    position = ListItemPosition.Top
                )
                SettingsSwitchRow(
                    icon = Icons.Default.Code,
                    iconBgColor = grey_light,
                    iconTint = grey_dark,
                    title = stringResource(R.string.settings_developer_mode_title),
                    subtitle = stringResource(R.string.settings_developer_mode_subtitle),
                    checked = isDeveloperModeEnabled,
                    onCheckedChange = { settingsViewModel.toggleDeveloperMode(it) },
                    position = ListItemPosition.Bottom
                )
            }

            // ── Support & Community ──
            SectionHeaderV2(title = stringResource(R.string.settings_support_section))
            SettingsGroup {
                SettingsNavItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    iconBgColor = pink_light,
                    iconTint = pink_dark,
                    title = stringResource(R.string.settings_help_faq_title),
                    subtitle = stringResource(R.string.settings_help_faq_subtitle),
                    onClick = onNavigateToFaq,
                    position = ListItemPosition.Top
                )
                SettingsNavItem(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconBgColor = teal_light,
                    iconTint = teal_dark,
                    title = stringResource(R.string.settings_user_guide_title),
                    subtitle = stringResource(R.string.settings_user_guide_subtitle),
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.Links.USER_GUIDE_URL)))
                    },
                    position = ListItemPosition.Middle,
                    trailingIcon = Icons.AutoMirrored.Filled.OpenInNew
                )
                SettingsNavItem(
                    icon = Icons.Default.BugReport,
                    iconBgColor = blue_light,
                    iconTint = blue_dark,
                    title = stringResource(R.string.settings_report_issue_title),
                    subtitle = stringResource(R.string.settings_report_issue_subtitle),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sarim2000/pennywiseai-tracker/issues/new/choose"))
                        context.startActivity(intent)
                    },
                    position = ListItemPosition.Bottom,
                    trailingIcon = Icons.AutoMirrored.Filled.OpenInNew
                )
            }

            // App Version
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.settings_app_version, com.pennywiseai.tracker.BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }

    // ── Dialogs ──

    // Display Currency Dialog
    if (showDisplayCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showDisplayCurrencyDialog = false },
            title = { Text(stringResource(R.string.settings_display_currency_title)) },
            text = {
                // Scrollable: the full currency list overflows the dialog's max
                // height, so without this the entries below the fold (e.g. MXN)
                // are unreachable. (#615)
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    availableCurrencies.forEach { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = currency == displayCurrency,
                                    onClick = {
                                        settingsViewModel.setDisplayCurrency(currency)
                                        showDisplayCurrencyDialog = false
                                    }
                                )
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currency == displayCurrency,
                                onClick = {
                                    settingsViewModel.setDisplayCurrency(currency)
                                    showDisplayCurrencyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = "${CurrencyFormatter.getCurrencySymbol(currency)} $currency",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisplayCurrencyDialog = false }) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }

    // Number Format Dialog
    if (showNumberFormatDialog) {
        AlertDialog(
            onDismissRequest = { showNumberFormatDialog = false },
            title = { Text(stringResource(R.string.settings_number_format_title)) },
            text = {
                Column {
                    NumberFormatStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = style == numberFormatStyle,
                                    onClick = {
                                        settingsViewModel.updateNumberFormatStyle(style)
                                        showNumberFormatDialog = false
                                    }
                                )
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = style == numberFormatStyle,
                                onClick = {
                                    settingsViewModel.updateNumberFormatStyle(style)
                                    showNumberFormatDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column {
                                Text(
                                    text = numberFormatStyleLabel(style),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = numberFormatStyleExample(style),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNumberFormatDialog = false }) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }

    // Budget Cycle Start Day Dialog
    if (showBudgetCycleDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetCycleDialog = false },
            title = { Text(stringResource(R.string.settings_budget_cycle_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.settings_budget_cycle_dialog_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    (1..31).forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = day == budgetCycleStartDay,
                                    onClick = {
                                        settingsViewModel.updateBudgetCycleStartDay(day)
                                        showBudgetCycleDialog = false
                                    }
                                )
                                .padding(vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = day == budgetCycleStartDay,
                                onClick = {
                                    settingsViewModel.updateBudgetCycleStartDay(day)
                                    showBudgetCycleDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = ordinalSuffix(day),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBudgetCycleDialog = false }) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }

    // SMS Scan Period Dialog
    if (showSmsScanDialog) {
        AlertDialog(
            onDismissRequest = { showSmsScanDialog = false },
            title = { Text(stringResource(R.string.settings_sms_scan_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.settings_sms_scan_dialog_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    val options = listOf(-1, -2) + listOf(1, 2, 3, 6, 12, 24)
                    options.forEach { months ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (months) {
                                        -1 -> {
                                            settingsViewModel.updateSmsScanAllTime(true)
                                            showSmsScanDialog = false
                                        }
                                        -2 -> {
                                            showSmsScanDialog = false
                                            showSmsScanDatePicker = true
                                        }
                                        else -> {
                                            settingsViewModel.updateSmsScanMonths(months)
                                            settingsViewModel.updateSmsScanAllTime(false)
                                            showSmsScanDialog = false
                                        }
                                    }
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isSelected = when (months) {
                                -1 -> smsScanAllTime
                                -2 -> smsScanUseCustomDate && !smsScanAllTime
                                else -> smsScanMonths == months && !smsScanAllTime && !smsScanUseCustomDate
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    when (months) {
                                        -1 -> {
                                            settingsViewModel.updateSmsScanAllTime(true)
                                            showSmsScanDialog = false
                                        }
                                        -2 -> {
                                            showSmsScanDialog = false
                                            showSmsScanDatePicker = true
                                        }
                                        else -> {
                                            settingsViewModel.updateSmsScanMonths(months)
                                            settingsViewModel.updateSmsScanAllTime(false)
                                            showSmsScanDialog = false
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text = when (months) {
                                    -1 -> stringResource(R.string.settings_sms_scan_all_time)
                                    -2 -> {
                                        val formattedDate = smsScanCustomDate?.let { formatSmsScanCustomDate(it) }
                                        if (formattedDate != null) {
                                            stringResource(R.string.settings_sms_scan_option_custom_with_date, formattedDate)
                                        } else {
                                            stringResource(R.string.settings_sms_scan_option_custom)
                                        }
                                    }
                                    24 -> pluralStringResource(R.plurals.settings_sms_scan_option_years, 2, 2)
                                    else -> pluralStringResource(R.plurals.settings_sms_scan_option_months, months, months)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSmsScanDialog = false }) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }

    if (showSmsScanDatePicker) {
        val todayMillis = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val initialSelectedDateMillis = smsScanCustomDate
            ?: java.time.LocalDate.now()
                .minusMonths(3)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayMillis
                }
            }
        )

        // Cancelling/dismissing the picker returns to the scan-period dialog rather than
        // silently dropping the user back to Settings (they came here to change the period).
        fun reopenScanDialog() {
            showSmsScanDatePicker = false
            showSmsScanDialog = true
        }

        DatePickerDialog(
            onDismissRequest = { reopenScanDialog() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            settingsViewModel.updateSmsScanCustomDate(millis)
                        }
                        showSmsScanDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.settings_action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { reopenScanDialog() }) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Delete-all-transactions confirmation. Irreversible and unbatched, so it
    // asks for the word DELETE rather than a single tap, names the exact number
    // of rows, and points at Export Data first.
    deleteAllTransactionsCount?.let { count ->
        var confirmationText by rememberSaveable(count) { mutableStateOf("") }
        // The confirmation word is deliberately not translated.
        val confirmWord = "DELETE"
        val confirmed = confirmationText.trim().equals(confirmWord, ignoreCase = false)

        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAllTransactions) settingsViewModel.cancelDeleteAllTransactions()
            },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.settings_delete_all_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        pluralStringResource(R.plurals.settings_delete_all_dialog_body, count, count)
                    )
                    Text(
                        stringResource(R.string.settings_delete_all_dialog_kept),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = confirmationText,
                        onValueChange = { confirmationText = it },
                        singleLine = true,
                        enabled = !isDeletingAllTransactions,
                        label = { Text(stringResource(R.string.settings_delete_all_dialog_type_to_confirm, confirmWord)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { settingsViewModel.deleteAllTransactions(count) },
                    enabled = confirmed && !isDeletingAllTransactions,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isDeletingAllTransactions) stringResource(R.string.settings_delete_all_dialog_deleting) else stringResource(R.string.settings_delete_all_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { settingsViewModel.cancelDeleteAllTransactions() },
                    enabled = !isDeletingAllTransactions
                ) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }

    deleteAllTransactionsResult?.let { sentences ->
        AlertDialog(
            onDismissRequest = { settingsViewModel.clearDeleteAllTransactionsResult() },
            title = { Text(stringResource(R.string.settings_delete_all_result_title)) },
            text = { Text(sentences.map { it.asString() }.joinToString(" ")) },
            confirmButton = {
                TextButton(onClick = { settingsViewModel.clearDeleteAllTransactionsResult() }) {
                    Text(stringResource(R.string.settings_action_ok))
                }
            }
        )
    }

    // Show import/export message
    importExportMessage?.let { message ->
        if (exportedBackupFile != null && message == UiText.Res(R.string.settings_backup_created_choose)) {
            showExportOptionsDialog = true
        } else {
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(5000)
                settingsViewModel.clearImportExportMessage()
            }

            AlertDialog(
                onDismissRequest = { settingsViewModel.clearImportExportMessage() },
                title = { Text(stringResource(R.string.settings_backup_status_title)) },
                text = { Text(message.asString()) },
                confirmButton = {
                    TextButton(onClick = { settingsViewModel.clearImportExportMessage() }) {
                        Text(stringResource(R.string.settings_action_ok))
                    }
                }
            )
        }
    }

    // Export options dialog
    if (showExportOptionsDialog && exportedBackupFile != null) {
        val timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss")
        )
        val fileName = "PennyWise_Backup_$timestamp.pennywisebackup"

        AlertDialog(
            onDismissRequest = {
                showExportOptionsDialog = false
                settingsViewModel.clearImportExportMessage()
            },
            title = { Text(stringResource(R.string.settings_save_backup_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_save_backup_created))
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(stringResource(R.string.settings_save_backup_choose), style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            exportSaveLauncher.launch(fileName)
                            showExportOptionsDialog = false
                            settingsViewModel.clearImportExportMessage()
                        }
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_save_backup_to_files))
                    }

                    TextButton(
                        onClick = {
                            settingsViewModel.shareBackup()
                            showExportOptionsDialog = false
                            settingsViewModel.clearImportExportMessage()
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_save_backup_share))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportOptionsDialog = false
                        settingsViewModel.clearImportExportMessage()
                    }
                ) {
                    Text(stringResource(R.string.settings_action_cancel))
                }
            }
        )
    }

    // Lock Timeout Dialog
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text(stringResource(R.string.settings_lock_timeout_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.settings_lock_timeout_dialog_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    val timeoutOptions = listOf(
                        0 to stringResource(R.string.settings_lock_timeout_option_immediately),
                        1 to pluralStringResource(R.plurals.settings_lock_timeout_option_minutes, 1, 1),
                        5 to pluralStringResource(R.plurals.settings_lock_timeout_option_minutes, 5, 5),
                        15 to pluralStringResource(R.plurals.settings_lock_timeout_option_minutes, 15, 15)
                    )

                    timeoutOptions.forEach { (minutes, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appLockViewModel.setTimeoutMinutes(minutes)
                                    showTimeoutDialog = false
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLockUiState.timeoutMinutes == minutes,
                                onClick = {
                                    appLockViewModel.setTimeoutMinutes(minutes)
                                    showTimeoutDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) {
                    Text(stringResource(R.string.settings_action_done))
                }
            }
        )
    }

    if (showWhitelistDialog) {
        val allAvailablePackages = listOf(
            "com.google.android.apps.nbu.paisa.user" to "Google Pay (India)",
            "com.sbi.yono" to "SBI Yono",
            "com.sbi.yonolite" to "SBI Yono Lite",
            "com.sbi.upi" to "SBI UPI",
            "com.avanza.ambitwizfbl" to "Faysal Bank",
            "finansbank.enpara" to "Enpara (Personal)",
            "com.enparabank.retail" to "Enpara Bank"
        )
        AlertDialog(
            onDismissRequest = { showWhitelistDialog = false },
            title = { Text("Monitored Bank Apps") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = "Select which bank app notifications to monitor for transaction parsing:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                    allAvailablePackages.forEach { (pkg, name) ->
                        val isChecked = monitoredBankPackages.contains(pkg)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = if (isChecked) {
                                        monitoredBankPackages - pkg
                                    } else {
                                        monitoredBankPackages + pkg
                                    }
                                    settingsViewModel.updateMonitoredBankPackages(newSet)
                                }
                                .padding(vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val newSet = if (checked == true) {
                                        monitoredBankPackages + pkg
                                    } else {
                                        monitoredBankPackages - pkg
                                    }
                                    settingsViewModel.updateMonitoredBankPackages(newSet)
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Column {
                                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                                Text(text = pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWhitelistDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showExportPasswordDialog = false 
                exportPasswordText = ""
            },
            title = { Text("Encrypt Backup") },
            text = {
                Column {
                    Text("Enter a password to encrypt your backup file. You will need this password to restore your data.")
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = exportPasswordText,
                        onValueChange = { exportPasswordText = it },
                        label = { Text("Backup Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (exportPasswordText.isNotBlank()) {
                            settingsViewModel.exportBackup(exportPasswordText)
                            showExportPasswordDialog = false
                            exportPasswordText = ""
                        }
                    },
                    enabled = exportPasswordText.isNotBlank()
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showExportPasswordDialog = false 
                    exportPasswordText = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportPasswordDialog && importPendingUri != null) {
        AlertDialog(
            onDismissRequest = { 
                showImportPasswordDialog = false 
                importPasswordText = ""
                importPendingUri = null
            },
            title = { Text("Decrypt Backup") },
            text = {
                Column {
                    Text("Enter the password used to encrypt this backup file to decrypt and restore your data.")
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = importPasswordText,
                        onValueChange = { importPasswordText = it },
                        label = { Text("Backup Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = importPendingUri
                        if (uri != null && importPasswordText.isNotBlank()) {
                            settingsViewModel.importBackup(uri, importPasswordText)
                            showImportPasswordDialog = false
                            importPasswordText = ""
                            importPendingUri = null
                        }
                    },
                    enabled = importPasswordText.isNotBlank()
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showImportPasswordDialog = false 
                    importPasswordText = ""
                    importPendingUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSetBackupPasswordDialog) {
        var tempPassword by remember { mutableStateOf("") }
        var isPasswordVisible by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            val currentPass = settingsViewModel.getBackupPassword() ?: ""
            tempPassword = currentPass
        }

        AlertDialog(
            onDismissRequest = { showSetBackupPasswordDialog = false },
            title = { Text("Auto-Backup Password") },
            text = {
                Column {
                    Text("Set a password to encrypt automated local backups. If left blank, auto-backups will be skipped.")
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Toggle Password Visibility" else "Toggle Password Visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.setBackupPassword(tempPassword.takeIf { it.isNotBlank() })
                        showSetBackupPasswordDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetBackupPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUpgradeSheet) {
        com.pennywiseai.tracker.presentation.paywall.UpgradeSheet(
            onDismiss = { showUpgradeSheet = false },
        )
    }

    if (showSupportDialog) {
        SupportDevelopmentDialog(onDismiss = { showSupportDialog = false })
    }
}

// ── Reusable Settings Components ──
//
// Row chrome — tonal surface, grouped-corner shape, padding, minimum height,
// the tinted icon circle, title/subtitle typography — lives in the shared
// `GroupedList` / `GroupedRow` / `IconTile` / `RowLabels` primitives, so a
// settings row and a grouped row on any other screen are literally the same
// object. These wrappers only add the settings-specific trailing affordance.

@Composable
private fun SettingsGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    GroupedList(content = content)
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    position: ListItemPosition,
    trailingText: String? = null,
    trailingIcon: ImageVector = Icons.Default.ChevronRight
) {
    GroupedRow(position = position, onClick = onClick) {
        IconTile(icon = icon, containerColor = iconBgColor, contentColor = iconTint)
        RowLabels(title = title, subtitle = subtitle)
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // The chevron is a hint, not a control — at 20dp it stops competing
        // with the leading icon for attention the way a 24dp one did.
        Icon(
            trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimensions.Icon.inline)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: ListItemPosition,
    enabled: Boolean = true
) {
    GroupedRow(
        position = position,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) }
    ) {
        IconTile(icon = icon, containerColor = iconBgColor, contentColor = iconTint)
        RowLabels(
            title = title,
            subtitle = subtitle,
            subtitleColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdownItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    currentValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    position: ListItemPosition,
    dropdownContent: @Composable ColumnScope.() -> Unit
) {
    GroupedColumn(position = position) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon = icon, containerColor = iconBgColor, contentColor = iconTint)
            RowLabels(title = title, subtitle = subtitle)
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            TextField(
                value = currentValue,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_currency_field_label)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    // A field nested inside an already-tonal row needs a step
                    // of contrast against it, otherwise the input boundary
                    // disappears.
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                content = dropdownContent
            )
        }
    }
}

@Composable
private fun AiChatSettingsItem(
    downloadState: DownloadState,
    downloadProgress: Int,
    downloadedMB: Long,
    totalMB: Long,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    GroupedColumn(
        position = ListItemPosition.Single,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = Icons.Default.AutoAwesome,
                containerColor = yellow_light,
                contentColor = yellow_dark
            )
            RowLabels(
                title = stringResource(R.string.settings_ai_chat_title),
                subtitle = when (downloadState) {
                    DownloadState.NOT_DOWNLOADED -> stringResource(R.string.settings_ai_status_not_downloaded, Constants.ModelDownload.MODEL_SIZE_MB)
                    DownloadState.DOWNLOADING -> stringResource(R.string.settings_ai_status_downloading)
                    DownloadState.PAUSED -> stringResource(R.string.settings_ai_status_paused)
                    DownloadState.COMPLETED -> stringResource(R.string.settings_ai_status_completed)
                    DownloadState.FAILED -> stringResource(R.string.settings_ai_status_failed)
                    DownloadState.ERROR_INSUFFICIENT_SPACE -> stringResource(R.string.settings_ai_status_insufficient_space)
                }
            )

            when (downloadState) {
                DownloadState.NOT_DOWNLOADED -> {
                    Button(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_ai_download))
                    }
                }
                DownloadState.DOWNLOADING -> {
                    Text(
                        text = "$downloadProgress%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                DownloadState.PAUSED -> {
                    Button(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_ai_retry))
                    }
                }
                DownloadState.COMPLETED -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.settings_ai_downloaded),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimensions.Icon.medium)
                        )
                        TextButton(onClick = onDelete) {
                            Text(stringResource(R.string.settings_ai_delete))
                        }
                    }
                }
                DownloadState.FAILED -> {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.settings_ai_retry))
                    }
                }
                DownloadState.ERROR_INSUFFICIENT_SPACE -> {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = stringResource(R.string.settings_ai_error),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimensions.Icon.medium)
                    )
                }
            }
        }

        // Progress details during download
        AnimatedVisibility(
            visible = downloadState == DownloadState.DOWNLOADING,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.settings_ai_download_progress, downloadedMB, totalMB),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.settings_ai_cancel_download))
                }
            }
        }

        // Info about AI features
        if (downloadState == DownloadState.NOT_DOWNLOADED ||
            downloadState == DownloadState.ERROR_INSUFFICIENT_SPACE
        ) {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.settings_ai_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsNavigationContent(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .animateContentSize()
            .padding(start = Spacing.md)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onNavigateBack,
            ),
    ) {
        IconButton(
            onClick = onNavigateBack,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                modifier = Modifier.size(Dimensions.Icon.inline)
            )
        }
    }
}

@Composable
private fun numberFormatStyleLabel(style: NumberFormatStyle): String = stringResource(
    when (style) {
        NumberFormatStyle.AUTO -> R.string.settings_number_format_auto
        NumberFormatStyle.INDIAN -> R.string.settings_number_format_indian
        NumberFormatStyle.INTERNATIONAL -> R.string.settings_number_format_international
    }
)

@Composable
private fun numberFormatStyleExample(style: NumberFormatStyle): String = stringResource(
    when (style) {
        NumberFormatStyle.AUTO -> R.string.settings_number_format_auto_example
        NumberFormatStyle.INDIAN -> R.string.settings_number_format_indian_example
        NumberFormatStyle.INTERNATIONAL -> R.string.settings_number_format_international_example
    }
)

/**
 * English ordinal suffix for the budget cycle start day — "1st", "2nd", "3rd",
 * "4th"… "11th", "12th", "13th" follow the standard rule that the last two
 * digits decide the suffix (the 11/12/13 teens are always "th").
 */
private fun ordinalSuffix(day: Int): String {
    val safe = day.coerceIn(1, 31)
    val suffix = when {
        safe in 11..13 -> "th"
        safe % 10 == 1 -> "st"
        safe % 10 == 2 -> "nd"
        safe % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$safe$suffix"
}

private fun formatSmsScanCustomDate(dateMillis: Long): String {
    return java.time.Instant.ofEpochMilli(dateMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
}

private fun formatSmsScanCustomDateShort(dateMillis: Long): String {
    return java.time.Instant.ofEpochMilli(dateMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
}
