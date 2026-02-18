package com.Linkbyte.Shift.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val White = Color.White

// ============================================
// Modern Dark Palette
// ============================================

// Backgrounds
val DarkBg = Color(0xFF111318)          // Primary background
val DarkSurface = Color(0xFF1A1D24)     // Card / elevated surface
val DarkElevated = Color(0xFF22262E)    // Higher elevation (inputs, modals)
val DarkBorder = Color(0xFF2E323A)      // Subtle border

// Accent Colors
val AccentBlue = Color(0xFF5B7BF0)      // Primary accent
val AccentTeal = Color(0xFF4AD5C7)      // Secondary accent
val AccentPurple = Color(0xFF9B7BF0)    // Tertiary accent

// Functional
val Success = Color(0xFF34D399)
val Error = Color(0xFFEF4444)
val ErrorRed = Error
val Warning = Color(0xFFFBBF24)

// Text (Dark Mode)
val TextPrimary = Color(0xFFF0F0F5)
val TextSecondary = Color(0xFF9298A3)
val TextTertiary = Color(0xFF5A5F6B)

// ============================================
// Modern Light Palette
// ============================================

// Backgrounds (Light Mode)
val LightBg = Color(0xFFFFFFFF)             // Primary background
val LightSurface = Color(0xFFF3F4F6)        // Card / elevated surface
val LightElevated = Color(0xFFE5E7EB)       // Higher elevation (inputs, modals)
val LightBorder = Color(0xFFE5E7EB)         // Subtle border

// Text (Light Mode)
val LightTextPrimary = Color(0xFF111827)
val LightTextSecondary = Color(0xFF6B7280)
val LightTextTertiary = Color(0xFF9CA3AF)

// Accents (Light Mode - adjusted for contrast)
val LightAccentBlue = Color(0xFF2563EB)
val LightAccentTeal = Color(0xFF0D9488)
val LightAccentPurple = Color(0xFF7C3AED)

// Legacy compatibility aliases (used across screens)
val DeepSpaceBlack = DarkBg
val AuroraBackground = DarkBg
val SurfaceDark = DarkSurface
val SurfaceData = DarkElevated
val ElectricViolet = AccentPurple
val NeonCyan = AccentTeal
val VividPink = Color(0xFFE8508A)
val BrightTeal = AccentTeal
val BrandPrimary = AccentBlue
val GlassWhite20 = Color(0x33FFFFFF)
val GlassWhite10 = Color(0x1AFFFFFF)
val GlassWhite05 = Color(0x0DFFFFFF)
val GlassBorder = DarkBorder
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate900 = Color(0xFF0F172A)

// Gradients
val BrandGradientStart = AccentBlue
val BrandGradientEnd = AccentBlue     // Solid — no gradient

val AuroraGradient = Brush.verticalGradient(
    colors = listOf(DarkBg, Color(0xFF141720))
)
