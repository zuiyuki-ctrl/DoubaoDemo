package org.example.project.data

import org.example.project.config.ChatAppConfig
import org.example.project.data.remote.tts.DoubaoTtsRemoteDataSource

class TtsRepository(
    private val remoteDataSource: DoubaoTtsRemoteDataSource
) {
    suspend fun synthesize(text: String): ByteArray {
        return remoteDataSource.synthesize(text)
    }

    companion object {
        fun create(config: ChatAppConfig): TtsRepository {
            return TtsRepository(
                remoteDataSource = DoubaoTtsRemoteDataSource(
                    appKey = config.doubaoTtsAppKey,
                    accessKey = config.doubaoTtsAccessKey,
                    resourceId = config.doubaoTtsResourceId,
                    speaker = config.doubaoTtsSpeaker
                )
            )
        }
    }
}
