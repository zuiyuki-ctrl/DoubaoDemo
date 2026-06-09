package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.config.ChatAppConfig
import org.example.project.model.ChatMessage
import org.example.project.model.NoOpOperatingHandClassifier
import org.example.project.model.OperatingHandClassifier
import org.example.project.presentation.ChatViewModel
import org.example.project.ui.chat.ChatScreen

@Composable
@Preview
fun App(
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
    MaterialTheme {
        ChatScreen(
            config = config,
            viewModel = viewModel,
            selectedImagePreview = selectedImagePreview,
            isMenuOpen = isMenuOpen,
            onSendClick = onSendClick,
            onCameraClick = onCameraClick,
            onMenuClick = onMenuClick,
            onGalleryClick = onGalleryClick,
            onFileClick = onFileClick,
            onPhoneClick = onPhoneClick,
            onSpeakClick = onSpeakClick,
            onRemoveImageClick = onRemoveImageClick,
            onVoiceRecordStart = onVoiceRecordStart,
            onVoiceRecordCancel = onVoiceRecordCancel,
            onVoiceRecordEnd = onVoiceRecordEnd,
            onTopBackClick = onTopBackClick,
            onTopSearchClick = onTopSearchClick,
            onTopMuteClick = onTopMuteClick,
            onTopMoreClick = onTopMoreClick,
            operatingHandClassifier = operatingHandClassifier
        )
    }
}
