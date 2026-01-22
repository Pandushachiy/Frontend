package com.health.companion.data.repositories

import com.health.companion.data.local.dao.ChatMessageDao
import com.health.companion.data.local.database.ChatMessageEntity
import com.health.companion.data.remote.api.ChatApi
import com.health.companion.data.remote.api.ChatMessageRequest
import com.health.companion.data.remote.api.ChatMessageResponse
import com.health.companion.data.remote.api.ConversationDTO
import com.health.companion.data.remote.api.DeleteResponse
import com.health.companion.services.WebSocketManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ChatRepositoryTest {

    private lateinit var chatRepository: ChatRepository
    private val chatApi = mockk<ChatApi>()
    private val chatMessageDao = mockk<ChatMessageDao>()
    private val webSocketManager = mockk<WebSocketManager>()

    @Before
    fun setup() {
        chatRepository = ChatRepositoryImpl(chatApi, chatMessageDao, webSocketManager)
    }

    @Test
    fun `sendMessage should return response and save to database`() = runTest {
        // Arrange
        val message = "Hello, how are you?"
        val conversationId = "conv-123"
        val expectedResponse = ChatMessageResponse(
            response = "I'm doing well! How can I help you today?",
            conversation_id = conversationId,
            agents_used = listOf("wellness_agent"),
            confidence = 0.95f,
            sources = listOf("medical_knowledge_base")
        )

        coEvery { chatApi.sendMessage(any()) } returns expectedResponse
        coEvery { chatMessageDao.insert(any()) } just runs

        // Act
        val result = chatRepository.sendMessage(message, conversationId)

        // Assert
        assertEquals(expectedResponse, result)
        assertEquals(expectedResponse.response, result.response)
        assertEquals(conversationId, result.conversation_id)
        
        // Verify database insertions (user message + assistant message)
        coVerify(exactly = 2) { chatMessageDao.insert(any()) }
    }

    @Test
    fun `sendMessage with null conversationId should create new conversation`() = runTest {
        // Arrange
        val message = "Start new conversation"
        val newConversationId = "new-conv-456"
        val expectedResponse = ChatMessageResponse(
            response = "Hello! I'm your health companion.",
            conversation_id = newConversationId,
            agents_used = listOf("general_agent"),
            confidence = 0.9f,
            sources = emptyList()
        )

        coEvery { chatApi.sendMessage(any()) } returns expectedResponse
        coEvery { chatMessageDao.insert(any()) } just runs

        // Act
        val result = chatRepository.sendMessage(message, null)

        // Assert
        assertEquals(newConversationId, result.conversation_id)
        coVerify {
            chatApi.sendMessage(ChatMessageRequest(
                message = message,
                conversation_id = null
            ))
        }
    }

    @Test
    fun `getConversationMessages should return messages from database`() = runTest {
        // Arrange
        val conversationId = "conv-123"
        val entities = listOf(
            ChatMessageEntity(
                id = "msg-1",
                conversationId = conversationId,
                content = "Hello",
                role = "user"
            ),
            ChatMessageEntity(
                id = "msg-2",
                conversationId = conversationId,
                content = "Hi there!",
                role = "assistant",
                agentName = "wellness_agent"
            )
        )

        coEvery { chatMessageDao.getMessagesFlow(conversationId) } returns flowOf(entities)

        // Act
        chatRepository.getConversationMessages(conversationId)
            .collect { messages ->
                // Assert
                assertEquals(2, messages.size)
                assertEquals("Hello", messages[0].content)
                assertEquals("user", messages[0].role)
                assertEquals("Hi there!", messages[1].content)
                assertEquals("assistant", messages[1].role)
                assertEquals("wellness_agent", messages[1].agent_name)
            }
    }

    @Test
    fun `getAllConversations should return conversations from API`() = runTest {
        // Arrange
        val expectedConversations = listOf(
            ConversationDTO(
                id = "conv-1",
                title = "Health consultation",
                created_at = "2024-01-15T10:30:00Z"
            ),
            ConversationDTO(
                id = "conv-2",
                title = "Medication questions",
                created_at = "2024-01-14T14:00:00Z"
            )
        )

        coEvery { chatApi.getConversations() } returns expectedConversations

        // Act
        chatRepository.getAllConversations()
            .collect { conversations ->
                // Assert
                assertEquals(2, conversations.size)
                assertEquals("Health consultation", conversations[0].title)
                assertEquals("Medication questions", conversations[1].title)
            }
    }

    @Test
    fun `deleteConversation should delete from API and database`() = runTest {
        // Arrange
        val conversationId = "conv-to-delete"
        
        coEvery { chatApi.deleteConversation(conversationId) } returns DeleteResponse(status = "deleted")
        coEvery { chatMessageDao.deleteByConversation(conversationId) } just runs

        // Act
        chatRepository.deleteConversation(conversationId)

        // Assert
        coVerify { chatApi.deleteConversation(conversationId) }
        coVerify { chatMessageDao.deleteByConversation(conversationId) }
    }

    @Test
    fun `sendMessage should throw exception on API error`() = runTest {
        // Arrange
        val message = "Test message"
        val expectedException = RuntimeException("Network error")

        coEvery { chatApi.sendMessage(any()) } throws expectedException

        // Act & Assert
        try {
            chatRepository.sendMessage(message, null)
            assert(false) { "Expected exception to be thrown" }
        } catch (e: Exception) {
            assertEquals("Network error", e.message)
        }
    }
}
