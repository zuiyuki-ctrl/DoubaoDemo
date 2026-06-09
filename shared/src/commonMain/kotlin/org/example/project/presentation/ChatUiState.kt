package org.example.project.presentation

import org.example.project.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = ""
)
