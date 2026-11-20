package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ai.VoiceProductionPack
import com.example.data.model.AiTaskState
import com.example.data.model.CharacterProfile
import com.example.data.model.SavedPrompt
import com.example.data.model.StoryboardProject
import com.example.data.model.StoryboardScene
import com.example.data.model.StudioSubTab
import com.example.data.model.UiState
import com.example.ui.theme.LocalStudioColors
import com.example.viewmodel.ImageRenderState
import java.io.File

@Composable
fun StudioHubScreen(
    selectedTab: StudioSubTab,
    onSelectTab: (StudioSubTab) -> Unit,
    projects: List<StoryboardProject>,
    activeProject: StoryboardProject?,
    scenesState: UiState<List<StoryboardScene>>,
    charactersState: UiState<List<CharacterProfile>>,
    promptHistory: List<SavedPrompt>,
    storyboardTaskState: AiTaskState,
    regeneratingSceneId: String?,
    characterTaskState: AiTaskState,
    characterImages: Map<String, ImageRenderState>,
    cachedCharImageLookup: (String) -> File?,
    imageStudioTaskState: AiTaskState,
    generatedImagePrompt: String,
    studioPreviewImageState: ImageRenderState,
    videoStudioTaskState: AiTaskState,
    generatedVideoPrompt: String,
    voiceStudioTaskState: AiTaskState,
    generatedVoicePack: VoiceProductionPack?,
    defaultArtStyle: String,
    defaultAspectRatio: String,
    onSelectProject: (String) -> Unit,
    onCreateStory: () -> Unit,
    onGenerateScenes: (Int) -> Unit,
    onRegenerateScene: (StoryboardScene, String) -> Unit,
    onSaveScene: (StoryboardScene, Boolean) -> Unit,
    onDuplicateScene: (StoryboardScene) -> Unit,
    onDeleteScene: (StoryboardScene) -> Unit,
    onGenerateCharacters: (String) -> Unit,
    onSaveCharacter: (CharacterProfile, Boolean) -> Unit,
    onDeleteCharacter: (String) -> Unit,
    onGenerateCharacterPortrait: (CharacterProfile) -> Unit,
    onGenerateImagePrompt: (String, String, String, Set<String>) -> Unit,
    onRenderPreviewImage: (String, String, String) -> Unit,
    onGenerateVideoPrompt: (String, String, Long, String, String, Set<String>) -> Unit,
    onSavePromptToHistory: (String, String, String, String, String, Long) -> Unit,
    onDeleteSavedPrompt: (String) -> Unit,
    onGenerateVoicePack: (String, String) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    val charactersList = (charactersState as? UiState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Studio Module Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioSubTab.entries.forEach { tab ->
                val active = tab == selectedTab
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        1.dp,
                        if (active) MaterialTheme.colorScheme.primary else LocalStudioColors.current.cardBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTab(tab) }
                        .testTag("studio_subtab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                StudioSubTab.STORYBOARD -> StoryboardStudioTab(
                    projects = projects,
                    activeProject = activeProject,
                    scenesState = scenesState,
                    taskState = storyboardTaskState,
                    regeneratingSceneId = regeneratingSceneId,
                    onSelectProject = onSelectProject,
                    onCreateStory = onCreateStory,
                    onGenerateScenes = onGenerateScenes,
                    onRegenerateScene = onRegenerateScene,
                    onSaveScene = onSaveScene,
                    onDuplicateScene = onDuplicateScene,
                    onDeleteScene = onDeleteScene,
                    onCopiedNotice = onCopiedNotice
                )
                StudioSubTab.CHARACTERS -> CharacterStudioTab(
                    activeProject = activeProject,
                    charactersState = charactersState,
                    taskState = characterTaskState,
                    characterImages = characterImages,
                    cachedImageLookup = cachedCharImageLookup,
                    onGenerateCastWithAi = onGenerateCharacters,
                    onSaveCharacter = onSaveCharacter,
                    onDeleteCharacter = onDeleteCharacter,
                    onGeneratePortrait = onGenerateCharacterPortrait,
                    onCopiedNotice = onCopiedNotice
                )
                StudioSubTab.IMAGE_PROMPT -> ImagePromptStudioTab(
                    characters = charactersList,
                    savedImagePrompts = promptHistory.filter { it.promptType == "IMAGE" },
                    generatedPrompt = generatedImagePrompt,
                    taskState = imageStudioTaskState,
                    previewImageState = studioPreviewImageState,
                    defaultArtStyle = activeProject?.artStyle ?: defaultArtStyle,
                    defaultAspectRatio = activeProject?.aspectRatio ?: defaultAspectRatio,
                    onGeneratePrompt = onGenerateImagePrompt,
                    onRenderPreviewImage = onRenderPreviewImage,
                    onSavePrompt = { title, prompt, ratio, style ->
                        onSavePromptToHistory("IMAGE", title, prompt, ratio, style, 0L)
                    },
                    onDeletePrompt = onDeleteSavedPrompt,
                    onCopiedNotice = onCopiedNotice
                )
                StudioSubTab.VIDEO_PROMPT -> VideoPromptStudioTab(
                    characters = charactersList,
                    savedVideoPrompts = promptHistory.filter { it.promptType == "VIDEO" },
                    generatedPrompt = generatedVideoPrompt,
                    taskState = videoStudioTaskState,
                    defaultArtStyle = activeProject?.artStyle ?: defaultArtStyle,
                    defaultAspectRatio = activeProject?.aspectRatio ?: defaultAspectRatio,
                    onGenerateVideoPrompt = onGenerateVideoPrompt,
                    onSavePrompt = { title, prompt, ratio, cam, dur ->
                        onSavePromptToHistory("VIDEO", title, prompt, ratio, cam, dur)
                    },
                    onDeletePrompt = onDeleteSavedPrompt,
                    onCopiedNotice = onCopiedNotice
                )
                StudioSubTab.VOICE_AUDIO -> VoiceStudioTab(
                    activeProject = activeProject,
                    voicePack = generatedVoicePack,
                    taskState = voiceStudioTaskState,
                    onGenerateVoicePack = onGenerateVoicePack,
                    onSaveVoiceScript = { title, script ->
                        onSavePromptToHistory("VOICE", title, script, "", "Hindi VO", 0L)
                    },
                    onCopiedNotice = onCopiedNotice
                )
            }
        }
    }
}
