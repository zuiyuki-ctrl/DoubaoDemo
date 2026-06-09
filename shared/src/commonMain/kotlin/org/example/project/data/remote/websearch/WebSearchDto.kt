package org.example.project.data.remote.websearch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebSearchRequest(
    @SerialName("Query")
    val query: String,
    @SerialName("SearchType")
    val searchType: String = "web",
    @SerialName("Count")
    val count: Int = 5,
    @SerialName("Filter")
    val filter: WebSearchFilter = WebSearchFilter(),
    @SerialName("NeedSummary")
    val needSummary: Boolean = true,
    @SerialName("QueryControl")
    val queryControl: WebSearchQueryControl = WebSearchQueryControl(),
    @SerialName("ContentFormats")
    val contentFormats: String = "text"
)

@Serializable
data class WebSearchFilter(
    @SerialName("NeedUrl")
    val needUrl: Boolean = true
)

@Serializable
data class WebSearchQueryControl(
    @SerialName("QueryRewrite")
    val queryRewrite: Boolean = true
)

@Serializable
data class WebSearchResponse(
    @SerialName("ResponseMetadata")
    val responseMetadata: WebSearchResponseMetadata? = null,
    @SerialName("Result")
    val result: WebSearchResult? = null
)

@Serializable
data class WebSearchResponseMetadata(
    @SerialName("Error")
    val error: WebSearchError? = null
)

@Serializable
data class WebSearchError(
    @SerialName("Code")
    val code: String? = null,
    @SerialName("Message")
    val message: String? = null
)

@Serializable
data class WebSearchResult(
    @SerialName("WebResults")
    val webResults: List<WebSearchItem>? = null
)

@Serializable
data class WebSearchItem(
    @SerialName("Title")
    val title: String = "",
    @SerialName("SiteName")
    val siteName: String? = null,
    @SerialName("Url")
    val url: String? = null,
    @SerialName("Snippet")
    val snippet: String = "",
    @SerialName("Summary")
    val summary: String? = null,
    @SerialName("Content")
    val content: String? = null,
    @SerialName("PublishTime")
    val publishTime: String? = null
)
