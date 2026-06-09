package org.example.project.data.remote.doubao

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoubaoChatRequest(
    val model: String,
    val messages: List<DoubaoMessage>,
    val stream: Boolean = false
)

@Serializable
data class DoubaoMessage(
    val role: String,
    val content: String
)

@Serializable
data class DoubaoVisionChatRequest(
    val model: String,
    val messages: List<DoubaoVisionMessage>,
    val stream: Boolean = false
)

@Serializable
data class DoubaoVisionMessage(
    val role: String,
    val content: List<DoubaoVisionContentPart>
)

@Serializable
data class DoubaoVisionContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url")
    val imageUrl: DoubaoImageUrl? = null
)

@Serializable
data class DoubaoImageUrl(
    val url: String
)

@Serializable
data class DoubaoChatResponse(
    val choices: List<DoubaoChoice> = emptyList()
)

@Serializable
data class DoubaoChoice(
    val message: DoubaoMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class DoubaoImageGenerationRequest(
    val model: String,
    val prompt: String,
    val size: String = "2K",
    @SerialName("response_format")
    val responseFormat: String = "url",
    @SerialName("output_format")
    val outputFormat: String = "png",
    val watermark: Boolean = false
)

@Serializable
data class DoubaoImageGenerationResponse(
    val data: List<DoubaoGeneratedImage> = emptyList()
)

@Serializable
data class DoubaoGeneratedImage(
    val url: String? = null,
    @SerialName("b64_json")
    val b64Json: String? = null
)
