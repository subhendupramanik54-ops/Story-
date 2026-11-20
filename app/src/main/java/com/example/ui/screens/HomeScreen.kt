package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BottomNavTab
import com.example.data.model.CharacterProfile
import com.example.data.model.StoryboardProject
import com.example.data.model.StoryboardScene
import com.example.data.model.StudioSubTab
import com.example.ui.components.ActiveProjectStrip
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.theme.LocalStudioColors

@Composable
fun HomeScreen(
    projects: List<StoryboardProject>,
    activeProject: StoryboardProject?,
    activeScenes: List<StoryboardScene>,
    characters: List<CharacterProfile>,
    onSelectProject: (String) -> Unit,
    onNavigateToTab: (BottomNavTab) -> Unit,
    onOpenStudioTab: (StudioSubTab) -> Unit,
    onExportActiveProjectPdf: () -> Unit
) {
    val studioColors = LocalStudioColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero_1795091972315),
                    contentDescription = "Bharat Storyboard AI Studio Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(18.dp)
                        .fillMaxWidth(0.75f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "INDIAN CREATOR STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Active Production Switcher
        item {
            ActiveProjectStrip(
                projects = projects,
                activeProject = activeProject,
                onSelectProject = onSelectProject,
                onCreateNew = { onNavigateToTab(BottomNavTab.STORIES) }
            )
        }

        // Quick Launch Studio Modules (all 8 requested home tiles in 2-column grid)
        item {
            SectionHeader(
                title = "Production Suite",
                subtitle = "Jump directly into any AI filmmaking studio tool",
                badge = "${projects.size} Projects"
            )
        }

        item {
            val tiles = listOf(
                HomeTileData(
                    title = stringResource(R.string.action_create_story),
                    subtitle = "Hindi, Gujarati, Bengali & EN",
                    icon = Icons.Filled.AutoStories,
                    accent = MaterialTheme.colorScheme.primary,
                    tag = "home_tile_create_story",
                    onClick = { onNavigateToTab(BottomNavTab.STORIES) }
                ),
                HomeTileData(
                    title = stringResource(R.string.action_storyboard_studio),
                    subtitle = "${activeScenes.size} scenes in deck",
                    icon = Icons.Filled.DashboardCustomize,
                    accent = MaterialTheme.colorScheme.secondary,
                    tag = "home_tile_storyboard",
                    onClick = { onOpenStudioTab(StudioSubTab.STORYBOARD) }
                ),
                HomeTileData(
                    title = stringResource(R.string.action_character_studio),
                    subtitle = "${characters.size} Character Bibles",
                    icon = Icons.Filled.FaceRetouchingNatural,
                    accent = MaterialTheme.colorScheme.tertiary,
                    tag = "home_tile_characters",
                    onClick = { onOpenStudioTab(StudioSubTab.CHARACTERS) }
                ),
                HomeTileData(
                    title = stringResource(R.string.action_image_prompts),
                    subtitle = "9:16, 16:9, 1:1 & Nano Art",
                    icon = Icons.Filled.Image,
                    accent = Color(0xFFA78BFA),
                    tag = "home_tile_image_prompts",
                    onClick = { onOpenStudioTab(StudioSubTab.IMAGE_PROMPT) }
                ),
                HomeTileData(
                    title = stringResource(R.string.action_video_prompts),
                    subtitle = "Google Flow motion cues",
                    icon = Icons.Filled.VideoCameraBack,
                    accent = Color(0xFF38BDF8),
                    tag = "home_tile_video_prompts",
                    onClick = { onOpenStudioTab(StudioSubTab.VIDEO_PROMPT) }
                ),
                HomeTileData(
                    title = stringResource(R.string.action_voice_studio),
                    subtitle = "Hindi VO, SRT, BGM & SFX",
                    icon = Icons.Filled.GraphicEq,
                    accent = Color(0xFFF59E0B),
                    tag = "home_tile_voice_studio",
                    onClick = { onOpenStudioTab(StudioSubTab.VOICE_AUDIO) }
                ),
                HomeTileData(
                    title = stringResource(R.string.action_my_projects),
                    subtitle = "Cloud history & episodes",
                    icon = Icons.Filled.FolderSpecial,
                    accent = studioColors.success,
                    tag = "home_tile_projects",
                    onClick = { onNavigateToTab(BottomNavTab.PROJECTS) }
                ),
                HomeTileData(
                    title = stringResource(R.string.action_export_project),
                    subtitle = "Share PDF & script deck",
                    icon = Icons.Filled.IosShare,
                    accent = Color(0xFFF472B6),
                    tag = "home_tile_export",
                    onClick = onExportActiveProjectPdf
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { tile ->
                            HomeModuleCard(
                                data = tile,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 6-Step AI Storyboard Workflow Tracker
        item {
            SectionHeader(
                title = "AI Storyboard Workflow",
                subtitle = "Follow the 6-stage Indian filmmaking pipeline"
            )
        }

        item {
            val hasStory = activeProject != null && activeProject.storyText.isNotBlank()
            val hasChars = characters.isNotEmpty()
            val hasScenes = activeScenes.isNotEmpty()
            val hasPrompts = activeScenes.any { it.imagePrompt.isNotBlank() }
            val hasVideo = activeScenes.any { it.videoPrompt.isNotBlank() }
            val hasVoice = activeScenes.any { it.hindiDialogue.isNotBlank() || it.voiceOverScript.isNotBlank() }

            val steps = listOf(
                WorkflowStep("1", "Enter Your Story", "Write or stream an AI story (1–60 episodes)", hasStory) {
                    onNavigateToTab(BottomNavTab.STORIES)
                },
                WorkflowStep("2", "Generate Characters", "Create consistent Character Bibles & portraits", hasChars) {
                    onOpenStudioTab(StudioSubTab.CHARACTERS)
                },
                WorkflowStep("3", "Generate Scenes", "Auto-break story into Scene 1, 2, 3…", hasScenes) {
                    onOpenStudioTab(StudioSubTab.STORYBOARD)
                },
                WorkflowStep("4", "Generate Image Prompts", "Style-locked English image prompts & previews", hasPrompts) {
                    onOpenStudioTab(StudioSubTab.IMAGE_PROMPT)
                },
                WorkflowStep("5", "Google Flow Video Prompts", "Camera motion, action & duration instructions", hasVideo) {
                    onOpenStudioTab(StudioSubTab.VIDEO_PROMPT)
                },
                WorkflowStep("6", "Voice & Export", "Hindi dialogue, BGM, SFX & PDF script", hasVoice) {
                    onOpenStudioTab(StudioSubTab.VOICE_AUDIO)
                }
            )

            StudioCard(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    steps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { step.onClick() }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (step.done) studioColors.successContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (step.done) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Done",
                                        tint = studioColors.success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = step.num,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = step.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class HomeTileData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val tag: String,
    val onClick: () -> Unit
)

private data class WorkflowStep(
    val num: String,
    val title: String,
    val subtitle: String,
    val done: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun HomeModuleCard(
    data: HomeTileData,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(112.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable { data.onClick() }
            .testTag(data.tag),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, data.accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(data.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = data.title,
                    tint = data.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = data.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
