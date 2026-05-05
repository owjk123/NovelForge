package com.novelforge.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelforge.app.R
import com.novelforge.app.data.model.Novel
import com.novelforge.app.data.repository.NovelStats
import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.ui.theme.getGenreGradient
import com.novelforge.app.viewmodel.LibraryViewModel
import com.novelforge.app.viewmodel.SortType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToWriting: (Long) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
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
    
    // 删除确认对话框
    if (uiState.novelToDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirmation,
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text("确定要删除《${uiState.novelToDelete?.title}》吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteNovel(uiState.novelToDelete!!) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirmation) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 导出确认对话框
    if (uiState.novelToExport != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissExportConfirmation,
            icon = {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("导出小说") },
            text = { 
                Text("确定要导出《${uiState.novelToExport?.title}》到 Downloads 目录吗？") 
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.exportNovel(uiState.novelToExport!!) }
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExportConfirmation) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 长按上下文菜单 BottomSheet
    if (uiState.showContextMenu && uiState.selectedNovelForMenu != null) {
        NovelContextBottomSheet(
            novel = uiState.selectedNovelForMenu!!,
            onDismiss = viewModel::dismissContextMenu,
            onViewDetails = {
                viewModel.dismissContextMenu()
                onNavigateToWriting(uiState.selectedNovelForMenu!!.id)
            },
            onExport = { viewModel.showExportConfirmation(uiState.selectedNovelForMenu!!) },
            onDelete = { viewModel.showDeleteConfirmation(uiState.selectedNovelForMenu!!) }
        )
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
                            imageVector = Icons.Default.LibraryBooks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(stringResource(R.string.library_title))
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
        ) {
            // 搜索栏和排序
            SearchAndSortBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                sortType = uiState.sortType,
                onSortTypeChange = viewModel::updateSortType
            )
            
            // 类型筛选条
            GenreFilterRow(
                selectedGenre = uiState.selectedGenre,
                onGenreSelected = viewModel::updateGenreFilter
            )
            
            // 内容区域
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.novels.isEmpty()) {
                EmptyLibraryState(
                    onCreateNovel = { /* Navigate to home */ }
                )
            } else {
                NovelGrid(
                    novels = uiState.novels,
                    novelStats = uiState.novelStats,
                    onNovelClick = { onNavigateToWriting(it.id) },
                    onNovelLongClick = { viewModel.showContextMenu(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndSortBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortType: SortType,
    onSortTypeChange: (SortType) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("搜索小说标题") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )
        
        // 排序按钮
        Box {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "排序",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sortType == SortType.LAST_UPDATED) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("最近更新")
                        }
                    },
                    onClick = {
                        onSortTypeChange(SortType.LAST_UPDATED)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sortType == SortType.CREATED_TIME) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("创建时间")
                        }
                    },
                    onClick = {
                        onSortTypeChange(SortType.CREATED_TIME)
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sortType == SortType.TITLE) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("标题")
                        }
                    },
                    onClick = {
                        onSortTypeChange(SortType.TITLE)
                        showSortMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun GenreFilterRow(
    selectedGenre: NovelGenre?,
    onGenreSelected: (NovelGenre?) -> Unit
) {
    val genres = listOf(
        null to "全部",
        NovelGenre.FANTASY to "玄幻",
        NovelGenre.SCIFI to "科幻",
        NovelGenre.URBAN to "都市",
        NovelGenre.HAREM to "后宫",
        NovelGenre.MYSTERY to "悬疑",
        NovelGenre.CUSTOM to "自定义"
    )
    
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(genres) { (genre, label) ->
            FilterChip(
                selected = selectedGenre == genre,
                onClick = { onGenreSelected(genre) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun NovelGrid(
    novels: List<Novel>,
    novelStats: Map<Long, NovelStats>,
    onNovelClick: (Novel) -> Unit,
    onNovelLongClick: (Novel) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(novels, key = { it.id }) { novel ->
            NovelCard(
                novel = novel,
                stats = novelStats[novel.id],
                onClick = { onNovelClick(novel) },
                onLongClick = { onNovelLongClick(novel) }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NovelCard(
    novel: Novel,
    stats: NovelStats?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    val (gradientStart, gradientEnd) = getGenreGradient(novel.genre)
    
    // 解析类型显示名称
    val genreDisplay = if (novel.genre.startsWith("CUSTOM:")) {
        novel.genre.substringAfter("CUSTOM:")
    } else {
        try {
            NovelGenre.valueOf(novel.genre).displayName
        } catch (e: Exception) {
            novel.genre
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // 渐变色封面区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(gradientStart, gradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = novel.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = genreDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // 信息区域
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // 章节统计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stats?.getFormattedChapterCount() ?: "0章",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stats?.getFormattedWordCount() ?: "0字",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 最后更新时间
                Text(
                    text = "更新 ${dateFormat.format(Date(novel.updatedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelContextBottomSheet(
    novel: Novel,
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // 标题
            Text(
                text = novel.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 查看详情
            ListItem(
                headlineContent = { Text("查看详情") },
                leadingContent = {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onViewDetails)
            )
            
            // 导出
            ListItem(
                headlineContent = { Text("导出小说") },
                leadingContent = {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = onExport)
            )
            
            // 删除
            ListItem(
                headlineContent = { 
                    Text("删除", color = MaterialTheme.colorScheme.error)
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable(onClick = onDelete)
            )
        }
    }
}

@Composable
fun EmptyLibraryState(
    onCreateNovel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LibraryBooks,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "书架空空如也",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "去创作第一本小说吧",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCreateNovel,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("去创作")
        }
    }
}
