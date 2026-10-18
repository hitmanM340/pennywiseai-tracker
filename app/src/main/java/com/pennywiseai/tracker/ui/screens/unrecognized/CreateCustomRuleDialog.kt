package com.pennywiseai.tracker.ui.screens.unrecognized

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pennywiseai.tracker.ui.theme.Spacing

@Composable
fun CreateCustomRuleDialog(
    initialSender: String,
    sampleSmsBody: String,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        senderPattern: String,
        amountRegex: String,
        merchantRegex: String?,
        accountRegex: String?,
        transactionType: String
    ) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialSender.trim()) }
    var senderPattern by rememberSaveable { mutableStateOf(Regex.escape(initialSender.trim())) }
    var amountRegex by rememberSaveable {
        mutableStateOf("""(?:Rs\.?|INR|USD|AED|NPR)\s*([0-9,]+(?:\.[0-9]{1,2})?)""")
    }
    var merchantRegex by rememberSaveable {
        mutableStateOf("""(?:at|to|info:?)\s+([A-Za-z0-9 &._-]+)""")
    }
    var accountRegex by rememberSaveable {
        mutableStateOf("""(?:A/c|acct|card|xx+|\*\*+)\s*([0-9]{3,4})""")
    }
    var transactionType by rememberSaveable { mutableStateOf("EXPENSE") }

    val validation by remember(senderPattern, amountRegex, merchantRegex, accountRegex, sampleSmsBody) {
        derivedStateOf {
            val senderValid = runCatching { Regex(senderPattern, RegexOption.IGNORE_CASE) }.isSuccess
            val amountPattern = runCatching { Regex(amountRegex, RegexOption.IGNORE_CASE) }.getOrNull()
            val merchantValid = merchantRegex.isBlank() ||
                runCatching { Regex(merchantRegex, RegexOption.IGNORE_CASE) }.isSuccess
            val accountValid = accountRegex.isBlank() ||
                runCatching { Regex(accountRegex, RegexOption.IGNORE_CASE) }.isSuccess

            val previewAmount = amountPattern?.find(sampleSmsBody)?.groupValues?.getOrNull(1)
            Triple(
                senderValid && amountPattern != null && merchantValid && accountValid,
                previewAmount,
                if (amountPattern == null) "Invalid amount regex syntax" else null
            )
        }
    }

    val (isRegexValid, matchedAmountPreview, errorMessage) = validation
    val canSave = name.isNotBlank() && senderPattern.isNotBlank() && amountRegex.isNotBlank() && isRegexValid

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Create Custom SMS Parser",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Spacing.sm)) {
                        Text(
                            text = "Sample SMS ($initialSender)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = sampleSmsBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (matchedAmountPreview != null) {
                                "Live Match Preview: Amount = $matchedAmountPreview"
                            } else {
                                "No amount matched yet (Capture Group 1 required)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (matchedAmountPreview != null) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(top = Spacing.xs)
                        )
                    }
                }

                androidx.compose.material3.FilledTonalButton(
                    onClick = {
                        val auto = RegexRuleAutoGenerator.generateFromSms(sampleSmsBody)
                        amountRegex = """(?:INR|Rs\.?|₹|USD|EUR)?\s*([0-9,]+(?:\.[0-9]{1,2})?)"""
                        if (auto.accountGroup != null) {
                            accountRegex = """(?:a/c|acct|card|ending|xx+)[^0-9]*([0-9]{3,4})"""
                        }
                        if (auto.merchantGroup != null) {
                            merchantRegex = """(?:at|to|vpa)\s+([A-Za-z0-9\*\.\-]{2,30})"""
                        }
                        if (sampleSmsBody.contains("credit", ignoreCase = true) || sampleSmsBody.contains("deposit", ignoreCase = true)) {
                            transactionType = "INCOME"
                        } else {
                            transactionType = "EXPENSE"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Auto-Generate Safe Rules (AI)")
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = senderPattern,
                    onValueChange = { senderPattern = it },
                    label = { Text("Sender Regex") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountRegex,
                    onValueChange = { amountRegex = it },
                    label = { Text("Amount Regex (Group 1)") },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = merchantRegex,
                    onValueChange = { merchantRegex = it },
                    label = { Text("Merchant Regex (Optional, Group 1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accountRegex,
                    onValueChange = { accountRegex = it },
                    label = { Text("Account Last-4 Regex (Optional, Group 1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("EXPENSE", "INCOME", "CREDIT", "TRANSFER").forEach { type ->
                        FilterChip(
                            selected = transactionType == type,
                            onClick = { transactionType = type },
                            label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        name.trim(),
                        senderPattern.trim(),
                        amountRegex.trim(),
                        merchantRegex.trim().ifBlank { null },
                        accountRegex.trim().ifBlank { null },
                        transactionType
                    )
                }
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
