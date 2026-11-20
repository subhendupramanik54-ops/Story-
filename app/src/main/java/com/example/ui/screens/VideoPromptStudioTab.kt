package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AiTaskState
import com.example.data.model.CharacterProfile
import com.example.data.model.SavedPrompt
import com.example.data.model.StudioOptions
import com.example.ui.components.AiStatusBanner
import com.example.ui.components.CopyablePromptBlock
import com.example.ui.components.GradientActionButton
import com.example.ui.components.HorizontalOptionRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.components.copyToClipboard

@Composable
fun VideoPromptStudioTab(
    characters: List<CharacterProfile>,
    savedVideoPrompts: List<SavedPrompt>,
    generatedPrompt: String,
    taskState: AiTaskState,
    defaultArtStyle: String,
    defaultAspectRatio: String,
    onGenerateVideoPrompt: (
        action: String,
        cameraMovement: String,
        durationSeconds: Long,
        aspectRatio: String,
        artStyle: String,
        selectedCharIds: Set<String>
    ) -> Unit,
    onSavePrompt: (title: String, prompt: String, aspectRatio: String, cameraMove: String, durationSeconds: Long) -> Unit,
    onDeletePrompt: (String) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    val context = LocalContext.current
    var sceneAction by rememberSaveable { mutableStateOf("") }
    var cameraMovement by rememberSaveable { mutableStateOf(StudioOptions.cameraMovements.first()) }
    var durationSeconds by rememberSaveable { mutableLongStateOf(6L) }
    var aspectRatio by rememberSaveable { mutableStateOf(defaultAspectRatio) }
    var artStyle by rememberSaveable { mutableStateOf(defaultArtStyle) }
    var selectedCharIds by remember { mutableStateOf(setOf<String>()) }
    val isBusy = taskState is AiTaskState.Loading

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Google Flow Video Studio",
                subtitle = "Generate Image-to-Video camera motion, action & consistency prompts",
                badge = "${durationSeconds}s • $aspectRatio"
            )
        }

        item {
            StudioCard {
                OutlinedTextField(
                    value = sceneAction,
                    onValueChange = { sceneAction = it.take(600) },
                    label = { Text("Character Action & Shot Motion") },
                    placeholder = { Text("e.g., Hero leaps across neon temple pillars as golden sparks trail behind cape") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_studio_action_input")
                )

                Spacer(Modifier.height(12.dp))
                HorizontalOptionRow(
                    label = "CAMERA MOVEMENT",
                    options = StudioOptions.cameraMovements,
                    selected = cameraMovement,
                    onSelected = { cameraMovement = it },
                    testTagPrefix = "video_cam"
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "SCENE DURATION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioOptions.videoDurations.forEach { sec ->
                        FilterChip(
                            selected = durationSeconds == sec,
                            onClick = { durationSeconds = sec },
                            label = { Text("${sec}s") },
                            modifier = Modifier.testTag("video_duration_${sec}s")
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalOptionRow(
                    label = "ASPECT RATIO",
                    options = listOf("9:16", "16:9"),
                    selected = if (aspectRatio == "1:1") "9:16" else aspectRatio,
                    onSelected = { aspectRatio = it },
                    testTagPrefix = "video_ratio"
                )

                Spacer(Modifier.height(10.dp))
                HorizontalOptionRow(
                    label = "VISUAL STYLE",
                    options = StudioOptions.artStyles,
                    selected = artStyle,
                    onSelected = { artStyle = it },
                    testTagPrefix = "video_style"
                )

                if (characters.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "LOCK CHARACTER CONSISTENCY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        characters.forEach { ch ->
                            val isSelected = ch.id in selectedCharIds
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCharIds = if (isSelected) selectedCharIds - ch.id else selectedCharIds + ch.id
                                },
                                label = { Text(ch.name) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                GradientActionButton(
                    text = if (isBusy) "Composing Flow Prompt…" else "Generate Google Flow Prompt",
                    icon = Icons.Filled.VideoCameraBack,
                    isLoading = isBusy,
                    enabled = !isBusy,
                    testTag = "generate_video_prompt_button",
                    onClick = {
                        onGenerateVideoPrompt(
                            sceneAction,
                            cameraMovement,
                            durationSeconds,
                            if (aspectRatio == "1:1") "9:16" else aspectRatio,
                            artStyle,
                            selectedCharIds
                        )
                    }
                )
            }
        }

        if (taskState !is AiTaskState.Idle) {
            item { AiStatusBanner(state = taskState) }
        }

        if (generatedPrompt.isNotBlank()) {
            item {
                StudioCard {
                    CopyablePromptBlock(
                        label = "Google Flow Image-to-Video Prompt (${durationSeconds}s)",
                        text = generatedPrompt,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onCopied = { onCopiedNotice("Google Flow prompt copied") }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                copyToClipboard(context, "Google Flow Video Prompt", generatedPrompt)
                                onCopiedNotice("Copied for Google Flow!")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("copy_video_prompt_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy Prompt")
                        }
                        OutlinedButton(
                            onClick = {
                                onSavePrompt(
                                    sceneAction.take(60).ifBlank { "$cameraMovement (${durationSeconds}s)" },
                                    generatedPrompt,
                                    if (aspectRatio == "1:1") "9:16" else aspectRatio,
                                    cameraMovement,
                                    durationSeconds
                                )
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("save_video_prompt_button")
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Prompt")
                        }
                    }
                }
            }
        }

        if (savedVideoPrompts.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Saved Google Flow Prompts",
                    badge = "${savedVideoPrompts.size}"
                )
            }
            items(savedVideoPrompts, key = { it.id }) { item ->
                StudioCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${item.artStyle} • ${item.durationSeconds}s • ${item.aspectRatio}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        IconButton(onClick = { onDeletePrompt(item.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete video prompt", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    CopyablePromptBlock(
                        label = "Google Flow Prompt",
                        text = item.promptText,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onCopied = { onCopiedNotice("Google Flow prompt copied") }
                    )
                }
            }
        }
    }
}
