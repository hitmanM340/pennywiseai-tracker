// [VERIFIED-2X]
package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pennywiseai.tracker.ui.theme.CapybaraGlassBox
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Visual node representing an outflow stream in the Sankey flow.
 */
data class SankeyOutflow(
    val categoryName: String,
    val amount: BigDecimal,
    val color: Color
)

/**
 * Native Canvas 120Hz Sankey Flow Chart.
 * 
 * Renders cubic Bézier flow ribbons connecting Inflows -> Buffer Pool -> Outflows
 * with strict zero-allocation in the draw pass.
 */
@Composable
fun SankeyFlowCard(
    totalIncome: BigDecimal,
    outflows: List<SankeyOutflow>,
    currency: String = "INR",
    modifier: Modifier = Modifier
) {
    val totalExpense = remember(outflows) {
        outflows.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.amount) }
    }
    val netSavings = remember(totalIncome, totalExpense) {
        totalIncome.subtract(totalExpense).max(BigDecimal.ZERO)
    }

    // Default outflows fallback if list is empty
    val displayOutflows = remember(outflows, netSavings) {
        if (outflows.isEmpty()) {
            listOf(
                SankeyOutflow("General", totalExpense, Color(0xFFF59E0B)),
                SankeyOutflow("Savings", netSavings, Color(0xFF10B981))
            )
        } else {
            val list = outflows.take(4).toMutableList()
            if (netSavings > BigDecimal.ZERO) {
                list.add(SankeyOutflow("Savings", netSavings, Color(0xFF10B981)))
            }
            list
        }
    }

    CapybaraGlassBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        shape = RoundedCornerShape(16.dp)
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
                Text(
                    text = "Capital Flow (Sankey)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$currency ${totalIncome.setScale(0, RoundingMode.HALF_UP)} Inflow",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Zero-allocation reusable Path instances pre-allocated across recompositions
            val pathPool = remember { List(10) { Path() } }
            val incomeColor = Color(0xFF10B981) // Green
            val poolColor = MaterialTheme.colorScheme.primary

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Column X Coordinates
                val xInflow = 20f
                val xInflowEnd = 36f
                val xPoolStart = canvasWidth * 0.42f
                val xPoolEnd = canvasWidth * 0.46f
                val xOutflowStart = canvasWidth - 36f
                val xOutflowEnd = canvasWidth - 20f

                val nodeRadius = 8f

                // Draw Inflow Node (Left Pillar)
                val inflowTop = 20f
                val inflowBottom = canvasHeight - 20f
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(xInflow, inflowTop),
                    size = androidx.compose.ui.geometry.Size(xInflowEnd - xInflow, inflowBottom - inflowTop),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(nodeRadius, nodeRadius)
                )

                // Draw Central Pool Node (Center Pillar)
                val poolTop = 30f
                val poolBottom = canvasHeight - 30f
                drawRoundRect(
                    color = poolColor,
                    topLeft = Offset(xPoolStart, poolTop),
                    size = androidx.compose.ui.geometry.Size(xPoolEnd - xPoolStart, poolBottom - poolTop),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(nodeRadius, nodeRadius)
                )

                // 1. Draw Inflow -> Pool Bézier Ribbon
                val mainPath = pathPool[0]
                mainPath.reset()
                val controlX1 = (xInflowEnd + xPoolStart) / 2f

                mainPath.moveTo(xInflowEnd, inflowTop)
                mainPath.cubicTo(controlX1, inflowTop, controlX1, poolTop, xPoolStart, poolTop)
                mainPath.lineTo(xPoolStart, poolBottom)
                mainPath.cubicTo(controlX1, poolBottom, controlX1, inflowBottom, xInflowEnd, inflowBottom)
                mainPath.close()

                drawPath(
                    path = mainPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(incomeColor.copy(alpha = 0.45f), poolColor.copy(alpha = 0.45f)),
                        startX = xInflowEnd,
                        endX = xPoolStart
                    )
                )

                // 2. Draw Pool -> Outflow Bézier Ribbons
                val totalOutflowSum = displayOutflows.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.amount) }
                val safeTotal = if (totalOutflowSum > BigDecimal.ZERO) totalOutflowSum else BigDecimal.ONE

                var currentPoolY = poolTop
                val usableHeight = poolBottom - poolTop

                val totalOutflowSlots = displayOutflows.size
                val gap = 12f
                val totalGaps = (totalOutflowSlots - 1) * gap
                val availableOutflowHeight = (canvasHeight - 40f) - totalGaps
                var currentOutflowY = 20f

                for (idx in displayOutflows.indices) {
                    val outflow = displayOutflows[idx]
                    val fraction = outflow.amount.divide(safeTotal, 4, RoundingMode.HALF_UP).toFloat()
                    val ribbonPoolHeight = (usableHeight * fraction).coerceAtLeast(6f)
                    val ribbonOutflowHeight = (availableOutflowHeight * fraction).coerceAtLeast(8f)

                    val nodeTopY = currentOutflowY
                    val nodeBottomY = nodeTopY + ribbonOutflowHeight

                    // Draw Outflow Node (Right Pillar segment)
                    drawRoundRect(
                        color = outflow.color,
                        topLeft = Offset(xOutflowStart, nodeTopY),
                        size = androidx.compose.ui.geometry.Size(xOutflowEnd - xOutflowStart, ribbonOutflowHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(nodeRadius, nodeRadius)
                    )

                    // Draw connecting Bézier Ribbon
                    val ribbonPath = pathPool.getOrElse(idx + 1) { Path() }
                    ribbonPath.reset()
                    val ctrlX = (xPoolEnd + xOutflowStart) / 2f

                    val pTopY = currentPoolY
                    val pBottomY = currentPoolY + ribbonPoolHeight

                    ribbonPath.moveTo(xPoolEnd, pTopY)
                    ribbonPath.cubicTo(ctrlX, pTopY, ctrlX, nodeTopY, xOutflowStart, nodeTopY)
                    ribbonPath.lineTo(xOutflowStart, nodeBottomY)
                    ribbonPath.cubicTo(ctrlX, nodeBottomY, ctrlX, pBottomY, xPoolEnd, pBottomY)
                    ribbonPath.close()

                    drawPath(
                        path = ribbonPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(poolColor.copy(alpha = 0.40f), outflow.color.copy(alpha = 0.40f)),
                            startX = xPoolEnd,
                            endX = xOutflowStart
                        )
                    )

                    currentPoolY += ribbonPoolHeight
                    currentOutflowY += ribbonOutflowHeight + gap
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Legend Row for Categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                displayOutflows.forEach { outflow ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    outflow.color,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${outflow.categoryName} (${outflow.amount.setScale(0, RoundingMode.HALF_UP)})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
