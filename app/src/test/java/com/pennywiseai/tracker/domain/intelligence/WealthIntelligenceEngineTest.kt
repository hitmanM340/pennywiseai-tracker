package com.pennywiseai.tracker.domain.intelligence

import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class WealthIntelligenceEngineTest {

    private lateinit var engine: WealthIntelligenceEngine

    @Before
    fun setup() {
        engine = WealthIntelligenceEngine()
    }

    @Test
    fun computeNetWorth_calculatesCorrectLiquidAndLiabilities() {
        val now = LocalDateTime.now()
        val accounts = listOf(
            AccountBalanceEntity(
                bankName = "HDFC Bank",
                accountLast4 = "1234",
                balance = BigDecimal("150000.00"),
                timestamp = now,
                accountType = "SAVINGS",
                isCreditCard = false
            ),
            AccountBalanceEntity(
                bankName = "ICICI Bank",
                accountLast4 = "5678",
                balance = BigDecimal("50000.00"),
                timestamp = now,
                accountType = "CURRENT",
                isCreditCard = false
            ),
            AccountBalanceEntity(
                bankName = "Axis Credit Card",
                accountLast4 = "9999",
                balance = BigDecimal("35000.00"),
                timestamp = now,
                accountType = "CREDIT",
                isCreditCard = true
            ),
            AccountBalanceEntity(
                bankName = "Car Loan",
                accountLast4 = "8888",
                balance = BigDecimal("65000.00"),
                timestamp = now,
                accountType = "LOAN",
                isCreditCard = false
            )
        )

        val snapshot = engine.computeNetWorth(accounts, "INR")

        assertEquals(BigDecimal("200000.00"), snapshot.liquidAssets)
        assertEquals(BigDecimal("100000.00"), snapshot.liabilities)
        assertEquals(BigDecimal("100000.00"), snapshot.netWorth)
    }

    @Test
    fun computeEmergencyRunway_evaluatesHealthAccurately() {
        val now = LocalDateTime.now()
        val transactions = listOf(
            TransactionEntity(
                id = 1L,
                amount = BigDecimal("25000.00"),
                merchantName = "Rent",
                category = "Housing",
                transactionType = TransactionType.EXPENSE,
                dateTime = now.minusDays(5),
                transactionHash = "h1"
            ),
            TransactionEntity(
                id = 2L,
                amount = BigDecimal("15000.00"),
                merchantName = "Supermarket",
                category = "Groceries",
                transactionType = TransactionType.EXPENSE,
                dateTime = now.minusDays(10),
                transactionHash = "h2"
            )
        )

        // Total burn = 40,000. Liquid reserves = 160,000. Runway = 4 months (STABLE)
        val runway = engine.computeEmergencyRunway(BigDecimal("160000.00"), transactions, 30)

        assertEquals(BigDecimal("40000.00"), runway.monthlyBurnRate)
        assertEquals(4.0, runway.runwayMonths, 0.05)
        assertEquals(RunwayHealth.STABLE, runway.health)
    }

    @Test
    fun simulateSpend_detectsRunwayBreach() {
        // Liquid: 100,000. Burn: 40,000 / month. Daily burn ~ 1315
        // Spending 80,000 leaves 20,000 reserves (~15 days runway < 90 days threshold) -> Breached!
        val result = engine.simulateSpend(
            currentLiquidReserves = BigDecimal("100000.00"),
            monthlyBurnRate = BigDecimal("40000.00"),
            spendAmount = BigDecimal("80000.00"),
            monthlySavingsCapacity = BigDecimal("15000.00")
        )

        assertEquals(BigDecimal("20000.00"), result.projectedLiquidReserves)
        assertTrue(result.isThresholdBreached)
        assertTrue(result.recoveryDaysNeeded > 0)
    }

    @Test
    fun compareDebtStrategies_sortsCorrectlyForSnowballAndAvalanche() {
        val debts = listOf(
            DebtItem("Credit Card A", BigDecimal("45000.00"), 36.0, BigDecimal("2000.00")),
            DebtItem("Personal Loan B", BigDecimal("12000.00"), 14.5, BigDecimal("1000.00")),
            DebtItem("Auto Loan C", BigDecimal("95000.00"), 9.5, BigDecimal("3500.00"))
        )

        val comparison = engine.compareDebtStrategies(debts)

        // Snowball: smallest balance first (12k, then 45k, then 95k)
        assertEquals("Personal Loan B", comparison.snowballOrder[0].name)
        assertEquals("Credit Card A", comparison.snowballOrder[1].name)
        assertEquals("Auto Loan C", comparison.snowballOrder[2].name)

        // Avalanche: highest interest first (36%, then 14.5%, then 9.5%)
        assertEquals("Credit Card A", comparison.avalancheOrder[0].name)
        assertEquals("Personal Loan B", comparison.avalancheOrder[1].name)
        assertEquals("Auto Loan C", comparison.avalancheOrder[2].name)

        assertTrue(comparison.estimatedAvalancheInterestSaved > BigDecimal.ZERO)
    }
}
