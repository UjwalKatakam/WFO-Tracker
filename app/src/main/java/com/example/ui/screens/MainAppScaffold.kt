package com.example.ui.screens

import android.Manifest
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.WfoViewModel

enum class NavigationDestination(val label: String, val icon: ImageVector, val testTag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    LOGS("History", Icons.Default.History, "nav_logs"),
    CALENDAR("Calendar", Icons.Default.CalendarMonth, "nav_calendar"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: WfoViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDestinationIndex by remember { mutableIntStateOf(0) }

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

    Scaffold(
        containerColor = AppTheme.colors.ground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val currentDestination = NavigationDestination.entries[selectedDestinationIndex]
                            val titleText = when (currentDestination) {
                                NavigationDestination.DASHBOARD -> "WFO Tracker"
                                NavigationDestination.LOGS -> "History"
                                NavigationDestination.CALENDAR -> "Calendar"
                                NavigationDestination.SETTINGS -> "Settings"
                            }
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleLarge,
                                color = AppTheme.colors.ink
                            )
                            if (currentDestination == NavigationDestination.DASHBOARD && uiState.isPunchedIn) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(AppTheme.colors.present, CircleShape)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.pushToCloud() },
                            enabled = !uiState.isSyncingCloud,
                            modifier = Modifier.testTag("top_cloud_sync_button")
                        ) {
                            if (uiState.isSyncingCloud) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = AppTheme.colors.present
                                )
                            } else {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = "Sync with Gist",
                                    tint = AppTheme.colors.muted
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppTheme.colors.ground,
                        scrolledContainerColor = AppTheme.colors.ground
                    )
                )
                HorizontalDivider(
                    color = AppTheme.colors.rule,
                    thickness = 1.dp
                )
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(
                    color = AppTheme.colors.rule,
                    thickness = 1.dp
                )
                NavigationBar(
                    containerColor = AppTheme.colors.ground,
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
                                                modifier = Modifier.size(6.dp)
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
                                selectedIconColor = AppTheme.colors.ink,
                                unselectedIconColor = AppTheme.colors.muted,
                                selectedTextColor = AppTheme.colors.ink,
                                unselectedTextColor = AppTheme.colors.muted,
                                indicatorColor = AppTheme.colors.raised
                            ),
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            when (NavigationDestination.entries[selectedDestinationIndex]) {
                NavigationDestination.DASHBOARD -> DashboardScreen(uiState, viewModel)
                NavigationDestination.LOGS -> LogHistoryScreen(uiState, viewModel)
                NavigationDestination.CALENDAR -> CalendarArchiveScreen(uiState, viewModel)
                NavigationDestination.SETTINGS -> SettingsScreen(uiState, viewModel)
            }
        }
    }
}
