package org.example.project.data.remote.doubao

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.example.project.data.ChatIntent
import org.example.project.data.remote.ChatRemoteDataSource
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DoubaoRemoteDataSource(
    private val apiKey: String,
    private val model: String,
    private val visionModel: String,
    private val imageModel: String,
    private val client: HttpClient = createDefaultClient()
) : ChatRemoteDataSource {
    override suspend fun sendTextMessage(userText: String): String {
        return requestChat(userText)
    }

    override suspend fun classifyTextIntent(userText: String): ChatIntent {
        val prompt = """
        请判断下面用户问题属于哪一种意图。

        IMAGE_GENERATION：
        - 用户想生成图片、画图、出图、做一张图
        - 例如：生成一张小狗图片、画一张赛博朋克城市、帮我做一张海报

        WEB_SEARCH：
        - 用户需要今天、现在、实时、最新的信息
        - 用户问天气、新闻、热搜、股票、比赛结果
        - 用户明确要求搜索、查一下、联网

        NORMAL：
        - 普通常识解释
        - 编程概念
        - 写作、翻译、总结
        - 不需要实时信息，也不是生图

        用户问题：
        $userText

        只输出一个词：
        NORMAL 或 WEB_SEARCH 或 IMAGE_GENERATION
    """.trimIndent()

        val result = requestChat(
            userText = prompt,
            systemPrompt = "你是一个用户意图判断器，只能输出 NORMAL、WEB_SEARCH 或 IMAGE_GENERATION。"
        ).trim().uppercase()

        return when {
            result.contains("IMAGE_GENERATION") -> ChatIntent.IMAGE_GENERATION
            result.contains("WEB_SEARCH") -> ChatIntent.WEB_SEARCH
            else -> ChatIntent.NORMAL
        }
    }

    override suspend fun retryTextMessage(userText: String): String {
        return requestChat(userText)
    }

    override suspend fun sendImageMessage(
        userText: String,
        imageDataUrl: String
    ): String {
        val prompt = userText.ifBlank {
            "请描述这张图片"
        }

        val response = client.post(DOUBAO_CHAT_COMPLETIONS_URL) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(
                DoubaoVisionChatRequest(
                    model = visionModel,
                    messages = listOf(
                        DoubaoVisionMessage(
                            role = "user",
                            content = listOf(
                                DoubaoVisionContentPart(
                                    type = "image_url",
                                    imageUrl = DoubaoImageUrl(url = imageDataUrl)
                                ),
                                DoubaoVisionContentPart(
                                    type = "text",
                                    text = prompt
                                )
                            )
                        )
                    )
                )
            )
        }.body<DoubaoChatResponse>()

        return response.choices
            .firstOrNull()
            ?.message
            ?.content
            ?.takeIf { it.isNotBlank() }
            ?: "AI 暂时没有返回图片识别内容"
    }

    override suspend fun generateImage(prompt: String): String {
        require(imageModel.isNotBlank()) {
            "DOUBAO_IMAGE_MODEL 不能为空"
        }

        val response = client.post(DOUBAO_IMAGE_GENERATIONS_URL) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(
                DoubaoImageGenerationRequest(
                    model = imageModel,
                    prompt = prompt
                )
            )
        }.body<DoubaoImageGenerationResponse>()

        val generatedImage = response.data.firstOrNull()
        val b64Json = generatedImage?.b64Json
        if (!b64Json.isNullOrBlank()) {
            return "data:image/png;base64,$b64Json"
        }

        val imageUrl = generatedImage?.url?.takeIf { it.isNotBlank() }
            ?: error("图片生成接口没有返回图片")

        val imageBytes = client.get(imageUrl).body<ByteArray>()
        return imageBytes.toPngDataUrl()
    }

    override suspend fun generateFollowUpQuestions(
        userText: String,
        aiReply: String
    ): List<String> {
        val prompt = """
            请根据下面的用户问题和 AI 回复，生成 2 个适合继续追问的问题。
            要求：
            1. 每行只输出一个问题
            2. 不要输出序号之外的解释
            3. 问题要简短自然，像用户会直接点击继续问的话

            用户问题：
            $userText

            AI 回复：
            $aiReply
        """.trimIndent()

        return requestChat(
            userText = prompt,
            systemPrompt = "你负责生成聊天追问问题，只输出问题本身。"
        )
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.removePrefix("-").trim() }
            .map { it.replace(Regex("^\\d+[.、)]\\s*"), "") }
            .filter { it.isNotBlank() }
            .take(2)
            .toList()
    }

    private suspend fun requestChat(
        userText: String,
        systemPrompt: String = "你是一个简洁、友好的 AI 智能助手。"
    ): String {
        val response = client.post(DOUBAO_CHAT_COMPLETIONS_URL) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(
                DoubaoChatRequest(
                    model = model,
                    messages = listOf(
                        DoubaoMessage(
                            role = "system",
                            content = systemPrompt
                        ),
                        DoubaoMessage(
                            role = "user",
                            content = userText
                        )
                    )
                )
            )
        }.body<DoubaoChatResponse>()

        return response.choices
            .firstOrNull()
            ?.message
            ?.content
            ?.takeIf { it.isNotBlank() }
            ?: "AI 暂时没有返回内容"
    }
}

private const val DOUBAO_CHAT_COMPLETIONS_URL =
    "https://ark.cn-beijing.volces.com/api/v3/chat/completions"

private const val DOUBAO_IMAGE_GENERATIONS_URL =
    "https://ark.cn-beijing.volces.com/api/v3/images/generations"

@OptIn(ExperimentalSerializationApi::class)
private fun createDefaultClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    explicitNulls = false
                    ignoreUnknownKeys = true
                }
            )
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.toPngDataUrl(): String {
    return "data:image/png;base64,${Base64.encode(this)}"
}
