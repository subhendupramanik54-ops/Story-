package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ai.VoiceProductionPack
import com.example.data.model.AiTaskState
import com.example.data.model.StoryboardProject
import com.example.ui.components.AiStatusBanner
import com.example.ui.components.CopyablePromptBlock
import com.example.ui.components.GradientActionButton
import com.example.ui.components.HorizontalOptionRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.theme.LocalStudioColors
import com.example.util.VoiceAuditionHelper

@Composable
fun VoiceStudioTab(
    activeProject: StoryboardProject?,
    voicePack: VoiceProductionPack?,
    taskState: AiTaskState,
    onGenerateVoicePack: (topicOrScene: String, tone: String) -> Unit,
    onSaveVoiceScript: (title: String, fullScript: String) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    val context = LocalContext.current
    val ttsHelper = remember { VoiceAuditionHelper(context) }
    DisposableEffect(Unit) {
        onDispose { ttsHelper.shutdown() }
    }

    var sceneContext by rememberSaveable { mutableStateOf("") }
    var tone by rememberSaveable { mutableStateOf("Epic & Cinematic") }
    val toneOptions = remember {
        listOf("Epic & Cinematic", "Emotional Drama", "Anime High-Energy", "Suspense Horror", "Warm Storytelling")
    }
    val isBusy = taskState is AiTaskState.Loading

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Voice & Audio Studio",
                subtitle = "Hindi AI voice-over scripts, character dialogues, SRT subtitles, BGM & SFX",
                badge = "Hindi VO"
            )
        }

        item {
            StudioCard {
                OutlinedTextField(
                    value = sceneContext,
                    onValueChange = { sceneContext = it.take(800) },
                    label = { Text("Scene / Dialogue Beat (or leave blank for Active Project)") },
                    placeholder = {
                        Text(
                            activeProject?.let { "Leave blank to script Hindi VO for \"${it.title}\"" }
                                ?: "e.g., Emotional climax confrontation on Varanasi ghats in heavy rain"
                        )
                    },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_studio_input")
                )

                Spacer(Modifier.height(12.dp))
                HorizontalOptionRow(
                    label = "DELIVERY MOOD",
                    options = toneOptions,
                    selected = tone,
                    onSelected = { tone = it },
                    testTagPrefix = "voice_tone"
                )

                Spacer(Modifier.height(14.dp))
                GradientActionButton(
                    text = if (isBusy) "Generating Hindi Voice Pack…" else "Generate Hindi Dialogues, SRT & Audio",
                    icon = Icons.Filled.GraphicEq,
                    isLoading = isBusy,
                    enabled = !isBusy,
                    testTag = "generate_voice_pack_button",
                    onClick = { onGenerateVoicePack(sceneContext, tone) }
                )
            }
        }

        if (taskState !is AiTaskState.Idle) {
            item { AiStatusBanner(state = taskState) }
        }

        if (voicePack != null) {
            // Character-wise Hindi Dialogues with on-device Hindi TTS Audition
            item {
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Character-Wise Hindi Dialogues",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { ttsHelper.stop() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "Stop audio audition", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        voicePack.dialogueLines.forEach { line ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, LocalStudioColors.current.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${line.character} • ${line.emotion}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = line.hindiDialogue,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (line.romanizedHint.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = line.romanizedHint,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val spoken = ttsHelper.speak(line.hindiDialogue)
                                            if (!spoken) onCopiedNotice("Device Hindi TTS initializing — try again")
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Audition Hindi dialogue",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Narrator Voice-Over + Audition + Save
            item {
                StudioCard {
                    CopyablePromptBlock(
                        label = "Hindi Voice-Over Script",
                        text = voicePack.voiceOverScript,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        onCopied = { onCopiedNotice("Voice-over script copied") }
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val spoken = ttsHelper.speak(voicePack.voiceOverScript)
                                if (!spoken) onCopiedNotice("Device Hindi TTS initializing — try again")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("audition_voice_over_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Audition VO")
                        }
                        OutlinedButton(
                            onClick = {
                                val fullScript = buildString {
                                    appendLine("VOICE-OVER:\n${voicePack.voiceOverScript}\n")
                                    appendLine("DIALOGUES:")
                                    voicePack.dialogueLines.forEach {
                                        appendLine("${it.character} (${it.emotion}): ${it.hindiDialogue}")
                                    }
                                    appendLine("\nSRT SUBTITLES:\n${voicePack.srtSubtitles}")
                                    appendLine("\nBGM:\n${voicePack.bgmDirection}")
                                    appendLine("\nSFX:\n${voicePack.sfxList}")
                                }
                                onSaveVoiceScript(
                                    activeProject?.title?.let { "$it — Hindi Voice Pack" } ?: "Hindi Voice Pack ($tone)",
                                    fullScript
                                )
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("save_voice_pack_button")
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Script")
                        }
                    }
                }
            }

            // Subtitles, BGM & SFX Blocks
            item {
                StudioCard {
                    CopyablePromptBlock(
                        label = "SRT Subtitles",
                        text = voicePack.srtSubtitles,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onCopied = { onCopiedNotice("SRT subtitles copied") }
                    )
                    Spacer(Modifier.height(10.dp))
                    CopyablePromptBlock(
                        label = "BGM Direction",
                        text = voicePack.bgmDirection,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onCopied = { onCopiedNotice("BGM prompt copied") }
                    )
                    Spacer(Modifier.height(10.dp))
                    CopyablePromptBlock(
                        label = "SFX Cue Sheet",
                        text = voicePack.sfxList,
                        accentColor = LocalStudioColors.current.accentSaffron,
                        onCopied = { onCopiedNotice("SFX cue sheet copied") }
                    )
                }
            }
        }
    }
}
