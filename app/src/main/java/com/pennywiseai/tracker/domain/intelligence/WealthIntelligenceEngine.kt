// [VERIFIED-2X]
package com.pennywiseai.tracker.domain.intelligence

import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health assessment of emergency reserves based on monthly burn rate.
 */
enum class RunwayHealth {
    CRITICAL,   // < 1 month runway
    VULNERABLE, // 1 - 3 months runway
    STABLE,     // 3 - 6 months runway
    FORTIFIED   // > 6 months runway
}

/**
 * Snapshot of true balance sheet: Liquid Assets vs Liabilities.
 */
data class NetWorthSnapshot(
    val liquidAssets: BigDecimal,
    val liabilities: BigDecimal,
    val netWorth: BigDecimal,
    val currency: String = "INR"
)

/**
 * Emergency reserve runway projection.
 */
data class EmergencyRunway(
    val monthlyBurnRate: BigDecimal,
    val liquidReserves: BigDecimal,
    val runwayMonths: Double,
    val runwayDays: Int,
    val health: RunwayHealth
)

/**
 * Result of a "What-If" spending simulation.
 */
data class SpendSimulationResult(
    val spendAmount: BigDecimal,
    val currentLiquidReserves: BigDecimal,
    val projectedLiquidReserves: BigDecimal,
    val currentRunwayDays: Int,
    val projectedRunwayDays: Int,
    val isThresholdBreached: Boolean,
    val recoveryDaysNeeded: Int
)

/**
 * Debt item representation for payoff optimization.
 */
data class DebtItem(
    val name: String,
    val balance: BigDecimal,
    val interestRatePercent: Double,
    val minimumMonthlyPayment: BigDecimal
)

/**
 * Comparative payoff analysis between Snowball (smallest balance first)
 * and Avalanche (highest interest first).
 */
data class DebtPayoffComparison(
    val snowballOrder: List<DebtItem>,
    val avalancheOrder: List<DebtItem>,
    val totalDebt: BigDecimal,
    val estimatedAvalancheInterestSaved: BigDecimal,
    val recommendedOrder: List<DebtItem>
)

/**
 * Comprehensive Wealth Intelligence summary.
 */
data class WealthIntelligenceReport(
    val netWorth: NetWorthSnapshot,
    val emergencyRunway: EmergencyRunway,
    val debtOptimization: DebtPayoffComparison?
)

/**
 * Wealth Intelligence Engine: Provides deterministic financial analytics,
 * emergency runway calculations, What-If spending simulations, and debt payoff optimization.
 */
@Singleton
class WealthIntelligenceEngine @Inject constructor() {

    companion object {
        private val DAYS_IN_MONTH = BigDecimal("30.4")
        private const val SAFE_RUNWAY_MONTHS_THRESHOLD = 3.0
    }

    /**
     * Computes the complete wealth intelligence report.
     */
    fun computeWealthReport(
        accounts: List<AccountBalanceEntity>,
        recentTransactions: List<TransactionEntity>,
        debts: List<DebtItem> = emptyList(),
        currency: String = "INR"
    ): WealthIntelligenceReport {
        val netWorth = computeNetWorth(accounts, currency)
        val runway = computeEmergencyRunway(netWorth.liquidAssets, recentTransactions)
        val debtOptimization = if (debts.isNotEmpty()) compareDebtStrategies(debts) else null

        return WealthIntelligenceReport(
            netWorth = netWorth,
            emergencyRunway = runway,
            debtOptimization = debtOptimization
        )
    }

    /**
     * Calculates True Net Worth: Liquid Assets - Liabilities.
     */
    fun computeNetWorth(
        accounts: List<AccountBalanceEntity>,
        currency: String = "INR"
    ): NetWorthSnapshot {
        var liquidAssets = BigDecimal.ZERO
        var liabilities = BigDecimal.ZERO

        for (account in accounts) {
            val balance = account.balance
            val isLiabilityAccount = account.isCreditCard || 
                account.accountType.equals("CREDIT", ignoreCase = true) || 
                account.accountType.equals("LOAN", ignoreCase = true)

            if (isLiabilityAccount) {
                // For credit cards and loans, positive balance represents owed debt
                liabilities = liabilities.add(balance.abs())
            } else {
                if (balance >= BigDecimal.ZERO) {
                    liquidAssets = liquidAssets.add(balance)
                } else {
                    // Overdrafts count as liabilities
                    liabilities = liabilities.add(balance.abs())
                }
            }
        }

        val netWorth = liquidAssets.subtract(liabilities)
        return NetWorthSnapshot(
            liquidAssets = liquidAssets.setScale(2, RoundingMode.HALF_UP),
            liabilities = liabilities.setScale(2, RoundingMode.HALF_UP),
            netWorth = netWorth.setScale(2, RoundingMode.HALF_UP),
            currency = currency
        )
    }

    /**
     * Calculates Emergency Runway based on liquid reserves and 30-day burn rate.
     */
    fun computeEmergencyRunway(
        liquidReserves: BigDecimal,
        transactions: List<TransactionEntity>,
        lookbackDays: Int = 30
    ): EmergencyRunway {
        val cutoff = LocalDateTime.now().minusDays(lookbackDays.toLong())
        var totalBurn = BigDecimal.ZERO

        for (tx in transactions) {
            if (tx.dateTime.isAfter(cutoff) && 
                tx.transactionType == TransactionType.EXPENSE && 
                !tx.isDeleted && 
                !tx.excludedFromAnalytics
            ) {
                totalBurn = totalBurn.add(tx.amount.abs())
            }
        }

        // Normalize burn rate to a monthly (30-day) figure
        val monthlyBurnRate = if (lookbackDays != 30 && lookbackDays > 0) {
            totalBurn.multiply(BigDecimal("30")).divide(BigDecimal(lookbackDays), 2, RoundingMode.HALF_UP)
        } else {
            totalBurn.setScale(2, RoundingMode.HALF_UP)
        }

        val runwayMonths: Double
        val runwayDays: Int
        val health: RunwayHealth

        if (monthlyBurnRate <= BigDecimal.ZERO || liquidReserves <= BigDecimal.ZERO) {
            if (liquidReserves <= BigDecimal.ZERO) {
                runwayMonths = 0.0
                runwayDays = 0
                health = RunwayHealth.CRITICAL
            } else {
                runwayMonths = 99.0
                runwayDays = 999
                health = RunwayHealth.FORTIFIED
            }
        } else {
            val monthsBd = liquidReserves.divide(monthlyBurnRate, 4, RoundingMode.HALF_UP)
            runwayMonths = monthsBd.toDouble()
            runwayDays = monthsBd.multiply(DAYS_IN_MONTH).toInt()

            health = when {
                runwayMonths < 1.0 -> RunwayHealth.CRITICAL
                runwayMonths < 3.0 -> RunwayHealth.VULNERABLE
                runwayMonths < 6.0 -> RunwayHealth.STABLE
                else -> RunwayHealth.FORTIFIED
            }
        }

        return EmergencyRunway(
            monthlyBurnRate = monthlyBurnRate,
            liquidReserves = liquidReserves.setScale(2, RoundingMode.HALF_UP),
            runwayMonths = runwayMonths,
            runwayDays = runwayDays,
            health = health
        )
    }

    /**
     * Simulates the impact of a planned purchase on liquidity and emergency runway.
     */
    fun simulateSpend(
        currentLiquidReserves: BigDecimal,
        monthlyBurnRate: BigDecimal,
        spendAmount: BigDecimal,
        monthlySavingsCapacity: BigDecimal = BigDecimal.ZERO
    ): SpendSimulationResult {
        val projectedLiquidReserves = currentLiquidReserves.subtract(spendAmount).setScale(2, RoundingMode.HALF_UP)
        val dailyBurn = if (monthlyBurnRate > BigDecimal.ZERO) {
            monthlyBurnRate.divide(DAYS_IN_MONTH, 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val currentRunwayDays = if (dailyBurn > BigDecimal.ZERO && currentLiquidReserves > BigDecimal.ZERO) {
            currentLiquidReserves.divide(dailyBurn, 0, RoundingMode.HALF_UP).toInt()
        } else {
            0
        }

        val projectedRunwayDays = if (dailyBurn > BigDecimal.ZERO && projectedLiquidReserves > BigDecimal.ZERO) {
            projectedLiquidReserves.divide(dailyBurn, 0, RoundingMode.HALF_UP).toInt()
        } else {
            0
        }

        val isThresholdBreached = projectedRunwayDays < (SAFE_RUNWAY_MONTHS_THRESHOLD * 30.4).toInt() || 
            projectedLiquidReserves < BigDecimal.ZERO

        val recoveryDaysNeeded = if (monthlySavingsCapacity > BigDecimal.ZERO && spendAmount > BigDecimal.ZERO) {
            val dailySavings = monthlySavingsCapacity.divide(DAYS_IN_MONTH, 4, RoundingMode.HALF_UP)
            spendAmount.divide(dailySavings, 0, RoundingMode.CEILING).toInt()
        } else {
            -1
        }

        return SpendSimulationResult(
            spendAmount = spendAmount.setScale(2, RoundingMode.HALF_UP),
            currentLiquidReserves = currentLiquidReserves.setScale(2, RoundingMode.HALF_UP),
            projectedLiquidReserves = projectedLiquidReserves,
            currentRunwayDays = currentRunwayDays,
            projectedRunwayDays = projectedRunwayDays,
            isThresholdBreached = isThresholdBreached,
            recoveryDaysNeeded = recoveryDaysNeeded
        )
    }

    /**
     * Compares Debt Payoff Strategies:
     * - Snowball: Sorted by smallest balance first (behavioral momentum).
     * - Avalanche: Sorted by highest interest rate first (mathematical optimization).
     */
    fun compareDebtStrategies(
        debts: List<DebtItem>,
        monthlyExtraPayment: BigDecimal = BigDecimal.ZERO
    ): DebtPayoffComparison {
        if (debts.isEmpty()) {
            return DebtPayoffComparison(
                snowballOrder = emptyList(),
                avalancheOrder = emptyList(),
                totalDebt = BigDecimal.ZERO,
                estimatedAvalancheInterestSaved = BigDecimal.ZERO,
                recommendedOrder = emptyList()
            )
        }

        val totalDebt = debts.fold(BigDecimal.ZERO) { acc, debt -> acc.add(debt.balance) }
            .setScale(2, RoundingMode.HALF_UP)

        val snowballOrder = debts.sortedBy { it.balance }
        val avalancheOrder = debts.sortedByDescending { it.interestRatePercent }

        // Estimate interest saved by attacking highest interest rates first
        var estimatedInterestSaved = BigDecimal.ZERO
        if (debts.size > 1) {
            val maxRate = debts.maxOfOrNull { it.interestRatePercent } ?: 0.0
            val minRate = debts.minOfOrNull { it.interestRatePercent } ?: 0.0
            val rateSpread = (maxRate - minRate) / 100.0

            if (rateSpread > 0.0) {
                // Approximate annual interest differential on the average balance over a 12-month horizon
                val diffFactor = BigDecimal.valueOf(rateSpread * 0.45)
                estimatedInterestSaved = totalDebt.multiply(diffFactor).setScale(2, RoundingMode.HALF_UP)
            }
        }

        return DebtPayoffComparison(
            snowballOrder = snowballOrder,
            avalancheOrder = avalancheOrder,
            totalDebt = totalDebt,
            estimatedAvalancheInterestSaved = estimatedInterestSaved,
            recommendedOrder = avalancheOrder
        )
    }
}
