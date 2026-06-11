package org.example.project

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.example.project.config.ChatAppConfig
import org.example.project.data.AsrRepository
import org.example.project.data.TtsRepository
import org.example.project.data.local.AndroidChatLocalDataSource
import org.example.project.operatinghand.AndroidOperatingHandClassifier
import org.example.project.presentation.ChatViewModel
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val config = remember {
                ChatAppConfig(
                    doubaoApiKey = BuildConfig.DOUBAO_API_KEY,
                    doubaoModel = BuildConfig.DOUBAO_MODEL,
                    doubaoVisionModel = BuildConfig.DOUBAO_VISION_MODEL,
                    doubaoImageModel = BuildConfig.DOUBAO_IMAGE_MODEL,
                    doubaoTtsAppKey = BuildConfig.DOUBAO_TTS_APP_KEY,
                    doubaoTtsAccessKey = BuildConfig.DOUBAO_TTS_ACCESS_KEY,
                    doubaoTtsResourceId = BuildConfig.DOUBAO_TTS_RESOURCE_ID,
                    doubaoTtsSpeaker = BuildConfig.DOUBAO_TTS_SPEAKER,
                    webSearchApiKey = BuildConfig.WEB_SEARCH_API_KEY,
                    webSearchApiKeyId = BuildConfig.WEB_SEARCH_API_KEY_ID,
                    localDataSource = AndroidChatLocalDataSource(applicationContext),
                    doubaoAsrAppKey = BuildConfig.DOUBAO_ASR_APP_KEY,
                    doubaoAsrAccessKey = BuildConfig.DOUBAO_ASR_ACCESS_KEY,
                    doubaoAsrResourceId = BuildConfig.DOUBAO_ASR_RESOURCE_ID
                )
            }
            val coroutineScope = rememberCoroutineScope()
            val ttsRepository = remember(config) {
                TtsRepository.create(config)
            }
            val asrRepository = remember(config) {
                AsrRepository.create(config)
            }
            val chatViewModel: ChatViewModel = viewModel {
                ChatViewModel(config)
            }
            val operatingHandClassifier = remember {
                AndroidOperatingHandClassifier(applicationContext)
            }
            var selectedImageDataUrl by remember { mutableStateOf<String?>(null) }
            var selectedImagePreview by remember { mutableStateOf<ImageBitmap?>(null) }
            var isMenuOpen by remember { mutableStateOf(false) }
            var isTtsReady by remember { mutableStateOf(false) }
            var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
            val textToSpeech = remember {
                TextToSpeech(applicationContext) { status ->
                    isTtsReady = status == TextToSpeech.SUCCESS
                }
            }
            var hasAudioPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
            val audioRecorder = remember {
                AudioRecorder()
            }
            var currentVoiceFile by remember { mutableStateOf<File?>(null) }

            DisposableEffect(textToSpeech) {
                onDispose {
                    audioRecorder.cancel()
                    mediaPlayer?.release()
                    textToSpeech.stop()
                    textToSpeech.shutdown()
                    operatingHandClassifier.close()
                }
            }

            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    val imagePreview = uri.toImageBitmap(applicationContext)
                    if (imagePreview != null) {
                        selectedImageDataUrl = uri.toImageDataUrl(applicationContext)
                        selectedImagePreview = imagePreview
                    }
                }
            }

            val audioPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                hasAudioPermission = granted
            }

            App(
                config = config,
                viewModel = chatViewModel,
                selectedImagePreview = selectedImagePreview,
                isMenuOpen = isMenuOpen,
                onSendClick = {
                    val imageDataUrl = selectedImageDataUrl
                    if (imageDataUrl == null) {
                        chatViewModel.sendCurrentMessage()
                    } else {
                        chatViewModel.sendImageMessage(
                            text = chatViewModel.uiState.inputText,
                            imageDataUrl = imageDataUrl
                        )
                        selectedImageDataUrl = null
                        selectedImagePreview = null
                        isMenuOpen = false
                    }
                },
                onCameraClick = {
                    Toast.makeText(
                        applicationContext,
                        "相机功能暂不支持",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onMenuClick = {
                    isMenuOpen = !isMenuOpen
                },
                onGalleryClick = {
                    isMenuOpen = false
                    imagePickerLauncher.launch("image/*")
                },
                onFileClick = {
                    Toast.makeText(
                        applicationContext,
                        "文件功能暂不支持",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onPhoneClick = {
                    Toast.makeText(
                        applicationContext,
                        "打电话功能暂不支持",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onTopBackClick = {
                    Toast.makeText(
                        applicationContext,
                        "返回功能暂不支持",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onTopSearchClick = {
                    Toast.makeText(
                        applicationContext,
                        "搜索功能暂不支持",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onTopMuteClick = {
                    Toast.makeText(
                        applicationContext,
                        "静音功能暂不支持",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onTopMoreClick = {
                    Toast.makeText(
                        applicationContext,
                        "更多功能暂不支持",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onSpeakClick = { message ->
                    val speechText = message.content.toSpeechText()
                    coroutineScope.launch {
                        val audioBytes = runCatching {
                            ttsRepository.synthesize(speechText)
                        }.getOrElse { throwable ->
                            Log.e("DoubaoTTS", "豆包语音合成失败", throwable)
                            Toast.makeText(
                                applicationContext,
                                "豆包语音合成失败，已使用系统播报",
                                Toast.LENGTH_SHORT
                            ).show()
                            null
                        }

                        if (audioBytes != null) {
                            mediaPlayer?.release()
                            mediaPlayer = playAudioBytes(
                                context = applicationContext,
                                audioBytes = audioBytes
                            )
                        } else if (isTtsReady) {
                            textToSpeech.language = Locale.CHINESE
                            textToSpeech.speak(
                                speechText,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "message-${message.id}"
                            )
                        }
                    }
                },
                onVoiceRecordStart = {
                    if (!hasAudioPermission) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        return@App
                    }

                    val file = File(
                        cacheDir,
                        "voice_${System.currentTimeMillis()}.wav"
                    )
                    currentVoiceFile = file
                    audioRecorder.start(file)
                },
                onVoiceRecordEnd = {
                    if (currentVoiceFile == null) return@App

                    val file = audioRecorder.stop()
                    currentVoiceFile = null

                    if (file == null || file.length() <= WAV_HEADER_SIZE) {
                        Toast.makeText(
                            applicationContext,
                            "没有录到有效语音",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@App
                    }

                    coroutineScope.launch {
                        val voiceText = runCatching {
                            asrRepository.recognize(file.readBytes())
                        }.getOrElse { throwable ->
                            Log.e("DoubaoASR", "语音识别失败", throwable)
                            Toast.makeText(
                                applicationContext,
                                "语音识别失败：${throwable.message.orEmpty()}",
                                Toast.LENGTH_SHORT
                            ).show()
                            null
                        }

                        if (!voiceText.isNullOrBlank()) {
                            val imageDataUrl = selectedImageDataUrl
                            if (imageDataUrl == null) {
                                chatViewModel.sendTextFromVoice(voiceText)
                            } else {
                                chatViewModel.sendImageMessage(
                                    text = voiceText,
                                    imageDataUrl = imageDataUrl
                                )
                                selectedImageDataUrl = null
                                selectedImagePreview = null
                                isMenuOpen = false
                            }
                        }
                    }
                },
                onVoiceRecordCancel = {
                    audioRecorder.cancel()
                    currentVoiceFile = null
                },
                onRemoveImageClick = {
                    selectedImageDataUrl = null
                    selectedImagePreview = null
                },
                operatingHandClassifier = operatingHandClassifier
            )
        }
    }
}

private const val WAV_HEADER_SIZE = 44

private val searchReferenceTipRegex = Regex(
    pattern = """^搜索\s*\d+\s*个关键词，参考\s*\d+\s*篇(资料|文献)\s*>?\s*$"""
)

private fun String.toSpeechText(): String {
    val lines = lines()
    val speechLines = if (lines.firstOrNull()?.matches(searchReferenceTipRegex) == true) {
        lines.drop(1)
    } else {
        lines
    }

    return speechLines
        .joinToString("\n")
        .trim()
        .ifBlank { trim() }
}

private fun Uri.toImageDataUrl(context: Context): String {
    val mimeType = context.contentResolver.getType(this) ?: "image/jpeg"
    val bytes = context.contentResolver.openInputStream(this)?.use { inputStream ->
        inputStream.readBytes()
    } ?: ByteArray(0)
    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

    return "data:$mimeType;base64,$base64"
}

private fun Uri.toImageBitmap(context: Context): ImageBitmap? {
    val bitmap = context.contentResolver.openInputStream(this)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    }

    return bitmap?.asImageBitmap()
}

private fun playAudioBytes(
    context: Context,
    audioBytes: ByteArray
): MediaPlayer {
    val audioFile = File(context.cacheDir, "doubao_tts.mp3")
    audioFile.writeBytes(audioBytes)

    return MediaPlayer().apply {
        setDataSource(audioFile.absolutePath)
        setOnCompletionListener { player ->
            player.release()
        }
        prepare()
        start()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
