/*
 * SPDX-FileCopyrightText: 2023-2026 Andrew Gunnerson
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.chiller3.mirrormobile.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.txurtxil.lmonitor.BuildConfig
import com.chiller3.mirrormobile.Logcat
import com.chiller3.mirrormobile.Permissions
import com.chiller3.mirrormobile.Preferences
import com.chiller3.mirrormobile.SessionLog
import com.txurtxil.lmonitor.R
import com.chiller3.mirrormobile.extension.canWriteSystemSettings
import com.chiller3.mirrormobile.extension.forceLandscapeRotation
import com.chiller3.mirrormobile.extension.formattedString
import com.chiller3.mirrormobile.extension.restoreAutoRotation
import com.chiller3.mirrormobile.launcher.LauncherActivity
import com.chiller3.mirrormobile.launcher.LauncherConfigActivity
import com.chiller3.mirrormobile.extension.ShizukuStatus
import com.chiller3.mirrormobile.ui.AppScreen
import com.chiller3.mirrormobile.ui.BetterSegmentedShapes
import com.chiller3.mirrormobile.ui.Preference
import com.chiller3.mirrormobile.ui.PreferenceCategory
import com.chiller3.mirrormobile.ui.PreferenceColumn
import com.chiller3.mirrormobile.ui.SwitchPreference
import com.chiller3.mirrormobile.ui.betterSegmentedShapes
import com.chiller3.mirrormobile.ui.theme.AppTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val resources = LocalResources.current

    val prefs = remember { Preferences(context) }
    var reloadPrefs by remember { mutableIntStateOf(0) }
    val autoStart = remember(reloadPrefs) { prefs.autoStart }
    val wakeLock = remember(reloadPrefs) { prefs.wakeLock }
    val stopOnDisconnect = remember(reloadPrefs) { prefs.stopOnDisconnect }
    val forceHorizontal = remember(reloadPrefs) { prefs.forceHorizontal }
    val speedThreshold = remember(reloadPrefs) { prefs.speedThreshold }
    val widthOffset = remember(reloadPrefs) { prefs.widthOffset }
    val heightOffset = remember(reloadPrefs) { prefs.heightOffset }
    val lastSurfaceInfo = remember(reloadPrefs) { prefs.lastSurfaceInfo }
    val isDebugMode = remember(reloadPrefs) { prefs.isDebugMode }

    var reloadPerms by remember { mutableIntStateOf(0) }
    val notificationsGranted = remember(reloadPerms) {
        Permissions.have(context, Permissions.NOTIFICATIONS)
    }
    val speedGranted = remember(reloadPerms) { Permissions.have(context, Permissions.SPEED) }

    val requestPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.all { it.value }) {
            reloadPrefs++
        } else {
            context.startActivity(Permissions.getAppInfoIntent(context))
        }
    }
    val requestSafSaveLogs = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(Logcat.MIMETYPE),
    ) { uri ->
        uri?.let { viewModel.saveLogs(it) }
    }
    val requestSafSaveSessionLog = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(SessionLog.MIMETYPE),
    ) { uri ->
        uri?.let { viewModel.saveSessionLog(it) }
    }
    val requestWriteSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (context.canWriteSystemSettings()) {
            context.forceLandscapeRotation()
            prefs.forceHorizontal = true
            reloadPrefs++
        }
    }

    AppScreen(
        title = { Text(text = stringResource(R.string.app_name)) },
    ) { params ->
        LaunchedEffect(Unit) {
            viewModel.alerts.collect { alerts ->
                val alert = alerts.firstOrNull() ?: return@collect
                val msg = when (alert) {
                    is SettingsAlert.LogcatSucceeded -> resources.getString(
                        R.string.alert_logcat_success,
                        alert.uri.formattedString,
                    )
                    is SettingsAlert.LogcatFailed -> resources.getString(
                        R.string.alert_logcat_failure,
                        alert.uri.formattedString,
                        alert.error,
                    )
                    is SettingsAlert.SessionLogSucceeded -> resources.getString(
                        R.string.alert_session_log_success,
                        alert.uri.formattedString,
                    )
                    is SettingsAlert.SessionLogFailed -> resources.getString(
                        R.string.alert_session_log_failure,
                        alert.uri.formattedString,
                        alert.error,
                    )
                    SettingsAlert.BrowserNotFound ->
                        resources.getString(R.string.alert_browser_not_found)
                    SettingsAlert.DocumentsUINotFound ->
                        resources.getString(R.string.alert_documentsui_not_found)
                }

                params.snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
                viewModel.acknowledgeFirstAlert()
            }
        }

        SettingsContent(
            notificationsGranted = notificationsGranted,
            speedGranted = speedGranted,
            autoStart = autoStart,
            wakeLock = wakeLock,
            stopOnDisconnect = stopOnDisconnect,
            forceHorizontal = forceHorizontal,
            speedThreshold = speedThreshold,
            widthOffset = widthOffset,
            heightOffset = heightOffset,
            lastSurfaceInfo = lastSurfaceInfo,
            isDebugMode = isDebugMode,
            onNotificationsGrant = {
                requestPermissions.launch(Permissions.NOTIFICATIONS)
            },
            onSpeedGrant = {
                requestPermissions.launch(Permissions.SPEED)
            },
            onAutoStartChange = { enabled ->
                prefs.autoStart = enabled
                reloadPrefs++
            },
            onWakeLockChange = { enabled ->
                prefs.wakeLock = enabled
                reloadPrefs++
            },
            onStopOnDisconnectChange = { enabled ->
                prefs.stopOnDisconnect = enabled
                reloadPrefs++
            },
            onForceHorizontalChange = { enabled ->
                if (enabled) {
                    if (context.canWriteSystemSettings()) {
                        context.forceLandscapeRotation()
                        prefs.forceHorizontal = true
                        reloadPrefs++
                    } else {
                        requestWriteSettings.launch(
                            Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                "package:${context.packageName}".toUri(),
                            )
                        )
                    }
                } else {
                    context.restoreAutoRotation()
                    prefs.forceHorizontal = false
                    reloadPrefs++
                }
            },
            onSpeedThresholdCycle = {
                val presets = Preferences.SPEED_THRESHOLD_PRESETS
                val idx = presets.indexOfFirst { it == prefs.speedThreshold }
                    .let { if (it == -1) 0 else it }
                prefs.speedThreshold = presets[(idx + 1) % presets.size]
                reloadPrefs++
            },
            onWidthOffsetCycle = {
                val presets = Preferences.PIXEL_OFFSET_PRESETS
                val idx = presets.indexOfFirst { it == prefs.widthOffset }
                    .let { if (it == -1) 0 else it }
                prefs.widthOffset = presets[(idx + 1) % presets.size]
                reloadPrefs++
            },
            onHeightOffsetCycle = {
                val presets = Preferences.PIXEL_OFFSET_PRESETS
                val idx = presets.indexOfFirst { it == prefs.heightOffset }
                    .let { if (it == -1) 0 else it }
                prefs.heightOffset = presets[(idx + 1) % presets.size]
                reloadPrefs++
            },
            onDebugModeChange = { enabled ->
                prefs.isDebugMode = enabled
                reloadPrefs++
            },
            onSourceRepoOpen = {
                val uri = BuildConfig.PROJECT_URL_AT_COMMIT.toUri()
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: ActivityNotFoundException) {
                    viewModel.addAlert(SettingsAlert.BrowserNotFound)
                }
            },
            onSupportOpen = {
                val uri = "https://ko-fi.com/txurtxil".toUri()
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: ActivityNotFoundException) {
                    viewModel.addAlert(SettingsAlert.BrowserNotFound)
                }
            },
            onSaveLogs = {
                requestSafSaveLogs.launch(Logcat.FILENAME_DEFAULT)
            },
            onSaveSessionLog = {
                requestSafSaveSessionLog.launch(SessionLog.FILENAME_DEFAULT)
            },
            contentPadding = params.contentPadding,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsContent(
    notificationsGranted: Boolean,
    speedGranted: Boolean,
    autoStart: Boolean,
    wakeLock: Boolean,
    stopOnDisconnect: Boolean,
    forceHorizontal: Boolean,
    speedThreshold: Float,
    widthOffset: Int,
    heightOffset: Int,
    lastSurfaceInfo: String,
    isDebugMode: Boolean,
    onNotificationsGrant: () -> Unit,
    onSpeedGrant: () -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onWakeLockChange: (Boolean) -> Unit,
    onStopOnDisconnectChange: (Boolean) -> Unit,
    onForceHorizontalChange: (Boolean) -> Unit,
    onSpeedThresholdCycle: () -> Unit,
    onWidthOffsetCycle: () -> Unit,
    onHeightOffsetCycle: () -> Unit,
    onDebugModeChange: (Boolean) -> Unit,
    onSourceRepoOpen: () -> Unit,
    onSupportOpen: () -> Unit,
    onSaveLogs: () -> Unit,
    onSaveSessionLog: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    data class MissingPermission(
        val key: String,
        val title: String,
        val summary: String,
        val onGrant: () -> Unit,
    )

    val missingPermissions = mutableListOf<MissingPermission>().apply {
        if (!notificationsGranted) {
            add(MissingPermission(
                key = "missing_notifications",
                title = stringResource(R.string.pref_missing_notifications_name),
                summary = stringResource(R.string.pref_missing_notifications_desc),
                onGrant = onNotificationsGrant,
            ))
        }
        if (!speedGranted) {
            add(MissingPermission(
                key = "missing_speed",
                title = stringResource(R.string.pref_missing_speed_name),
                summary = stringResource(R.string.pref_missing_speed_desc),
                onGrant = onSpeedGrant,
            ))
        }
    }

    PreferenceColumn(contentPadding = contentPadding) {
        if (missingPermissions.isNotEmpty()) {
            item(key = "permissions") {
                PreferenceCategory(
                    title = { Text(text = stringResource(R.string.pref_header_permissions)) },
                    modifier = Modifier.animateItem(),
                )
            }

            itemsIndexed(missingPermissions, key = { _, m -> m.key }) { index, missing ->
                Preference(
                    onClick = missing.onGrant,
                    shapes = betterSegmentedShapes(index, missingPermissions.size),
                    title = { Text(text = missing.title) },
                    summary = { Text(text = missing.summary) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        item(key = "behavior") {
            PreferenceCategory(
                title = { Text(text = stringResource(R.string.pref_header_behavior)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "auto_start") {
            SwitchPreference(
                checked = autoStart,
                onCheckedChange = onAutoStartChange,
                shapes = BetterSegmentedShapes.top(),
                title = { Text(text = stringResource(R.string.pref_auto_start_name)) },
                summary = { Text(text = stringResource(R.string.pref_auto_start_desc)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "wake_lock") {
            SwitchPreference(
                checked = wakeLock,
                onCheckedChange = onWakeLockChange,
                shapes = BetterSegmentedShapes.middle(),
                title = { Text(text = stringResource(R.string.pref_wake_lock_name)) },
                summary = { Text(text = stringResource(R.string.pref_wake_lock_desc)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "force_horizontal") {
            SwitchPreference(
                checked = forceHorizontal,
                onCheckedChange = onForceHorizontalChange,
                shapes = BetterSegmentedShapes.middle(),
                title = { Text(text = stringResource(R.string.pref_force_horizontal_name)) },
                summary = { Text(text = stringResource(R.string.pref_force_horizontal_desc)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "stop_on_disconnect") {
            SwitchPreference(
                checked = stopOnDisconnect,
                onCheckedChange = onStopOnDisconnectChange,
                shapes = BetterSegmentedShapes.middle(),
                title = { Text(text = stringResource(R.string.pref_stop_on_disconnect_name)) },
                summary = { Text(text = stringResource(R.string.pref_stop_on_disconnect_desc)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "speed_threshold") {
            Preference(
                onClick = onSpeedThresholdCycle,
                shapes = BetterSegmentedShapes.middle(),
                title = { Text(text = stringResource(R.string.pref_speed_threshold_name)) },
                summary = {
                    Text(
                        text = stringResource(
                            R.string.pref_speed_threshold_desc,
                            speedThreshold,
                            speedThreshold * 3.6f,
                        )
                    )
                },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "width_offset") {
            Preference(
                onClick = onWidthOffsetCycle,
                shapes = BetterSegmentedShapes.middle(),
                title = { Text(text = stringResource(R.string.pref_width_offset_name)) },
                summary = {
                    Text(
                        text = stringResource(
                            R.string.pref_width_offset_desc,
                            widthOffset,
                        )
                    )
                },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "height_offset") {
            Preference(
                onClick = onHeightOffsetCycle,
                shapes = BetterSegmentedShapes.bottom(),
                title = { Text(text = stringResource(R.string.pref_height_offset_name)) },
                summary = {
                    Text(
                        text = stringResource(
                            R.string.pref_height_offset_desc,
                            heightOffset,
                        )
                    )
                },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "header_launcher") {
            PreferenceCategory(
                title = { Text(text = stringResource(R.string.pref_header_launcher)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "open_launcher") {
            val context = LocalContext.current
            Preference(
                onClick = { context.startActivity(Intent(context, LauncherActivity::class.java)) },
                shapes = BetterSegmentedShapes.top(),
                title = { Text(text = stringResource(R.string.pref_open_launcher_name)) },
                summary = { Text(text = stringResource(R.string.pref_open_launcher_desc)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "choose_launcher_apps") {
            val context = LocalContext.current
            Preference(
                onClick = { context.startActivity(Intent(context, LauncherConfigActivity::class.java)) },
                shapes = BetterSegmentedShapes.bottom(),
                title = { Text(text = stringResource(R.string.pref_choose_launcher_apps_name)) },
                summary = { Text(text = stringResource(R.string.pref_choose_launcher_apps_desc)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "about") {
            PreferenceCategory(
                title = { Text(text = stringResource(R.string.pref_header_about)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "shizuku_status") {
            val shizukuAvailable by ShizukuStatus.isAvailable.collectAsState()
            Preference(
                onClick = {},
                shapes = BetterSegmentedShapes.top(),
                title = { Text(text = stringResource(R.string.pref_shizuku_status_name)) },
                summary = {
                    Text(
                        text = if (shizukuAvailable) {
                            stringResource(R.string.pref_shizuku_status_available)
                        } else {
                            stringResource(R.string.pref_shizuku_status_unavailable)
                        }
                    )
                },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "last_surface_info") {
            Preference(
                onClick = {},
                shapes = BetterSegmentedShapes.middle(),
                title = { Text(text = stringResource(R.string.pref_last_surface_info_name)) },
                summary = {
                    Text(
                        text = lastSurfaceInfo.ifBlank {
                            stringResource(R.string.pref_last_surface_info_unknown)
                        }
                    )
                },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "version") {
            Preference(
                onClick = onSourceRepoOpen,
                onLongClick = { onDebugModeChange(!isDebugMode) },
                shapes = BetterSegmentedShapes.middle(),
                title = { Text(text = stringResource(R.string.pref_version_name)) },
                summary = { Text(text = versionSummary(isDebugMode)) },
                modifier = Modifier.animateItem(),
            )
        }

        item(key = "support") {
            Preference(
                onClick = onSupportOpen,
                shapes = BetterSegmentedShapes.bottom(),
                title = { Text(text = stringResource(R.string.pref_support_name)) },
                summary = { Text(text = stringResource(R.string.pref_support_desc)) },
                modifier = Modifier.animateItem(),
            )
        }

        if (isDebugMode) {
            item(key = "debug") {
                PreferenceCategory(
                    title = { Text(text = stringResource(R.string.pref_header_debug)) },
                    modifier = Modifier.animateItem(),
                )
            }

            item(key = "save_logs") {
                Preference(
                    onClick = onSaveLogs,
                    shapes = BetterSegmentedShapes.top(),
                    title = { Text(text = stringResource(R.string.pref_save_logs_name)) },
                    summary = { Text(text = stringResource(R.string.pref_save_logs_desc)) },
                    modifier = Modifier.animateItem(),
                )
            }

            item(key = "save_session_log") {
                Preference(
                    onClick = onSaveSessionLog,
                    shapes = BetterSegmentedShapes.bottom(),
                    title = { Text(text = stringResource(R.string.pref_save_session_log_name)) },
                    summary = { Text(text = stringResource(R.string.pref_save_session_log_desc)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun versionSummary(isDebugMode: Boolean): String {
    val suffix = if (isDebugMode) "+debugmode" else ""

    return "${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE}${suffix})"
}

@Preview(
    name = "Light Mode",
    showBackground = true,
)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun PreviewSettingsScreen() {
    AppTheme {
        AppScreen(
            title = { Text(text = stringResource(R.string.app_name)) },
        ) { params ->
            SettingsContent(
                notificationsGranted = false,
                speedGranted = false,
                autoStart = true,
                wakeLock = true,
                stopOnDisconnect = true,
                forceHorizontal = false,
                speedThreshold = Preferences.SPEED_THRESHOLD_PRESETS[0],
                widthOffset = 0,
                heightOffset = 0,
                lastSurfaceInfo = "1920x720 @ 160dpi",
                isDebugMode = true,
                onNotificationsGrant = {},
                onSpeedGrant = {},
                onAutoStartChange = {},
                onWakeLockChange = {},
                onStopOnDisconnectChange = {},
                onForceHorizontalChange = {},
                onSpeedThresholdCycle = {},
                onWidthOffsetCycle = {},
                onHeightOffsetCycle = {},
                onDebugModeChange = {},
                onSourceRepoOpen = {},
                onSupportOpen = {},
                onSaveLogs = {},
                onSaveSessionLog = {},
                contentPadding = params.contentPadding,
            )
        }
    }
}
