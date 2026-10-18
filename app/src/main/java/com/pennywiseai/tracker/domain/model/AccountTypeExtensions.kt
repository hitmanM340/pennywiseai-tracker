package com.pennywiseai.tracker.domain.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.presentation.accounts.AccountType
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity

/**
 * Converts AccountType enum to database string representation
 */
fun AccountType.toDatabaseString(): String = this.name

/**
 * Converts database string to AccountType enum, with fallback
 */
fun String?.toAccountType(): AccountType = when (this?.uppercase()) {
    "SAVINGS" -> AccountType.SAVINGS
    "CURRENT" -> AccountType.CURRENT
    "CREDIT" -> AccountType.CREDIT
    "CASH" -> AccountType.CASH
    "LOAN" -> AccountType.LOAN
    else -> AccountType.SAVINGS  // Default fallback
}

/**
 * Gets AccountType from AccountBalanceEntity, using accountType field
 * or falling back to isCreditCard flag for backward compatibility
 */
fun AccountBalanceEntity.getAccountType(): AccountType {
    return when {
        accountType != null -> accountType.toAccountType()
        isCreditCard -> AccountType.CREDIT
        else -> AccountType.SAVINGS
    }
}

/**
 * Returns true if this account represents a debt/liability account
 * (credit card, loan, or credit line) where incoming repayments reduce debt.
 */
fun AccountBalanceEntity.isDebtAccount(): Boolean =
    isCreditCard || accountType.equals("LOAN", ignoreCase = true) || accountType.equals("CREDIT", ignoreCase = true)

/**
 * Formats account type for UI display
 */
@Composable
@ReadOnlyComposable
fun AccountType.displayName(): String = stringResource(
    when (this) {
        AccountType.SAVINGS -> R.string.account_type_savings
        AccountType.CURRENT -> R.string.account_type_current
        AccountType.CREDIT -> R.string.account_type_credit_card
        AccountType.CASH -> R.string.account_type_cash
        AccountType.LOAN -> R.string.account_type_loan
    }
)
