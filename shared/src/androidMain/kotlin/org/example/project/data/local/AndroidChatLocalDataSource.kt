package org.example.project.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.project.model.ChatMessage

class AndroidChatLocalDataSource(
    context: Context
) : ChatLocalDataSource {
    private val preferences = context.applicationContext.getSharedPreferences(
        CHAT_HISTORY_PREFERENCES,
        Context.MODE_PRIVATE
    )
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun loadMessages(): List<ChatMessage> {
        return withContext(Dispatchers.IO) {
            val rawJson = preferences.getString(CHAT_HISTORY_KEY, null)
                ?: return@withContext emptyList()

            runCatching {
                json.decodeFromString(
                    ListSerializer(ChatMessage.serializer()),
                    rawJson
                )
            }.getOrDefault(emptyList())
        }
    }

    override suspend fun saveMessages(messages: List<ChatMessage>) {
        withContext(Dispatchers.IO) {
            val rawJson = json.encodeToString(
                ListSerializer(ChatMessage.serializer()),
                messages
            )

            preferences.edit()
                .putString(CHAT_HISTORY_KEY, rawJson)
                .apply()
        }
    }
}

private const val CHAT_HISTORY_PREFERENCES = "chat_history_preferences"
private const val CHAT_HISTORY_KEY = "chat_history"
