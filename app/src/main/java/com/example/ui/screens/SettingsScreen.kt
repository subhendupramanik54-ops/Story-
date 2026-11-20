package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ConnectionHealth
import com.example.data.model.StudioOptions
import com.example.data.repository.StudioPreferences
import com.example.firebase.AppCheckState
import com.example.firebase.AppCheckStatusRegistry
import com.example.ui.components.GradientActionButton
import com.example.ui.components.HorizontalOptionRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.theme.LocalStudioColors

@Composable
fun SettingsScreen(
    userDisplayName: String,
    userEmail: String,
    preferences: StudioPreferences,
    connectionHealth: ConnectionHealth,
    onSelectModel: (String) -> Unit,
    onTestConnection: () -> Unit,
    onDefaultLanguageChanged: (String) -> Unit,
    onDefaultArtStyleChanged: (String) -> Unit,
    onDefaultAspectRatioChanged: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val studioColors = LocalStudioColors.current
    val databaseId = context.getString(R.string.firestore_database_id)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Studio Settings",
                subtitle = "AI engine configuration, live backend status & creator defaults"
            )
        }

        // Google Creator Account Card
        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Creator profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userDisplayName.ifBlank { "Bharat Creator" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userEmail.ifBlank { "Google Account Connected" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("settings_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out of Google")
                }
            }
        }

        // AI API Configuration & Live Connection Status
        item {
            StudioCard(
                borderColor = when (connectionHealth.isConnected) {
                    true -> studioColors.success.copy(alpha = 0.6f)
                    false -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    null -> LocalStudioColors.current.cardBorder
                }
            ) {
                SectionHeader(
                    title = "AI Engine & Connection Status",
                    subtitle = "Secured via Firebase AI Logic & App Check (no manual API key needed)",
                    badge = when (connectionHealth.isConnected) {
                        true -> "ONLINE"
                        false -> "ERROR"
                        null -> "READY"
                    }
                )

                Spacer(Modifier.height(12.dp))

                // App Check & Firestore telemetry pills
                StatusMetricRow(
                    icon = Icons.Filled.VerifiedUser,
                    label = "App Check Attestation",
                    value = AppCheckStatusRegistry.detail,
                    isHealthy = AppCheckStatusRegistry.state == AppCheckState.DEBUG_CONFIGURED ||
                        AppCheckStatusRegistry.state == AppCheckState.RELEASE_RECAPTCHA
                )
                Spacer(Modifier.height(8.dp))
                StatusMetricRow(
                    icon = Icons.Filled.CloudDone,
                    label = "Cloud Firestore Enterprise",
                    value = databaseId.take(36),
                    isHealthy = true
                )
                Spacer(Modifier.height(8.dp))
                StatusMetricRow(
                    icon = if (connectionHealth.isConnected == false) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                    label = "Gemini Live Ping",
                    value = buildString {
                        append(connectionHealth.statusDetail)
                        if (connectionHealth.lastCheckedAt.isNotBlank()) {
                            append(" • ${connectionHealth.lastCheckedAt}")
                        }
                    },
                    isHealthy = connectionHealth.isConnected != false
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "ACTIVE GEMINI TEXT MODEL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioOptions.geminiModels.forEach { spec ->
                        val selected = preferences.selectedModelId == spec.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else studioColors.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectModel(spec.id) }
                                .testTag("settings_model_${spec.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSelectModel(spec.id) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = spec.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = spec.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Image Generation Model: gemini-nano-banana-2.1 (Nano Banana)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(Modifier.height(14.dp))
                GradientActionButton(
                    text = if (connectionHealth.isTesting) "Pinging Gemini AI…" else "Test Live Connection",
                    icon = Icons.Filled.NetworkCheck,
                    isLoading = connectionHealth.isTesting,
                    enabled = !connectionHealth.isTesting,
                    testTag = "settings_test_connection_button",
                    onClick = onTestConnection
                )
            }
        }

        // Studio Production Defaults
        item {
            StudioCard {
                SectionHeader(
                    title = "Creator Defaults",
                    subtitle = "Pre-selected settings for new stories and prompts"
                )
                Spacer(Modifier.height(12.dp))
                HorizontalOptionRow(
                    label = "DEFAULT STORY LANGUAGE",
                    options = StudioOptions.languages,
                    selected = preferences.defaultLanguage,
                    onSelected = onDefaultLanguageChanged,
                    testTagPrefix = "pref_lang"
                )
                Spacer(Modifier.height(12.dp))
                HorizontalOptionRow(
                    label = "DEFAULT ART STYLE",
                    options = StudioOptions.artStyles,
                    selected = preferences.defaultArtStyle,
                    onSelected = onDefaultArtStyleChanged,
                    testTagPrefix = "pref_style"
                )
                Spacer(Modifier.height(12.dp))
                HorizontalOptionRow(
                    label = "DEFAULT ASPECT RATIO",
                    options = StudioOptions.aspectRatios,
                    selected = preferences.defaultAspectRatio,
                    onSelected = onDefaultAspectRatioChanged,
                    testTagPrefix = "pref_ratio"
                )
            }
        }
    }
}

@Composable
private fun StatusMetricRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isHealthy: Boolean
) {
    val studioColors = LocalStudioColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isHealthy) studioColors.success else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = if (isHealthy) Color.White else MaterialTheme.colorScheme.error
            )
        }
    }
}
