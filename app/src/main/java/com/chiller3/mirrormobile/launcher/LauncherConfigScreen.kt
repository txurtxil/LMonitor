/*
 * SPDX-FileCopyrightText: 2026 txurtxil
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.chiller3.mirrormobile.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun LauncherConfigScreen() {
    val context = LocalContext.current
    val prefs = remember { Preferences(context) }
    val apps = remember { context.queryLauncherApps() }
    var selected by remember { mutableStateOf(prefs.launcherApps) }

    AppScreen(
        title = { Text(text = stringResource(R.string.launcher_config_title)) },
    ) { params ->
        LazyColumn(contentPadding = params.contentPadding) {
            items(apps, key = { it.packageName }) { app ->
                val checked = app.packageName in selected

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (checked) {
                                selected - app.packageName
                            } else {
                                selected + app.packageName
                            }
                            prefs.launcherApps = selected
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        bitmap = remember(app.packageName) { app.icon.toImageBitmap() },
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = app.label, modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            selected = if (isChecked) {
                                selected + app.packageName
                            } else {
                                selected - app.packageName
                            }
                            prefs.launcherApps = selected
                        },
                    )
                }
            }
        }
    }
}
