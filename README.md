# Doubao AI Demo

基于 Kotlin Multiplatform + Compose Multiplatform 实现的仿豆包 AI 聊天客户端 Demo，当前主要运行目标为 Android。

项目实现了文本对话、图片识别、图片生成、联网搜索、语音输入、语音播报、历史消息保存、消息互动，以及端侧左右手识别等能力。其中左右手识别通过 TensorFlow 训练二分类模型，并使用 LiteRT/TFLite 在 Android 客户端本地推理，根据用户当前操作手自动调整发送按钮位置。

## 功能特性

- 文本对话：接入豆包文本模型，支持用户输入、AI 回复和 Loading 状态展示。
- 意图分类：根据用户输入自动判断普通对话、联网搜索或文生图。
- 联网搜索：使用火山引擎搜索能力增强模型回答。
- 图片识别：支持选择图片并调用视觉模型理解图片内容。
- 文生图：支持通过文本提示生成图片并在聊天流中展示。
- 语音输入：通过 Android AudioRecord 录音，调用 ASR 完成语音转文本。
- 语音播报：优先调用远端 TTS，失败时降级使用 Android 系统 TTS。
- 历史消息：使用本地存储保存聊天记录。
- 消息互动：支持复制、点赞、点踩、重新回复和追问。
- 左右手识别：端侧识别用户当前操作手，动态调整发送按钮左右位置。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 跨平台框架 | Kotlin Multiplatform |
| UI 框架 | Compose Multiplatform |
| Android 宿主 | Android Activity + Compose |
| 状态管理 | ViewModel + Compose state |
| 网络请求 | Ktor Client |
| JSON 序列化 | kotlinx.serialization |
| 本地存储 | SharedPreferences + JSON |
| 大模型能力 | 豆包 Ark Chat / Vision / Image |
| 联网搜索 | 火山引擎 Web Search |
| 语音识别 | 火山引擎 ASR |
| 语音合成 | 火山引擎 TTS + Android TextToSpeech |
| 端侧推理 | LiteRT / TensorFlow Lite Interpreter |
| 模型训练 | Python + TensorFlow + NumPy |

## 项目结构

```text
KotlinProject
├── androidApp
│   └── src/main
│       ├── kotlin/org/example/project
│       │   ├── MainActivity.kt
│       │   └── AudioRecorder.kt
│       └── assets
│           └── operating_hand.tflite
├── shared
│   └── src
│       ├── commonMain/kotlin/org/example/project
│       │   ├── App.kt
│       │   ├── config
│       │   ├── data
│       │   ├── model
│       │   ├── presentation
│       │   └── ui/chat
│       └── androidMain/kotlin/org/example/project
│           ├── data/local
│           ├── operatinghand
│           └── ui/chat
└── ml
    └── operating_hand
        ├── train_operating_hand.py
        ├── requirements.txt
        └── README.md
```

## 运行环境

- Android Studio
- JDK 11 或更高版本
- Android SDK
- Gradle Wrapper
- Python 3.11，只有训练左右手识别模型时需要

## 本地配置

项目中的模型接口 Key 通过 `local.properties` 注入，不应提交到 GitHub。

在项目根目录创建或修改 `local.properties`：

```properties
DOUBAO_API_KEY=your_api_key
DOUBAO_MODEL=your_chat_model
DOUBAO_VISION_MODEL=your_vision_model
DOUBAO_IMAGE_MODEL=your_image_model

WEB_SEARCH_API_KEY=your_web_search_api_key
WEB_SEARCH_API_KEY_ID=your_web_search_api_key_id

DOUBAO_ASR_APP_KEY=your_asr_app_key
DOUBAO_ASR_ACCESS_KEY=your_asr_access_key
DOUBAO_ASR_RESOURCE_ID=your_asr_resource_id

DOUBAO_TTS_APP_KEY=your_tts_app_key
DOUBAO_TTS_ACCESS_KEY=your_tts_access_key
DOUBAO_TTS_RESOURCE_ID=your_tts_resource_id
DOUBAO_TTS_SPEAKER=your_tts_speaker
```

部分字段在 `androidApp/build.gradle.kts` 中提供了默认值，但真实接口调用仍需要有效 Key。

## 构建 Android APK

Windows:

```powershell
.\gradlew.bat :androidApp:assembleDebug
```

macOS / Linux:

```bash
./gradlew :androidApp:assembleDebug
```

构建产物位于：

```text
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

## 左右手识别模型

### 训练数据格式

训练数据按左右手分目录存放：

```text
dataset
├── left
│   └── *.json
└── right
    └── *.json
```

每个 JSON 文件中可以包含多条触摸轨迹。每条轨迹由多个触摸点组成，每个点包含：

```text
x, y, width, height, density, downTimeDeltaMillis
```

### 特征处理

原始坐标会在训练和端侧推理时统一转换为相对特征：

```text
x / width
y / height
(x - startX) / width
(y - startY) / height
(time - startTime) / duration
durationSeconds
```

这样可以减少不同手机屏幕尺寸、Compose 布局坐标和原始训练数据坐标不一致带来的影响。

### 训练步骤

创建并激活 Python 虚拟环境：

```powershell
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
```

安装依赖：

```powershell
pip install -r ml\operating_hand\requirements.txt
```

训练并导出 TFLite 模型：

```powershell
python ml\operating_hand\train_operating_hand.py --data D:\dataset --output androidApp\src\main\assets\operating_hand.tflite --epochs 80
```

模型导出后需要重新构建 APK，新的 `operating_hand.tflite` 才会被打包进客户端。

## 左右手识别客户端链路

```text
Compose pointerInput 采集触摸轨迹
-> 过滤过短轨迹和多指触摸
-> 重采样为 9 个点
-> 转换为相对特征
-> AndroidOperatingHandClassifier 执行 TFLite 推理
-> 输出 LEFT / RIGHT 和置信度
-> 连续 2 次预测同一只手才切换 UI
-> ChatInputBar 调整发送按钮位置
```

默认隐藏调试信息。如需排查模型输出，可在 `ChatScreen.kt` 中打开：

```kotlin
private const val ShowOperatingHandDebug = true
```

## 测试

运行 shared Android host test：

```powershell
.\gradlew.bat :shared:testAndroidHostTest
```

构建检查：

```powershell
.\gradlew.bat :androidApp:assembleDebug
```

## 注意事项

- `.venv/`、`.gradle/`、`build/` 等本地环境和构建产物不应提交到 Git。
- `local.properties` 包含敏感 Key，不应提交到 Git。
- `androidApp/src/main/assets/operating_hand.tflite` 是客户端运行所需的端侧模型，可以随项目提交。
- Debug APK 使用调试签名，如果覆盖安装失败，可能是签名不一致，需要先卸载旧版本。

## 参考

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [OperatingHandRecognition](https://github.com/ahcyd008/OperatingHandRecognition)
- [LiteRT Android 文档](https://ai.google.dev/edge/litert/android)
