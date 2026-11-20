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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AiTaskState
import com.example.data.model.StoryboardProject
import com.example.data.model.StudioOptions
import com.example.ui.components.AiStatusBanner
import com.example.ui.components.GradientActionButton
import com.example.ui.components.HorizontalOptionRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import kotlin.math.roundToInt

@Composable
fun StoryGeneratorScreen(
    activeProject: StoryboardProject?,
    storyDraftText: String,
    taskState: AiTaskState,
    defaultLanguage: String,
    defaultArtStyle: String,
    defaultAspectRatio: String,
    onDraftChanged: (String) -> Unit,
    onGenerateStory: (premise: String, language: String, genre: String, artStyle: String, totalEpisodes: Int) -> Unit,
    onContinueEpisode: (project: StoryboardProject, twist: String) -> Unit,
    onSaveAsProject: (
        title: String,
        logline: String,
        language: String,
        genre: String,
        artStyle: String,
        aspectRatio: String,
        totalEpisodes: Int,
        storyText: String,
        onSaved: (String) -> Unit
    ) -> Unit,
    onOpenStoryboard: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var premise by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf(defaultLanguage) }
    var genre by rememberSaveable { mutableStateOf("Anime") }
    var artStyle by rememberSaveable { mutableStateOf(defaultArtStyle) }
    var aspectRatio by rememberSaveable { mutableStateOf(defaultAspectRatio) }
    var episodesSlider by rememberSaveable { mutableFloatStateOf(10f) }
    var nextEpisodeTwist by rememberSaveable { mutableStateOf("") }

    val isBusy = taskState is AiTaskState.Loading || taskState is AiTaskState.Streaming

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "AI Story Generator",
                subtitle = "Craft original Indian stories across 1–60 episodes with live Gemini streaming",
                badge = language
            )
        }

        item {
            StudioCard {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Story / Series Title") },
                    placeholder = { Text("e.g., Agni Yodha: Shadows of Kashi") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_title_input")
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = premise,
                    onValueChange = { premise = it.take(500) },
                    label = { Text("Story Idea / Premise") },
                    placeholder = { Text("Describe your hero, setting, conflict, or emotional hook…") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_premise_input")
                )

                Spacer(Modifier.height(14.dp))

                HorizontalOptionRow(
                    label = "STORY LANGUAGE",
                    options = StudioOptions.languages,
                    selected = language,
                    onSelected = { language = it },
                    testTagPrefix = "story_lang"
                )

                Spacer(Modifier.height(12.dp))

                HorizontalOptionRow(
                    label = "GENRE",
                    options = StudioOptions.genres,
                    selected = genre,
                    onSelected = { genre = it },
                    testTagPrefix = "story_genre"
                )

                Spacer(Modifier.height(12.dp))

                HorizontalOptionRow(
                    label = "VISUAL ART STYLE",
                    options = StudioOptions.artStyles,
                    selected = artStyle,
                    onSelected = { artStyle = it },
                    testTagPrefix = "story_style"
                )

                Spacer(Modifier.height(12.dp))

                HorizontalOptionRow(
                    label = "TARGET ASPECT RATIO",
                    options = StudioOptions.aspectRatios,
                    selected = aspectRatio,
                    onSelected = { aspectRatio = it },
                    testTagPrefix = "story_ratio"
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EPISODE ARC LENGTH",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${episodesSlider.roundToInt()} Episodes",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Slider(
                    value = episodesSlider,
                    onValueChange = { episodesSlider = it },
                    valueRange = 1f..60f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.tertiary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("story_episodes_slider")
                )

                Spacer(Modifier.height(8.dp))

                GradientActionButton(
                    text = if (isBusy) "Streaming Story…" else "Generate Episode 1 with Gemini AI",
                    icon = Icons.Filled.AutoAwesome,
                    isLoading = isBusy,
                    enabled = !isBusy,
                    testTag = "generate_story_button",
                    onClick = {
                        onGenerateStory(
                            premise,
                            language,
                            genre,
                            artStyle,
                            episodesSlider.roundToInt()
                        )
                    }
                )
            }
        }

        if (taskState !is AiTaskState.Idle) {
            item {
                AiStatusBanner(state = taskState)
            }
        }

        // Story Manuscript Editor & Save Actions
        item {
            StudioCard {
                SectionHeader(
                    title = "Story Manuscript",
                    subtitle = "Edit directly or save straight into your cloud projects"
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = storyDraftText,
                    onValueChange = onDraftChanged,
                    placeholder = { Text("Your generated story or custom script appears here…") },
                    minLines = 7,
                    maxLines = 16,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_manuscript_editor")
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveAsProject(
                                title.ifBlank { "$genre Saga ($language)" },
                                premise,
                                language,
                                genre,
                                artStyle,
                                aspectRatio,
                                episodesSlider.roundToInt(),
                                storyDraftText
                            ) { }
                        },
                        enabled = storyDraftText.isNotBlank() && !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("save_story_project_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save Project")
                    }

                    OutlinedButton(
                        onClick = {
                            if (storyDraftText.isNotBlank()) {
                                onSaveAsProject(
                                    title.ifBlank { "$genre Saga ($language)" },
                                    premise,
                                    language,
                                    genre,
                                    artStyle,
                                    aspectRatio,
                                    episodesSlider.roundToInt(),
                                    storyDraftText
                                ) {
                                    onOpenStoryboard()
                                }
                            } else {
                                onOpenStoryboard()
                            }
                        },
                        enabled = !isBusy,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("story_to_storyboard_button")
                    ) {
                        Icon(Icons.Filled.DashboardCustomize, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("To Storyboard")
                    }
                }
            }
        }

        // Automatic Story Continuation for Active Project
        if (activeProject != null) {
            item {
                StudioCard(
                    borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
                ) {
                    SectionHeader(
                        title = "Automatic Episode Continuation",
                        subtitle = "Active: ${activeProject.title} • Currently on Episode ${activeProject.currentEpisode} of ${activeProject.totalEpisodes}",
                        badge = "Ep ${activeProject.currentEpisode}/${activeProject.totalEpisodes}"
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nextEpisodeTwist,
                        onValueChange = { nextEpisodeTwist = it.take(300) },
                        label = { Text("Optional Twist / Direction for Next Episode") },
                        placeholder = { Text("e.g., Introduce a rival from Mumbai underground…") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("continuation_twist_input")
                    )
                    Spacer(Modifier.height(12.dp))
                    GradientActionButton(
                        text = "Continue Episode ${(activeProject.currentEpisode + 1L).coerceAtMost(60L)}",
                        icon = Icons.Filled.FastForward,
                        isLoading = isBusy,
                        enabled = !isBusy && activeProject.currentEpisode < 60L,
                        testTag = "continue_episode_button",
                        onClick = {
                            onContinueEpisode(activeProject, nextEpisodeTwist)
                            nextEpisodeTwist = ""
                        }
                    )
                }
            }
        }
    }
}
