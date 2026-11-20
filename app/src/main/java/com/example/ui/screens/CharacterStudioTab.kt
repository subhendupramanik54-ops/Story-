package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AiTaskState
import com.example.data.model.CharacterProfile
import com.example.data.model.StoryboardProject
import com.example.data.model.UiState
import com.example.ui.components.AiStatusBanner
import com.example.ui.components.CopyablePromptBlock
import com.example.ui.components.EmptyStudioPlaceholder
import com.example.ui.components.GradientActionButton
import com.example.ui.components.LocalGeneratedImage
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.theme.LocalStudioColors
import com.example.viewmodel.ImageRenderState
import java.io.File

@Composable
fun CharacterStudioTab(
    activeProject: StoryboardProject?,
    charactersState: UiState<List<CharacterProfile>>,
    taskState: AiTaskState,
    characterImages: Map<String, ImageRenderState>,
    cachedImageLookup: (String) -> File?,
    onGenerateCastWithAi: (String) -> Unit,
    onSaveCharacter: (CharacterProfile, Boolean) -> Unit,
    onDeleteCharacter: (String) -> Unit,
    onGeneratePortrait: (CharacterProfile) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    var customConcept by remember { mutableStateOf("") }
    var editingCharacter by remember { mutableStateOf<Pair<CharacterProfile, Boolean>?>(null) }
    val isBusy = taskState is AiTaskState.Loading

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Character Studio Atmospheric Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_character_forge_1795092024419),
                    contentDescription = "Character Bible Studio Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.25f), MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Character Bible Studio",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Face, wardrobe, expressions & reusable consistency locks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // AI Cast Generator Card
        item {
            StudioCard {
                OutlinedTextField(
                    value = customConcept,
                    onValueChange = { customConcept = it.take(400) },
                    label = { Text("Cast Concept (or leave blank to extract from Active Project)") },
                    placeholder = {
                        Text(
                            activeProject?.let { "Leave blank to extract cast from \"${it.title}\"" }
                                ?: "e.g., Cyber-Rajput warrior, mystical astrologer mentor, rival shadow assassin"
                        )
                    },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("character_concept_input")
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GradientActionButton(
                        text = if (isBusy) "Creating Cast…" else "Generate Character Bible",
                        icon = Icons.Filled.AutoAwesome,
                        isLoading = isBusy,
                        enabled = !isBusy,
                        testTag = "generate_characters_button",
                        onClick = { onGenerateCastWithAi(customConcept) },
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, LocalStudioColors.current.cardBorder)
                    ) {
                        IconButton(
                            onClick = {
                                editingCharacter = CharacterProfile(
                                    projectId = activeProject?.id.orEmpty()
                                ) to true
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("add_manual_character_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add manual character",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }

        if (taskState !is AiTaskState.Idle) {
            item { AiStatusBanner(state = taskState) }
        }

        when (charactersState) {
            is UiState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item { AiStatusBanner(state = AiTaskState.Error(charactersState.message)) }
            }
            is UiState.Success -> {
                val chars = charactersState.data
                if (chars.isEmpty()) {
                    item {
                        EmptyStudioPlaceholder(
                            icon = Icons.Filled.FaceRetouchingNatural,
                            title = "No Characters in Bible",
                            description = "Generate consistent Indian anime/cinema character sheets from your story or create custom reusable character profiles.",
                            actionLabel = "Auto-Generate Cast",
                            onAction = { onGenerateCastWithAi(customConcept) }
                        )
                    }
                } else {
                    item {
                        SectionHeader(
                            title = "Saved Character Profiles",
                            subtitle = "Reusable across Storyboard, Image Studio & Google Flow",
                            badge = "${chars.size} Cast"
                        )
                    }
                    items(chars, key = { it.id }) { character ->
                        val imageState = characterImages[character.id]
                            ?: cachedImageLookup(character.id)?.let { ImageRenderState.Success(it, it.lastModified()) }
                            ?: ImageRenderState.Idle

                        CharacterCard(
                            character = character,
                            imageState = imageState,
                            onEdit = { editingCharacter = character to false },
                            onDelete = { onDeleteCharacter(character.id) },
                            onGeneratePortrait = { onGeneratePortrait(character) },
                            onCopiedNotice = onCopiedNotice
                        )
                    }
                }
            }
        }
    }

    editingCharacter?.let { (char, isNew) ->
        CharacterEditorDialog(
            initial = char,
            isNew = isNew,
            onDismiss = { editingCharacter = null },
            onSave = { updated ->
                onSaveCharacter(updated, isNew)
                editingCharacter = null
            }
        )
    }
}

@Composable
private fun CharacterCard(
    character: CharacterProfile,
    imageState: ImageRenderState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onGeneratePortrait: () -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    StudioCard(modifier = Modifier.testTag("character_card_${character.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (character.role.isNotBlank()) {
                    Text(
                        text = character.role.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit character", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete character", tint = MaterialTheme.colorScheme.error)
            }
        }

        // Reference Portrait Area (Nano Banana 2.1)
        Spacer(Modifier.height(10.dp))
        when (imageState) {
            is ImageRenderState.Success -> {
                LocalGeneratedImage(
                    file = imageState.file,
                    version = imageState.version,
                    contentDescription = "${character.name} AI reference portrait",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.small)
                )
            }
            is ImageRenderState.Loading -> {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Rendering AI reference portrait (30–60s)…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is ImageRenderState.Error -> {
                AiStatusBanner(state = AiTaskState.Error(imageState.message))
            }
            is ImageRenderState.Idle -> Unit
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onGeneratePortrait,
            enabled = imageState !is ImageRenderState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("character_render_portrait_${character.id}"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (imageState is ImageRenderState.Success) "Regenerate AI Reference Portrait"
                else "Generate AI Reference Portrait"
            )
        }

        Spacer(Modifier.height(12.dp))
        if (character.description.isNotBlank()) {
            CharacterSpecLine("Appearance", character.description)
        }
        if (character.clothing.isNotBlank()) {
            CharacterSpecLine("Wardrobe", character.clothing)
        }
        if (character.hairstyle.isNotBlank()) {
            CharacterSpecLine("Hairstyle", character.hairstyle)
        }
        if (character.expressions.isNotBlank()) {
            CharacterSpecLine("Expressions", character.expressions)
        }
        if (character.voiceTone.isNotBlank()) {
            CharacterSpecLine("Voice Casting", character.voiceTone)
        }

        if (character.consistencyToken.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            CopyablePromptBlock(
                label = "Consistency Lock Prompt",
                text = character.consistencyToken,
                accentColor = MaterialTheme.colorScheme.tertiary,
                onCopied = { onCopiedNotice("Character consistency prompt copied") }
            )
        }
    }
}

@Composable
private fun CharacterSpecLine(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CharacterEditorDialog(
    initial: CharacterProfile,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (CharacterProfile) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var role by remember { mutableStateOf(initial.role) }
    var description by remember { mutableStateOf(initial.description) }
    var clothing by remember { mutableStateOf(initial.clothing) }
    var hairstyle by remember { mutableStateOf(initial.hairstyle) }
    var expressions by remember { mutableStateOf(initial.expressions) }
    var consistencyToken by remember { mutableStateOf(initial.consistencyToken) }
    var voiceTone by remember { mutableStateOf(initial.voiceTone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New Character Bible" else "Edit ${initial.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(100) }, label = { Text("Character Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = role, onValueChange = { role = it.take(100) }, label = { Text("Role (Protagonist, Rival, Mentor)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it.take(2000) }, label = { Text("Face & Bio Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = clothing, onValueChange = { clothing = it.take(600) }, label = { Text("Signature Outfit") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = hairstyle, onValueChange = { hairstyle = it.take(400) }, label = { Text("Hairstyle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = expressions, onValueChange = { expressions = it.take(600) }, label = { Text("Key Expressions") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = voiceTone, onValueChange = { voiceTone = it.take(200) }, label = { Text("Hindi Voice Tone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = consistencyToken, onValueChange = { consistencyToken = it.take(1500) }, label = { Text("Consistency Prompt Anchor (EN)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.ifBlank { "Unnamed Character" },
                            role = role,
                            description = description,
                            clothing = clothing,
                            hairstyle = hairstyle,
                            expressions = expressions,
                            consistencyToken = consistencyToken,
                            voiceTone = voiceTone
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
