package org.example.project.data

import org.example.project.config.ChatAppConfig
import org.example.project.data.local.ChatLocalDataSource
import org.example.project.data.local.MemoryChatLocalDataSource
import org.example.project.data.remote.ChatRemoteDataSource
import org.example.project.data.remote.FakeChatRemoteDataSource
import org.example.project.data.remote.doubao.DoubaoRemoteDataSource
import org.example.project.data.remote.websearch.VolcengineWebSearchDataSource
import org.example.project.data.remote.websearch.WebSearchItem
import org.example.project.model.ChatMessage

data class ChatReply(
    val content: String,
    val followUpQuestions: List<String>,
    val imageDataUrl: String? = null
)

class ChatRepository(
    private val remoteDataSource: ChatRemoteDataSource = FakeChatRemoteDataSource(),
    private val localDataSource: ChatLocalDataSource = MemoryChatLocalDataSource(),
    private val webSearchDataSource: VolcengineWebSearchDataSource? = null
) {
    suspend fun loadMessages(): List<ChatMessage> {
        return localDataSource.loadMessages()
    }

    suspend fun saveMessages(messages: List<ChatMessage>) {
        localDataSource.saveMessages(
            messages = messages.filter { !it.isLoading }
        )
    }

    suspend fun sendTextMessage(
        userText: String,
        useWebSearch: Boolean
    ): ChatReply {
        if (useWebSearch && webSearchDataSource != null) {
            return sendWebSearchMessage(userText)
        }

        val replyContent = remoteDataSource.sendTextMessage(userText)

        return ChatReply(
            content = replyContent,
            followUpQuestions = generateFollowUpQuestions(
                userText = userText,
                aiReply = replyContent
            )
        )
    }

    suspend fun sendTextMessage(userText: String): ChatReply {
        return sendTextMessage(
            userText = userText,
            useWebSearch = shouldUseWebSearch(userText))
    }

    suspend fun shouldUseWebSearch(text: String): Boolean {
        if (webSearchDataSource == null) {
            return false
        }

        return classifyTextIntent(text) == ChatIntent.WEB_SEARCH
    }

    suspend fun classifyTextIntent(text: String): ChatIntent {
        return runCatching {
            remoteDataSource.classifyTextIntent(text)
        }.getOrElse {
            ChatIntent.NORMAL
        }
    }

    suspend fun retryTextMessage(userText: String): ChatReply {
        val replyContent = remoteDataSource.retryTextMessage(userText)

        return ChatReply(
            content = replyContent,
            followUpQuestions = generateFollowUpQuestions(
                userText = userText,
                aiReply = replyContent
            )
        )
    }

    suspend fun sendImageMessage(
        userText: String,
        imageDataUrl: String
    ): ChatReply {
        val prompt = userText.ifBlank {
            "请描述这张图片"
        }
        val replyContent = remoteDataSource.sendImageMessage(
            userText = prompt,
            imageDataUrl = imageDataUrl
        )

        return ChatReply(
            content = replyContent,
            followUpQuestions = generateFollowUpQuestions(
                userText = prompt,
                aiReply = replyContent
            )
        )
    }

    suspend fun generateImage(prompt: String): ChatReply {
        val imageDataUrl = remoteDataSource.generateImage(prompt)

        return ChatReply(
            content = "已为你生成图片",
            followUpQuestions = emptyList(),
            imageDataUrl = imageDataUrl
        )
    }

    private suspend fun generateFollowUpQuestions(
        userText: String,
        aiReply: String
    ): List<String> {
        return runCatching {
            remoteDataSource.generateFollowUpQuestions(
                userText = userText,
                aiReply = aiReply
            )
        }.getOrElse {
            listOf(
                "可以举个例子吗？",
                "这个问题还有什么需要注意的？"
            )
        }
    }

    private suspend fun sendWebSearchMessage(userText: String): ChatReply {
        val searchResults = webSearchDataSource
            ?.search(userText)
            .orEmpty()

        if (searchResults.isEmpty()) {
            return ChatReply(
                content = "我没有搜索到足够可靠的联网结果，你可以换个问法再试一次。",
                followUpQuestions = listOf(
                    "可以换个关键词搜索吗？",
                    "能帮我缩小范围吗？"
                )
            )
        }

        val replyContent = remoteDataSource.sendTextMessage(
            userText = buildWebSearchPrompt(
                userText = userText,
                searchResults = searchResults
            )
        )

        val searchTip = "搜索 1 个关键词，参考 ${searchResults.size} 篇资料 >"
        val displayContent = "$searchTip\n\n$replyContent"

        return ChatReply(
            content = displayContent,
            followUpQuestions = generateFollowUpQuestions(
                userText = userText,
                aiReply = replyContent
            )
        )
    }

    companion object {
        fun create(config: ChatAppConfig): ChatRepository {
            val remoteDataSource = if (
                config.doubaoApiKey.isNotBlank() &&
                config.doubaoModel.isNotBlank()
            ) {
                DoubaoRemoteDataSource(
                    apiKey = config.doubaoApiKey,
                    model = config.doubaoModel,
                    visionModel = config.doubaoVisionModel,
                    imageModel = config.doubaoImageModel
                )
            } else {
                FakeChatRemoteDataSource()
            }

            return ChatRepository(
                remoteDataSource = remoteDataSource,
                localDataSource = config.localDataSource,
                webSearchDataSource = config.createWebSearchDataSource()
            )
        }
    }
}

private fun ChatAppConfig.createWebSearchDataSource(): VolcengineWebSearchDataSource? {
    if (webSearchApiKey.isBlank()) return null

    return VolcengineWebSearchDataSource(
        apiKey = webSearchApiKey
    )
}



private fun buildWebSearchPrompt(
    userText: String,
    searchResults: List<WebSearchItem>
): String {
    val sourceText = searchResults
        .take(5)
        .mapIndexed { index, item ->
            val content = item.summary
                ?: item.content
                ?: item.snippet
            """
                [${index + 1}]
                title: ${item.title}
                site: ${item.siteName.orEmpty()}
                url: ${item.url.orEmpty()}
                publish_time: ${item.publishTime.orEmpty()}
                content: $content
            """.trimIndent()
        }
        .joinToString(separator = "\n\n")

    return """
        用户问题：
        $userText

        下面是联网搜索结果。请只基于这些结果回答用户问题。
        如果搜索结果里没有足够信息，请明确说“搜索结果不足以确认”。
        回答要自然、简洁，并在末尾列出参考来源标题。

        搜索结果：
        $sourceText
    """.trimIndent()
}
