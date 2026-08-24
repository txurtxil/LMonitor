/*
 * SPDX-FileCopyrightText: 2026 txurtxil
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.chiller3.mirrormobile.extension

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku

/**
 * Fase 1: solo deteccion de si el binder de Shizuku/Sui esta vivo. Todavia no se usa
 * para nada -- ni permisos, ni llamadas privilegiadas. Registrado una vez en
 * MainApplication.onCreate().
 */
object ShizukuStatus {
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _isAvailable.value = true
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isAvailable.value = false
    }

    fun init() {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }
}
