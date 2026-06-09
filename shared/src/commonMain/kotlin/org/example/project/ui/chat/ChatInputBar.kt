package org.example.project.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.ic_camera
import kotlinproject.shared.generated.resources.ic_keyboard
import kotlinproject.shared.generated.resources.ic_menu
import kotlinproject.shared.generated.resources.ic_menu_album
import kotlinproject.shared.generated.resources.ic_menu_camera
import kotlinproject.shared.generated.resources.ic_menu_file
import kotlinproject.shared.generated.resources.ic_menu_phone
import kotlinproject.shared.generated.resources.ic_send
import kotlinproject.shared.generated.resources.ic_sound
import org.example.project.model.OperatingHand
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val InputInnerHeight = 42.dp

@Composable
fun ChatInputBar(
    text: String,
    selectedImagePreview: ImageBitmap?,
    isMenuOpen: Boolean,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMenuClick: () -> Unit,
    onRemoveImageClick: () -> Unit,
    onVoiceRecordStart: () -> Unit,
    onVoiceRecordEnd: () -> Unit,
    onVoiceRecordCancel: () -> Unit,
    operatingHand: OperatingHand = OperatingHand.RIGHT
) {
    val hasText = text.isNotBlank()
    val canSend = hasText
    var isVoiceMode by remember { mutableStateOf(false) }
    var isVoiceRecording by remember { mutableStateOf(false) }
    var isVoiceCanceling by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isVoiceRecording) {
            VoiceRecordingPanel(isCanceling = isVoiceCanceling)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedImagePreview != null) {
                    SelectedImagePreview(
                        image = selectedImagePreview,
                        onRemoveClick = onRemoveImageClick
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canSend && operatingHand == OperatingHand.LEFT) {
                        SendButton(onClick = onSendClick)
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (!hasText) {
                        InputIconButton(
                            icon = Res.drawable.ic_camera,
                            contentDescription = "camera",
                            onClick = onCameraClick
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(InputInnerHeight),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (isVoiceMode && !canSend) {
                            VoiceHoldInput(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(InputInnerHeight),
                                onPressStart = {
                                    isVoiceRecording = true
                                    isVoiceCanceling = false
                                    onVoiceRecordStart()
                                },
                                onCancelStateChange = { isCanceling ->
                                    isVoiceCanceling = isCanceling
                                },
                                onPressEnd = { shouldCancel ->
                                    isVoiceRecording = false
                                    isVoiceCanceling = false

                                    if (shouldCancel) {
                                        onVoiceRecordCancel()
                                    } else {
                                        onVoiceRecordEnd()
                                    }
                                }
                            )
                        } else {
                            BasicTextField(
                                value = text,
                                onValueChange = onTextChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(InputInnerHeight),
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = Color(0xFF222222)
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(InputInnerHeight),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (text.isEmpty()) {
                                            Text(
                                                text = "\u53d1\u6d88\u606f\u6216\u6309\u4f4f\u8bf4\u8bdd...",
                                                fontSize = 16.sp,
                                                color = Color(0xFF9E9E9E)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (canSend && operatingHand != OperatingHand.LEFT) {
                        SendButton(onClick = onSendClick)
                    } else if (!canSend) {
                        if (isVoiceMode) {
                            InputIconButton(
                                icon = Res.drawable.ic_keyboard,
                                contentDescription = "keyboard",
                                iconSize = 28.dp,
                                buttonSize = 42.dp,
                                onClick = { isVoiceMode = false }
                            )
                        } else {
                            InputIconButton(
                                icon = Res.drawable.ic_sound,
                                contentDescription = "voice",
                                iconSize = 32.dp,
                                buttonSize = 42.dp,
                                onClick = { isVoiceMode = true }
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        MenuToggleButton(
                            isMenuOpen = isMenuOpen,
                            onClick = onMenuClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedImagePreview(
    image: ImageBitmap,
    onRemoveClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(10.dp))
    ) {
        Image(
            bitmap = image,
            contentDescription = "selected image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xAA000000))
                .clickable(onClick = onRemoveClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "x",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun VoiceHoldInput(
    modifier: Modifier = Modifier,
    onPressStart: () -> Unit,
    onCancelStateChange: (Boolean) -> Unit,
    onPressEnd: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isPressed) Color(0xFFEAF4FF) else Color(0xFFF7F8FA))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val startY = down.position.y
                        var shouldCancel = false

                        isPressed = true
                        onPressStart()

                        do {
                            val event = awaitPointerEvent()
                            val current = event.changes.first()
                            val offsetY = current.position.y - startY
                            val newCancelState = offsetY < -80f

                            if (newCancelState != shouldCancel) {
                                shouldCancel = newCancelState
                                onCancelStateChange(shouldCancel)
                            }
                        } while (event.changes.any { it.pressed })

                        isPressed = false
                        onPressEnd(shouldCancel)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u6309\u4f4f\u8bf4\u8bdd",
            color = if (isPressed) Color(0xFF1677FF) else Color(0xFF333333),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun VoiceRecordingPanel(isCanceling: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(if (isCanceling) Color(0xFFFF5A5F) else Color(0xFF35B8FF)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = if (isCanceling) {
                    "\u677e\u624b\u53d6\u6d88"
                } else {
                    "\u677e\u624b\u53d1\u9001\uff0c\u4e0a\u79fb\u53d6\u6d88"
                },
                color = Color.White,
                fontSize = 15.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(36) { index ->
                    val barHeight = if (index % 3 == 0) 18.dp else 10.dp
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}

@Composable
private fun InputIconButton(
    icon: DrawableResource,
    contentDescription: String,
    iconSize: Dp = 32.dp,
    buttonSize: Dp = 42.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun MenuToggleButton(
    isMenuOpen: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isMenuOpen) {
            Text(
                text = "x",
                color = Color.Black,
                fontSize = 26.sp
            )
        } else {
            Image(
                painter = painterResource(Res.drawable.ic_menu),
                contentDescription = "menu",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun SendButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0xFF0865FF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_send),
            contentDescription = "send",
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
fun ChatToolMenu(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFileClick: () -> Unit,
    onPhoneClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ToolMenuItem(
            modifier = Modifier.weight(1f),
            icon = Res.drawable.ic_menu_camera,
            title = "\u76f8\u673a",
            onClick = onCameraClick
        )
        ToolMenuItem(
            modifier = Modifier.weight(1f),
            icon = Res.drawable.ic_menu_album,
            title = "\u76f8\u518c",
            onClick = onGalleryClick
        )
        ToolMenuItem(
            modifier = Modifier.weight(1f),
            icon = Res.drawable.ic_menu_file,
            title = "\u6587\u4ef6",
            onClick = onFileClick
        )
        ToolMenuItem(
            modifier = Modifier.weight(1f),
            icon = Res.drawable.ic_menu_phone,
            title = "\u6253\u7535\u8bdd",
            onClick = onPhoneClick
        )
    }
}

@Composable
private fun ToolMenuItem(
    modifier: Modifier = Modifier,
    icon: DrawableResource,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF1F2F4)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = title,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = Color(0xFF666666),
                fontSize = 14.sp
            )
        }
    }
}
