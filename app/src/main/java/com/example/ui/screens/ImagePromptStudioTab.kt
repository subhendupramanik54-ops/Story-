package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.components.LocalGeneratedImage
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.viewmodel.ImageRenderState

@Composable
fun ImagePromptStudioTab(
    characters: List<CharacterProfile>,
    savedImagePrompts: List<SavedPrompt>,
    generatedPrompt: String,
    taskState: AiTaskState,
    previewImageState: ImageRenderState,
    defaultArtStyle: String,
    defaultAspectRatio: String,
    onGeneratePrompt: (idea: String, artStyle: String, aspectRatio: String, selectedCharIds: Set<String>) -> Unit,
    onRenderPreviewImage: (prompt: String, artStyle: String, aspectRatio: String) -> Unit,
    onSavePrompt: (title: String, prompt: String, aspectRatio: String, artStyle: String) -> Unit,
    onDeletePrompt: (String) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    var sceneIdea by rememberSaveable { mutableStateOf("") }
    var artStyle by rememberSaveable { mutableStateOf(defaultArtStyle) }
    var aspectRatio by rememberSaveable { mutableStateOf(defaultAspectRatio) }
    var selectedCharIds by remember { mutableStateOf(setOf<String>()) }
    val isBusy = taskState is AiTaskState.Loading

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Image Prompt Studio",
                subtitle = "Craft cinema-grade English prompts & render AI concept frames",
                badge = "$artStyle • $aspectRatio"
            )
        }

        item {
            StudioCard {
                OutlinedTextField(
                    value = sceneIdea,
                    onValueChange = { sceneIdea = it.take(600) },
                    label = { Text("Scene Visual Idea") },
                    placeholder = { Text("e.g., Hero standing on rain-slicked Jaipur rooftop at dusk with glowing celestial sword") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_studio_idea_input")
                )

                Spacer(Modifier.height(12.dp))
                HorizontalOptionRow(
                    label = "ASPECT RATIO",
                    options = StudioOptions.aspectRatios,
                    selected = aspectRatio,
                    onSelected = { aspectRatio = it },
                    testTagPrefix = "image_ratio"
                )

                Spacer(Modifier.height(12.dp))
                HorizontalOptionRow(
                    label = "ART STYLE",
                    options = StudioOptions.artStyles,
                    selected = artStyle,
                    onSelected = { artStyle = it },
                    testTagPrefix = "image_style"
                )

                if (characters.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "INJECT CHARACTER CONSISTENCY",
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
                    text = if (isBusy) "Crafting Image Prompt…" else "Generate Detailed Image Prompt",
                    icon = Icons.Filled.AutoAwesome,
                    isLoading = isBusy,
                    enabled = !isBusy,
                    testTag = "generate_image_prompt_button",
                    onClick = { onGeneratePrompt(sceneIdea, artStyle, aspectRatio, selectedCharIds) }
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
                        label = "Generated Image Prompt ($aspectRatio • $artStyle)",
                        text = generatedPrompt,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onCopied = { onCopiedNotice("Image prompt copied") }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onRenderPreviewImage(generatedPrompt, artStyle, aspectRatio) },
                            enabled = previewImageState !is ImageRenderState.Loading,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("render_image_preview_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Render Visual")
                        }
                        OutlinedButton(
                            onClick = {
                                onSavePrompt(
                                    sceneIdea.take(60).ifBlank { "$artStyle Frame ($aspectRatio)" },
                                    generatedPrompt,
                                    aspectRatio,
                                    artStyle
                                )
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("save_image_prompt_button")
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Prompt")
                        }
                    }

                    // Live Nano Banana 2.1 Preview Output
                    when (previewImageState) {
                        is ImageRenderState.Idle -> Unit
                        is ImageRenderState.Loading -> {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth().height(170.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Synthesizing artwork via Gemini Nano Banana (30–60s)…",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        is ImageRenderState.Success -> {
                            Spacer(Modifier.height(12.dp))
                            LocalGeneratedImage(
                                file = previewImageState.file,
                                version = previewImageState.version,
                                contentDescription = "AI Rendered Storyboard Visual",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                        }
                        is ImageRenderState.Error -> {
                            Spacer(Modifier.height(12.dp))
                            AiStatusBanner(state = AiTaskState.Error(previewImageState.message))
                        }
                    }
                }
            }
        }

        if (savedImagePrompts.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Saved Image Prompts",
                    badge = "${savedImagePrompts.size}"
                )
            }
            items(savedImagePrompts, key = { it.id }) { item ->
                StudioCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${item.artStyle} • ${item.aspectRatio}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(onClick = { onDeletePrompt(item.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete prompt", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    CopyablePromptBlock(
                        label = "Saved Prompt",
                        text = item.promptText,
                        onCopied = { onCopiedNotice("Prompt copied") }
                    )
                }
            }
        }
    }
}
