package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Pills for interactive controls
val ButtonShape = CircleShape
val ChipShape = CircleShape

// Sheet shape used explicitly by WfoBottomSheet
val SheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

// Discrete calendar cell shape & card/dialog group shape
val CalendarCellShape = RoundedCornerShape(8.dp)
val SurfaceGroupShape = RoundedCornerShape(16.dp)

// Material 3 Shapes:
// - extraSmall, small = 8.dp
// - medium, large, extraLarge = 16.dp (cards, dialogs, pickers)
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp)
)
