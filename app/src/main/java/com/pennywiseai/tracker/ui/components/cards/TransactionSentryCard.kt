// [VERIFIED-2X]
package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pennywiseai.tracker.domain.intelligence.SentryAuditReport
import com.pennywiseai.tracker.ui.theme.CapybaraGlassBox
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.RoundingMode

/**
 * Transaction Sentry Card: Autonomous security watchdog displaying duplicate charge
 * warnings, subscription price creep alerts (+5% to +80%), and anomaly reports.
 */
@Composable
fun TransactionSentryCard(
    report: SentryAuditReport,
    currency: String = "INR",
    modifier: Modifier = Modifier
) {
    val hasAlerts = report.duplicateCharges.isNotEmpty() || report.subscriptionCreeps.isNotEmpty()

    CapybaraGlassBox(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (hasAlerts) Color(0xFFEF4444).copy(alpha = 0.2f)
                                else Color(0xFF10B981).copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasAlerts) Icons.Default.Warning else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (hasAlerts) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "Revamped Sentry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasAlerts) Color(0xFFEF4444).copy(alpha = 0.15f)
                    else Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (hasAlerts) "${report.duplicateCharges.size + report.subscriptionCreeps.size} ALERTS"
                        else "PROTECTED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasAlerts) Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!hasAlerts) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "No duplicate charges or hidden subscription hikes detected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Duplicate Charges
                report.duplicateCharges.take(2).forEach { dup ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Duplicate Debit: ${dup.duplicateTransaction.merchantName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Charged twice within ${dup.timeDifferenceSeconds / 60}m",
                                    fontSize = 11.sp,
                                    color = Color(0xFFEF4444)
                                )
                            }
                            Text(
                                text = "$currency ${dup.amount.setScale(2, RoundingMode.HALF_UP)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                // Subscription Creeps
                report.subscriptionCreeps.take(2).forEach { creep ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Price Hike: ${creep.merchantName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "+${String.format("%.1f", creep.increasePercentage)}% increase from $currency ${creep.previousAmount.setScale(0, RoundingMode.HALF_UP)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                            Text(
                                text = "$currency ${creep.newAmount.setScale(2, RoundingMode.HALF_UP)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
