package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.CharacterProfile
import com.example.data.model.StoryboardProject
import com.example.data.model.StoryboardScene
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

private const val TAG = "BharatAiService"

data class VoiceProductionPack(
    val dialogueLines: List<VoiceDialogueLine>,
    val voiceOverScript: String,
    val srtSubtitles: String,
    val bgmDirection: String,
    val sfxList: String
)

data class VoiceDialogueLine(
    val character: String,
    val emotion: String,
    val hindiDialogue: String,
    val romanizedHint: String
)

data class PingResult(
    val latencyMs: Long,
    val modelName: String,
    val replyPreview: String
)

class BharatAiService(private val appContext: Context) {

    // Lazily cached text models so Firebase.ai is never evaluated eagerly at startup
    private val flashModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val flashLiteModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.5-flash-lite")
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private fun textModelFor(modelId: String): GenerativeModel =
        if (modelId == "gemini-3.5-flash-lite") flashLiteModel else flashModel

    private fun structuredModelFor(modelId: String, schema: Schema): GenerativeModel {
        val resolvedName = if (modelId == "gemini-3.5-flash-lite") {
            "gemini-3.5-flash-lite"
        } else {
            "gemini-3.8-flash"
        }
        return Firebase.ai.generativeModel(
            modelName = resolvedName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    // --- 1. Live Connection & Latency Test ---

    suspend fun testConnection(modelId: String): Result<PingResult> {
        val resolvedName = if (modelId == "gemini-3.5-flash-lite") {
            "gemini-3.5-flash-lite"
        } else {
            "gemini-3.8-flash"
        }
        return try {
            val start = System.currentTimeMillis()
            val response = withTimeout(30_000L) {
                textModelFor(resolvedName).generateContent("Respond with only the word READY.")
            }
            val elapsed = System.currentTimeMillis() - start
            val text = response.text?.trim().orEmpty()
            if (text.isBlank()) {
                Result.failure(IllegalStateException("Model returned an empty response."))
            } else {
                Result.success(PingResult(elapsed, resolvedName, text.take(40)))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Connection test timed out", e)
            Result.failure(IllegalStateException("Connection timed out after 30s. Check internet & App Check."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            Result.failure(e)
        }
    }

    // --- 2. AI Story Generator (Live Streaming) ---

    fun streamStory(
        modelId: String,
        premise: String,
        language: String,
        genre: String,
        artStyle: String,
        totalEpisodes: Int,
        episodeNumber: Int,
        previousStoryContext: String? = null
    ): Flow<String> = flow {
        val continuationSection = if (!previousStoryContext.isNullOrBlank()) {
            """
            Continue seamlessly from the previous episode(s) below. Keep all character names, locations, stakes, and emotional arcs consistent, and advance the plot meaningfully for Episode $episodeNumber of $totalEpisodes:
            --- PREVIOUS STORY CONTEXT ---
            ${previousStoryContext.takeLast(3500)}
            --- END CONTEXT ---
            """.trimIndent()
        } else {
            "Create an original, gripping opening Episode 1 of a $totalEpisodes-episode Indian series."
        }

        val prompt = """
            You are an acclaimed Indian showrunner and anime/short-drama scriptwriter for Bharat Storyboard AI.
            Write Episode $episodeNumber of $totalEpisodes in **$language** language (use authentic natural script and dialogue suitable for Indian YouTube & short-drama audiences).
            Genre: $genre
            Visual Aesthetic: $artStyle
            Core Idea / Premise: $premise

            $continuationSection

            Format the episode clearly with:
            - Episode Title
            - 3 to 4 vivid narrative beats with strong visual imagery, emotional Hindi/regional flavor, character dialogue, and a compelling cliffhanger ending suited for episode-to-episode continuation.
            Do not include markdown code fences. Write clean, readable prose.
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            textModelFor(modelId).generateContentStream(prompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Gemini returned an empty story response.")
        }
    }

    // --- 3. Storyboard Studio: Scene-by-Scene Structured Generation ---

    private val sceneItemSchema: Schema = Schema.obj(
        properties = mapOf(
            "sceneNumber" to Schema.integer(description = "Sequential scene number starting from 1"),
            "title" to Schema.string(description = "Short dramatic scene title"),
            "description" to Schema.string(description = "Visual scene staging, setting, lighting, and emotion"),
            "cameraAngle" to Schema.string(description = "Camera shot type and angle, e.g. Low-angle medium close-up"),
            "cameraMovement" to Schema.string(description = "Camera motion, e.g. Slow dolly push-in"),
            "characterAction" to Schema.string(description = "Precise character movement and gesture"),
            "durationSeconds" to Schema.integer(description = "Clip length between 4 and 12 seconds"),
            "imagePrompt" to Schema.string(description = "Detailed English text-to-image prompt matching art style and aspect ratio"),
            "videoPrompt" to Schema.string(description = "Detailed English Google Flow image-to-video motion prompt with camera movement and consistency"),
            "consistencyNotes" to Schema.string(description = "Wardrobe, face, lighting, and character continuity anchor"),
            "dialogueCharacter" to Schema.string(description = "Speaking character name or Narrator"),
            "hindiDialogue" to Schema.string(description = "Expressive Hindi dialogue line in Devanagari or natural Hinglish"),
            "voiceOverScript" to Schema.string(description = "Hindi voice-over narration with tone cues"),
            "subtitleText" to Schema.string(description = "Clean subtitle text for this shot"),
            "bgmPrompt" to Schema.string(description = "Background score mood and instruments"),
            "sfxPrompt" to Schema.string(description = "Sound effects cues")
        )
    )

    private val storyboardResponseSchema: Schema = Schema.obj(
        properties = mapOf(
            "scenes" to Schema.array(
                items = sceneItemSchema,
                description = "Ordered list of 5 cinematic scenes covering the story beat by beat"
            )
        )
    )

    suspend fun generateStoryboardScenes(
        modelId: String,
        project: StoryboardProject,
        characters: List<CharacterProfile>,
        sceneCount: Int = 5,
        startingSceneNumber: Int = 1
    ): Result<List<StoryboardScene>> {
        return try {
            val castContext = if (characters.isEmpty()) {
                "No pre-saved Character Bible entries yet. Maintain strong visual consistency across all scenes."
            } else {
                characters.joinToString("\n") { c ->
                    "- ${c.name} (${c.role}): ${c.consistencyToken.ifBlank { "${c.description}, wearing ${c.clothing}, hair: ${c.hairstyle}" }}"
                }
            }

            val prompt = """
                Convert this Indian story into exactly $sceneCount detailed, sequential cinema storyboard scenes starting from sceneNumber $startingSceneNumber for Episode ${project.currentEpisode}.
                Project Title: ${project.title}
                Genre: ${project.genre} | Visual Style: ${project.artStyle} | Aspect Ratio: ${project.aspectRatio} | Story Language: ${project.language}

                Character Bible (keep clothing, hair, and facial features 100% consistent in every imagePrompt and videoPrompt):
                $castContext

                Story Manuscript:
                ${project.storyText.ifBlank { project.logline }.take(8000)}

                Requirements:
                - imagePrompt MUST be rich English tailored for ${project.artStyle} style in ${project.aspectRatio} framing.
                - videoPrompt MUST be formatted for Google Flow Image-to-Video with explicit camera movement, subject motion, and physics.
                - hindiDialogue and voiceOverScript MUST be evocative Hindi tailored for Indian creators.
                - durationSeconds MUST be between 4 and 12.
            """.trimIndent()

            val model = structuredModelFor(modelId, storyboardResponseSchema)
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty storyboard response.")
            val root = JSONObject(rawJson)
            val array = root.getJSONArray("scenes")
            val parsed = (0 until array.length()).map { idx ->
                val obj = array.getJSONObject(idx)
                parseSceneJson(
                    obj = obj,
                    projectId = project.id,
                    episodeNumber = project.currentEpisode,
                    fallbackSceneNumber = startingSceneNumber + idx
                )
            }
            if (parsed.isEmpty()) {
                Result.failure(IllegalStateException("No scenes were returned by Gemini."))
            } else {
                Result.success(parsed)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Storyboard generation timed out", e)
            Result.failure(IllegalStateException("Storyboard generation timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Storyboard generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun regenerateSingleScene(
        modelId: String,
        project: StoryboardProject,
        existingScene: StoryboardScene,
        characters: List<CharacterProfile>,
        directorNotes: String
    ): Result<StoryboardScene> {
        return try {
            val castSummary = characters.joinToString("; ") { "${it.name}: ${it.consistencyToken}" }
            val prompt = """
                Regenerate and elevate Scene ${existingScene.sceneNumber} of "${project.title}" (Episode ${existingScene.episodeNumber}).
                Visual Style: ${project.artStyle} | Aspect Ratio: ${project.aspectRatio} | Genre: ${project.genre}
                Characters: $castSummary
                Current Scene Title: ${existingScene.title}
                Current Description: ${existingScene.description}
                Director Revision Notes: ${directorNotes.ifBlank { "Make camera movement more cinematic, enhance visual details in English prompts, and sharpen the Hindi dialogue." }}
            """.trimIndent()

            val model = structuredModelFor(modelId, sceneItemSchema)
            val response = withTimeout(60_000L) { model.generateContent(prompt) }
            val rawJson = response.text ?: throw IllegalStateException("Empty scene response.")
            val updated = parseSceneJson(
                obj = JSONObject(rawJson),
                projectId = project.id,
                episodeNumber = existingScene.episodeNumber,
                fallbackSceneNumber = existingScene.sceneNumber.toInt()
            ).copy(
                id = existingScene.id,
                userId = existingScene.userId,
                createdAt = existingScene.createdAt
            )
            Result.success(updated)
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Scene regeneration timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Scene regeneration failed", e)
            Result.failure(e)
        }
    }

    private fun parseSceneJson(
        obj: JSONObject,
        projectId: String,
        episodeNumber: Long,
        fallbackSceneNumber: Int
    ): StoryboardScene = StoryboardScene(
        projectId = projectId,
        episodeNumber = episodeNumber,
        sceneNumber = obj.optLong("sceneNumber", fallbackSceneNumber.toLong()).coerceIn(1L, 200L),
        title = obj.optString("title", "Scene $fallbackSceneNumber").ifBlank { "Scene $fallbackSceneNumber" },
        description = obj.optString("description", ""),
        cameraAngle = obj.optString("cameraAngle", "Medium Shot"),
        cameraMovement = obj.optString("cameraMovement", "Slow Dolly Push-In"),
        characterAction = obj.optString("characterAction", ""),
        durationSeconds = obj.optLong("durationSeconds", 6L).coerceIn(1L, 60L),
        imagePrompt = obj.optString("imagePrompt", ""),
        videoPrompt = obj.optString("videoPrompt", ""),
        consistencyNotes = obj.optString("consistencyNotes", ""),
        dialogueCharacter = obj.optString("dialogueCharacter", "Narrator"),
        hindiDialogue = obj.optString("hindiDialogue", ""),
        voiceOverScript = obj.optString("voiceOverScript", ""),
        subtitleText = obj.optString("subtitleText", ""),
        bgmPrompt = obj.optString("bgmPrompt", ""),
        sfxPrompt = obj.optString("sfxPrompt", "")
    )

    // --- 4. Character Studio: Character Bible Extraction & Generation ---

    private val characterBibleSchema: Schema = Schema.obj(
        properties = mapOf(
            "characters" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Character full name"),
                        "role" to Schema.string(description = "Role such as Protagonist, Deuteragonist, Antagonist, Mentor"),
                        "description" to Schema.string(description = "Age, facial structure, eyes, skin tone, physique, personality"),
                        "clothing" to Schema.string(description = "Detailed signature outfit, fabrics, colors, and accessories"),
                        "hairstyle" to Schema.string(description = "Hair length, color, texture, and styling"),
                        "expressions" to Schema.string(description = "Signature facial expressions and emotional range"),
                        "consistencyToken" to Schema.string(description = "Compact English prompt lock describing immutable visual traits for consistent AI image/video generation"),
                        "voiceTone" to Schema.string(description = "Voice casting description and Hindi delivery cadence")
                    )
                ),
                description = "List of 3 main characters for the Character Bible"
            )
        )
    )

    suspend fun generateCharacterBible(
        modelId: String,
        project: StoryboardProject?,
        customConcept: String
    ): Result<List<CharacterProfile>> {
        return try {
            val sourceText = when {
                customConcept.isNotBlank() -> customConcept
                project != null && project.storyText.isNotBlank() -> project.storyText.take(6000)
                project != null -> "${project.title}: ${project.logline}"
                else -> "Indian futuristic mythological anime heroes in neo-Varanasi"
            }
            val prompt = """
                Create a professional Character Bible (3 distinct characters) for Bharat Storyboard AI.
                Genre: ${project?.genre ?: "Anime"} | Art Style: ${project?.artStyle ?: "Anime"}
                Story / Concept:
                $sourceText

                Ensure each character has ultra-specific visual design details (Indian cultural aesthetics blended with modern cinema/anime design) and a crisp English consistencyToken that keeps their face, hair, and outfit identical across every shot.
            """.trimIndent()

            val model = structuredModelFor(modelId, characterBibleSchema)
            val response = withTimeout(60_000L) { model.generateContent(prompt) }
            val rawJson = response.text ?: throw IllegalStateException("Empty character response.")
            val arr = JSONObject(rawJson).getJSONArray("characters")
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CharacterProfile(
                    projectId = project?.id.orEmpty(),
                    name = o.optString("name", "Character ${i + 1}"),
                    role = o.optString("role", "Lead"),
                    description = o.optString("description", ""),
                    clothing = o.optString("clothing", ""),
                    hairstyle = o.optString("hairstyle", ""),
                    expressions = o.optString("expressions", ""),
                    consistencyToken = o.optString("consistencyToken", ""),
                    voiceTone = o.optString("voiceTone", "")
                )
            }
            if (list.isEmpty()) {
                Result.failure(IllegalStateException("No characters returned by Gemini."))
            } else {
                Result.success(list)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Character generation timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Character Bible generation failed", e)
            Result.failure(e)
        }
    }

    // --- 5. Image Prompt Studio ---

    suspend fun generateDetailedImagePrompt(
        modelId: String,
        sceneIdea: String,
        artStyle: String,
        aspectRatio: String,
        selectedCharacters: List<CharacterProfile>
    ): Result<String> {
        return try {
            val charLocks = selectedCharacters.joinToString("\n") {
                "- ${it.name}: ${it.consistencyToken.ifBlank { "${it.description}, ${it.clothing}, ${it.hairstyle}" }}"
            }
            val prompt = """
                Craft a single production-ready English AI image generation prompt for an Indian creator studio.
                Scene Concept: $sceneIdea
                Art Style: $artStyle
                Aspect Ratio Framing: $aspectRatio
                Character Consistency Profiles:
                ${charLocks.ifBlank { "None specified" }}

                Include subject framing, lighting, color palette, environment atmosphere, camera lens, and render quality keywords tailored to $artStyle style and $aspectRatio composition. Output ONLY the clean English prompt text with no explanations or markdown quotes.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                textModelFor(modelId).generateContent(prompt)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Model returned an empty image prompt."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Image prompt generation timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image prompt generation failed", e)
            Result.failure(e)
        }
    }

    // --- 6. Google Flow Video Prompt Studio ---

    suspend fun generateGoogleFlowVideoPrompt(
        modelId: String,
        sceneAction: String,
        cameraMovement: String,
        durationSeconds: Long,
        aspectRatio: String,
        artStyle: String,
        selectedCharacters: List<CharacterProfile>
    ): Result<String> {
        return try {
            val consistencyBlock = selectedCharacters.joinToString("; ") {
                "${it.name}: ${it.consistencyToken.ifBlank { "${it.clothing}, ${it.hairstyle}" }}"
            }
            val prompt = """
                Write an expert Google Flow Image-to-Video production prompt in English.
                Visual Style: $artStyle | Aspect Ratio: $aspectRatio | Clip Duration: ${durationSeconds}s
                Camera Movement: $cameraMovement
                Character Action & Scene Beat: $sceneAction
                Character Consistency Lock: ${consistencyBlock.ifBlank { "Preserve exact character face, costume, and lighting from keyframe" }}

                Structure the output cleanly with:
                1. Motion Prompt (camera movement, pacing across ${durationSeconds} seconds, subject performance, atmospheric motion like rain/fabric/particles)
                2. Consistency Directive (explicit instructions to preserve face, outfit, lighting, and color grade)
                Keep it directly copy-pasteable into Google Flow. No markdown code fences.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                textModelFor(modelId).generateContent(prompt)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Model returned an empty Google Flow prompt."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Video prompt generation timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Video prompt generation failed", e)
            Result.failure(e)
        }
    }

    // --- 7. Voice & Audio Studio (Structured Hindi Dialogue + VO + SRT + BGM/SFX) ---

    private val voicePackSchema: Schema = Schema.obj(
        properties = mapOf(
            "dialogues" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "character" to Schema.string(description = "Character name"),
                        "emotion" to Schema.string(description = "Vocal delivery cue, e.g. Intense whisper, Heroic resolve"),
                        "hindiDialogue" to Schema.string(description = "Dialogue in Hindi Devanagari script"),
                        "romanizedHint" to Schema.string(description = "Natural Hinglish pronunciation guide")
                    )
                ),
                description = "Character-wise Hindi dialogues"
            ),
            "voiceOverScript" to Schema.string(description = "Full Hindi narrator voice-over script with pause and tone markers"),
            "srtSubtitles" to Schema.string(description = "Timestamped SRT subtitle block starting from 00:00:00,000"),
            "bgmDirection" to Schema.string(description = "Indian + cinematic BGM composition prompt with BPM and instruments"),
            "sfxList" to Schema.string(description = "Timestamped SFX production cue sheet")
        )
    )

    suspend fun generateVoiceProductionPack(
        modelId: String,
        topicOrScene: String,
        characters: List<CharacterProfile>,
        languageTone: String
    ): Result<VoiceProductionPack> {
        return try {
            val castList = characters.joinToString("\n") { "- ${it.name} (${it.role}, Voice: ${it.voiceTone})" }
            val prompt = """
                Create a complete Hindi Voice-Over & Audio Production Pack for Bharat Storyboard AI.
                Scene / Story Beat: $topicOrScene
                Tone & Mood: $languageTone
                Characters:
                ${castList.ifBlank { "- Narrator (Warm cinematic Hindi baritone)\n- Protagonist (Passionate young Indian voice)" }}

                Generate expressive character-wise Hindi dialogues (Devanagari + Hinglish pronunciation hint), a cinematic narrator voice-over script, properly formatted SRT subtitles, BGM prompt, and SFX cue sheet.
            """.trimIndent()

            val model = structuredModelFor(modelId, voicePackSchema)
            val response = withTimeout(60_000L) { model.generateContent(prompt) }
            val rawJson = response.text ?: throw IllegalStateException("Empty voice production response.")
            val root = JSONObject(rawJson)
            val dArr = root.getJSONArray("dialogues")
            val dialogues = (0 until dArr.length()).map { idx ->
                val item = dArr.getJSONObject(idx)
                VoiceDialogueLine(
                    character = item.optString("character", "Character"),
                    emotion = item.optString("emotion", "Expressive"),
                    hindiDialogue = item.optString("hindiDialogue", ""),
                    romanizedHint = item.optString("romanizedHint", "")
                )
            }
            Result.success(
                VoiceProductionPack(
                    dialogueLines = dialogues,
                    voiceOverScript = root.optString("voiceOverScript", ""),
                    srtSubtitles = root.optString("srtSubtitles", ""),
                    bgmDirection = root.optString("bgmDirection", ""),
                    sfxList = root.optString("sfxList", "")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Voice script generation timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Voice pack generation failed", e)
            Result.failure(e)
        }
    }

    // --- 8. Runtime AI Image Generation (Gemini Nano Banana 2.1) ---

    suspend fun generateAndCacheImage(
        prompt: String,
        fileKey: String
    ): Result<File> {
        return try {
            val bitmap: Bitmap = withTimeout(60_000L) {
                val response = imageModel.generateContent(prompt)
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image ?: throw IllegalStateException("No image bitmap was returned by Gemini.")
            }
            val savedFile = withContext(Dispatchers.IO) {
                val dir = File(appContext.filesDir, "generated_images").apply { mkdirs() }
                val safeKey = fileKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
                val outFile = File(dir, "$safeKey.jpg")
                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                outFile
            }
            Result.success(savedFile)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            Result.failure(IllegalStateException("Image generation timed out after 60 seconds. Please try again."))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image generation quota exceeded", e)
            Result.failure(IllegalStateException("Image generation quota exceeded or Blaze billing required on Firebase project."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(e)
        }
    }

    fun getCachedImageFile(fileKey: String): File? {
        val safeKey = fileKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val file = File(File(appContext.filesDir, "generated_images"), "$safeKey.jpg")
        return if (file.exists() && file.length() > 0L) file else null
    }
}
