package org.example.project.data.remote

import org.example.project.data.ChatIntent

interface ChatRemoteDataSource {
    suspend fun sendTextMessage(userText: String): String

    suspend fun classifyTextIntent(userText: String): ChatIntent

    suspend fun retryTextMessage(userText: String): String

    suspend fun sendImageMessage(
        userText: String,
        imageDataUrl: String
    ): String

    suspend fun generateImage(prompt: String): String

    suspend fun generateFollowUpQuestions(
        userText: String,
        aiReply: String
    ): List<String>
}
