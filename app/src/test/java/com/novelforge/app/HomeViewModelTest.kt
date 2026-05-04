package com.novelforge.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.novelforge.app.data.db.ChapterDao
import com.novelforge.app.data.db.NovelDao
import com.novelforge.app.data.model.Novel
import com.novelforge.app.data.repository.NovelRepository
import com.novelforge.app.domain.prompt.NovelGenre
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var novelDao: NovelDao
    private lateinit var chapterDao: ChapterDao
    private lateinit var repository: NovelRepository
    private lateinit var viewModel: HomeViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        novelDao = mockk(relaxed = true)
        chapterDao = mockk(relaxed = true)
        repository = NovelRepository(novelDao, chapterDao)
        viewModel = HomeViewModel(repository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `updateTitle updates state correctly`() = runTest {
        viewModel.updateTitle("My Novel")
        
        assertEquals("My Novel", viewModel.uiState.value.title)
    }
    
    @Test
    fun `updateGenre updates state correctly`() = runTest {
        viewModel.updateGenre(NovelGenre.SCIFI)
        
        assertEquals(NovelGenre.SCIFI, viewModel.uiState.value.selectedGenre)
    }
    
    @Test
    fun `createNovel shows error when title is blank`() = runTest {
        viewModel.createNovel { }
        
        assertEquals("请输入小说标题", viewModel.uiState.value.errorMessage)
    }
    
    @Test
    fun `createNovel shows error when character setting is blank`() = runTest {
        viewModel.updateTitle("My Novel")
        viewModel.createNovel { }
        
        assertEquals("请输入主角设定", viewModel.uiState.value.errorMessage)
    }
    
    @Test
    fun `createNovel shows error when world setting is blank`() = runTest {
        viewModel.updateTitle("My Novel")
        viewModel.updateCharacterSetting("Hero character")
        viewModel.createNovel { }
        
        assertEquals("请输入世界观设定", viewModel.uiState.value.errorMessage)
    }
    
    @Test
    fun `createNovel succeeds with valid input`() = runTest {
        viewModel.updateTitle("My Novel")
        viewModel.updateGenre(NovelGenre.FANTASY)
        viewModel.updateCharacterSetting("Hero character")
        viewModel.updateWorldSetting("Fantasy world")
        
        coEvery { novelDao.insertNovel(any()) } returns 1L
        
        var successCalled = false
        var novelId: Long = 0
        
        viewModel.createNovel { id ->
            successCalled = true
            novelId = id
        }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(successCalled)
        assertEquals(1L, novelId)
    }
    
    @Test
    fun `clearError clears error message`() = runTest {
        viewModel.createNovel { }
        assertNotNull(viewModel.uiState.value.errorMessage)
        
        viewModel.clearError()
        
        assertNull(viewModel.uiState.value.errorMessage)
    }
    
    @Test
    fun `resetCreatedNovelId resets created novel id`() = runTest {
        viewModel.updateTitle("My Novel")
        viewModel.updateCharacterSetting("Hero")
        viewModel.updateWorldSetting("World")
        
        coEvery { novelDao.insertNovel(any()) } returns 1L
        viewModel.createNovel { }
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(1L, viewModel.uiState.value.createdNovelId)
        
        viewModel.resetCreatedNovelId()
        
        assertNull(viewModel.uiState.value.createdNovelId)
    }
}
