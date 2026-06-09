package org.example.project.data.remote.asr

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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DoubaoAsrRemoteDataSource(
    private val appKey: String,
    private val accessKey: String,
    private val resourceId: String,
    private val client: HttpClient = createDefaultClient()
) {
    @OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)
    suspend fun recognize(audioBytes: ByteArray): String {
        require(appKey.isNotBlank()) {
            "DOUBAO_ASR_APP_KEY 不能为空"
        }
        require(accessKey.isNotBlank()) {
            "DOUBAO_ASR_ACCESS_KEY 不能为空"
        }
        require(audioBytes.isNotEmpty()) {
            "音频文件为空"
        }

        val response = client.post(DOUBAO_ASR_FLASH_URL) {
            contentType(ContentType.Application.Json)
            header("X-Api-App-Key", appKey)
            header("X-Api-Access-Key", accessKey)
            header("X-Api-Resource-Id", resourceId)
            header("X-Api-Request-Id", Uuid.random().toString())
            header("X-Api-Sequence", "-1")
            setBody(
                DoubaoAsrRequest(
                    user = DoubaoAsrUser(uid = appKey),
                    audio = DoubaoAsrAudio(
                        data = Base64.encode(audioBytes)
                    )
                )
            )
        }

        val responseText = response.bodyAsText()

        require(response.status.isSuccess()) {
            "ASR HTTP ${response.status.value}: $responseText"
        }

        val statusCode = response.headers["X-Api-Status-Code"]
        val message = response.headers["X-Api-Message"]

        require(statusCode == "20000000") {
            "ASR failed: code=$statusCode, message=$message, body=$responseText"
        }

        val asrResponse = json.decodeFromString<DoubaoAsrResponse>(responseText)
        val text = asrResponse.result?.text.orEmpty().trim()

        require(text.isNotBlank()) {
            "ASR recognized empty text"
        }

        return text
    }
}

private const val DOUBAO_ASR_FLASH_URL =
    "https://openspeech.bytedance.com/api/v3/auc/bigmodel/recognize/flash"

private val json = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalSerializationApi::class)
private fun createDefaultClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }
}