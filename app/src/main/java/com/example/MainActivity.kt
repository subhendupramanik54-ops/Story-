package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.BottomNavTab
import com.example.data.model.StoryboardProject
import com.example.data.model.StudioSubTab
import com.example.data.model.UiState
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOutGoogle
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StoryGeneratorScreen
import com.example.ui.screens.StudioHubScreen
import com.example.ui.theme.BharatStoryboardTheme
import com.example.util.ProjectExporter
import com.example.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent BEFORE setContent or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            BharatStoryboardTheme {
                BharatAppNavigation()
            }
        }
    }
}

@Composable
fun BharatAppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            BharatStudioRoot(
                currentUserId = user.uid,
                userDisplayName = user.displayName.orEmpty(),
                userEmail = user.email.orEmpty()
            )
        }
    }
}

private data class NavDestinationItem(
    val tab: BottomNavTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BharatStudioRoot(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = StudioViewModel.provideFactory(currentUserId)
    )

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val activeScenesState by viewModel.activeScenesState.collectAsStateWithLifecycle()
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val promptHistoryState by viewModel.promptHistoryState.collectAsStateWithLifecycle()

    val storyTaskState by viewModel.storyTaskState.collectAsStateWithLifecycle()
    val storyDraftText by viewModel.storyDraftText.collectAsStateWithLifecycle()
    val storyboardTaskState by viewModel.storyboardTaskState.collectAsStateWithLifecycle()
    val regeneratingSceneId by viewModel.regeneratingSceneId.collectAsStateWithLifecycle()
    val characterTaskState by viewModel.characterTaskState.collectAsStateWithLifecycle()
    val characterImages by viewModel.characterImages.collectAsStateWithLifecycle()
    val imageStudioTaskState by viewModel.imageStudioTaskState.collectAsStateWithLifecycle()
    val generatedImagePrompt by viewModel.generatedImagePrompt.collectAsStateWithLifecycle()
    val studioPreviewImageState by viewModel.studioPreviewImageState.collectAsStateWithLifecycle()
    val videoStudioTaskState by viewModel.videoStudioTaskState.collectAsStateWithLifecycle()
    val generatedVideoPrompt by viewModel.generatedVideoPrompt.collectAsStateWithLifecycle()
    val voiceStudioTaskState by viewModel.voiceStudioTaskState.collectAsStateWithLifecycle()
    val generatedVoicePack by viewModel.generatedVoicePack.collectAsStateWithLifecycle()
    val connectionHealth by viewModel.connectionHealth.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(BottomNavTab.HOME) }
    var selectedStudioTab by rememberSaveable { mutableStateOf(StudioSubTab.STORYBOARD) }

    // Return to Home on system back from secondary tabs
    BackHandler(enabled = currentTab != BottomNavTab.HOME) {
        currentTab = BottomNavTab.HOME
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val projectsList = (projectsState as? UiState.Success)?.data.orEmpty()
    val activeScenesList = (activeScenesState as? UiState.Success)?.data.orEmpty()
    val charactersList = (charactersState as? UiState.Success)?.data.orEmpty()
    val promptHistoryList = (promptHistoryState as? UiState.Success)?.data.orEmpty()

    fun exportProject(project: StoryboardProject?, asPdf: Boolean) {
        if (project == null) {
            scope.launch { snackbarHostState.showSnackbar("Create or select a project to export.") }
            return
        }
        val scenes = if (project.id == activeProject?.id) activeScenesList else emptyList()
        val chars = charactersList.filter { it.projectId.isBlank() || it.projectId == project.id }
        val result = if (asPdf) {
            ProjectExporter.exportAsPdf(context, project, scenes, chars)
        } else {
            ProjectExporter.exportAsText(context, project, scenes, chars)
        }
        result.onSuccess { uri ->
            ProjectExporter.shareExportedFile(
                context = context,
                uri = uri,
                mimeType = if (asPdf) "application/pdf" else "text/plain",
                shareTitle = "${project.title} Storyboard"
            )
        }.onFailure { err ->
            scope.launch {
                snackbarHostState.showSnackbar("Export failed: ${err.localizedMessage ?: "Error"}")
            }
        }
    }

    val navItems = remember {
        listOf(
            NavDestinationItem(BottomNavTab.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavDestinationItem(BottomNavTab.STORIES, "Stories", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
            NavDestinationItem(BottomNavTab.STUDIO, "Studio", Icons.Filled.DashboardCustomize, Icons.Outlined.DashboardCustomize),
            NavDestinationItem(BottomNavTab.PROJECTS, "Projects", Icons.Filled.FolderSpecial, Icons.Outlined.FolderSpecial),
            NavDestinationItem(BottomNavTab.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                BottomNavTab.HOME -> "Bharat Storyboard AI"
                                BottomNavTab.STORIES -> "AI Story Generator"
                                BottomNavTab.STUDIO -> "Creator Studio"
                                BottomNavTab.PROJECTS -> "My Projects"
                                BottomNavTab.SETTINGS -> "Studio Settings"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        navItems.forEach { item ->
                            val selected = currentTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = item.tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${item.tab.route}"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { item ->
                            val selected = currentTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = item.tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("nav_rail_${item.tab.route}"),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        when (currentTab) {
                            BottomNavTab.HOME -> HomeScreen(
                                projects = projectsList,
                                activeProject = activeProject,
                                activeScenes = activeScenesList,
                                characters = charactersList,
                                onSelectProject = viewModel::selectProject,
                                onNavigateToTab = { currentTab = it },
                                onOpenStudioTab = { sub ->
                                    selectedStudioTab = sub
                                    currentTab = BottomNavTab.STUDIO
                                },
                                onExportActiveProjectPdf = { exportProject(activeProject, asPdf = true) }
                            )

                            BottomNavTab.STORIES -> StoryGeneratorScreen(
                                activeProject = activeProject,
                                storyDraftText = storyDraftText,
                                taskState = storyTaskState,
                                defaultLanguage = preferences.defaultLanguage,
                                defaultArtStyle = preferences.defaultArtStyle,
                                defaultAspectRatio = preferences.defaultAspectRatio,
                                onDraftChanged = viewModel::updateStoryDraft,
                                onGenerateStory = viewModel::generateNewStory,
                                onContinueEpisode = viewModel::continueNextEpisode,
                                onSaveAsProject = viewModel::saveStoryAsNewProject,
                                onOpenStoryboard = {
                                    selectedStudioTab = StudioSubTab.STORYBOARD
                                    currentTab = BottomNavTab.STUDIO
                                }
                            )

                            BottomNavTab.STUDIO -> StudioHubScreen(
                                selectedTab = selectedStudioTab,
                                onSelectTab = { selectedStudioTab = it },
                                projects = projectsList,
                                activeProject = activeProject,
                                scenesState = activeScenesState,
                                charactersState = charactersState,
                                promptHistory = promptHistoryList,
                                storyboardTaskState = storyboardTaskState,
                                regeneratingSceneId = regeneratingSceneId,
                                characterTaskState = characterTaskState,
                                characterImages = characterImages,
                                cachedCharImageLookup = viewModel::cachedCharacterImage,
                                imageStudioTaskState = imageStudioTaskState,
                                generatedImagePrompt = generatedImagePrompt,
                                studioPreviewImageState = studioPreviewImageState,
                                videoStudioTaskState = videoStudioTaskState,
                                generatedVideoPrompt = generatedVideoPrompt,
                                voiceStudioTaskState = voiceStudioTaskState,
                                generatedVoicePack = generatedVoicePack,
                                defaultArtStyle = preferences.defaultArtStyle,
                                defaultAspectRatio = preferences.defaultAspectRatio,
                                onSelectProject = viewModel::selectProject,
                                onCreateStory = { currentTab = BottomNavTab.STORIES },
                                onGenerateScenes = viewModel::generateStoryboardForActiveProject,
                                onRegenerateScene = viewModel::regenerateScene,
                                onSaveScene = viewModel::saveScene,
                                onDuplicateScene = viewModel::duplicateScene,
                                onDeleteScene = viewModel::deleteScene,
                                onGenerateCharacters = viewModel::generateCharactersWithAi,
                                onSaveCharacter = viewModel::saveCharacter,
                                onDeleteCharacter = viewModel::deleteCharacter,
                                onGenerateCharacterPortrait = viewModel::generateCharacterPortrait,
                                onGenerateImagePrompt = viewModel::generateImagePrompt,
                                onRenderPreviewImage = viewModel::renderImageStudioPreview,
                                onGenerateVideoPrompt = viewModel::generateVideoPrompt,
                                onSavePromptToHistory = viewModel::savePromptToHistory,
                                onDeleteSavedPrompt = viewModel::deleteSavedPrompt,
                                onGenerateVoicePack = viewModel::generateVoicePack,
                                onCopiedNotice = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )

                            BottomNavTab.PROJECTS -> ProjectsScreen(
                                projectsState = projectsState,
                                activeProjectId = activeProject?.id,
                                defaultLanguage = preferences.defaultLanguage,
                                defaultArtStyle = preferences.defaultArtStyle,
                                defaultAspectRatio = preferences.defaultAspectRatio,
                                onCreateProject = { title, logline, lang, genre, style, ratio, episodes ->
                                    viewModel.saveStoryAsNewProject(
                                        title = title,
                                        logline = logline,
                                        language = lang,
                                        genre = genre,
                                        artStyle = style,
                                        aspectRatio = ratio,
                                        totalEpisodes = episodes,
                                        storyText = ""
                                    )
                                },
                                onUpdateProject = viewModel::updateProjectMetadata,
                                onDeleteProject = viewModel::deleteProject,
                                onOpenInStudio = { proj ->
                                    viewModel.selectProject(proj.id)
                                    selectedStudioTab = StudioSubTab.STORYBOARD
                                    currentTab = BottomNavTab.STUDIO
                                },
                                onExportPdf = { proj ->
                                    viewModel.selectProject(proj.id)
                                    exportProject(proj, asPdf = true)
                                },
                                onExportText = { proj ->
                                    viewModel.selectProject(proj.id)
                                    exportProject(proj, asPdf = false)
                                }
                            )

                            BottomNavTab.SETTINGS -> SettingsScreen(
                                userDisplayName = userDisplayName,
                                userEmail = userEmail,
                                preferences = preferences,
                                connectionHealth = connectionHealth,
                                onSelectModel = viewModel::updateSelectedModel,
                                onTestConnection = viewModel::testAiConnection,
                                onDefaultLanguageChanged = viewModel::updateDefaultLanguage,
                                onDefaultArtStyleChanged = viewModel::updateDefaultArtStyle,
                                onDefaultAspectRatioChanged = viewModel::updateDefaultAspectRatio,
                                onSignOut = {
                                    signOutGoogle(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
