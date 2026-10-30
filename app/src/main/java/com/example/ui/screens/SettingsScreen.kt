package com.example.ui.screens

import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ReportMonthPickerSheet
import com.example.ui.components.WfoBottomSheet
import com.example.ui.components.WfoListRow
import com.example.ui.components.WfoPrimaryButton
import com.example.ui.components.WfoSectionHeader
import com.example.ui.components.WfoSwitchRow
import com.example.ui.components.WfoTextButton
import com.example.ui.components.WfoTonalButton
import com.example.ui.components.sharePdfReport
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.WfoUiState
import com.example.ui.viewmodel.WfoViewModel
import com.example.util.DateUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: WfoUiState,
    viewModel: WfoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Sub-sheet states
    var showEditPolicySheet by remember { mutableStateOf(false) }
    var showEditLocationSheet by remember { mutableStateOf(false) }
    var showEditTokenSheet by remember { mutableStateOf(false) }
    var showEditGistIdSheet by remember { mutableStateOf(false) }
    var showReportMonthPicker by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    // SAF Document Launchers for JSON File Backup & Restore
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonStr = viewModel.exportJsonString()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonStr.toByteArray(Charsets.UTF_8))
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    if (stream != null) {
                        val reader = BufferedReader(InputStreamReader(stream))
                        val content = reader.readText()
                        reader.close()
                        viewModel.importJsonString(content)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Danger Zone Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = "Clear local data?",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.ink,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all local check-in records and leaves from this device.\n\nYour remote backup on GitHub Gist (if configured) will not be deleted. You can export a local backup file first if you wish.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.muted
                )
            },
            confirmButton = {
                WfoTextButton(
                    text = "Clear data",
                    isDanger = true,
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    }
                )
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WfoTextButton(
                        text = "Export backup first",
                        onClick = {
                            showClearDataDialog = false
                            exportJsonLauncher.launch("WFO_Backup_${DateUtils.todayDateStr()}.json")
                        }
                    )
                    WfoTextButton(
                        text = "Cancel",
                        onClick = { showClearDataDialog = false }
                    )
                }
            }
        )
    }

    // Sub-sheet: Attendance Policy Editor
    if (showEditPolicySheet) {
        EditPolicySheet(
            settings = uiState.settings,
            onDismissRequest = { showEditPolicySheet = false },
            onSave = { updatedSettings ->
                viewModel.updateSettings(updatedSettings)
                showEditPolicySheet = false
            }
        )
    }

    // Sub-sheet: Office Location Editor
    if (showEditLocationSheet) {
        EditLocationSheet(
            settings = uiState.settings,
            onDismissRequest = { showEditLocationSheet = false },
            onSave = { updatedSettings ->
                viewModel.updateSettings(updatedSettings)
                showEditLocationSheet = false
            }
        )
    }

    // Sub-sheet: GitHub Token Editor
    if (showEditTokenSheet) {
        EditTokenSheet(
            currentToken = uiState.settings.gitToken,
            onDismissRequest = { showEditTokenSheet = false },
            onSave = { token ->
                viewModel.updateSettings(uiState.settings.copy(gitToken = token))
                showEditTokenSheet = false
            }
        )
    }

    // Sub-sheet: Gist ID Editor
    if (showEditGistIdSheet) {
        EditGistIdSheet(
            currentGistId = uiState.settings.gistId,
            onDismissRequest = { showEditGistIdSheet = false },
            onSave = { gistId ->
                viewModel.updateSettings(uiState.settings.copy(gistId = gistId))
                showEditGistIdSheet = false
            }
        )
    }

    // Sub-sheet: Monthly Audit Report Picker
    if (showReportMonthPicker) {
        ReportMonthPickerSheet(
            initialYear = uiState.dashboardNavYear,
            initialMonth = uiState.dashboardNavMonth,
            onDismissRequest = { showReportMonthPicker = false },
            onConfirm = { year, month ->
                val pdfFile = viewModel.generatePdfReport(context, year, month)
                sharePdfReport(context, pdfFile)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.ground)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp)
    ) {
        // --------------------------------------------------------------------
        // 1. Attendance Policy Section
        // --------------------------------------------------------------------
        WfoSectionHeader(title = "Attendance policy")

        WfoListRow(
            title = "Name",
            subtitle = uiState.settings.employeeName.ifBlank { "Not configured" },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = { showEditPolicySheet = true }
        )

        WfoListRow(
            title = "Daily target",
            subtitle = "${String.format(Locale.US, "%.1f", uiState.settings.minHours)} hours",
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = { showEditPolicySheet = true }
        )

        WfoListRow(
            title = "Weekly target",
            subtitle = "${uiState.settings.targetDaysPerWeek} days a week",
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = { showEditPolicySheet = true }
        )

        WfoListRow(
            title = "Expected shift start",
            subtitle = uiState.settings.shiftStart,
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = { showEditPolicySheet = true }
        )

        WfoSwitchRow(
            title = "Target reached alert",
            subtitle = "Audible alert when daily target is met",
            checked = uiState.settings.notifyOnTarget,
            onCheckedChange = { checked ->
                viewModel.updateSettings(uiState.settings.copy(notifyOnTarget = checked))
            }
        )

        WfoSwitchRow(
            title = "Departure reminder",
            subtitle = "Alert if timer runs past departure threshold",
            checked = uiState.settings.departureReminderEnabled,
            onCheckedChange = { checked ->
                viewModel.updateSettings(uiState.settings.copy(departureReminderEnabled = checked))
            }
        )

        if (uiState.settings.departureReminderEnabled) {
            WfoListRow(
                title = "Departure alert threshold",
                subtitle = "${String.format(Locale.US, "%.1f", uiState.settings.departureReminderHours)} hours active",
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = AppTheme.colors.muted,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = { showEditPolicySheet = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --------------------------------------------------------------------
        // 2. Office Location Section
        // --------------------------------------------------------------------
        WfoSectionHeader(title = "Office location")

        WfoSwitchRow(
            title = "Verify location on punch in",
            subtitle = "Warn if device is outside allowed office perimeter",
            checked = uiState.settings.gpsVerificationEnabled,
            onCheckedChange = { checked ->
                viewModel.updateSettings(uiState.settings.copy(gpsVerificationEnabled = checked))
            }
        )

        WfoSwitchRow(
            title = "Arrival notifications",
            subtitle = "Prompt to punch in when arriving at the office",
            checked = uiState.settings.passiveGeofenceArrivalEnabled,
            onCheckedChange = { checked ->
                viewModel.updateSettings(uiState.settings.copy(passiveGeofenceArrivalEnabled = checked))
            }
        )

        WfoSwitchRow(
            title = "Mute on holidays and weekends",
            subtitle = "Suppress arrival alerts on non-working days",
            checked = uiState.settings.autoMuteHolidays,
            onCheckedChange = { checked ->
                viewModel.updateSettings(uiState.settings.copy(autoMuteHolidays = checked))
            }
        )

        WfoListRow(
            title = "Office coordinates",
            subtitle = String.format(Locale.US, "%.5f, %.5f", uiState.settings.lat, uiState.settings.lon),
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = { showEditLocationSheet = true }
        )

        WfoListRow(
            title = "Allowed radius",
            subtitle = "${String.format(Locale.US, "%.2f", uiState.settings.radiusKm)} km",
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = { showEditLocationSheet = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --------------------------------------------------------------------
        // 3. Cloud Sync Section
        // --------------------------------------------------------------------
        WfoSectionHeader(title = "Cloud sync")

        WfoListRow(
            title = "Personal access token",
            subtitle = if (uiState.settings.gitToken.isNotBlank()) "Configured (tap to change)" else "Not configured",
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = { showEditTokenSheet = true }
        )

        WfoListRow(
            title = "Gist ID",
            subtitle = if (uiState.settings.gistId.isNotBlank()) uiState.settings.gistId else "Auto-created on first sync",
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = { showEditGistIdSheet = true }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WfoTonalButton(
                text = if (uiState.isSyncingCloud) "Syncing..." else "Push to cloud",
                icon = if (!uiState.isSyncingCloud) Icons.Default.CloudUpload else null,
                onClick = { viewModel.pushToCloud() },
                enabled = !uiState.isSyncingCloud,
                modifier = Modifier.weight(1f)
            )

            WfoTonalButton(
                text = "Pull and merge",
                icon = if (!uiState.isSyncingCloud) Icons.Default.CloudDownload else null,
                onClick = { viewModel.pullFromCloud() },
                enabled = !uiState.isSyncingCloud,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --------------------------------------------------------------------
        // 4. Audit Report Section
        // --------------------------------------------------------------------
        WfoSectionHeader(title = "Audit report")

        WfoListRow(
            title = "Monthly compliance report",
            subtitle = "Generate attendance report PDF for HR or records",
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = { showReportMonthPicker = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --------------------------------------------------------------------
        // 5. Data & Backup Section
        // --------------------------------------------------------------------
        WfoSectionHeader(title = "Data & backup")

        WfoListRow(
            title = "Export JSON backup",
            subtitle = "Save attendance records and leaves as a file",
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(18.dp)
                )
            },
            onClick = {
                exportJsonLauncher.launch("WFO_Backup_${DateUtils.todayDateStr()}.json")
            }
        )

        WfoListRow(
            title = "Import JSON backup",
            subtitle = "Restore or merge records from a JSON file",
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(18.dp)
                )
            },
            onClick = {
                importJsonLauncher.launch(arrayOf("application/json", "*/*"))
            }
        )

        WfoListRow(
            title = "Clear local data",
            subtitle = "Delete local attendance records and leave data",
            isDanger = true,
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = AppTheme.colors.danger,
                    modifier = Modifier.size(18.dp)
                )
            },
            onClick = { showClearDataDialog = true }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ============================================================================
// Sub-sheet Composables
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPolicySheet(
    settings: com.example.data.preferences.WfoSettings,
    onDismissRequest: () -> Unit,
    onSave: (com.example.data.preferences.WfoSettings) -> Unit
) {
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf(settings.employeeName) }
    var minHoursStr by remember { mutableStateOf(settings.minHours.toString()) }
    var targetDaysStr by remember { mutableStateOf(settings.targetDaysPerWeek.toString()) }
    var shiftStart by remember { mutableStateOf(settings.shiftStart) }
    var departureHoursStr by remember { mutableStateOf(settings.departureReminderHours.toString()) }

    WfoBottomSheet(
        title = "Attendance policy",
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = minHoursStr,
                    onValueChange = { minHoursStr = it },
                    label = { Text("Daily target (hours)", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("8.0") },
                    singleLine = true,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = targetDaysStr,
                    onValueChange = { targetDaysStr = it },
                    label = { Text("Weekly target (days)", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("3") },
                    singleLine = true,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = shiftStart,
                onValueChange = { shiftStart = it },
                label = { Text("Expected shift start", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("12:00") },
                singleLine = true,
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = if (settings.departureReminderEnabled) ImeAction.Next else ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            if (settings.departureReminderEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = departureHoursStr,
                    onValueChange = { departureHoursStr = it },
                    label = { Text("Departure alert threshold (hours active)", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("9.0") },
                    singleLine = true,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            WfoPrimaryButton(
                text = "Save policy",
                onClick = {
                    val updated = settings.copy(
                        employeeName = name.trim(),
                        minHours = minHoursStr.toDoubleOrNull() ?: settings.minHours,
                        targetDaysPerWeek = targetDaysStr.toIntOrNull() ?: settings.targetDaysPerWeek,
                        shiftStart = shiftStart.trim(),
                        departureReminderHours = departureHoursStr.toDoubleOrNull() ?: settings.departureReminderHours
                    )
                    onSave(updated)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditLocationSheet(
    settings: com.example.data.preferences.WfoSettings,
    onDismissRequest: () -> Unit,
    onSave: (com.example.data.preferences.WfoSettings) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var latStr by remember { mutableStateOf(String.format(Locale.US, "%.5f", settings.lat)) }
    var lonStr by remember { mutableStateOf(String.format(Locale.US, "%.5f", settings.lon)) }
    var radiusStr by remember { mutableStateOf(settings.radiusKm.toString()) }
    var isFetchingGps by remember { mutableStateOf(false) }

    WfoBottomSheet(
        title = "Office location",
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            WfoTonalButton(
                text = if (isFetchingGps) "Fetching coordinates..." else "Set to current location",
                icon = if (!isFetchingGps) Icons.Default.MyLocation else null,
                onClick = {
                    isFetchingGps = true
                    try {
                        val client = LocationServices.getFusedLocationProviderClient(context)
                        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { loc: Location? ->
                                isFetchingGps = false
                                if (loc != null) {
                                    latStr = String.format(Locale.US, "%.5f", loc.latitude)
                                    lonStr = String.format(Locale.US, "%.5f", loc.longitude)
                                }
                            }
                            .addOnFailureListener { isFetchingGps = false }
                    } catch (_: SecurityException) {
                        isFetchingGps = false
                    }
                },
                enabled = !isFetchingGps,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = latStr,
                    onValueChange = { latStr = it },
                    label = { Text("Latitude", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = lonStr,
                    onValueChange = { lonStr = it },
                    label = { Text("Longitude", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = radiusStr,
                onValueChange = { radiusStr = it },
                label = { Text("Allowed radius (km)", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("0.5") },
                singleLine = true,
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            WfoPrimaryButton(
                text = "Save location",
                onClick = {
                    val updated = settings.copy(
                        lat = latStr.toDoubleOrNull() ?: settings.lat,
                        lon = lonStr.toDoubleOrNull() ?: settings.lon,
                        radiusKm = radiusStr.toDoubleOrNull() ?: settings.radiusKm
                    )
                    onSave(updated)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTokenSheet(
    currentToken: String,
    onDismissRequest: () -> Unit,
    onSave: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var token by remember { mutableStateOf(currentToken) }
    var showToken by remember { mutableStateOf(false) }

    WfoBottomSheet(
        title = "Personal access token",
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Enter a GitHub token with the 'gist' scope to sync your attendance across devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.muted
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("GitHub token", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("ghp_xxxxxxxxxxxx") },
                singleLine = true,
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { showToken = !showToken },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showToken) "Hide token" else "Show token",
                            tint = AppTheme.colors.muted
                        )
                    }
                },
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrect = false,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            WfoPrimaryButton(
                text = "Save token",
                onClick = {
                    onSave(token.trim())
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditGistIdSheet(
    currentGistId: String,
    onDismissRequest: () -> Unit,
    onSave: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var gistId by remember { mutableStateOf(currentGistId) }

    WfoBottomSheet(
        title = "Gist ID",
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Leave empty to automatically create a private Gist on your first push, or paste an existing Gist ID.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.muted
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = gistId,
                onValueChange = { gistId = it },
                label = { Text("Gist ID", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("e.g. 7f8a9b2c3d...") },
                singleLine = true,
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            WfoPrimaryButton(
                text = "Save Gist ID",
                onClick = {
                    onSave(gistId.trim())
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AppTheme.colors.raised,
    unfocusedContainerColor = AppTheme.colors.raised,
    focusedBorderColor = AppTheme.colors.present,
    unfocusedBorderColor = AppTheme.colors.rule,
    focusedTextColor = AppTheme.colors.ink,
    unfocusedTextColor = AppTheme.colors.ink,
    focusedLabelColor = AppTheme.colors.present,
    unfocusedLabelColor = AppTheme.colors.muted
)
