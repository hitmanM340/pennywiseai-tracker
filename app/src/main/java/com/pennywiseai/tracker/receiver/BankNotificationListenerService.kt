package com.pennywiseai.tracker.receiver

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.pennywiseai.tracker.data.repository.BankNotificationRepository
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.data.manager.SmsTransactionProcessor
import com.pennywiseai.tracker.data.manager.TransactionDeduplication
import com.pennywiseai.parser.core.bank.BankParserFactory
import com.pennywiseai.tracker.worker.BankNotificationRetryWorker
import com.pennywiseai.tracker.data.mapper.toEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Notification listener that ingests bank app notifications and routes them
 * through the existing parser pipeline.
 */
class BankNotificationListenerService : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationListenerEntryPoint {
        fun smsTransactionProcessor(): SmsTransactionProcessor
        fun bankNotificationRepository(): BankNotificationRepository
        fun transactionRepository(): TransactionRepository
        fun userPreferencesRepository(): UserPreferencesRepository
    }

    companion object {
        private const val TAG = "BankNotificationListener"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return

        // 1. FAST SYNCHRONOUS PRE-CHECK: Drop non-bank notifications instantly
        // to save battery and prevent disk I/O spam
        if (!BankNotificationConfig.isSupportedPackage(packageName) && packageName != "com.google.android.apps.nbu.paisa.user") return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()

        // 2. ISOLATED GPAY HANDLING: Only merge Title + Text for GPay.
        // Preserve EXTRA_BIG_TEXT for SBI and other traditional banks.
        val messageBody = if (packageName == "com.google.android.apps.nbu.paisa.user") {
            "$title $text".trim()
        } else {
            bigText.ifBlank { text }
        }

        if (messageBody.isBlank()) return

        // Skip group summaries to avoid duplicate processing
        if ((sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0) {
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationListenerEntryPoint::class.java
        )
        val processor = entryPoint.smsTransactionProcessor()
        val notificationRepository = entryPoint.bankNotificationRepository()
        val transactionRepository = entryPoint.transactionRepository()
        val userPreferencesRepository = entryPoint.userPreferencesRepository()
        val senderAlias = BankNotificationConfig.senderAlias(packageName)
        val timestamp = sbn.postTime

        // 3. Defer to background worker thread safely
        serviceScope.launch {
            // Double-check user's custom runtime preferences whitelist
            val monitoredPackages = userPreferencesRepository.monitoredBankPackages.first()
            val isWhitelisted = if (monitoredPackages.isEmpty()) {
                packageName.equals("com.google.android.apps.nbu.paisa.user", ignoreCase = true)
            } else {
                monitoredPackages.contains(packageName.lowercase())
            }

            if (!isWhitelisted) {
                Log.d(TAG, "Notification skipped: package $packageName is not in whitelisted packages")
                return@launch
            }

            var notificationId: Long? = null
            try {
                notificationId = notificationRepository.logNotification(
                    packageName = packageName,
                    senderAlias = senderAlias,
                    messageBody = messageBody,
                    postedAtMillis = timestamp
                ).takeIf { it > 0 }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to store bank notification", e)
            }

            try {
<<<<<<< HEAD
                // Two-layer cross-channel dedup. Layer 1: exact transaction
                // hash (the same value the SMS path stores) — identical bodies
                // dedup here. Layer 2: same bank + merchant + amount within a
                // ±2-minute window — catches the same charge whose sender code
                // or body text differs between the SMS and the notification.
                val parsed = BankParserFactory.getParsers(senderAlias)
                    .firstNotNullOfOrNull { it.parse(body, senderAlias, timestamp) }
=======
                // Cross-dedup: check if SMS already created this transaction.
                // Parse first to get amount, then look for an existing transaction
                // with the same amount within a ±2-minute window.
                val parser = BankParserFactory.getParser(senderAlias)
                val parsed = parser?.parse(messageBody, senderAlias, timestamp)
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
                if (parsed != null) {
                    val incoming = parsed.toEntity()
                    val hashMatch = incoming.transactionHash
                        ?.let { transactionRepository.getTransactionByHash(it) } != null
                    val eventTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()
                    )
                    val nearby = transactionRepository.getTransactionByAmountAndDate(
                        parsed.amount,
                        eventTime.minusMinutes(2),
                        eventTime.plusMinutes(2)
                    )
                    if (hashMatch || nearby.any { TransactionDeduplication.isSameCharge(it, incoming) }) {
                        Log.d(TAG, "Notification matches existing transaction (dedup)")
                        notificationId?.let { notificationRepository.markProcessed(it, null) }
                        return@launch
                    }
                }

                val result = processor.processAndSaveTransaction(
                    sender = senderAlias,
                    body = messageBody,
                    timestamp = timestamp
                )
                if (result.success || result.intentionallySkipped) {
                    // An ignored account is settled, not failed — leaving it
                    // unprocessed would have it re-parsed by every retry run
                    // and never cleared (#826).
                    notificationId?.let {
                        notificationRepository.markProcessed(it, result.transactionId)
                    }
                }
                if (!result.success) {
                    Log.d(TAG, "Notification skipped: ${result.reason}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process bank notification", e)
                BankNotificationRetryWorker.enqueue(applicationContext)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
