package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ============================================================================
// WFO TRACKER DESIGN SYSTEM: QUIET UTILITY
// ============================================================================
// Color is used with strict purpose: presence (#5BB5A2) is the primary accent.
// No gradients, no shadows. Surfaces are separated by tone and 1dp Rule lines.

// Dark Theme (Default)
val DarkGround = Color(0xFF15171A)
val DarkRaised = Color(0xFF1E2126)
val DarkRule = Color(0xFF2D3138)
val DarkInk = Color(0xFFECEDEF)
val DarkMuted = Color(0xFF8B919A)

// Status Accents (Dark)
val DarkPresent = Color(0xFF5BB5A2)  // The only accent: primary actions & office day met (7.2:1 on Ground)
val DarkShort = Color(0xFFE0A458)    // Under minimum hours (7.9:1 on Ground)
val DarkDanger = Color(0xFFE5655A)   // Destructive actions (4.7:1 on Ground)

// Light Theme
val LightGround = Color(0xFFF5F6F8)
val LightRaised = Color(0xFFFFFFFF)
val LightRule = Color(0xFFDFE2E6)
val LightInk = Color(0xFF15171A)
val LightMuted = Color(0xFF5E6570)   // 5.2:1 contrast on LightGround

// Status Accents (Light - tuned to ensure >= 4.5:1 contrast on LightRaised & LightGround)
val LightPresent = Color(0xFF247564) // 5.6:1 on LightRaised
val LightShort = Color(0xFF9E5D0A)   // 5.1:1 on LightRaised
val LightDanger = Color(0xFFC53328)  // 5.3:1 on LightRaised

@Immutable
data class AppColors(
    val ground: Color,
    val raised: Color,
    val rule: Color,
    val ink: Color,
    val muted: Color,
    val present: Color,
    val short: Color,
    val danger: Color,
    val isDark: Boolean
)
