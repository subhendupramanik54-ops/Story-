package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.ai.BharatAiService
import com.example.data.ai.VoiceProductionPack
import com.example.data.model.AiTaskState
import com.example.data.model.CharacterProfile
import com.example.data.model.ConnectionHealth
import com.example.data.model.SavedPrompt
import com.example.data.model.StoryboardProject
import com.example.data.model.StoryboardScene
import com.example.data.model.UiState
import com.example.data.repository.StoryboardRepository
import com.example.data.repository.StudioPreferences
import com.example.data.repository.StudioPreferencesRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "StudioViewModel"
private const val STOP_TIMEOUT_MILLIS = 5_000L

sealed interface ImageRenderState {
    data object Idle : ImageRenderState
    data object Loading : ImageRenderState
    data class Success(val file: File, val version: Long = System.currentTimeMillis()) : ImageRenderState
    data class Error(val message: String) : ImageRenderState
}

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(
    private val repository: StoryboardRepository,
    private val aiService: BharatAiService,
    private val preferencesRepository: StudioPreferencesRepository,
    private val currentUserId: String
) : ViewModel() {

    val preferences: StateFlow<StudioPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StudioPreferences())

    val projectsState: StateFlow<UiState<List<StoryboardProject>>> =
        repository.observeProjects(currentUserId)
            .toUiStateFlow("Failed to load projects")

    val charactersState: StateFlow<UiState<List<CharacterProfile>>> =
        repository.observeCharacters(currentUserId)
            .toUiStateFlow("Failed to load character bible")

    val promptHistoryState: StateFlow<UiState<List<SavedPrompt>>> =
        repository.observePromptHistory(currentUserId)
            .toUiStateFlow("Failed to load saved prompts")

    private val _selectedProjectId = MutableStateFlow<String?>(null)

    val activeProject: StateFlow<StoryboardProject?> = combine(
        projectsState,
        _selectedProjectId
    ) { projState, selectedId ->
        val list = (projState as? UiState.Success)?.data.orEmpty()
        list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val activeScenesState: StateFlow<UiState<List<StoryboardScene>>> = activeProject
        .map { it?.id }
        .flatMapLatest { projectId ->
            if (projectId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeScenes(currentUserId, projectId)
                    .toUiStateFlow("Failed to load storyboard scenes")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private val _storyTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val storyTaskState: StateFlow<AiTaskState> = _storyTaskState.asStateFlow()

    private val _storyDraftText = MutableStateFlow("")
    val storyDraftText: StateFlow<String> = _storyDraftText.asStateFlow()

    private val _storyboardTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val storyboardTaskState: StateFlow<AiTaskState> = _storyboardTaskState.asStateFlow()

    private val _regeneratingSceneId = MutableStateFlow<String?>(null)
    val regeneratingSceneId: StateFlow<String?> = _regeneratingSceneId.asStateFlow()

    private val _characterTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val characterTaskState: StateFlow<AiTaskState> = _characterTaskState.asStateFlow()

    private val _characterImages = MutableStateFlow<Map<String, ImageRenderState>>(emptyMap())
    val characterImages: StateFlow<Map<String, ImageRenderState>> = _characterImages.asStateFlow()

    private val _imageStudioTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val imageStudioTaskState: StateFlow<AiTaskState> = _imageStudioTaskState.asStateFlow()

    private val _generatedImagePrompt = MutableStateFlow("")
    val generatedImagePrompt: StateFlow<String> = _generatedImagePrompt.asStateFlow()

    private val _studioPreviewImageState = MutableStateFlow<ImageRenderState>(ImageRenderState.Idle)
    val studioPreviewImageState: StateFlow<ImageRenderState> = _studioPreviewImageState.asStateFlow()

    private val _videoStudioTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val videoStudioTaskState: StateFlow<AiTaskState> = _videoStudioTaskState.asStateFlow()

    private val _generatedVideoPrompt = MutableStateFlow("")
    val generatedVideoPrompt: StateFlow<String> = _generatedVideoPrompt.asStateFlow()

    private val _voiceStudioTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val voiceStudioTaskState: StateFlow<AiTaskState> = _voiceStudioTaskState.asStateFlow()

    private val _generatedVoicePack = MutableStateFlow<VoiceProductionPack?>(null)
    val generatedVoicePack: StateFlow<VoiceProductionPack?> = _generatedVoicePack.asStateFlow()

    private val _connectionHealth = MutableStateFlow(ConnectionHealth())
    val connectionHealth: StateFlow<ConnectionHealth> = _connectionHealth.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private fun <T> Flow<T>.toUiStateFlow(fallbackMessage: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMessage, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackMessage))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private fun emitMessage(msg: String) {
        _snackbarEvents.tryEmit(msg)
    }

    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
    }

    // --- Preferences & Live Connection Status ---

    fun updateSelectedModel(modelId: String) {
        viewModelScope.launch {
            preferencesRepository.setSelectedModel(modelId)
            _connectionHealth.update { it.copy(modelName = modelId) }
            emitMessage("Active AI model set to $modelId")
        }
    }

    fun updateDefaultLanguage(language: String) {
        viewModelScope.launch { preferencesRepository.setDefaultLanguage(language) }
    }

    fun updateDefaultArtStyle(style: String) {
        viewModelScope.launch { preferencesRepository.setDefaultArtStyle(style) }
    }

    fun updateDefaultAspectRatio(ratio: String) {
        viewModelScope.launch { preferencesRepository.setDefaultAspectRatio(ratio) }
    }

    fun testAiConnection() {
        val modelId = preferences.value.selectedModelId
        viewModelScope.launch {
            _connectionHealth.value = ConnectionHealth(
                isTesting = true,
                modelName = modelId,
                statusDetail = "Testing live Firebase AI Logic connection…"
            )
            val result = aiService.testConnection(modelId)
            val timestamp = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            result.onSuccess { ping ->
                _connectionHealth.value = ConnectionHealth(
                    isTesting = false,
                    isConnected = true,
                    latencyMs = ping.latencyMs,
                    modelName = ping.modelName,
                    statusDetail = "Connected via Firebase AI Logic (${ping.latencyMs} ms)",
                    lastCheckedAt = timestamp
                )
                emitMessage("Gemini connected (${ping.latencyMs} ms)")
            }.onFailure { err ->
                _connectionHealth.value = ConnectionHealth(
                    isTesting = false,
                    isConnected = false,
                    modelName = modelId,
                    statusDetail = err.localizedMessage ?: "Connection test failed",
                    lastCheckedAt = timestamp
                )
            }
        }
    }

    // --- 1. AI Story Generator & Continuation ---

    fun updateStoryDraft(text: String) {
        _storyDraftText.value = text
    }

    fun generateNewStory(
        premise: String,
        language: String,
        genre: String,
        artStyle: String,
        totalEpisodes: Int
    ) {
        if (premise.isBlank()) {
            _storyTaskState.value = AiTaskState.Error("Please enter a story idea or premise first.")
            return
        }
        val modelId = preferences.value.selectedModelId
        viewModelScope.launch {
            _storyTaskState.value = AiTaskState.Loading("Writing Episode 1 in $language…")
            _storyDraftText.value = ""
            try {
                var finalStory = ""
                aiService.streamStory(
                    modelId = modelId,
                    premise = premise.trim(),
                    language = language,
                    genre = genre,
                    artStyle = artStyle,
                    totalEpisodes = totalEpisodes.coerceIn(1, 60),
                    episodeNumber = 1,
                    previousStoryContext = null
                ).collect { partial ->
                    finalStory = partial
                    _storyDraftText.value = partial
                    _storyTaskState.value = AiTaskState.Streaming(partial)
                }
                _storyTaskState.value = AiTaskState.Success("Episode 1 generated successfully.")
                emitMessage("Story Episode 1 ready!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Story generation failed", e)
                _storyTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Failed to generate story.")
            }
        }
    }

    fun continueNextEpisode(
        project: StoryboardProject,
        customTwist: String = ""
    ) {
        val nextEp = (project.currentEpisode + 1L).coerceAtMost(project.totalEpisodes.coerceIn(1L, 60L))
        if (project.currentEpisode >= project.totalEpisodes && project.totalEpisodes >= 60L) {
            emitMessage("Reached maximum 60 episodes for this project.")
            return
        }
        val targetEpisode = if (project.currentEpisode >= project.totalEpisodes) {
            (project.currentEpisode + 1L).coerceAtMost(60L)
        } else {
            nextEp
        }
        val newTotal = maxOf(project.totalEpisodes, targetEpisode)
        val modelId = preferences.value.selectedModelId
        val premise = buildString {
            append(project.logline.ifBlank { project.title })
            if (customTwist.isNotBlank()) append(" | Next episode twist: $customTwist")
        }

        viewModelScope.launch {
            _storyTaskState.value = AiTaskState.Loading("Continuing Episode $targetEpisode in ${project.language}…")
            try {
                var newEpisodeText = ""
                aiService.streamStory(
                    modelId = modelId,
                    premise = premise,
                    language = project.language,
                    genre = project.genre,
                    artStyle = project.artStyle,
                    totalEpisodes = newTotal.toInt(),
                    episodeNumber = targetEpisode.toInt(),
                    previousStoryContext = project.storyText
                ).collect { partial ->
                    newEpisodeText = partial
                    _storyDraftText.value = partial
                    _storyTaskState.value = AiTaskState.Streaming(partial)
                }
                val combinedStory = buildString {
                    if (project.storyText.isNotBlank()) {
                        append(project.storyText.trimEnd())
                        append("\n\n--- EPISODE $targetEpisode ---\n\n")
                    }
                    append(newEpisodeText)
                }.take(25000)

                val saveResult = repository.updateProject(
                    userId = currentUserId,
                    projectId = project.id,
                    storyText = combinedStory,
                    currentEpisode = targetEpisode,
                    totalEpisodes = newTotal
                )
                saveResult.onSuccess {
                    _storyTaskState.value = AiTaskState.Success("Episode $targetEpisode added and saved to cloud.")
                    emitMessage("Episode $targetEpisode saved!")
                }.onFailure { err ->
                    _storyTaskState.value = AiTaskState.Error(err.localizedMessage ?: "Failed to save continuation.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Story continuation failed", e)
                _storyTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Failed to continue story.")
            }
        }
    }

    fun saveStoryAsNewProject(
        title: String,
        logline: String,
        language: String,
        genre: String,
        artStyle: String,
        aspectRatio: String,
        totalEpisodes: Int,
        storyText: String,
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val project = StoryboardProject(
                title = title.ifBlank { "$genre Story ($language)" },
                logline = logline,
                language = language,
                genre = genre,
                artStyle = artStyle,
                aspectRatio = aspectRatio,
                totalEpisodes = totalEpisodes.toLong().coerceIn(1L, 60L),
                currentEpisode = 1L,
                storyText = storyText,
                status = "DRAFT"
            )
            repository.createProject(currentUserId, project)
                .onSuccess { newId ->
                    _selectedProjectId.value = newId
                    emitMessage("Project \"${project.title}\" saved to cloud")
                    onSaved(newId)
                }
                .onFailure { err ->
                    _storyTaskState.value = AiTaskState.Error(err.localizedMessage ?: "Could not save project.")
                }
        }
    }

    // --- 2. Project CRUD ---

    fun updateProjectMetadata(
        projectId: String,
        title: String,
        logline: String,
        language: String,
        genre: String,
        artStyle: String,
        aspectRatio: String,
        totalEpisodes: Long,
        status: String
    ) {
        viewModelScope.launch {
            repository.updateProject(
                userId = currentUserId,
                projectId = projectId,
                title = title,
                logline = logline,
                language = language,
                genre = genre,
                artStyle = artStyle,
                aspectRatio = aspectRatio,
                totalEpisodes = totalEpisodes,
                status = status
            ).onSuccess {
                emitMessage("Project updated")
            }.onFailure { err ->
                emitMessage("Update failed: ${err.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(currentUserId, projectId)
                .onSuccess {
                    if (_selectedProjectId.value == projectId) {
                        _selectedProjectId.value = null
                    }
                    emitMessage("Project deleted")
                }
                .onFailure { err ->
                    emitMessage("Delete failed: ${err.localizedMessage ?: "Unknown error"}")
                }
        }
    }

    // --- 3. Storyboard Studio ---

    fun generateStoryboardForActiveProject(sceneCount: Int = 5) {
        val project = activeProject.value
        if (project == null) {
            _storyboardTaskState.value = AiTaskState.Error("Create or select a project first.")
            return
        }
        val existingScenes = (activeScenesState.value as? UiState.Success)?.data.orEmpty()
        val nextStartNumber = (existingScenes.maxOfOrNull { it.sceneNumber } ?: 0L).toInt() + 1
        val characters = (charactersState.value as? UiState.Success)?.data.orEmpty()
            .filter { it.projectId.isBlank() || it.projectId == project.id }
        val modelId = preferences.value.selectedModelId

        viewModelScope.launch {
            _storyboardTaskState.value = AiTaskState.Loading("Breaking story into $sceneCount cinematic scenes…")
            aiService.generateStoryboardScenes(
                modelId = modelId,
                project = project,
                characters = characters,
                sceneCount = sceneCount,
                startingSceneNumber = nextStartNumber
            ).onSuccess { generatedScenes ->
                repository.addScenesBatch(currentUserId, project.id, generatedScenes)
                    .onSuccess { count ->
                        _storyboardTaskState.value = AiTaskState.Success("Generated $count scenes for ${project.title}.")
                        emitMessage("$count storyboard scenes added!")
                    }
                    .onFailure { err ->
                        _storyboardTaskState.value = AiTaskState.Error(err.localizedMessage ?: "Failed to save scenes.")
                    }
            }.onFailure { err ->
                _storyboardTaskState.value = AiTaskState.Error(err.localizedMessage ?: "Storyboard generation failed.")
            }
        }
    }

    fun regenerateScene(scene: StoryboardScene, directorNotes: String = "") {
        val project = activeProject.value ?: return
        val characters = (charactersState.value as? UiState.Success)?.data.orEmpty()
            .filter { it.projectId.isBlank() || it.projectId == project.id }
        val modelId = preferences.value.selectedModelId

        viewModelScope.launch {
            _regeneratingSceneId.value = scene.id
            _storyboardTaskState.value = AiTaskState.Loading("Regenerating Scene ${scene.sceneNumber}…")
            aiService.regenerateSingleScene(modelId, project, scene, characters, directorNotes)
                .onSuccess { revised ->
                    repository.updateScene(currentUserId, project.id, revised)
                        .onSuccess {
                            _storyboardTaskState.value = AiTaskState.Success("Scene ${scene.sceneNumber} updated by Gemini.")
                            emitMessage("Scene ${scene.sceneNumber} regenerated!")
                        }
                        .onFailure { err ->
                            _storyboardTaskState.value = AiTaskState.Error(err.localizedMessage ?: "Failed to save regenerated scene.")
                        }
                }
                .onFailure { err ->
                    _storyboardTaskState.value = AiTaskState.Error(err.localizedMessage ?: "Regeneration failed.")
                }
            _regeneratingSceneId.value = null
        }
    }

    fun saveScene(scene: StoryboardScene, isNew: Boolean) {
        val projectId = activeProject.value?.id ?: scene.projectId
        if (projectId.isBlank()) {
            emitMessage("Select a project first")
            return
        }
        viewModelScope.launch {
            val result = if (isNew) {
                repository.addScene(currentUserId, projectId, scene).map { Unit }
            } else {
                repository.updateScene(currentUserId, projectId, scene)
            }
            result.onSuccess {
                emitMessage(if (isNew) "Scene added" else "Scene saved")
            }.onFailure { err ->
                emitMessage("Scene save failed: ${err.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun duplicateScene(scene: StoryboardScene) {
        val projectId = activeProject.value?.id ?: scene.projectId
        if (projectId.isBlank()) return
        val existingScenes = (activeScenesState.value as? UiState.Success)?.data.orEmpty()
        val nextNum = ((existingScenes.maxOfOrNull { it.sceneNumber } ?: scene.sceneNumber) + 1L).coerceAtMost(200L)
        val copy = scene.copy(
            id = "",
            sceneNumber = nextNum,
            title = "${scene.title.take(138)} (Copy)",
            createdAt = null,
            updatedAt = null
        )
        viewModelScope.launch {
            repository.addScene(currentUserId, projectId, copy)
                .onSuccess { emitMessage("Scene duplicated as Scene $nextNum") }
                .onFailure { err -> emitMessage("Duplicate failed: ${err.localizedMessage ?: "Error"}") }
        }
    }

    fun deleteScene(scene: StoryboardScene) {
        val projectId = activeProject.value?.id ?: scene.projectId
        if (projectId.isBlank() || scene.id.isBlank()) return
        viewModelScope.launch {
            repository.deleteScene(currentUserId, projectId, scene.id)
                .onSuccess { emitMessage("Scene deleted") }
                .onFailure { err -> emitMessage("Delete failed: ${err.localizedMessage ?: "Error"}") }
        }
    }

    // --- 4. Character Studio ---

    fun generateCharactersWithAi(customConcept: String = "") {
        val project = activeProject.value
        val modelId = preferences.value.selectedModelId
        viewModelScope.launch {
            _characterTaskState.value = AiTaskState.Loading("Building Character Bible with Gemini…")
            aiService.generateCharacterBible(modelId, project, customConcept)
                .onSuccess { generated ->
                    repository.addCharactersBatch(currentUserId, generated)
                        .onSuccess { count ->
                            _characterTaskState.value = AiTaskState.Success("Created $count Character Bible profiles.")
                            emitMessage("$count characters added to Character Bible!")
                        }
                        .onFailure { err ->
                            _characterTaskState.value = AiTaskState.Error(err.localizedMessage ?: "Failed to save characters.")
                        }
                }
                .onFailure { err ->
                    _characterTaskState.value = AiTaskState.Error(err.localizedMessage ?: "Character generation failed.")
                }
        }
    }

    fun saveCharacter(character: CharacterProfile, isNew: Boolean) {
        viewModelScope.launch {
            val result = if (isNew) {
                repository.addCharacter(currentUserId, character).map { Unit }
            } else {
                repository.updateCharacter(currentUserId, character)
            }
            result.onSuccess {
                emitMessage(if (isNew) "Character saved" else "Character updated")
            }.onFailure { err ->
                emitMessage("Character save failed: ${err.localizedMessage ?: "Error"}")
            }
        }
    }

    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            repository.deleteCharacter(currentUserId, characterId)
                .onSuccess { emitMessage("Character deleted") }
                .onFailure { err -> emitMessage("Delete failed: ${err.localizedMessage ?: "Error"}") }
        }
    }

    fun cachedCharacterImage(characterId: String): File? =
        aiService.getCachedImageFile("char_$characterId")

    fun generateCharacterPortrait(character: CharacterProfile) {
        val style = activeProject.value?.artStyle ?: preferences.value.defaultArtStyle
        val prompt = buildString {
            append("Character concept reference portrait in $style style for Indian cinema storyboard. ")
            append("Character: ${character.name} (${character.role}). ")
            if (character.consistencyToken.isNotBlank()) {
                append(character.consistencyToken).append(". ")
            } else {
                append("${character.description}. Outfit: ${character.clothing}. Hairstyle: ${character.hairstyle}. ")
            }
            append("Expressions: ${character.expressions}. Studio dark navy rim lighting, sharp facial details, high contrast, clean turnaround portrait, no text.")
        }

        viewModelScope.launch {
            _characterImages.update { it + (character.id to ImageRenderState.Loading) }
            aiService.generateAndCacheImage(prompt, "char_${character.id}")
                .onSuccess { file ->
                    _characterImages.update { it + (character.id to ImageRenderState.Success(file)) }
                    emitMessage("Reference portrait ready for ${character.name}")
                }
                .onFailure { err ->
                    _characterImages.update {
                        it + (character.id to ImageRenderState.Error(err.localizedMessage ?: "Portrait generation failed."))
                    }
                }
        }
    }

    // --- 5. Image Prompt Studio ---

    fun generateImagePrompt(
        sceneIdea: String,
        artStyle: String,
        aspectRatio: String,
        selectedCharacterIds: Set<String>
    ) {
        if (sceneIdea.isBlank()) {
            _imageStudioTaskState.value = AiTaskState.Error("Describe your scene concept first.")
            return
        }
        val characters = (charactersState.value as? UiState.Success)?.data.orEmpty()
            .filter { it.id in selectedCharacterIds }
        val modelId = preferences.value.selectedModelId

        viewModelScope.launch {
            _imageStudioTaskState.value = AiTaskState.Loading("Crafting $artStyle ($aspectRatio) English image prompt…")
            aiService.generateDetailedImagePrompt(modelId, sceneIdea, artStyle, aspectRatio, characters)
                .onSuccess { promptText ->
                    _generatedImagePrompt.value = promptText
                    _imageStudioTaskState.value = AiTaskState.Success("Image prompt generated.")
                }
                .onFailure { err ->
                    _imageStudioTaskState.value = AiTaskState.Error(err.localizedMessage ?: "Image prompt generation failed.")
                }
        }
    }

    fun renderImageStudioPreview(promptText: String, artStyle: String, aspectRatio: String) {
        if (promptText.isBlank()) return
        val fullPrompt = "$promptText. Composition framed for $aspectRatio aspect ratio, $artStyle style, no watermark, no text."
        viewModelScope.launch {
            _studioPreviewImageState.value = ImageRenderState.Loading
            aiService.generateAndCacheImage(fullPrompt, "studio_preview_latest")
                .onSuccess { file ->
                    _studioPreviewImageState.value = ImageRenderState.Success(file)
                    emitMessage("Preview image rendered!")
                }
                .onFailure { err ->
                    _studioPreviewImageState.value = ImageRenderState.Error(
                        err.localizedMessage ?: "Image rendering failed."
                    )
                }
        }
    }

    // --- 6. Google Flow Video Prompt Studio ---

    fun generateVideoPrompt(
        sceneAction: String,
        cameraMovement: String,
        durationSeconds: Long,
        aspectRatio: String,
        artStyle: String,
        selectedCharacterIds: Set<String>
    ) {
        if (sceneAction.isBlank()) {
            _videoStudioTaskState.value = AiTaskState.Error("Describe the shot action or keyframe motion first.")
            return
        }
        val characters = (charactersState.value as? UiState.Success)?.data.orEmpty()
            .filter { it.id in selectedCharacterIds }
        val modelId = preferences.value.selectedModelId

        viewModelScope.launch {
            _videoStudioTaskState.value = AiTaskState.Loading("Composing Google Flow Image-to-Video prompt…")
            aiService.generateGoogleFlowVideoPrompt(
                modelId = modelId,
                sceneAction = sceneAction,
                cameraMovement = cameraMovement,
                durationSeconds = durationSeconds,
                aspectRatio = aspectRatio,
                artStyle = artStyle,
                selectedCharacters = characters
            ).onSuccess { promptText ->
                _generatedVideoPrompt.value = promptText
                _videoStudioTaskState.value = AiTaskState.Success("Google Flow video prompt ready.")
            }.onFailure { err ->
                _videoStudioTaskState.value = AiTaskState.Error(err.localizedMessage ?: "Video prompt generation failed.")
            }
        }
    }

    fun savePromptToHistory(
        promptType: String,
        title: String,
        promptText: String,
        aspectRatio: String,
        artStyle: String,
        durationSeconds: Long = 0L
    ) {
        if (promptText.isBlank()) return
        viewModelScope.launch {
            val saved = SavedPrompt(
                projectId = activeProject.value?.id.orEmpty(),
                promptType = promptType,
                title = title.ifBlank { "$promptType Prompt" },
                promptText = promptText,
                aspectRatio = aspectRatio,
                artStyle = artStyle,
                durationSeconds = durationSeconds
            )
            repository.addSavedPrompt(currentUserId, saved)
                .onSuccess { emitMessage("Saved to Studio Prompt History") }
                .onFailure { err -> emitMessage("Save failed: ${err.localizedMessage ?: "Error"}") }
        }
    }

    fun deleteSavedPrompt(promptId: String) {
        viewModelScope.launch {
            repository.deleteSavedPrompt(currentUserId, promptId)
                .onSuccess { emitMessage("Prompt removed") }
                .onFailure { err -> emitMessage("Delete failed: ${err.localizedMessage ?: "Error"}") }
        }
    }

    // --- 7. Voice & Audio Studio ---

    fun generateVoicePack(topicOrScene: String, tone: String) {
        val fallbackTopic = topicOrScene.ifBlank {
            activeProject.value?.let { "${it.title}: ${it.storyText.take(1500).ifBlank { it.logline }}" }.orEmpty()
        }
        if (fallbackTopic.isBlank()) {
            _voiceStudioTaskState.value = AiTaskState.Error("Enter a scene or select a project with a story first.")
            return
        }
        val characters = (charactersState.value as? UiState.Success)?.data.orEmpty()
            .filter { it.projectId.isBlank() || it.projectId == activeProject.value?.id }
        val modelId = preferences.value.selectedModelId

        viewModelScope.launch {
            _voiceStudioTaskState.value = AiTaskState.Loading("Generating Hindi dialogues, subtitles, BGM & SFX…")
            aiService.generateVoiceProductionPack(modelId, fallbackTopic, characters, tone)
                .onSuccess { pack ->
                    _generatedVoicePack.value = pack
                    _voiceStudioTaskState.value = AiTaskState.Success("Hindi voice & audio pack ready.")
                }
                .onFailure { err ->
                    _voiceStudioTaskState.value = AiTaskState.Error(err.localizedMessage ?: "Voice script generation failed.")
                }
        }
    }

    companion object {
        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudioViewModel(
                    repository = StoryboardRepository(db),
                    aiService = BharatAiService(app),
                    preferencesRepository = StudioPreferencesRepository(app),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
