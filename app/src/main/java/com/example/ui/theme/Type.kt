package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Static IBM Plex Sans Font Family with separate TTF files for each weight:
// - res/font/ibm_plex_sans_regular.ttf (Weight 400)
// - res/font/ibm_plex_sans_medium.ttf (Weight 500)
// - res/font/ibm_plex_sans_semibold.ttf (Weight 600)
val IbmPlexSansFamily = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
)

// Complete Material 3 Typography Scale (All 15 text styles)
// Scale: 12, 14, 16, 20, 28, and 56 sp. Tabular numerals ("tnum") on every style.
val Typography = Typography(
    // 56 sp: exclusively for the live timer
    displayLarge = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        fontFeatureSettings = "tnum"
    ),
    displayMedium = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontFeatureSettings = "tnum"
    ),
    displaySmall = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontFeatureSettings = "tnum"
    ),
    // 28 sp: for hero quota numbers ("8 of 10 days")
    headlineLarge = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontFeatureSettings = "tnum"
    ),
    headlineMedium = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontFeatureSettings = "tnum"
    ),
    headlineSmall = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontFeatureSettings = "tnum"
    ),
    // 20 sp: for screen/section titles and major dialog headings
    titleLarge = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontFeatureSettings = "tnum"
    ),
    titleMedium = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontFeatureSettings = "tnum"
    ),
    // 16 sp: for list titles, standard buttons, emphasized body
    titleSmall = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = "tnum"
    ),
    bodyLarge = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontFeatureSettings = "tnum"
    ),
    // 14 sp: for secondary text, list subtitles, chips, regular buttons
    bodyMedium = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum"
    ),
    labelLarge = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum"
    ),
    // 12 sp: minimum allowed text size, for timestamps, axis markers, captions
    bodySmall = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    ),
    labelMedium = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    ),
    labelSmall = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    )
)
