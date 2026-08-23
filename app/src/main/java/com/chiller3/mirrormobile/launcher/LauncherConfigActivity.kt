/*
 * SPDX-FileCopyrightText: 2026 txurtxil
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.chiller3.mirrormobile.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chiller3.mirrormobile.ui.theme.AppTheme

class LauncherConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                LauncherConfigScreen()
            }
        }
    }
}
