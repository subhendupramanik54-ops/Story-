package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.CharacterProfile
import com.example.data.model.StoryboardProject
import com.example.data.model.StoryboardScene
import java.io.File
import java.io.FileOutputStream

object ProjectExporter {

    private fun exportsDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    private fun safeFileName(title: String): String =
        title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "bharat_storyboard" }.take(40)

    fun exportAsText(
        context: Context,
        project: StoryboardProject,
        scenes: List<StoryboardScene>,
        characters: List<CharacterProfile>
    ): Result<Uri> = runCatching {
        val sb = StringBuilder()
        sb.appendLine("====================================================")
        sb.appendLine("BHARAT STORYBOARD AI — PRODUCTION SCRIPT")
        sb.appendLine("====================================================")
        sb.appendLine("Project: ${project.title}")
        sb.appendLine("Language: ${project.language} | Genre: ${project.genre} | Style: ${project.artStyle} | Ratio: ${project.aspectRatio}")
        sb.appendLine("Episodes: ${project.currentEpisode} / ${project.totalEpisodes} | Status: ${project.status}")
        if (project.logline.isNotBlank()) {
            sb.appendLine("Logline: ${project.logline}")
        }
        sb.appendLine()

        if (project.storyText.isNotBlank()) {
            sb.appendLine("--- STORY MANUSCRIPT ---")
            sb.appendLine(project.storyText)
            sb.appendLine()
        }

        if (characters.isNotEmpty()) {
            sb.appendLine("--- CHARACTER BIBLE (${characters.size}) ---")
            characters.forEach { c ->
                sb.appendLine("• ${c.name} (${c.role})")
                if (c.description.isNotBlank()) sb.appendLine("  Bio: ${c.description}")
                if (c.clothing.isNotBlank()) sb.appendLine("  Outfit: ${c.clothing}")
                if (c.hairstyle.isNotBlank()) sb.appendLine("  Hair: ${c.hairstyle}")
                if (c.expressions.isNotBlank()) sb.appendLine("  Expressions: ${c.expressions}")
                if (c.consistencyToken.isNotBlank()) sb.appendLine("  Consistency Anchor: ${c.consistencyToken}")
                sb.appendLine()
            }
        }

        sb.appendLine("--- SCENE-BY-SCENE STORYBOARD (${scenes.size} Scenes) ---")
        scenes.forEach { scene ->
            sb.appendLine()
            sb.appendLine("[EP ${scene.episodeNumber} | SCENE ${scene.sceneNumber}] ${scene.title} (${scene.durationSeconds}s)")
            sb.appendLine("Camera: ${scene.cameraAngle} | Movement: ${scene.cameraMovement}")
            if (scene.description.isNotBlank()) sb.appendLine("Visual: ${scene.description}")
            if (scene.characterAction.isNotBlank()) sb.appendLine("Action: ${scene.characterAction}")
            if (scene.imagePrompt.isNotBlank()) sb.appendLine("Image Prompt (EN): ${scene.imagePrompt}")
            if (scene.videoPrompt.isNotBlank()) sb.appendLine("Google Flow Video Prompt: ${scene.videoPrompt}")
            if (scene.consistencyNotes.isNotBlank()) sb.appendLine("Consistency Notes: ${scene.consistencyNotes}")
            if (scene.hindiDialogue.isNotBlank()) sb.appendLine("Hindi Dialogue (${scene.dialogueCharacter}): ${scene.hindiDialogue}")
            if (scene.voiceOverScript.isNotBlank()) sb.appendLine("Voice-Over: ${scene.voiceOverScript}")
            if (scene.subtitleText.isNotBlank()) sb.appendLine("Subtitle: ${scene.subtitleText}")
            if (scene.bgmPrompt.isNotBlank()) sb.appendLine("BGM: ${scene.bgmPrompt}")
            if (scene.sfxPrompt.isNotBlank()) sb.appendLine("SFX: ${scene.sfxPrompt}")
        }

        val outFile = File(exportsDir(context), "${safeFileName(project.title)}_script.txt")
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }

    fun exportAsPdf(
        context: Context,
        project: StoryboardProject,
        scenes: List<StoryboardScene>,
        characters: List<CharacterProfile>
    ): Result<Uri> = runCatching {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = pageWidth - margin * 2

        val titlePaint = Paint().apply {
            color = Color.rgb(20, 26, 56)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(91, 56, 214)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(34, 40, 56)
            textSize = 10.5f
            isAntiAlias = true
        }
        val metaPaint = Paint().apply {
            color = Color.rgb(90, 102, 130)
            textSize = 9.5f
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(210, 216, 232)
            strokeWidth = 1f
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin + 16f

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > pageHeight - margin) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = margin + 16f
            }
        }

        fun drawWrapped(text: String, paint: Paint, lineSpacing: Float = 15f) {
            if (text.isBlank()) return
            val paragraphs = text.split("\n")
            for (para in paragraphs) {
                val words = para.split(" ")
                var currentLine = ""
                for (word in words) {
                    val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(candidate) <= contentWidth) {
                        currentLine = candidate
                    } else {
                        ensureSpace(lineSpacing)
                        canvas.drawText(currentLine, margin, y, paint)
                        y += lineSpacing
                        currentLine = word
                    }
                }
                if (currentLine.isNotEmpty()) {
                    ensureSpace(lineSpacing)
                    canvas.drawText(currentLine, margin, y, paint)
                    y += lineSpacing
                }
            }
        }

        // Cover Header
        drawWrapped("BHARAT STORYBOARD AI — PRODUCTION BOOK", metaPaint, 14f)
        drawWrapped(project.title, titlePaint, 24f)
        drawWrapped(
            "${project.language} • ${project.genre} • ${project.artStyle} • ${project.aspectRatio} • Ep ${project.currentEpisode}/${project.totalEpisodes}",
            metaPaint,
            16f
        )
        if (project.logline.isNotBlank()) {
            y += 4f
            drawWrapped(project.logline, bodyPaint)
        }
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 16f

        if (characters.isNotEmpty()) {
            ensureSpace(24f)
            drawWrapped("CHARACTER BIBLE (${characters.size})", sectionPaint, 18f)
            characters.forEach { c ->
                drawWrapped("${c.name} (${c.role})", bodyPaint)
                if (c.consistencyToken.isNotBlank()) {
                    drawWrapped("Consistency: ${c.consistencyToken}", metaPaint, 14f)
                }
                y += 4f
            }
            y += 8f
        }

        ensureSpace(24f)
        drawWrapped("STORYBOARD SCENES (${scenes.size})", sectionPaint, 18f)
        scenes.forEach { scene ->
            ensureSpace(70f)
            drawWrapped(
                "Scene ${scene.sceneNumber} (Ep ${scene.episodeNumber}): ${scene.title} [${scene.durationSeconds}s]",
                sectionPaint,
                16f
            )
            drawWrapped("Camera: ${scene.cameraAngle} | Motion: ${scene.cameraMovement}", metaPaint, 14f)
            if (scene.description.isNotBlank()) drawWrapped("Visual: ${scene.description}", bodyPaint)
            if (scene.imagePrompt.isNotBlank()) drawWrapped("Image Prompt: ${scene.imagePrompt}", bodyPaint)
            if (scene.videoPrompt.isNotBlank()) drawWrapped("Google Flow Video: ${scene.videoPrompt}", bodyPaint)
            if (scene.hindiDialogue.isNotBlank()) {
                drawWrapped("Dialogue (${scene.dialogueCharacter}): ${scene.hindiDialogue}", bodyPaint)
            }
            if (scene.voiceOverScript.isNotBlank()) drawWrapped("Voice-Over: ${scene.voiceOverScript}", bodyPaint)
            if (scene.bgmPrompt.isNotBlank() || scene.sfxPrompt.isNotBlank()) {
                drawWrapped("Audio — BGM: ${scene.bgmPrompt} | SFX: ${scene.sfxPrompt}", metaPaint, 14f)
            }
            y += 8f
            canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
            y += 14f
        }

        document.finishPage(page)

        val outFile = File(exportsDir(context), "${safeFileName(project.title)}_storyboard.pdf")
        FileOutputStream(outFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }

    fun shareExportedFile(context: Context, uri: Uri, mimeType: String, shareTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, shareTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, shareTitle))
    }
}
