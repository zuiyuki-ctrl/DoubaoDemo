package org.example.project.data.remote.tts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoubaoTtsRequest(
    val user: DoubaoTtsUser = DoubaoTtsUser(),
    @SerialName("req_params")
    val reqParams: DoubaoTtsReqParams
)

@Serializable
data class DoubaoTtsUser(
    val uid: String = "doubao-demo"
)

@Serializable
data class DoubaoTtsReqParams(
    val text: String,
    val speaker: String,
    @SerialName("audio_params")
    val audioParams: DoubaoTtsAudioParams = DoubaoTtsAudioParams()
)

@Serializable
data class DoubaoTtsAudioParams(
    val format: String = "mp3",
    @SerialName("sample_rate")
    val sampleRate: Int = 24000
)
