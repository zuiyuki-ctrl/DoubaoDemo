package org.example.project.data.remote.asr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoubaoAsrRequest(
    val user: DoubaoAsrUser,
    val audio: DoubaoAsrAudio,
    val request: DoubaoAsrRequestConfig = DoubaoAsrRequestConfig()
)

@Serializable
data class DoubaoAsrUser(
    val uid: String
)

@Serializable
data class DoubaoAsrAudio(
    val data: String
)

@Serializable
data class DoubaoAsrRequestConfig(
    @SerialName("model_name")
    val modelName: String = "bigmodel",
    @SerialName("enable_punc")
    val enablePunc: Boolean = true,
    @SerialName("enable_itn")
    val enableItn: Boolean = true
)

@Serializable
data class DoubaoAsrResponse(
    val result: DoubaoAsrResult? = null
)

@Serializable
data class DoubaoAsrResult(
    val text: String = ""
)