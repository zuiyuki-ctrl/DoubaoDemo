package org.example.project.data.remote

import kotlinx.coroutines.delay
import org.example.project.data.ChatIntent

class FakeChatRemoteDataSource : ChatRemoteDataSource {
    override suspend fun sendTextMessage(userText: String): String {
        delay(600)

        return "这是 AI 的模拟回复：$userText"
    }

    override suspend fun classifyTextIntent(userText: String): ChatIntent {
        return ChatIntent.NORMAL
    }

    override suspend fun retryTextMessage(userText: String): String {
        delay(600)

        return "重新生成的模拟回复：$userText"
    }

    override suspend fun sendImageMessage(
        userText: String,
        imageDataUrl: String
    ): String {
        delay(600)

        return "这是图片识别的模拟回复：我已经收到了图片，你的问题是「$userText」。"
    }

    override suspend fun generateImage(prompt: String): String {
        delay(600)

        return FAKE_IMAGE_DATA_URL
    }

    override suspend fun generateFollowUpQuestions(
        userText: String,
        aiReply: String
    ): List<String> {
        return listOf(
            "可以举个例子吗？",
            "这个问题还有什么需要注意的？"
        )
    }
}

private const val FAKE_IMAGE_DATA_URL =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
