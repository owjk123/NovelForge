package com.novelforge.app.ui.writing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelforge.app.R
import com.novelforge.app.domain.prompt.EmotionalTone
import com.novelforge.app.viewmodel.WritingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    novelId: Long,
    onNavigateBack: () -> Unit,
    viewModel: WritingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(novelId) {
        viewModel.loadNovel(novelId)
    }
    
    // 监听生成成功，自动滚动到顶部
    LaunchedEffect(uiState.currentChapter) {
        if (uiState.currentChapter != null && !uiState.isGenerating) {
            // 新章节生成完成后，滚动到顶部
            scope.launch {
                scrollState.animateScrollTo(0)
            }
        }
    }
    
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar("错误: $message")
            viewModel.clearMessages()
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }
    
    // 章节规划对话框
    if (uiState.showGuidanceDialog) {
        ChapterGuidanceDialog(
            guidance = uiState.chapterGuidance,
            onPlotDirectionChange = viewModel::updateGuidancePlotDirection,
            onKeyEventsChange = viewModel::updateGuidanceKeyEvents,
            onEmotionalToneChange = viewModel::updateGuidanceEmotionalTone,
            onConfirm = viewModel::generateNewChapter,
            onDismiss = viewModel::dismissGuidanceDialog
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.novel?.title ?: "创作中",
                            style = MaterialTheme.typography.titleMedium
                        )
                        uiState.currentChapter?.let { chapter ->
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 导出按钮
                    if (uiState.chapters.isNotEmpty()) {
                        IconButton(onClick = viewModel::exportToDownloads) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = stringResource(R.string.export_novel),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            WritingBottomBar(
                isGenerating = uiState.isGenerating,
                hasContent = uiState.displayContent.isNotBlank(),
                hasCurrentChapter = uiState.currentChapter != null,
                onGenerateNewChapter = viewModel::showGuidanceDialog,
                onContinueWriting = viewModel::continueWriting,
                onSaveDraft = viewModel::saveDraft
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (!uiState.isGenerating && uiState.displayContent.isBlank()) {
                NewChapterInput(
                    chapterSummary = uiState.chapterSummary,
                    onChapterSummaryChange = viewModel::updateChapterSummary,
                    modifier = Modifier.weight(1f)
                )
            } else {
                ChapterContent(
                    content = uiState.displayContent,
                    scrollState = scrollState,
                    modifier = Modifier.weight(1f)
                )
                
                if (uiState.isGenerating) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NewChapterInput(
    chapterSummary: String,
    onChapterSummaryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AutoStories,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "准备开始创作新章节",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = chapterSummary,
            onValueChange = onChapterSummaryChange,
            label = { Text("本章剧情摘要（可选）") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 6,
            placeholder = { Text("描述本章想要发展的剧情方向，帮助AI更好地创作...") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "点击下方「开始创作」按钮开始生成第一章",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChapterContent(
    content: String,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(scrollState)
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5f
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterGuidanceDialog(
    guidance: com.novelforge.app.domain.prompt.ChapterGuidance,
    onPlotDirectionChange: (String) -> Unit,
    onKeyEventsChange: (String) -> Unit,
    onEmotionalToneChange: (EmotionalTone) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expandedTone by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("章节规划", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "为新章节提供引导，帮助AI生成更符合你期望的内容",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = guidance.plotDirection,
                    onValueChange = onPlotDirectionChange,
                    label = { Text("本章剧情方向 *") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    placeholder = { Text("描述本章的主要剧情发展方向，如：主角与反派对峙并揭露真相") }
                )
                
                OutlinedTextField(
                    value = guidance.keyEvents,
                    onValueChange = onKeyEventsChange,
                    label = { Text("关键事件/转折（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    placeholder = { Text("描述本章的关键事件或转折点") }
                )
                
                // 情感基调选择
                ExposedDropdownMenuBox(
                    expanded = expandedTone,
                    onExpandedChange = { expandedTone = it }
                ) {
                    OutlinedTextField(
                        value = guidance.emotionalTone.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("情感基调（可选）") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTone) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedTone,
                        onDismissRequest = { expandedTone = false }
                    ) {
                        EmotionalTone.entries.forEach { tone ->
                            DropdownMenuItem(
                                text = { Text(tone.displayName) },
                                onClick = {
                                    onEmotionalToneChange(tone)
                                    expandedTone = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = guidance.plotDirection.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("开始创作")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun WritingBottomBar(
    isGenerating: Boolean,
    hasContent: Boolean,
    hasCurrentChapter: Boolean,
    onGenerateNewChapter: () -> Unit,
    onContinueWriting: () -> Unit,
    onSaveDraft: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onGenerateNewChapter,
                modifier = Modifier.weight(1f),
                enabled = !isGenerating
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.start_writing))
            }
            
            if (hasContent && hasCurrentChapter) {
                OutlinedButton(
                    onClick = onContinueWriting,
                    modifier = Modifier.weight(1f),
                    enabled = !isGenerating
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.continue_writing))
                }
                
                IconButton(onClick = onSaveDraft) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = stringResource(R.string.save_draft)
                    )
                }
            }
        }
    }
}
