package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class StoryboardProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val logline: String = "",
    val language: String = "Hindi",
    val genre: String = "Anime",
    val artStyle: String = "Anime",
    val aspectRatio: String = "9:16",
    val totalEpisodes: Long = 1L,
    val currentEpisode: Long = 1L,
    val storyText: String = "",
    val status: String = "DRAFT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryboardScene(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val episodeNumber: Long = 1L,
    val sceneNumber: Long = 1L,
    val title: String = "",
    val description: String = "",
    val cameraAngle: String = "",
    val cameraMovement: String = "",
    val characterAction: String = "",
    val durationSeconds: Long = 6L,
    val imagePrompt: String = "",
    val videoPrompt: String = "",
    val consistencyNotes: String = "",
    val dialogueCharacter: String = "",
    val hindiDialogue: String = "",
    val voiceOverScript: String = "",
    val subtitleText: String = "",
    val bgmPrompt: String = "",
    val sfxPrompt: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CharacterProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val name: String = "",
    val role: String = "",
    val description: String = "",
    val clothing: String = "",
    val hairstyle: String = "",
    val expressions: String = "",
    val consistencyToken: String = "",
    val voiceTone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedPrompt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val promptType: String = "IMAGE",
    val title: String = "",
    val promptText: String = "",
    val aspectRatio: String = "9:16",
    val artStyle: String = "Anime",
    val durationSeconds: Long = 6L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class BottomNavTab(val route: String) {
    HOME("home"),
    STORIES("stories"),
    STUDIO("studio"),
    PROJECTS("projects"),
    SETTINGS("settings")
}

enum class StudioSubTab(val label: String) {
    STORYBOARD("Storyboard"),
    CHARACTERS("Characters"),
    IMAGE_PROMPT("Image AI"),
    VIDEO_PROMPT("Flow Video"),
    VOICE_AUDIO("Voice & SFX")
}

object StudioOptions {
    val languages = listOf("Hindi", "Gujarati", "Bengali", "English")
    val genres = listOf("Anime", "Romance", "Fantasy", "Horror", "Mystery", "Drama")
    val artStyles = listOf("Anime", "Realistic 3D", "Cinematic")
    val aspectRatios = listOf("9:16", "16:9", "1:1")
    val projectStatuses = listOf("DRAFT", "IN_PRODUCTION", "COMPLETED")
    val videoDurations = listOf(4L, 6L, 8L, 12L)
    val cameraMovements = listOf(
        "Slow Dolly Push-In",
        "Dynamic Orbit 360°",
        "Drone Crane Reveal",
        "Handheld Action Tracking",
        "Low-Angle Hero Tilt Up",
        "Whip Pan Transition",
        "Static Dramatic Close-Up"
    )
    val geminiModels = listOf(
        AiModelSpec(
            id = "gemini-3.8-flash",
            label = "Gemini 3.8 Flash",
            subtitle = "Recommended • Balanced quality & speed"
        ),
        AiModelSpec(
            id = "gemini-3.5-flash-lite",
            label = "Gemini 3.5 Flash-Lite",
            subtitle = "Ultra-fast • High-volume prompting"
        )
    )
}

data class AiModelSpec(
    val id: String,
    val label: String,
    val subtitle: String
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data class Loading(val statusMessage: String) : AiTaskState
    data class Streaming(val partialText: String) : AiTaskState
    data class Success(val message: String) : AiTaskState
    data class Error(val message: String) : AiTaskState
}

data class ConnectionHealth(
    val isTesting: Boolean = false,
    val isConnected: Boolean? = null,
    val latencyMs: Long? = null,
    val modelName: String = "gemini-3.8-flash",
    val statusDetail: String = "Tap Test Live Connection to verify Firebase AI Logic",
    val lastCheckedAt: String = ""
)
