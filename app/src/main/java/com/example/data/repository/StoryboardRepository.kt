package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CharacterProfile
import com.example.data.model.SavedPrompt
import com.example.data.model.StoryboardProject
import com.example.data.model.StoryboardScene
import com.example.data.model.StudioOptions
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StoryboardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google before accessing cloud projects.")
        check(uid == expectedUserId) { "Signed-in account changed; refusing cross-user access." }
        return uid
    }

    private fun projectsCol(userId: String) =
        db.collection("users").document(userId).collection("projects")

    private fun scenesCol(userId: String, projectId: String) =
        projectsCol(userId).document(projectId).collection("scenes")

    private fun charactersCol(userId: String) =
        db.collection("users").document(userId).collection("characters")

    private fun promptsCol(userId: String) =
        db.collection("users").document(userId).collection("promptHistory")

    // --- Real-time Observations ---

    fun observeProjects(userId: String): Flow<List<StoryboardProject>> {
        val path = "users/$userId/projects"
        return projectsCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StoryboardProject::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamErrorIfAuthenticated(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeScenes(userId: String, projectId: String): Flow<List<StoryboardScene>> {
        val path = "users/$userId/projects/$projectId/scenes"
        return scenesCol(userId, projectId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StoryboardScene::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareBy<StoryboardScene> { it.episodeNumber }.thenBy { it.sceneNumber })
            }
            .catch { error ->
                logStreamErrorIfAuthenticated(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeCharacters(userId: String): Flow<List<CharacterProfile>> {
        val path = "users/$userId/characters"
        return charactersCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CharacterProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamErrorIfAuthenticated(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observePromptHistory(userId: String): Flow<List<SavedPrompt>> {
        val path = "users/$userId/promptHistory"
        return promptsCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SavedPrompt::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamErrorIfAuthenticated(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    private fun logStreamErrorIfAuthenticated(
        error: Throwable,
        operation: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operation, path)
        }
    }

    // --- Project CRUD ---

    suspend fun createProject(userId: String, project: StoryboardProject): Result<String> {
        var docPath = "users/$userId/projects"
        return try {
            val uid = requireUserId(userId)
            val docRef = projectsCol(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to project.title.trim().ifBlank { "Untitled Storyboard" }.take(120),
                "logline" to project.logline.trim().take(500),
                "language" to sanitizeEnum(project.language, StudioOptions.languages, "Hindi"),
                "genre" to sanitizeEnum(project.genre, StudioOptions.genres, "Anime"),
                "artStyle" to sanitizeEnum(project.artStyle, StudioOptions.artStyles, "Anime"),
                "aspectRatio" to sanitizeEnum(project.aspectRatio, StudioOptions.aspectRatios, "9:16"),
                "totalEpisodes" to project.totalEpisodes.coerceIn(1L, 60L),
                "currentEpisode" to project.currentEpisode.coerceIn(1L, 60L),
                "storyText" to project.storyText.take(25000),
                "status" to sanitizeEnum(project.status, StudioOptions.projectStatuses, "DRAFT"),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProject(
        userId: String,
        projectId: String,
        title: String? = null,
        logline: String? = null,
        language: String? = null,
        genre: String? = null,
        artStyle: String? = null,
        aspectRatio: String? = null,
        totalEpisodes: Long? = null,
        currentEpisode: Long? = null,
        storyText: String? = null,
        status: String? = null
    ): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId(userId)
            require(projectId.isNotBlank()) { "Project ID cannot be blank" }
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            title?.let { updates["title"] = it.trim().ifBlank { "Untitled Storyboard" }.take(120) }
            logline?.let { updates["logline"] = it.trim().take(500) }
            language?.let { updates["language"] = sanitizeEnum(it, StudioOptions.languages, "Hindi") }
            genre?.let { updates["genre"] = sanitizeEnum(it, StudioOptions.genres, "Anime") }
            artStyle?.let { updates["artStyle"] = sanitizeEnum(it, StudioOptions.artStyles, "Anime") }
            aspectRatio?.let { updates["aspectRatio"] = sanitizeEnum(it, StudioOptions.aspectRatios, "9:16") }
            totalEpisodes?.let { updates["totalEpisodes"] = it.coerceIn(1L, 60L) }
            currentEpisode?.let { updates["currentEpisode"] = it.coerceIn(1L, 60L) }
            storyText?.let { updates["storyText"] = it.take(25000) }
            status?.let { updates["status"] = sanitizeEnum(it, StudioOptions.projectStatuses, "DRAFT") }

            projectsCol(uid).document(projectId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(userId: String, projectId: String): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId(userId)
            require(projectId.isNotBlank()) { "Project ID cannot be blank" }
            val existingScenes = scenesCol(uid, projectId).get().await()
            val batch = db.batch()
            existingScenes.documents.forEach { batch.delete(it.reference) }
            batch.delete(projectsCol(uid).document(projectId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Scene CRUD ---

    suspend fun addScene(userId: String, projectId: String, scene: StoryboardScene): Result<String> {
        var docPath = "users/$userId/projects/$projectId/scenes"
        return try {
            val uid = requireUserId(userId)
            require(projectId.isNotBlank()) { "Project ID cannot be blank" }
            val docRef = scenesCol(uid, projectId).document()
            docPath = docRef.path
            docRef.set(buildSceneCreateMap(uid, projectId, scene)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addScenesBatch(
        userId: String,
        projectId: String,
        scenes: List<StoryboardScene>
    ): Result<Int> {
        val colPath = "users/$userId/projects/$projectId/scenes"
        return try {
            val uid = requireUserId(userId)
            require(projectId.isNotBlank()) { "Project ID cannot be blank" }
            if (scenes.isEmpty()) return Result.success(0)
            val batch = db.batch()
            scenes.forEach { scene ->
                val ref = scenesCol(uid, projectId).document()
                batch.set(ref, buildSceneCreateMap(uid, projectId, scene))
            }
            batch.update(
                projectsCol(uid).document(projectId),
                mapOf(
                    "status" to "IN_PRODUCTION",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(scenes.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colPath)
            Result.failure(e)
        }
    }

    suspend fun updateScene(userId: String, projectId: String, scene: StoryboardScene): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId/scenes/${scene.id}"
        return try {
            val uid = requireUserId(userId)
            require(projectId.isNotBlank() && scene.id.isNotBlank()) { "IDs cannot be blank" }
            val updates = mapOf(
                "episodeNumber" to scene.episodeNumber.coerceIn(1L, 60L),
                "sceneNumber" to scene.sceneNumber.coerceIn(1L, 200L),
                "title" to scene.title.trim().ifBlank { "Scene ${scene.sceneNumber}" }.take(150),
                "description" to scene.description.trim().take(3000),
                "cameraAngle" to scene.cameraAngle.trim().take(200),
                "cameraMovement" to scene.cameraMovement.trim().take(300),
                "characterAction" to scene.characterAction.trim().take(1000),
                "durationSeconds" to scene.durationSeconds.coerceIn(1L, 60L),
                "imagePrompt" to scene.imagePrompt.trim().take(3000),
                "videoPrompt" to scene.videoPrompt.trim().take(3000),
                "consistencyNotes" to scene.consistencyNotes.trim().take(1000),
                "dialogueCharacter" to scene.dialogueCharacter.trim().take(120),
                "hindiDialogue" to scene.hindiDialogue.trim().take(2000),
                "voiceOverScript" to scene.voiceOverScript.trim().take(2000),
                "subtitleText" to scene.subtitleText.trim().take(1000),
                "bgmPrompt" to scene.bgmPrompt.trim().take(500),
                "sfxPrompt" to scene.sfxPrompt.trim().take(500),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            scenesCol(uid, projectId).document(scene.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteScene(userId: String, projectId: String, sceneId: String): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId/scenes/$sceneId"
        return try {
            val uid = requireUserId(userId)
            require(projectId.isNotBlank() && sceneId.isNotBlank()) { "IDs cannot be blank" }
            scenesCol(uid, projectId).document(sceneId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun buildSceneCreateMap(
        uid: String,
        projectId: String,
        scene: StoryboardScene
    ): Map<String, Any> = mapOf(
        "userId" to uid,
        "projectId" to projectId,
        "episodeNumber" to scene.episodeNumber.coerceIn(1L, 60L),
        "sceneNumber" to scene.sceneNumber.coerceIn(1L, 200L),
        "title" to scene.title.trim().ifBlank { "Scene ${scene.sceneNumber}" }.take(150),
        "description" to scene.description.trim().take(3000),
        "cameraAngle" to scene.cameraAngle.trim().take(200),
        "cameraMovement" to scene.cameraMovement.trim().take(300),
        "characterAction" to scene.characterAction.trim().take(1000),
        "durationSeconds" to scene.durationSeconds.coerceIn(1L, 60L),
        "imagePrompt" to scene.imagePrompt.trim().take(3000),
        "videoPrompt" to scene.videoPrompt.trim().take(3000),
        "consistencyNotes" to scene.consistencyNotes.trim().take(1000),
        "dialogueCharacter" to scene.dialogueCharacter.trim().take(120),
        "hindiDialogue" to scene.hindiDialogue.trim().take(2000),
        "voiceOverScript" to scene.voiceOverScript.trim().take(2000),
        "subtitleText" to scene.subtitleText.trim().take(1000),
        "bgmPrompt" to scene.bgmPrompt.trim().take(500),
        "sfxPrompt" to scene.sfxPrompt.trim().take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    // --- Character Bible CRUD ---

    suspend fun addCharacter(userId: String, character: CharacterProfile): Result<String> {
        var docPath = "users/$userId/characters"
        return try {
            val uid = requireUserId(userId)
            val docRef = charactersCol(uid).document()
            docPath = docRef.path
            docRef.set(buildCharacterCreateMap(uid, character)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addCharactersBatch(
        userId: String,
        characters: List<CharacterProfile>
    ): Result<Int> {
        val colPath = "users/$userId/characters"
        return try {
            val uid = requireUserId(userId)
            if (characters.isEmpty()) return Result.success(0)
            val batch = db.batch()
            characters.forEach { ch ->
                val ref = charactersCol(uid).document()
                batch.set(ref, buildCharacterCreateMap(uid, ch))
            }
            batch.commit().await()
            Result.success(characters.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colPath)
            Result.failure(e)
        }
    }

    suspend fun updateCharacter(userId: String, character: CharacterProfile): Result<Unit> {
        val docPath = "users/$userId/characters/${character.id}"
        return try {
            val uid = requireUserId(userId)
            require(character.id.isNotBlank()) { "Character ID cannot be blank" }
            val updates = mapOf(
                "projectId" to sanitizeOptionalId(character.projectId),
                "name" to character.name.trim().ifBlank { "Unnamed Character" }.take(100),
                "role" to character.role.trim().take(100),
                "description" to character.description.trim().take(2000),
                "clothing" to character.clothing.trim().take(600),
                "hairstyle" to character.hairstyle.trim().take(400),
                "expressions" to character.expressions.trim().take(600),
                "consistencyToken" to character.consistencyToken.trim().take(1500),
                "voiceTone" to character.voiceTone.trim().take(200),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            charactersCol(uid).document(character.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCharacter(userId: String, characterId: String): Result<Unit> {
        val docPath = "users/$userId/characters/$characterId"
        return try {
            val uid = requireUserId(userId)
            require(characterId.isNotBlank()) { "Character ID cannot be blank" }
            charactersCol(uid).document(characterId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun buildCharacterCreateMap(
        uid: String,
        character: CharacterProfile
    ): Map<String, Any> = mapOf(
        "userId" to uid,
        "projectId" to sanitizeOptionalId(character.projectId),
        "name" to character.name.trim().ifBlank { "Unnamed Character" }.take(100),
        "role" to character.role.trim().take(100),
        "description" to character.description.trim().take(2000),
        "clothing" to character.clothing.trim().take(600),
        "hairstyle" to character.hairstyle.trim().take(400),
        "expressions" to character.expressions.trim().take(600),
        "consistencyToken" to character.consistencyToken.trim().take(1500),
        "voiceTone" to character.voiceTone.trim().take(200),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    // --- Saved Prompts CRUD ---

    suspend fun addSavedPrompt(userId: String, prompt: SavedPrompt): Result<String> {
        var docPath = "users/$userId/promptHistory"
        return try {
            val uid = requireUserId(userId)
            val docRef = promptsCol(uid).document()
            docPath = docRef.path
            val validType = if (prompt.promptType in listOf("IMAGE", "VIDEO", "VOICE")) {
                prompt.promptType
            } else {
                "IMAGE"
            }
            val payload = mapOf(
                "userId" to uid,
                "projectId" to sanitizeOptionalId(prompt.projectId),
                "promptType" to validType,
                "title" to prompt.title.trim().ifBlank { "Studio Prompt" }.take(150),
                "promptText" to prompt.promptText.trim().ifBlank { "Prompt" }.take(5000),
                "aspectRatio" to prompt.aspectRatio.trim().take(16),
                "artStyle" to prompt.artStyle.trim().take(80),
                "durationSeconds" to prompt.durationSeconds.coerceIn(0L, 60L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedPrompt(userId: String, promptId: String): Result<Unit> {
        val docPath = "users/$userId/promptHistory/$promptId"
        return try {
            val uid = requireUserId(userId)
            require(promptId.isNotBlank()) { "Prompt ID cannot be blank" }
            promptsCol(uid).document(promptId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun sanitizeEnum(value: String, allowed: List<String>, fallback: String): String {
        return allowed.firstOrNull { it.equals(value.trim(), ignoreCase = true) } ?: fallback
    }

    private fun sanitizeOptionalId(raw: String): String {
        val cleaned = raw.trim().take(128)
        return if (cleaned.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) cleaned else ""
    }
}
