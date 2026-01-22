package com.health.companion.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.health.companion.data.remote.api.ChatMessageResponse
import com.health.companion.data.remote.api.MessageDTO
import com.health.companion.data.repositories.AuthRepository
import com.health.companion.data.repositories.ChatRepository
import com.health.companion.presentation.screens.chat.ChatUiState
import com.health.companion.presentation.screens.chat.ChatViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var viewModel: ChatViewModel
    private val chatRepository = mockk<ChatRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val savedStateHandle = mockk<SavedStateHandle>(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { savedStateHandle.get<String>("conversationId") } returns null
        coEvery { authRepository.getCurrentUserId() } returns "user-123"
        coEvery { chatRepository.connectWebSocket(any()) } returns flowOf()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Loading`() = runTest {
        // Arrange & Act
        viewModel = ChatViewModel(chatRepository, authRepository, savedStateHandle)
        
        // Assert
        assertTrue(viewModel.uiState.value is ChatUiState.Loading)
    }

    @Test
    fun `sendMessage should add user message and assistant response`() = runTest {
        // Arrange
        val userMessage = "Hello"
        val assistantResponse = ChatMessageResponse(
            response = "Hi there!",
            conversation_id = "conv-1",
            agents_used = listOf("general_agent"),
            confidence = 0.95f,
            sources = emptyList()
        )

        coEvery { chatRepository.sendMessage(userMessage, null) } returns assistantResponse
        
        viewModel = ChatViewModel(chatRepository, authRepository, savedStateHandle)
        advanceUntilIdle()

        // Act
        viewModel.sendMessage(userMessage)
        advanceUntilIdle()

        // Assert
        val messages = viewModel.messages.value
        assertEquals(2, messages.size)
        assertEquals("Hello", messages[0].content)
        assertEquals("user", messages[0].role)
        assertEquals("Hi there!", messages[1].content)
        assertEquals("assistant", messages[1].role)
    }

    @Test
    fun `sendMessage with blank text should not send`() = runTest {
        // Arrange
        viewModel = ChatViewModel(chatRepository, authRepository, savedStateHandle)
        advanceUntilIdle()

        // Act
        viewModel.sendMessage("")
        viewModel.sendMessage("   ")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { chatRepository.sendMessage(any(), any()) }
    }

    @Test
    fun `updateCurrentMessage should update state`() = runTest {
        // Arrange
        viewModel = ChatViewModel(chatRepository, authRepository, savedStateHandle)
        advanceUntilIdle()

        // Act
        viewModel.updateCurrentMessage("Test message")

        // Assert
        assertEquals("Test message", viewModel.currentMessage.value)
    }

    @Test
    fun `isLoading should be true while sending message`() = runTest {
        // Arrange
        coEvery { chatRepository.sendMessage(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            ChatMessageResponse(
                response = "Response",
                conversation_id = "conv-1",
                agents_used = emptyList(),
                confidence = 0.9f,
                sources = emptyList()
            )
        }
        
        viewModel = ChatViewModel(chatRepository, authRepository, savedStateHandle)
        advanceUntilIdle()

        // Act
        viewModel.sendMessage("Hello")

        // Assert
        assertTrue(viewModel.isLoading.value)
        
        advanceUntilIdle()
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `sendMessage error should update uiState to Error`() = runTest {
        // Arrange
        coEvery { chatRepository.sendMessage(any(), any()) } throws RuntimeException("Network error")
        
        viewModel = ChatViewModel(chatRepository, authRepository, savedStateHandle)
        advanceUntilIdle()

        // Act
        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state is ChatUiState.Error)
        assertEquals("Network error", (state as ChatUiState.Error).message)
    }

    @Test
    fun `clearError should reset state to Success`() = runTest {
        // Arrange
        coEvery { chatRepository.sendMessage(any(), any()) } throws RuntimeException("Error")
        
        viewModel = ChatViewModel(chatRepository, authRepository, savedStateHandle)
        advanceUntilIdle()
        
        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        // Act
        viewModel.clearError()

        // Assert
        assertTrue(viewModel.uiState.value is ChatUiState.Success)
    }

    @Test
    fun `deleteConversation should clear messages`() = runTest {
        // Arrange
        every { savedStateHandle.get<String>("conversationId") } returns "conv-123"
        coEvery { chatRepository.deleteConversation("conv-123") } returns Unit
        
        viewModel = ChatViewModel(chatRepository, authRepository, savedStateHandle)
        advanceUntilIdle()

        // Act
        viewModel.deleteConversation()
        advanceUntilIdle()

        // Assert
        coVerify { chatRepository.deleteConversation("conv-123") }
        assertTrue(viewModel.messages.value.isEmpty())
    }

    @Test
    fun `setRecording should update isRecording state`() = runTest {
        // Arrange
        viewModel = ChatViewModel(chatRepository, authRepository, savedStateHandle)
        advanceUntilIdle()

        // Act
        viewModel.setRecording(true)

        // Assert
        assertTrue(viewModel.isRecording.value)

        // Act
        viewModel.setRecording(false)

        // Assert
        assertFalse(viewModel.isRecording.value)
    }

    @Test
    fun `onVoiceResult should update currentMessage`() = runTest {
        // Arrange
        viewModel = ChatViewModel(chatRepository, authRepository, savedStateHandle)
        advanceUntilIdle()

        // Act
        viewModel.onVoiceResult("Voice recognized text")

        // Assert
        assertEquals("Voice recognized text", viewModel.currentMessage.value)
    }
}
