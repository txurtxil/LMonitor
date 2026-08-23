/*
 * SPDX-FileCopyrightText: 2026 txurtxil
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.chiller3.mirrormobile.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chiller3.mirrormobile.Preferences
import com.txurtxil.lmonitor.R
import com.chiller3.mirrormobile.extension.queryLauncherApps
import com.chiller3.mirrormobile.extension.toImageBitmap
import com.chiller3.mirrormobile.ui.AppScreen

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val prefs = remember { Preferences(context) }
    val allApps = remember { context.queryLauncherApps() }
    val selectedPackages = remember { prefs.launcherApps }
    val apps = remember(allApps, selectedPackages) {
        allApps.filter { it.packageName in selectedPackages }
    }

    AppScreen(
        title = { Text(text = stringResource(R.string.launcher_title)) },
    ) { params ->
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(params.contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.launcher_empty))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                contentPadding = params.contentPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .clickable {
                                context.packageManager.getLaunchIntentForPackage(app.packageName)
                                    ?.let { context.startActivity(it) }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            bitmap = remember(app.packageName) { app.icon.toImageBitmap() },
                            contentDescription = app.label,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = app.label, maxLines = 1)
                    }
                }
            }
        }
    }
}
