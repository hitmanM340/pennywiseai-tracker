package com.pennywiseai.tracker.domain.intelligence

import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class TransactionSentryEngineTest {

    private lateinit var sentry: TransactionSentryEngine

    @Before
    fun setup() {
        sentry = TransactionSentryEngine()
    }

    @Test
    fun detectDuplicateCharges_flagsTransactionsWithin10Minutes() {
        val now = LocalDateTime.now()
        val tx1 = TransactionEntity(
            id = 101L,
            amount = BigDecimal("450.00"),
            merchantName = "Uber",
            category = "Transportation",
            transactionType = TransactionType.EXPENSE,
            dateTime = now.minusMinutes(5),
            transactionHash = "hash1"
        )
        val tx2 = TransactionEntity(
            id = 102L,
            amount = BigDecimal("450.00"),
            merchantName = "Uber",
            category = "Transportation",
            transactionType = TransactionType.EXPENSE,
            dateTime = now.minusMinutes(2),
            transactionHash = "hash2"
        )
        val tx3 = TransactionEntity(
            id = 103L,
            amount = BigDecimal("450.00"),
            merchantName = "Uber",
            category = "Transportation",
            transactionType = TransactionType.EXPENSE,
            dateTime = now.minusHours(3), // 3 hours ago -> not duplicate
            transactionHash = "hash3"
        )

        val duplicates = sentry.detectDuplicateCharges(listOf(tx1, tx2, tx3))

        assertEquals(1, duplicates.size)
        assertEquals(tx1.id, duplicates[0].originalTransaction.id)
        assertEquals(tx2.id, duplicates[0].duplicateTransaction.id)
        assertEquals(BigDecimal("450.00"), duplicates[0].amount)
    }

    @Test
    fun detectSubscriptionCreep_flagsSubtlePriceIncrease() {
        val now = LocalDateTime.now()
        val prevMonth = TransactionEntity(
            id = 201L,
            amount = BigDecimal("649.00"),
            merchantName = "Netflix",
            category = "Entertainment",
            transactionType = TransactionType.EXPENSE,
            dateTime = now.minusDays(30),
            transactionHash = "net1"
        )
        val currentMonth = TransactionEntity(
            id = 202L,
            amount = BigDecimal("749.00"), // ~15.4% increase (in 5%..80% range)
            merchantName = "Netflix",
            category = "Entertainment",
            transactionType = TransactionType.EXPENSE,
            dateTime = now,
            transactionHash = "net2"
        )

        val creeps = sentry.detectSubscriptionCreep(listOf(prevMonth, currentMonth))

        assertEquals(1, creeps.size)
        assertEquals("Netflix", creeps[0].merchantName)
        assertEquals(BigDecimal("649.00"), creeps[0].previousAmount)
        assertEquals(BigDecimal("749.00"), creeps[0].newAmount)
        assertTrue(creeps[0].increasePercentage in 15.0..16.0)
    }

    @Test
    fun auditTransactions_computesTotalPotentialWaste() {
        val now = LocalDateTime.now()
        val dup1 = TransactionEntity(
            id = 1L,
            amount = BigDecimal("100.00"),
            merchantName = "Coffee",
            category = "Food",
            transactionType = TransactionType.EXPENSE,
            dateTime = now.minusMinutes(4),
            transactionHash = "c1"
        )
        val dup2 = TransactionEntity(
            id = 2L,
            amount = BigDecimal("100.00"),
            merchantName = "Coffee",
            category = "Food",
            transactionType = TransactionType.EXPENSE,
            dateTime = now.minusMinutes(1),
            transactionHash = "c2"
        )
        val sub1 = TransactionEntity(
            id = 3L,
            amount = BigDecimal("500.00"),
            merchantName = "Cloud Storage",
            category = "Services",
            transactionType = TransactionType.EXPENSE,
            dateTime = now.minusDays(30),
            transactionHash = "s1"
        )
        val sub2 = TransactionEntity(
            id = 4L,
            amount = BigDecimal("550.00"), // +50 creep
            merchantName = "Cloud Storage",
            category = "Services",
            transactionType = TransactionType.EXPENSE,
            dateTime = now,
            transactionHash = "s2"
        )

        val report = sentry.auditTransactions(listOf(dup1, dup2, sub1, sub2))

        assertEquals(1, report.duplicateCharges.size)
        assertEquals(1, report.subscriptionCreeps.size)
        // 100 duplicate + 50 creep = 150 potential waste
        assertEquals(BigDecimal("150.00"), report.totalPotentialWaste)
    }
}
