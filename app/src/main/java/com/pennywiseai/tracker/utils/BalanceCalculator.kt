package com.pennywiseai.tracker.utils

import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal

object BalanceCalculator {
    /**
     * Carries an account's type over when its balance row is rebuilt from a new
     * SMS/transaction. The single implementation every balance-rebuild call site
     * shares, so a type can't silently drop on one path while staying fixed on
     * another (#798).
     */
    fun preservedAccountType(existing: AccountBalanceEntity?): String? = existing?.accountType

    /**
     * Helper to check if an account or account flags represent a debt account
     * (credit card, loan, credit line) where spending raises outstanding debt
     * and repayments lower it. (#792)
     */
    fun isDebtAccount(isCreditCard: Boolean, accountType: String? = null): Boolean =
        isCreditCard || accountType.equals("LOAN", ignoreCase = true) || accountType.equals("CREDIT", ignoreCase = true)

    fun isDebtAccount(account: AccountBalanceEntity?): Boolean =
        account?.let { isDebtAccount(it.isCreditCard, it.accountType) } ?: false

    /**
     * Balance an account row should carry after a parsed SMS whose amount is in
     * [transactionCurrency] while the account is kept in [accountCurrency].
     *
     * A spend in another currency (USD on an INR card) can't be added to the
     * balance — that would sum across currencies, and the bank's own converted
     * figure only arrives with a later SMS. So it keeps the explicit balance the
     * SMS reports (that is the account's own figure), else the current balance.
     * Same-currency SMS go through [calculateNewBalance] unchanged. (#888)
     */
    fun balanceAfter(
        explicitBalance: BigDecimal?,
        isCreditCard: Boolean,
        transactionType: TransactionType,
        transactionAmount: BigDecimal,
        transactionCurrency: String,
        accountCurrency: String,
        currentBalance: BigDecimal?,
        accountType: String? = null
    ): BigDecimal {
        val debt = isDebtAccount(isCreditCard, accountType)
        return if (transactionCurrency == accountCurrency) {
            calculateNewBalance(explicitBalance, debt, transactionType, transactionAmount, currentBalance, accountType)
        } else {
            explicitBalance ?: currentBalance ?: BigDecimal.ZERO
        }
    }

    /**
     * Calculates the new account balance post-transaction.
     * For credit cards and loans, balance represents outstanding debt. (#792)
     */
    fun calculateNewBalance(
        explicitBalance: BigDecimal?,
        isCreditCard: Boolean,
        transactionType: TransactionType,
        transactionAmount: BigDecimal,
        currentBalance: BigDecimal?,
        accountType: String? = null
    ): BigDecimal {
        val cur = currentBalance ?: BigDecimal.ZERO
        val debt = isDebtAccount(isCreditCard, accountType)
        return when {
            explicitBalance != null -> explicitBalance
            debt -> {
                when (transactionType) {
                    TransactionType.INCOME -> {
                        // Refunds or repayments reduce outstanding debt
                        cur - transactionAmount
                    }
                    TransactionType.EXPENSE, TransactionType.INVESTMENT, TransactionType.CREDIT -> {
                        // Purchases or cash withdrawals increase outstanding debt
                        cur + transactionAmount
                    }
                    TransactionType.TRANSFER -> {
                        // Transfers between own accounts keep outstanding debt unchanged
                        // (Requires explicit balance or complex leg resolution to modify)
                        cur
                    }
                }
            }
            else -> {
                when (transactionType) {
                    TransactionType.INCOME -> cur + transactionAmount
                    TransactionType.EXPENSE, TransactionType.INVESTMENT -> cur - transactionAmount
                    TransactionType.TRANSFER, TransactionType.CREDIT -> {
                        // Transfers/credits on debit accounts do not change standard balance directly
                        cur
                    }
                }
            }
        }
    }

    /**
     * Signed delta a non-TRANSFER transaction applies to its own account's stored
     * balance. This is the single sign convention shared by manual add, edit and
     * delete (`AccountBalanceRepository`), so a revert can never disagree with the
     * apply that preceded it.
     *
     * Credit cards and loans store outstanding owed, so spending raises the figure and
     * income (refund/repayment) lowers it. TRANSFER is zero here — transfers move
     * balances through their from/to legs ([transferLegEffect]), never through
     * the account the row happens to be filed under. (#792)
     */
    fun signedBalanceEffect(
        isCreditCard: Boolean,
        transactionType: TransactionType,
        amount: BigDecimal,
        accountType: String? = null
    ): BigDecimal {
        val debt = isDebtAccount(isCreditCard, accountType)
        return when (transactionType) {
            TransactionType.INCOME -> if (debt) amount.negate() else amount
            TransactionType.EXPENSE,
            TransactionType.CREDIT,
            TransactionType.INVESTMENT -> if (debt) amount else amount.negate()
            TransactionType.TRANSFER -> BigDecimal.ZERO
        }
    }

    /**
     * Signed delta one TRANSFER leg applies to the leg's account. Money arriving
     * at a debit account raises its balance; arriving at a credit card or loan it pays
     * down outstanding, so the sign flips. (#792)
     */
    fun transferLegEffect(
        isCreditCard: Boolean,
        incoming: Boolean,
        amount: BigDecimal,
        accountType: String? = null
    ): BigDecimal {
        val debt = isDebtAccount(isCreditCard, accountType)
        val debitEffect = if (incoming) amount else amount.negate()
        return if (debt) debitEffect.negate() else debitEffect
    }
}
