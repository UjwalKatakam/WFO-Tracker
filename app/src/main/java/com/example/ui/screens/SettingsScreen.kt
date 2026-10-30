package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.location.Location
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.preferences.WfoSettings
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.GreenSuccess
import com.example.ui.viewmodel.WfoUiState
import com.example.ui.viewmodel.WfoViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun SettingsScreen(
    uiState: WfoUiState,
    viewModel: WfoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var employeeName by remember(uiState.settings.employeeName) { mutableStateOf(uiState.settings.employeeName) }
    var minHoursStr by remember(uiState.settings.minHours) { mutableStateOf(uiState.settings.minHours.toString()) }
    var targetDaysStr by remember(uiState.settings.targetDaysPerWeek) { mutableStateOf(uiState.settings.targetDaysPerWeek.toString()) }
    var shiftStart by remember(uiState.settings.shiftStart) { mutableStateOf(uiState.settings.shiftStart) }
    var notifyOnTarget by remember(uiState.settings.notifyOnTarget) { mutableStateOf(uiState.settings.notifyOnTarget) }

    var latStr by remember(uiState.settings.lat) { mutableStateOf(uiState.settings.lat.toString()) }
    var lonStr by remember(uiState.settings.lon) { mutableStateOf(uiState.settings.lon.toString()) }
    var radiusStr by remember(uiState.settings.radiusKm) { mutableStateOf(uiState.settings.radiusKm.toString()) }
    var gpsVerificationEnabled by remember(uiState.settings.gpsVerificationEnabled) { mutableStateOf(uiState.settings.gpsVerificationEnabled) }
    var passiveGeofenceArrivalEnabled by remember(uiState.settings.passiveGeofenceArrivalEnabled) { mutableStateOf(uiState.settings.passiveGeofenceArrivalEnabled) }
    var departureReminderEnabled by remember(uiState.settings.departureReminderEnabled) { mutableStateOf(uiState.settings.departureReminderEnabled) }
    var departureReminderHoursStr by remember(uiState.settings.departureReminderHours) { mutableStateOf(uiState.settings.departureReminderHours.toString()) }
    var autoMuteHolidays by remember(uiState.settings.autoMuteHolidays) { mutableStateOf(uiState.settings.autoMuteHolidays) }

    var gitToken by remember(uiState.settings.gitToken) { mutableStateOf(uiState.settings.gitToken) }
    var gistId by remember(uiState.settings.gistId) { mutableStateOf(uiState.settings.gistId) }

    var showClearDataDialog by remember { mutableStateOf(false) }
    var isFetchingGps by remember { mutableStateOf(false) }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CrimsonDanger) },
            title = { Text("Danger Zone: Clear All Data") },
            text = { Text("This will permanently wipe all local attendance records, leaves, and settings. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
                ) {
                    Text("Permanently Clear Everything")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearDataDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "⚙️ WORKPLACE & COMPLIANCE SETTINGS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "Configure work policies, notifications, GPS geofencing, and cloud sync.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )

        // 1. Corporate Policy Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Corporate WFO Policy",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = employeeName,
                    onValueChange = { employeeName = it },
                    label = { Text("Employee / User Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = minHoursStr,
                        onValueChange = { minHoursStr = it },
                        label = { Text("Min Hours / Day") },
                        placeholder = { Text("8.0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = targetDaysStr,
                        onValueChange = { targetDaysStr = it },
                        label = { Text("Target Days / Week") },
                        placeholder = { Text("3") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = shiftStart,
                    onValueChange = { shiftStart = it },
                    label = { Text("Expected Shift Start (HH:MM)") },
                    placeholder = { Text("12:00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Quota Target Notification",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Trigger audible ringtone & alert when min hours reached",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifyOnTarget,
                        onCheckedChange = { notifyOnTarget = it }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Departure Reminder: Did you forget to punch out?
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Departure Reminder",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Alerts if timer runs past departure threshold ('Did you forget to punch out?')",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = departureReminderEnabled,
                        onCheckedChange = { departureReminderEnabled = it }
                    )
                }

                if (departureReminderEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = departureReminderHoursStr,
                        onValueChange = { departureReminderHoursStr = it },
                        label = { Text("Departure Alert Threshold (Hours Active)") },
                        placeholder = { Text("9.0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Office Geofence Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Office GPS Geofencing",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = {
                            isFetchingGps = true
                            try {
                                val client = LocationServices.getFusedLocationProviderClient(context)
                                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                    .addOnSuccessListener { loc: Location? ->
                                        isFetchingGps = false
                                        if (loc != null) {
                                            latStr = String.format(java.util.Locale.US, "%.5f", loc.latitude)
                                            lonStr = String.format(java.util.Locale.US, "%.5f", loc.longitude)
                                        }
                                    }
                                    .addOnFailureListener { isFetchingGps = false }
                            } catch (e: SecurityException) {
                                isFetchingGps = false
                            }
                        }
                    ) {
                        if (isFetchingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Current GPS", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = latStr,
                        onValueChange = { latStr = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = lonStr,
                        onValueChange = { lonStr = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = radiusStr,
                    onValueChange = { radiusStr = it },
                    label = { Text("Allowed Office Radius (km)") },
                    placeholder = { Text("0.5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Verify Location on Punch-In",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Warn if device is outside allowed office perimeter",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = gpsVerificationEnabled,
                        onCheckedChange = { gpsVerificationEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Passive Geofence Arrival Notifications",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Alerts on office arrival. Never auto-adds transit offsets; always prompts you first.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = passiveGeofenceArrivalEnabled,
                        onCheckedChange = { passiveGeofenceArrivalEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Holiday & Weekend Auto-Mute
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Holiday & Weekend Auto-Mute",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Suppresses arrival geofence alerts on weekends and approved holidays",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoMuteHolidays,
                        onCheckedChange = { autoMuteHolidays = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. GitHub Gist Cloud Sync Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "GitHub Gist Cloud Sync",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Seamless two-way cloud synchronization between devices via a private GitHub Gist.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                OutlinedTextField(
                    value = gitToken,
                    onValueChange = { gitToken = it },
                    label = { Text("Personal Access Token (PAT)") },
                    placeholder = { Text("ghp_xxxxxxxxxxxx") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = gistId,
                    onValueChange = { gistId = it },
                    label = { Text("Gist ID (leave empty to auto-create on first push)") },
                    placeholder = { Text("e.g. 7f8a9b2c3d...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.pushToCloud() },
                        enabled = !uiState.isSyncingCloud,
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isSyncingCloud) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Push to Cloud", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.pullFromCloud() },
                        enabled = !uiState.isSyncingCloud,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pull & Merge", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Monthly PDF Audit Report Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📄 Monthly Compliance Audit Report",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generate a formal A4 attendance compliance report PDF with verification badge, statistics table, check-in history, and signature lines for HR or management.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Button(
                    onClick = {
                        val cal = Calendar.getInstance()
                        val pdfFile = viewModel.generatePdfReport(
                            context = context,
                            year = uiState.dashboardNavYear,
                            month = uiState.dashboardNavMonth
                        )
                        sharePdfFile(context, pdfFile)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate & Share Monthly Audit PDF", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Settings Action Button
        Button(
            onClick = {
                val newSettings = WfoSettings(
                    employeeName = employeeName.trim(),
                    minHours = minHoursStr.toDoubleOrNull() ?: 8.0,
                    targetDaysPerWeek = targetDaysStr.toIntOrNull() ?: 3,
                    shiftStart = shiftStart.trim(),
                    notifyOnTarget = notifyOnTarget,
                    lat = latStr.toDoubleOrNull() ?: 12.9716,
                    lon = lonStr.toDoubleOrNull() ?: 77.5946,
                    radiusKm = radiusStr.toDoubleOrNull() ?: 0.5,
                    gpsVerificationEnabled = gpsVerificationEnabled,
                    passiveGeofenceArrivalEnabled = passiveGeofenceArrivalEnabled,
                    departureReminderEnabled = departureReminderEnabled,
                    departureReminderHours = departureReminderHoursStr.toDoubleOrNull() ?: 9.0,
                    autoMuteHolidays = autoMuteHolidays,
                    gitToken = gitToken.trim(),
                    gistId = gistId.trim()
                )
                viewModel.updateSettings(newSettings)
            },
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_settings_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("SAVE CONFIGURATION", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Danger Zone
        OutlinedButton(
            onClick = { showClearDataDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDanger),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear All Local Data (Danger Zone)")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun sharePdfFile(context: Context, file: java.io.File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.wfotracker.uqznv.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "WFO Monthly Compliance Audit Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Compliance Audit PDF"))
    } catch (e: Exception) {
        // Handled
    }
}
