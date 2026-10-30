package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Pills for interactive controls
val ButtonShape = CircleShape
val ChipShape = CircleShape

// Sheet shape used explicitly by WfoBottomSheet
val SheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)

// Discrete calendar cell shape
val CalendarCellShape = RoundedCornerShape(4.dp)
val SurfaceGroupShape = RoundedCornerShape(12.dp)

// Material 3 Shapes:
// - small = 4.dp
// - medium, large, extraLarge = 12.dp (ensuring dialogs/pickers don't get pill/circle shape)
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(12.dp)
)
