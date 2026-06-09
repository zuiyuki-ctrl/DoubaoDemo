package org.example.project.data.remote.tts

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class DoubaoTtsRemoteDataSource(
    private val appKey: String,
    private val accessKey: String,
    private val resourceId: String,
    private val speaker: String,
    private val client: HttpClient = createDefaultClient()
) {
    suspend fun synthesize(text: String): ByteArray {
        require(appKey.isNotBlank()) {
            "DOUBAO_TTS_APP_KEY 不能为空"
        }
        require(accessKey.isNotBlank()) {
            "DOUBAO_TTS_ACCESS_KEY 不能为空"
        }

        val response = client.post(DOUBAO_TTS_UNIDIRECTIONAL_URL) {
            contentType(ContentType.Application.Json)
            header("X-Api-App-Key", appKey)
            header("X-Api-Access-Key", accessKey)
            header("X-Api-Resource-Id", resourceId)
            setBody(
                DoubaoTtsRequest(
                    reqParams = DoubaoTtsReqParams(
                        text = text,
                        speaker = speaker
                    )
                )
            )
        }
        val responseText = response.bodyAsText()

        require(response.status.isSuccess()) {
            "TTS HTTP ${response.status.value}: $responseText"
        }
        responseText.throwIfTtsError()

        return responseText.extractAudioBytes()
    }
}

private const val DOUBAO_TTS_UNIDIRECTIONAL_URL =
    "https://openspeech.bytedance.com/api/v3/tts/unidirectional"

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

private fun String.throwIfTtsError() {
    val code = Regex(""""code"\s*:\s*(\d+)"""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
    val message = Regex(""""message"\s*:\s*"([^"]*)"""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)

    if (code != null && code != 0) {
        error("TTS 服务返回错误 code=$code, message=$message")
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun String.extractAudioBytes(): ByteArray {
    val audioBase64Chunks = Regex(""""data"\s*:\s*"([^"]+)"""")
        .findAll(this)
        .map { it.groupValues[1] }
        .filter { it.isNotBlank() }
        .toList()

    require(audioBase64Chunks.isNotEmpty()) {
        "TTS 接口没有返回音频数据：$this"
    }

    return audioBase64Chunks
        .map { Base64.decode(it) }
        .fold(ByteArray(0)) { total, chunk -> total + chunk }
}
