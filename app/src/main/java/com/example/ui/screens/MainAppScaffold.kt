package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.ui.components.DashboardCalculations
import com.example.ui.components.GreetingTimeOfDay
import com.example.ui.theme.AppTheme
import com.example.ui.theme.NightSceneInk
import com.example.ui.viewmodel.WfoViewModel
import java.util.Calendar

enum class NavigationDestination(val label: String, val icon: ImageVector, val testTag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    LOGS("History", Icons.Default.History, "nav_logs"),
    CALENDAR("Calendar", Icons.Default.CalendarMonth, "nav_calendar"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: WfoViewModel
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDestinationIndex by remember { mutableIntStateOf(0) }
    val dashboardScrollState = rememberScrollState()

    val currentDestination = NavigationDestination.entries[selectedDestinationIndex]
    val isDashboard = currentDestination == NavigationDestination.DASHBOARD
    val isDarkTheme = AppTheme.colors.isDark

    val currentHour = remember(uiState.selectedCalendarDateKey, uiState.logs.size, selectedDestinationIndex) {
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }
    val greetingTimeOfDay = DashboardCalculations.timeOfDay(currentHour)
    val topBarFadeThresholdPx = with(density) { 80.dp.toPx() }
    val topBarBgAlpha = if (!isDashboard) {
        1f
    } else {
        (dashboardScrollState.value / topBarFadeThresholdPx).coerceIn(0f, 1f)
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            val lightStatusIcons = if (isDashboard && topBarBgAlpha < 0.5f) {
                !isDarkTheme && greetingTimeOfDay != GreetingTimeOfDay.EVENING
            } else {
                !isDarkTheme
            }
            controller.isAppearanceLightStatusBars = lightStatusIcons
            controller.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    // Runtime Permission Request for Notifications (Android 13+) and Location
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Display snackbar feedback
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    val isEveningLightScene = !isDarkTheme && greetingTimeOfDay == GreetingTimeOfDay.EVENING
    val sceneTitleColor = when {
        isDarkTheme -> NightSceneInk
        isEveningLightScene -> Color.White
        else -> AppTheme.colors.ink
    }
    val sceneBuildingColor = when {
        isDarkTheme -> AppTheme.colors.presentDeep
        isEveningLightScene -> Color.White
        else -> AppTheme.colors.presentDeep
    }
    val sceneSyncColor = when {
        isDarkTheme -> NightSceneInk
        isEveningLightScene -> Color.White
        else -> AppTheme.colors.ink
    }

    val topTitleColor = if (isDashboard) lerp(sceneTitleColor, AppTheme.colors.ink, topBarBgAlpha) else AppTheme.colors.ink
    val topBuildingColor = if (isDashboard) lerp(sceneBuildingColor, AppTheme.colors.presentDeep, topBarBgAlpha) else AppTheme.colors.presentDeep
    val topSyncColor = if (isDashboard) lerp(sceneSyncColor, AppTheme.colors.muted, topBarBgAlpha) else AppTheme.colors.muted

    Scaffold(
        containerColor = AppTheme.colors.ground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Apartment,
                                contentDescription = null,
                                tint = topBuildingColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val titleText = when (currentDestination) {
                                NavigationDestination.DASHBOARD -> "WFO Tracker"
                                NavigationDestination.LOGS -> "History"
                                NavigationDestination.CALENDAR -> "Calendar"
                                NavigationDestination.SETTINGS -> "Settings"
                            }
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleLarge,
                                color = topTitleColor
                            )
                            if (currentDestination == NavigationDestination.DASHBOARD && uiState.isPunchedIn) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(AppTheme.colors.present, CircleShape)
                                        .semantics { this.contentDescription = "Session active" }
                                )
                            }
                        }
                    },
                    actions = {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text("Back up to Gist")
                                }
                            },
                            state = rememberTooltipState()
                        ) {
                            IconButton(
                                onClick = {
                                    if (uiState.settings.gitToken.isBlank()) {
                                        selectedDestinationIndex = NavigationDestination.SETTINGS.ordinal
                                    } else {
                                        viewModel.pushToCloud()
                                    }
                                },
                                enabled = !uiState.isSyncingCloud,
                                modifier = Modifier.testTag("top_cloud_sync_button")
                            ) {
                                if (uiState.isSyncingCloud) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = topBuildingColor
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = "Back up to Gist",
                                        tint = topSyncColor
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDashboard) AppTheme.colors.ground.copy(alpha = topBarBgAlpha) else AppTheme.colors.ground,
                        scrolledContainerColor = if (isDashboard) AppTheme.colors.ground.copy(alpha = topBarBgAlpha) else AppTheme.colors.ground
                    )
                )
                if (!isDashboard || topBarBgAlpha >= 0.98f) {
                    HorizontalDivider(
                        color = AppTheme.colors.rule,
                        thickness = 1.dp
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = if (!isDarkTheme) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        clip = false,
                        ambientColor = AppTheme.colors.shadowInk.copy(alpha = 0.14f),
                        spotColor = AppTheme.colors.shadowInk.copy(alpha = 0.14f)
                    )
                } else {
                    Modifier
                }
            ) {
                if (isDarkTheme) {
                    HorizontalDivider(
                        color = AppTheme.colors.rule,
                        thickness = 1.dp
                    )
                }
                NavigationBar(
                    containerColor = if (isDarkTheme) AppTheme.colors.ground else AppTheme.colors.sheet,
                    tonalElevation = 0.dp
                ) {
                    NavigationDestination.entries.forEachIndexed { index, dest ->
                        val isSelected = selectedDestinationIndex == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedDestinationIndex = index },
                            icon = {
                                if (dest == NavigationDestination.DASHBOARD && uiState.isPunchedIn) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = AppTheme.colors.present,
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .semantics { this.contentDescription = "Session active" }
                                            )
                                        }
                                    ) {
                                        Icon(dest.icon, contentDescription = dest.label)
                                    }
                                } else {
                                    Icon(dest.icon, contentDescription = dest.label)
                                }
                            },
                            label = {
                                Text(
                                    text = dest.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppTheme.colors.presentDeep,
                                unselectedIconColor = AppTheme.colors.muted,
                                selectedTextColor = AppTheme.colors.presentDeep,
                                unselectedTextColor = AppTheme.colors.muted,
                                indicatorColor = AppTheme.colors.presentContainer
                            ),
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val contentPaddingModifier = if (isDashboard) {
            Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        } else {
            Modifier.padding(innerPadding)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(contentPaddingModifier)
                .imePadding()
        ) {
            when (currentDestination) {
                NavigationDestination.DASHBOARD -> DashboardScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    scrollState = dashboardScrollState,
                    onNavigateToSettings = {
                        selectedDestinationIndex = NavigationDestination.SETTINGS.ordinal
                    }
                )
                NavigationDestination.LOGS -> LogHistoryScreen(uiState, viewModel)
                NavigationDestination.CALENDAR -> CalendarArchiveScreen(uiState, viewModel)
                NavigationDestination.SETTINGS -> SettingsScreen(uiState, viewModel)
            }
        }
    }
}
