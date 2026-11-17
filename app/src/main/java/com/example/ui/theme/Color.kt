package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ============================================================================
// WFO TRACKER DESIGN SYSTEM: V2 LIGHT-FIRST GREEN + SCENE PALETTE
// ============================================================================

// Dark Theme
val DarkGround = Color(0xFF0B1F17)
val DarkRaised = Color(0xFF12291F)
val DarkSheet = Color(0xFF17342A)
val DarkCard = Color(0xFF12291F)
val DarkRule = Color(0xFF1F3A2D)
val DarkInk = Color(0xFFE8F5EC)
val DarkMuted = Color(0xFF9FB5A8)

// Status & Brand Accents (Dark)
val DarkPresent = Color(0xFF4ADE80)
val DarkPresentDeep = Color(0xFF4ADE80)
val DarkOnPresent = Color(0xFF0B1F17)
val DarkPresentContainer = Color(0xFF1B4332)
val DarkProgress = Color(0xFF4ADE80)
val DarkActionStart = Color(0xFF4ADE80)
val DarkActionEnd = Color(0xFF34D399)
val DarkOnAction = Color(0xFF0B1F17)
val DarkTrackEmpty = Color(0xFF2E5744)
val DarkTrackOutline = Color(0xFF4A7E66)
val DarkShort = Color(0xFFF59E0B)
val DarkDanger = Color(0xFFF87171)
val DarkReward = Color(0xFFFBBF24)

// Light Theme
val LightGround = Color(0xFFF0FDF4)
val LightRaised = Color(0xFFDCFCE7)
val LightSheet = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightRule = Color(0xFFBBE5C8)
val LightInk = Color(0xFF0B1F17)
val LightMuted = Color(0xFF5B7266)

// Status & Brand Accents (Light)
// LightPresent is for fills only (paired with LightOnPresent); LightPresentDeep is for green text, links, and icons.
val LightPresent = Color(0xFF16A34A)
val LightPresentDeep = Color(0xFF065F46)
val LightOnPresent = Color(0xFF0B1F17)
val LightPresentContainer = Color(0xFFBBF7D0)
val LightProgress = Color(0xFF15803D)
val LightActionStart = Color(0xFF15803D)
val LightActionEnd = Color(0xFF166534)
val LightOnAction = Color(0xFFFFFFFF)
val LightTrackEmpty = Color(0xFFECF7F0)
val LightTrackOutline = Color(0xFF7A9485)
val LightShort = Color(0xFFB45309)
val LightDanger = Color(0xFFB91C1C)
val LightReward = Color(0xFFD97706)

val NightSceneInk = Color(0xFFE8F5EC)

@Immutable
data class AppColors(
    val ground: Color,
    val raised: Color,
    val sheet: Color,
    val card: Color,
    val rule: Color,
    val ink: Color,
    val muted: Color,
    val present: Color,
    val presentDeep: Color,
    val onPresent: Color,
    val presentContainer: Color,
    val progress: Color,
    val actionStart: Color,
    val actionEnd: Color,
    val onAction: Color,
    val trackEmpty: Color,
    val trackOutline: Color,
    val short: Color,
    val danger: Color,
    val reward: Color,
    val shadowInk: Color,
    val isDark: Boolean
)
