package org.example.project.config

import org.example.project.data.local.ChatLocalDataSource
import org.example.project.data.local.MemoryChatLocalDataSource

data class ChatAppConfig(
    val doubaoApiKey: String = "",
    val doubaoModel: String = "",
    val doubaoVisionModel: String = "doubao-seed-2-0-mini-260428",
    val doubaoImageModel: String = "doubao-seedream-5-0-260128",
    val doubaoTtsAppKey: String = "",
    val doubaoTtsAccessKey: String = "",
    val doubaoTtsResourceId: String = "seed-tts-2.0",
    val doubaoTtsSpeaker: String = "zh_female_vv_uranus_bigtts",
    val webSearchApiKey: String = "",
    val webSearchApiKeyId: String = "",
    val localDataSource: ChatLocalDataSource = MemoryChatLocalDataSource(),
    val doubaoAsrAppKey: String = "",
    val doubaoAsrAccessKey: String = "",
    val doubaoAsrResourceId: String = "volc.bigasr.auc_turbo",
)
