package org.example.project.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.ic_check
import kotlinproject.shared.generated.resources.ic_copy
import kotlinproject.shared.generated.resources.ic_dislike
import kotlinproject.shared.generated.resources.ic_dislike_selected
import kotlinproject.shared.generated.resources.ic_like
import kotlinproject.shared.generated.resources.ic_like_selected
import kotlinproject.shared.generated.resources.ic_retry
import kotlinproject.shared.generated.resources.ic_tts
import kotlinx.coroutines.delay
import org.example.project.model.ChatMessage
import org.example.project.model.MessageReaction
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MessageBubble(
    message: ChatMessage,
    onLikeClick: (ChatMessage) -> Unit,
    onDislikeClick: (ChatMessage) -> Unit,
    onRetryClick: (ChatMessage) -> Unit,
    onSpeakClick: (ChatMessage) -> Unit,
    onFollowUpClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Surface(
            color = if (message.isFromUser) Color(0xFFCDEBFF) else Color(0xFFF2F3F5),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            if (message.isFromUser) {
                MessageText(message.content)
            } else {
                Column {
                    MessageText(
                        content = message.content,
                        isLoading = message.isLoading
                    )

                    if (message.imageDataUrl != null) {
                        GeneratedImage(imageDataUrl = message.imageDataUrl)
                    }

                    if (!message.isLoading) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = Color(0xFFE0E3E7)
                        )

                        MessageActionBar(
                            message = message,
                            onLikeClick = onLikeClick,
                            onDislikeClick = onDislikeClick,
                            onRetryClick = onRetryClick,
                            onSpeakClick = onSpeakClick
                        )

                        FollowUpQuestions(
                            questions = message.followUpQuestions,
                            onFollowUpClick = onFollowUpClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedImage(imageDataUrl: String) {
    val imageBitmap = remember(imageDataUrl) {
        imageBitmapFromDataUrl(imageDataUrl)
    } ?: return

    Image(
        bitmap = imageBitmap,
        contentDescription = "AI 生成图片",
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun FollowUpQuestions(
    questions: List<String>,
    onFollowUpClick: (String) -> Unit
) {
    if (questions.isEmpty()) return

    Column(
        modifier = Modifier.padding(
            start = 12.dp,
            end = 12.dp,
            bottom = 10.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        questions.forEach { question ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(10.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clickable { onFollowUpClick(question) }
            ) {
                Text(
                    text = question,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Color(0xFF222222),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MessageText(
    content: String,
    isLoading: Boolean = false
) {
    Text(
        text = content,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        color = if (isLoading) Color(0xFF8A8F98) else Color(0xFF111111),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun MessageActionBar(
    message: ChatMessage,
    onLikeClick: (ChatMessage) -> Unit,
    onDislikeClick: (ChatMessage) -> Unit,
    onRetryClick: (ChatMessage) -> Unit,
    onSpeakClick: (ChatMessage) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember(message.id) { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1200)
            copied = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionIconButton(
            icon = if (copied) Res.drawable.ic_check else Res.drawable.ic_copy,
            isSelected = copied,
            contentDescription = "复制",
            onClick = {
                clipboardManager.setText(AnnotatedString(message.content))
                copied = true
            }
        )

        ActionIconButton(
            icon = Res.drawable.ic_tts,
            isSelected = false,
            contentDescription = "语音播报",
            onClick = { onSpeakClick(message) }
        )

        ActionIconButton(
            icon = if (message.reaction == MessageReaction.LIKE) {
                Res.drawable.ic_like_selected
            } else {
                Res.drawable.ic_like
            },
            isSelected = message.reaction == MessageReaction.LIKE,
            contentDescription = "点赞",
            onClick = { onLikeClick(message) }
        )

        ActionIconButton(
            icon = if (message.reaction == MessageReaction.DISLIKE) {
                Res.drawable.ic_dislike_selected
            } else {
                Res.drawable.ic_dislike
            },
            isSelected = message.reaction == MessageReaction.DISLIKE,
            contentDescription = "点踩",
            onClick = { onDislikeClick(message) }
        )

        Spacer(modifier = Modifier.weight(1f))

        ActionIconButton(
            icon = Res.drawable.ic_retry,
            isSelected = false,
            contentDescription = "重新回复",
            onClick = { onRetryClick(message) }
        )
    }
}

@Composable
private fun ActionIconButton(
    icon: DrawableResource,
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Image(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    Color(0xFFDCEBFF)
                } else {
                    Color(0xFFEAF4FF)
                }
            )
            .clickable(onClick = onClick)
            .padding(2.dp)
    )
}
