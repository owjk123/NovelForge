package com.novelforge.app.ui.home

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelforge.app.R
import com.novelforge.app.data.preference.SettingsManager
import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLibrary: () -> Unit,
    onNavigateToWriting: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context.applicationContext) }

    // 收集当前配置
    val currentEndpoint by settingsManager.selectedEndpoint.collectAsState(initial = SettingsManager.DEFAULT_ENDPOINT)
    val currentModel by settingsManager.modelName.collectAsState(initial = SettingsManager.DEFAULT_MODEL)
    val customModelName by settingsManager.customModelName.collectAsState(initial = "")

    val displayEndpoint = settingsManager.getEndpointDisplayName(currentEndpoint)
    val displayModel = if (currentModel == "custom") customModelName else currentModel

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(uiState.createdNovelId) {
        uiState.createdNovelId?.let { novelId ->
            viewModel.resetCreatedNovelId()
            onNavigateToWriting(novelId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onNavigateToLibrary) {
                        Icon(
                            imageVector = Icons.Default.LibraryBooks,
                            contentDescription = stringResource(R.string.library_title)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 当前配置显示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = stringResource(R.string.current_config, displayEndpoint, displayModel),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "创建新小说",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.novel_title_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Text(
                text = stringResource(R.string.select_genre),
                style = MaterialTheme.typography.titleMedium
            )
            
            GenreSelector(
                selectedGenre = uiState.selectedGenre,
                onGenreSelected = viewModel::updateGenre
            )
            
            OutlinedTextField(
                value = uiState.characterSetting,
                onValueChange = viewModel::updateCharacterSetting,
                label = { Text(stringResource(R.string.character_setting_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            
            OutlinedTextField(
                value = uiState.worldSetting,
                onValueChange = viewModel::updateWorldSetting,
                label = { Text(stringResource(R.string.world_setting_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.createNovel(onNavigateToWriting) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isCreating
            ) {
                if (uiState.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.start_writing),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GenreSelector(
    selectedGenre: NovelGenre,
    onGenreSelected: (NovelGenre) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NovelGenre.entries.chunked(3).forEach { rowGenres ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowGenres.forEach { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = { onGenreSelected(genre) },
                        label = { Text(genre.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowGenres.size < 3) {
                    Spacer(modifier = Modifier.weight(1f))
                    if (rowGenres.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
