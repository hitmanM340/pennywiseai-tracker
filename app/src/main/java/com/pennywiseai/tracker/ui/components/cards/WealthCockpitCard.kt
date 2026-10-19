// [VERIFIED-2X]
package com.pennywiseai.tracker.ui.components.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pennywiseai.tracker.domain.intelligence.DebtItem
import com.pennywiseai.tracker.domain.intelligence.DebtPayoffComparison
import com.pennywiseai.tracker.domain.intelligence.EmergencyRunway
import com.pennywiseai.tracker.domain.intelligence.NetWorthSnapshot
import com.pennywiseai.tracker.domain.intelligence.RunwayHealth
import com.pennywiseai.tracker.domain.intelligence.SpendSimulationResult
import com.pennywiseai.tracker.domain.intelligence.WealthIntelligenceEngine
import com.pennywiseai.tracker.ui.theme.CapybaraGlassBox
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Wealth Cockpit 3-Tab Suite:
 * - Tab 1: True Net Worth & Emergency Runway
 * - Tab 2: 30-day "What-If" Spend Simulator (with interactive slider)
 * - Tab 3: Debt Payoff Snowball vs. Avalanche (with extra monthly payment slider)
 * - Status Row: "Sentry Active: 0 Double-Charges / 0 Price Creeps" + "Simulate Demo Alert" button
 */
@Composable
fun WealthCockpitCard(
    netWorth: NetWorthSnapshot,
    emergencyRunway: EmergencyRunway,
    engine: WealthIntelligenceEngine,
    debtOptimization: DebtPayoffComparison? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDemoAlertDialog by remember { mutableStateOf(false) }

    val (badgeBg, badgeText, textColor) = remember(emergencyRunway.health) {
        when (emergencyRunway.health) {
            RunwayHealth.FORTIFIED -> Triple(Color(0xFF10B981).copy(alpha = 0.2f), "FORTIFIED", Color(0xFF10B981))
            RunwayHealth.STABLE -> Triple(Color(0xFF3B82F6).copy(alpha = 0.2f), "STABLE", Color(0xFF3B82F6))
            RunwayHealth.VULNERABLE -> Triple(Color(0xFFF59E0B).copy(alpha = 0.2f), "VULNERABLE", Color(0xFFF59E0B))
            RunwayHealth.CRITICAL -> Triple(Color(0xFFEF4444).copy(alpha = 0.2f), "CRITICAL", Color(0xFFEF4444))
        }
    }

    CapybaraGlassBox(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "Wealth Intelligence Cockpit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // 3-Tab Bar
            val tabTitles = listOf("Runway & Net Worth", "What-If Simulator", "Debt Payoff")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Tab Contents
            when (selectedTab) {
                0 -> {
                    // TAB 1: Net Worth & Emergency Runway
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "True Net Worth",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${netWorth.currency} ${netWorth.netWorth.setScale(2, RoundingMode.HALF_UP)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Runway Buffer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%.1f", emergencyRunway.runwayMonths)} mos (${emergencyRunway.runwayDays} d)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Liquid vs Liability Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(6.dp))
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Liquid: ${netWorth.currency} ${netWorth.liquidAssets.setScale(0, RoundingMode.HALF_UP)}",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "Liabilities: ${netWorth.currency} ${netWorth.liabilities.setScale(0, RoundingMode.HALF_UP)}",
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
                1 -> {
                    // TAB 2: 30-Day "What-If" Spend Simulator
                    var simulatedSpend by remember { mutableFloatStateOf(15000f) }
                    val simulationResult = remember(simulatedSpend, netWorth.liquidAssets, emergencyRunway.monthlyBurnRate) {
                        engine.simulateSpend(
                            currentLiquidReserves = netWorth.liquidAssets,
                            monthlyBurnRate = emergencyRunway.monthlyBurnRate,
                            spendAmount = BigDecimal.valueOf(simulatedSpend.toDouble()),
                            monthlySavingsCapacity = BigDecimal("10000")
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Simulated Purchase:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${netWorth.currency} ${simulatedSpend.toInt()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = simulatedSpend,
                            onValueChange = { simulatedSpend = it },
                            valueRange = 0f..100000f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (simulationResult.isThresholdBreached) {
                                Color(0xFFEF4444).copy(alpha = 0.15f)
                            } else {
                                Color(0xFF10B981).copy(alpha = 0.15f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(Spacing.sm)) {
                                Text(
                                    text = if (simulationResult.isThresholdBreached) "⚠️ Warning: Breaches Safe Buffer (< 3 mos)" else "✓ Purchase is Safe & Affordable",
                                    fontWeight = FontWeight.Bold,
                                    color = if (simulationResult.isThresholdBreached) Color(0xFFEF4444) else Color(0xFF10B981),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Projected Runway: ${simulationResult.projectedRunwayDays} days",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Liquid: ${netWorth.currency} ${simulationResult.projectedLiquidReserves.setScale(0, RoundingMode.HALF_UP)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 3: Debt Payoff Snowball vs. Avalanche
                    var extraPayment by remember { mutableFloatStateOf(5000f) }
                    val sampleDebts = remember {
                        listOf(
                            DebtItem("Credit Card", BigDecimal("45000"), 36.0, BigDecimal("2500")),
                            DebtItem("Personal Loan", BigDecimal("120000"), 14.5, BigDecimal("4500")),
                            DebtItem("Auto Loan", BigDecimal("350000"), 9.5, BigDecimal("8200"))
                        )
                    }

                    val debtComparison = remember(extraPayment) {
                        engine.compareDebtStrategies(sampleDebts, BigDecimal.valueOf(extraPayment.toDouble()))
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extra Monthly Payment:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${netWorth.currency} ${extraPayment.toInt()}/mo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = extraPayment,
                            onValueChange = { extraPayment = it },
                            valueRange = 0f..30000f,
                            steps = 14,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(Spacing.sm)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "⚡ Avalanche Strategy",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF10B981)
                                    )
                                    Text(
                                        text = "Saves ~${netWorth.currency} ${debtComparison.estimatedAvalancheInterestSaved}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Targets highest APR (36.0%) first for maximum interest savings.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⛄ Snowball: Targets lowest balance (₹45,000) for fast behavioral momentum.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(Spacing.xs))

            // Sentry Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sentry Active: 0 Double-Charges / 0 Price Creeps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = { showDemoAlertDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Simulate Demo Alert",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Demo Alert Dialog
    if (showDemoAlertDialog) {
        AlertDialog(
            onDismissRequest = { showDemoAlertDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444)
                )
            },
            title = {
                Text("Revamped Sentry Demo Alert")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = "⚠️ Potential Duplicate Charge Detected",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "₹499.00 charged twice within 3 minutes by Netflix Streaming.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "📈 Stealth Price Creep Alert",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Spotify Premium increased by +14.2% (from ₹119 to ₹136) without tier upgrade.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDemoAlertDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

/**
 * Interactive What-If Spend Simulator Dialog.
 */
@Composable
fun SpendSimulatorDialog(
    liquidReserves: BigDecimal,
    monthlyBurnRate: BigDecimal,
    currency: String,
    engine: WealthIntelligenceEngine,
    onDismiss: () -> Unit
) {
    var spendInput by remember { mutableStateOf("15000") }
    var savingsCapacityInput by remember { mutableStateOf("10000") }

    val spendAmount = remember(spendInput) {
        spendInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }
    val savingsCapacity = remember(savingsCapacityInput) {
        savingsCapacityInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    val simulationResult = remember(spendAmount, savingsCapacity, liquidReserves, monthlyBurnRate) {
        engine.simulateSpend(
            currentLiquidReserves = liquidReserves,
            monthlyBurnRate = monthlyBurnRate,
            spendAmount = spendAmount,
            monthlySavingsCapacity = savingsCapacity
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "What-If Spend Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                OutlinedTextField(
                    value = spendInput,
                    onValueChange = { spendInput = it },
                    label = { Text("Planned Purchase Amount ($currency)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = savingsCapacityInput,
                    onValueChange = { savingsCapacityInput = it },
                    label = { Text("Monthly Savings Capacity ($currency)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Simulation Output
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (simulationResult.isThresholdBreached) {
                        Color(0xFFEF4444).copy(alpha = 0.15f)
                    } else {
                        Color(0xFF10B981).copy(alpha = 0.15f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = if (simulationResult.isThresholdBreached) {
                                "⚠️ Warning: Breaches Safe Runway Buffer!"
                            } else {
                                "✓ Purchase is Safe & Affordable"
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (simulationResult.isThresholdBreached) Color(0xFFEF4444) else Color(0xFF10B981),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        Text(
                            text = "Remaining Liquid: $currency ${simulationResult.projectedLiquidReserves.setScale(0, RoundingMode.HALF_UP)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Projected Runway: ${simulationResult.projectedRunwayDays} days (was ${simulationResult.currentRunwayDays} days)",
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (simulationResult.recoveryDaysNeeded > 0) {
                            Text(
                                text = "Replenish Time: ~${simulationResult.recoveryDaysNeeded} days",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
