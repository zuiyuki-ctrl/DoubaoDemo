package org.example.project.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.ic_back
import kotlinproject.shared.generated.resources.ic_no_speaker
import kotlinproject.shared.generated.resources.ic_search
import kotlinproject.shared.generated.resources.ic_setting
import kotlinproject.shared.generated.resources.ic_speaker
import kotlinx.coroutines.launch
import org.example.project.config.ChatAppConfig
import org.example.project.model.ChatMessage
import org.example.project.model.NoOpOperatingHandClassifier
import org.example.project.model.OperatingHand
import org.example.project.model.OperatingHandClassifier
import org.example.project.model.OperatingHandTouchPoint
import org.example.project.presentation.ChatViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun ChatScreen(
    config: ChatAppConfig = ChatAppConfig(),
    viewModel: ChatViewModel = viewModel { ChatViewModel(config) },
    selectedImagePreview: ImageBitmap? = null,
    isMenuOpen: Boolean = false,
    onSendClick: () -> Unit = viewModel::sendCurrentMessage,
    onCameraClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onFileClick: () -> Unit = {},
    onPhoneClick: () -> Unit = {},
    onSpeakClick: (ChatMessage) -> Unit = {},
    onRemoveImageClick: () -> Unit = {},
    onVoiceRecordStart: () -> Unit = {},
    onVoiceRecordEnd: () -> Unit = {},
    onVoiceRecordCancel: () -> Unit = {},
    onTopBackClick: () -> Unit = {},
    onTopSearchClick: () -> Unit = {},
    onTopMuteClick: () -> Unit = {},
    onTopMoreClick: () -> Unit = {},
    operatingHandClassifier: OperatingHandClassifier = NoOpOperatingHandClassifier
) {
    val uiState = viewModel.uiState
    val coroutineScope = rememberCoroutineScope()
    var operatingHand by remember { mutableStateOf(OperatingHand.RIGHT) }
    var operatingHandDebugText by remember {
        mutableStateOf("Hand: waiting score=-- points=0 used=false")
    }
    var operatingHandFeatureText by remember {
        mutableStateOf("Track: avgX=-- startX=-- endX=-- avgY=-- duration=--")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .operatingHandTracking(enabled = true) { points ->
                coroutineScope.launch {
                    val prediction = operatingHandClassifier.classify(points)
                    val shouldUsePrediction =
                        prediction.hand != OperatingHand.UNKNOWN && prediction.score >= 0.55f

                    operatingHandDebugText = "Hand: ${prediction.hand.name} " +
                        "score=${prediction.score.formatScore()} " +
                        "points=${points.size} " +
                        "used=$shouldUsePrediction"
                    operatingHandFeatureText = points.toDebugSummary()

                    if (shouldUsePrediction) {
                        operatingHand = prediction.hand
                    }
                }
            }
            .background(Color(0xFFF7F8FA))
    ) {
        ChatTopBar(
            onBackClick = onTopBackClick,
            onSearchClick = onTopSearchClick,
            onMuteClick = onTopMuteClick,
            onMoreClick = onTopMoreClick
        )

        ChatMessageList(
            messages = uiState.messages,
            modifier = Modifier.weight(1f),
            onLikeClick = viewModel::toggleLike,
            onDislikeClick = viewModel::toggleDislike,
            onRetryClick = viewModel::retryReply,
            onSpeakClick = onSpeakClick,
            onFollowUpClick = viewModel::sendFollowUp
        )

        Text(
            text = operatingHandDebugText,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF7D6))
                .padding(horizontal = 12.dp, vertical = 3.dp),
            color = Color(0xFF7A4D00),
            fontSize = 12.sp
        )
        Text(
            text = operatingHandFeatureText,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF7D6))
                .padding(horizontal = 12.dp, vertical = 3.dp),
            color = Color(0xFF7A4D00),
            fontSize = 12.sp
        )

        ChatInputBar(
            text = uiState.inputText,
            selectedImagePreview = selectedImagePreview,
            isMenuOpen = isMenuOpen,
            onTextChange = viewModel::updateInputText,
            onSendClick = onSendClick,
            onCameraClick = onCameraClick,
            onMenuClick = onMenuClick,
            onRemoveImageClick = onRemoveImageClick,
            onVoiceRecordStart = onVoiceRecordStart,
            onVoiceRecordCancel = onVoiceRecordCancel,
            onVoiceRecordEnd = onVoiceRecordEnd,
            operatingHand = operatingHand
        )

        if (isMenuOpen) {
            ChatToolMenu(
                onCameraClick = onCameraClick,
                onGalleryClick = onGalleryClick,
                onFileClick = onFileClick,
                onPhoneClick = onPhoneClick
            )
        }
    }
}

private fun Float.formatScore(): String {
    return ((this * 100).toInt() / 100f).toString()
}

private fun List<OperatingHandTouchPoint>.toDebugSummary(): String {
    if (isEmpty()) {
        return "Track: avgX=-- startX=-- endX=-- avgY=-- duration=--"
    }

    val normalizedX = map { point ->
        point.x / point.width.coerceAtLeast(1f)
    }
    val normalizedY = map { point ->
        point.y / point.height.coerceAtLeast(1f)
    }
    val duration = (maxOf { it.downTimeDeltaMillis } - minOf { it.downTimeDeltaMillis })
        .coerceAtLeast(0f)

    return "Track: avgX=${normalizedX.average().toFloat().formatScore()} " +
        "startX=${normalizedX.first().formatScore()} " +
        "endX=${normalizedX.last().formatScore()} " +
        "avgY=${normalizedY.average().toFloat().formatScore()} " +
        "duration=${duration.toInt()}ms"
}

@Composable
private fun ChatTopBar(
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMuteClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopBarIconButton(
            icon = Res.drawable.ic_back,
            contentDescription = "back",
            onClick = onBackClick
        )
        Spacer(modifier = Modifier.width(4.dp))
        TopBarIconButton(
            icon = Res.drawable.ic_search,
            contentDescription = "search",
            onClick = onSearchClick
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "\u8c46\u5305",
                    color = Color(0xFF111111),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = " \u203a",
                    color = Color(0xFFB8B8B8),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "\u5185\u5bb9\u7531 AI \u751f\u6210",
                color = Color(0xFF9E9E9E),
                fontSize = 12.sp
            )
        }

        TopBarIconButton(
            icon = if (isMuted) Res.drawable.ic_no_speaker else Res.drawable.ic_speaker,
            contentDescription = if (isMuted) "no speaker" else "speaker",
            onClick = {
                isMuted = !isMuted
                onMuteClick()
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        TopBarIconButton(
            icon = Res.drawable.ic_setting,
            contentDescription = "setting",
            onClick = onMoreClick
        )
    }
}

@Composable
private fun TopBarIconButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(34.dp)
        )
    }
}
