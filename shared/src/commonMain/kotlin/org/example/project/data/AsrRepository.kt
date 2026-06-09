package org.example.project.data

import org.example.project.config.ChatAppConfig
import org.example.project.data.remote.asr.DoubaoAsrRemoteDataSource

class AsrRepository(
    private val remoteDataSource: DoubaoAsrRemoteDataSource
) {
    suspend fun recognize(audioBytes: ByteArray): String {
        return remoteDataSource.recognize(audioBytes)
    }

    companion object {
        fun create(config: ChatAppConfig): AsrRepository {
            return AsrRepository(
                remoteDataSource = DoubaoAsrRemoteDataSource(
                    appKey = config.doubaoAsrAppKey,
                    accessKey = config.doubaoAsrAccessKey,
                    resourceId = config.doubaoAsrResourceId
                )
            )
        }
    }
}