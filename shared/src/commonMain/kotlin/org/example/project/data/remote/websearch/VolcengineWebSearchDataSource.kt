package org.example.project.data.remote.websearch

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class VolcengineWebSearchDataSource(
    private val apiKey: String,
    private val client: HttpClient = createDefaultClient()
) {
    suspend fun search(query: String): List<WebSearchItem> {
        require(apiKey.isNotBlank()) {
            "WEB_SEARCH_API_KEY cannot be empty"
        }

        val httpResponse = client.post(WEB_SEARCH_URL) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(
                WebSearchRequest(
                    query = query
                )
            )
        }
        val responseText = httpResponse.bodyAsText()

        require(httpResponse.status.isSuccess()) {
            "Web search HTTP ${httpResponse.status.value}: $responseText"
        }

        val response = json.decodeFromString<WebSearchResponse>(responseText)

        val error = response.responseMetadata?.error
        if (error != null) {
            error("Web search failed: ${error.code} ${error.message}")
        }

        return response.result?.webResults.orEmpty()
    }
}

private const val WEB_SEARCH_URL = "https://open.feedcoopapi.com/search_api/web_search"

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
