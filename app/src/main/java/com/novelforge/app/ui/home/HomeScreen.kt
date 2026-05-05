package com.novelforge.app.ui.home

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
    onNavigateToWriting: (Long) -> Unit,
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
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
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
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(stringResource(R.string.home_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 当前配置显示 - 更紧凑的设计
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.current_config, displayEndpoint, displayModel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题区域
                Text(
                    text = "创建新小说",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // 故事描述区域
                OutlinedTextField(
                    value = uiState.storyDescription,
                    onValueChange = viewModel::updateStoryDescription,
                    label = { Text("故事描述（用于AI自动填充）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4,
                    placeholder = { Text("描述你的故事构思，如：\"一个少年在末世中觉醒异能，踏上拯救世界的旅途\"") },
                    shape = RoundedCornerShape(12.dp)
                )
                
                // AI自动填充按钮
                Button(
                    onClick = viewModel::autoFillWithAI,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isAutoFilling && uiState.storyDescription.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isAutoFilling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onTertiary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI填充中...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI智能填充")
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 小说基本信息标题
                Text(
                    text = "小说基本信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // 标题输入
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text(stringResource(R.string.novel_title_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                // 类型选择标签
                Text(
                    text = stringResource(R.string.select_genre),
                    style = MaterialTheme.typography.titleSmall
                )
                
                // 类型选择器
                GenreSelector(
                    selectedGenre = uiState.selectedGenre,
                    customGenreName = uiState.customGenreName,
                    onGenreSelected = viewModel::updateGenre,
                    onCustomGenreNameChange = viewModel::updateCustomGenreName
                )
                
                // 主角设定
                OutlinedTextField(
                    value = uiState.characterSetting,
                    onValueChange = viewModel::updateCharacterSetting,
                    label = { Text(stringResource(R.string.character_setting_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
                
                // 世界观设定
                OutlinedTextField(
                    value = uiState.worldSetting,
                    onValueChange = viewModel::updateWorldSetting,
                    label = { Text(stringResource(R.string.world_setting_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 创建按钮 - 更醒目
                Button(
                    onClick = { viewModel.createNovel(onNavigateToWriting) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    enabled = !uiState.isCreating,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState.isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.start_writing),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GenreSelector(
    selectedGenre: NovelGenre,
    customGenreName: String,
    onGenreSelected: (NovelGenre) -> Unit,
    onCustomGenreNameChange: (String) -> Unit
) {
    var showCustomInput by remember { mutableStateOf(selectedGenre == NovelGenre.CUSTOM) }
    
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
                        onClick = {
                            onGenreSelected(genre)
                            showCustomInput = (genre == NovelGenre.CUSTOM)
                        },
                        label = { Text(genre.displayName) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                if (rowGenres.size < 3) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        // 自定义类型输入框
        AnimatedVisibility(visible = showCustomInput) {
            OutlinedTextField(
                value = customGenreName,
                onValueChange = onCustomGenreNameChange,
                label = { Text("自定义类型名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("例如：修仙、穿越、末日生存等") },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
