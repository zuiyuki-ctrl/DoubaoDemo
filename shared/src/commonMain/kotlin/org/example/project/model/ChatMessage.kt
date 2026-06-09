package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Long,
    val content: String,
    val isFromUser: Boolean,
    val reaction: MessageReaction = MessageReaction.NONE,
    val followUpQuestions: List<String> = emptyList(),
    val imageDataUrl: String? = null,
    val isLoading: Boolean = false
)
