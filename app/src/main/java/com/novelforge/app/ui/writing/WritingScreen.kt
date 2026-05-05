package com.novelforge.app.ui.writing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelforge.app.R
import com.novelforge.app.data.model.Chapter
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
    LaunchedEffect(uiState.currentChapterIndex) {
        if (!uiState.isGenerating) {
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
    
    // 章节列表 BottomSheet
    if (uiState.showChapterList) {
        ChapterListBottomSheet(
            chapters = uiState.chapters,
            currentIndex = uiState.currentChapterIndex,
            onChapterSelect = viewModel::selectChapter,
            onChapterEditTitle = { viewModel.showEditTitleDialog(it) },
            onChapterDelete = { viewModel.showDeleteChapterDialog(it) },
            onCreateChapter = {
                viewModel.dismissChapterList()
                viewModel.showGuidanceSheet()
            },
            onDismiss = viewModel::dismissChapterList
        )
    }
    
    // AI创作引导 BottomSheet
    if (uiState.showGuidanceSheet) {
        ChapterGuidanceBottomSheet(
            guidance = uiState.chapterGuidance,
            targetWordCount = uiState.targetWordCount,
            onPlotDirectionChange = viewModel::updateGuidancePlotDirection,
            onKeyEventsChange = viewModel::updateGuidanceKeyEvents,
            onEmotionalToneChange = viewModel::updateGuidanceEmotionalTone,
            onTargetWordCountChange = viewModel::updateTargetWordCount,
            onConfirm = viewModel::generateNewChapter,
            onDismiss = viewModel::dismissGuidanceSheet
        )
    }
    
    // 小说信息编辑 BottomSheet
    if (uiState.showNovelInfoSheet) {
        NovelInfoBottomSheet(
            novel = uiState.novel,
            onConfirm = { title, char, world ->
                viewModel.updateNovelInfo(title, char, world)
            },
            onDismiss = viewModel::dismissNovelInfoSheet
        )
    }
    
    // 章节标题编辑对话框
    if (uiState.showEditTitleDialog) {
        ChapterTitleEditDialog(
            currentTitle = uiState.editingChapterTitle,
            onConfirm = viewModel::updateChapterTitle,
            onDismiss = viewModel::dismissEditTitleDialog
        )
    }
    
    // 删除章节确认对话框
    if (uiState.showDeleteChapterDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteChapterDialog,
            title = { Text("删除章节") },
            text = { Text("确定要删除「${uiState.chapterToDelete?.title}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteChapter,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteChapterDialog) {
                    Text("取消")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            WritingTopBar(
                novelTitle = uiState.novel?.title ?: "创作中",
                chapterTitle = uiState.currentChapter?.title,
                onBackClick = onNavigateBack,
                onTitleClick = viewModel::showChapterList,
                onMoreClick = { /* Show more menu */ },
                onExportClick = viewModel::exportToDownloads,
                onEditNovelInfoClick = viewModel::showNovelInfoSheet,
                onBackToLibrary = onNavigateBack,
                hasChapters = uiState.chapters.isNotEmpty()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            WritingBottomBar(
                isGenerating = uiState.isGenerating,
                hasCurrentChapter = uiState.currentChapter != null,
                onGenerateNewChapter = viewModel::showGuidanceSheet,
                onContinueWriting = viewModel::continueWriting,
                onSaveDraft = viewModel::saveDraft,
                hasUnsavedChanges = uiState.hasUnsavedChanges
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (uiState.isGenerating) {
                    // 生成中状态
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = uiState.displayContent.ifEmpty { "正在生成内容..." },
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "AI正在创作中，请稍候...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (uiState.currentChapter == null && uiState.chapters.isEmpty()) {
                    // 无章节状态
                    EmptyChapterState(
                        onGenerate = viewModel::showGuidanceSheet
                    )
                } else {
                    // 内容编辑器
                    EditableContent(
                        content = uiState.displayContent,
                        onContentChange = viewModel::updateContent,
                        scrollState = scrollState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // 字数统计栏
            if (uiState.currentChapter != null || uiState.displayContent.isNotBlank()) {
                WordCountBar(
                    wordCount = uiState.currentWordCount,
                    hasUnsavedChanges = uiState.hasUnsavedChanges
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingTopBar(
    novelTitle: String,
    chapterTitle: String?,
    onBackClick: () -> Unit,
    onTitleClick: () -> Unit,
    onMoreClick: () -> Unit,
    onExportClick: () -> Unit,
    onEditNovelInfoClick: () -> Unit,
    onBackToLibrary: () -> Unit,
    hasChapters: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = {
            Column(
                modifier = Modifier.clickable(onClick = onTitleClick)
            ) {
                Text(
                    text = novelTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                if (chapterTitle != null) {
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
        },
        actions = {
            // 更多菜单
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多"
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (hasChapters) {
                        DropdownMenuItem(
                            text = { Text("导出小说") },
                            onClick = {
                                showMenu = false
                                onExportClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.FileDownload, contentDescription = null)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("编辑小说信息") },
                        onClick = {
                            showMenu = false
                            onEditNovelInfoClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("返回书架") },
                        onClick = {
                            showMenu = false
                            onBackToLibrary()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.LibraryBooks, contentDescription = null)
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
fun EditableContent(
    content: String,
    onContentChange: (String) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(scrollState)
    ) {
        BasicTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 16.sp,
                lineHeight = 28.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box {
                    if (content.isEmpty()) {
                        Text(
                            text = "开始输入内容...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun WordCountBar(
    wordCount: Int,
    hasUnsavedChanges: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "共 $wordCount 字",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (hasUnsavedChanges) {
                Text(
                    text = "● 未保存",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EmptyChapterState(
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoStories,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "准备开始创作",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "点击下方按钮开始第一章的创作",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onGenerate) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI创作")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListBottomSheet(
    chapters: List<Chapter>,
    currentIndex: Int,
    onChapterSelect: (Int) -> Unit,
    onChapterEditTitle: (Chapter) -> Unit,
    onChapterDelete: (Chapter) -> Unit,
    onCreateChapter: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "章节列表",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            if (chapters.isEmpty()) {
                Text(
                    text = "暂无章节",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    itemsIndexed(chapters) { index, chapter ->
                        var showChapterMenu by remember { mutableStateOf(false) }
                        
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = chapter.title,
                                    fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                                    color = if (index == currentIndex) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "${chapter.content.length}字",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                if (index == currentIndex) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "当前章节",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = "${chapter.order}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onChapterSelect(index) }
                        )
                        
                        // Long press to show edit/delete menu
                        DropdownMenu(
                            expanded = showChapterMenu,
                            onDismissRequest = { showChapterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("编辑标题") },
                                onClick = {
                                    showChapterMenu = false
                                    onChapterEditTitle(chapter)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Text("删除", color = MaterialTheme.colorScheme.error) 
                                },
                                onClick = {
                                    showChapterMenu = false
                                    onChapterDelete(chapter)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 新建章节按钮
            Button(
                onClick = onCreateChapter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("新建章节")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterGuidanceBottomSheet(
    guidance: com.novelforge.app.domain.prompt.ChapterGuidance,
    targetWordCount: Int,
    onPlotDirectionChange: (String) -> Unit,
    onKeyEventsChange: (String) -> Unit,
    onEmotionalToneChange: (EmotionalTone) -> Unit,
    onTargetWordCountChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expandedTone by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "章节规划",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
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
                    label = { Text("情感基调") },
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
            
            // 字数目标滑块
            Column {
                Text(
                    text = "字数目标: ${targetWordCount}字",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = targetWordCount.toFloat(),
                    onValueChange = { onTargetWordCountChange(it.toInt()) },
                    valueRange = 1000f..5000f,
                    steps = 7
                )
            }
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = guidance.plotDirection.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始创作")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NovelInfoBottomSheet(
    novel: com.novelforge.app.data.model.Novel?,
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(novel?.title ?: "") }
    var characterSetting by remember { mutableStateOf(novel?.characterSetting ?: "") }
    var worldSetting by remember { mutableStateOf(novel?.worldSetting ?: "") }
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "编辑小说信息",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("小说标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = characterSetting,
                onValueChange = { characterSetting = it },
                label = { Text("主角设定") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            
            OutlinedTextField(
                value = worldSetting,
                onValueChange = { worldSetting = it },
                label = { Text("世界观设定") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            
            Button(
                onClick = { onConfirm(title, characterSetting, worldSetting) },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        }
    }
}

@Composable
fun ChapterTitleEditDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑章节标题") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("章节标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank()
            ) {
                Text("保存")
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
    hasCurrentChapter: Boolean,
    onGenerateNewChapter: () -> Unit,
    onContinueWriting: () -> Unit,
    onSaveDraft: () -> Unit,
    hasUnsavedChanges: Boolean
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI创作按钮（主按钮）
            Button(
                onClick = onGenerateNewChapter,
                modifier = Modifier.weight(1f),
                enabled = !isGenerating
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI创作")
            }
            
            // 续写按钮（当有当前章节时显示）
            if (hasCurrentChapter) {
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
                    Text("续写")
                }
            }
            
            // 保存按钮
            if (hasCurrentChapter && hasUnsavedChanges) {
                FilledTonalIconButton(
                    onClick = onSaveDraft,
                    enabled = !isGenerating
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = stringResource(R.string.save_draft)
                    )
                }
            }
        }
    }
}
