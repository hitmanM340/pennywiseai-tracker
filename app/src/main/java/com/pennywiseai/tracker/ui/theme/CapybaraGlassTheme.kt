// [VERIFIED-2X]
package com.pennywiseai.tracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Surface material rendering modes for Capybara Wallet.
 */
enum class SurfaceMaterialStyle {
    /** Dynamic glassmorphism with translucency and specular gradient rim */
    LIQUID_GLASS,
    /** Pitch black #000000 AMOLED with high contrast razor borders and 0% glass overhead */
    PURE_AMOLED,
    /** Soft diffused acrylic with frosted matte finish */
    FROSTED_ACRYLIC,
    /** Flat minimal cards with solid background and clean borders */
    MATTE_MINIMAL
}

/**
 * Signature color palettes for Capybara Wallet.
 */
enum class CapybaraPalette(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val surfaceColor: Color,
    val accentGlow: Color
) {
    YUZU_ESPRESSO(
        displayName = "Yuzu Espresso",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFD97706),
        tertiaryColor = Color(0xFF78350F),
        surfaceColor = Color(0xFF1E1711),
        accentGlow = Color(0xFFFBBF24)
    ),
    EMERALD_CYBER(
        displayName = "Emerald Cyber",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF059669),
        tertiaryColor = Color(0xFF064E3B),
        surfaceColor = Color(0xFF0D1D16),
        accentGlow = Color(0xFF34D399)
    ),
    SAKURA_QUARTZ(
        displayName = "Sakura Quartz",
        primaryColor = Color(0xFFEC4899),
        secondaryColor = Color(0xFFDB2777),
        tertiaryColor = Color(0xFF831843),
        surfaceColor = Color(0xFF1E0E17),
        accentGlow = Color(0xFFF472B6)
    ),
    ARCTIC_SAPPHIRE(
        displayName = "Arctic Sapphire",
        primaryColor = Color(0xFF3B82F6),
        secondaryColor = Color(0xFF2563EB),
        tertiaryColor = Color(0xFF1E3A8A),
        surfaceColor = Color(0xFF0D1626),
        accentGlow = Color(0xFF60A5FA)
    ),
    OBSIDIAN_GOLD(
        displayName = "Obsidian Gold",
        primaryColor = Color(0xFFEAB308),
        secondaryColor = Color(0xFFCA8A04),
        tertiaryColor = Color(0xFF713F12),
        surfaceColor = Color(0xFF141414),
        accentGlow = Color(0xFFFDE047)
    ),
    NORDIC_FROST(
        displayName = "Nordic Frost",
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFF0891B2),
        tertiaryColor = Color(0xFF164E63),
        surfaceColor = Color(0xFF0B1B20),
        accentGlow = Color(0xFF22D3EE)
    )
}

/**
 * Cached styling configuration to ensure ZERO memory allocation during drawing.
 */
@Immutable
data class GlassRenderStyle(
    val backgroundBrush: Brush,
    val borderBrush: Brush,
    val borderWidth: Dp,
    val shape: Shape
)

/**
 * Ultra-high-performance Glass/AMOLED surface container with zero allocations in draw loop.
 */
@Composable
fun CapybaraGlassBox(
    modifier: Modifier = Modifier,
    materialStyle: SurfaceMaterialStyle = SurfaceMaterialStyle.LIQUID_GLASS,
    palette: CapybaraPalette = CapybaraPalette.YUZU_ESPRESSO,
    glassIntensity: Float = 0.7f,
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val renderStyle = remember(materialStyle, palette, glassIntensity, shape, borderWidth) {
        val clampedIntensity = glassIntensity.coerceIn(0.1f, 1.0f)
        when (materialStyle) {
            SurfaceMaterialStyle.PURE_AMOLED -> {
                GlassRenderStyle(
                    backgroundBrush = Brush.linearGradient(listOf(Color.Black, Color.Black)),
                    borderBrush = Brush.linearGradient(
                        listOf(
                            palette.primaryColor.copy(alpha = 0.35f),
                            Color(0xFF222222)
                        )
                    ),
                    borderWidth = 1.dp,
                    shape = shape
                )
            }
            SurfaceMaterialStyle.LIQUID_GLASS -> {
                val baseSurface = palette.surfaceColor
                val topHighlight = palette.accentGlow.copy(alpha = 0.25f * clampedIntensity)
                val bottomShadow = baseSurface.copy(alpha = 0.85f * clampedIntensity)
                GlassRenderStyle(
                    backgroundBrush = Brush.verticalGradient(
                        listOf(
                            baseSurface.copy(alpha = 0.70f * clampedIntensity),
                            baseSurface.copy(alpha = 0.90f * clampedIntensity)
                        )
                    ),
                    borderBrush = Brush.verticalGradient(
                        listOf(
                            topHighlight,
                            palette.primaryColor.copy(alpha = 0.15f * clampedIntensity),
                            bottomShadow.copy(alpha = 0.30f)
                        )
                    ),
                    borderWidth = borderWidth,
                    shape = shape
                )
            }
            SurfaceMaterialStyle.FROSTED_ACRYLIC -> {
                val surfaceBg = palette.surfaceColor.copy(alpha = 0.88f)
                GlassRenderStyle(
                    backgroundBrush = Brush.linearGradient(
                        listOf(surfaceBg, palette.surfaceColor.copy(alpha = 0.95f))
                    ),
                    borderBrush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            palette.primaryColor.copy(alpha = 0.12f)
                        )
                    ),
                    borderWidth = borderWidth,
                    shape = shape
                )
            }
            SurfaceMaterialStyle.MATTE_MINIMAL -> {
                GlassRenderStyle(
                    backgroundBrush = Brush.linearGradient(
                        listOf(palette.surfaceColor, palette.surfaceColor)
                    ),
                    borderBrush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    ),
                    borderWidth = borderWidth,
                    shape = shape
                )
            }
        }
    }

    Box(
        modifier = modifier
            .clip(renderStyle.shape)
            .background(renderStyle.backgroundBrush)
            .border(renderStyle.borderWidth, renderStyle.borderBrush, renderStyle.shape),
        content = content
    )
}
