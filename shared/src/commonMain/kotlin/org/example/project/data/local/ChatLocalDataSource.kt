package org.example.project.data.local

import org.example.project.model.ChatMessage

interface ChatLocalDataSource {
    suspend fun loadMessages(): List<ChatMessage>

    suspend fun saveMessages(messages: List<ChatMessage>)
}

class MemoryChatLocalDataSource : ChatLocalDataSource {
    override suspend fun loadMessages(): List<ChatMessage> {
        return emptyList()
    }

    override suspend fun saveMessages(messages: List<ChatMessage>) {
    }
}
