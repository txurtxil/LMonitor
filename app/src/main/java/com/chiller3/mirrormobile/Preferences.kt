/*
 * SPDX-FileCopyrightText: 2023-2024 Andrew Gunnerson
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.chiller3.mirrormobile

import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Preferences(context: Context) {
    companion object {
        // Keep in the same order as the helper functions below.
        const val PREF_DEBUG_MODE = "debug_mode"
        private const val PREF_AUTO_START = "auto_start"
        private const val PREF_WAKE_LOCK = "wake_lock"
        private const val PREF_SPEED_THRESHOLD = "speed_threshold"
        private const val PREF_LAST_SURFACE_INFO = "last_surface_info"
        private const val PREF_LAUNCHER_APPS = "launcher_apps"
        private const val PREF_STOP_ON_DISCONNECT = "stop_on_disconnect"

        // Presets del umbral de velocidad (m/s), se ciclan al tocar la preferencia.
        // 0.001f es el valor original de upstream; se mantiene como preset[0] para que
        // el comportamiento no cambie salvo que el usuario elija otro explícitamente.
        val SPEED_THRESHOLD_PRESETS = floatArrayOf(0.001f, 0.5f, 1.0f, 1.5f, 2.0f)
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun registerListener(listener: OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    var isDebugMode: Boolean
        get() = prefs.getBoolean(PREF_DEBUG_MODE, false)
        set(enabled) = prefs.edit { putBoolean(PREF_DEBUG_MODE, enabled) }

    var autoStart: Boolean
        get() = prefs.getBoolean(PREF_AUTO_START, true)
        set(enabled) = prefs.edit { putBoolean(PREF_AUTO_START, enabled) }

    var wakeLock: Boolean
        get() = prefs.getBoolean(PREF_WAKE_LOCK, true)
        set(enabled) = prefs.edit { putBoolean(PREF_WAKE_LOCK, enabled) }

    var speedThreshold: Float
        get() = prefs.getFloat(PREF_SPEED_THRESHOLD, SPEED_THRESHOLD_PRESETS[0])
        set(value) = prefs.edit { putFloat(PREF_SPEED_THRESHOLD, value) }

    var lastSurfaceInfo: String
        get() = prefs.getString(PREF_LAST_SURFACE_INFO, "") ?: ""
        set(value) = prefs.edit { putString(PREF_LAST_SURFACE_INFO, value) }

    var launcherApps: Set<String>
        get() = prefs.getStringSet(PREF_LAUNCHER_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet(PREF_LAUNCHER_APPS, value) }

    // true (default, comportamiento original): al desconectar de Android Auto se
    // libera el permiso de MediaProjection por completo.
    // false: se mantiene la sesion de captura viva en segundo plano para reanudar
    // sin volver a pedir permiso al reconectar -- coste: notificacion persistente
    // y wakelock activos mientras el coche este desconectado.
    var stopOnDisconnect: Boolean
        get() = prefs.getBoolean(PREF_STOP_ON_DISCONNECT, true)
        set(enabled) = prefs.edit { putBoolean(PREF_STOP_ON_DISCONNECT, enabled) }
}
