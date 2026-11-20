package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AiTaskState
import com.example.data.model.StoryboardProject
import com.example.data.model.StudioOptions
import com.example.data.model.UiState
import com.example.ui.components.AiStatusBanner
import com.example.ui.components.EmptyStudioPlaceholder
import com.example.ui.components.GradientActionButton
import com.example.ui.components.HorizontalOptionRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.theme.LocalStudioColors

@Composable
fun ProjectsScreen(
    projectsState: UiState<List<StoryboardProject>>,
    activeProjectId: String?,
    defaultLanguage: String,
    defaultArtStyle: String,
    defaultAspectRatio: String,
    onCreateProject: (
        title: String,
        logline: String,
        language: String,
        genre: String,
        artStyle: String,
        aspectRatio: String,
        totalEpisodes: Int
    ) -> Unit,
    onUpdateProject: (
        projectId: String,
        title: String,
        logline: String,
        language: String,
        genre: String,
        artStyle: String,
        aspectRatio: String,
        totalEpisodes: Long,
        status: String
    ) -> Unit,
    onDeleteProject: (String) -> Unit,
    onOpenInStudio: (StoryboardProject) -> Unit,
    onExportPdf: (StoryboardProject) -> Unit,
    onExportText: (StoryboardProject) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<StoryboardProject?>(null) }
    var deletingProject by remember { mutableStateOf<StoryboardProject?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "My Projects",
                subtitle = "Cloud-synced Indian filmmaking projects, scripts & PDF exports",
                badge = "Firestore Cloud"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search projects, genres, languages…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("projects_search_input")
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                ) {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("projects_create_new_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create new project",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        when (projectsState) {
            is UiState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item { AiStatusBanner(state = AiTaskState.Error(projectsState.message)) }
            }
            is UiState.Success -> {
                val filtered = projectsState.data.filter { p ->
                    searchQuery.isBlank() ||
                        p.title.contains(searchQuery, ignoreCase = true) ||
                        p.genre.contains(searchQuery, ignoreCase = true) ||
                        p.language.contains(searchQuery, ignoreCase = true)
                }
                if (filtered.isEmpty()) {
                    item {
                        EmptyStudioPlaceholder(
                            icon = Icons.Filled.FolderSpecial,
                            title = if (searchQuery.isBlank()) "No Saved Projects Yet" else "No Matching Projects",
                            description = if (searchQuery.isBlank()) {
                                "Create a blank storyboard project here or generate a complete story in the Stories tab."
                            } else {
                                "Try a different search keyword."
                            },
                            actionLabel = if (searchQuery.isBlank()) "Create New Project" else null,
                            onAction = if (searchQuery.isBlank()) ({ showCreateDialog = true }) else null
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            isActive = project.id == activeProjectId,
                            onOpenInStudio = { onOpenInStudio(project) },
                            onEdit = { editingProject = project },
                            onExportPdf = { onExportPdf(project) },
                            onExportText = { onExportText(project) },
                            onDelete = { deletingProject = project }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ProjectEditorDialog(
            initial = StoryboardProject(
                language = defaultLanguage,
                artStyle = defaultArtStyle,
                aspectRatio = defaultAspectRatio,
                totalEpisodes = 10L
            ),
            isNew = true,
            onDismiss = { showCreateDialog = false },
            onSave = { title, logline, lang, genre, style, ratio, episodes, _ ->
                onCreateProject(title, logline, lang, genre, style, ratio, episodes.toInt())
                showCreateDialog = false
            }
        )
    }

    editingProject?.let { proj ->
        ProjectEditorDialog(
            initial = proj,
            isNew = false,
            onDismiss = { editingProject = null },
            onSave = { title, logline, lang, genre, style, ratio, episodes, status ->
                onUpdateProject(proj.id, title, logline, lang, genre, style, ratio, episodes, status)
                editingProject = null
            }
        )
    }

    deletingProject?.let { proj ->
        AlertDialog(
            onDismissRequest = { deletingProject = null },
            title = { Text("Delete Project?") },
            text = { Text("Delete \"${proj.title}\" and all its storyboard scenes permanently from cloud storage?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProject(proj.id)
                        deletingProject = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProject = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: StoryboardProject,
    isActive: Boolean,
    onOpenInStudio: () -> Unit,
    onEdit: () -> Unit,
    onExportPdf: () -> Unit,
    onExportText: () -> Unit,
    onDelete: () -> Unit
) {
    StudioCard(
        borderColor = if (isActive) MaterialTheme.colorScheme.primary else LocalStudioColors.current.cardBorder,
        modifier = Modifier.testTag("project_card_${project.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isActive) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${project.language} • ${project.genre} • ${project.artStyle} • ${project.aspectRatio} • Ep ${project.currentEpisode}/${project.totalEpisodes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit project", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete project", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }

        if (project.logline.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = project.logline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(12.dp))
        GradientActionButton(
            text = "Open in Storyboard Studio",
            icon = Icons.Filled.DashboardCustomize,
            testTag = "project_open_studio_${project.id}",
            onClick = onOpenInStudio
        )

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onExportPdf,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("project_export_pdf_${project.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export PDF")
            }
            OutlinedButton(
                onClick = onExportText,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("project_export_txt_${project.id}"),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export TXT")
            }
        }
    }
}

@Composable
private fun ProjectEditorDialog(
    initial: StoryboardProject,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        logline: String,
        language: String,
        genre: String,
        artStyle: String,
        aspectRatio: String,
        totalEpisodes: Long,
        status: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var logline by remember { mutableStateOf(initial.logline) }
    var language by remember { mutableStateOf(initial.language) }
    var genre by remember { mutableStateOf(initial.genre) }
    var artStyle by remember { mutableStateOf(initial.artStyle) }
    var aspectRatio by remember { mutableStateOf(initial.aspectRatio) }
    var episodesText by remember { mutableStateOf(initial.totalEpisodes.toString()) }
    var status by remember { mutableStateOf(initial.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New Filmmaking Project" else "Edit Project") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Project Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = logline,
                    onValueChange = { logline = it.take(500) },
                    label = { Text("Logline / Premise") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = episodesText,
                    onValueChange = { episodesText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Total Episodes (1–60)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalOptionRow("LANGUAGE", StudioOptions.languages, language, { language = it }, "dlg_lang")
                HorizontalOptionRow("GENRE", StudioOptions.genres, genre, { genre = it }, "dlg_genre")
                HorizontalOptionRow("ART STYLE", StudioOptions.artStyles, artStyle, { artStyle = it }, "dlg_style")
                HorizontalOptionRow("ASPECT RATIO", StudioOptions.aspectRatios, aspectRatio, { aspectRatio = it }, "dlg_ratio")
                if (!isNew) {
                    HorizontalOptionRow("STATUS", StudioOptions.projectStatuses, status, { status = it }, "dlg_status")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val eps = episodesText.toLongOrNull()?.coerceIn(1L, 60L) ?: 1L
                    onSave(
                        title.ifBlank { "Untitled Storyboard" },
                        logline,
                        language,
                        genre,
                        artStyle,
                        aspectRatio,
                        eps,
                        status
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
