package com.novelforge.app.ui.writing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelforge.app.R
import com.novelforge.app.viewmodel.WritingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    novelId: Long,
    onNavigateBack: () -> Unit,
    viewModel: WritingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(novelId) {
        viewModel.loadNovel(novelId)
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
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            WritingBottomBar(
                isGenerating = uiState.isGenerating,
                hasContent = uiState.displayContent.isNotBlank(),
                hasCurrentChapter = uiState.currentChapter != null,
                onGenerateNewChapter = viewModel::generateNewChapter,
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
            imageVector = Icons.Default.PlayArrow,
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5f
        )
    }
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
        shadowElevation = 8.dp
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
                    imageVector = Icons.Default.PlayArrow,
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
