// [VERIFIED-2X]
package com.pennywiseai.tracker.domain.intelligence

import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alert for potential duplicate charges processed within a short time window.
 */
data class DuplicateChargeAlert(
    val originalTransaction: TransactionEntity,
    val duplicateTransaction: TransactionEntity,
    val timeDifferenceSeconds: Long,
    val amount: BigDecimal
)

/**
 * Alert for silent price increases in recurring subscriptions or services.
 */
data class SubscriptionCreepAlert(
    val merchantName: String,
    val previousAmount: BigDecimal,
    val newAmount: BigDecimal,
    val increasePercentage: Double,
    val latestTransaction: TransactionEntity
)

/**
 * Comprehensive Sentry audit report for risk and irregularity detection.
 */
data class SentryAuditReport(
    val duplicateCharges: List<DuplicateChargeAlert>,
    val subscriptionCreeps: List<SubscriptionCreepAlert>,
    val totalPotentialWaste: BigDecimal
)

/**
 * Transaction Sentry Engine: Autonomous security guardian detecting duplicate charges,
 * silent subscription price creep (+5% to +80%), and billing anomalies.
 */
@Singleton
class TransactionSentryEngine @Inject constructor() {

    companion object {
        private const val DUPLICATE_TIME_WINDOW_MINUTES = 10L
        private const val CREEP_MIN_PERCENT = 5.0
        private const val CREEP_MAX_PERCENT = 80.0
    }

    /**
     * Audits a transaction list to identify duplicate charges and subscription price creeps.
     */
    fun auditTransactions(transactions: List<TransactionEntity>): SentryAuditReport {
        val validTxs = transactions.filter { !it.isDeleted && it.transactionType == TransactionType.EXPENSE }
        val duplicates = detectDuplicateCharges(validTxs)
        val creeps = detectSubscriptionCreep(validTxs)

        var totalWaste = BigDecimal.ZERO
        for (dup in duplicates) {
            totalWaste = totalWaste.add(dup.amount)
        }
        for (creep in creeps) {
            val delta = creep.newAmount.subtract(creep.previousAmount)
            if (delta > BigDecimal.ZERO) {
                totalWaste = totalWaste.add(delta)
            }
        }

        return SentryAuditReport(
            duplicateCharges = duplicates,
            subscriptionCreeps = creeps,
            totalPotentialWaste = totalWaste.setScale(2, RoundingMode.HALF_UP)
        )
    }

    /**
     * Detects identical debit amounts at the same merchant or bank within 10 minutes.
     */
    fun detectDuplicateCharges(
        transactions: List<TransactionEntity>,
        windowMinutes: Long = DUPLICATE_TIME_WINDOW_MINUTES
    ): List<DuplicateChargeAlert> {
        val alerts = mutableListOf<DuplicateChargeAlert>()
        val sorted = transactions.sortedBy { it.dateTime }

        for (i in 0 until sorted.size) {
            val tx1 = sorted[i]
            for (j in i + 1 until sorted.size) {
                val tx2 = sorted[j]
                val duration = Duration.between(tx1.dateTime, tx2.dateTime).abs()

                // If gap exceeds window, subsequent transactions in sorted order will also exceed
                if (duration.toMinutes() > windowMinutes) {
                    break
                }

                val sameAmount = tx1.amount.compareTo(tx2.amount) == 0
                val sameMerchant = tx1.merchantName.equals(tx2.merchantName, ignoreCase = true)
                val sameBank = tx1.bankName != null && tx1.bankName.equals(tx2.bankName, ignoreCase = true)

                if (sameAmount && (sameMerchant || sameBank)) {
                    alerts.add(
                        DuplicateChargeAlert(
                            originalTransaction = tx1,
                            duplicateTransaction = tx2,
                            timeDifferenceSeconds = duration.seconds,
                            amount = tx2.amount
                        )
                    )
                }
            }
        }

        return alerts
    }

    /**
     * Detects subtle subscription price increases (+5% to +80%) over recurring intervals.
     */
    fun detectSubscriptionCreep(transactions: List<TransactionEntity>): List<SubscriptionCreepAlert> {
        val alerts = mutableListOf<SubscriptionCreepAlert>()

        // Group by normalized merchant name
        val groupedByMerchant = transactions
            .filter { it.merchantName.isNotBlank() }
            .groupBy { it.merchantName.trim().lowercase() }

        for ((_, merchantTxs) in groupedByMerchant) {
            // Need at least 2 transactions to compare price trend
            if (merchantTxs.size < 2) continue

            val chronological = merchantTxs.sortedBy { it.dateTime }
            for (idx in 0 until chronological.size - 1) {
                val earlier = chronological[idx]
                val later = chronological[idx + 1]

                // Ensure both transactions are between 20 and 45 days apart (roughly monthly subscription cycle)
                val daysBetween = Duration.between(earlier.dateTime, later.dateTime).toDays()
                if (daysBetween in 20..45) {
                    val prevAmt = earlier.amount
                    val newAmt = later.amount

                    if (prevAmt > BigDecimal.ZERO && newAmt > prevAmt) {
                        val increase = newAmt.subtract(prevAmt)
                        val pct = increase.divide(prevAmt, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal("100")).toDouble()

                        if (pct in CREEP_MIN_PERCENT..CREEP_MAX_PERCENT) {
                            alerts.add(
                                SubscriptionCreepAlert(
                                    merchantName = later.merchantName,
                                    previousAmount = prevAmt.setScale(2, RoundingMode.HALF_UP),
                                    newAmount = newAmt.setScale(2, RoundingMode.HALF_UP),
                                    increasePercentage = pct,
                                    latestTransaction = later
                                )
                            )
                        }
                    }
                }
            }
        }

        return alerts
    }
}
