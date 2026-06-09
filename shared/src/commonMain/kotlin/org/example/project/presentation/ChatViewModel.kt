package org.example.project.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.example.project.config.ChatAppConfig
import org.example.project.data.ChatIntent
import org.example.project.data.ChatRepository
import org.example.project.model.ChatMessage
import org.example.project.model.MessageReaction

class ChatViewModel(
    config: ChatAppConfig = ChatAppConfig()
) : ViewModel() {
    var uiState by mutableStateOf(ChatUiState())
        private set

    private val chatRepository = ChatRepository.create(config)
    private var messageId by mutableLongStateOf(0L)
    private var saveHistoryJob: Job? = null

    init {
        loadHistoryMessages()
    }

    fun updateInputText(text: String) {
        uiState = uiState.copy(inputText = text)
    }

    fun sendCurrentMessage() {
        val text = uiState.inputText.trim()
        if (text.isEmpty()) return

        sendMessage(text)
        uiState = uiState.copy(inputText = "")
    }

    fun sendFollowUp(question: String) {
        val text = question.trim()
        if (text.isEmpty()) return

        sendMessage(text)
    }

    fun sendTextFromVoice(text: String) {
        val voiceText = text.trim()
        if (voiceText.isEmpty()) return

        sendMessage(voiceText)
    }

    fun sendImageMessage(
        text: String,
        imageDataUrl: String
    ) {
        val prompt = text.trim()
        val displayText = if (prompt.isEmpty()) {
            "[图片]"
        } else {
            "[图片] $prompt"
        }
        val userMessage = ChatMessage(
            id = messageId++,
            content = displayText,
            isFromUser = true
        )
        val loadingMessage = ChatMessage(
            id = messageId++,
            content = "正在识别图片...",
            isFromUser = false,
            isLoading = true
        )

        uiState = uiState.copy(
            messages = uiState.messages + userMessage + loadingMessage,
            inputText = ""
        )
        saveHistoryMessages()

        viewModelScope.launch {
            val reply = runCatching {
                chatRepository.sendImageMessage(
                    userText = prompt,
                    imageDataUrl = imageDataUrl
                )
            }

            updateMessage(loadingMessage.id) { oldMessage ->
                reply.fold(
                    onSuccess = { chatReply ->
                        oldMessage.copy(
                            content = chatReply.content,
                            followUpQuestions = chatReply.followUpQuestions,
                            isLoading = false
                        )
                    },
                    onFailure = {
                        oldMessage.copy(
                            content = "图片识别失败，请稍后再试",
                            followUpQuestions = emptyList(),
                            isLoading = false
                        )
                    }
                )
            }
        }
    }

    private fun sendGeneratedImageMessage(prompt: String) {
        val userMessage = ChatMessage(
            id = messageId++,
            content = prompt,
            isFromUser = true
        )
        val loadingMessage = ChatMessage(
            id = messageId++,
            content = "正在生成图片...",
            isFromUser = false,
            isLoading = true
        )

        uiState = uiState.copy(
            messages = uiState.messages + userMessage + loadingMessage
        )
        saveHistoryMessages()

        viewModelScope.launch {
            val reply = runCatching {
                chatRepository.generateImage(prompt)
            }

            updateMessage(loadingMessage.id) { oldMessage ->
                reply.fold(
                    onSuccess = { chatReply ->
                        oldMessage.copy(
                            content = chatReply.content,
                            imageDataUrl = chatReply.imageDataUrl,
                            followUpQuestions = chatReply.followUpQuestions,
                            isLoading = false
                        )
                    },
                    onFailure = {
                        oldMessage.copy(
                            content = "图片生成失败，请稍后再试",
                            followUpQuestions = emptyList(),
                            isLoading = false
                        )
                    }
                )
            }
        }
    }

    private fun sendMessage(text: String) {
        val userMessage = ChatMessage(
            id = messageId++,
            content = text,
            isFromUser = true
        )

        uiState = uiState.copy(
            messages = uiState.messages + userMessage
        )
        saveHistoryMessages()

        viewModelScope.launch {
            val useWebSearch = chatRepository.shouldUseWebSearch(text)

            val intent = chatRepository.classifyTextIntent(text)

            val loadingMessage = ChatMessage(
                id = messageId++,
                content = when(intent){
                    ChatIntent.NORMAL -> "正在思考..."
                    ChatIntent.WEB_SEARCH -> "正在搜索..."
                    ChatIntent.IMAGE_GENERATION -> "正在生成图片..."
                },
                isFromUser = false,
                isLoading = true
            )

            uiState = uiState.copy(
                messages = uiState.messages + loadingMessage
            )
            saveHistoryMessages()

            val reply = runCatching {
                when(intent){
                    ChatIntent.IMAGE_GENERATION -> chatRepository.generateImage(text)
                    ChatIntent.WEB_SEARCH -> chatRepository.sendTextMessage(
                        userText = text,
                        useWebSearch = true
                    )
                    ChatIntent.NORMAL -> chatRepository.sendTextMessage(
                        userText = text,
                        useWebSearch = false
                    )
                }
            }

            updateMessage(loadingMessage.id) { oldMessage ->
                reply.fold(
                    onSuccess = { chatReply ->
                        oldMessage.copy(
                            content = chatReply.content,
                            imageDataUrl = chatReply.imageDataUrl,
                            followUpQuestions = chatReply.followUpQuestions,
                            isLoading = false
                        )
                    },
                    onFailure = { throwable ->
                        oldMessage.copy(
                            content = "请求失败：${throwable.message.orEmpty()}",
                            followUpQuestions = emptyList(),
                            isLoading = false
                        )
                    }
                )
            }
        }
    }

    fun toggleLike(message: ChatMessage) {
        updateMessage(message.id) { oldMessage ->
            oldMessage.copy(
                reaction = if (oldMessage.reaction == MessageReaction.LIKE) {
                    MessageReaction.NONE
                } else {
                    MessageReaction.LIKE
                }
            )
        }
    }

    fun toggleDislike(message: ChatMessage) {
        updateMessage(message.id) { oldMessage ->
            oldMessage.copy(
                reaction = if (oldMessage.reaction == MessageReaction.DISLIKE) {
                    MessageReaction.NONE
                } else {
                    MessageReaction.DISLIKE
                }
            )
        }
    }

    fun retryReply(message: ChatMessage) {
        val userText = findPreviousUserText(message.id)

        updateMessage(message.id) { oldMessage ->
            oldMessage.copy(
                content = "正在思考...",
                reaction = MessageReaction.NONE,
                followUpQuestions = emptyList(),
                isLoading = true
            )
        }

        viewModelScope.launch {
            val reply = runCatching {
                chatRepository.retryTextMessage(userText)
            }

            updateMessage(message.id) { oldMessage ->
                reply.fold(
                    onSuccess = { chatReply ->
                        oldMessage.copy(
                            content = chatReply.content,
                            reaction = MessageReaction.NONE,
                            imageDataUrl = chatReply.imageDataUrl,
                            followUpQuestions = chatReply.followUpQuestions,
                            isLoading = false
                        )
                    },
                    onFailure = {
                        oldMessage.copy(
                            content = "重新回复失败，请稍后再试",
                            reaction = MessageReaction.NONE,
                            followUpQuestions = emptyList(),
                            isLoading = false
                        )
                    }
                )
            }
        }
    }

    private fun updateMessage(
        messageId: Long,
        update: (ChatMessage) -> ChatMessage
    ) {
        uiState = uiState.copy(
            messages = uiState.messages.map { message ->
                if (message.id == messageId) {
                    update(message)
                } else {
                    message
                }
            }
        )
        saveHistoryMessages()
    }

    private fun findPreviousUserText(aiMessageId: Long): String {
        return uiState.messages
            .takeWhile { it.id != aiMessageId }
            .lastOrNull { it.isFromUser }
            ?.content
            .orEmpty()
    }

    private fun loadHistoryMessages() {
        viewModelScope.launch {
            val historyMessages = chatRepository.loadMessages()

            uiState = uiState.copy(
                messages = historyMessages
            )
            messageId = (historyMessages.maxOfOrNull { it.id } ?: -1L) + 1L
        }
    }

    private fun saveHistoryMessages() {
        val messages = uiState.messages

        saveHistoryJob?.cancel()
        saveHistoryJob = viewModelScope.launch {
            chatRepository.saveMessages(messages)
        }
    }
}
