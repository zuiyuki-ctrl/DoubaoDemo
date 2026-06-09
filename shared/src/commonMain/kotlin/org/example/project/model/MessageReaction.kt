package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageReaction {
    NONE,
    LIKE,
    DISLIKE
}
