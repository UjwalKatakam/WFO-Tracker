package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.CalendarCellShape
import com.example.ui.theme.ChipShape
import com.example.ui.theme.SheetShape
import com.example.ui.theme.SurfaceGroupShape

// ============================================================================
// 1. BUTTON SET: Primary, Tonal, Text
// ============================================================================

/**
 * Filled primary button: Present background, high-contrast dark/light on-color,
 * fully rounded (CircleShape pill), minimum 48dp touch target, sentence case label.
 */
@Composable
fun WfoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isDanger: Boolean = false
) {
    val containerColor = if (isDanger) AppTheme.colors.danger else AppTheme.colors.present
    val contentColor = if (AppTheme.colors.isDark) AppTheme.colors.ground else Color.White

    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = AppTheme.colors.raised,
            disabledContentColor = AppTheme.colors.muted
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Tonal button: Raised surface with a 1dp Rule line border, Ink text,
 * fully rounded pill, minimum 48dp touch target.
 */
@Composable
fun WfoTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = ButtonShape,
        border = BorderStroke(1.dp, AppTheme.colors.rule),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppTheme.colors.raised,
            contentColor = AppTheme.colors.ink,
            disabledContainerColor = AppTheme.colors.raised.copy(alpha = 0.5f),
            disabledContentColor = AppTheme.colors.muted
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = AppTheme.colors.ink
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Clean text button: minimum 48dp touch target, Ink or Danger label.
 */
@Composable
fun WfoTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDanger: Boolean = false,
    icon: ImageVector? = null
) {
    val textColor = when {
        !enabled -> AppTheme.colors.muted
        isDanger -> AppTheme.colors.danger
        else -> AppTheme.colors.ink
    }

    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.textButtonColors(
            contentColor = textColor,
            disabledContentColor = AppTheme.colors.muted
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = textColor
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

// ============================================================================
// 2. CHIP
// ============================================================================

/**
 * Filter and selection chip: uses Surface(selected = ...) overload,
 * fully rounded pill, 48dp minimum touch target, sentence case, Tab role.
 */
@Composable
fun WfoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val backgroundColor = when {
        !enabled -> AppTheme.colors.raised.copy(alpha = 0.5f)
        selected -> AppTheme.colors.present.copy(alpha = if (AppTheme.colors.isDark) 0.16f else 0.12f)
        else -> AppTheme.colors.raised
    }
    val borderColor = when {
        !enabled -> AppTheme.colors.rule.copy(alpha = 0.5f)
        selected -> AppTheme.colors.present
        else -> AppTheme.colors.rule
    }
    val textColor = when {
        !enabled -> AppTheme.colors.muted.copy(alpha = 0.5f)
        selected -> AppTheme.colors.present
        else -> AppTheme.colors.ink
    }

    Surface(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        shape = ChipShape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .semantics {
                role = Role.Tab
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

// ============================================================================
// 3. LIST ROW
// ============================================================================

/**
 * Clean list row: 16sp Ink title, 14sp Muted subtitle, separated by a 1dp Rule line.
 * Minimum 48dp height placed before padding in the chain for accurate ~52dp single-line height.
 */
@Composable
fun WfoListRow(
    title: String,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isDanger: Boolean = false,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactiveModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .then(interactiveModifier)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDanger) AppTheme.colors.danger else AppTheme.colors.ink,
                    fontWeight = FontWeight.Normal
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.muted
                    )
                }
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailingContent()
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = AppTheme.colors.rule,
                thickness = 1.dp
            )
        }
    }
}

// ============================================================================
// 4. SECTION HEADER
// ============================================================================

/**
 * Section header: plain text only, no icons, no all-caps, no emojis.
 * Sentence case, clean quiet utility style with semantics heading().
 */
@Composable
fun WfoSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = AppTheme.colors.muted,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { heading() }
    )
}

// ============================================================================
// 5. SWITCH ROW
// ============================================================================

/**
 * Switch row: defaultMinSize before padding, 1dp Rule separator, Present accent switch.
 * Uses Modifier.toggleable(role = Role.Switch) on the row with Switch(onCheckedChange = null).
 */
@Composable
fun WfoSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) AppTheme.colors.ink else AppTheme.colors.muted
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.muted
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (AppTheme.colors.isDark) AppTheme.colors.ground else Color.White,
                    checkedTrackColor = AppTheme.colors.present,
                    uncheckedThumbColor = AppTheme.colors.muted,
                    uncheckedTrackColor = AppTheme.colors.raised,
                    uncheckedBorderColor = AppTheme.colors.rule
                )
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = AppTheme.colors.rule,
                thickness = 1.dp
            )
        }
    }
}

// ============================================================================
// 6. BOTTOM SHEET SCAFFOLD
// ============================================================================

/**
 * Standard bottom sheet scaffold: 12dp top corners (SheetShape), Raised background,
 * optional title with 1dp Rule line, and high-contrast content area.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WfoBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = AppTheme.colors.raised,
        contentColor = AppTheme.colors.ink,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(AppTheme.colors.rule, CircleShape)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            if (!title.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.colors.ink
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close sheet",
                            tint = AppTheme.colors.muted
                        )
                    }
                }
                HorizontalDivider(
                    color = AppTheme.colors.rule,
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            content()
        }
    }
}

// ============================================================================
// 7. SEGMENTED QUOTA BAR
// ============================================================================

/**
 * Quota bar made of one discrete segment per required day.
 * - Completed: Filled Present (#5BB5A2)
 * - Under minimum: Unfilled segment with 1.5 dp Short outline (distinguishable by shape & color)
 * - Uncompleted: Filled Rule (#2D3138)
 * - Accessibility: takes an explicit contentDescription parameter (e.g. "8 of 10 days")
 */
@Composable
fun WfoQuotaBar(
    totalRequired: Int,
    completed: Int,
    underMinimum: Int = 0,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val total = maxOf(1, totalRequired)
    val metDays = completed.coerceIn(0, total)
    val shortDays = underMinimum.coerceIn(0, maxOf(0, total - metDays))

    val rowSemantics = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .then(rowSemantics),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (index in 0 until total) {
            val isMet = index < metDays
            val isShort = index >= metDays && index < metDays + shortDays

            if (isShort) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .border(1.5.dp, AppTheme.colors.short, CalendarCellShape)
                )
            } else {
                val fillColor = if (isMet) AppTheme.colors.present else AppTheme.colors.rule
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CalendarCellShape)
                        .background(fillColor)
                )
            }
        }
    }
}

// ============================================================================
// 8. HIGH-CONTRAST UNDO BANNER
// ============================================================================

/**
 * Persistent Undo Punch Out Banner meeting >= 4.5:1 text contrast.
 * Raised background (#1E2126), 1dp Rule border (#2D3138), Ink text (#ECEDEF),
 * Present action (#5BB5A2), liveRegion = Polite, and defaultMinSize before padding.
 */
@Composable
fun WfoUndoBanner(
    message: String,
    onUndoClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = SurfaceGroupShape,
        color = AppTheme.colors.raised,
        border = BorderStroke(1.dp, AppTheme.colors.rule),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.ink,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = onUndoClick,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppTheme.colors.present
                    )
                ) {
                    Text(
                        text = "Undo",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTheme.colors.present,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onDismissClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss undo banner",
                        tint = AppTheme.colors.muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
