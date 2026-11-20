package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AiTaskState
import com.example.data.model.StoryboardProject
import com.example.data.model.StoryboardScene
import com.example.data.model.UiState
import com.example.ui.components.ActiveProjectStrip
import com.example.ui.components.AiStatusBanner
import com.example.ui.components.CopyablePromptBlock
import com.example.ui.components.EmptyStudioPlaceholder
import com.example.ui.components.GradientActionButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.theme.LocalStudioColors

@Composable
fun StoryboardStudioTab(
    projects: List<StoryboardProject>,
    activeProject: StoryboardProject?,
    scenesState: UiState<List<StoryboardScene>>,
    taskState: AiTaskState,
    regeneratingSceneId: String?,
    onSelectProject: (String) -> Unit,
    onCreateStory: () -> Unit,
    onGenerateScenes: (Int) -> Unit,
    onRegenerateScene: (StoryboardScene, String) -> Unit,
    onSaveScene: (StoryboardScene, Boolean) -> Unit,
    onDuplicateScene: (StoryboardScene) -> Unit,
    onDeleteScene: (StoryboardScene) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    var editingScene by remember { mutableStateOf<Pair<StoryboardScene, Boolean>?>(null) }
    var regenTargetScene by remember { mutableStateOf<StoryboardScene?>(null) }
    val isBusy = taskState is AiTaskState.Loading || taskState is AiTaskState.Streaming

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ActiveProjectStrip(
                projects = projects,
                activeProject = activeProject,
                onSelectProject = onSelectProject,
                onCreateNew = onCreateStory
            )
        }

        item {
            SectionHeader(
                title = "AI Storyboard Studio",
                subtitle = if (activeProject != null) {
                    "${activeProject.title} • ${activeProject.artStyle} • ${activeProject.aspectRatio}"
                } else {
                    "Convert your story into shot-by-shot cinema scenes"
                },
                badge = activeProject?.let { "Ep ${it.currentEpisode}" }
            )
        }

        if (activeProject != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GradientActionButton(
                        text = if (isBusy) "Generating Scenes…" else "Generate 5 Scenes with AI",
                        icon = Icons.Filled.AutoAwesome,
                        isLoading = isBusy,
                        enabled = !isBusy,
                        testTag = "storyboard_generate_scenes_button",
                        onClick = { onGenerateScenes(5) },
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, LocalStudioColors.current.cardBorder)
                    ) {
                        IconButton(
                            onClick = {
                                val list = (scenesState as? UiState.Success)?.data.orEmpty()
                                val nextNum = ((list.maxOfOrNull { it.sceneNumber } ?: 0L) + 1L).coerceAtMost(200L)
                                editingScene = StoryboardScene(
                                    projectId = activeProject.id,
                                    episodeNumber = activeProject.currentEpisode,
                                    sceneNumber = nextNum,
                                    title = "Scene $nextNum",
                                    durationSeconds = 6L
                                ) to true
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("storyboard_add_manual_scene_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add manual scene",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        if (taskState !is AiTaskState.Idle) {
            item { AiStatusBanner(state = taskState) }
        }

        when (scenesState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    AiStatusBanner(state = AiTaskState.Error(scenesState.message))
                }
            }
            is UiState.Success -> {
                val scenes = scenesState.data
                if (activeProject == null) {
                    item {
                        EmptyStudioPlaceholder(
                            icon = Icons.Filled.DashboardCustomize,
                            title = "Create a Story First",
                            description = "Generate or write a story in the Stories tab, then return here to auto-build your shot-by-shot storyboard.",
                            actionLabel = "Open Story Generator",
                            onAction = onCreateStory
                        )
                    }
                } else if (scenes.isEmpty()) {
                    item {
                        EmptyStudioPlaceholder(
                            icon = Icons.Filled.DashboardCustomize,
                            title = "No Storyboard Scenes Yet",
                            description = "Tap 'Generate 5 Scenes with AI' to automatically break \"${activeProject.title}\" into camera shots, English image prompts, Google Flow video prompts, and Hindi dialogue.",
                            actionLabel = "Generate Scenes Now",
                            onAction = { onGenerateScenes(5) }
                        )
                    }
                } else {
                    items(scenes, key = { it.id }) { scene ->
                        SceneCard(
                            scene = scene,
                            isRegenerating = regeneratingSceneId == scene.id,
                            onEdit = { editingScene = scene to false },
                            onRegenerate = { regenTargetScene = scene },
                            onDuplicate = { onDuplicateScene(scene) },
                            onDelete = { onDeleteScene(scene) },
                            onCopiedNotice = onCopiedNotice
                        )
                    }
                }
            }
        }
    }

    editingScene?.let { (scene, isNew) ->
        SceneEditorDialog(
            initialScene = scene,
            isNew = isNew,
            onDismiss = { editingScene = null },
            onSave = { updated ->
                onSaveScene(updated, isNew)
                editingScene = null
            }
        )
    }

    regenTargetScene?.let { scene ->
        RegenerateSceneDialog(
            scene = scene,
            onDismiss = { regenTargetScene = null },
            onConfirm = { notes ->
                onRegenerateScene(scene, notes)
                regenTargetScene = null
            }
        )
    }
}

@Composable
private fun SceneCard(
    scene: StoryboardScene,
    isRegenerating: Boolean,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    StudioCard(
        modifier = Modifier.testTag("scene_card_${scene.sceneNumber}")
    ) {
        // Header row: Scene badge + Title + Duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SC ${scene.sceneNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scene.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ep ${scene.episodeNumber} • ${scene.durationSeconds}s clip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (isRegenerating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        if (scene.description.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = scene.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Camera & Action Specs
        if (scene.cameraAngle.isNotBlank() || scene.cameraMovement.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, LocalStudioColors.current.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "${scene.cameraAngle} • ${scene.cameraMovement}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (scene.characterAction.isNotBlank()) {
                            Text(
                                text = "Action: ${scene.characterAction}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // English Image Prompt
        if (scene.imagePrompt.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            CopyablePromptBlock(
                label = "Image Prompt (English)",
                text = scene.imagePrompt,
                accentColor = MaterialTheme.colorScheme.primary,
                onCopied = { onCopiedNotice("Image prompt copied") }
            )
        }

        // Google Flow Video Prompt
        if (scene.videoPrompt.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            CopyablePromptBlock(
                label = "Google Flow Video Prompt",
                text = scene.videoPrompt,
                accentColor = MaterialTheme.colorScheme.secondary,
                onCopied = { onCopiedNotice("Google Flow video prompt copied") }
            )
        }

        // Hindi Dialogue & Voice-over
        if (scene.hindiDialogue.isNotBlank() || scene.voiceOverScript.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Filled.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "HINDI DIALOGUE & VOICE-OVER (${scene.dialogueCharacter.ifBlank { "Narrator" }})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (scene.hindiDialogue.isNotBlank()) {
                        Text(
                            text = "\"${scene.hindiDialogue}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (scene.voiceOverScript.isNotBlank()) {
                        Text(
                            text = "VO: ${scene.voiceOverScript}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // BGM & SFX cues
        if (scene.bgmPrompt.isNotBlank() || scene.sfxPrompt.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = LocalStudioColors.current.accentBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "BGM: ${scene.bgmPrompt.ifBlank { "—" }}  |  SFX: ${scene.sfxPrompt.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Scene Action Row: Edit, Regenerate, Duplicate, Delete
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(44.dp).testTag("scene_edit_${scene.sceneNumber}")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit scene", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    IconButton(
                        onClick = onRegenerate,
                        enabled = !isRegenerating,
                        modifier = Modifier.size(44.dp).testTag("scene_regen_${scene.sceneNumber}")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Regenerate scene with AI", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    }
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(44.dp).testTag("scene_duplicate_${scene.sceneNumber}")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate scene", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(44.dp).testTag("scene_delete_${scene.sceneNumber}")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete scene", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun RegenerateSceneDialog(
    scene: StoryboardScene,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Regenerate Scene ${scene.sceneNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Give Gemini optional director notes to elevate \"${scene.title}\":",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(300) },
                    placeholder = { Text("e.g., Add dramatic rain lighting & slow-motion sword unsheathing") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(notes) }) {
                Text("Regenerate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SceneEditorDialog(
    initialScene: StoryboardScene,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (StoryboardScene) -> Unit
) {
    var title by remember { mutableStateOf(initialScene.title) }
    var description by remember { mutableStateOf(initialScene.description) }
    var cameraAngle by remember { mutableStateOf(initialScene.cameraAngle) }
    var cameraMovement by remember { mutableStateOf(initialScene.cameraMovement) }
    var duration by remember { mutableStateOf(initialScene.durationSeconds.toString()) }
    var imagePrompt by remember { mutableStateOf(initialScene.imagePrompt) }
    var videoPrompt by remember { mutableStateOf(initialScene.videoPrompt) }
    var dialogueCharacter by remember { mutableStateOf(initialScene.dialogueCharacter) }
    var hindiDialogue by remember { mutableStateOf(initialScene.hindiDialogue) }
    var voiceOver by remember { mutableStateOf(initialScene.voiceOverScript) }
    var bgm by remember { mutableStateOf(initialScene.bgmPrompt) }
    var sfx by remember { mutableStateOf(initialScene.sfxPrompt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add Storyboard Scene" else "Edit Scene ${initialScene.sceneNumber}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it.take(150) }, label = { Text("Scene Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it.take(3000) }, label = { Text("Visual Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cameraAngle, onValueChange = { cameraAngle = it.take(200) }, label = { Text("Camera Angle") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = duration, onValueChange = { duration = it.filter(Char::isDigit).take(2) }, label = { Text("Sec") }, singleLine = true, modifier = Modifier.width(72.dp))
                }
                OutlinedTextField(value = cameraMovement, onValueChange = { cameraMovement = it.take(300) }, label = { Text("Camera Movement") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = imagePrompt, onValueChange = { imagePrompt = it.take(3000) }, label = { Text("Image Prompt (English)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = videoPrompt, onValueChange = { videoPrompt = it.take(3000) }, label = { Text("Google Flow Video Prompt") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dialogueCharacter, onValueChange = { dialogueCharacter = it.take(120) }, label = { Text("Speaking Character") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = hindiDialogue, onValueChange = { hindiDialogue = it.take(2000) }, label = { Text("Hindi Dialogue") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = voiceOver, onValueChange = { voiceOver = it.take(2000) }, label = { Text("Voice-Over Script") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bgm, onValueChange = { bgm = it.take(500) }, label = { Text("BGM Prompt") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sfx, onValueChange = { sfx = it.take(500) }, label = { Text("SFX Cue") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    val dur = duration.toLongOrNull()?.coerceIn(1L, 60L) ?: 6L
                    onSave(
                        initialScene.copy(
                            title = title.ifBlank { "Scene ${initialScene.sceneNumber}" },
                            description = description,
                            cameraAngle = cameraAngle,
                            cameraMovement = cameraMovement,
                            durationSeconds = dur,
                            imagePrompt = imagePrompt,
                            videoPrompt = videoPrompt,
                            dialogueCharacter = dialogueCharacter,
                            hindiDialogue = hindiDialogue,
                            voiceOverScript = voiceOver,
                            bgmPrompt = bgm,
                            sfxPrompt = sfx
                        )
                    )
                }
            ) {
                Text("Save Scene")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
