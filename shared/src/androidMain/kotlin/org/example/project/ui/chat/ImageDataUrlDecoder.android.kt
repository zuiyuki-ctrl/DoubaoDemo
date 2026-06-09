package org.example.project.ui.chat

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun imageBitmapFromDataUrl(dataUrl: String): ImageBitmap? {
    val base64 = dataUrl.substringAfter(",", missingDelimiterValue = "")
    if (base64.isBlank()) return null

    val bytes = runCatching {
        Base64.decode(base64, Base64.DEFAULT)
    }.getOrNull() ?: return null

    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}
